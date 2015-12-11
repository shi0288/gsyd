package com.esoft.yeepay.autorepay.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.autorepay.model.AutoRepay;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.service.impl.YeePayOperationServiceAbs;

/**
 * 易宝2.0自动还款接口
 * 
 * @author lyz
 * 
 */

@Service("cancelAutoRepayService")
public class YeepayCancelAutoRepayServiceImpl extends
		YeePayOperationServiceAbs<AutoRepay> { 

	@Logger
	static Log log;

	@Resource
	HibernateTemplate ht;
	
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	/**
	 * 易宝2.0取消自动还款授权接口
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public TrusteeshipOperation createOperation(AutoRepay repay,
			FacesContext fc) throws IOException {

		String reqXML = getReqXML(repay);// 请求参数原字符串
		String sign = CFCASignUtil.sign(reqXML);//签名
		log.debug("开启自动还款reqXML:" + reqXML);
		log.debug("开启自动还款sign:" + sign);

		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", reqXML);
		params.put("sign", sign);
		
		String reqURL = YeePayConstants.RequestUrl.RequestDirectUrl;
		if (FacesUtil.isMobileRequest((HttpServletRequest) fc.getExternalContext().getRequest())) {
			reqURL = YeePayConstants.RequestUrl.MOBILE_INVEST;//TODO  手机端自动还款暂定
		}
		//易宝请求记录
		TrusteeshipOperation to = super.createTrusteeshipOperation(repay.getId(),repay.getId(),YeePayConstants.OperationType.CANCEL_AUTO_REPAY ,params,reqURL);
		
		//发送操作
		boolean flag = super.sendHttpClientOperation(to, "CANCEL_AUTHORIZE_AUTO_REPAYMENT");
		
		if(flag){
			repay.setOpen(false);
			ht.update(repay);
		}
		
		return null;
	}

	@Override
	public void receiveOperationPostCallback(ServletRequest request)
			throws TrusteeshipReturnException {
		
		
		
	}

	@Override
	public void receiveOperationS2SCallback(ServletRequest request,
			ServletResponse response) {
		
	
	}


	/**
	 * 获取请求参数XML
	 * 
	 * @param repay
	 * @return
	 */
	private String getReqXML(AutoRepay repay) {

		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version='1.0' encoding='utf-8' standalone='yes'?>");
		sb.append("<request platformNo='" + YeePayConstants.Config.MER_CODE+ "'>");
		sb.append("<platformUserNo>" + repay.getUser().getId()+ "</platformUserNo>");
		sb.append("<requestNo>" + YeePayConstants.RequestNoPre.AUTO_REPAYMENT+repay.getId() + "</requestNo>");
		sb.append("<orderNo>" + YeePayConstants.RequestNoPre.AUTO_REPAYMENT+repay.getLoanId() + "</orderNo>");
		/*sb.append("<notifyUrl>"+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.AUTO_REPAY + "</notifyUrl>");
		sb.append("<callbackUrl>"+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.AUTO_REPAY + "</callbackUrl>");*/
		sb.append("</request>");
		
		return sb.toString();

	}

}
