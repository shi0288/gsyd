package com.esoft.archer.user.controller;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.faces.application.FacesMessage;

import org.apache.commons.logging.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.model.Role;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.StringManager;

@Component
@Scope(ScopeType.VIEW)
public class RoleHome extends EntityHome<Role> {

	// private static final long serialVersionUID = -5194410042254832100L;

	@Logger
	Log log;

	private static StringManager sm = StringManager
			.getManager(UserConstants.Package);
	private final static String UPDATE_VIEW = FacesUtil
			.redirect("/admin/user/roleList");
	
	public RoleHome() {
		// FIXME：保存角色的时候会执行一条更新User的语句
		setUpdateView(UPDATE_VIEW);
	}

	@Override
	@Transactional(readOnly=false)
	public String delete() {
		if (log.isInfoEnabled()) {
			log.info(sm.getString("log.info.deleteRole",
					FacesUtil
					.getExpressionValue("#{loginUserInfo.loginUserId}"), new Date(), getId()));
		}
		List<User> userLists = getInstance().getUsers();
		if (userLists != null && userLists.size() > 0) {
			FacesUtil.addWarnMessage(sm
					.getString("canNotDeleteRole"));
			if (log.isInfoEnabled()) {
				log.info(sm.getString(
						"log.info.deleteRoleUnsuccessful",
						FacesUtil
						.getExpressionValue("#{loginUserInfo.loginUserId}"), new Date(), getId()));
			}
			return null;
		}else{
			return super.delete();
		}
	}
	
	/**
	 * 角色管理删除操作,角色关联用户,没法删除,需要清理关联表再做删除
	 */
	@Override
	@Transactional(readOnly=false)
	public String delete(String id) {
		Role role = getBaseService().get(Role.class, id);
		List<User> userLists = role.getUsers();
		if (userLists != null && userLists.size() > 0) {
			FacesUtil.addErrorMessage(sm.getString("canNotDeleteRole"));			
			return null;
		}
		//清理关联的权限和用户的关联关系
		role.getPermissions().clear();
		role.getUsers().clear();
		getBaseService().delete(role);
		FacesUtil.addInfoMessage(super.commonSM.getString("deleteSuccessMessage"));
		return super.getDeleteView();
	}

}
