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
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.bankcard.BankCardConstants;
import com.esoft.archer.bankcard.model.BankCard;
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
@Service("yeePayUnbindingBankCardOperation")
public class YeePayUnbindingBankCardOperation extends YeePayOperationServiceAbs<BankCard> {
	@Resource
	private HibernateTemplate ht;
	@Resource
	private TrusteeshipOperationBO trusteeshipOperationBO;
	@Logger
	Log log;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(BankCard bankCard,
			FacesContext fc) throws IOException {

		// String resquestNo = IdGenerator.randomUUID();
		String userId = bankCard.getUser().getId();
		String hql = "from BankCard bc where bc.user.id = ? and bc.status = ?";
		List l = ht.find(hql, new String[]{userId,"VERIFIED"});
		BankCard bc = null;
		if(l.size() > 0){
			bc = (BankCard) l.get(0);
		}else{
			FacesUtil.addErrorMessage("未找到银行卡信息。");
			return null;
		}
		
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='utf-8'?>");
		// 商户编号:商户在易宝唯一标识
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		// 商户平台会员标识:会员在商户平台唯一标识
		content.append("<platformUserNo>" + userId
				+ "</platformUserNo>");
		// 请求流水号 银行卡的 id
		content.append("<requestNo>"+ YeePayConstants.RequestNoPre.UNBINDING_YEEPAY_BANKCARD + bc.getId() + "</requestNo>");
		// 页面回跳URL
		content.append("<callbackUrl>"
				+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.UNBINDING_YEEPAY_BANKCARD
				+ "</callbackUrl>");
		// 服务器通知 URL:服务器通知 URL
		content.append("<notifyUrl>"
				+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.UNBINDING_YEEPAY_BANKCARD
				+ "</notifyUrl>");
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
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(bc.getId());
		to.setOperator(bc.getId());
		to.setRequestUrl(YeePayConstants.RequestUrl.UNBINDING_YEEPAY_BANKCARD);
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.UNBINDING_YEEPAY_BANKCARD);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
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
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.UNBINDING_YEEPAY_BANKCARD, "");
			String description = resultMap.get("description");
			TrusteeshipOperation to = trusteeshipOperationBO
					.get(YeePayConstants.OperationType.UNBINDING_YEEPAY_BANKCARD,
							requestNo, requestNo, "yeepay");
			if ("1".equals(code)) {
				BankCard bc = ht.get(BankCard.class, requestNo);
				if(bc != null){
					bc.setStatus(BankCardConstants.BankCardStatus.DELETED);
					ht.update(bc);
				}
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
				throw new TrusteeshipReturnException(code
						+ ":" + description);
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
		String cardNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.UNBINDING_YEEPAY_BANKCARD, "");
//		String cardNo = resultMap.get("cardNo");
//		String bank = resultMap.get("bank");
//		String cardStatus = resultMap.get("cardStatus");
//		String platformUserNo = resultMap.get("platformUserNo");
		boolean flag = CFCASignUtil.isVerifySign(notifyXML, sign);
		log.debug(notifyXML);
		if (flag) {
			BankCard bc = ht.get(BankCard.class, cardNo);
			if(bc != null){
				bc.setStatus(BankCardConstants.BankCardStatus.DELETED);
				ht.update(bc);
			}
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
