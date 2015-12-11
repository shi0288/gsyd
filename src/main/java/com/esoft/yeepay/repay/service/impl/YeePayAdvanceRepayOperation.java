package com.esoft.yeepay.repay.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.risk.FeeConfigConstants.FeePoint;
import com.esoft.archer.risk.FeeConfigConstants.FeeType;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.DateUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.MapUtil;
import com.esoft.jdp2p.invest.InvestConstants.TransferStatus;
import com.esoft.jdp2p.invest.model.TransferApply;
import com.esoft.jdp2p.invest.service.TransferService;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.LoanConstants.RepayStatus;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.repay.exception.AdvancedRepayException;
import com.esoft.jdp2p.repay.model.InvestRepay;
import com.esoft.jdp2p.repay.model.LoanRepay;
import com.esoft.jdp2p.repay.service.RepayService;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.cptransaction.YeepayCpTransacionOperation;
import com.esoft.yeepay.cptransaction.YeepayCpTransactionConstant;
import com.esoft.yeepay.cptransaction.requestModel.CPTransaction;
import com.esoft.yeepay.cptransaction.requestModel.Extend;
import com.esoft.yeepay.cptransaction.requestModel.FundDetail;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;
import com.esoft.yeepay.trusteeship.service.impl.YeePayOperationServiceAbs;

@Service("yeePayAdvanceRepayOperation")
public class YeePayAdvanceRepayOperation extends
		YeePayOperationServiceAbs<Loan> {

	@Logger
	static Log log;

	@Resource
	HibernateTemplate ht;

	@Resource
	FeeConfigBO feeConfigBO;

	@Resource
	TransferService transferService;

	@Resource
	RepayService repayService;

	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;
	
	@Resource
	YeepayCpTransacionOperation yeepayCpTransacionOperation;

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(Loan la, FacesContext fc)
			throws IOException {
		Loan loan = ht.get(Loan.class, la.getId());
		// 查询当期还款是否已还清
		String repaysHql = "select lr from LoanRepay lr where lr.loan.id = ?";
		List<LoanRepay> repays = ht.find(repaysHql, la.getId());
		// 剩余所有本金
		double sumCorpus = 0D;
		// 手续费总额
		double feeAll = 0D;
		for (LoanRepay repay : repays) {
			if (repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING)) {
				// 在还款期，而且没还款
				sumCorpus = ArithUtil.add(sumCorpus, repay.getCorpus());
				feeAll = ArithUtil.add(feeAll, repay.getFee());
			}
		}
		// 给投资人的罚金
		double feeToInvestor = feeConfigBO.getFee(
				FeePoint.ADVANCE_REPAY_INVESTOR, FeeType.PENALTY, null, null,
				sumCorpus);
		// 给系统的罚金
		double feeToSystem = feeConfigBO.getFee(FeePoint.ADVANCE_REPAY_SYSTEM,
				FeeType.PENALTY, null, null, sumCorpus);
		if (log.isDebugEnabled()) {
			log.debug("feeToSystem: "+feeToSystem);			
			log.debug("feeToInvestor: "+feeToInvestor);			
		}
		
		// 取消投资下面的所有正在转让的债权
		String hql = "from TransferApply ta where ta.invest.loan.id=? and ta.status=?";
		List<TransferApply> tas = ht.find(hql, new String[] { loan.getId(),
				TransferStatus.WAITCONFIRM });
		if (tas.size() > 0) {
			// 有购买了等待第三方确认的债权，所以不能还款。
			throw new YeePayOperationException("有等待第三方确认的债权转让，不能还款！");
		}
		tas = ht.find(hql, new String[] { loan.getId(),
				TransferStatus.TRANSFERING });
		for (TransferApply ta : tas) {
			transferService.cancel(ta.getId());
		}

		// 余额不足，抛异常
		// 按比例分给投资人和系统（默认优先给投资人，剩下的给系统，以防止提转差额的出现）
		List<InvestRepay> irs = ht
				.find("from InvestRepay ir where ir.invest.loan.id=? and ir.status=?",
						new Object[] { loan.getId(), RepayStatus.REPAYING });

		// 判断标志 还款金额是否有小于1的投资
		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");
		
		//构建投资参数实体
		CPTransaction cp = new CPTransaction();
		cp.setNotifyUrl(YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.ADVANCE_REPAY);
		cp.setCallbackUrl(YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.ADVANCE_REPAY);
		cp.setExpired(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(DateUtil.addMinute(new Date(), 10)));
		cp.setRequestNo(YeePayConstants.RequestNoPre.ADVANCE_REPAY + loan.getId());
		cp.setPlateformNo(YeePayConstants.Config.MER_CODE);
		cp.setPlatformUserNo(loan.getUser().getId());
		cp.setBizType(YeepayCpTransactionConstant.REPAYMENT.name());
		cp.setUserType("MEMBER");
		
		// 更改投资的还款信息
		double feeToInvestorTemp = feeToInvestor;
		//平台分润
		double irFee = 0D;
		//资金明细
		for (int i = 0; i < irs.size(); i++) {
			InvestRepay ir = irs.get(i);
			// 判断本金是否为0和null,易宝还款金额不能为0
			if (ir.getCorpus() != 0 && ir.getCorpus() != null) {
				// 罚金
				double ircashFineToInvestor = 0D;
				double irRepayMoney = 0D;
				if (i == irs.size() - 1) {
					ircashFineToInvestor = feeToInvestorTemp;
				} else {
					ircashFineToInvestor = ArithUtil.round(ir.getCorpus()
							/ sumCorpus * feeToInvestor, 2);
					feeToInvestorTemp = ArithUtil.sub(feeToInvestorTemp,
							ircashFineToInvestor);
				}
				irRepayMoney = ArithUtil.add(ir.getCorpus(),
						ircashFineToInvestor);
				
				//投资账户资金明细
				FundDetail fd = new FundDetail();
				fd.setAmount(String.valueOf(currentNumberFormat.format(irRepayMoney)));
				fd.setTargetPlatformUserNo(ir.getInvest().getUser().getId());
				fd.setTargetUserType("MEMBER");
				fd.setBizType(YeepayCpTransactionConstant.REPAYMENT.name());
				cp.getFundDetails().add(fd);
			}	
		}
		irFee = ArithUtil.add(feeAll, feeToSystem);
		log.debug("提前还款平台得到的钱" + irFee);
		//平台分润资金明细,当分润大于0的时间添加到资金明细中
		if(irFee > 0){
			FundDetail fd = new FundDetail();
			fd.setAmount(String.valueOf(currentNumberFormat.format(irFee)));
			fd.setTargetPlatformUserNo(YeePayConstants.Config.MER_CODE);
			fd.setTargetUserType("MERCHANT");
			fd.setBizType(YeepayCpTransactionConstant.COMMISSION.name());
			cp.getFundDetails().add(fd);
		}
		//扩展参数
		Extend extend = new Extend();
		extend.setTenderOrderNo(loan.getId());
		cp.setExtend(extend);
		
		
		log.debug("提前还款发送信息" + cp.toString());

		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", cp.toString());
		String sign = CFCASignUtil.sign(cp.toString());
		params.put("sign", sign);

		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(loan.getId());
		to.setOperator(loan.getId());
		to.setRequestUrl(YeePayConstants.RequestUrl.REPAY);
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.ADVANCE_REPAY);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		try {
			sendOperation(to.getId(), "utf-8", fc);
		} catch (IOException e) {
			throw new YeePayOperationException("网络连接不通畅", e);
		}
		return null;
		
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationPostCallback(ServletRequest request)
			throws TrusteeshipReturnException {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new YeePayOperationException(e.getMessage(), e);
		}
		// 响应的参数 为xml格式
		String respXML = request.getParameter("resp");
		log.debug(respXML.toString());
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		if (flag) {
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
			String code = resultMap.get("code");
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.ADVANCE_REPAY, "");
			log.debug(respXML);
			Loan loan = ht.get(Loan.class, requestNo);
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.ADVANCE_REPAY,
					requestNo, "yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(),
					LockMode.UPGRADE);
			to.setResponseTime(new Date());
			if(TrusteeshipConstants.Status.PASSED.equals(to.getStatus())){
				return ;
			}
			if ("1".equals(code)) {
				//1.还款操作完成，易宝冻结了该借款用户的金额
				//2.调用解冻并转让金额接口
				boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.ADVANCE_REPAY+requestNo, "CONFIRM");
				try {
					log.debug("repayservice.advanceRepay(repayID" + requestNo
							+ ")");
					if(result){//易宝转账确认成功
						if (loan.getStatus().equals(
								LoanConstants.LoanStatus.REPAYING) ) {
							repayService.advanceRepay(requestNo);
						}
					}else{//易宝转账确认失败
						throw new YeePayOperationException("易宝转账确认失败");
					}
					
				} catch (InsufficientBalance e) {
					throw new YeePayOperationException(e.getMessage(), e);
				} catch (AdvancedRepayException e) {
					throw new YeePayOperationException(e.getMessage(), e);
				}
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				to.setResponseData(respXML);
				to.setResponseTime(new Date());
				ht.update(to);
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(respXML);
				to.setResponseTime(new Date());
				ht.update(to);
			}
		}
		log.debug("respXML:" + respXML + "sign:" + sign + "flag" + flag);
	}

	/**
	 * 回调通知
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationS2SCallback(ServletRequest request,
			ServletResponse response) {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new YeePayOperationException(e.getMessage(), e);
		}

		// 响应的参数 为xml格式
		String notifyXML = request.getParameter("notify");
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(notifyXML, sign);
		log.debug("notifyXML:" + notifyXML + "sign:" + sign + "flag" + flag);
		if (flag) {
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(notifyXML);
			String code = resultMap.get("code");
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.ADVANCE_REPAY, "");
			log.debug(notifyXML);
			Loan loan = ht.get(Loan.class, requestNo);
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.ADVANCE_REPAY, requestNo,
					requestNo, "yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(),
					LockMode.UPGRADE);
			to.setResponseTime(new Date());
			if(TrusteeshipConstants.Status.PASSED.equals(to.getStatus())){
				return ;
			}
			if ("1".equals(code)) {
				//1.还款操作完成，易宝冻结了该借款用户的金额
				//2.调用解冻并转让金额接口
				boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.ADVANCE_REPAY+requestNo, "CONFIRM");
				try {
					log.debug("repayservice.advanceRepay(repayID" + requestNo
							+ ")");
					if(result){//易宝转账确认成功
						if (loan.getStatus().equals(
								LoanConstants.LoanStatus.REPAYING) ) {
							repayService.advanceRepay(requestNo);
						}
					}else{//易宝转账确认失败
						throw new YeePayOperationException("易宝转账确认失败");
					}
				} catch (InsufficientBalance e) {
					throw new YeePayOperationException(e.getMessage(), e);
				} catch (AdvancedRepayException e) {
					throw new YeePayOperationException(e.getMessage(), e);
				}
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				to.setResponseData(notifyXML);
				to.setResponseTime(new Date());
				ht.update(to);
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(notifyXML);
				to.setResponseTime(new Date());
				ht.update(to);
			}
		}
		try {
			response.getWriter().print("SUCCESS");
			FacesUtil.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			throw new YeePayOperationException("网络连接不通畅", e);
		}
	}
}
