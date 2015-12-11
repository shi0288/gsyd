package com.esoft.archer.message.exception;

/**
 * Company: jdp2p <br/>
 * Copyright: Copyright (c)2013 <br/>
 * Description: 信息发送出错，短信、语音之类
 * 
 * @author: wangzhi
 * @version: 1.0 Create at: 2014-2-10 下午4:11:37
 * 
 *           Modification History: <br/>
 *           Date Author Version Description
 *           ------------------------------------------------------------------
 *           2014-2-10 wangzhi 1.0
 */
public class MessageSendErrorException extends RuntimeException {
	public MessageSendErrorException(String msg, Throwable e) {
		super(msg, e);
	}

	public MessageSendErrorException(String msg) {
		super(msg);
	}
}
