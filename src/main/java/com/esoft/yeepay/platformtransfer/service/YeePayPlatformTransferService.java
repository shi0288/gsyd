package com.esoft.yeepay.platformtransfer.service;


import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.yeepay.platformtransfer.model.YeePayPlatformTransfer;

public interface YeePayPlatformTransferService {
	
	/**
	 * 平台划账
	 * 
	 * @param userId 用户编号
	 * @param money 划款金额         
	 * @throws InsufficientBalance
	 * @throws PlatformTransferException 
	 */
	public void platformTransfer(YeePayPlatformTransfer platformTransfer) throws InsufficientBalance;
}
