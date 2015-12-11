package com.esoft.yeepay.withdraw.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.hibernate.ObjectNotFoundException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.model.WithdrawCash;
import com.esoft.archer.user.service.WithdrawCashService;
import com.esoft.archer.user.service.impl.UserBillBO;
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

@Service("yeePayWithdrawOperation")
public class YeePayWithdrawOperation extends
		YeePayOperationServiceAbs<WithdrawCash> {
	
	@Resource
	WithdrawCashService withdrawCashService;

	@Resource
	HibernateTemplate ht;

	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	@Resource
	ConfigService configService;

	@Logger
	static Log log;
	@Resource
	UserBillBO ub;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(WithdrawCash withdrawCash,
			FacesContext fc) throws IOException {

		// 得到一个提现订单
		WithdrawCash wc = ht.get(WithdrawCash.class, withdrawCash.getId());
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		// 商户编号:商户在易宝唯一标识
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		// 商户平台会员标识:会员在商户平台唯一标识
		content.append("<platformUserNo>" + wc.getUser().getId()
				+ "</platformUserNo>");
		// 请求流水号:接入方必须保证参数内的 requestNo 全局唯一，并且需要保存，留待后续业务使用
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.WITHDRAW_CASH + withdrawCash.getId() + "</requestNo>");
		// 费率模式:见费率模式
		// yee手续费收取方:1：平台支付 2：提现方支付
		String feeType = "2";
		try {
			feeType = configService.getConfigValue("yeepay.withdraw_fee_type");
		} catch (ObjectNotFoundException e) {
			if (log.isDebugEnabled()) {
				log.debug(e);
			}
		}

		// 平台手续费
		if (feeType.equals("1")) {
			content.append("<feeMode>" + "PLATFORM" + "</feeMode>");
		} else {
			// 如果是用户自己承担手续费，假如提现100，手续费3块，则账户扣除103
			content.append("<feeMode>" + "USER" + "</feeMode>");
		}

		// 回调通知 URL:回调通知 URL
		content.append("<callbackUrl>"
				+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.WITHDRAW_CASH
				+ "</callbackUrl>");
		// 服务器通知 URL:服务器通知 URL
		content.append("<notifyUrl>"
				+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.WITHDRAW_CASH + "</notifyUrl>");
		// 提现金额:如果传入此，将使用该金额提现且用户将不可更改
		content.append("<amount>" + wc.getMoney() + "</amount>");
		content.append("</request>");

		// 生成cfca签名
		String contentString = content.toString();
		String sign = CFCASignUtil.sign(contentString);

		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", content.toString());
		params.put("sign", sign);

		log.debug("req:" + contentString + "sign" + sign);
		// 保存本地
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		// 用来查询一条提现记录
		to.setMarkId(withdrawCash.getId());
		to.setOperator(withdrawCash.getId());
		// 设置请求的地址
		if (FacesUtil.isMobileRequest((HttpServletRequest) fc.getExternalContext().getRequest())) {
			to.setRequestUrl(YeePayConstants.RequestUrl.MOBILE_WITHDRAW_CASH);
		} else {
			to.setRequestUrl(YeePayConstants.RequestUrl.WITHDRAW_CASH);
		}
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.WITHDRAW_CASH);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		try {
			sendOperation(to.getId(), "utf-8", fc);
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
		log.debug(respXML);
		// 签名
		String sign = request.getParameter("sign");
		if (respXML == null) {
			System.out.println(respXML + "没有参数");
			return;
		}
		if (CFCASignUtil.isVerifySign(respXML, sign) == false) {
			System.out.println(respXML + "没有参数");
			return;
		}
		// 数据格式转换
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
		StringBuffer sb = new StringBuffer();
		String platformNo = resultMap.get("platformNo");
		sb.append(platformNo).append(",");
		String service = resultMap.get("service");
		sb.append(service).append(",");
		String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.WITHDRAW_CASH, "");
		sb.append(requestNo).append(",");
		String code = resultMap.get("code");
		sb.append(code).append(",");
		String description = resultMap.get("description");
		sb.append(description);
		TrusteeshipOperation to = trusteeshipOperationBO.get(
				YeePayConstants.OperationType.WITHDRAW_CASH, requestNo,
				requestNo, "yeepay");
		to.setResponseTime(new Date());
		to.setResponseData(respXML);

		WithdrawCash wc = ht.get(WithdrawCash.class, requestNo);
		ht.evict(wc);
		wc = ht.get(WithdrawCash.class, requestNo, LockMode.UPGRADE);
		if (wc.getStatus().equals(UserConstants.WithdrawStatus.WAIT_VERIFY)) {
			if ("1".equals(code)) {
				withdrawCashService.passWithdrawCashRecheck(wc);
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				ht.update(to);
			} else {
				// FIXME：直接进行拒绝处理，不太合适。
				withdrawCashService.refuseWithdrawCashApply(wc);
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.update(to);
				throw new TrusteeshipReturnException(description);
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
		String notifyXML = request.getParameter("notify");
		log.debug(notifyXML);
		// 签名
		String sign = request.getParameter("sign");

		if (notifyXML == null) {
			System.out.println(notifyXML + "没有参数");
			return;
		}
		if (CFCASignUtil.isVerifySign(notifyXML, sign) == false) {
			System.out.println(notifyXML + "没有参数");
			return;
		}

		// 数据格式转换
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = Dom4jUtil.xmltoMap(notifyXML);
		StringBuffer sb = new StringBuffer();
		String platformNo = resultMap.get("platformNo");
		sb.append(platformNo).append(",");
		String bizType = resultMap.get("bizType");
		sb.append(bizType).append(",");
		String code = resultMap.get("code");
		sb.append(code).append(",");
		String message = resultMap.get("message");
		sb.append(message).append(",");
		String requestNo = resultMap.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.WITHDRAW_CASH, "");
		sb.append(requestNo).append(",");
		String platformUserNo = resultMap.get("platformUserNo");
		sb.append(platformUserNo).append(",");
		String cardNo = resultMap.get("cardNo");
		sb.append(cardNo).append(",");
		String bank = resultMap.get("bank");
		sb.append(bank);
		String strFee = resultMap.get("fee");
		sb.append(strFee);
		double fee = Double.parseDouble(strFee);
		TrusteeshipOperation to = trusteeshipOperationBO.get(
				YeePayConstants.OperationType.WITHDRAW_CASH, requestNo,
				requestNo, "yeepay");
		to.setResponseTime(new Date());
		to.setResponseData(notifyXML);

		WithdrawCash wc = ht.get(WithdrawCash.class, requestNo);
		ht.evict(wc);
		wc = ht.get(WithdrawCash.class, requestNo, LockMode.UPGRADE);
		if (wc.getStatus().equals(UserConstants.WithdrawStatus.WAIT_VERIFY)) {
			if ("1".equals(code)) {
				withdrawCashService.passWithdrawCashRecheck(wc);
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				ht.update(to);
			} else {
				// FIXME：直接进行拒绝处理，不太合适。
				withdrawCashService.refuseWithdrawCashApply(wc);
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.update(to);
			}
		}
		try {
			response.setCharacterEncoding("utf-8");
			response.getWriter().print("SUCCESS");
			FacesUtil.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
