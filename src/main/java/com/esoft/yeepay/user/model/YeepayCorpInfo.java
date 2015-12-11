package com.esoft.yeepay.user.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.esoft.archer.user.model.User;

@Entity
@Table(name="yeepay_corp_info")
public class YeepayCorpInfo {
	private String id;
	private User user;
	/**企业名称**/
	private String enterpriseName;
	/**开户银行许可证**/
	private String bankLicense;
	/**组织机构代码**/
	private String orgNo;
	/**营业执照编号**/
	private String businessLicense;
	/**税务登记号**/
	private String taxNo;
	/**法人姓名*/
	private String legal;
	/**法人身份证号*/
	private String legalIdNo;
	/**企业联系人*/
	private String contact;
	/**联系人手机号*/
	private String contactPhone;
	/**联系人邮箱*/
	private String email;
	/**ENTERPRISE：企业借款人   GUARANTEE_CORP：担保公司*/
	private String memberClassType;
	//审核状态
	private String status;
	/**记录请求了几次，用于易宝的请求流水号全局唯一的要求*/
	private Integer seq;
	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	@Column(name="enterprise_name")
	public String getEnterpriseName() {
		return enterpriseName;
	}
	public void setEnterpriseName(String enterpriseName) {
		this.enterpriseName = enterpriseName;
	}
	@Column(name="bank_license")
	public String getBankLicense() {
		return bankLicense;
	}
	public void setBankLicense(String bankLicense) {
		this.bankLicense = bankLicense;
	}
	@Column(name="org_no")
	public String getOrgNo() {
		return orgNo;
	}
	public void setOrgNo(String orgNo) {
		this.orgNo = orgNo;
	}
	@Column(name="business_license")
	public String getBusinessLicense() {
		return businessLicense;
	}
	public void setBusinessLicense(String businessLicense) {
		this.businessLicense = businessLicense;
	}
	@Column(name="tax_no")
	public String getTaxNo() {
		return taxNo;
	}
	public void setTaxNo(String taxNo) {
		this.taxNo = taxNo;
	}
	public String getLegal() {
		return legal;
	}
	public void setLegal(String legal) {
		this.legal = legal;
	}
	@Column(name="legal_id_no")
	public String getLegalIdNo() {
		return legalIdNo;
	}
	public void setLegalIdNo(String legalIdNo) {
		this.legalIdNo = legalIdNo;
	}
	public String getContact() {
		return contact;
	}
	public void setContact(String contact) {
		this.contact = contact;
	}
	@Column(name="contact_phone")
	public String getContactPhone() {
		return contactPhone;
	}
	public void setContactPhone(String contactPhone) {
		this.contactPhone = contactPhone;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	@Column(name="member_class_type")
	public String getMemberClassType() {
		return memberClassType;
	}
	public void setMemberClassType(String memberClassType) {
		this.memberClassType = memberClassType;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Integer getSeq() {
		if(this.seq == null){
			return 0;
		}
		return seq;
	}
	public void setSeq(Integer seq) {
		this.seq = seq;
	}
}
