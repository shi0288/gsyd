package com.esoft.archer.message.service.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import com.esoft.archer.message.exception.MessageSendErrorException;
import com.esoft.archer.message.service.SmsService;
import com.esoft.core.util.HttpClientUtil;

public class GsSmsServiceImpl extends SmsService {

	private static Properties props = new Properties();
	static {
		try {
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("sms_gs_config.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(String content, String mobileNumber) {

		String account = props.getProperty("account");
		String pwd = props.getProperty("pwd");
		if (account == null || pwd == null) {
			throw new MessageSendErrorException("短信发送失败！ ");
		}
		
		try {

			content = URLEncoder.encode(content, "utf-8");
			String base_url = props.getProperty("base_url");
			String url = base_url + "/sms.aspx?action=send&userid=gsjr&account=" + account + "&password=" + pwd + "&mobile=" + mobileNumber + "&content=" + content + "&sendTime=";
			String res = HttpClientUtil.getResponseBodyAsString(url);
			if (!res.contains("Success")) {
				throw new MessageSendErrorException("短信发送失败!");
			}
		} catch (UnsupportedEncodingException e) {
			throw new MessageSendErrorException(null, e);
		}
	}
}
