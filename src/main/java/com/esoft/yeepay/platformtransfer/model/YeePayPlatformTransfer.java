package com.esoft.yeepay.platformtransfer.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 平台划款
 */
@SuppressWarnings("serial")
@Entity
@Table(name = "yeepay_platform_transfer")
public class YeePayPlatformTransfer implements java.io.Serializable {

	private String id;
	private String username;
	private Date time;
	// 到账金额
	private Double actualMoney;
	private Date successTime;
	private String status;

	private String remarks;

	private String operationUsername;

	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "username", length = 50)
	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Column(name = "time", length = 19)
	public Date getTime() {
		return this.time;
	}

	@Column(name = "success_time", length = 19)
	public Date getSuccessTime() {
		return successTime;
	}

	public void setSuccessTime(Date successTime) {
		this.successTime = successTime;
	}

	@Column(name = "status", length = 4000)
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	@Column(name = "actual_money", nullable = false, precision = 22, scale = 0)
	public Double getActualMoney() {
		return this.actualMoney;
	}

	public void setActualMoney(Double actualMoney) {
		this.actualMoney = actualMoney;
	}

	@Column(name = "remarks", length = 4000)
	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	@Column(name = "operation_username", length = 50)
	public String getOperationUsername() {
		return this.operationUsername;
	}

	public void setOperationUsername(String operationUsername) {
		this.operationUsername = operationUsername;
	}

}