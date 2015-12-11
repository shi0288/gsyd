package com.esoft.archer.message.service;

import com.esoft.archer.message.exception.MessageSendErrorException;


/**
 * 语音验证码
 * @author Administrator
 * 
 */
public interface VoiceCodeService {

	/**
	 * 发送语音验证码
	 * @param content
	 * @param mobileNumber
	 * @throws MessageSendErrorException
	 * @return 发送标识
	 */
	public String send(String content, String mobileNumber) throws MessageSendErrorException;
	
	/**
	 * 查询发送结果
	 * @param id 发送时候的标识
	 * @return 查询结果
	 */
	public String queryCallResult(String id);
}
