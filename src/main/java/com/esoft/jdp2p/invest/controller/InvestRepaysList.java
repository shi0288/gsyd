package com.esoft.jdp2p.invest.controller;
import java.io.Serializable;
import java.util.Arrays;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.core.annotations.ScopeType;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.repay.model.InvestRepay;
/**
 * 
 * 我的投资-还款计划
 * @author ada
 *
 */
@Component
@Scope(ScopeType.VIEW)
public class InvestRepaysList extends EntityQuery<InvestRepay> implements Serializable{
	private static final String lazyModelCountHql = "select count(distinct repay) from InvestRepay repay";
	private static final String lazyModelHql = "select distinct repay from InvestRepay repay";
	
	public InvestRepaysList() {
		setCountHql(lazyModelCountHql);
		setHql(lazyModelHql);
		final String[] RESTRICTIONS = {
				"repay.invest.id like #{investRepaysList.example.invest.id}"
				};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}
	@Override
	protected void initExample() {
		InvestRepay example = new InvestRepay();
		Invest loan = new Invest();
		example.setInvest(loan);
		setExample(example);
	}
}
