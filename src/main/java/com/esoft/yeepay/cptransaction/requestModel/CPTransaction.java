package com.esoft.yeepay.cptransaction.requestModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 转让授权请求实体模型
 * 
 * @author lyz
 * 
 */
public class CPTransaction {

	/** 请求流水号 */
	private String requestNo;
	/** 商户编号 */
	private String plateformNo;
	/** 出款人平台用户编号 */
	private String platformUserNo;
	/** 出款人用户类型，目前只支持传入MEMBER */
	private String userType = "MEMBER";// 默认值是MEMBER
	/** 根据业务的不同，需要传入不同的值 */
	private String bizType;
	/** 超过此时间即不允许提交订单 */
	private String expired;
	/** 服务器通知URL */
	private String notifyUrl;
	/** 页面回跳URL */
	private String callbackUrl;
	/**扩展参数*/
	private Extend extend;
	/**资金明细记录*/
	private List<FundDetail> FundDetails = new ArrayList<FundDetail>() ;

	public String getRequestNo() {
		return requestNo;
	}

	public void setRequestNo(String requestNo) {
		this.requestNo = requestNo;
	}

	public String getPlateformNo() {
		return plateformNo;
	}

	public void setPlateformNo(String plateformNo) {
		this.plateformNo = plateformNo;
	}

	public String getPlatformUserNo() {
		return platformUserNo;
	}

	public void setPlatformUserNo(String platformUserNo) {
		this.platformUserNo = platformUserNo;
	}

	public String getUserType() {
		return userType;
	}

	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getBizType() {
		return bizType;
	}

	public void setBizType(String bizType) {
		this.bizType = bizType;
	}

	public String getExpired() {
		return expired;
	}

	public void setExpired(String expired) {
		this.expired = expired;
	}

	public String getNotifyUrl() {
		return notifyUrl;
	}

	public void setNotifyUrl(String notifyUrl) {
		this.notifyUrl = notifyUrl;
	}

	public String getCallbackUrl() {
		return callbackUrl;
	}

	public void setCallbackUrl(String callbackUrl) {
		this.callbackUrl = callbackUrl;
	}
	
	
	public Extend getExtend() {
		return extend;
	}

	public void setExtend(Extend extend) {
		this.extend = extend;
	}


	public List<FundDetail> getFundDetails() {
		return FundDetails;
	}

	public void setFundDetails(List<FundDetail> fundDetails) {
		FundDetails = fundDetails;
	}


	/**
	 * 构建请求XML参数
	 */
	public String toString(){
		
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version='1.0' encoding='utf-8' standalone='yes'?>");
		sb.append("<request platformNo='"+this.plateformNo+"'>");
			sb.append("<requestNo>"+this.requestNo+"</requestNo>");
			sb.append("<platformUserNo>"+this.platformUserNo+"</platformUserNo>");
			sb.append("<userType>"+this.userType+"</userType>");
			//sb.append("<expired>"+this.expired+"</expired>");
			sb.append("<bizType>"+this.bizType+"</bizType>");
			
			sb.append("<details>");
				for(FundDetail fd : FundDetails){
					sb.append("<detail>");
					sb.append("<targetUserType>"+fd.getTargetUserType()+"</targetUserType>");
					sb.append("<targetPlatformUserNo>"+fd.getTargetPlatformUserNo()+"</targetPlatformUserNo>");
					sb.append("<amount>"+fd.getAmount()+"</amount>");
					sb.append("<bizType>"+fd.getBizType()+"</bizType>");
					sb.append("</detail>");
				}
			sb.append("</details>");
			
			if(extend != null){
				sb.append("<extend>");
					if(extend.getTenderOrderNo()!= null)
						sb.append("<property name='tenderOrderNo' value='"+extend.getTenderOrderNo()+"' />");
					if(extend.getTenderName()!= null)
						sb.append("<property name='tenderName' value='"+extend.getTenderName()+"' />");
					if(extend.getTenderAmount()!=null)
						sb.append("<property name='tenderAmount' value='"+extend.getTenderAmount()+"' />");
					if(extend.getTenderDescription()!=null)
						sb.append("<property name='tenderDescription' value='"+extend.getTenderDescription()+"' />");
					if(extend.getBorrowerPlatformUserNo()!=null)
						sb.append("<property name='borrowerPlatformUserNo' value='"+extend.getBorrowerPlatformUserNo()+"' />");
					if(extend.getCreditorPlatformUserNo()!=null)
						sb.append("<property name='creditorPlatformUserNo' value='"+extend.getCreditorPlatformUserNo()+"' />");
					if(extend.getOriginalRequestNo()!=null)
						sb.append("<property name='originalRequestNo' value='"+extend.getOriginalRequestNo()+"' />");
				sb.append("</extend>");
			}
			
			sb.append("<notifyUrl>"+this.notifyUrl+"</notifyUrl>");
			if(this.callbackUrl != null){
				sb.append("<callbackUrl>"+this.callbackUrl+"</callbackUrl>");
			}
		sb.append("</request>");
		
		System.out.println(sb.toString());
		return sb.toString();
	}
	

}
