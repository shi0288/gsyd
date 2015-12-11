package com.esoft.archer.subst.controller;

import java.io.Serializable;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.subst.service.SubstService;
import com.esoft.archer.user.exception.UserNotFoundException;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.UserService;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;

@Component
@Scope(ScopeType.VIEW)
public class SubstHome extends EntityHome<Subst> implements Serializable{
	@Logger
	Log log;
	
	@Resource
	HibernateTemplate ht;
	
	@Resource
	private SubstService substService;
	
	@Resource
	private UserService userService;
	
//	private User user0;
	
	/**
	 * 后台分站列表
	 */
	public final static String substListUrl = "/admin/subst/substList";
	
	/**
	 * 添加一个分站
	 * @return
	 */
	public String addSubst() {
		
		log.debug("SubstHome addSubst start!");
		User user;
		try {
			user=userService.getUserById(getInstance().getUserId());
			if (user != null && ht.get(getInstance().getClass(), getInstance().getId())==null) {
				//当此编号不存在时，执行以下操作，保存一条新的分站记录
				substService.addSubst(getInstance());
				FacesUtil.addErrorMessage("添加成功");
				//跳转到后台分站列表页
				return FacesUtil.redirect(substListUrl);
			}else {
				FacesUtil.addErrorMessage("编号已经存在");
				return null;
			}
		} catch (UserNotFoundException e) {
			FacesUtil.addErrorMessage("该用户不存在!");
		}
		return null;
	}
	
	
	/**
	 * 修改分站
	 * @return
	 */
	public String updateSubst() {
		log.debug("SubstHome updateSubst start!");
		User user;
		try {
			user=userService.getUserById(getInstance().getUserId());
			if (user != null && getInstance().getId() != null) {
				substService.updateSubst(getInstance());
				FacesUtil.addErrorMessage("修改成功");
				return FacesUtil.redirect(substListUrl);
			}else {
				FacesUtil.addErrorMessage("编号不能为空");
				return null;
			}
		} catch (UserNotFoundException e) {
			FacesUtil.addErrorMessage("该用户不存在!");
		}
		return null;
	}
	
	
	/**
	 * 
	 * 当分站下有用户时，删除分站
	 * @return
	 */
	public String deleteSubst(String substId) {
		log.debug("SubstHome deleteSubst start!");
		boolean flag=false;
		if (substId == null) {
			log.debug("SubstHome deleteSubst substId=null");
		}
		
		if (getBaseService().find("from User u where u.subst.id= ? ", substId).size()==0) {
			List<Subst> substs=getBaseService().find("from Subst s where s.id=?", substId);
			if (substs.size()==1) {
				substService.deleteSubst(substs.get(0));
				FacesUtil.addErrorMessage("删除成功");
				flag=true;
			}else {
				FacesUtil.addErrorMessage("该分站不存在!");
			}
		}else {
			FacesUtil.addErrorMessage("无法删除分站,请先删除该分站下的用户!");
		}
		
		if (flag) {
			return FacesUtil.redirect(substListUrl);
		}else {
			return null;
		}
	}
	
	/**
	 * 删除一个分站下的用户
	 */
	
	public void deleteSubstUser(String userid){
		log.debug("SubstHome deleteSubstUser start!");
		User user;
		try {
			user=userService.getUserById(userid);
			if (user != null) {
				substService.deleteSubstUser(user);
				FacesUtil.addErrorMessage("删除成功!");
			}
		} catch (UserNotFoundException e) {
			FacesUtil.addErrorMessage("删除失败!");
		}
	}


//	public User getUser0() {
//		return user0;
//	}
//
//
//	public void setUser0(User user0) {
//		this.user0 = user0;
//	}
	

	
	
}
