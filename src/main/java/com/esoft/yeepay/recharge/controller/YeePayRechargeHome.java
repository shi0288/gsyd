package com.esoft.yeepay.recharge.controller;

import java.io.IOException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import com.esoft.archer.user.controller.RechargeHome;
import com.esoft.yeepay.recharge.service.impl.YeePayRechargeOperation;

public class YeePayRechargeHome extends RechargeHome{
	@Resource
	YeePayRechargeOperation yeePayRechargeOperation;
	
	/**
	 * 充值
	 */
	@Override
	public void recharge() {
		
		this.getInstance().setRechargeWay("YeePay");
		try {
			yeePayRechargeOperation.createOperation(this.getInstance(),
					FacesContext.getCurrentInstance());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
}
