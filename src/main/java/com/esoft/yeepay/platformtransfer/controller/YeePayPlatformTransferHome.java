package com.esoft.yeepay.platformtransfer.controller;

import java.util.Date;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.yeepay.platformtransfer.model.YeePayPlatformTransfer;
import com.esoft.yeepay.platformtransfer.service.impl.YeePayPlatformTransferOperation;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;

@Component
@Scope(ScopeType.VIEW)
public class YeePayPlatformTransferHome extends
		EntityHome<YeePayPlatformTransfer> {
	@Resource
	YeePayPlatformTransferOperation yeePayPlatformTransferOperation;

	@Resource
	private LoginUserInfo loginUserInfo;

	public void platformTransferTrusteeship() {
		try {
			YeePayPlatformTransfer platformTransfer = this.getInstance();
			String loginUserId = loginUserInfo.getLoginUserId();

			platformTransfer.setOperationUsername(loginUserId);
			platformTransfer.setTime(new Date());
			yeePayPlatformTransferOperation
					.sendPlatformTransfer(platformTransfer);
			FacesUtil.addInfoMessage("平台划账交易成功");
		} catch (YeePayOperationException e) {
			FacesUtil.addErrorMessage(e.getMessage());
		}
	}

}
