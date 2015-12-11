package com.esoft.yeepay.autoinvest.controller;

import java.io.IOException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.controller.AutoInvestHome;
import com.esoft.jdp2p.invest.model.AutoInvest;
import com.esoft.jdp2p.invest.service.AutoInvestService;
import com.esoft.yeepay.autoinvest.service.impl.YeePayAuthorizeAutoTransferOperation;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;

public class YeePayAutoInvestHome extends AutoInvestHome {

	@Resource
	private LoginUserInfo loginUserInfo;

	@Resource
	private AutoInvestService autoInvestService;

	@Resource
	private HibernateTemplate ht;

	@Logger
	Log log;

	// 开启自动投标
	@Resource
	private YeePayAuthorizeAutoTransferOperation yeePayAuthorizeAutoTransferOperation;

	/**
	 * 获取自动投标权限
	 * 
	 * @return
	 */
	@Override
	public String saveAutoInvest() {
		AutoInvest ai = getInstance();
		String userId = loginUserInfo.getLoginUserId();
		ai.setUser(new User(userId));
		ai.setStatus(InvestConstants.AutoInvest.Status.ON);
		AutoInvest ai2 = ht.get(AutoInvest.class, ai.getUserId());
		if (ai2 != null) {
			autoInvestService.settingAutoInvest(getInstance());
		} else if (ai2 == null) {
			try {
				yeePayAuthorizeAutoTransferOperation.createOperation(ai,
						FacesContext.getCurrentInstance());
			} catch (YeePayOperationException e) {
				FacesUtil.addInfoMessage(e.getMessage());
			} catch (IOException e) {
				log.debug(e);
			}
		}
		this.clearInstance();
		return null;
	}

	@Override
	public Class<AutoInvest> getEntityClass() {
		return AutoInvest.class;
	}
}
