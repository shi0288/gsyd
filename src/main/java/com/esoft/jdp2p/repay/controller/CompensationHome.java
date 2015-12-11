package com.esoft.jdp2p.repay.controller;

import java.util.Date;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.risk.FeeConfigConstants.FeePoint;
import com.esoft.archer.risk.FeeConfigConstants.FeeType;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.DateUtil;
import com.esoft.jdp2p.repay.model.RepayCompensation;
import com.esoft.jdp2p.repay.service.CompensationService;

@Component
@Scope(ScopeType.VIEW)
public class CompensationHome extends EntityQuery<RepayCompensation> {

	@Resource
	FeeConfigBO feeConfigBO;

	@Resource
	CompensationService compensationService;

	/**
	 * 支付因代偿欠下的金额
	 */
	public void payForCompensation(RepayCompensation rc) {
		try {
			compensationService.payForCompensation(rc);
			FacesUtil.addInfoMessage("还款成功！");
		} catch (InsufficientBalance e) {
			FacesUtil.addErrorMessage("余额不足！");
		}
	}

	/**
	 * 计算应该给系统的罚息
	 * 
	 * @param rc
	 * @return
	 */
	public double getFeeToSystem(RepayCompensation rc) {
		// 应还本金和利息
		double money = ArithUtil.add(rc.getLoanRepay().getCorpus(), rc
				.getLoanRepay().getInterest());
		return feeConfigBO.getFee(FeePoint.OVERDUE_REPAY_SYSTEM,
				FeeType.PENALTY, null, null, ArithUtil.mul(money, DateUtil
						.getIntervalDays(rc.getLoanRepay().getRepayDay(),
								new Date())));
	}

	/**
	 * 计算应该给代偿者的罚息
	 * 
	 * @param rc
	 * @return
	 */
	public double getFeeToCompensator(RepayCompensation rc) {
		// 应还本金和利息
		double money = ArithUtil.add(rc.getLoanRepay().getCorpus(), rc
				.getLoanRepay().getInterest());
		return feeConfigBO.getFee(FeePoint.OVERDUE_REPAY_INVESTOR,
				FeeType.PENALTY, null, null, ArithUtil.mul(money, DateUtil
						.getIntervalDays(rc.getLoanRepay().getRepayDay(),
								new Date())));
	}

}
