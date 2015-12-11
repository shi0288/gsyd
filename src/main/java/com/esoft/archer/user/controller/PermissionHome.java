package com.esoft.archer.user.controller;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.archer.menu.model.Menu;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.model.Permission;
import com.esoft.archer.user.model.Role;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.StringManager;

@Component
@Scope(ScopeType.VIEW)
public class PermissionHome extends EntityHome<Permission> {

	@Logger
	static Log log;
	private static StringManager userSM = StringManager
			.getManager(UserConstants.Package);
	private final static String UPDATE_VIEW = FacesUtil
			.redirect("/admin/user/permissionList");

	@Resource
	HibernateTemplate ht;

	public PermissionHome() {
		setUpdateView(UPDATE_VIEW);
	}

	public Permission createInstance() {
		Permission permission = new Permission();

		// 设置父菜单
		String parentId = FacesUtil.getParameter("parentId");
		if (StringUtils.isNotEmpty(parentId)) {
			Permission parent = new Permission();
			parent.setId(parentId);
			permission.setParent(parent);
		} else {
			permission.setParent(new Permission());
		}
		permission.setSeqNum(0);
//		permission.setMustSelected(false);
		return permission;
	}

	@Transactional(readOnly = false)
	public void ajaxDelete() {
		super.delete();
		FacesUtil.addInfoMessage(commonSM.getString("deleteLabel")
				+ commonSM.getString("success"));
	}

	@Override
	@Transactional(readOnly = false)
	public String delete() {
		if (log.isInfoEnabled()) {
			log.info(userSM.getString("log.info.deletePermission", FacesUtil
					.getExpressionValue("#{loginUserInfo.loginUserId}"),
					new Date(), getId()));
		}
		
		Permission instance =ht.get(Permission.class, getId());
		super.setInstance(instance);	
		Set<Role> roleSets = getInstance().getRoles();
		Set<Menu> menuSets = getInstance().getMenus();
		List<Permission> children = getInstance().getChildren();
		if (roleSets != null && roleSets.size() > 0) {
			FacesUtil.addInfoMessage("不能删除该权限，某些角色拥有该权限");
			if (log.isInfoEnabled()) {
				log.info(userSM
						.getString(
								"log.info.deletePermissionByRoleUnsuccessful",
								FacesUtil
										.getExpressionValue("#{loginUserInfo.loginUserId}"),
								new Date(), getId()));
			}
			return null;
		} else if (menuSets != null && menuSets.size() > 0) {
			FacesUtil.addInfoMessage("不能删除该权限，某些菜单拥有该权限");
			if (log.isInfoEnabled()) {
				log.info(userSM
						.getString(
								"log.info.deletePermissionByMenuUnsuccessful",
								FacesUtil
										.getExpressionValue("#{loginUserInfo.loginUserId}"),
								new Date(), getId()));
			}
			return null;
		} else if (children != null && children.size() > 0) {
			FacesUtil.addInfoMessage("不能删除该权限，某些权限为该权限的子结点");
			if (log.isInfoEnabled()) {
				log.info(userSM
						.getString(
								"log.info.deletePermissionByChildrenUnsuccessful",
								FacesUtil
										.getExpressionValue("#{loginUserInfo.loginUserId}"),
								new Date(), getId()));
			}
			return null;
		} else {
			if(getInstance().getParent()!=null)
			{
				getInstance().getParent().getChildren().remove(getInstance());
			}
			return super.delete();
		}
	}
	
	/**
	 * 保存权限
	 * */
	@Override
	@Transactional(readOnly = false)
	public String save() {
		if (getInstance().getParent() != null) {
			// // 当前权限的上级权限不能是它本身
			if (parentIsItself(getInstance().getId(), getInstance().getParent()
					.getId())) {
				return FacesUtil.redirect("/admin/user/permissionList?id="
						+ getInstance().getId());
			}
			// 父权限不能是它的子权限
			if (parentIsChild(getInstance().getId(), getInstance().getParent()
					.getId())) {
				return FacesUtil.redirect("/admin/user/permissionList?id="
						+ getInstance().getId());
			}
			if (StringUtils.isEmpty(getInstance().getParent().getId())) {
				getInstance().setParent(null);
			}
		}
		return super.save();
	}

	/**
	 * 判断父权限是否是自己
	 * */
	private boolean parentIsItself(String permissionId, String parentId) {
		boolean result = false;
		if (permissionId.equals(parentId)) {
			FacesUtil.addWarnMessage(userSM.getString("parentCanNotBeItself"));
			result = true;
		}
		return result;
	}

	/**
	 * 判断父权限是否已经是自己的子权限
	 * */
	private boolean parentIsChild(String permissionId, String parentId) {
		boolean result = false;
		List<Permission> permissionList = ht.findByNamedQuery(
				"Permission.findPermissionsByParentId", permissionId);
		if (permissionList != null && permissionList.size() > 0) {
			for (int i = 0; i < permissionList.size(); i++) {
				String childId = permissionList.get(i).getId();
				if (childId.equals(parentId)) {
					FacesUtil.addWarnMessage(userSM
							.getString("childIdCanNotBeTheParent"));
					result = true;
					return result;
				} else {
					result = parentIsChild(childId, parentId);
					if (result)
						return result;
				}
			}
		}
		return result;
	}
}
