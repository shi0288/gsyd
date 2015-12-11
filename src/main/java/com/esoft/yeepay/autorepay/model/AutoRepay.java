package com.esoft.yeepay.autorepay.model;

import javax.persistence.Column;
import javax.persistence.Id;

import com.esoft.archer.user.model.User;

/**
 * 自动还款
 * @author lyz
 *
 */
public class AutoRepay implements java.io.Serializable{
	
	/**请求流水号*/
	private String id;
	/**收转自动还款标的号*/
	private String loanId;
	/**平台用户编号*/
	private User user;
	/**是否开启自动还款*/
	private Boolean open;
	
	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	
	@Column(name = "loan_id")
	public String getLoanId() {
		return loanId;
	}
	public void setLoanId(String loanId) {
		this.loanId = loanId;
	}
	@Column(name = "user_id")
	public User getUser() {
		return user;
	}
	public void setUser(User user) {
		this.user = user;
	}
	@Column(name = "open")
	public Boolean getOpen() {
		return open;
	}
	public void setOpen(Boolean open) {
		this.open = open;
	}
	

}
