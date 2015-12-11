package com.esoft.archer.coupon.model;

// default package

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * 红包 Coupon entity. @author MyEclipse Persistence Tools
 */
@Entity
@Table(name = "coupon")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "entityCache")
public class Coupon implements java.io.Serializable {

	// Fields

	/**
	 * 规则：使用节点+类型+金额+使用下限
	 */
	private String id;
	/**
	 * 红包名称
	 */
	private String name;
	/**
	 * 红包类型
	 */
	private String type;

	/**
	 * 状态（是否可用等等），为此红包整体的状态
	 */
	private String status;
	/**
	 * 金额
	 */
	private Double money;

	/**
	 * 使用下限（多少钱以上才可以使用该红包）
	 */
	private Double lowerLimitMoney;
	/**
	 * 有效期（秒），空值则为永久有效
	 */
	private Integer periodOfValidity;

	/**
	 * 该红包的生成时间
	 */
	private Date generateTime;

	// Constructors

	/** default constructor */
	public Coupon() {
	}

	@Column(name = "generate_time")
	public Date getGenerateTime() {
		return generateTime;
	}

	// Property accessors
	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return this.id;
	}

	@Column(name = "lower_limit_money", precision = 22, scale = 0)
	public Double getLowerLimitMoney() {
		return lowerLimitMoney;
	}

	@Column(name = "money", precision = 22, scale = 0)
	public Double getMoney() {
		return this.money;
	}

	@Column(name = "name", length = 200)
	public String getName() {
		return this.name;
	}

	@Column(name = "period_of_validity")
	public Integer getPeriodOfValidity() {
		return periodOfValidity;
	}

	@Transient
	public Integer getPeriodOfValidityDay() {
		if (this.periodOfValidity != null) {
			return periodOfValidity / 86400;
		}
		return null;
	}

	@Column(name = "status", nullable = false, length = 50)
	public String getStatus() {
		return this.status;
	}

	@Column(name = "type", nullable = false, length = 200)
	public String getType() {
		return type;
	}

	public void setGenerateTime(Date generateTime) {
		this.generateTime = generateTime;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setLowerLimitMoney(Double lowerLimitMoney) {
		this.lowerLimitMoney = lowerLimitMoney;
	}

	public void setMoney(Double money) {
		this.money = money;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPeriodOfValidity(Integer periodOfValidity) {
		this.periodOfValidity = periodOfValidity;
	}

	public void setPeriodOfValidityDay(Integer day) {
		if (day != null) {
			this.periodOfValidity = day * 86400;			
		}
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setType(String type) {
		this.type = type;
	}

}