package com.esoft.archer.user.controller;

import java.io.IOException;
import java.util.List;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;

import com.esoft.archer.user.UserConstants.UserStatus;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.impl.MyAuthenticationManager;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.Base64Coder;
import com.esoft.core.util.HashCrypt;

/**
 * 处理用户登录用
 * 
 * @author Administrator
 * 
 */
@Component
@Scope(ScopeType.VIEW)
public class UserLoginHome {

	@Resource
	HibernateTemplate ht;

	@Resource
	UserDetailsService userDetailsService;

	@Autowired
	SessionRegistry sessionRegistry;

	@Autowired
	RememberMeServices rememberMeServices;

	@Resource
	MyAuthenticationManager myAuthenticationManager;

	/**
	 * ajax登录
	 */
	public void ajaxLogin() {
		// 用户名/密码加解密
		String username = FacesUtil.getParameter("j_username");
		String password = FacesUtil.getParameter("j_password");
		String encrypt = FacesUtil.getParameter("encrypt");
		if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
			FacesUtil.addErrorMessage("请输入用户名和密码！");
			return;
		}
		if ("encrypt".equals(encrypt)) {
			username = new String(Base64Coder.decryptBase64(username));
			password = new String(Base64Coder.decryptBase64(password));
		}
		List<User> users = ht
				.find("from User u where u.username=? or u.mobileNumber=? or u.email=?",
						username, username, username);
		if (users.size() == 0) {
			FacesUtil.addErrorMessage("用户名或密码错误！");
			myAuthenticationManager.handleLoginFail(null,
					FacesUtil.getHttpServletRequest());
			return;
		}
		User u = users.get(0);
		if (!u.getPassword()
				.equals(HashCrypt.getDigestHash(password))) {
			FacesUtil.addErrorMessage("用户名或密码错误！");
			myAuthenticationManager.handleLoginFail(u,
					FacesUtil.getHttpServletRequest());
			return;
		}

		if (u.getStatus().equals(UserStatus.DISABLE)) {
			FacesUtil.addErrorMessage("该用户已被禁用！");
			myAuthenticationManager.handleLoginFail(u,
					FacesUtil.getHttpServletRequest());
			return;
		} else if (u.getStatus().equals(UserStatus.NOACTIVE)) {
			FacesUtil.addErrorMessage("该用户尚未激活！");
			myAuthenticationManager.handleLoginFail(u,
					FacesUtil.getHttpServletRequest());
			return;
		} else if (u.getStatus().equals(UserStatus.ENABLE)) {
			// 登录
			UserDetails userDetails = userDetailsService.loadUserByUsername(u
					.getUsername());
			UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
					u.getUsername(), userDetails.getPassword(),
					userDetails.getAuthorities());

			SecurityContextHolder.getContext().setAuthentication(token);
			FacesUtil
					.getHttpSession()
					.setAttribute(
							HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
							SecurityContextHolder.getContext());
			sessionRegistry.registerNewSession(FacesUtil.getHttpSession()
					.getId(), userDetails);
			myAuthenticationManager.handleLoginSuccess(u,
					FacesUtil.getHttpServletRequest());
			// 下次自动登录
			rememberMeServices.loginSuccess(FacesUtil.getHttpServletRequest(),
					FacesUtil.getHttpServletResponse(), token);
			// 刷新当前页面
			String ajaxRedirectXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
					+ "<partial-response><redirect url=\""
					+ FacesUtil.getHttpServletRequest().getHeader("Referer")
					+ "\"></redirect></partial-response>";
			FacesUtil.getHttpServletResponse().setContentType("text/xml");
			try {
				FacesUtil.getHttpServletResponse().getWriter()
						.write(ajaxRedirectXml);
			} catch (IOException e) {
				e.printStackTrace();
			}
			FacesUtil.getCurrentInstance().responseComplete();
		}
	}

}
