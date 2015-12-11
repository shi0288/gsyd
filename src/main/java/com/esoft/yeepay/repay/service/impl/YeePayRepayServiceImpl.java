package com.esoft.yeepay.repay.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.archer.user.service.UserBillService;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.DateUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.LoanService;
import com.esoft.jdp2p.repay.model.InvestRepay;

public class YeePayRepayServiceImpl{
	@Logger
	static Log log;

	@Resource
	HibernateTemplate ht;
	
	@Resource
	UserBillService ubs;

	@Resource
	SystemBillService sbs;

	@Resource
	LoanService loanService;

	public void autoRepay() {

		if (log.isDebugEnabled()) {
			log.debug("autoRepay start");
		}
		List<InvestRepay> repays = ht.find("from InvestRepay repay where repay.status !='"
				+ LoanConstants.RepayStatus.COMPLETE + "'");
		for (InvestRepay repay : repays) {
			ht.lock(repay, LockMode.UPGRADE);
			Loan loan = repay.getInvest().getLoan();
			ht.lock(loan, LockMode.UPGRADE);
			if (repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING)
					&& repay.getRepayDay().before(new Date())) {
				// 到还款日了，自动扣款
				double balance = ubs.getBalance(repay.getInvest().getUser().getId());
				double payMoney = ArithUtil.add(repay.getCorpus(),
						repay.getInterest());
				if (payMoney <= balance) {
					// 余额充足，自动扣款
					// 正常还款
				/*	try {
						normalRepay(repay.getId());
						if (log.isDebugEnabled()) {
							log.debug("autoRepay normalRepay repayId:"
									+ repay.getId());
						}
					} catch (InsufficientBalance e) {
						throw new RuntimeException(e);
					} catch (NormalRepayException e) {
						throw new RuntimeException(e);
					}*/
				} else {
					// 账户余额不足，则逾期
					if (log.isDebugEnabled()) {
						log.debug("autoRepay InsufficientBalance overdue repayId:"
								+ repay.getId());
					}
					repay.setStatus(LoanConstants.RepayStatus.OVERDUE);
					loan.setStatus(LoanConstants.LoanStatus.OVERDUE);
					for (Invest iv : loan.getInvests()) {
						if (iv.getStatus().equals(
								InvestConstants.InvestStatus.REPAYING)) {
							iv.setStatus(InvestConstants.InvestStatus.OVERDUE);
							ht.update(iv);
						}
					}
					// FIXME:冻结用户，只允许还钱，其他都不能干。
					ht.update(repay);
					ht.update(loan);
				}
			} else if (repay.getStatus().equals(
					LoanConstants.RepayStatus.OVERDUE)) {
				if (log.isDebugEnabled()) {
					log.debug("autoRepay overdue repayId:" + repay.getId());
				}
				// 计算逾期罚息
			   //repay.setDefaultInterest(calculateOverdueSumByPeriod(repay.getId()));
				ht.update(repay);
			} else if (DateUtil.addYear(repay.getRepayDay(), 1).before(
					new Date())) {
				// 逾期一年以后，项目改为还账状态
				if (log.isDebugEnabled()) {
					log.debug("autoRepay badDebt repayId:" + repay.getId());
				}
				repay.setStatus(LoanConstants.RepayStatus.BAD_DEBT);
				loan.setStatus(LoanConstants.LoanStatus.BAD_DEBT);
				for (Invest iv : loan.getInvests()) {
					if (iv.getStatus().equals(
							InvestConstants.InvestStatus.OVERDUE)) {
						iv.setStatus(InvestConstants.InvestStatus.BAD_DEBT);
						ht.update(iv);
					}
				}
				ht.update(repay);
				ht.update(loan);
			}
		}
	}
}
