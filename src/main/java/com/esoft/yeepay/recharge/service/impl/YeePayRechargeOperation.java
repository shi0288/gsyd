package com.esoft.yeepay.recharge.service.impl;

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
import org.hibernate.ObjectNotFoundException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.bankcard.model.BankCard;
import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.user.UserConstants.RechargeStatus;
import com.esoft.archer.user.model.Recharge;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.RechargeService;
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

@Service("yeePayRechargeOperation")
public class YeePayRechargeOperation extends
		YeePayOperationServiceAbs<Recharge> {

	@Resource
	RechargeService rechargeService;

	@Resource
	HibernateTemplate ht;

	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	@Resource
	ConfigService configService;

	@Logger
	static Log log;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(Recharge recharge,
			FacesContext fc) throws IOException {
		// 保存一个充值订单
		String id = rechargeService.createRechargeOrder(recharge);
		log.debug(id);
		// 根据id获取刚刚保存的充值订单
		recharge = ht.get(Recharge.class, recharge.getId());
		User user = ht.get(User.class, recharge.getUser().getId());
		log.debug(user.getUsername());
		recharge.setUser(user);

		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");

		content.append("<platformUserNo>" + recharge.getUser().getId()
				+ "</platformUserNo>");
		content.append("<requestNo>" + YeePayConstants.RequestNoPre.RECHARGE
				+ recharge.getId() + "</requestNo>");
		content.append("<amount>" + recharge.getActualMoney() + "</amount>");

		// 谁付手续费 1：平台支付 2：用户支付
		String feeType = "1";
		try {
			feeType = configService.getConfigValue("yeepay.recharge_fee_type");
		} catch (ObjectNotFoundException e) {
			if (log.isDebugEnabled()) {
				log.debug(e);
			}
		}
		if ("2".equals(feeType)) {
			content.append("<feeMode>" + "USER" + "</feeMode>");
		} else {
			content.append("<feeMode>" + "PLATFORM" + "</feeMode>");
		}

		content.append("<callbackUrl>"
				+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.RECHARGE + "</callbackUrl>");
		// 服务器通知 URL
		content.append("<notifyUrl>"
				+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.RECHARGE + "</notifyUrl>");
		content.append("</request>");
		log.debug(content.toString());
		// 生成cfca签名
		String sign = CFCASignUtil.sign(content.toString());

		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", content.toString());
		params.put("sign", sign);
		log.debug("createRechargeOrder的" + params);

		// 保存到本地数据库中
		TrusteeshipOperation to = new TrusteeshipOperation();
		// to表中的主键
		to.setId(IdGenerator.randomUUID());
		// 操作的唯一标识（与回调的唯一标识一致，用于查询某一条操作记录）
		to.setMarkId(recharge.getId());
		// Qr2mui67rUra
		to.setOperator(recharge.getId());
		if (FacesUtil.isMobileRequest((HttpServletRequest) fc
				.getExternalContext().getRequest())) {
			to.setRequestUrl(YeePayConstants.RequestUrl.MOBILE_RECHARGE);
		} else {
			to.setRequestUrl(YeePayConstants.RequestUrl.RECHARGE);
		}
		to.setRequestData(MapUtil.mapToString(params));
		// 第一次保存数据库中的数据为un_send,等待发送
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		// 类型,充值
		to.setType(YeePayConstants.OperationType.RECHARGE);
		// 托管方
		to.setTrusteeship("yeepay");
		// 将to对象保存到数据库中
		trusteeshipOperationBO.save(to);
		try {
			// 执行发送
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
			throw new TrusteeshipReturnException(e.getMessage());
		}
		// 响应的参数 为xml格式
		String respXML = request.getParameter("resp");
		// 签名
		String sign = request.getParameter("sign");
		if (respXML == null) {
			throw new TrusteeshipReturnException("没有参数");
		}
		if (CFCASignUtil.isVerifySign(respXML, sign) == false) {
			throw new TrusteeshipReturnException("没有参数");
		}

		// 将xml个数数据转换成map
		@SuppressWarnings("unchecked")
		Map<String, String> resultMap = Dom4jUtil.xmltoMap(respXML);
		StringBuffer sb = new StringBuffer();
		String platformNo = resultMap.get("platformNo");
		sb.append(platformNo).append(",");
		String service = resultMap.get("service");
		sb.append(service).append(",");
		String requestNo = resultMap.get("requestNo").replaceFirst(
				YeePayConstants.RequestNoPre.RECHARGE, "");
		sb.append(requestNo).append(",");
		String code = resultMap.get("code");
		sb.append(code).append(",");
		String description = resultMap.get("description");
		sb.append(description);

		// 根据充值流水号,查处对应的充值记录
		TrusteeshipOperation to = trusteeshipOperationBO.get(
				YeePayConstants.OperationType.RECHARGE, requestNo, requestNo,
				"yeepay");
		String operator = to.getOperator();
		// 设置响应时间
		to.setResponseTime(new Date());
		// FIXME:记录返回信息
		// to.setResponseData(description);

		if ("1".equals(code)) {
			// 充值成功
			rechargeService.rechargePaySuccess(requestNo);
			// 设置用户操作状态,是否充值成功,充值成功修改操作状态
			to.setStatus(TrusteeshipConstants.Status.PASSED);
			to.setResponseData(sb.toString());
			// 更新该调记录
			ht.update(to);
		} else {
			// 标记充值失败。
			Recharge recharge = ht.get(Recharge.class, operator);
			recharge.setStatus(RechargeStatus.FAIL);
			ht.update(recharge);
			to.setStatus(TrusteeshipConstants.Status.REFUSED);
			to.setResponseData(sb.toString());
			ht.update(to);
			throw new TrusteeshipReturnException(description);
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

		// 签名
		String sign = request.getParameter("sign");

		if (notifyXML == null) {
			return;
		}
		if (CFCASignUtil.isVerifySign(notifyXML, sign) == false) {
			return;
		}

		// 将xml个数数据转换成map
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
		String requestNo = resultMap.get("requestNo").replaceFirst(
				YeePayConstants.RequestNoPre.RECHARGE, "");
		sb.append(requestNo).append(",");
		String platformUserNo = resultMap.get("platformUserNo");
		sb.append(platformUserNo);
		if (bizType.equals("BIND_BANK_CARD ")) {
			// 快捷支付绑卡回调
			Recharge r = ht.get(Recharge.class, requestNo);
			String cardNo = resultMap.get("cardNo");
			BankCard bc;
			List<BankCard> bcs = ht.find(
					"from BankCard bc where bc.user.id=? and bc.cardNo=?", r
							.getUser().getId(), cardNo);
			if (bcs.size() == 0) {
				bc = new BankCard();
				bc.setCardNo(cardNo);
				bc.setBankNo(resultMap.get("bank"));
				bc.setId(IdGenerator.randomUUID());
				bc.setBank(rechargeService.getBankNameByNo(bc.getBankNo()));
				bc.setTime(new Date());
				bc.setUser(r.getUser());
			} else {
				bc = bcs.get(0);
			}
			bc.setStatus("VERIFIED");
			ht.saveOrUpdate(bc);
		} else {
			// 根据充值流水号,查处对应的充值记录
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.RECHARGE, requestNo,
					requestNo, "yeepay");
			String operator = to.getOperator();
			// 设置响应时间
			to.setResponseTime(new Date());
			to.setResponseData(notifyXML);

			if ("1".equals(code)) {
				// 充值成功
				rechargeService.rechargePaySuccess(requestNo);
				// 设置用户操作状态,是否充值成功,充值成功修改操作状态
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				to.setResponseData(sb.toString());
				// 更新该调记录
				ht.update(to);
			} else {
				// 标记充值失败。
				Recharge recharge = ht.get(Recharge.class, operator);
				recharge.setStatus(RechargeStatus.FAIL);
				ht.update(recharge);
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(sb.toString());
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
