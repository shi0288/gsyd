package com.esoft.yeepay.cptransaction.requestModel;

public class Extend {

	/**项目编号*/
	private String tenderOrderNo;
	/**项目名称*/
	private String tenderName;
	/**项目金额*/
	private String tenderAmount;
	/**项目描述信息*/
	private String tenderDescription;
	/**项目的借款人平台用户编号*/
	private String borrowerPlatformUserNo;
	/**债权购买人*/
	private String creditorPlatformUserNo;
	/**需要转让的投资记录流水号*/
	private String originalRequestNo;

	public String getTenderOrderNo() {
		return tenderOrderNo;
	}

	public void setTenderOrderNo(String tenderOrderNo) {
		this.tenderOrderNo = tenderOrderNo;
	}

	public String getTenderName() {
		return tenderName;
	}

	public void setTenderName(String tenderName) {
		this.tenderName = tenderName;
	}

	public String getTenderAmount() {
		return tenderAmount;
	}

	public void setTenderAmount(String tenderAmount) {
		this.tenderAmount = tenderAmount;
	}

	public String getTenderDescription() {
		return tenderDescription;
	}

	public void setTenderDescription(String tenderDescription) {
		this.tenderDescription = tenderDescription;
	}

	public String getBorrowerPlatformUserNo() {
		return borrowerPlatformUserNo;
	}

	public void setBorrowerPlatformUserNo(String borrowerPlatformUserNo) {
		this.borrowerPlatformUserNo = borrowerPlatformUserNo;
	}

	public String getCreditorPlatformUserNo() {
		return creditorPlatformUserNo;
	}

	public void setCreditorPlatformUserNo(String creditorPlatformUserNo) {
		this.creditorPlatformUserNo = creditorPlatformUserNo;
	}

	public String getOriginalRequestNo() {
		return originalRequestNo;
	}

	public void setOriginalRequestNo(String originalRequestNo) {
		this.originalRequestNo = originalRequestNo;
	}
	
	
}
