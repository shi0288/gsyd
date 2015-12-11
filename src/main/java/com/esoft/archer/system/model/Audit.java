package com.esoft.archer.system.model;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.esoft.core.util.IdGenerator;

/**
 * 审计
 * 
 * @author Administrator
 * 
 */
@Entity
@Table(name = "audit")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "entityCache")
public class Audit {

	private String id;
	/** 操作时间 */
	private Date time;
	/** 操作者 */
	private String operator;
	/** 操作ip */
	private String ip;
	/** 具体操作 */
	private String operation;
	/** 操作描述 */
	private String o_description;
	/** 操作结果 */
	private String o_result;
	/** 备注 */
	private String remark;

	public Audit() {
		super();
	}

	public Audit(String operator, String ip, String operation,
			String o_description, String o_result, String remark) {
		super();
		this.id = IdGenerator.randomUUID();
		this.time = new Date();
		this.operator = operator;
		this.ip = ip;
		this.operation = operation;
		this.o_description = o_description;
		this.o_result = o_result;
		this.remark = remark;
	}

	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return id;
	}

	@Column(name = "ip", length = 50)
	public String getIp() {
		return ip;
	}

	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(name = "o_description", columnDefinition = "longtext")
	public String getO_description() {
		return o_description;
	}

	@Column(name = "o_result", length = 1024)
	public String getO_result() {
		return o_result;
	}

	@Column(name = "operation", length = 1024)
	public String getOperation() {
		return operation;
	}

	@Column(name = "operator", length = 1024)
	public String getOperator() {
		return operator;
	}

	@Lob
	@Column(name = "remark")
	public String getRemark() {
		return remark;
	}

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "time")
	public Date getTime() {
		return time;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setO_description(String o_description) {
		this.o_description = o_description;
	}

	public void setO_result(String o_result) {
		this.o_result = o_result;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public void setTime(Date time) {
		this.time = time;
	}

}
