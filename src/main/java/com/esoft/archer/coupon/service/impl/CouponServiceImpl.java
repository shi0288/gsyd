package com.esoft.archer.coupon.service.impl;

import org.springframework.stereotype.Service;

import com.esoft.archer.coupon.service.CouponService;

@Service
public class CouponServiceImpl implements CouponService{

	@Override
	public void disable(String couponId) {
		throw new RuntimeException("you must override this method!");
	}

	@Override
	public void enable(String couponId) {
		throw new RuntimeException("you must override this method!");		
	}

}
