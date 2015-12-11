package com.esoft.archer.message.service;

import com.esoft.archer.message.exception.MessageSendErrorException;


/**
 * 发短信
 * 返回信息详见文档。
 * @author Administrator
 * 
 */
public abstract class SmsService {

	/**
	 * 发送短信
	 * @param content
	 * @param mobileNumber
	 * @throws MessageSendErrorException
	 */
	public abstract void send(String content, String mobileNumber) throws MessageSendErrorException;
}
