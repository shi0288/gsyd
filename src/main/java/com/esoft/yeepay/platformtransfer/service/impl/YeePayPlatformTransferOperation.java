package com.esoft.yeepay.platformtransfer.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.yeepay.platformtransfer.model.YeePayPlatformTransfer;
import com.esoft.yeepay.platformtransfer.service.YeePayPlatformTransferService;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;

@Service("yeePayPlatformTransferOperation")
public class YeePayPlatformTransferOperation {
	@Resource
	HibernateTemplate ht;

	@Resource
	YeePayPlatformTransferService yeePayPlatformTransferService;
	@Logger
	static Log log;

	@Transactional(rollbackFor = Exception.class)
	public void sendPlatformTransfer(YeePayPlatformTransfer platformTransfer) throws YeePayOperationException{
		HttpClient client = new HttpClient();

		// 创建一个post方法
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);

		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>" + IdGenerator.randomUUID()
				+ "</requestNo>");
		content.append("<sourceUserType>" + "MERCHANT" + "</sourceUserType>");
		content.append("<sourcePlatformUserNo>"
				+ YeePayConstants.Config.MER_CODE + "</sourcePlatformUserNo>");
		content.append("<amount>" + platformTransfer.getActualMoney()
				+ "</amount>");
		content.append("<targetPlatformUserNo>"
				+ platformTransfer.getUsername() + "</targetPlatformUserNo>");
		content.append("<targetUserType>" + "MEMBER" + "</targetUserType>");
		content.append("</request>");
		// 生成密文
		String sign = CFCASignUtil.sign(content.toString());

		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("sign", sign);
		postMethod.setParameter("service", "PLATFORM_TRANSFER");

		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);

			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));

			String code = resultMap.get("code");
			String description = resultMap.get("description");

			if (code.equals("1")) {
				platformTransfer.setSuccessTime(new Date());
				platformTransfer.setStatus("成功");

				yeePayPlatformTransferService
						.platformTransfer(platformTransfer);

			} else {
				log.debug("平台划款失败" + description);
				throw new YeePayOperationException("平台划款失败" + description);
			}

		} catch (InsufficientBalance e) {
			throw new YeePayOperationException("用户余额不足", e);
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
			postMethod.releaseConnection();
		}
	}
}
