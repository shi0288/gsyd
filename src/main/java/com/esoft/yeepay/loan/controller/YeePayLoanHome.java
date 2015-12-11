package com.esoft.yeepay.loan.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.loan.controller.LoanHome;
import com.esoft.jdp2p.loan.exception.ExistWaitAffirmInvests;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.yeepay.loan.service.impl.YeePayCancelInvestOperation;
import com.esoft.yeepay.loan.service.impl.YeePayRecheckOperation;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;

@Component
@Scope(ScopeType.VIEW)
public class YeePayLoanHome extends LoanHome {
	@Resource
	private LoginUserInfo loginUserInfo;
	@Resource
	YeePayRecheckOperation yeePayRecheckOperation;
	@Resource
	YeePayCancelInvestOperation yeePayCancelInvestOperation;

	/**
	 * 放款
	 */
	@Override
	public String recheck() {
		Loan loan = getBaseService().get(Loan.class, getInstance().getId());
		List<Invest> invests = loan.getInvests();
		for (Invest invest : invests) {
			if (invest.getStatus().equals(InvestConstants.InvestStatus.WAIT_AFFIRM)) {
				FacesUtil.addInfoMessage("放款失败，存在等待第三方资金托管确认的投资");
				return null;
			}
		}
		try {
			yeePayRecheckOperation.createOperation(loan);
			FacesUtil.addInfoMessage("放款成功");
			return FacesUtil.redirect(loanListUrl);

		} catch (ExistWaitAffirmInvests e) {
			FacesUtil.addInfoMessage("放款失败，存在等待第三方资金托管确认的投资。");
		} catch (YeePayOperationException e) {
			FacesUtil.addInfoMessage(e.getMessage());
		} catch (InsufficientBalance e) {
			FacesUtil.addInfoMessage("平台余额小于红包总额,平台需要充值。");
		}
		return null;
	}

	@Override
	public String failByManager() {
		Long l = (Long) getBaseService()
				.find("select count(invest) from Invest invest where invest.loan.id=? and invest.status=?",
						getInstance().getId(), InvestStatus.WAIT_AFFIRM).get(0);
		if (l > 0) {
			FacesUtil.addInfoMessage("流标失败，存在等待第三方资金托管确认的投资。");
			return null;
		}
		yeePayCancelInvestOperation.createOperation(this.getInstance().getId(),
				loginUserInfo.getLoginUserId());
		FacesUtil.addInfoMessage("流标成功");
		return FacesUtil.redirect(loanListUrl);
	}

	@Override
	public Class<Loan> getEntityClass() {
		return Loan.class;
	}

}
