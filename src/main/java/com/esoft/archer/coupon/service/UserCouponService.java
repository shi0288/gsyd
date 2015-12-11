package com.esoft.archer.coupon.service;

import java.util.Date;

/**
 * Company: jdp2p <br/>
 * Copyright: Copyright (c)2013 <br/>
 * Description: 红包servcie
 * 
 * @author: wangzhi
 * @version: 1.0 Create at: 2014-7-16 下午2:27:23
 * 
 *           Modification History: <br/>
 *           Date Author Version Description
 *           ------------------------------------------------------------------
 *           2014-7-16 wangzhi 1.0
 */
public interface UserCouponService {
	/**
	 * 给用户发放红包
	 * 
	 * @param userId
	 *            用户id
	 * @param couponId
	 *            红包id
	 *            @param periodOfValidity 有效期（秒），如果为0，则为红包本身有效期
	 */
	public void giveUserCoupon(String userId, String couponId, int periodOfValidity, String description);

	/**
	 * 使用红包
	 * 
	 * @param userCouponId
	 *            用户红包id
	 */
	public void useUserCoupon(String userCouponId);
	
	/**
	 * 使用红包失败，把红包置为可用状态
	 * 
	 * @param userCouponId
	 *            用户红包id
	 */
	public void unuseUserCoupon(String userCouponId);

	/**
	 * 红包失效
	 * 
	 * @param userCouponId
	 *            用户红包id
	 */
	public void disable(String userCouponId);
	
	/**
	 * 红包过期
	 * @param userCouponId 用户红包id
	 */
	public void exceedTimeLimit(String userCouponId);

	/**
	 * 启用红包
	 * 
	 * @param userCouponId
	 *            用户红包id
	 */
	public void enable(String userCouponId);
	
	/**
	 * 处理所有的红包，如果过期了，则进行过期处理
	 * @param userCouponId 用户红包id
	 */
	public void checkExceedTimeLimit();

	/**
	 * 给用户发放红包
	 * 
	 * @param userId
	 *            用户id
	 * @param couponId
	 *            红包id
	 * @param deadline 到期日
	 */
	public void giveUserCoupon(String userId, String couponId, Date deadline,
			String description);

}
