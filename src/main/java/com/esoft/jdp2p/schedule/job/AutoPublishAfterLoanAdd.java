package com.esoft.jdp2p.schedule.job;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.LoanService;

/**
 * Company: jdp2p <br/>
 * Copyright: Copyright (c)2013 <br/>
 * Description: 借款审核通过以后，开启当前借款的自动投标
 * 
 * @author: guoyw 2015-03-14
 */
@Component
public class AutoPublishAfterLoanAdd implements Job {

	public static final String LOAN_ID = "apala_loan_id";

	@Resource
	private LoanService loanService;
	@Resource
	private HibernateTemplate ht;

	Log log = LogFactory.getLog(AutoPublishAfterLoanAdd.class);

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		try {
			String loanId = context.getJobDetail().getJobDataMap()
					.getString(LOAN_ID);
			if (log.isDebugEnabled()) {
				log.debug("AutoPublishAfterLoanAdd, loanId: " + loanId);
			}
			Loan loan = ht.get(Loan.class, loanId);
			loanService.passApplyPublish(loan);
		} catch (InsufficientBalance e) {
			if (log.isDebugEnabled()) {
				log.debug("AutoPublishAfterLoanAdd, InsufficientBalance: " + e);
			}
		}
	}
}
