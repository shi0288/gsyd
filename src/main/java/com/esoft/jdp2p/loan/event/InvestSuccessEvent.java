package com.esoft.jdp2p.loan.event;

import org.springframework.context.ApplicationEvent;

import com.esoft.archer.coupon.model.IUserCouponEvent;
import com.esoft.archer.user.model.User;

public class InvestSuccessEvent extends ApplicationEvent implements IUserCouponEvent {

	private User user;
	
	public InvestSuccessEvent(User user) {
		super(user);
		this.user = user;
	}

	@Override
	public String getCouponReceiverId() {
		return user.getId();
	}

}