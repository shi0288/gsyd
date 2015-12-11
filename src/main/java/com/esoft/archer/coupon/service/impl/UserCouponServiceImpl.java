package com.esoft.archer.coupon.service.impl;

import java.util.Date;

import org.springframework.stereotype.Service;

import com.esoft.archer.coupon.service.UserCouponService;

/**
 * Company: jdp2p <br/>
 * Copyright: Copyright (c)2013 <br/>
 * Description:
 * 
 * @author: wangzhi
 * @version: 1.0 Create at: 2014-7-19 下午2:01:16
 * 
 *           Modification History: <br/>
 *           Date Author Version Description
 *           ------------------------------------------------------------------
 *           2014-7-19 wangzhi 1.0
 */
@Service("userCouponService")
public class UserCouponServiceImpl implements UserCouponService {

	public void giveUserCoupon(String userId, String couponId,
			int periodOfValidity, String description) {
		throw new RuntimeException("you must override this method!");
	}

	public void giveUserCoupon(String userId, String couponId, Date deadline,
			String description) {
		throw new RuntimeException("you must override this method!");
	}

	public void useUserCoupon(String userCouponId) {
		throw new RuntimeException("you must override this method!");
	}

	public void unuseUserCoupon(String userCouponId) {
		throw new RuntimeException("you must override this method!");
	}

	@Override
	public void disable(String userCouponId) {
		throw new RuntimeException("you must override this method!");
	}

	@Override
	public void enable(String userCouponId) {
		throw new RuntimeException("you must override this method!");
	}

	@Override
	public void exceedTimeLimit(String userCouponId) {
		throw new RuntimeException("you must override this method!");
	}

	@Override
	public void checkExceedTimeLimit() {
		throw new RuntimeException("you must override this method!");
	}

}
