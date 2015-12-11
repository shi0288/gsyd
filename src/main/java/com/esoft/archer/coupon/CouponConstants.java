package com.esoft.archer.coupon;



public class CouponConstants {
	/**
	 * 红包类型
	 * @author Administrator
	 *
	 */
	public static class Type{
		/**
		 * 现金
		 */
		public static final String CASH = "cash";
		/**
		 * 体验金
		 */
		public static final String EXPERIENCE = "experience";
		/**
		 * 优惠券
		 */
		public static final String DISCOUNT = "discount";
	}
	
	/**
	 * 用户红包状态
	 * @author Administrator
	 *
	 */
	public static class UserCouponStatus{
		/**
		 * 未使用
		 */
		public static final String UNUSED = "unused";
		/**
		 * 已使用
		 */
		public static final String USED = "used";
		/**
		 * 不可用
		 */
		public static final String DISABLE = "disable";
		/**
		 * 过期
		 */
		public static final String EXPIRE = "expire";
		
	}
	
	/**
	 * 红包状态
	 * @author Administrator
	 *
	 */
	public static class CouponStatus{
		/**
		 * 可用
		 */
		public static final String ENABLE = "enable";
		/**
		 * 不可用
		 */
		public static final String DISABLE = "disable";
	}
	
	
}
