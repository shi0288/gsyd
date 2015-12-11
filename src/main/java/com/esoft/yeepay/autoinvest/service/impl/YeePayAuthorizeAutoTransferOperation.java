package com.esoft.yeepay.autoinvest.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.core.annotations.Logger;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.HtmlElementUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.model.AutoInvest;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.service.impl.YeePayOperationServiceAbs;
import com.thoughtworks.xstream.XStream;


@Service("yeePayAuthorizeAutoTransferOperation")
public class YeePayAuthorizeAutoTransferOperation extends
		YeePayOperationServiceAbs<AutoInvest> {

	@Resource
	private HibernateTemplate ht;

	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	@Logger
	static Log log;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(AutoInvest ai, FacesContext fc)
			throws IOException {
		AutoInvest ai2 = ht.get(AutoInvest.class, ai.getUserId());
		if (ai2 == null || !ai2.getStatus().equals(ai.getStatus())) {
			// 之前没有设置，或者开启或者关闭了设置。直接扔到队尾
			ai.setLastAutoInvestTime(new Date());
		}
		// ht.merge(ai);

		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='utf-8'?>");
		// 商户编号:商户在易宝唯一标识
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		// 商户平台会员标识:会员在商户平台唯一标识
		content.append("<platformUserNo>" + ai.getUser().getId()
				+ "</platformUserNo>");
		// 商户平台会员标识:会员在商户平台唯一标识
		content.append("<requestNo>"
				+ YeePayConstants.RequestNoPre.AUTHORIZE_AUTO_TRANSFER
				+ ai.getUser().getId() + "</requestNo>");
		// 页面回跳URL
		content.append("<callbackUrl>"
				+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.AUTHORIZE_AUTO_TRANSFER
				+ "</callbackUrl>");
		// 服务器通知 URL:服务器通知 URL
		content.append("<notifyUrl>"
				+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.AUTHORIZE_AUTO_TRANSFER
				+ "</notifyUrl>");
		// 提现金额:如果传入此，将使用该金额提现且用户将不可更改
		content.append("</request>");

		Map<String, String> params = new HashMap<String, String>();
		params.put("req", content.toString());
		String sign = CFCASignUtil.sign(content.toString());
		params.put("sign", sign);

		// 保存本地
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(ai.getUserId());
		to.setOperator(ai.getUserId());
		to.setRequestUrl(YeePayConstants.RequestUrl.AUTHORIZE_AUTO_TRANSFER);
		to.setRequestData(new XStream().toXML(ai));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.AUTHORIZE_AUTO_TRANSFER);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		try {
			String reqContent = HtmlElementUtil.createAutoSubmitForm(params,
					to.getRequestUrl(), "utf-8");
			ExternalContext ec = fc.getExternalContext();
			ec.responseReset();
			ec.setResponseCharacterEncoding("utf-8");
			ec.setResponseContentType("text/html");
			ec.getResponseOutputWriter().write(reqContent);
			fc.responseComplete();

			to.setRequestTime(new Date());
			to.setStatus(TrusteeshipConstants.Status.SENDED);
			ht.update(to);
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
		// 响应的参数 为xml格式
		String respXML = request.getParameter("resp");
		log.debug(respXML.toString());
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		if (flag) {
			// 处理账户开通成功
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
			// 请求流水号 注册时传递的userId
			String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.AUTHORIZE_AUTO_TRANSFER, "");

			// 返回码
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.AUTHORIZE_AUTO_TRANSFER,
					requestNo, requestNo, "yeepay");

			to.setResponseTime(new Date());
			to.setResponseData(respXML);
			// 以服务器通知为准 服务器通知会再次做处理
			if ("1".equals(code)) {
				AutoInvest ai = ht.get(AutoInvest.class, requestNo);
				if (ai != null) {
					ht.evict(ai);
					ai = ht.get(AutoInvest.class, requestNo, LockMode.UPGRADE);
					ai.setStatus(InvestConstants.AutoInvest.Status.ON);
				} else {
					ai = (AutoInvest) new XStream()
							.fromXML(to.getRequestData());
				}
				ht.merge(ai);

				to.setStatus(TrusteeshipConstants.Status.PASSED);
				ht.update(to);
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.update(to);
				if ("0".equals(code)) {
					throw new TrusteeshipReturnException(description);
				}
				// 真实错误原因
				throw new TrusteeshipReturnException(code + ":" + description);
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
		// 响应的参数 为xml格式
		String respXML = request.getParameter("resp");
		log.debug(respXML.toString());
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		if (flag) {
			// 处理账户开通成功
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
			String platformUserNo = resultMap.get("platformUserNo");
			// 返回码
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.AUTHORIZE_AUTO_TRANSFER,
					platformUserNo, platformUserNo, "yeepay");

			to.setResponseTime(new Date());
			to.setResponseData(respXML);
			if ("1".equals(code)) {
				AutoInvest ai = ht.get(AutoInvest.class, platformUserNo);
				if (ai != null) {
					ht.evict(ai);
					ai = ht.get(AutoInvest.class, platformUserNo,
							LockMode.UPGRADE);
					ai.setStatus(InvestConstants.AutoInvest.Status.ON);
					ai.setLastAutoInvestTime(new Date());
				} else {
					ai = (AutoInvest) new XStream()
							.fromXML(to.getRequestData());
				}
				ht.merge(ai);

				to.setStatus(TrusteeshipConstants.Status.PASSED);
				ht.update(to);
			}
		}

	}

}
