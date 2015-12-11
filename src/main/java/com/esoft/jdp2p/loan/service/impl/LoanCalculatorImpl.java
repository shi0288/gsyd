package com.esoft.jdp2p.loan.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.hibernate.ObjectNotFoundException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;

import com.esoft.archer.common.exception.NoMatchingObjectsException;
import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.risk.FeeConfigConstants.FeePoint;
import com.esoft.archer.risk.FeeConfigConstants.FeeType;
import com.esoft.archer.risk.model.RiskRank;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.core.util.ArithUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.loan.LoanConstants.LoanStatus;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.LoanCalculator;
import com.esoft.jdp2p.repay.RepayConstants.RepayType;
import com.esoft.jdp2p.repay.exception.IllegalLoanTypeException;
import com.esoft.jdp2p.repay.model.LoanRepay;
import com.esoft.jdp2p.repay.model.Repay;
import com.esoft.jdp2p.repay.service.RepayCalculator;
import com.esoft.jdp2p.repay.service.impl.NormalRepayCPMCalculator;
import com.esoft.jdp2p.repay.service.impl.NormalRepayRFCLCalculator;
import com.esoft.jdp2p.repay.service.impl.NormalRepayRLIOCalculator;

@Service("loanCalculator")
public class LoanCalculatorImpl implements LoanCalculator {

	@Resource
	HibernateTemplate ht;

	@Resource
	FeeConfigBO feeConfigBO;

	@Resource
	NormalRepayRFCLCalculator normalRepayRFCLCalculator;

	@Resource
	NormalRepayCPMCalculator normalRepayCPMCalculator;

	@Resource
	NormalRepayRLIOCalculator normalRepayRLIOCalculator;

	@Resource
	RepayCalculator repayCalculator;
	@Resource
	ConfigService configService;

	@Override
	public Double calculateCashDeposit(double loanMoney) {
		return feeConfigBO.getFee(FeePoint.APPLY_LOAN, FeeType.CASH_DEPOSIT, null, null, loanMoney);
	}

	@Override
	public Double calculateRaiseCompletedRate(String loanId) throws NoMatchingObjectsException {
		double remainMoney = calculateMoneyNeedRaised(loanId);
		Loan loan = ht.get(Loan.class, loanId);
		double loanMoney = loan.getLoanMoney();
		return ArithUtil.round((loanMoney - remainMoney) / loanMoney * 100, 2);
	}

	@Override
	public String calculateRemainTime(String loanId) throws NoMatchingObjectsException {
		Loan loan = ht.get(Loan.class, loanId);
		// FIXME:loan 为空验证
		if (loan == null) {
			throw new NoMatchingObjectsException(Loan.class, "loanId:" + loanId);
		}
		if (loan.getExpectTime() == null) {
			return "未开始";
		}
		Long time = (loan.getExpectTime().getTime() - System.currentTimeMillis()) / 1000;

		if (time < 0 || !loan.getStatus().equals(LoanStatus.RAISING)) {
			return "已到期";
		}
		long days = time / 3600 / 24;
		long hours = (time / 3600) % 24;
		long minutes = (time / 60) % 60;
		if (minutes < 1) {
			minutes = 1L;
		}

		return days + "天" + hours + "时" + minutes + "分";
	}

	@Override
	public Double calculateMoneyNeedRaised(String loanId) throws NoMatchingObjectsException {
		Loan loan = (Loan) this.ht.get(Loan.class, loanId);
		if (loan == null) {
			throw new NoMatchingObjectsException(Loan.class, "cannot find loan by loanId:" + loanId);
		}

		double sumMoney = calculateMoneyRaised(loanId).doubleValue();
		double remain = ArithUtil.sub(loan.getLoanMoney().doubleValue(), sumMoney);

		return Double.valueOf(remain < 0.0D ? 0.0D : remain);
	}

	public Double calculateMoneyRaised(String loanId) throws NoMatchingObjectsException {
		Loan loan = (Loan) this.ht.get(Loan.class, loanId);
		if (loan == null) {
			throw new NoMatchingObjectsException(Loan.class, "cannot find loan by loanId:" + loanId);
		}

		String freezeMoney;
		try {
			freezeMoney = this.configService.getConfigValue("freeze_money");
		} catch (ObjectNotFoundException e) {			
			freezeMoney = "";
		}
		List investMoney=null;
		if ("0".equals(freezeMoney))
			investMoney = this.ht.find("select sum(invest.investMoney) from Invest invest where invest.loan.id=? and invest.status not in (?,?)", new String[] { loanId, "cancel", "wait_affirm" });
		else {
			investMoney = this.ht.find("select sum(invest.investMoney) from Invest invest where invest.loan.id=? and invest.status !=?", new String[] { loanId, "cancel" });
		}

		double sumMoney = investMoney.get(0) == null ? 0.0D : ((Double) investMoney.get(0)).doubleValue();
		return Double.valueOf(sumMoney);
	}

	@Override
	public Double calculateMoneyMaxInvested(String loanId) throws NoMatchingObjectsException {
		Loan loan = ht.get(Loan.class, loanId);
		if (loan == null) {
			throw new NoMatchingObjectsException(Loan.class, "cannot find loan by loanId:" + loanId);
		}
		Double needRaised = calculateMoneyNeedRaised(loanId);
		Double maxInvested = loan.getMaxInvestMoney();

		return needRaised < maxInvested ? needRaised : maxInvested;
	}

	@Override
	public Long countSuccessInvest(String loanId) {
		Object o = ht.find("select count(im) from Invest im where im.loan.id = ? and  im.status in (?,?,?,?,?)", new String[] { loanId, InvestConstants.InvestStatus.BID_SUCCESS, InvestConstants.InvestStatus.COMPLETE, InvestConstants.InvestStatus.OVERDUE, InvestConstants.InvestStatus.BAD_DEBT, InvestConstants.InvestStatus.REPAYING }).get(0);
		if (o == null) {
			return 0L;
		}
		return (Long) o;
	}

	@Override
	public RiskRank calculateRiskRank(String loanId) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Double calculateLoanInterest(String loanId) {
		Loan loan = ht.get(Loan.class, loanId);
		// 如果借款已经有还款生成，则直接取还款数据
		List<LoanRepay> lrs = loan.getLoanRepays();
		Double sumInterest = 0D;
		if (lrs.size() > 0) {
			for (LoanRepay loanRepay : lrs) {
				sumInterest = ArithUtil.add(sumInterest, loanRepay.getInterest());
			}
		} else {
			// 计算借款的预计利息
			sumInterest = repayCalculator.calculateAnticipatedInterest(loan.getLoanMoney(), loanId);
		}
		return sumInterest;
	}

	@Override
	public List<Repay> calculateAnticipatedRepays(Loan loan) {
		if (loan.getType() == null) {
			return null;
		}
		Date interestBeginTime = loan.getInterestBeginTime() == null ? loan.getExpectTime() : loan.getInterestBeginTime();
		Date investTime = new Date();
		// 先付利息后还本金
		if (loan.getType().getRepayType().equals(RepayType.RFCL)) {
			// 如果是即投即生息，则为T+1计息，从计息日开始计息，所以investTime的值为（计息日-1天）
			return normalRepayRFCLCalculator.generateRepays(loan.getLoanMoney(), investTime, loan.getRate(), loan.getRateIBOI(), loan.getDeadline(), loan.getType().getRepayTimeUnit(), loan.getType().getRepayTimePeriod(), interestBeginTime, loan.getType().getInterestType(), loan.getType().getInterestPoint(), null);
		} else if (loan.getType().getRepayType().equals(RepayType.CPM)) {
			return normalRepayCPMCalculator.generateRepays(loan.getLoanMoney(), investTime, loan.getRate(), loan.getRateIBOI(), loan.getDeadline(), loan.getType().getRepayTimeUnit(), loan.getType().getRepayTimePeriod(), interestBeginTime, loan.getType().getInterestType(), loan.getType().getInterestPoint(), null);
		} else if (loan.getType().getRepayType().equals(RepayType.RLIO)) {
			return normalRepayRLIOCalculator.generateRepays(loan.getLoanMoney(), investTime, loan.getRate(), loan.getRateIBOI(), loan.getDeadline(), loan.getType().getRepayTimeUnit(), loan.getType().getRepayTimePeriod(), interestBeginTime, loan.getType().getInterestType(), loan.getType().getInterestPoint(), null);
		} else {
			throw new IllegalLoanTypeException("RepayType: " + loan.getType().getRepayType() + ". 不支持该还款类型。");
		}
	}

	@Override
	public Double calculateAnticipatedInterest(Loan loan) {
		List<Repay> repays = calculateAnticipatedRepays(loan);
		if (repays == null) {
			return null;
		}
		double interest = 0D;
		for (Repay repay : repays) {
			interest = ArithUtil.add(interest, repay.getInterest());
		}
		return interest;
	}

}
