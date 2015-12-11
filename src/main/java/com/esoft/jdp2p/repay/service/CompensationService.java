package com.esoft.jdp2p.repay.service;

import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.jdp2p.repay.exception.AdvancedRepayException;
import com.esoft.jdp2p.repay.exception.NormalRepayException;
import com.esoft.jdp2p.repay.exception.OverdueRepayException;
import com.esoft.jdp2p.repay.model.LoanRepay;
import com.esoft.jdp2p.repay.model.RepayCompensation;

public interface CompensationService {
	/**
	 * 支付因代偿欠下的金额
	 * @param rc
	 * @throws InsufficientBalance 
	 */
	public void payForCompensation(RepayCompensation rc) throws InsufficientBalance;
	
	/**
	 * 正常还款
	 * 
	 * @param loanRepay 
	 *            还款
	 * @throws InsufficientBalance
	 * @throws NormalRepayException
	 */
	public void normalRepay(LoanRepay loanRepay, String repayerId) throws InsufficientBalance,
			NormalRepayException;	

	/**
	 * 逾期还款
	 * 
	 * @param repayId
	 *            还款id
	 * @throws InsufficientBalance
	 *             余额不足
	 * @throws OverdueRepayException 
	 */
	public void overdueRepay(String repayId,String repayerId) throws InsufficientBalance, OverdueRepayException;

}
