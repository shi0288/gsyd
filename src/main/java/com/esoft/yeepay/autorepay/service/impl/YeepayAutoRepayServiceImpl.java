package com.esoft.yeepay.autorepay.service.impl;

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
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
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

@Service("autoRepayService")
public class YeepayAutoRepayServiceImpl extends
		YeePayOperationServiceAbs<AutoRepay> { 

	@Logger
	static Log log;

	@Resource
	HibernateTemplate ht;
	
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	/**
	 * 易宝2.0自动还款授权接口，一次只能对一个标的还款授权，网关接口
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public TrusteeshipOperation createOperation(AutoRepay repay,
			FacesContext fc) throws IOException {

		String id = generateId();
		repay.setId(id);
		repay.setOpen(false);
		ht.save(repay);

		String reqXML = getReqXML(repay);// 请求参数原字符串
		String sign = CFCASignUtil.sign(reqXML);
		log.debug("开启自动还款reqXML:" + reqXML);
		log.debug("开启自动还款sign:" + sign);

		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", reqXML);
		params.put("sign", sign);
		
		String reqURL = YeePayConstants.RequestUrl.AUTHORIZE_AUTO_REPAYMENT;
		if (FacesUtil.isMobileRequest((HttpServletRequest) fc.getExternalContext().getRequest())) {
			reqURL = YeePayConstants.RequestUrl.MOBILE_INVEST;//TODO  手机端自动还款暂定
		}
		//易宝请求记录
		TrusteeshipOperation to = super.createTrusteeshipOperation(repay.getId(),repay.getId(),YeePayConstants.OperationType.AUTO_REPAY ,params,reqURL);
		//发送操作
		super.sendOperation(to.getId(), "UTF-8", fc);
		
		return null;
	}

	@Override
	public void receiveOperationPostCallback(ServletRequest request)
			throws TrusteeshipReturnException {
		
			try {
				request.setCharacterEncoding("UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			String respXML = request.getParameter("resp");// 响应的参数 为xml格式
			log.debug("易宝自动还款响应报文respXML："+respXML);
			String sign = request.getParameter("sign");// 签名
			boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
			if(!flag){
				log.debug("自动还款验签失败");
				return ;
			}
			//验签通过	
			signPass(respXML);
		
	}

	@Override
	public void receiveOperationS2SCallback(ServletRequest request,
			ServletResponse response) {
		
		String respXML = request.getParameter("notify");// 响应的参数 为xml格式
		log.debug("易宝自动还款响应报文s2s respXML："+respXML);
		String sign = request.getParameter("sign");// 签名
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		if(!flag){
			log.debug("自动还款s2s验签失败");
			return ;
		}
		//验签通过	
		signPass(respXML);
		//向易宝发出成功信息
		try {
			response.getWriter().write("SUCCESS");
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}




	@SuppressWarnings({ "unchecked", "deprecation" })
	@Transactional(rollbackFor = Exception.class)
	private void signPass(String respXML){
		
		Map<String, String> map = Dom4jUtil.xmltoMap(respXML);
		String requestNo = map.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.AUTO_REPAYMENT, "");
		TrusteeshipOperation to = trusteeshipOperationBO.get(YeePayConstants.OperationType.INVEST, requestNo,"yeepay");
		ht.evict(to);
		to = ht.get(TrusteeshipOperation.class, to.getId(),LockMode.UPGRADE);
		to.setResponseData(respXML);
		to.setResponseTime(new Date());
		if("1".equals(map.get("code"))){//成功
			
			to.setStatus(TrusteeshipConstants.Status.PASSED);
			ht.update(to);
			
			AutoRepay repay = ht.get(AutoRepay.class, requestNo);
			repay.setOpen(true);
			ht.update(repay);
			
		}else{//失败
			to.setStatus(TrusteeshipConstants.Status.REFUSED);
			ht.update(to);
		}
	
	}
	
	/**
	 * 
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public String generateId() {

		String gid = DateUtil.DateToString(new Date(), DateStyle.YYYYMMDD);
		String hql = "select im from AutoRepay im where im.id = (select max(imM.id) from AutoRepay imM where imM.id like ?)";
		List<AutoRepay> contractList = ht.find(hql, gid + "%");
		Integer itemp = 0;
		if (contractList.size() == 1) {
			AutoRepay im = contractList.get(0);
			ht.lock(im, LockMode.UPGRADE);
			String temp = im.getId();
			temp = temp.substring(temp.length() - 6);
			itemp = Integer.valueOf(temp);
		}
		itemp++;
		gid += String.format("%06d", itemp);
		return gid;
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
		sb.append("<request platformNo='" + YeePayConstants.Config.MER_CODE
				+ "'>");
		sb.append("<requestNo>" + YeePayConstants.RequestNoPre.AUTO_REPAYMENT+repay.getId() + "</requestNo>");
		sb.append("<orderNo>" + YeePayConstants.RequestNoPre.AUTO_REPAYMENT+repay.getLoanId() + "</orderNo>");
		sb.append("<notifyUrl>"
				+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.AUTO_REPAY
				+ "</notifyUrl>");
		sb.append("<callbackUrl>"
				+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.AUTO_REPAY
				+ "</callbackUrl>");
		sb.append("<platformUserNo>" + repay.getUser().getId()+ "</platformUserNo>");
		sb.append("</request>");

		return sb.toString();

	}

}
