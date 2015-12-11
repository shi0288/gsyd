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
import org.hibernate.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.esoft.archer.common.exception.NoMatchingObjectsException;
import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.coupon.CouponConstants;
import com.esoft.archer.coupon.exception.ExceedDeadlineException;
import com.esoft.archer.coupon.exception.UnreachedMoneyLimitException;
import com.esoft.archer.coupon.model.UserCoupon;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.DateUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.MapUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.invest.exception.ExceedCoupon;
import com.esoft.jdp2p.invest.exception.ExceedMoneyNeedRaised;
import com.esoft.jdp2p.invest.exception.IllegalLoanStatusException;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.service.InvestService;
import com.esoft.jdp2p.loan.LoanConstants.LoanStatus;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.LoanCalculator;
import com.esoft.jdp2p.loan.service.LoanService;
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

@Service
public class YeePayInvestNofreezeOperation extends
		YeePayOperationServiceAbs<Invest> {
	@Logger
	static Log log;
	@Resource
	InvestService investService;
	@Resource
	HibernateTemplate ht;
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;
	@Resource
	UserBillBO ubs;
	@Resource
	LoanCalculator loanCalculator;
	@Resource
	LoanService loanService;
	@Resource
	YeepayCpTransacionOperation yeepayCpTransacionOperation;
	@Resource
	ConfigService configService;
	@Autowired
	private ApplicationContext applicationContext;
	@SuppressWarnings("deprecation")
	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(Invest invest, FacesContext fc)
			throws IOException {

		try {
			String loanId = invest.getLoan().getId();
			Loan loan = ht.get(Loan.class, loanId);
			ht.evict(loan);// 防止并发出现
			loan = ht.get(Loan.class, loanId, LockMode.UPGRADE);
			if (!loan.getStatus().equals(LoanStatus.RAISING)) {
				throw new IllegalLoanStatusException("标的【" + loan.getId() + "】状态(" + loan.getStatus() + ")不合法");
			}

			double remainMoney = loanCalculator.calculateMoneyNeedRaised(loan.getId());// 尚未认购金额
			if (invest.getMoney() > remainMoney) {// 投资金额大于尚未认购的金额
				throw new ExceedMoneyNeedRaised();
			}

			// 判断是否有代金券；判断能否用代金券
			if (invest.getUserCoupon() != null) {
				log.debug(invest.getUserCoupon().getCoupon().getMoney()+"==================================");
				// 代金券不是未使用状态，抛出异常
				if (!invest.getUserCoupon().getStatus().equals(CouponConstants.UserCouponStatus.UNUSED)) {
					throw new ExceedDeadlineException();
				}
				// 判断代金券是否达到使用条件(即投资的钱大于代金券的钱数要求，例：投资100元方可使用代金卷)
				if (invest.getMoney() < invest.getUserCoupon().getCoupon().getLowerLimitMoney()) {
					throw new UnreachedMoneyLimitException();
				}
				// 用户填写认购的钱数以后，判断余额，如果余额不够，不能提交，弹出新页面让用户充值
				// investMoney>代金券金额+用户余额
				if (invest.getMoney() > (invest.getUserCoupon().getCoupon().getMoney() + ubs.getBalance(invest.getUser().getId()))) {
					throw new InsufficientBalance();
				}
				// investMoney<代金券金额
				if (invest.getMoney() < invest.getUserCoupon().getCoupon().getMoney()) {
					throw new ExceedCoupon();
				}
				// investMoney > 可用余额，抛异常
			} else if (invest.getMoney() > ubs.getBalance(invest.getUser().getId())) {
				throw new InsufficientBalance();
			}

			invest.setInvestMoney(invest.getMoney());
			invest.setId(investService.generateId(invest.getLoan().getId()));
			invest.setLoan(ht.get(Loan.class, invest.getLoan().getId()));
			invest.setUser(ht.get(User.class, invest.getUser().getId()));
			invest.setStatus(InvestConstants.InvestStatus.CANCEL);
			invest.setRate(loan.getRate());
			invest.setTime(new Date());
			ht.saveOrUpdate(invest);

			if (invest.getTransferApply() == null
					|| StringUtils.isEmpty(invest.getTransferApply().getId())) {
				invest.setTransferApply(null);
			}
		} catch (InsufficientBalance e) {
			throw new YeePayOperationException("账户余额不足，请充值！", e);
		} catch (ExceedMoneyNeedRaised e) {
			throw new YeePayOperationException("投资金额不能大于尚未募集的金额！", e);
		} catch (IllegalLoanStatusException e) {
			throw new YeePayOperationException("当前借款不可投资", e);
		} catch (NoMatchingObjectsException e) {
			throw new YeePayOperationException("找不到匹配配置", e);
		} catch (ExceedDeadlineException e) {
			throw new YeePayOperationException("优惠券超过有效期！", e);
		} catch (UnreachedMoneyLimitException e) {
			throw new YeePayOperationException("金额未达到优惠券使用条件！", e);
		} catch (ExceedCoupon e) {
			throw new YeePayOperationException("投资金额必须大于优惠劵", e);
		}

		// 数字十进制格式
		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");

		// 构建投资参数实体
		CPTransaction cp = new CPTransaction();
		cp.setNotifyUrl(YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.INVEST);
		cp.setCallbackUrl(YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.INVEST);
		// cp.setExpired(new
		// SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(DateUtil.addMinute(new
		// Date(), 10)));
		cp.setRequestNo(YeePayConstants.RequestNoPre.INVEST + invest.getId());
		cp.setPlateformNo(YeePayConstants.Config.MER_CODE);
		cp.setPlatformUserNo(invest.getUser().getId());
		cp.setBizType(YeepayCpTransactionConstant.TENDER.name());
		cp.setUserType("MEMBER");

		// 资金明细
		String loanId = invest.getLoan().getId();
		Loan loan = ht.get(Loan.class, loanId);
		//借款管理费guranteeFee,即：放款时的借款手续费
		double guranteeFee = loan.getLoanGuranteeFee() == null ? 0.0 : loan
				.getLoanGuranteeFee();
		//按照投资比例，计算借款管理费
		double fee = ArithUtil.round(
				ArithUtil.mul(guranteeFee, ArithUtil.div(invest.getInvestMoney(), loan.getLoanMoney())), 2);
		FundDetail fd = new FundDetail();
		//投资人给借款人的钱=投资金额-fee-红包钱
//		fd.setAmount(currentNumberFormat.format(invest.getMoney() - fee));
		if (invest.getUserCoupon() != null) {
			fd.setAmount(currentNumberFormat.format(invest.getMoney() - fee - invest.getUserCoupon().getCoupon().getMoney()));
			log.debug("易宝冻结的资金：" + (invest.getMoney() - invest.getUserCoupon().getCoupon().getMoney()));
			invest.setCouponMoney(invest.getUserCoupon().getCoupon().getMoney());
		} else {
			fd.setAmount(currentNumberFormat.format(invest.getMoney() - fee));
			invest.setCouponMoney(0D);
		}
		fd.setTargetPlatformUserNo(loan.getUser().getId());
		fd.setTargetUserType("MEMBER");
		fd.setBizType(YeepayCpTransactionConstant.TENDER.name());
		cp.getFundDetails().add(fd);

		// 平台分润
		if (fee > 0.0) {
			fd = new FundDetail();
			//投资人给平台的钱=fee
			fd.setAmount(currentNumberFormat.format(fee));
			fd.setTargetPlatformUserNo(YeePayConstants.Config.MER_CODE);
			fd.setTargetUserType("MERCHANT");
			fd.setBizType(YeepayCpTransactionConstant.COMMISSION.name());
			cp.getFundDetails().add(fd);
		}

		// 扩展参数
		Extend extend = new Extend();
		extend.setBorrowerPlatformUserNo(loan.getUser().getId());
		// 判断是否使用红包
		if (invest.getUserCoupon() != null) {
			// 使用代金券：冻结金额=投资金额-代金券金额
			extend.setTenderAmount(currentNumberFormat.format(invest.getMoney() - invest.getUserCoupon().getCoupon().getMoney()));
		} else {
			// 未使用代金券：冻结金额=投资金额
			extend.setTenderAmount(currentNumberFormat.format(loan.getLoanMoney()));
		}
		extend.setTenderDescription(loan.getName());
		extend.setTenderName(loan.getName());
		extend.setTenderOrderNo(loan.getId());
		cp.setExtend(extend);

		log.debug("易宝投资不冻结请求报文req：" + cp.toString());

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
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.INVEST);
		to.setTrusteeship("yeepay");
		if (FacesUtil.isMobileRequest((HttpServletRequest) fc.getExternalContext().getRequest())) {
			to.setRequestUrl(YeePayConstants.RequestUrl.MOBILE_INVEST);
		} else {
			to.setRequestUrl(YeePayConstants.RequestUrl.INVEST);
		}
		trusteeshipOperationBO.save(to);
		try {
			sendOperation(to.getId(), "utf-8", fc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings({ "deprecation", "unchecked" })
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationPostCallback(ServletRequest request)
			throws TrusteeshipReturnException {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		String respXML = request.getParameter("resp");// 响应的参数 为xml格式
		log.debug("易宝 投资不冻结页面回调报文respXML：" + respXML);
		String sign = request.getParameter("sign");// 签名
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		if (flag) {// 验签成功

			Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.INVEST, "");// 请求流水号
			TrusteeshipOperation to = trusteeshipOperationBO.get(YeePayConstants.OperationType.INVEST, requestNo, "yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(), LockMode.UPGRADE);
			if (TrusteeshipConstants.Status.PASSED.equals(to.getStatus())) {// 状态是已发送，则不做后续处理
				return;
			}

			String description = resultMap.get("description");
			String code = resultMap.get("code");
			Invest invest = ht.get(Invest.class, requestNo);
			if ("1".equals(code)) {// 易宝处理成功

				to.setResponseData(respXML);
				to.setResponseTime(new Date());
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				ht.update(to);

				Loan loan = invest.getLoan();
				ht.evict(loan);
				loan = ht.get(Loan.class, invest.getLoan().getId(), LockMode.UPGRADE);
				double remainMoney = 0.0;
				try {
					remainMoney = loanCalculator.calculateMoneyNeedRaised(loan.getId());// 尚未募集的金额
				} catch (NoMatchingObjectsException e1) {
					e1.printStackTrace();
				}
				log.debug(invest.getStatus().equals(InvestConstants.InvestStatus.CANCEL) + "invest.getStatus()---------------------->" + invest.getStatus());
				if (invest.getMoney() > remainMoney || !LoanStatus.RAISING.equals(loan.getStatus())) {// 投资金额大于尚未募集的金额或者标的状态不是“投资中”
					// 调用易宝转账取消端口
					boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.INVEST + requestNo, "CANCEL");
					if (!result) {
						log.debug("投资【" + invest.getId() + "】易宝转账取消失败");
						// throw new YeePayOperationException("投资易宝转账取消失败");
					}
				} else if (invest.getStatus().equals(
						InvestConstants.InvestStatus.CANCEL)) {// 可以投资

					try {
						invest.setStatus(InvestConstants.InvestStatus.BID_SUCCESS);
						// 标的是否投满，投满改变状态
						loanService.dealRaiseComplete(loan.getId());
						// 是否使用红包
						if (invest.getUserCoupon() != null) {
							// 设置红包状态
							UserCoupon userCoupon = invest.getUserCoupon();
							ht.evict(userCoupon);
							userCoupon = ht.get(UserCoupon.class, invest
									.getUserCoupon().getId(), LockMode.UPGRADE);
							if(CouponConstants.UserCouponStatus.USED.equals(userCoupon.getStatus())){//红包已使用
								boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.INVEST + requestNo, "CANCEL");
								log.info("投资【" + invest.getId() + "】红包已使用，红包："+userCoupon.getId());
								if (!result) {
									log.debug("投资【" + invest.getId() + "】易宝转账取消失败");
								}
								throw new TrusteeshipReturnException("红包已使用");
							}
							userCoupon.setStatus(CouponConstants.UserCouponStatus.USED);
							userCoupon.setUsedTime(new Date());
							ht.update(userCoupon);
							
							ubs.freezeMoney(invest.getUser().getId(),
									invest.getMoney()
											- userCoupon.getCoupon().getMoney(),
									OperatorInfo.INVEST_SUCCESS,
									"投资成功：冻结金额。借款ID:" + loan.getId()
											+ "  投资id:" + invest.getId());
							log.debug("易宝冻结的资金："
									+ (invest.getMoney() - userCoupon.getCoupon()
											.getMoney()));
						} else {
							ubs.freezeMoney(invest.getUser().getId(),
									invest.getMoney(),
									OperatorInfo.INVEST_SUCCESS,
									"投资成功：冻结金额。借款ID:" + loan.getId()
											+ "  投资id:" + invest.getId());
						}
						ht.update(invest);
					} catch (NoMatchingObjectsException e) {
						// throw new RuntimeException(e);
						e.printStackTrace();
					} catch (InsufficientBalance e) {
						// throw new RuntimeException(e);
						e.printStackTrace();
					}
				}

			} else {// 易宝处理失败

				to.setResponseTime(new Date());
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.update(to);
			}
		}

	}

	@SuppressWarnings({ "unchecked", "deprecation" })
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationS2SCallback(ServletRequest request,
			ServletResponse response) {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		String respXML = request.getParameter("notify");// 响应的参数 为xml格式
		log.debug("易宝 投资不冻结页面回调报文respXML：" + respXML);
		String sign = request.getParameter("sign");// 签名
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		if (flag) {// 验签成功

			Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.INVEST, "");// 请求流水号
			TrusteeshipOperation to = trusteeshipOperationBO.get(YeePayConstants.OperationType.INVEST, requestNo, "yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(), LockMode.UPGRADE);
			if (TrusteeshipConstants.Status.PASSED.equals(to.getStatus())) {// 状态是已发送，则不做后续处理
				return;
			}

			String description = resultMap.get("description");
			String code = resultMap.get("code");
			Invest invest = ht.get(Invest.class, requestNo);
			if ("1".equals(code)) {// 易宝处理成功

				to.setResponseData(respXML);
				to.setResponseTime(new Date());
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				ht.update(to);

				Loan loan = invest.getLoan();
				ht.evict(loan);
				loan = ht.get(Loan.class, invest.getLoan().getId(), LockMode.UPGRADE);
				double remainMoney = 0.0;
				try {
					remainMoney = loanCalculator.calculateMoneyNeedRaised(loan.getId());
				} catch (NoMatchingObjectsException e1) {
					e1.printStackTrace();
				}// 尚未募集的金额
				if (invest.getMoney() > remainMoney || !LoanStatus.RAISING.equals(loan.getStatus())) {// 投资金额大于尚未募集的金额或者标的状态不是“投资中”
					// 调用易宝转账取消端口
					boolean result = yeepayCpTransacionOperation
							.transactionComform(
									YeePayConstants.RequestNoPre.TRANSFER
											+ requestNo, "CANCEL");
					if (!result) {
						log.debug("投资【" + invest.getId() + "】易宝转账取消失败");
						throw new YeePayOperationException("投资易宝转账取消失败");
					}
				} else if (invest.getStatus().equals(
						InvestConstants.InvestStatus.CANCEL)) {// 可以投资

					try {
						invest.setStatus(InvestConstants.InvestStatus.BID_SUCCESS);
						loanService.dealRaiseComplete(loan.getId());
						// 是否使用红包
						if (invest.getUserCoupon() != null) {
							// 设置红包状态
							UserCoupon userCoupon = invest.getUserCoupon();
							ht.evict(userCoupon);
							userCoupon = ht.get(UserCoupon.class, invest
									.getUserCoupon().getId(), LockMode.UPGRADE);
							if(CouponConstants.UserCouponStatus.USED.equals(userCoupon.getStatus())){//红包已使用
								log.info("投资【" + invest.getId() + "】红包已使用，红包："+userCoupon.getId());
								boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.INVEST + requestNo, "CANCEL");
								if (!result) {
									log.debug("投资【" + invest.getId() + "】易宝转账取消失败");
								}
								throw new RuntimeException("红包已使用");
							}
							userCoupon
									.setStatus(CouponConstants.UserCouponStatus.USED);
							userCoupon.setUsedTime(new Date());
							ht.update(userCoupon);
							
							ubs.freezeMoney(invest.getUser().getId(),
									invest.getMoney() - userCoupon.getCoupon().getMoney(),
									OperatorInfo.INVEST_SUCCESS,
									"投资成功：冻结金额。借款ID:" + loan.getId()
											+ "  投资id:" + invest.getId());
						} else {
							ubs.freezeMoney(invest.getUser().getId(),
									invest.getMoney(),
									OperatorInfo.INVEST_SUCCESS,
									"投资成功：冻结金额。借款ID:" + loan.getId()
											+ "  投资id:" + invest.getId());
						}
						ht.update(invest);
						

					} catch (NoMatchingObjectsException e) {
						throw new RuntimeException(e);
					} catch (InsufficientBalance e) {
						throw new RuntimeException(e);
					}
				}

			} else {// 易宝处理失败

				to.setResponseTime(new Date());
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.update(to);
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
