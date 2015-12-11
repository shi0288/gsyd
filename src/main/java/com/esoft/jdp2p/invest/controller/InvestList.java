package com.esoft.jdp2p.invest.controller;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.util.HttpClientUtil;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.model.TransferApply;
import com.esoft.jdp2p.loan.model.Loan;

/**
 * Filename: InvestList.java Description: Copyright: Copyright (c)2013 Company:
 * jdp2p
 * 
 * @author: yinjunlu
 * @version: 1.0 Create at: 2014-1-11 下午4:27:32
 * 
 *           Modification History: Date Author Version Description
 *           ------------------------------------------------------------------
 *           2014-1-11 yinjunlu 1.0 1.0 Version
 */
@Component
@Scope(ScopeType.VIEW)
public class InvestList extends EntityQuery<Invest> implements Serializable{

	private Date searchcommitMinTime;
	private Date searchcommitMaxTime;

	private static final String lazyModelCountHql = "select count(distinct invest) from Invest invest";
	private static final String lazyModelHql = "select distinct invest from Invest invest";
	

	public InvestList() {
		setCountHql(lazyModelCountHql);
		setHql(lazyModelHql);
		final String[] RESTRICTIONS = {
				"invest.id like #{investList.example.id}",
				"invest.status like #{investList.example.status}",
				"invest.loan.user.id like #{investList.example.loan.user.id}",
				"invest.loan.id like #{investList.example.loan.id}",
				"invest.loan.name like #{investList.example.loan.name}",
				"invest.loan.type like #{investList.example.loan.type}",
				"invest.user.id = #{investList.example.user.id}",
				"invest.user.referrer = #{investList.example.user.referrer}",
				"invest.user.username = #{investList.example.user.username}",
				"invest.time >= #{investList.searchcommitMinTime}",
				"invest.status like #{investList.example.status}",
				"invest.time <= #{investList.searchcommitMaxTime}",
				"invest.user.subst.userId like #{investList.example.user.subst.userId}",
				"invest.transferApply.id = #{investList.example.transferApply.id}"};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}

	@Override
	protected void initExample() {
		Invest example = new Invest();
		Loan loan = new Loan();
		User user=new User();
		user.setSubst(new Subst());
		User user2 = new User();
		user2.setSubst(new Subst());
		loan.setUser(user);
		example.setUser(user2);
		example.setLoan(loan);
		example.setTransferApply(new TransferApply());
		setExample(example);
	}

	public Object getSumMoney(){
		
		final String hql = parseHql("Select sum(invest.money) from Invest invest");
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
	
	
	/**
	 * 本次投资排名情况
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Object getInvestRanking(String loanId , String investMoney){
		if(null==investMoney||"".equals(investMoney)){
			return 0;
		}
		String hql = "from Invest invest where invest.loan.id=? and invest.money>=?";
		List<Invest> Invests = getHt().find(hql,new Object[]{loanId,Double.parseDouble(investMoney)});
		if(null!=Invests&&Invests.size()>0){
			return Invests.size()+1;
		}else{
			return 1;
		}
	}
	
	
	public Date getSearchcommitMinTime() {
		return searchcommitMinTime;
	}

	public void setSearchcommitMinTime(Date searchcommitMinTime) {
		this.searchcommitMinTime = searchcommitMinTime;
	}

	public Date getSearchcommitMaxTime() {
		return searchcommitMaxTime;
	}

	public void setSearchcommitMaxTime(Date searchcommitMaxTime) {
		this.searchcommitMaxTime = searchcommitMaxTime;
	}

	/**
	 * 设置查询的起始和结束时间
	 */
	public void setSearchStartEndTime(Date startTime, Date endTime) {
		this.searchcommitMinTime = startTime;
		this.searchcommitMaxTime = endTime;
	}

}
