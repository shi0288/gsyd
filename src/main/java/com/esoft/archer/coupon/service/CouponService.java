package com.esoft.archer.coupon.service;
/**  
 * Company:     jdp2p <br/> 
 * Copyright:   Copyright (c)2013 <br/>
 * Description:  红包service
 * @author:     wangzhi  
 * @version:    1.0
 * Create at:   2014-7-16 下午2:28:20  
 *  
 * Modification History: <br/>
 * Date         Author      Version     Description  
 * ------------------------------------------------------------------  
 * 2014-7-16      wangzhi      1.0          
 */
public interface CouponService {
	
	/**
	 * 禁用
	 * @param couponId
	 */
	public void disable(String couponId);
	
	/**
	 * 启用
	 * @param couponId
	 */
	public void enable(String couponId);
	
}
