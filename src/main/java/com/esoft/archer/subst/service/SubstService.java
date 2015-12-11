package com.esoft.archer.subst.service;

import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.user.model.User;

public interface SubstService {
	//添加分站信息
	public void addSubst(Subst subst);
	//修改分站信息
	public void updateSubst(Subst subst);
	//删除一个分站
	public void deleteSubst(Subst subst);
	//删除分站下的用户
	public void deleteSubstUser(User user);
}
