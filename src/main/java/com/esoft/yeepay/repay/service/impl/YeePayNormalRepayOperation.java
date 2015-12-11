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

import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.service.impl.UserBillBO;
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
import com.esoft.jdp2p.repay.exception.NormalRepayException;
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
@Service("yeePayNormalRepayOperation")
public class YeePayNormalRepayOperation extends
		YeePayOperationServiceAbs<LoanRepay> {
	@Logger
	static Log log;

	@Resource
	HibernateTemplate ht;

	@Resource
	UserBillBO userBillBO;

	@Resource
	SystemBillService systemBillService;

	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;
	@Resource
	RepayService repayService;
	
	@Resource
	TransferService transferService;
	
	@Resource
	YeepayCpTransacionOperation yeepayCpTransacionOperation;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(LoanRepay loanRepay,
			FacesContext fc) throws IOException {
		// FIXME:验证
		LoanRepay repay = ht.get(LoanRepay.class, loanRepay.getId());
		List<InvestRepay> irs = ht
				.find("from InvestRepay ir where ir.invest.loan.id=? and ir.period=?",
						new Object[] { repay.getLoan().getId(),
								repay.getPeriod() });
		
		// 取消投资下面的所有正在转让的债权
		String hql = "from TransferApply ta where ta.invest.loan.id=? and ta.status=?";
		List<TransferApply> tas = ht.find(hql, new String[] { repay.getLoan().getId(),
				TransferStatus.WAITCONFIRM });
		if (tas.size() > 0) {
			// 有购买了等待第三方确认的债权，所以不能还款。
			throw new YeePayOperationException("有等待第三方确认的债权转让，不能还款！");
		}
		tas = ht.find(hql, new String[] { repay.getLoan().getId(),
				TransferStatus.TRANSFERING });
		for (TransferApply ta : tas) {
			transferService.cancel(ta.getId());
		}

		// 判断标志 还款金额是否有小于1的投资
		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");
		
		//构建投资参数实体
		CPTransaction cp = new CPTransaction();
		cp.setNotifyUrl(YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.REPAY);
		cp.setCallbackUrl(YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.REPAY);
		cp.setExpired(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(DateUtil.addMinute(new Date(), 10)));
		cp.setRequestNo(YeePayConstants.RequestNoPre.REPAY + repay.getId());
		cp.setPlateformNo(YeePayConstants.Config.MER_CODE);
		cp.setPlatformUserNo(repay.getLoan().getUser().getId());
		cp.setBizType(YeepayCpTransactionConstant.REPAYMENT.name());
		cp.setUserType("MEMBER");
		
		//资金明细
		double irRepayMoney = 0D;
		double irFee = 0D;
		for(InvestRepay ir:irs){
			// 借款人还款手续费
			if (irs.get(0).equals(ir)) {
				irRepayMoney = ArithUtil.add(ir.getCorpus(), ir.getInterest());
				irFee += ArithUtil.add(ir.getFee(), repay.getFee());
			} else {
				irRepayMoney = ArithUtil.add(ir.getCorpus(), ir.getInterest());
				irFee += ir.getFee();
			}
			//投资账户资金明细
			log.debug("投资人id:" + ir.getInvest().getUser().getId());
			log.debug("投资人得到的钱:" + (irRepayMoney - ir.getFee()));
			FundDetail fd = new FundDetail();
			fd.setAmount(String.valueOf(currentNumberFormat.format(ArithUtil.sub(irRepayMoney, ir.getFee()))));
			fd.setTargetPlatformUserNo(ir.getInvest().getUser().getId());
			fd.setTargetUserType("MEMBER");
			fd.setBizType(YeepayCpTransactionConstant.REPAYMENT.name());
			cp.getFundDetails().add(fd);
			
		}
		//平台分润资金明细,当分润大于0的时间添加到资金明细中,(此处将平台分润统计后一次性转账)
		log.debug("平台得到的钱:" + irFee);
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
		extend.setTenderOrderNo(repay.getLoan().getId());
		cp.setExtend(extend);
		log.debug("正常还款报文"+cp.toString());
		
		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", cp.toString());
		String sign = CFCASignUtil.sign(cp.toString());
		params.put("sign", sign);

		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(repay.getId());
		to.setOperator(repay.getId());
		to.setRequestUrl(YeePayConstants.RequestUrl.REPAY);
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(TrusteeshipConstants.OperationType.REPAY);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		try {
			sendOperation(to.getId(), "utf-8", fc);
		} catch (IOException e) {
			throw new YeePayOperationException("网络连接不通畅！", e);
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
		log.debug("正常还款页面回跳报文respXML："+respXML.toString());
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		if (flag) {
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
			String code = resultMap.get("code");
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.REPAY, "");
			LoanRepay repay = ht.get(LoanRepay.class, requestNo);
			TrusteeshipOperation to = trusteeshipOperationBO.get(YeePayConstants.OperationType.REPAY, requestNo,"yeepay");
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
				boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.REPAY+requestNo, "CONFIRM");
				try {
					log.debug("repayservice.normalRepay(repayID" + requestNo+ ")result = " + result + " repayStatus = " + repay.getStatus());
					if(result){//易宝转账确认成功
						if (repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING)) {
							repayService.normalRepay(requestNo);
						}
					}else{//易宝转账确认失败
						throw new YeePayOperationException("易宝转账确认失败");
					}
				} catch (InsufficientBalance e) {
					throw new YeePayOperationException(e.getMessage(), e);
				} catch (NormalRepayException e) {
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
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.REPAY, "");
			log.debug(notifyXML);
			LoanRepay repay = ht.get(LoanRepay.class, requestNo);
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.REPAY,  requestNo,
					"yeepay");
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
				boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.REPAY+requestNo, "CONFIRM");
				try {
					log.debug("repayservice.normalRepay(repayID" + requestNo + ")");
					if(result){//易宝转账确认成功
						if (repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING)) {
							repayService.normalRepay(requestNo);
						}
					}else{//易宝转账确认失败
						throw new YeePayOperationException("易宝转账确认失败");
					}
				} catch (InsufficientBalance e) {
					throw new YeePayOperationException(e.getMessage(), e);

				} catch (NormalRepayException e) {
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
			throw new YeePayOperationException("网络连接不通畅！", e);
		}
	}
	


}
