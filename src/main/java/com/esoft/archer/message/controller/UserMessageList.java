package com.esoft.archer.message.controller;

import java.util.Arrays;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.message.model.UserMessage;
import com.esoft.archer.message.model.UserMessageTemplate;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.ScopeType;

@Component
@Scope(ScopeType.VIEW)
public class UserMessageList extends EntityQuery<UserMessage> {

	public UserMessageList() {
		final String[] RESTRICTIONS = {
				"user.id like #{userMessageList.example.user.id}",
				"userMessageTemplate.name like #{userMessageList.example.userMessageTemplate.name}" };
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}

	@Override
	protected void initExample() {
		UserMessage um = new UserMessage();
		um.setUser(new User());
		um.setUserMessageTemplate(new UserMessageTemplate());
		this.setExample(um);
	}
}
