package com.esoft.archer.user.controller;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.user.model.Recharge;
import com.esoft.archer.user.model.RechargeBankCard;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.RechargeService;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;

/**
 * 充值查询
 * 
 */
@Component
@Scope(ScopeType.VIEW)
public class RechargeList extends EntityQuery<Recharge> implements
		java.io.Serializable {
 
	private static final long serialVersionUID = 9057256750216810237L;

	@Logger
	static Log log;

	@Resource
	private RechargeService rechargeService;

	private List<RechargeBankCard> rechargeBankCards;
	private String commitDate;
	private Date startTime ;
	private Date endTime ;

	public RechargeList() {
		final String[] RESTRICTIONS = { "id like #{rechargeList.example.id}",
				"time >= #{rechargeList.startTime}",
				"time <= #{rechargeList.endTime}",
				"status = #{rechargeList.example.status}",
				"rechargeWay like #{rechargeList.example.rechargeWay}",
				"user.username like #{rechargeList.example.user.username}",
				"user.subst.userId like #{rechargeList.example.user.subst.userId}"};

		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
//		addOrder("time", super.DIR_DESC);
	}

	@Override
	protected void initExample() {
		super.initExample();
		User user = new User();
		Subst subst = new Subst();
		user.setSubst(subst);
		getExample().setUser(user);
	}

	public List<RechargeBankCard> getRechargeBankCards() {
		if (this.rechargeBankCards == null) {
			this.rechargeBankCards = rechargeService.getBankCardsList();
		}
		return this.rechargeBankCards;
	}

	public Double getSumActualMoney(){
		final String hql = parseHql("Select sum(actualMoney) from Recharge");
		@SuppressWarnings("unchecked")
		List<Double> resultList = getHt().execute(new HibernateCallback<List<Double>>() {

			public List<Double> doInHibernate(Session session)
			
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
	
	//~~~~~~~~~~~~~~~~
	
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getEndTime() {
		return endTime;
	}
	public String getCommitDate() {
		return commitDate;
	}

	public void setCommitDate(String commitDate) {
		Date date;
		Date dateLast;
		if(commitDate.length() == 7){
			date = DateUtil.StringToDate(commitDate, DateStyle.YYYY_MM);
			dateLast = DateUtil.addSecond(DateUtil.addMonth(date, 1), -1);
		}else{
			date = DateUtil.StringToDate(commitDate, DateStyle.YYYY_MM_DD);
			dateLast = DateUtil.addSecond(DateUtil.addDay(date, 1), -1);
		}
		this.setStartTime(date);
		this.setEndTime(dateLast);
		this.commitDate = commitDate;
	}

}
