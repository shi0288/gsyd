package com.esoft.yeepay.trusteeship.controller;

import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.hibernate.ObjectNotFoundException;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.config.service.ConfigService;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.yeepay.autoinvest.service.impl.YeePayAuthorizeAutoTransferOperation;
import com.esoft.yeepay.autoinvest.service.impl.YeePayAutoInvestOperation;
import com.esoft.yeepay.bankcard.service.impl.YeePayBindingBankCardOperation;
import com.esoft.yeepay.bankcard.service.impl.YeePayUnbindingBankCardOperation;
import com.esoft.yeepay.invest.service.impl.YeePayInvestNofreezeOperation;
import com.esoft.yeepay.invest.service.impl.YeePayInvestOperation;
import com.esoft.yeepay.invest.service.impl.YeePayInvestTransferNofreezeOperation;
import com.esoft.yeepay.invest.service.impl.YeePayInvestTransferOperation;
import com.esoft.yeepay.loan.service.impl.YeePayRecheckOperation;
import com.esoft.yeepay.recharge.service.impl.YeePayRechargeOperation;
import com.esoft.yeepay.repay.service.impl.YeePayAdvanceRepayOperation;
import com.esoft.yeepay.repay.service.impl.YeePayNormalRepayOperation;
import com.esoft.yeepay.repay.service.impl.YeePayOverdueRepayOperation;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.user.service.impl.YeePayCorpAccountOperation;
import com.esoft.yeepay.user.service.impl.YeePayUserOperation;
import com.esoft.yeepay.withdraw.service.impl.YeePayWithdrawNofreezeOperation;
import com.esoft.yeepay.withdraw.service.impl.YeePayWithdrawOperation;

@Component
@Scope(ScopeType.REQUEST)
public class TrusteeshipHome {

	@Resource
	YeePayUnbindingBankCardOperation yeePayUnbindingBankCardOperation;
	@Resource
	YeePayBindingBankCardOperation yeePayBindingBankCardOperation;
	@Resource
	YeePayInvestOperation yeePayInvestOperation;
	@Resource
	YeePayInvestNofreezeOperation investNofreezeOperation;
	@Resource
	YeePayRechargeOperation yeePayRechargeOperation;
	@Resource
	YeePayWithdrawOperation yeePayWithdrawOperation;
	@Resource
	YeePayWithdrawNofreezeOperation yeePayWithdrawNofreezeOperation;
	@Resource
	YeePayNormalRepayOperation yeePayNormalRepayOperation;

	@Resource
	YeePayAdvanceRepayOperation yeePayAdvanceRepayOperation;

	@Resource
	YeePayOverdueRepayOperation yeePayOverdueRepayOperation;

	@Resource
	YeePayUserOperation yeePayUserOperation;
	@Resource
	YeePayCorpAccountOperation yeePayCorpAccountOperation;

	@Resource
	YeePayInvestTransferNofreezeOperation yeePayInvestTransferNofreezeOperation;
	
	@Resource
	YeePayInvestTransferOperation yeePayInvestTransferOperation;
	
	
	@Resource
	YeePayRecheckOperation yeePayRecheckOperation;
	/**
	 * 开启自动投标
	 */
	@Resource
	private YeePayAuthorizeAutoTransferOperation yeePayAuthorizeAutoTransferOperation;

	/**
	 * 自动投标
	 */
	@Resource
	private YeePayAutoInvestOperation yeePayAutoInvestOperation;

	@Logger
	static Log log;

	private String operationType;
	@Resource
	ConfigService configService;
	
	public String handleWebReturn() {
		if (log.isInfoEnabled()) {
			log.info("web call back: " + this.operationType + "  "
					+ getRequestParameters(FacesUtil.getHttpServletRequest()));
		}
		String freezeMoney = "1";
		try{
			freezeMoney = configService.getConfigValue("freeze_money");
		}catch(ObjectNotFoundException e){}
		if (YeePayConstants.OperationType.CREATE_ACCOUNT
				.equals(this.operationType)) {
			return this.getInvestorPermission();
		} else if (YeePayConstants.OperationType.RECHARGE
				.equals(this.operationType)) {
			return this.recharge();
		} else if (YeePayConstants.OperationType.INVEST
				.equals(this.operationType)) {
			return this.investWeb();
		} else if (YeePayConstants.OperationType.REPAY
				.equals(this.operationType)) {
			this.repayWeb();
			return null;
		} else if (YeePayConstants.OperationType.WITHDRAW_CASH
				.equals(this.operationType)) {
			return this.withdrawCash();
		} else if (YeePayConstants.OperationType.BINDING_YEEPAY_BANKCARD
				.equals(this.operationType)) {
			this.bindingCard();
			return null;
		} else if (YeePayConstants.OperationType.UNBINDING_YEEPAY_BANKCARD
				.equals(this.operationType)) {
			this.unbindingCard();
			return null;
		} else if (YeePayConstants.OperationType.AUTHORIZE_AUTO_TRANSFER
				.equals(this.operationType)) {
			this.setAutoInvest();
			return null;
		} else if (YeePayConstants.OperationType.TRANSFER
				.equals(this.operationType)) {
			this.transfer();
			return null;
		} else if (YeePayConstants.OperationType.ADVANCE_REPAY
				.equals(this.operationType)) {
			this.advanceRepayWeb();
			return null;
		} else if (YeePayConstants.OperationType.OVERDUE_REPAY
				.equals(this.operationType)) {
			this.overdueWeb();
			return null;
		} else if (YeePayConstants.OperationType.RESERVE_REPAY
				.equals(this.operationType)) {
			// this.reserveWeb();
			return null;
		} else if(YeePayConstants.OperationType.ENTERPRISE_REGISTER
				.equals(this.operationType)){
			return this.openCorpAccount();
		}
		// FIXME:跳转到404页面
		return "404";
	}

	/**
	 * 注册页面开户
	 */
	public String handleWebReturnReg() {
		if (YeePayConstants.OperationType.CREATE_ACCOUNT
				.equals(this.operationType)) {
			// 开通账户
			return this.getInvestorPermissionReg();
		}

		// FIXME:跳转到404页面
		return "404";
	}

	private String bindingCard() {
		try {

			yeePayBindingBankCardOperation
					.receiveOperationPostCallback(FacesUtil
							.getHttpServletRequest());
			FacesUtil
					.addInfoMessage("您的银行卡绑定已经提交到易宝，请等待易宝审核，您可以到 账户管理-银行卡信息 查看审核状态！");
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
		}
		// FIXME:为什么直接return pretty:XXX,就不显示消息
		// return "/user/bankCardList";
		return null;
	}

	private String unbindingCard() {
		try {
			yeePayUnbindingBankCardOperation
					.receiveOperationPostCallback(FacesUtil
							.getHttpServletRequest());
			FacesUtil.addInfoMessage("解除绑定成功！");
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
		}
		// FIXME:为什么直接return pretty:XXX,就不显示消息
		// return "/user/bankCardList";
		return null;
	}

	private String bindingCardS2S() {
		yeePayBindingBankCardOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
		return "/user/bankCardList";
	}

	private String unbindingCardS2S() {
		yeePayUnbindingBankCardOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
		return "/user/bankCardList";
	}

	private String investWeb() {
		String freezeMoney = "1";
		try{
			freezeMoney = configService.getConfigValue("freeze_money");
		}catch(ObjectNotFoundException e){}
		try {
			FacesUtil.addInfoMessage("投资成功");
			if("0".equals(freezeMoney)){
				investNofreezeOperation.receiveOperationPostCallback(FacesUtil
						.getHttpServletRequest());
			}else{
				yeePayInvestOperation.receiveOperationPostCallback(FacesUtil
						.getHttpServletRequest());
			}
			FacesUtil.addMessagesOutOfJSFLifecycle(FacesUtil
					.getCurrentInstance());
			if (FacesUtil.isMobileRequest()) {
				return "pretty:mobile_user_invests";
			} else {
				return "pretty:user_invest_bidding";
			}
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
		}
		return null;
	}

	private void investS2S() {
		String freezeMoney = "1";
		try{
			freezeMoney = configService.getConfigValue("freeze_money");
		}catch(ObjectNotFoundException e){}
		if("0".equals(freezeMoney)){
			investNofreezeOperation.receiveOperationS2SCallback(
					FacesUtil.getHttpServletRequest(),
					FacesUtil.getHttpServletResponse());
		}else{
			yeePayInvestOperation.receiveOperationS2SCallback(
					FacesUtil.getHttpServletRequest(),
					FacesUtil.getHttpServletResponse());
		}
	}

	private void repayWeb() {
		try {
			yeePayNormalRepayOperation.receiveOperationPostCallback(FacesUtil
					.getHttpServletRequest());
			FacesUtil.addInfoMessage("还款成功！");
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
		}
	}

	private void advanceRepayWeb() {
		try {
			yeePayAdvanceRepayOperation.receiveOperationPostCallback(FacesUtil
					.getHttpServletRequest());
			FacesUtil.addInfoMessage("提前还款成功！");
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
		}
	}

	private void overdueWeb() {
		try {
			yeePayOverdueRepayOperation.receiveOperationPostCallback(FacesUtil
					.getHttpServletRequest());
			FacesUtil.addInfoMessage("还款成功！");
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
		}
	}

	/*
	 * private void reserveWeb() { try {
	 * trusteeshipReserveRepayService.receiveOperationPostCallback(FacesUtil
	 * .getHttpServletRequest()); FacesUtil.addInfoMessage("还款成功！"); } catch
	 * (TrusteeshipReturnException e) { log.debug(e);
	 * FacesUtil.addErrorMessage(e.getMessage()); } }
	 */
	// 设置自动投标
	private void setAutoInvest() {
		try {
			yeePayAuthorizeAutoTransferOperation
					.receiveOperationPostCallback(FacesUtil
							.getHttpServletRequest());
			FacesUtil.addInfoMessage("开启自动投标成功！");
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
		}
	}

	// 购买债权
	private String transfer() {
		String freezeMoney = "1";
		try{
			freezeMoney = configService.getConfigValue("freeze_money");
		}catch(ObjectNotFoundException e){}
		try {
			if("0".equals(freezeMoney)){
				yeePayInvestTransferNofreezeOperation.receiveOperationPostCallback(FacesUtil.getHttpServletRequest());
			}else{
				yeePayInvestTransferOperation
				.receiveOperationPostCallback(FacesUtil
						.getHttpServletRequest());
			}
			FacesUtil.addInfoMessage("购买债权成功！");
			return "pretty:userCenter";
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
			return "pretty:userCenter";
		}
	}

	public String withdrawCash() {
		HttpServletRequest httpServletRequest = FacesUtil
				.getHttpServletRequest();
		String resp = httpServletRequest.getParameter("resp");
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = Dom4jUtil.xmltoMap(resp);
		String code = resultMap.get("code");
		String description = resultMap.get("description");
		if ("1".equals(code)) {
			FacesUtil.addInfoMessage("提现成功！");
		} else {
			FacesUtil.addInfoMessage(description);
		}
		FacesUtil.addMessagesOutOfJSFLifecycle(FacesUtil.getCurrentInstance());
		if (FacesUtil.isMobileRequest()) {
			return "pretty:mobile_userBill";
		} else {
			return "pretty:myCashFlow";
		}
	}

	public void handleS2SWebReturn() {
		if (log.isInfoEnabled()) {
			log.info("S2S call back: " + this.operationType + "  "
					+ getRequestParameters(FacesUtil.getHttpServletRequest()));
		}
		if (YeePayConstants.OperationType.CREATE_ACCOUNT
				.equals(this.operationType)) {
			// 开通账户
			this.getInvestorPermissionS2S();
		} else if (YeePayConstants.OperationType.RECHARGE
				.equals(this.operationType)) {
			// notifyUrl调用此方法
			this.recharge_s2s();
		} else if (YeePayConstants.OperationType.INVEST
				.equals(this.operationType)) {
			this.investS2S();
		} else if (YeePayConstants.OperationType.REPAY
				.equals(this.operationType)) {
			this.repayS2S();
		} else if (YeePayConstants.OperationType.WITHDRAW_CASH
				.equals(this.operationType)) {
			this.withdrawCash_s2s();
		} else if (YeePayConstants.OperationType.BINDING_YEEPAY_BANKCARD
				.equals(this.operationType)) {
			this.bindingCardS2S();
		} else if (YeePayConstants.OperationType.UNBINDING_YEEPAY_BANKCARD
				.equals(this.operationType)) {
			this.unbindingCardS2S();
		} else if (YeePayConstants.OperationType.AUTO_INVEST
				.equals(this.operationType)) {
			this.autoInvestS2S();
		} else if (YeePayConstants.OperationType.TRANSFER
				.equals(this.operationType)) {
			this.transferS2S();
		} else if (YeePayConstants.OperationType.ADVANCE_REPAY
				.equals(this.operationType)) {
			this.advanceRepayS2S();
		} else if (YeePayConstants.OperationType.OVERDUE_REPAY
				.equals(this.operationType)) {
			this.overdueRepayS2S();
		} else if (YeePayConstants.OperationType.RESERVE_REPAY
				.equals(this.operationType)) {
			// this.reserveRepayS2S();
		}else if(YeePayConstants.OperationType.GIVE_MOENY_TO_BORROWER
				.equals(this.operationType)){
			this.recheckS2S();
		}else if(YeePayConstants.OperationType.ENTERPRISE_REGISTER.equals(this.operationType)){
			openCorpAccountS2S();
		}
		FacesUtil.getCurrentInstance().responseComplete();
	}

	/**
	 * 放款回调
	 */
	private void recheckS2S(){
		yeePayRecheckOperation.receiveOperationS2SCallback(FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
	}
	
	private void autoInvestS2S() {
		yeePayAutoInvestOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
	}

	private void repayS2S() {
		yeePayNormalRepayOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
	}

	private void advanceRepayS2S() {
		yeePayAdvanceRepayOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
	}

	private void overdueRepayS2S() {
		yeePayOverdueRepayOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
	}

	/*
	 * private void reserveRepayS2S() {
	 * trusteeshipReserveRepayService.receiveOperationS2SCallback(
	 * FacesUtil.getHttpServletRequest(), FacesUtil.getHttpServletResponse()); }
	 */
	private void withdrawCash_s2s() {
		HttpServletRequest httpServletRequest = FacesUtil
				.getHttpServletRequest();
		HttpServletResponse httpServletResponse = FacesUtil
				.getHttpServletResponse();
		String freezeMoney = "1";
		try{
			freezeMoney = configService.getConfigValue("freeze_money");
		}catch(ObjectNotFoundException e){}
		if("0".equals(freezeMoney)){
			yeePayWithdrawNofreezeOperation.receiveOperationS2SCallback(httpServletRequest,
					httpServletResponse);
		}else{
			yeePayWithdrawOperation.receiveOperationS2SCallback(httpServletRequest,
					httpServletResponse);
		}
	}

	// 购买债权s2s
	private void transferS2S() {
		String freezeMoney = "1";
		try{
			freezeMoney = configService.getConfigValue("freeze_money");
		}catch(ObjectNotFoundException e){}
		if("0".equals(freezeMoney)){
			yeePayInvestTransferNofreezeOperation.receiveOperationS2SCallback(
					FacesUtil.getHttpServletRequest(),
					FacesUtil.getHttpServletResponse());
		}else{
			yeePayInvestTransferOperation.receiveOperationS2SCallback(
					FacesUtil.getHttpServletRequest(),
					FacesUtil.getHttpServletResponse());
		}
	}

	/**
	 * 从账户中心进行易宝开户
	 */
	public String getInvestorPermission() {

		log.info("从账户中心进行易宝开户");
		try {
			yeePayUserOperation.receiveOperationPostCallback(FacesUtil
					.getHttpServletRequest());
			FacesUtil.addInfoMessage("实名认证成功");
			FacesUtil.addMessagesOutOfJSFLifecycle(FacesUtil
					.getCurrentInstance());
			if (FacesUtil.isMobileRequest()) {
				return "pretty:mobile_user_center";
			} else {
				return "pretty:userCenter";
			}
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
			return "/user/createIpsAcctFail";
		}
	}
	public String openCorpAccount(){
		try {
			yeePayCorpAccountOperation.receiveOperationPostCallback(FacesUtil.getHttpServletRequest());
		FacesUtil.addInfoMessage("企业开户成功");
		FacesUtil.addMessagesOutOfJSFLifecycle(FacesUtil.getCurrentInstance());
		return "pretty:userCenter";
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
			return "/user/createIpsAcctFail";
		}
	}
	/**
	 * 从注册页面进行易宝开户
	 */
	private String getInvestorPermissionReg() {
		try {
			yeePayUserOperation.receiveOperationPostCallback(FacesUtil
					.getHttpServletRequest());
			return "/regSuccess";
		} catch (TrusteeshipReturnException e) {
			log.debug(e);
			FacesUtil.addErrorMessage(e.getMessage());
			return "/user/createIpsAcctFail";
		}
	}

	private void getInvestorPermissionS2S() {
		yeePayUserOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
	}
	
	
	private void openCorpAccountS2S() {
		yeePayCorpAccountOperation.receiveOperationS2SCallback(
				FacesUtil.getHttpServletRequest(),
				FacesUtil.getHttpServletResponse());
	}

	public String recharge() {
		try {
			yeePayRechargeOperation.receiveOperationPostCallback(FacesUtil
					.getHttpServletRequest());
			FacesUtil.addInfoMessage("充值成功");
		} catch (TrusteeshipReturnException e) {
			FacesUtil.addInfoMessage("充值失败：" + e.getMessage());
		}
		FacesUtil.addMessagesOutOfJSFLifecycle(FacesUtil.getCurrentInstance());
		if (FacesUtil.isMobileRequest()) {
			return "pretty:mobile_userBill";
		} else {
			return "pretty:myCashFlow";
		}

	}

	private void recharge_s2s() {
		HttpServletRequest httpServletRequest = FacesUtil
				.getHttpServletRequest();
		HttpServletResponse httpServletResponse = FacesUtil
				.getHttpServletResponse();

		yeePayRechargeOperation.receiveOperationS2SCallback(httpServletRequest,
				httpServletResponse);

	}

	public String getOperationType() {
		return operationType;
	}

	public void setOperationType(String operationType) {
		this.operationType = operationType;
	}

	private String getRequestParameters(ServletRequest request) {
		StringBuffer sb = new StringBuffer();
		Map map = request.getParameterMap();
		for (Object str : map.keySet()) {
			sb.append(str);
			sb.append(":");
			sb.append(request.getParameter(str.toString()));
			sb.append("  ");
		}
		return sb.toString();
	}
	
}
