package com.esoft.yeepay.loan.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.coupon.model.Coupon;
import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.event.InvestSuccessEvent;
import com.esoft.jdp2p.loan.exception.BorrowedMoneyTooLittle;
import com.esoft.jdp2p.loan.exception.ExistWaitAffirmInvests;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.LoanService;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.cptransaction.YeepayCpTransacionOperation;
import com.esoft.yeepay.platformtransfer.model.YeePayPlatformTransfer;
import com.esoft.yeepay.platformtransfer.service.impl.YeePayPlatformTransferOperation;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;

@Service("yeePayRecheckOperation")
public class YeePayRecheckOperation{
	@Resource(name="yeePayLoanService")
	LoanService loanService;
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;
	@Logger
	static Log log;
	@Resource
	HibernateTemplate ht;
	@Resource
	YeepayCpTransacionOperation yeepayCpTransacionOperation;
	@Resource
	SystemBillService sbs;
	@Resource
	ConfigService configService;
	
	/** 平台转账方法 */
	@Resource
	YeePayPlatformTransferOperation yeePayPlatformTransferOperation;
	
	/** 平台转账需要的实体类 */
	YeePayPlatformTransfer yeePayPlatformTransfer;
	
	/** 获得平台管理员的id */
	@Resource
	private LoginUserInfo loginUserInfo;
	
	/** 红包对象 */
	private Coupon coupon;
	
	@Autowired
	private ApplicationContext applicationContext;
	
	@Transactional(rollbackFor = Exception.class)
	public String createOperation(Loan instance) throws ExistWaitAffirmInvests, InsufficientBalance {
		
		Loan loan = ht.get(Loan.class, instance.getId());
		ht.evict(loan);
		loan = ht.get(Loan.class, instance.getId());
		//List<Invest> invests = loan.getInvests();
		List<Invest> invests = loanService.getSuccessfulInvests(loan.getId());
		boolean result = true;//判定是否有未放款成功的投资
		//1.判定该标的的状态
		if (loan.getStatus().equals(LoanConstants.LoanStatus.RECHECK)) {
			//判断平台的钱是否够支付红包
			//所有投资的红包总额
			double allCoupon = 0D;
			for(Invest invest : invests){
				if(invest.getUserCoupon() != null){
					allCoupon = allCoupon + invest.getUserCoupon().getCoupon().getMoney();
				}
			}
			//平台余额
			double platformBalance = 0D;
			platformBalance = sbs.getBalance();
			log.debug("红包总额:"+ allCoupon);
			log.debug("平台余额:" + platformBalance);
			if(platformBalance < allCoupon){
				log.debug("平台余额小于红包总额");
				throw new InsufficientBalance();
			}
			
			//2.易宝放款确认
			boolean flag =false;
			for(Invest invest : invests){
				log.debug("放款转账授权订单************************************"+invest.getId());
				//判断是否是自动投资，自动投资和普通投资的账单流水号前缀不同，普通投标为03，自动投标为13
				if(invest.getIsAutoInvest()){
					flag = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.AUTO_INVEST+invest.getId(), "CONFIRM");
				}else{
					flag = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.INVEST+invest.getId(), "CONFIRM");
				}
				if(!flag){
					log.debug("转账授权订单【"+invest.getId()+"】放款确认失败");
					result = false;
				}
				//判断是否有红包。若有红包，红包的钱平台进行转账
				if(invest.getUserCoupon() != null){
					yeePayPlatformTransfer = new YeePayPlatformTransfer();
					//借款人
					yeePayPlatformTransfer.setUsername(loan.getUser().getId());
					log.debug("借款人的id" + loan.getUser().getId());
					
					//平台账户
					String loginUserId = loginUserInfo.getLoginUserId();
					yeePayPlatformTransfer.setOperationUsername(loginUserId);
					log.debug("平台管理员的id" + loginUserId);
					
					//转账时间
					yeePayPlatformTransfer.setTime(new Date());
					//红包金额
					coupon = ht.get(Coupon.class, invest.getUserCoupon().getCoupon().getId());
					yeePayPlatformTransfer.setActualMoney(coupon.getMoney());
					log.debug("投资金额" + invest.getMoney());
					log.debug("红包金额" + coupon.getMoney());
					
					try {
						yeePayPlatformTransferOperation.sendPlatformTransfer(yeePayPlatformTransfer);
					} catch (YeePayOperationException e) {
						FacesUtil.addErrorMessage(e.getMessage());
					}
				}
				log.debug("========送红包开始");
				String config = configService.getConfigValue("invest_success_sum_curpon");
				Double configMoney = Double.valueOf(config);
				log.debug(config+"====="+configMoney);
				Double allMoney = (Double)ht.iterate("select sum(invest.money) from Invest invest where invest.user.id = ? and (invest.status = ? or invest.status = ? or invest.status = ?)", invest.getUser().getId(), InvestConstants.InvestStatus.COMPLETE, InvestConstants.InvestStatus.REPAYING, InvestConstants.InvestStatus.OVERDUE).next();
				if(null == allMoney){
					allMoney = 0D;
				}
				Double totalMoney = invest.getInvestMoney() + allMoney;
				Long integer = (Long)ht.iterate("select sum(1) from UserCoupon userCoupon where userCoupon.user.id = ? and userCoupon.coupon.id = ?", invest.getUser().getId(), "investSuccessRedPackage").next();
				if(integer == null){
					integer = 0L;
				}
				log.debug(allMoney+"====="+integer+"===="+totalMoney+"===="+invest.getInvestMoney());
				if((integer == 0) && (totalMoney >= configMoney)){
					log.debug("送红包中");
					applicationContext.publishEvent(new InvestSuccessEvent(invest.getUser()));
				}
			log.debug("送红包结束");
			}
			log.debug("放款result************************************"+result);
			//3.p2p平台放款
			if(result){
				try {
					loanService.giveMoneyToBorrower(loan.getId());
					log.debug("借款标的号【"+loan.getId()+"】放款成功");
				} catch (BorrowedMoneyTooLittle e) {
					log.debug(e.getMessage());
					e.printStackTrace();
				}
			}
			
		}
		return null;
	}
	
	
	
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationS2SCallback(ServletRequest request,ServletResponse response) {
		
		String notifyXML = request.getParameter("notify");
		String sign = request.getParameter("sign");
		
		boolean flag = CFCASignUtil.isVerifySign(notifyXML, sign);
		if(!flag){//验签未通过
			log.debug("放款S2S回调通知时，验签失败，notifyXML："+notifyXML);
			return;
		}else{
			log.debug("放款S2S回调通知时，验签成功，notifyXML："+notifyXML);
			try {//易宝对接收的请求已做处理
				response.getWriter().print("SUCCESS");
				FacesUtil.getCurrentInstance().responseComplete();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = Dom4jUtil.xmltoMap(notifyXML);
		String loanId = resultMap.get("orderNo");
		String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.GIVE_MOENY_TO_BORROWER, "");
		TrusteeshipOperation to = trusteeshipOperationBO.get(
				YeePayConstants.OperationType.GIVE_MOENY_TO_BORROWER,
				requestNo, "yeepay");
		String toId = to.getId();
		TrusteeshipOperation to1 = ht.load(TrusteeshipOperation.class, toId);
		ht.evict(to1);
		to1 =  ht.load(TrusteeshipOperation.class, toId,LockMode.UPGRADE);
		
		
		if(TrusteeshipConstants.Status.SENDED.equals(to1.getStatus())){
			
			String code = resultMap.get("code");
			
			if("1".equals(code)){
				to1.setStatus(TrusteeshipConstants.Status.PASSED);
				ht.update(to1);
				
				Loan loan = ht.get(Loan.class, loanId);
				ht.evict(loan);
				loan = ht.get(Loan.class, loanId);	
				
				if(LoanConstants.LoanStatus.RECHECK.equals(loan.getStatus())){
					try {
						loanService.giveMoneyToBorrower(loanId);
						log.debug("标的"+loanId+"放款成功");
					} catch (BorrowedMoneyTooLittle e) {
						log.debug("标的"+loanId+"放款失败");
						log.debug(e.getMessage());
						e.printStackTrace();
					} catch (ExistWaitAffirmInvests e) {
						log.debug("标的"+loanId+"放款失败");
						log.debug(e.getMessage());
						e.printStackTrace();
					} 
				}
			}else{
				log.debug("标的"+loanId+"异常返回码code="+code+",返回信息："+resultMap.get("message"));
				to1.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.update(to1);
			}
	
		}
		
	}

}
