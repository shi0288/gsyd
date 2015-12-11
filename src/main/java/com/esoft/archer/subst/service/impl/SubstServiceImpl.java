package com.esoft.archer.subst.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.common.service.BaseService;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.subst.service.SubstService;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.Logger;

@Service("substService")
public class SubstServiceImpl implements SubstService{
	
	@Logger
	private static Log log;
	
	@Resource
	HibernateTemplate ht;
	
	@Override
	@Transactional(readOnly=false,rollbackFor=Exception.class)
	public void addSubst(Subst subst) {
		Date date=new Date();
		subst.setCreateTime(date);
		ht.save(subst);
	}

	@Override
	@Transactional(readOnly=false,rollbackFor=Exception.class)
	public void updateSubst(Subst subst) {
		ht.update(subst);
		
	}

	@Override
	@Transactional(readOnly=false,rollbackFor=Exception.class)
	public void deleteSubst(Subst subst) {
		ht.delete(subst);
	}

	@Override
	@Transactional(readOnly=false,rollbackFor=Exception.class)
	public void deleteSubstUser(User user) {
		user.setSubst(null);
		ht.update(user);
	}

}
