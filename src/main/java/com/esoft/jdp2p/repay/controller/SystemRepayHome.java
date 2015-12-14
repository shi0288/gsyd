package com.esoft.jdp2p.repay.controller;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.jdp2p.repay.exception.AdvancedRepayException;
import com.esoft.jdp2p.repay.exception.NormalRepayException;
import com.esoft.jdp2p.repay.exception.OverdueRepayException;
import com.esoft.jdp2p.repay.service.impl.RepayByAminServiceImpl;


@Component
@Scope(ScopeType.VIEW)
public class SystemRepayHome {
	
	@Resource
	RepayByAminServiceImpl repayByAminService;
	
	/**
	 * 系统代偿，正常还款
	 * @param loanId
	 */
	public String  normalRepay(String repayId){
		
		try {
			repayByAminService.normalRepayByAdmin(repayId);
			FacesUtil.addInfoMessage("还款成功！");
		} catch (NormalRepayException e) {
			e.printStackTrace();
			FacesUtil.addInfoMessage(e.getMessage());
		} catch (InsufficientBalance e) {
			e.printStackTrace();
			FacesUtil.addInfoMessage(e.getMessage());
		}
		return FacesUtil.redirect("/admin/loan/repaymentInfoList");
	}
	
	/**
	 * 系统代偿，逾期还款
	 * @param repay
	 * @return
	 */
	public String overdueRepay(String repayId){
		
		try {
			repayByAminService.overdueRepayByAdmin(repayId);
			FacesUtil.addInfoMessage("还款成功！");
		} catch (InsufficientBalance e) {
			e.printStackTrace();
			FacesUtil.addInfoMessage("账户余额不足");
		} catch (OverdueRepayException e) {
			e.printStackTrace();
			FacesUtil.addInfoMessage(e.getMessage());
		}
		return FacesUtil.redirect("/admin/loan/repaymentInfoList");
	}
	
	/**
	 * 系统代偿，提前还款
	 * @param repay
	 * @return
	 */
	public String advanceRepay(String loanId){

		try {
			repayByAminService.advanceRepayByAdmin(loanId);
			FacesUtil.addInfoMessage("还款成功！");
		} catch (AdvancedRepayException e) {
			e.printStackTrace();
			FacesUtil.addInfoMessage(e.getMessage());
			return null;
		} catch (InsufficientBalance e) {
			e.printStackTrace();
			FacesUtil.addInfoMessage("账户余额不足");
			return null;
		}
		
		return FacesUtil.redirect("/admin/loan/repaymentInfoList");
	}
	
}
