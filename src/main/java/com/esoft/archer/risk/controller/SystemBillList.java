package com.esoft.archer.risk.controller;

import java.util.Arrays;
import java.util.Date;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.risk.model.SystemBill;
import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;

@Component
@Scope(ScopeType.VIEW)
public class SystemBillList extends EntityQuery<SystemBill> {

	@Resource
	private SystemBillService ssb;
	/**交易发生时间*/
	private String time;
	private String loanId;
	private Date startTime;
	private Date endTime;
	
	public SystemBillList() {
		final String[] RESTRICTIONS = { "id like #{systemBillList.example.id}",
				"type = #{systemBillList.example.type}",
				"reason = #{systemBillList.example.reason}",
				"time >= #{systemBillList.startTime}",
				"time <= #{systemBillList.endTime}",
				"detail like #{systemBillList.loanId}"
				};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}

	public double getBalance(){
		return ssb.getBalance();
	}
	
	public double getSumInByType(String type) {
		String hql = "select sum(sb.money) from SystemBill sb where sb.type =?";
		Double sum = (Double) getHt().find(hql, type).get(0);
		if (sum == null) {
			return 0;
		}
		return sum.doubleValue();
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
	
	public String getTime(){
		return this.time;
	}
	
	public void setTime(String time) {
		Date date;
		Date dateLast;
		if(time.length() == 7){
			date = DateUtil.StringToDate(time, DateStyle.YYYY_MM);
			dateLast = DateUtil.addSecond(DateUtil.addMonth(date, 1), -1);
		}else{
			date = DateUtil.StringToDate(time, DateStyle.YYYY_MM_DD);
			dateLast = DateUtil.addSecond(DateUtil.addDay(date, 1), -1);
		}
		this.setStartTime(date);
		this.setEndTime(dateLast);
		this.time = time;
	}

	public String getLoanId() {
		return loanId;
	}

	public void setLoanId(String loanId) {
		this.loanId = "%项目ID:"+loanId+"%";
	}

}
