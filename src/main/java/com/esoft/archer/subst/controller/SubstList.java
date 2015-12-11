package com.esoft.archer.subst.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.ScopeType;

@Component
@Scope(ScopeType.VIEW)
public class SubstList extends EntityQuery<Subst>{
	private static final String lazyModelCountHql = "select count(distinct subst) from Subst subst";
	private static final String lazyModelHql = "select distinct subst from Subst subst";
	
	public SubstList(){
		setCountHql(lazyModelCountHql);
		setHql(lazyModelHql);
		final String[] RESTRICTIONS = {
				"subst.id like #{substList.example.id}",
				"subst.name like #{substList.example.name}"};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}
	
	@Override
	protected void initExample() {
		Subst subst=new Subst();
		List<User> users=new ArrayList<User>();
		users.add(new User());
		subst.setUsers(users);
		setExample(subst);
	}
	
}
