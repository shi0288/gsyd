package com.esoft.jdp2p.repay.controller;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.repay.model.LoanRepay;

@Component
@Scope(ScopeType.VIEW)
public class LoanRepayList extends EntityQuery<LoanRepay> {
	@Logger
	static Log log;

	private Date searchMinTime;
	private Date searchMaxTime;
	private String overdueDate;
	public LoanRepayList() {
		final String[] RESTRICTIONS = { "id like #{loanRepayList.example.id}",
				"loan.id like #{loanRepayList.example.loan.id}",
				"loan.user.id = #{loanRepayList.example.loan.user.id}",
				"repayDay >= #{loanRepayList.searchMinTime}",
				"repayDay <= #{loanRepayList.searchMaxTime}",
				"loan.user.subst.userId like #{loanRepayList.example.loan.user.subst.userId}",
				"status like #{loanRepayList.example.status}" };
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}

	@Override
	protected void initExample() {
		LoanRepay example = new LoanRepay();
		Loan loan = new Loan();
		User user=new User();
		Subst subst=new Subst();
		user.setSubst(subst);
		loan.setUser(user);
		example.setLoan(loan);
		setExample(example);
	}
	
	public Object getSumMoney(){
		final String hql = parseHql("Select sum(corpus),sum(interest),sum(fee),sum(defaultInterest) from LoanRepay");
		@SuppressWarnings("unchecked")
		List<Object> resultList = getHt().execute(new HibernateCallback<List<Object>>() {

			public List<Object> doInHibernate(Session session)
			
					throws HibernateException, SQLException {
				Query query = session.createQuery(hql);
				// 从第0行开始
				query.setFirstResult(0);
				query.setMaxResults(5);
				for (int i = 0; i < getParameterValues().length; i++) {
					query.setParameter(i, getParameterValues()[i]);
				}
				return query.list();
			}

		});
		
		if(resultList != null && resultList.get(0) != null){
			return resultList.get(0);
		}
		return 0D;
	}

	public Date getSearchMinTime() {
		return searchMinTime;
	}

	public void setSearchMinTime(Date searchMinTime) {
		this.searchMinTime = searchMinTime;
	}

	public Date getSearchMaxTime() {
		return searchMaxTime;
	}

	public void setSearchMaxTime(Date searchMaxTime) {
		this.searchMaxTime = searchMaxTime;
	}

	public String getOverdueDate() {
		return overdueDate;
	}

	public void setOverdueDate(String overdueDate) {
		Date date;
		Date tomrrow;
		if(overdueDate.length() == 7){
			date = DateUtil.StringToDate(overdueDate, DateStyle.YYYY_MM);
			tomrrow = DateUtil.addMonth(date, 1);
		}else{
			date = DateUtil.StringToDate(overdueDate, DateStyle.YYYY_MM_DD);
			tomrrow = DateUtil.addDay(date, 1);
		}
		this.addRestriction("(DATE_FORMAT(repayDay, '%Y-%m-%d') < DATE_FORMAT(coalesce(time, now()), '%Y-%m-%d')) and ((repayDay > '"+dateToStr(date)+"' and repayDay < '"+dateToStr(tomrrow)+"') or (time > '"+dateToStr(date)+"' and time < '"+dateToStr(tomrrow)+"') or (repayDay < '"+dateToStr(date)+"' and (time > '"+dateToStr(tomrrow)+"' or time is null)))");
		this.overdueDate = overdueDate;
	}
	/**转化为 yyyy-MM-dd HH:mm:ss格式*/
	private String dateToStr(Date date){
		return DateUtil.DateToString(date, DateStyle.YYYY_MM_DD_HH_MM_SS);
	}
}
