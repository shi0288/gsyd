package com.esoft.yeepay.bankcard.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.bankcard.BankCardConstants;
import com.esoft.archer.bankcard.model.BankCard;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.RechargeService;
import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.MapUtil;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.service.impl.YeePayOperationServiceAbs;
@Service("yeePayBindingBankCardOperation")
public class YeePayBindingBankCardOperation extends YeePayOperationServiceAbs<BankCard> {
	@Resource
	private HibernateTemplate ht;
	@Resource
	private TrusteeshipOperationBO trusteeshipOperationBO;
	@Resource
	private RechargeService rechargeService;
	@Logger
	Log log;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(BankCard bankCard,
			FacesContext fc) throws IOException {

		// String resquestNo = IdGenerator.randomUUID();
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='utf-8'?>");
		// 商户编号:商户在易宝唯一标识
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		// 商户平台会员标识:会员在商户平台唯一标识
		content.append("<platformUserNo>" + bankCard.getUser().getId()
				+ "</platformUserNo>");
		// 请求流水号 银行卡的 id
		content.append("<requestNo>"+ YeePayConstants.RequestNoPre.BINDING_YEEPAY_BANKCARD + bankCard.getUser().getId()
				+ "</requestNo>");
		// 页面回跳URL
		content.append("<callbackUrl>"
				+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.BINDING_YEEPAY_BANKCARD
				+ "</callbackUrl>");
		// 服务器通知 URL:服务器通知 URL
		content.append("<notifyUrl>"
				+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.BINDING_YEEPAY_BANKCARD
				+ "</notifyUrl>");
		// 提现金额:如果传入此，将使用该金额提现且用户将不可更改
		content.append("</request>");

		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", content.toString());
		String sign = CFCASignUtil.sign(content.toString());
		params.put("sign", sign);
		log.debug(content.toString());
		log.debug(sign);

		// 保存本地
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(bankCard.getUser().getId());
		to.setMarkId(bankCard.getUser().getId());
		to.setOperator(bankCard.getUser().getId());
		to.setRequestUrl(YeePayConstants.RequestUrl.BINDING_YEEPAY_BANKCARD);
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.BINDING_YEEPAY_BANKCARD);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		ht.evict(bankCard);
		try {
			sendOperation(to.getId(),"utf-8", fc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		return null;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationPostCallback(ServletRequest request)
			throws TrusteeshipReturnException {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}

		String resp = request.getParameter("resp");
		String sign = request.getParameter("sign");
		log.debug(resp);
		log.debug(sign);
		boolean flag = CFCASignUtil.isVerifySign(resp, sign);
		log.debug(flag);
		if (flag) {
			// 处理账户开通成功
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(resp);
			String code = resultMap.get("code");
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.BINDING_YEEPAY_BANKCARD, "");
			String description = resultMap.get("description");
			TrusteeshipOperation to = trusteeshipOperationBO
					.get(YeePayConstants.OperationType.BINDING_YEEPAY_BANKCARD,
							requestNo, requestNo, "yeepay");
			if ("1".equals(code)) {
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				to.setResponseTime(new Date());
				to.setResponseData(resp);
				ht.update(to);
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseTime(new Date());
				to.setResponseData(description);
				ht.update(to);
				// 真实错误原因
				throw new RuntimeException(new TrusteeshipReturnException(code
						+ ":" + description));
			}
		}		
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationS2SCallback(ServletRequest request,
			ServletResponse response) {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		String notifyXML = request.getParameter("notify");
		String sign = request.getParameter("sign");
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = Dom4jUtil.xmltoMap(notifyXML);
		String cardNo = resultMap.get("cardNo");
		String bankNo = resultMap.get("bank");
		String cardStatus = resultMap.get("cardStatus");
		String platformUserNo = resultMap.get("platformUserNo");
		boolean flag = CFCASignUtil.isVerifySign(notifyXML, sign);
		log.debug(notifyXML);
		if (flag) {
			 User user=ht.get(User.class, platformUserNo);
			 ht.evict(user);
		     user=ht.get(User.class, platformUserNo,LockMode.UPGRADE);
		   List<BankCard> bankCardListbyLoginUser =ht.find(
			 "from BankCard where user.id = ? and status != ?", new String[]{user.getId(),BankCardConstants.BankCardStatus.DELETED});
		   BankCard bankCard=null;
		   if(bankCardListbyLoginUser == null || bankCardListbyLoginUser.size() < 1){
				bankCard = new BankCard();
				bankCard.setId(IdGenerator.randomUUID());
		    }else{
		    	bankCard=bankCardListbyLoginUser.get(0);
		    }
	        bankCard.setCardNo(cardNo);
			bankCard.setBankNo(bankNo);
			bankCard.setBank(rechargeService.getBankNameByNo(bankNo));
			bankCard.setStatus(cardStatus);
			bankCard.setTime(new Date());
			bankCard.setUser(user);
			ht.merge(bankCard);
			try {
				response.getWriter().print("SUCCESS");
				FacesUtil.getCurrentInstance().responseComplete();
			} catch (IOException e) {
				log.debug("trusteeshipBindingBancCard S2S response"
						+ e.getMessage());
				throw new RuntimeException(e.getMessage());
			}
		}
		
	}

}
