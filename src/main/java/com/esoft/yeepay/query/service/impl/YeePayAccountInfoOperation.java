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
import org.springframework.transaction.annotation.Transactional;

import com.esoft.core.annotations.Logger;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
@Service("yeePayAccountInfoOperation")
public class YeePayAccountInfoOperation {
	@Logger
	static Log log;
	
	@Transactional(rollbackFor = Exception.class)
	public void handleSendedOperation(String platformUserNo) {
		HttpClient client = new HttpClient();
		// 创建一个post方法
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);

		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<platformUserNo>" + platformUserNo
				+ "</platformUserNo>");
		content.append("</request>");
		// 生成密文
		String sign = CFCASignUtil.sign(content.toString());

		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("sign", sign);
		postMethod.setParameter("service", "ACCOUNT_INFO");

		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			System.out.println(statusCode);

			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));

			String code = resultMap.get("code");
			String description = resultMap.get("description");
			String balance = resultMap.get("balance");
			String availableAmount = resultMap.get("availableAmount");
			String freezeAmount = resultMap.get("freezeAmount");
			String cardNo = resultMap.get("cardNo");
			String cardStatus = resultMap.get("cardStatus");
			String bank = resultMap.get("bank");
			HttpServletRequest request = (HttpServletRequest) FacesContext
					.getCurrentInstance().getExternalContext().getRequest();

			if (code.equals("1")) {
				// 查询成功 处理查询记录
				// request.setAttribute("platformNo", platformNo);
				request.setAttribute("code", code);
				request.setAttribute("description", description);
				request.setAttribute("balance", balance);
				request.setAttribute("availableAmount", availableAmount);
				request.setAttribute("freezeAmount", freezeAmount);
				request.setAttribute("cardNo", cardNo);
				request.setAttribute("cardStatus", cardStatus);
				request.setAttribute("bank", bank);
			} else {
				request.setAttribute("description", "查询失败");
			}

			/*
			 * } else { log.error("Method failed: " +
			 * postMethod.getStatusLine()); }
			 */
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			// Release the connection.
			postMethod.releaseConnection();
		}
	}

}
