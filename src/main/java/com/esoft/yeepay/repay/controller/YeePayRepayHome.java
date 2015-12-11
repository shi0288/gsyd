package com.esoft.yeepay.repay.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import com.esoft.archer.risk.FeeConfigConstants.FeePoint;
import com.esoft.archer.risk.FeeConfigConstants.FeeType;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.service.UserBillService;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.ArithUtil;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.repay.controller.RepayHome;
import com.esoft.jdp2p.repay.model.LoanRepay;
import com.esoft.jdp2p.repay.service.RepayService;
import com.esoft.yeepay.repay.service.impl.YeePayAdvanceRepayOperation;
import com.esoft.yeepay.repay.service.impl.YeePayNormalRepayOperation;
import com.esoft.yeepay.repay.service.impl.YeePayOverdueRepayOperation;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;

public class YeePayRepayHome extends RepayHome {

	@Resource
	LoginUserInfo loginUserInfo;

	@Resource
	RepayService repayService;

	@Resource
	UserBillService ubs;

	@Resource
	YeePayNormalRepayOperation yeePayNormalRepayOperation;

	@Resource
	YeePayAdvanceRepayOperation yeePayAdvanceRepayOperation;
	
	@Resource
	YeePayOverdueRepayOperation yeePayOverdueRepayOperation;
	
	@Resource
	FeeConfigBO feeConfigBO;

	/**
	 * 正常还款
	 * 
	 * @return
	 */
	@Override
	public void normalRepay(String repayId) {
		// 用户可用余额
		double balance = ubs.getBalance(loginUserInfo.getLoginUserId());
		LoanRepay repay = getBaseService().get(LoanRepay.class, repayId);

		if (balance < ArithUtil.add(repay.getCorpus(), repay.getInterest(),
				repay.getFee())) {
			FacesUtil.addErrorMessage("您的账户余额不足，请先进行充值然后再还款。");
			return;
		}
		if (!repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING)) {
			FacesUtil.addErrorMessage("还款：" + repay.getId() + "不处于正常还款状态。");
			return;
		}
		try {
			yeePayNormalRepayOperation.createOperation(repay,
					FacesContext.getCurrentInstance());
		} catch (IOException e) {
			e.printStackTrace();
		}catch (YeePayOperationException e) {
			FacesUtil.addErrorMessage(e.getMessage());
		}

	}

	@Override
	public void advanceRepay(String loanId) {
		Loan loan = getBaseService().get(Loan.class, loanId);
		// 查询当期还款是否已还清
		String repaysHql = "select lr from LoanRepay lr where lr.loan.id = ?";
		List<LoanRepay> repays = getBaseService().find(repaysHql, loanId);
		// 剩余所有本金
		double sumCorpus = 0D;
		// 手续费总额
		double feeAll = 0D;
		for (LoanRepay repay : repays) {
			if (repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING)) {
				// 在还款期，而且没还款
				if (repayService.isInRepayPeriod(repay.getRepayDay())) {
					// 有未完成的当期还款。
					FacesUtil.addErrorMessage("当期还款未完成");
					return;
				} else {
					sumCorpus = ArithUtil.add(sumCorpus, repay.getCorpus());
					feeAll = ArithUtil.add(feeAll, repay.getFee());
				}
			} else if (repay.getStatus().equals(
					LoanConstants.RepayStatus.BAD_DEBT)
					|| repay.getStatus().equals(
							LoanConstants.RepayStatus.OVERDUE)) {
				// 还款中存在逾期或者坏账
				FacesUtil.addErrorMessage("还款中存在逾期或者坏账");
				return;
			}
		}

		// 给投资人的罚金
		double feeToInvestor = feeConfigBO.getFee(
				FeePoint.ADVANCE_REPAY_INVESTOR, FeeType.PENALTY, null, null,
				sumCorpus);
		// 给系统的罚金
		double feeToSystem = feeConfigBO.getFee(FeePoint.ADVANCE_REPAY_SYSTEM,
				FeeType.PENALTY, null, null, sumCorpus);

		double sumPay = ArithUtil.add(feeToInvestor, feeToSystem, sumCorpus,
				feeAll);
		// 扣除还款金额+罚金
		// 用户可用余额
		double balance = ubs.getBalance(loginUserInfo.getLoginUserId());
		if (balance < sumPay) {
			FacesUtil.addErrorMessage("您的账户余额不足，请先进行充值然后再还款。");
			return;
		}
		try {
			yeePayAdvanceRepayOperation.createOperation(loan, FacesContext.getCurrentInstance());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (YeePayOperationException e) {
			FacesUtil.addErrorMessage(e.getMessage());
		}
	}
	@Override
	public void overdueRepay(String repayId){
		// 用户可用余额
		double balance = ubs.getBalance(loginUserInfo.getLoginUserId());
		LoanRepay repay = getBaseService().get(LoanRepay.class, repayId);

		if (balance < ArithUtil.add(repay.getCorpus(), repay.getInterest(),
				repay.getFee(),repay.getDefaultInterest())) {
			FacesUtil.addErrorMessage("您的账户余额不足，请先进行充值然后再还款。");
			return;
		}
		if (!repay.getStatus().equals(LoanConstants.RepayStatus.OVERDUE) &&!repay.getStatus().equals(LoanConstants.RepayStatus.BAD_DEBT)) {
			FacesUtil.addErrorMessage("还款：" + repay.getId() + "不处于逾期和坏账还款状态。");
			return;
		}
		try {
			yeePayOverdueRepayOperation.createOperation(repay,
					FacesContext.getCurrentInstance());
		} catch (IOException e) {
			e.printStackTrace();
		}catch (YeePayOperationException e) {
			FacesUtil.addErrorMessage(e.getMessage());
		}
	}

	
	
}
