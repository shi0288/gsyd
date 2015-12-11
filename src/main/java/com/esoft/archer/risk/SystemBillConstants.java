package com.esoft.archer.risk;

public final class SystemBillConstants {
	public static class SystemOperatorInfo {
		/**
		 * 管理员操作(生日利率划账)
		 */
		public static final String ADMIN_OPERATION = "admin_operation";
	}

	public static class Type {
		/**
		 * 转入
		 */
		public static final String IN = "in";
		/**
		 * 转出
		 */
		public static final String OUT = "out";
		/**
		 * 转出(支付用户生日利率收益)
		 */
		public static final String OUT_FOR_USER_BIRTHDSRRATEMONEY = "out_for_user_birthday_rate_money";
	}
}
