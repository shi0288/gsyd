package com.esoft.archer.statistics.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.esoft.archer.user.service.UserBillService;
import com.esoft.core.annotations.ScopeType;

/**
 * 账户（单）统计
 * 
 * @author Administrator
 * 
 */
@Component
@Scope(ScopeType.REQUEST)
public class BillStatistics {

	@Resource
	private UserBillService ubs;
	@Resource
	private HibernateTemplate ht;
	/**
	 * 获取用户账户余额
	 * 
	 * @return
	 */
	public double getBalanceByUserId(String userId) {
		return ubs.getBalance(userId);
	}

	/**
	 * 获取用户账户冻结金额
	 * @param userId
	 * @return
	 */
	public double getFrozenMoneyByUserId(String userId) {
		return ubs.getFrozenMoney(userId);
	}
	
	/**
	 * 比某人更富的人数
	 */
	public long getCountRicher(String userId){
		String hql = "select count(ub.id) from UserBill ub, UserBillMaxView c where ub.user.id= c.userId and ub.seqNum=c.maxSeqNum and (ub.balance+ub.frozenMoney)>coalesce((select ub1.balance + ub1.frozenMoney from UserBill ub1, UserBillMaxView c1 where ub1.user.id= c1.userId and ub1.seqNum=c1.maxSeqNum and ub1.user.id = ?),0)";
		return (Long) ht.find(hql,userId).get(0);
	}
	/**
	 * 比某人更穷的人数
	 * 总人数减更富的人数减1，避免有人没钱的情况
	 */
	public long getCountPoorer(String userId){
		long count = (Long) ht.find("select count(user) from User user").get(0);
		return count-getCountRicher(userId)-1;
	}
	
}
