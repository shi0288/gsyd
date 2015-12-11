package com.esoft.yeepay.user.controller;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import org.apache.commons.lang.StringUtils;

import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.controller.UserHome;
import com.esoft.archer.user.model.User;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.HashCrypt;
import com.esoft.yeepay.user.model.YeepayCorpInfo;
import com.esoft.yeepay.user.service.impl.YeePayCorpAccountOperation;
import com.esoft.yeepay.user.service.impl.YeePayUserOperation;

public class YeePayUserHome extends UserHome  {
	@Resource
    YeePayUserOperation yeePayUserOperation;
	@Resource
	YeePayCorpAccountOperation yeePayCorpAccountOperation;
	@Resource
	LoginUserInfo loginUserInfo;
	private YeepayCorpInfo yci;
	@Override
	public String getInvestorPermission() {
		if (StringUtils.equals(
				HashCrypt.getDigestHash(getInstance().getCashPassword()),
				getInstance().getPassword())) {
			FacesUtil.addErrorMessage("交易密码不能与登录密码相同");
			return null;
		}
		try {
			yeePayUserOperation.createOperation(this.getInstance(),FacesContext.getCurrentInstance());
			
		} catch (Exception e) {
			FacesUtil.addErrorMessage("实名认证失败");
			e.printStackTrace();
		}
		return null;
	}
	
	public String openCorpAccount(){
		try {
			yci.setId(yci.getUser().getId());
			yci.setSeq(yci.getSeq()+1);
			yeePayCorpAccountOperation.createOperation(yci, FacesContext.getCurrentInstance());
		} catch (Exception e) {
			FacesUtil.addErrorMessage("企业开户失败");
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Class<User> getEntityClass() {
		return User.class;
	}

	public YeepayCorpInfo getYci() {
		if(yci == null){
			yci = getBaseService().get(YeepayCorpInfo.class, loginUserInfo.getLoginUserId());
			if(yci == null){
				yci = new YeepayCorpInfo();
			}
		}
		return yci;
	}

	public void setYci(YeepayCorpInfo yci) {
		this.yci = yci;
	}
}
