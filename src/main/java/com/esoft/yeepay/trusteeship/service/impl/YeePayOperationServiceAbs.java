package com.esoft.yeepay.trusteeship.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.core.annotations.Logger;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.HtmlElementUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.MapUtil;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationServiceAbs;

public abstract class YeePayOperationServiceAbs<T> extends
		TrusteeshipOperationServiceAbs<T> {

	@Logger
	Log log;

	@Resource
	HibernateTemplate ht;

	@SuppressWarnings("deprecation")
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void sendOperation(String content, String charset, FacesContext fc)
			throws IOException {
		TrusteeshipOperation to = ht.get(TrusteeshipOperation.class, content);
		Map<String, String> params = MapUtil.stringToHashMap(to
				.getRequestData());
		String reqContent = HtmlElementUtil.createAutoSubmitForm(params,
				to.getRequestUrl(), charset);
		ExternalContext ec = fc.getExternalContext();
		ec.responseReset();
		ec.setResponseCharacterEncoding(charset);
		ec.setResponseContentType("text/html");
		ec.getResponseOutputWriter().write(reqContent);
		fc.responseComplete();

		to.setRequestTime(new Date());
		to.setStatus(TrusteeshipConstants.Status.SENDED);
		ht.update(to);
		
	}

	
	/**
	 * 
	 * @param markId 
	 * 		   操作的唯一标识（与回调的唯一标识一致，用于查询某一条操作记录）
	 * @param operator
	 * 		  操作者（如果是开户，此字段为userId；如果为充值，此字段为rechargeId）
	 * @param type
	 * 		  操作类型
	 * @param requestData
	 * 		   请求参数
	 * @param reqURL
	 * 		  请求地址
	 * @return
	 * 		 TrusteeshipOperation
	 */
	@SuppressWarnings("deprecation")
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createTrusteeshipOperation(String markId,
			String operator, String type, Map<String,String> requestData,String reqURL) {
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(markId);
		to.setRequestUrl(reqURL);
		to.setOperator(operator);
		to.setCharset("utf-8");
		to.setRequestData(MapUtil.mapToString(requestData));
		to.setType(type);
		to.setTrusteeship("yeepay");
		to.setRequestTime(new Date());
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		ht.save(to);
		return to;
	}
	
	/**
	 * 直连接口请求
	 * @param to
	 * 		   请求操作对象TrusteeshipOperation	
	 * @param service
	 * 		  请求service名称
	 * @return
	 * 		请求处理成功返回true，失败返回false
	 */
	@SuppressWarnings("deprecation")
	@Transactional(rollbackFor = Exception.class)
	public boolean sendHttpClientOperation(TrusteeshipOperation to,String service){
		
		boolean flag = false;
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(to.getRequestUrl());
		Map<String,String> map = MapUtil.stringToHashMap(to.getRequestData());
		
		method.setParameter("req", String.valueOf(map.get("req")));
		method.setParameter("sign", String.valueOf(map.get("sign")));
		method.setParameter("service", service);
		
		try {
			
			int statusCode = client.executeMethod(method);
			if (statusCode != HttpStatus.SC_OK) {
				log.debug("Method failed: " + method.getStatusLine());
			}
			
			//请求已发送
			to.setStatus(TrusteeshipConstants.Status.SENDED);
			ht.update(to);
			
			/* 获得返回的结果 */
			byte[] responseBody = method.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			@SuppressWarnings("unchecked")
			Map<String, String> respMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));

			String code = respMap.get("code");
			
			if("1".equals(code)){
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				flag = true;
			}else{
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
			}
			ht.update(to);
			
		} catch (HttpException e) {
			log.debug("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.debug("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		}finally{
			/* Release the connection. */
			method.releaseConnection();
		}
		
		return flag;
	}
	
	
}
