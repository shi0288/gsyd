package com.esoft.archer.user.controller;

import java.util.Arrays;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.model.UserLoginLog;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;
import com.esoft.core.util.StringManager;

@Component
@Scope(ScopeType.VIEW)
public class UserLoginLogList extends EntityQuery<UserLoginLog> {

	private static StringManager sm = StringManager
			.getManager(UserConstants.Package);

	@Logger
	private static Log log ;
	private String startTime;
	private String endTime;
	public UserLoginLogList() {
		final String[] RESTRICTIONS = { 
				"username like #{userLoginLogList.example.username}",
				"loginIp like #{userLoginLogList.example.loginIp}",
				"isSuccess = #{userLoginLogList.example.isSuccess}",
				"loginTime >= #{userLoginLogList.loginTimeStart}",
				"loginTime <= #{userLoginLogList.loginTimeEnd}"};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}

	private Date loginTimeStart;
	private Date loginTimeEnd;

	public Date getLoginTimeStart() {
		return loginTimeStart;
	}
	public void setLoginTimeStart(Date loginTimeStart) {
		this.loginTimeStart = loginTimeStart;
	}
	public Date getLoginTimeEnd() {
		return loginTimeEnd;
	}
	public void setLoginTimeEnd(Date loginTimeEnd) {
		this.loginTimeEnd = loginTimeEnd;
	}
	public String getStartTime(){
		return this.startTime;
	}
	public void setStartTime(String startTime) {
		this.loginTimeStart = DateUtil.StringToDate(startTime, startTime.length()==7?DateStyle.YYYY_MM:DateStyle.YYYY_MM_DD);
		this.startTime = startTime;
	}
	public String getEndTime(){
		return this.endTime;
	}
	public void setEndTime(String endTime) {
		Date date;
		if(endTime.length() == 7){
			date = DateUtil.addSecond(DateUtil.addMonth(DateUtil.StringToDate(endTime, DateStyle.YYYY_MM),1),-1);
		}else{
			date = DateUtil.addSecond(DateUtil.addDay(DateUtil.StringToDate(endTime, DateStyle.YYYY_MM_DD),1),-1);
		}
		this.loginTimeEnd = date;
		this.endTime = endTime;
	}

	
	


}
