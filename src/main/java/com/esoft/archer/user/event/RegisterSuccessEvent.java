package com.esoft.archer.user.event;

import org.springframework.context.ApplicationEvent;

import com.esoft.archer.coupon.model.IUserCouponEvent;
import com.esoft.archer.user.model.User;

/**
 * 注册成功事件
 * 
 * @author Administrator
 * 
 */
public class RegisterSuccessEvent extends ApplicationEvent implements IUserCouponEvent {

	private User user;
	
	public RegisterSuccessEvent(User user) {
		super(user);
		this.user = user;
	}

	@Override
	public String getCouponReceiverId() {
		return user.getId();
	}

}
