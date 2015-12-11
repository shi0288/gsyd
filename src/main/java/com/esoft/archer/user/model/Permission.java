package com.esoft.archer.user.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

import com.esoft.archer.menu.model.Menu;
import com.esoft.archer.urlmapping.model.UrlMapping;

/**
 * Permission entity. 
 * 权限
 */
@Entity
@Table(name = "permission")
@NamedQueries({
	@NamedQuery(name = "Permission.findPermissionsByMenuId", query = "select distinct permission from Permission permission left join permission.menus menu where menu.id=:menuId"),
	@NamedQuery(name = "Permission.findPermissionsByUrlMappingId", query = "select distinct permission from Permission permission left join permission.urlMappings um where um.id=:urlMappingId"),
	@NamedQuery(name = "Permission.findPermissionsByParentId", query = "select distinct permission from Permission permission left join fetch permission.children where permission.parent.id = ?  order by permission.seqNum"),	
})
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region = "entityCache")
public class Permission implements java.io.Serializable {

	// Fields

	private String id;
	private String name;
	private String description;
	private Permission parent;
	private Integer seqNum;
	/** 权限是否为必选 */
	private Boolean mustSelected;
	
	/** 判断数据库中是否具有此权限(临时@Transient) */
	private Boolean isSelected;
	
	private Set<Role> roles = new HashSet<Role>(0);
	private Set<Menu> menus = new HashSet<Menu>(0);
	private Set<UrlMapping> urlMappings = new HashSet<UrlMapping>(0);
	private List<Permission> children = new ArrayList<Permission>(0);
	
	// Constructors

	/** default constructor */
	public Permission() {
		this.isSelected = false;
	}

	/** minimal constructor */
	public Permission(String id, String name) {
		this.id = id;
		this.name = name;
	}

	/** full constructor */
	public Permission(String id, String name, String description,
			Set<Role> roles, Set<Menu> menus) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.roles = roles;
		this.menus = menus;
	}

	
	// Property accessors
	@Id
	@Column(name = "id", unique = true, nullable = false, length = 32)
	public String getId() {
		return this.id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name = "name", nullable = false, length = 100)
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Lob 
	@Column(name = "description", columnDefinition="longtext")
	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "permissions")
	public Set<Role> getRoles() {
		return this.roles;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "permissions")
	public Set<Menu> getMenus() {
		return this.menus;
	}

	public void setMenus(Set<Menu> menus) {
		this.menus = menus;
	}

	@ManyToMany(fetch = FetchType.LAZY, mappedBy = "permissions")
	public Set<UrlMapping> getUrlMappings() {
		return this.urlMappings;
	}

	public void setUrlMappings(Set<UrlMapping> urlMappings) {
		this.urlMappings = urlMappings;
	}

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pid")
	public Permission getParent() {
		return this.parent;
	}

	public void setParent(Permission parent) {
		this.parent = parent;
	}
	
	@Column(name = "seq_num")
	public Integer getSeqNum() {
		return this.seqNum;
	}

	public void setSeqNum(Integer seqNum) {
		this.seqNum = seqNum;
	}
	
	@Transient
	public Boolean getIsSelected() {
		return this.isSelected;
	}
	
	public void setIsSelected(Boolean isSelected) {
		this.isSelected = isSelected;
	}
	
	@Column(name = "must_selected")
	public Boolean getMustSelected() {
		return this.mustSelected;
	}
	
	public void setMustSelected(Boolean mustSelected) {
		this.mustSelected = mustSelected;
	}
	
	@OneToMany(cascade = CascadeType.MERGE, fetch = FetchType.LAZY, mappedBy = "parent", orphanRemoval = true)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@OrderBy(value = "seqNum")
	public List<Permission> getChildren() {
		return this.children;
	}
	
	public void setChildren(List<Permission> children) {
		this.children = children;
	}
	
}