package com.esoft.yeepay.platformtransfer.service.impl;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.IdGenerator;
import com.esoft.yeepay.platformtransfer.model.YeePayPlatformTransfer;
import com.esoft.yeepay.platformtransfer.service.YeePayPlatformTransferService;

@Service("yeePayPlatformTransferService")
public class YeePayPlatformTransferServiceImpl implements YeePayPlatformTransferService  {
	@Logger
	static Log log;

	@Resource
	HibernateTemplate ht;

//	@Resource
//	UserBillService ubs;
//
//	@Resource
//	SystemBillService sbs;
    
	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void platformTransfer(YeePayPlatformTransfer platformTransfer)
			throws InsufficientBalance {
//		String username =platformTransfer.getUsername();
//		double actualMoney=platformTransfer.getActualMoney();
//		sbs.transferOut(actualMoney, "平台划账："+username,"金额"+actualMoney);
//		ubs.transferIntoBalance(username, actualMoney,OperatorInfo.ADMIN_OPERATION,"平台划账："+username+"金额"+actualMoney);
		platformTransfer.setId(IdGenerator.randomUUID());
		ht.save(platformTransfer);
	}
}
