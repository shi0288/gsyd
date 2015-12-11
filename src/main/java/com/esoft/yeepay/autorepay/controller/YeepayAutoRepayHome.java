package com.esoft.yeepay.autorepay.controller;

import java.io.IOException;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.yeepay.autorepay.model.AutoRepay;
import com.esoft.yeepay.autorepay.service.impl.YeepayAutoRepayServiceImpl;
import com.esoft.yeepay.autorepay.service.impl.YeepayCancelAutoRepayServiceImpl;

/**
 * 自动还款
 * @author lyz
 *
 */

@Component
@Scope(ScopeType.VIEW)
public class YeepayAutoRepayHome extends EntityHome<AutoRepay> {

	@Resource
	YeepayAutoRepayServiceImpl autoRepayService;
	
	@Resource
	YeepayCancelAutoRepayServiceImpl  cancelAutoRepayService;
	
	/**
	 * 自动还款授权
	 */
	public void toAuthorizeAutoRepayment(){
		
		try {
			autoRepayService.createOperation(getInstance(), FacesUtil.getCurrentInstance());
			FacesUtil.addInfoMessage("自动还款授权成功");
		} catch (IOException e) {
			e.printStackTrace();
			FacesUtil.addInfoMessage("自动还款授权失败");
		}
	}
	
	/**
	 * 取消自动还款授权
	 */
	public void cancelAuthorizeAutoRepayment(){
		
		try {
			cancelAutoRepayService.createOperation(getInstance(), FacesUtil.getCurrentInstance());
			FacesUtil.addInfoMessage("自动还款授权取消成功");
		} catch (IOException e) {
			FacesUtil.addInfoMessage("自动还款授权取消失败");
			e.printStackTrace();
		}
	}
	
	
	/**
	 * 自动转账
	 */
	public void autoTransaction(){
		
	}
}
