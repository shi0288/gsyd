package com.esoft.archer.coupon.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * 事件和红包关联 // 如果此表中有数据，当事件发生时，就送红包。
 * 
 * @author Administrator
 * 
 */
@Entity
@Table(name = "coupon_event_user_coupon")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "entityCache")
public class EventUserCoupon {

	private String id;

	/**
	 * 事件编号
	 */
	private String eventId;
	/**
	 * 红包类型
	 */
	private Coupon coupon;
	
	private String description;

	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "event_id", nullable = false, length = 100)
	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "coupon_id", nullable = false)
	public Coupon getCoupon() {
		return coupon;
	}

	public void setCoupon(Coupon coupon) {
		this.coupon = coupon;
	}

	@Column(name = "description", nullable = false, length = 500)
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	

}
