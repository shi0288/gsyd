package com.esoft.archer.coupon.model;

/**
 * 如果一个事件是要被送红包的，需要实现此接口
 * @author Administrator
 *
 */
public interface IUserCouponEvent {
	/**
	 * 获取接收红包的用户id
	 * @return
	 */
	public String getCouponReceiverId();
}
