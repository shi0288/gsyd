package com.esoft.yeepay.invest.controller;

import java.io.IOException;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import com.esoft.archer.config.controller.ConfigHome;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.model.User;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.jdp2p.invest.controller.InvestHome;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.service.InvestService;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.repay.service.RepayService;
import com.esoft.yeepay.invest.service.impl.YeePayInvestNofreezeOperation;
import com.esoft.yeepay.invest.service.impl.YeePayInvestOperation;
import com.esoft.yeepay.invest.service.impl.YeePayInvestTransferNofreezeOperation;
import com.esoft.yeepay.invest.service.impl.YeePayInvestTransferOperation;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;

public class YeePayInvestHome extends InvestHome {
	@Resource
	private YeePayInvestOperation yeePayInvestOperation;
	@Resource
	private YeePayInvestNofreezeOperation yeePayInvestNofreezeOperation;

	@Resource
	private YeePayInvestTransferOperation yeePayInvestTransferOperation;
	@Resource
	private YeePayInvestTransferNofreezeOperation yeePayInvestTransferNofreezeOperation;

	@Resource
	private InvestService investService;

	@Resource
	private LoginUserInfo loginUserInfo;

	@Resource
	private RepayService repayService;
	
	@Resource
	private ConfigHome configHome;
	/**
	 * 投资
	 * 
	 * @return
	 */
	@Override
	public String save() {
		Loan loan = getBaseService().get(Loan.class,
				getInstance().getLoan().getId());
		if (loan.getUser().getId().equals(loginUserInfo.getLoginUserId())) {
			
			FacesUtil.addInfoMessage("你不能投自己的项目！");
			return null;
		}
		this.getInstance().setUser(new User(loginUserInfo.getLoginUserId()));
		this.getInstance().setIsAutoInvest(false);
		try {
			if("0".equals(configHome.getConfigValue("freeze_money"))){
				yeePayInvestNofreezeOperation.createOperation(getInstance(), FacesContext.getCurrentInstance());
			}else{
				yeePayInvestOperation.createOperation(getInstance(),
						FacesContext.getCurrentInstance());
			}
		} catch (YeePayOperationException e) {
			FacesUtil.addInfoMessage(e.getMessage());
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public String transfer() {
		
		if (loginUserInfo.getLoginUserId().equals(
				getInstance().getTransferApply().getInvest().getUser()
						.getId())) {
			FacesUtil.addErrorMessage("您不能购买自己的债权");
			return null;
		}
		
		Invest invest = this.getInstance();
		invest.setUser(new User(loginUserInfo.getLoginUserId()));
		try {
			if (loginUserInfo.getLoginUserId().equals(((Invest)getInstance()).getTransferApply().getInvest().getUser().getId()))
		      {
		        FacesUtil.addErrorMessage("您不能购买自己的债权");
		        return null;
		      }
			if("0".equals(configHome.getConfigValue("freeze_money"))){
				yeePayInvestTransferNofreezeOperation.createOperation(invest,
						FacesContext.getCurrentInstance());
			}else{
				yeePayInvestTransferOperation.createOperation(invest,
						FacesContext.getCurrentInstance());
			}
		} catch (YeePayOperationException e) {
			FacesUtil.addInfoMessage(e.getMessage());
			return null;
		}catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Class<Invest> getEntityClass() {
		return Invest.class;
	}
}
