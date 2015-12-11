package com.esoft.yeepay.withdraw.controller;

import java.io.IOException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.config.controller.ConfigHome;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.controller.WithdrawHome;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.model.WithdrawCash;
import com.esoft.archer.user.service.WithdrawCashService;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.yeepay.withdraw.service.impl.YeePayWithdrawNofreezeOperation;
import com.esoft.yeepay.withdraw.service.impl.YeePayWithdrawOperation;
@Component
@Scope(ScopeType.VIEW)
public class YeePayWithdrawHome extends WithdrawHome  {
	@Resource
	WithdrawCashService wcs;
	
	@Resource
	UserBillBO userBillBO;
	
	@Resource
	LoginUserInfo loginUserInfo;
	
	@Resource
	YeePayWithdrawOperation yeePayWithdrawOperation;
	@Resource
	YeePayWithdrawNofreezeOperation nofreezeOperation;
	@Resource
	ConfigHome configHome;
	
	/**
	 * 提现
	 */
	@Override
	public String withdraw() {
	 try {  
		 	WithdrawCash wc = this.getInstance();
		    String userId=loginUserInfo.getLoginUserId();
		    wc.setUser(new User(userId));
		    if("0".equals(configHome.getConfigValue("freeze_money"))){
		    	double fee = wcs.calculateFee(wc.getMoney());
		    	if(fee+wc.getMoney()>userBillBO.getBalance(wc.getUser().getId())){
		    		throw new InsufficientBalance();
		    	}
		    	wc.setFee(fee);
		    	wc.setCashFine(0d);
		    	wc.setStatus(UserConstants.WithdrawStatus.WAIT_VERIFY);
				nofreezeOperation.createOperation(wc, FacesUtil.getCurrentInstance());
			}else{
				wcs.applyWithdrawCash(wc);
				yeePayWithdrawOperation.createOperation(wc, FacesContext.getCurrentInstance());
			}
		 } catch (InsufficientBalance e) {
			FacesUtil.addErrorMessage("余额不足！");
			return null ;
		 } catch (IOException e) {
			FacesUtil.addErrorMessage("提现出错！");
		  e.printStackTrace();
	   }
	   return null;
	}

	@Override
	public Class<WithdrawCash> getEntityClass() {
		return WithdrawCash.class;
	}
}
