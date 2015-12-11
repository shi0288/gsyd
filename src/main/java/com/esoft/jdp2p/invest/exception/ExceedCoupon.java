package com.esoft.jdp2p.invest.exception;
/**
 * Company: jdp2p <br/>
 * Copyright: Copyright (c)2015 <br/>
 * Description: 代金卷的金额大于投资额，这会导致用户投资钱数为0（用户投资钱数=投资金额-代金卷金额）
 * 
 * @author: wangzhi
 * @version: 1.0 Create at: 2015-6-12 
 * 
 *           Modification History: <br/>
 *           Date Author Version Description
 *           ------------------------------------------------------------------
 *           2015-6-12 jiayunlu 1.0
 */
public class ExceedCoupon extends Exception{
	
	public ExceedCoupon() {
	}
	
	public ExceedCoupon(String msg) {
		super(msg);
	}
		
}
