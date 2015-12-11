package com.esoft.archer.risk.service;


/**
 * 系统收益账户service
 * @author Administrator
 *
 */
public interface SystemBillService {

	/**
	 * 获取最新一条数据
	 * @return
	 */
//	public SystemBill getLastestBill();
	
	/**
	 * 获取账户余额
	 * @return
	 */
	public double getBalance();

	/**
	 * 转出
	 * @param money 金额
	 * @param operatorType 操作类型
	 * @param operatorDetail 操作详情
	 * 
	 */
	public void transferOut(double money, String reason, String detail);
	
	/**
	 * 转入.
	 * @param money 金额
	 * @param operatorType 操作类型
	 * @param operatorDetail 操作详情
	 */
	public void transferInto(double money, String reason,
			String detail);
	
}
