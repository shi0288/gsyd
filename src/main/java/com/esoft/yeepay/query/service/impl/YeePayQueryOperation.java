package com.esoft.yeepay.query.service.impl;

import java.io.IOException;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.springframework.stereotype.Service;

import com.esoft.core.annotations.Logger;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;

@Service("yeePayQueryOperation")
public class YeePayQueryOperation {

	@Logger
	static Log log;

	/**
	 * 易宝单笔交易查询
	 * 
	 * @param id
	 * @return
	 */
	public void handleSendedOperation(String id, String type) {
		boolean b = false;
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>" + id + "</requestNo>");
		content.append("<mode>" + type + "</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);

		try {
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();
			String result;
			int statusCode = client.executeMethod(postMethod);
			byte[] responseBody = postMethod.getResponseBody();
			String respInfo = new String(new String(responseBody, "UTF-8"));

			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			if ("1".equals(code)) {
				result = respInfo;
			} else {
				result = "验证未通过";
			}
			request.setAttribute("result", result);
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		}
	}
}
