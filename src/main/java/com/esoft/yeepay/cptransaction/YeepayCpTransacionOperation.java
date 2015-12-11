package com.esoft.yeepay.cptransaction;

import java.io.IOException;
import java.util.Map;
import javax.annotation.Resource;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;

/**
 * 转账确认接口统一操作，包括 还款，放款，债权转让接口操作	
 * @author cm
 *
 */
@Component("yeepayCpTransacionOperation")
public class YeepayCpTransacionOperation{

	@Logger
	Log log;

	@Resource
	HibernateTemplate ht;
	
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;
	
	/**
	 * 转账确认，供还款，放款，债权转让使用
	 * @param orderId
	 * 		  转账确认订单号
	 * @return
	 * 		true：转账确认成功；false：转账确认失败
	 */
	@Transactional(rollbackFor = Exception.class)
	public  boolean transactionComform(String orderId,String mode){
		
		boolean flag = false;
		
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version='1.0' encoding='utf-8' standalone='yes'?>");
		sb.append("<request platformNo='"+YeePayConstants.Config.MER_CODE+"'>");
		sb.append("<requestNo>"+orderId+"</requestNo>");
		sb.append("<mode>"+mode+"</mode>");
		sb.append("<notifyUrl>"+YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL+"</notifyUrl>");//TODO  暂时不处理s2s服务器回调
		sb.append("</request>");
		log.debug("转账确认XML："+sb.toString());
		//保存本地
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(orderId);
		to.setOperator(orderId);
		to.setRequestUrl(YeePayConstants.RequestUrl.RequestDirectUrl);
		to.setRequestData(sb.toString());
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.GIVE_MOENY_TO_BORROWER);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(YeePayConstants.RequestUrl.RequestDirectUrl);
		method.setParameter("req", sb.toString());
		String sign = CFCASignUtil.sign(sb.toString());
		method.setParameter("sign", sign);
		method.setParameter("service", "COMPLETE_TRANSACTION");
		
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
			log.debug("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		}finally{
			/* Release the connection. */
			method.releaseConnection();
		}
		
		return flag;
		
	}

}
