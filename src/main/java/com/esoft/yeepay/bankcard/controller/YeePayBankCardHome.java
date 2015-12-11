package com.esoft.yeepay.bankcard.controller;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.bankcard.controller.BankCardHome;
import com.esoft.archer.bankcard.model.BankCard;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.RechargeService;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.yeepay.bankcard.service.impl.YeePayBindingBankCardOperation;
import com.esoft.yeepay.bankcard.service.impl.YeePayUnbindingBankCardOperation;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;

public class YeePayBankCardHome extends BankCardHome {

	@Resource
	private LoginUserInfo loginUserInfo;
	@Resource
	YeePayBindingBankCardOperation yeePayBindingBankCardOperation;
	@Resource
	YeePayUnbindingBankCardOperation yeePayUnbindingBankCardOperation;

	@Resource
	private RechargeService rechargeService;

	/**
	 * 绑定银行卡(资金托管)
	 * 
	 * @return
	 */
	@Transactional(readOnly = false)
	public void bindingCardTrusteeship() {
		HttpClient client = new HttpClient();
		// 创建一个post方法
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);

		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<platformUserNo>" + loginUserInfo.getLoginUserId()
				+ "</platformUserNo>");
		content.append("</request>");
		// 生成密文
		String sign = CFCASignUtil.sign(content.toString());

		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("sign", sign);
		postMethod.setParameter("service", "ACCOUNT_INFO");
		try {
			int statusCode = client.executeMethod(postMethod);
			byte[] responseBody = postMethod.getResponseBody();
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));

			String code = resultMap.get("code");
			String cardNo = resultMap.get("cardNo");
			String cardStatus = resultMap.get("cardStatus");
			String bank = resultMap.get("bank");

			if ("1".equals(code) && cardNo != null && !"".equals(cardNo)) {
				// 查询成功 处理查询记录
				User user = getBaseService().get(User.class,
						loginUserInfo.getLoginUserId());
				List<BankCard> bcs = getBaseService()
						.find("from BankCard bc where bc.user.id=? and bc.cardNo=? and bc.status=?",
								user.getId(), cardNo, cardStatus);
				if (bcs.size() == 0) {
					BankCard bc = new BankCard();
					bc.setCardNo(cardNo);
					bc.setBankNo(bank);
					bc.setStatus(cardStatus);
					bc.setId(IdGenerator.randomUUID());
					bc.setBank(rechargeService.getBankNameByNo(bc.getBankNo()));
					bc.setTime(new Date());
					bc.setUser(user);
					getBaseService().save(bc);
				}
				FacesUtil.addInfoMessage("绑卡成功");
				return;
			}
		} catch (HttpException e) {
		} catch (IOException e) {
		} finally {
			postMethod.releaseConnection();
		}

		// FIXME:是不是需要加判断
		User loginUser = getBaseService().get(User.class,
				loginUserInfo.getLoginUserId());
		getInstance().setUser(loginUser);
		try {
			yeePayBindingBankCardOperation.createOperation(getInstance(),
					FacesContext.getCurrentInstance());
		} catch (IOException e) {
			FacesUtil.addErrorMessage("绑定银行卡错误");
			e.printStackTrace();
		}
	}

	public void unbindingCardTrusteeship() {
		// FIXME:是不是需要加判断
		User loginUser = getBaseService().get(User.class,
				loginUserInfo.getLoginUserId());
		getInstance().setUser(loginUser);
		try {
			yeePayUnbindingBankCardOperation.createOperation(getInstance(),
					FacesContext.getCurrentInstance());
		} catch (IOException e) {
			FacesUtil.addErrorMessage("解绑银行卡错误");
			e.printStackTrace();
		}
	}

	@Override
	public Class<BankCard> getEntityClass() {
		return BankCard.class;
	}
}
