package com.esoft.jdp2p.repay.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.esoft.archer.user.model.User;

/**
 * 还款代偿
 * 
 * @author Administrator
 * 
 */
@Entity
@Table(name = "repay_compensation")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "entityCache")
public class RepayCompensation {
	private String id;
	// 被偿还的还款
	private LoanRepay loanRepay;
	// 代偿者
	private User repayer;
	// 代偿时间
	private Date compensateTime;
	// 还款给代偿者的时间
	private Date payCompensationTime;

	private String status;

	@Column(name = "compensate_time")
	public Date getCompensateTime() {
		return compensateTime;
	}

	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return id;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loan_repay_id")
	public LoanRepay getLoanRepay() {
		return loanRepay;
	}

	@Column(name = "pay_compensation_time")
	public Date getPayCompensationTime() {
		return payCompensationTime;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "repayer_id")
	public User getRepayer() {
		return repayer;
	}

	@Column(name = "status", length = 200)
	public String getStatus() {
		return status;
	}

	public void setCompensateTime(Date compensateTime) {
		this.compensateTime = compensateTime;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLoanRepay(LoanRepay loanRepay) {
		this.loanRepay = loanRepay;
	}

	public void setPayCompensationTime(Date payCompensationTime) {
		this.payCompensationTime = payCompensationTime;
	}

	public void setRepayer(User repayer) {
		this.repayer = repayer;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
