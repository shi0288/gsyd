package com.esoft.yeepay.user.service.impl;

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
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.system.service.SpringSecurityService;
import com.esoft.archer.user.model.Role;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.UserService;
import com.esoft.archer.user.service.impl.UserBO;
import com.esoft.core.annotations.Logger;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.MapUtil;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipAccount;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.service.impl.YeePayOperationServiceAbs;
import com.esoft.yeepay.user.model.YeepayCorpInfo;

@Service("yeePayCorpAccountOperation")
public class YeePayCorpAccountOperation extends YeePayOperationServiceAbs<YeepayCorpInfo> {
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	@Resource
	HibernateTemplate ht;

	@Resource
	UserService userService;
	@Resource
	private SpringSecurityService springSecurityService;
	@Logger
	static Log log;
	@Resource
	private UserBO userBO;
	@Override
	@Transactional(rollbackFor = Exception.class)
	public TrusteeshipOperation createOperation(YeepayCorpInfo yci, FacesContext fc) throws IOException {
		ht.saveOrUpdate(yci);
		User user = yci.getUser();
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='" + YeePayConstants.Config.MER_CODE + "'>");
		// 请求流水号
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.ENTERPRISE_REGISTER + yci.getSeq() + "a" + user.getId()+ "</requestNo>");
		// 商户平台会员标识:会员在商户平台唯一标识
		content.append("<platformUserNo>" + user.getId() + "</platformUserNo>");
		// 企业名称
		content.append("<enterpriseName>" + yci.getEnterpriseName() + "</enterpriseName>");
		// 开户银行许可证
		content.append("<bankLicense>" + yci.getBankLicense() + "</bankLicense>");
		// 组织机构代码
		content.append("<orgNo>" + yci.getOrgNo() + "</orgNo>");
		// 营业执照编号
		content.append("<businessLicense>" + yci.getBusinessLicense() + "</businessLicense>");
		// 税务登记号
		content.append("<taxNo>" + yci.getTaxNo() + "</taxNo>");
		// 法人姓名
		content.append("<legal>" + yci.getLegal() + "</legal>");
		// 法人身份证号
		content.append("<legalIdNo>" + yci.getLegalIdNo() + "</legalIdNo>");
		// 企业联系人
		content.append("<contact>" + yci.getContact() + "</contact>");
		// 联系人手机号
		content.append("<contactPhone>" + yci.getContactPhone() + "</contactPhone>");
		// 联系人邮箱
		content.append("<email>" + yci.getEmail() + "</email>");
		// 用户类型
		content.append("<memberClassType>" + yci.getMemberClassType() + "</memberClassType>");
		// 回调通知 URL
		content.append("<callbackUrl>"
				+ YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.ENTERPRISE_REGISTER
				+ "</callbackUrl>");
		// 服务器通知 URL
		content.append("<notifyUrl>"
				+ YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL
				+ YeePayConstants.OperationType.ENTERPRISE_REGISTER + "</notifyUrl>");
		content.append("</request>");
		log.debug(content.toString());
		// 包装参数
		Map<String, String> params = new HashMap<String, String>();
		params.put("req", content.toString());
		String sign = CFCASignUtil.sign(content.toString());
		log.debug(sign);
		params.put("sign", sign);
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(user.getId());
		to.setOperator(user.getId());
		to.setRequestUrl(YeePayConstants.RequestUrl.ENTERPRISE_REGISTER);
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.ENTERPRISE_REGISTER);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		try {
			super.sendOperation(to.getId(), "utf-8", fc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		ht.evict(user);
		user = ht.get(User.class, user.getId());
		userBO.addRole(user, new Role("WAIT_CONFIRM"));
		springSecurityService.refreshLoginUserAuthorities(user.getId());
		return null;
	}
	@Override
	@Transactional(rollbackFor = Exception.class, noRollbackFor=TrusteeshipReturnException.class)
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
			String requestNo = resultMap.get("requestNo").substring(resultMap.get("requestNo").indexOf("a")+1);
			// 返回码
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.ENTERPRISE_REGISTER, requestNo,
					requestNo, "yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(),
					LockMode.UPGRADE);

			to.setResponseTime(new Date());
			to.setResponseData(respXML);
			// 以服务器通知为准 服务器通知会再次做处理
			User user = ht.get(User.class, requestNo);
			if ("1".equals(code)) {
				if (user != null) {
					TrusteeshipAccount ta = ht.get(TrusteeshipAccount.class,
							user.getId());
					if (ta == null) {
						ta = new TrusteeshipAccount();
						ta.setId(user.getId());
						ta.setUser(user);
					}
					ta.setAccountId(user.getId());
					ta.setCreateTime(new Date());
					ta.setStatus(TrusteeshipConstants.Status.PASSED);
					ta.setTrusteeship("yeepay");
					ht.saveOrUpdate(ta);
					userBO.removeRole(user, new Role("WAIT_CONFIRM"));
					userBO.addRole(user, new Role("LOANER"));
					// 刷新登录用户权限
					springSecurityService.refreshLoginUserAuthorities(user.getId());
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.merge(to);
				}
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.merge(to);
				userBO.removeRole(user, new Role("WAIT_CONFIRM"));
				// 刷新登录用户权限
				springSecurityService.refreshLoginUserAuthorities(user.getId());
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
		String notifyxml = request.getParameter("notify");
		// 签名
		String sign = request.getParameter("sign");
		boolean flag = CFCASignUtil.isVerifySign(notifyxml, sign);
		if (flag) {
			// 处理账户开通成功
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(notifyxml);
			String code = resultMap.get("code");
			String message = resultMap.get("message");
			String platformUserNo = resultMap.get("platformUserNo");
			TrusteeshipOperation to = trusteeshipOperationBO.get(
					YeePayConstants.OperationType.ENTERPRISE_REGISTER,
					platformUserNo, platformUserNo, "yeepay");
			ht.evict(to);
			to = ht.get(TrusteeshipOperation.class, to.getId(),
					LockMode.UPGRADE);
			to.setResponseTime(new Date());
			to.setResponseData(notifyxml);

			User user = ht.get(User.class, platformUserNo);
			if ("1".equals(code)) {
				if (user != null) {
					TrusteeshipAccount ta = ht.get(TrusteeshipAccount.class,
							user.getId());
					if (ta == null) {
						ta = new TrusteeshipAccount();
						ta.setId(user.getId());
						ta.setUser(user);
					}
					ta.setAccountId(user.getId());
					ta.setCreateTime(new Date());
					ta.setStatus(TrusteeshipConstants.Status.PASSED);
					ta.setTrusteeship("yeepay");
					ht.saveOrUpdate(ta);
					userBO.removeRole(user, new Role("WAIT_CONFIRM"));
					userBO.addRole(user, new Role("LOANER"));
					// 刷新登录用户权限
					springSecurityService.refreshLoginUserAuthorities(user.getId());
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.merge(to);
				}
			} else if ("0".equals(code) || "104".equals(code)) {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				ht.merge(to);
				userBO.removeRole(user, new Role("WAIT_CONFIRM"));
				// 刷新登录用户权限
				springSecurityService.refreshLoginUserAuthorities(user.getId());
			} else {
				// 真实错误原因
				throw new RuntimeException(new TrusteeshipReturnException(code
						+ ":" + message));
			}
		}

		try {
			response.getWriter().write("SUCCESS");
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}

	}

}
