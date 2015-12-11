package com.esoft.jdp2p.borrower.controller;

import java.util.Arrays;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.ScopeType;
import com.esoft.jdp2p.borrower.model.BorrowerInfo;

@Component
@Scope(ScopeType.VIEW)
public class BorrowerInfoList extends EntityQuery<BorrowerInfo> {

	public BorrowerInfoList(){
		final String[] RESTRICTIONS = {"borrowerInfo.userId like #{borrowerInfoList.example.userId}",
				"borrowerInfo.user.subst.userId like #{borrowerInfoList.example.user.subst.userId}"};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}
	
	@Override
	protected void initExample() {
		BorrowerInfo li = new BorrowerInfo();
		User user = new User();
		Subst subst=new Subst();
		user.setSubst(subst);
		li.setUser(user);
		setExample(li);
	}
}
