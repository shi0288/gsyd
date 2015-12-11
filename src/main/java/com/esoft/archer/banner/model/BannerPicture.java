package com.esoft.archer.banner.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
@Table(name = "banner_picture")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "entityCache")
public class BannerPicture implements java.io.Serializable {

	// Fields

	private String id;
	private Banner banner;
	private String title;
	private String url;
	private Integer seqNum;
	/**
	 * 是否为外链
	 */
	private Boolean isOutSite;
	private String picture;

	// Constructors

	/** default constructor */
	public BannerPicture() {
	}

	/** full constructor */
	public BannerPicture(Banner banner, String picture) {
		this.banner = banner;
		this.picture = picture;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "banner_id")
	public Banner getBanner() {
		return this.banner;
	}

	// Property accessors
	// @GenericGenerator(name = "generator", strategy = "uuid.hex")
	@Id
	// @GeneratedValue(generator = "generator")
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return this.id;
	}

	@Column(name = "is_out_site", columnDefinition = "BOOLEAN")
	public Boolean getIsOutSite() {
		return isOutSite;
	}

	@Column(name = "picture", length = 300)
	public String getPicture() {
		return this.picture;
	}

	@Column(name = "title", length = 100)
	public String getTitle() {
		return title;
	}

	@Column(name = "url", length = 300)
	public String getUrl() {
		return url;
	}

	@Column(name = "seq_num", nullable = false)
	public Integer getSeqNum() {
		return this.seqNum;
	}

	public void setSeqNum(Integer seqNum) {
		this.seqNum = seqNum;
	}

	public void setBanner(Banner product) {
		this.banner = product;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setIsOutSite(Boolean isOutSite) {
		this.isOutSite = isOutSite;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}