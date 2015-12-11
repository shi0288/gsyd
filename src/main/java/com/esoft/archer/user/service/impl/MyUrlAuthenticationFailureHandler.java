package com.esoft.archer.user.service.impl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.util.StringUtils;

public class MyUrlAuthenticationFailureHandler extends
		SimpleUrlAuthenticationFailureHandler {

	private String targetUrlParameter = null;

	@Override
	public void onAuthenticationFailure(HttpServletRequest request,
			HttpServletResponse response, AuthenticationException exception)
			throws IOException, ServletException {

		if (StringUtils.hasText(targetUrlParameter)
				&& StringUtils
						.hasText(request.getParameter(targetUrlParameter))) {
			String failTargetUrl = request.getParameter(targetUrlParameter);
			saveException(request, exception);
			logger.debug("Forwarding to " + failTargetUrl);
			request.getRequestDispatcher(failTargetUrl).forward(request,
					response);
		} else {
			super.onAuthenticationFailure(request, response, exception);
		}
	}

	public String getTargetUrlParameter() {
		return targetUrlParameter;
	}

	public void setTargetUrlParameter(String targetUrlParameter) {
		this.targetUrlParameter = targetUrlParameter;
	}

}