package com.esoft.archer.message.service.impl;

import org.springframework.stereotype.Service;

import com.esoft.archer.message.exception.MessageSendErrorException;
import com.esoft.archer.message.service.VoiceCodeService;

@Service
public class VoiceCodeServiceImpl implements VoiceCodeService {

	@Override
	public String send(String content, String mobileNumber)
			throws MessageSendErrorException {
		throw new RuntimeException("you must override this method!");
	}

	@Override
	public String queryCallResult(String id) {
		throw new RuntimeException("you must override this method!");
	}

}
