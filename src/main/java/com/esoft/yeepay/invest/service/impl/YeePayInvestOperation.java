package com.esoft.yeepay.invest.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.coupon.exception.ExceedDeadlineException;
import com.esoft.archer.coupon.exception.UnreachedMoneyLimitException;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.MapUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.invest.exception.ExceedMaxAcceptableRate;
import com.esoft.jdp2p.invest.exception.ExceedMoneyNeedRaised;
import com.esoft.jdp2p.invest.exception.IllegalLoanStatusException;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.service.InvestService;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.cptransaction.YeepayCpTransactionConstant;
import com.esoft.yeepay.cptransaction.requestModel.CPTransaction;
import com.esoft.yeepay.cptransaction.requestModel.Extend;
import com.esoft.yeepay.cptransaction.requestModel.FundDetail;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;
import com.esoft.yeepay.trusteeship.service.impl.YeePayOperationServiceAbs;

@Service("yeePayInvestOperation")
public class YeePayInvestOperation extends YeePayOperationServiceAbs<Invest> {

	@Resource
	InvestService investService;
	@Resource
	HibernateTemplate ht;
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;
	@Resource
	UserBillBO ubs;
	@Logger
	static Log log;
	
	
	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(Invest invest,
			FacesContext fc) throws IOException {
		
		try {
			investService.create(invest);
		} catch (InsufficientBalance e) {
			throw new YeePayOperationException("账户余额不足，请充值！", e);
		} catch (ExceedMoneyNeedRaised e) {
			throw new YeePayOperationException("投资金额不能大于尚未募集的金额！", e);
		} catch (ExceedMaxAcceptableRate e) {
			throw new YeePayOperationException("竞标利率不能大于借款者可接受的最高利率！", e);
		} catch (ExceedDeadlineException e) {
			throw new YeePayOperationException("红包过期！", e);
		} catch (UnreachedMoneyLimitException e) {
			throw new YeePayOperationException("红包未达到使用条件！", e);
		} catch (IllegalLoanStatusException e) {
			throw new YeePayOperationException("当前借款不可投资", e);
		}
		
		invest.setStatus(InvestConstants.InvestStatus.WAIT_AFFIRM);
		ht.saveOrUpdate(invest);
		invest.setLoan(ht.get(Loan.class, invest.getLoan().getId()));
		invest.setUser(ht.get(User.class, invest.getUser().getId()));
		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");
		//构建投资参数实体
		CPTransaction cp = new CPTransaction();
		cp.setNotifyUrl(YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.INVEST);
		cp.setCallbackUrl(YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.INVEST);
		//cp.setExpired(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(DateUtil.addMinute(new Date(), 10)));
		cp.setRequestNo(YeePayConstants.RequestNoPre.INVEST + invest.getId());
		cp.setPlateformNo(YeePayConstants.Config.MER_CODE);
		cp.setPlatformUserNo(invest.getUser().getId());
		cp.setBizType(YeepayCpTransactionConstant.TENDER.name());
		cp.setUserType("MEMBER");
		
		//资金明细
		String loanId = invest.getLoan().getId();
		Loan loan = ht.get(Loan.class, loanId);
		double fee = loan.getLoanGuranteeFee()==null?0.0:loan.getLoanGuranteeFee();
		FundDetail fd = new FundDetail();
		fd.setAmount(currentNumberFormat.format(invest.getMoney()-fee));
		fd.setTargetPlatformUserNo(loan.getUser().getId());
		fd.setTargetUserType("MEMBER");
		fd.setBizType(YeepayCpTransactionConstant.TENDER.name());
		cp.getFundDetails().add(fd);
		
		
		//平台分润(从第一次成功投资中扣取)
		if(!investBidSuccess(loan.getId()) && fee > 0.0){
			fd = new FundDetail();
			fd.setAmount(currentNumberFormat.format(fee));
			fd.setTargetPlatformUserNo(YeePayConstants.Config.MER_CODE);
			fd.setTargetUserType("MERCHANT");
			fd.setBizType(YeepayCpTransactionConstant.COMMISSION.name());
			cp.getFundDetails().add(fd);
		}
		
		//扩展参数
		Extend extend = new Extend();
		extend.setBorrowerPlatformUserNo(loan.getUser().getId());
		extend.setTenderAmount(currentNumberFormat.format(loan.getLoanMoney()));
		extend.setTenderDescription(loan.getDescription());
		extend.setTenderName(loan.getName());
		extend.setTenderOrderNo(loan.getId());
		cp.setExtend(extend);
		
		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", cp.toString());
		String sign = CFCASignUtil.sign(cp.toString());
		params.put("sign", sign);
		
		// 保存本地
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(invest.getId());
		to.setOperator(invest.getId());
		if (FacesUtil.isMobileRequest((HttpServletRequest) fc.getExternalContext().getRequest())) {
			to.setRequestUrl(YeePayConstants.RequestUrl.MOBILE_INVEST);
		} else {
			to.setRequestUrl(YeePayConstants.RequestUrl.INVEST);
		}
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.INVEST);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		try {
			sendOperation(to.getId(), "utf-8", fc);
		} catch (IOException e) {
			e.printStackTrace();
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
			throw new RuntimeException(e);
		}
		// 响应的参数 为xml格式
		String respXML = request.getParameter("resp");
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		log.debug(respXML);
		log.debug(sign);
		log.debug(flag);
		if (flag) {
			// 处理响应
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
			// 商户编号
			// 请求流水号:注册不传入请求流水号，返回无流水号,为投资id
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.INVEST, "");
			String code = resultMap.get("code");
			String description = resultMap.get("description");

			Invest invest = ht.get(Invest.class, requestNo);
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.INVEST, requestNo,
					"yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(),LockMode.UPGRADE);
			to.setResponseData(respXML);
			to.setResponseTime(new Date());
			if(TrusteeshipConstants.Status.PASSED.equals(to.getStatus())){
				return ;
			}
			if ("1".equals(code)) {
				if (invest.getStatus().equals(
						InvestConstants.InvestStatus.WAIT_AFFIRM)) {
					invest.setStatus(InvestConstants.InvestStatus.BID_SUCCESS);
					ht.update(invest);
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.update(to);
				}
			} else {
				if (invest.getStatus().equals(
						InvestConstants.InvestStatus.BID_SUCCESS)
						|| invest.getStatus().equals(
								InvestConstants.InvestStatus.WAIT_AFFIRM)) {
					invest.setStatus(InvestConstants.InvestStatus.CANCEL);
					ht.update(invest);
					// 改项目状态，项目如果是等待复核的状态，改为募集中
					if (invest.getLoan().getStatus()
							.equals(LoanConstants.LoanStatus.RECHECK) && invest.getLoan().getExpectTime().after(new Date())) {
						invest.getLoan().setStatus(
								LoanConstants.LoanStatus.RAISING);
						ht.update(invest.getLoan());
					}
					// 解冻投资金额
					try {
						ubs.unfreezeMoney(invest.getUser().getId(),
								invest.getMoney(), OperatorInfo.CANCEL_INVEST,
								"投资：" + invest.getId() + "失败，解冻投资金额, 借款ID："
										+ invest.getLoan().getId());
					} catch (InsufficientBalance e) {
						//throw new RuntimeException(e);
						e.printStackTrace();
					}
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
					ht.update(to);
				}
				// 真实错误原因
				//throw new TrusteeshipReturnException(description);
				// FacesUtil.addErrorMessage(description);

			}
		}

	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationS2SCallback(ServletRequest request,
			ServletResponse response) {
		// 响应的参数 为xml格式
		String notifyXML = request.getParameter("notify");
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(notifyXML, sign);
		log.debug(notifyXML);
		log.debug(sign);
		log.debug(flag);
		if (flag) {
			if (log.isDebugEnabled()) {
				log.debug("TrustheeshipInvest receiveOperationS2SCallback respXML:"
						+ notifyXML);
			}
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(notifyXML);
			String code = resultMap.get("code");
			String message = resultMap.get("message");
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.INVEST, "");

			Invest invest = ht.get(Invest.class, requestNo);
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.INVEST, requestNo,
					"yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(),LockMode.UPGRADE);
			to.setResponseTime(new Date());
			to.setResponseData(notifyXML);
			if(TrusteeshipConstants.Status.PASSED.equals(to.getStatus())){
				return ;
			}
			if ("1".equals(code)) {
				if (invest.getStatus().equals(
						InvestConstants.InvestStatus.WAIT_AFFIRM)) {
					invest.setStatus(InvestConstants.InvestStatus.BID_SUCCESS);
					ht.update(invest);
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.update(to);
				}
			} else {
				if (invest.getStatus().equals(
						InvestConstants.InvestStatus.WAIT_AFFIRM)) {
					invest.setStatus(InvestConstants.InvestStatus.CANCEL);
					ht.update(invest);
					// 改项目状态，项目如果是等待复核的状态，改为募集中
					if (invest.getLoan().getStatus()
							.equals(LoanConstants.LoanStatus.RECHECK)) {
						invest.getLoan().setStatus(
								LoanConstants.LoanStatus.RAISING);
						ht.update(invest.getLoan());
					}
					// 解冻投资金额
					try {
						ubs.unfreezeMoney(invest.getUser().getId(),
								invest.getMoney(), OperatorInfo.CANCEL_INVEST,
								"投资：" + invest.getId() + "失败，解冻投资金额, 借款ID："
										+ invest.getLoan().getId());
					} catch (InsufficientBalance e) {
						e.printStackTrace();
						//throw new RuntimeException(e);
					}
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
					ht.update(to);
				}
				// 真实错误原因
				log.debug("返回错误代码："+code+ ":" + message);
			/*	throw new RuntimeException(new TrusteeshipReturnException(code
						+ ":" + message));*/
			}
		}
		try {
			response.getWriter().write("SUCCESS");
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	private boolean investBidSuccess(String loanId){
		
		String hql = "select count(*) from Invest  invest where invest.loan.id='"+loanId+"' and invest.status = '"+InvestStatus.BID_SUCCESS+"'";
		Integer count  = (Integer)ht.find(hql).iterator().next();
		
		return count != null && count > 0?true:false;
	}

}
