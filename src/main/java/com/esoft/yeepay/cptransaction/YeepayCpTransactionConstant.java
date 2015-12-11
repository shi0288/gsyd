package com.esoft.yeepay.cptransaction;

/**
 * 易宝转账授权业务类型
 * @author cm
 *
 */
public enum YeepayCpTransactionConstant {

	/**投标*/
	TENDER,
	/**还款*/
	REPAYMENT,
	/**债权转让*/
	CREDIT_ASSIGNMENT,
	/**转让*/
	TRANSFER,
	/**分润，仅在资金转账明细中使用*/
	COMMISSION
}
