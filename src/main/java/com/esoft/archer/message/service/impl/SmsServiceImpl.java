package com.esoft.archer.message.service.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Properties;

import org.springframework.stereotype.Service;

import com.esoft.archer.message.exception.MessageSendErrorException;
import com.esoft.archer.message.service.SmsService;

/**
 * 发短信 返回信息详见文档。
 * 
 * @author Administrator
 * 
 */
@Service("smsService")
public class SmsServiceImpl extends SmsService {
	
	private static Properties props = new Properties(); 
	static{
		try {
			props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("zucp_sms_config.properties"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void send(String content, String mobileNumber) {
		String sn = props.getProperty("sn");
		String pwd = props.getProperty("password");
		if (sn == null || pwd == null) {
			throw new MessageSendErrorException("短信发送失败，sn或password未定义");
		}
		try {
			content = URLEncoder.encode(content, "utf-8");
			ZucpWebServiceClient client = new ZucpWebServiceClient(sn, pwd);
			String result_mt = client.mdSmsSend_u(mobileNumber, content, "",
					"", "");
			if (result_mt.startsWith("-") || result_mt.equals(""))// 发送短信，如果是以负号开头就是发送失败。
			{
				throw new MessageSendErrorException("短信发送失败，错误代码：" + result_mt);
			}
		} catch (UnsupportedEncodingException e) {
			throw new MessageSendErrorException(null, e);
		}
	}
	
//	public static void main(String[] args) {
//		new SmsServiceImpl().send("test【金鼎海汇】", "18600238751");
//	}
}
