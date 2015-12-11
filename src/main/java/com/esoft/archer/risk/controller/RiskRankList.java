package com.esoft.archer.risk.controller;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.risk.model.RiskRank;
import com.esoft.core.annotations.ScopeType;

@Component
@Scope(ScopeType.VIEW)
public class RiskRankList extends EntityQuery<RiskRank> implements Serializable{

	public RiskRankList(){
		final String[] RESTRICTIONS = {"id like #{riskRankList.example.id}",
				"rank like #{riskRankList.example.rank}",
				};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}
	/* (non-Javadoc)
	 * @see com.esoft.archer.common.controller.EntityQuery#getAllResultList()
	 */
	@Override
	public List<RiskRank> getAllResultList() {
		if (this.allResultList == null) {
			this.allResultList = super.getAllResultList();
			Collections.sort(this.allResultList, new Comparator<RiskRank>() {
				@Override
				public int compare(RiskRank o1, RiskRank o2) {
					return o1.getScore()-o2.getScore();
				}
			});
		}
		return this.allResultList;
	}
}
