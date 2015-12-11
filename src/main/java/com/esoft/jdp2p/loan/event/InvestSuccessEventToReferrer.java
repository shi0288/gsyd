package com.esoft.jdp2p.loan.event;

import org.springframework.context.ApplicationEvent;

import com.esoft.archer.coupon.model.IUserCouponEvent;
import com.esoft.archer.user.model.User;

/**
 * 投资成功后，给推荐人红包
 * @author Administrator
 *
 */
public class InvestSuccessEventToReferrer extends ApplicationEvent implements IUserCouponEvent {

	private User user;
	
	public InvestSuccessEventToReferrer(User user) {
		super(user);
		this.user = user;
	}

	@Override
	public String getCouponReceiverId() {
		return user.getId();
	}

}