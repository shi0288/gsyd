package com.esoft.yeepay.withdraw.service.impl;

import java.util.Date;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.model.WithdrawCash;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.archer.user.service.impl.WithdrawCashServiceImpl;
import com.esoft.core.annotations.Logger;

public class YeePayWithdrawCashServiceImpl extends WithdrawCashServiceImpl {
	@Logger
	private static Log log;

	@Resource
	HibernateTemplate ht;

	@Resource
	private FeeConfigBO feeConfigBO;

	@Resource
	UserBillBO userBillBO;

	@Resource
	SystemBillService sbs;

	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void passWithdrawCashRecheck(WithdrawCash withdrawCash) {
		// 从冻结金额中取，系统账户也要记录
		WithdrawCash wdc = ht.get(WithdrawCash.class, withdrawCash.getId());
		ht.evict(wdc);
		wdc = ht.get(WithdrawCash.class, withdrawCash.getId(), LockMode.UPGRADE);
		wdc.setRecheckTime(new Date());
		wdc.setStatus(UserConstants.WithdrawStatus.SUCCESS);
		ht.merge(wdc);
		try {
			userBillBO.transferOutFromFrozen(wdc.getUser().getId(),
					wdc.getMoney(), OperatorInfo.WITHDRAW_SUCCESS,
					"提现申请通过，取出冻结金额, 提现ID:" + withdrawCash.getId());
			userBillBO.transferOutFromFrozen(wdc.getUser().getId(),
					wdc.getFee(), OperatorInfo.WITHDRAW_SUCCESS,
					"提现申请通过，取出冻结手续费, 提现ID:" + withdrawCash.getId());
		} catch (InsufficientBalance e) {
			throw new RuntimeException(e);
		}

		if (log.isInfoEnabled())
			log.info("提现审核复核通过，提现编号：" + wdc.getId());
	}

}
