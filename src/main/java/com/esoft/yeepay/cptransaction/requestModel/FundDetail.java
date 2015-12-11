package com.esoft.yeepay.cptransaction.requestModel;

public class FundDetail {

	/**转入金额*/
	private String amount;
	/**用户类型*/
	private String targetUserType;
	/**平台用户编号*/
	private String targetPlatformUserNo;
	/**资金明细业务类型*/
	private String bizType;

	public String getAmount() {
		return amount;
	}

	public void setAmount(String amount) {
		this.amount = amount;
	}

	public String getTargetUserType() {
		return targetUserType;
	}

	public void setTargetUserType(String targetUserType) {
		this.targetUserType = targetUserType;
	}

	public String getTargetPlatformUserNo() {
		return targetPlatformUserNo;
	}

	public void setTargetPlatformUserNo(String targetPlatformUserNo) {
		this.targetPlatformUserNo = targetPlatformUserNo;
	}

	public String getBizType() {
		return bizType;
	}

	public void setBizType(String bizType) {
		this.bizType = bizType;
	}
	
	
}
