package com.esoft.jdp2p.invest.controller;

import java.util.Arrays;
import java.util.Date;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.util.DateUtil;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.model.TransferApply;
import com.esoft.jdp2p.loan.model.Loan;

@Component
@Scope(ScopeType.VIEW)
public class TransferApplyList extends EntityQuery<TransferApply> {

	private Double minCorpus;
	private Double maxCorpus;
	private Double maxRate;
	private Double minRate;
	private Double maxPremium;
	private Double minPremium;
	private Date beginTime;
	private Date endTime;
	private Date endZeroTime;

	public void setMinAndMaxCorpus(double minCorpus, double maxCorpus) {
		this.minCorpus = minCorpus;
		this.maxCorpus = maxCorpus;
	}

	public void setMinAndMaxRate(double minRate, double maxRate) {
		this.minRate = minRate;
		this.maxRate = maxRate;
	}

	public void setMinAndMaxPremium(double minPremium, double maxPremium) {
		this.minPremium = minPremium;
		this.maxPremium = maxPremium;
	}

	public TransferApplyList() {
		final String[] RESTRICTIONS = {
				"transferApply.status like #{transferApplyList.example.status}",
				"transferApply.corpus >= #{transferApplyList.minCorpus}",
				"transferApply.corpus <= #{transferApplyList.maxCorpus}",
				"transferApply.invest.rate <= #{transferApplyList.maxRate}",
				"transferApply.invest.rate >= #{transferApplyList.minRate}",
				"transferApply.invest.loan.id = #{transferApplyList.example.invest.loan.id}",
				"transferApply.invest.loan.name like #{transferApplyList.example.invest.loan.name}",
				"transferApply.premium <= #{transferApplyList.maxPremium}",
				"transferApply.premium >= #{transferApplyList.minPremium}",
				"transferApply.applyTime >= #{transferApplyList.beginTime}",
				"transferApply.applyTime < #{transferApplyList.endZeroTime}",
				"transferApply.invest.user.subst.userId like #{transferApplyList.example.invest.user.subst.userId}",
				"transferApply.invest.user.username like #{transferApplyList.example.invest.user.username}" };
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}

	@Override
	protected void initExample() {
		TransferApply ta = new TransferApply();
		Invest i = new Invest();
		i.setLoan(new Loan());
		i.setUser(new User());
		User user = new User();
		Subst subst = new Subst();
		user.setSubst(subst);
		i.setUser(user);
		ta.setInvest(i);
		this.setExample(ta);
	}

	public Double getMinCorpus() {
		return minCorpus;
	}

	public void setMinCorpus(Double minCorpus) {
		this.minCorpus = minCorpus;
	}

	public Double getMaxCorpus() {
		return maxCorpus;
	}

	public void setMaxCorpus(Double maxCorpus) {
		this.maxCorpus = maxCorpus;
	}

	public Double getMaxRate() {
		return maxRate;
	}

	public void setMaxRate(Double maxRate) {
		this.maxRate = maxRate;
	}

	public Double getMinRate() {
		return minRate;
	}

	public void setMinRate(Double minRate) {
		this.minRate = minRate;
	}

	public Double getMaxPremium() {
		return maxPremium;
	}

	public void setMaxPremium(Double maxPremium) {
		this.maxPremium = maxPremium;
	}

	public Double getMinPremium() {
		return minPremium;
	}

	public void setMinPremium(Double minPremium) {
		this.minPremium = minPremium;
	}

	public Date getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(Date beginTime) {
		this.beginTime = beginTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Date getEndZeroTime() {
		if (this.endTime != null) {
			return DateUtil.addDay(endTime, 1);
		}
		return endZeroTime;
	}

	public void setEndZeroTime(Date endZeroTime) {
		this.endZeroTime = endZeroTime;
	}

}
