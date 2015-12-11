package com.esoft.yeepay.trusteeship.exception;
/**
 * 易宝支付操作异常
 * @author Administrator
 *
 */
public class YeePayOperationException extends RuntimeException {
	public YeePayOperationException(String msg) {
		super(msg);
	}

	public YeePayOperationException(String msg, Throwable t) {
		super(msg, t);
	}

	public YeePayOperationException(Throwable e) {
		super(e);
	}
}
