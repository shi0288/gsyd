package com.esoft.yeepay.query.controller;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.core.annotations.ScopeType;
import com.esoft.yeepay.query.service.impl.YeePayAccountInfoOperation;
import com.esoft.yeepay.query.service.impl.YeePayQueryOperation;

@Component
@Scope(ScopeType.VIEW)
public class YeePayQueryHome {
	@Resource
	YeePayQueryOperation  yeePayQueryOperation;
	@Resource
	YeePayAccountInfoOperation  yeePayAccountInfoOperation;
	
	private String platformUserNo;
	private String orderId;
	private String orderType;

	public void query(){
		yeePayQueryOperation.handleSendedOperation(orderId, orderType);
	}
	public void queryAccountInfo(){
		yeePayAccountInfoOperation.handleSendedOperation(platformUserNo);
	}
	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getOrderType() {
		return orderType;
	}

	public void setOrderType(String orderType) {
		this.orderType = orderType;
	}

	public String getPlatformUserNo() {
		return platformUserNo;
	}

	public void setPlatformUserNo(String platformUserNo) {
		this.platformUserNo = platformUserNo;
	}
}
