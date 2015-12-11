package com.esoft.jdp2p.repay.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.risk.FeeConfigConstants.FeePoint;
import com.esoft.archer.risk.FeeConfigConstants.FeeType;
import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;
import com.esoft.jdp2p.invest.InvestConstants.TransferStatus;
import com.esoft.jdp2p.invest.model.TransferApply;
import com.esoft.jdp2p.invest.service.TransferService;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.LoanConstants.LoanStatus;
import com.esoft.jdp2p.loan.LoanConstants.RepayStatus;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.LoanService;
import com.esoft.jdp2p.repay.exception.AdvancedRepayException;
import com.esoft.jdp2p.repay.exception.NormalRepayException;
import com.esoft.jdp2p.repay.exception.OverdueRepayException;
import com.esoft.jdp2p.repay.exception.RepayException;
import com.esoft.jdp2p.repay.model.InvestRepay;
import com.esoft.jdp2p.repay.model.LoanRepay;


@Component("repayByAminService")
public class RepayByAminServiceImpl{

	@Logger
	static Log log;
	@Resource
	HibernateTemplate ht;
	@Resource
	UserBillBO userBillBO;
	@Resource
	SystemBillService systemBillService;
	@Resource
	TransferService transferService;
	@Resource
	LoanService loanService;
	@Resource
	FeeConfigBO feeConfigBO;
	
	@SuppressWarnings({ "unchecked", "deprecation" })
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void normalRepayByAdmin(String repayId) throws InsufficientBalance,
			NormalRepayException {
		LoanRepay repay = ht.get(LoanRepay.class, repayId);
		ht.evict(repay);
		repay = ht.get(LoanRepay.class, repay.getId(), LockMode.UPGRADE);
		
		// 正常还款
		if (!(repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING) 
			|| repay.getStatus().equals(LoanConstants.RepayStatus.WAIT_REPAY_VERIFY))) {
			// 该还款不处于正常还款状态。
			throw new NormalRepayException("还款：" + repay.getId() + "不处于正常还款状态。");
		}
		
		List<InvestRepay> irs = ht.find("from InvestRepay ir where ir.invest.loan.id=? and ir.period=?"
										,new Object[] { repay.getLoan().getId(),repay.getPeriod() });
		
		// 更改投资的还款信息
		for (InvestRepay ir : irs) {

			ir.setStatus(LoanConstants.RepayStatus.COMPLETE);
			ir.setTime(new Date());
			ht.update(ir);

			userBillBO.transferIntoBalance(ir.getInvest().getUser().getId()
										   ,ArithUtil.add(ir.getCorpus(), ir.getInterest())
										   ,OperatorInfo.NORMAL_REPAY
										   ,"投资：" + ir.getInvest().getId() + "收到系统代偿还款, 还款ID:"
										   + repay.getId() + "  借款ID:"
										   + repay.getLoan().getId() + "  本金："
										   + ir.getCorpus() + "  利息：" + ir.getInterest());
			// 投资者手续费
			userBillBO.transferOutFromBalance(ir.getInvest().getUser().getId()
											  ,ir.getFee(), OperatorInfo.NORMAL_REPAY, "投资："
											  + ir.getInvest().getId() + "收到系统代偿还款，扣除手续费, 还款ID:"
											  + repay.getId());
			systemBillService.transferInto(ir.getFee()
										   ,OperatorInfo.NORMAL_REPAY, "投资：" + ir.getInvest().getId()
										   + "收到系统代偿还款，扣除手续费, 还款ID:" + repay.getId()+",项目ID:"+ir.getInvest().getLoan().getId());

		}
		
		
		try {
			cancelTransfering(repay.getLoan().getId());
		} catch (RepayException e) {
			throw new NormalRepayException(e.getMessage(), e.getCause());
		}
		
		
		// 更改借款的还款信息
		double payMoney = ArithUtil.add(ArithUtil.add(repay.getCorpus(), repay.getInterest()),repay.getFee());
		repay.setTime(new Date());
		repay.setStatus(LoanConstants.RepayStatus.COMPLETE);

		// 系统商户的账户，扣除还款。
		systemBillService.transferOut(payMoney,
									  OperatorInfo.NORMAL_REPAY,
									  "借款：" + repay.getLoan().getId() + "系统代偿正常还款, 还款ID：" + repay.getId()
									  + " 本金：" + repay.getCorpus() + "  利息："
									  + repay.getInterest() + "  手续费：" + repay.getFee());
		// 借款者手续费
		systemBillService.transferInto(repay.getFee(),
									   OperatorInfo.NORMAL_REPAY, "项目ID:" + repay.getLoan().getId()
									   + "系统代偿正常还款，扣除手续费， 还款ID：" + repay.getId());

		ht.merge(repay);
		// 判断是否所有还款结束，更改等待还款的投资状态和还款状态，还有项目状态。
		loanService.dealComplete(repay.getLoan().getId());

	}

	/**
	 * 系统代偿，提前还款
	 * @param loanId
	 * @throws InsufficientBalance
	 * @throws AdvancedRepayException
	 */
	@SuppressWarnings({ "deprecation", "unchecked" })
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void advanceRepayByAdmin(String loanId) throws InsufficientBalance,
			AdvancedRepayException {
		Loan loan = ht.get(Loan.class, loanId);
		ht.evict(loan);
		loan = ht.get(Loan.class, loanId, LockMode.UPGRADE);
		if (loan.getStatus().equals(LoanStatus.REPAYING) || loan.getStatus().equals(LoanStatus.WAIT_REPAY_VERIFY)) {
			// 查询当期还款是否已还清
			String repaysHql = "select lr from LoanRepay lr where lr.loan.id = ?";
			List<LoanRepay> repays = ht.find(repaysHql, loanId);
			// 剩余所有本金
			double sumCorpus = 0D;
			// 手续费总额
			double feeAll = 0D;
			for (LoanRepay repay : repays) {
				if (repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING)
					|| repay.getStatus().equals(RepayStatus.WAIT_REPAY_VERIFY)) {
					// 在还款期，而且没还款
					if (isInRepayPeriod(repay.getRepayDay())) {
						// 有未完成的当期还款。
						throw new AdvancedRepayException("当期还款未完成");
					} else {
						sumCorpus = ArithUtil.add(sumCorpus, repay.getCorpus());
						feeAll = ArithUtil.add(feeAll, repay.getFee());
						repay.setTime(new Date());
						repay.setStatus(LoanConstants.RepayStatus.COMPLETE);
					}
				} else if (repay.getStatus().equals(LoanConstants.RepayStatus.BAD_DEBT)
						|| repay.getStatus().equals(LoanConstants.RepayStatus.OVERDUE)) {
					// 还款中存在逾期或者坏账
					throw new AdvancedRepayException("还款中存在逾期或者坏账");
				}
			}
			
			
			// 给投资人的罚金
			double feeToInvestor = feeConfigBO.getFee(
					FeePoint.ADVANCE_REPAY_INVESTOR, FeeType.PENALTY, null,
					null, sumCorpus);
			// 给系统的罚金
			double feeToSystem = feeConfigBO.getFee(
					FeePoint.ADVANCE_REPAY_SYSTEM, FeeType.PENALTY, null, null,
					sumCorpus);

			double sumPay = ArithUtil.add(feeToInvestor, feeToSystem,sumCorpus, feeAll);
			// 扣除还款金额+罚金
			systemBillService.transferOut(sumPay,
					OperatorInfo.ADVANCE_REPAY, "借款：" + loan.getId()
							+ "系统代偿提前还款，本金：" + sumCorpus + "，用户罚金：" + feeToInvestor
							+ "，系统罚金：" + feeToSystem + "，借款手续费：" + feeAll);
			// 余额不足，抛异常
			// 按比例分给投资人和系统（默认优先给投资人，剩下的给系统，以防止提转差额的出现）
			List<InvestRepay> irs = ht
					.find("from InvestRepay ir where ir.invest.loan.id=? and ir.status=?",
							new Object[] { loan.getId(), RepayStatus.REPAYING });

			double feeToInvestorTemp = feeToInvestor;
			// 更改投资的还款信息
			for (int i = 0; i < irs.size(); i++) {
				InvestRepay ir = irs.get(i);

				// FIXME: 记录repayWay信息
				ir.setStatus(LoanConstants.RepayStatus.COMPLETE);
				ir.setTime(new Date());

				// 罚金
				double cashFine;
				if (i == irs.size() - 1) {
					cashFine = feeToInvestorTemp;
				} else {
					cashFine = ArithUtil.round(ir.getCorpus() / sumCorpus
							* feeToInvestor, 2);
					feeToInvestorTemp = ArithUtil.sub(feeToInvestorTemp,
							cashFine);
				}

				userBillBO.transferIntoBalance(
						ir.getInvest().getUser().getId(),
						ArithUtil.add(ir.getCorpus(), cashFine),
						OperatorInfo.ADVANCE_REPAY, "投资："
								+ ir.getInvest().getId() + "收到系统代偿提前还款" + "  本金："
								+ ir.getCorpus() + "  罚息：" + cashFine);
			}

			try {
				cancelTransfering(loanId);
			} catch (RepayException e) {
				throw new AdvancedRepayException(e.getMessage(), e.getCause());
			}

			systemBillService.transferInto(ArithUtil.add(feeToSystem, feeAll),
					OperatorInfo.ADVANCE_REPAY,
					"系统代偿提前还款罚金及借款手续费到账，项目ID:" + loan.getId());

			// 改项目状态。
			loan.setStatus(LoanConstants.LoanStatus.COMPLETE);
			ht.merge(loan);
			loanService.dealComplete(loan.getId());
			
		}
		
		
	}

	
	/**
	 * 系统代偿，逾期还款
	 * @param repayId
	 * @throws InsufficientBalance
	 * @throws OverdueRepayException
	 */
	@SuppressWarnings({ "deprecation","unchecked" })
	public void overdueRepayByAdmin(String repayId) throws InsufficientBalance, OverdueRepayException {
		
		LoanRepay repay = ht.get(LoanRepay.class, repayId);
		ht.evict(repay);
		repay = ht.get(LoanRepay.class, repayId, LockMode.UPGRADE);
		
		if (repay.getStatus().equals(LoanConstants.RepayStatus.OVERDUE)
			|| repay.getStatus().equals(LoanConstants.RepayStatus.BAD_DEBT)
			|| repay.getStatus().equals(RepayStatus.WAIT_REPAY_VERIFY)) {
			double defaultInterest = repay.getDefaultInterest();
			List<InvestRepay> irs = ht.find("from InvestRepay ir where ir.invest.loan.id=? and ir.period=?"
					,new Object[] { repay.getLoan().getId(), repay.getPeriod() });
			// 更改投资的还款信息
			for (InvestRepay ir : irs) {
				ir.setStatus(LoanConstants.RepayStatus.COMPLETE);
				ir.setTime(new Date());
				ht.update(ir);

				userBillBO.transferIntoBalance(ir.getInvest().getUser().getId()
											  ,ArithUtil.add(ir.getCorpus(), ir.getInterest()
											  ,ir.getDefaultInterest())
											  ,OperatorInfo.OVERDUE_REPAY
											  ,"投资：" + ir.getInvest().getId() + "收到系统代偿逾期还款, 还款ID:"
											  + repay.getId() + "  借款ID:" + repay.getLoan().getId()
											  + "  本金：" + ir.getCorpus() + "  利息："
											  + ir.getInterest() + "  罚息："
											  + ir.getDefaultInterest());
				defaultInterest = ArithUtil.sub(defaultInterest,ir.getDefaultInterest());
				// 投资者手续费
				userBillBO.transferOutFromBalance(ir.getInvest().getUser().getId()
												  ,ir.getFee(), OperatorInfo.OVERDUE_REPAY
												  ,"投资：" + ir.getInvest().getId() + "收到系统代偿逾期还款，扣除手续费, 还款ID:"
												  + repay.getId());
				systemBillService.transferInto(ir.getFee()
											   ,OperatorInfo.OVERDUE_REPAY, "投资："
											   + ir.getInvest().getId() + "收到系统代偿逾期还款，扣除手续费, 还款ID:"
											   + repay.getId()+",项目ID:"+ir.getInvest().getLoan().getId());
			}
			
			// 更改借款的还款信息
			double payMoney = ArithUtil.add(
					ArithUtil.add(repay.getCorpus(), repay.getInterest()),
					repay.getFee(), repay.getDefaultInterest());
			repay.setTime(new Date());
			repay.setStatus(LoanConstants.RepayStatus.COMPLETE);

			// 系统商户账户，扣除还款。
			systemBillService.transferOut(payMoney
										  ,OperatorInfo.OVERDUE_REPAY
										  ,"借款：" + repay.getLoan().getId() + "系统代偿逾期还款, 还款ID：" + repay.getId()
										  + " 本金：" + repay.getCorpus() + "  利息："
										  + repay.getInterest() + "  手续费：" + repay.getFee()
										  + "  罚息：" + repay.getDefaultInterest());
			// 借款者手续费
			systemBillService.transferInto(repay.getFee()
										   ,OperatorInfo.OVERDUE_REPAY, "项目ID:" + repay.getLoan().getId()
										   + "系统代偿逾期还款，扣除手续费， 还款ID：" + repay.getId());
			// 罚息转入网站账户
			systemBillService.transferInto(defaultInterest
										   ,OperatorInfo.OVERDUE_REPAY, "项目ID:" + repay.getLoan().getId()
										   + "系统代偿逾期还款，扣除罚金， 还款ID：" + repay.getId());
			ht.merge(repay);
			Long count = (Long) ht.find("select count(repay) from LoanRepay repay where repay.loan.id=? and (repay.status=? or repay.status=?)"
										,repay.getLoan().getId(), RepayStatus.OVERDUE, RepayStatus.BAD_DEBT).get(0);
			if(count == 0){
				//如果没有逾期或者坏账的还款，则更改借款状态。
				repay.getLoan().setStatus(LoanStatus.REPAYING);
				ht.update(repay.getLoan());
			}
			// 判断是否所有还款结束，更改等待还款的投资状态和还款状态，还有项目状态。
			loanService.dealComplete(repay.getLoan().getId());
			try {
				cancelTransfering(repay.getLoan().getId());
			} catch (RepayException e) {
				throw new OverdueRepayException(e.getMessage(), e.getCause());
			}

		}else{
			throw new OverdueRepayException("还款不处于逾期还款状态");
		}	
	}

	
	
	/**
	 * 还款时候，取消正在转让的债权
	 * 
	 * @param loanId
	 * @throws RepayException
	 */
	private void cancelTransfering(String loanId) throws RepayException {
		// 取消投资下面的所有正在转让的债权
		String hql = "from TransferApply ta where ta.invest.loan.id=? and ta.status=?";
		List<TransferApply> tas = ht.find(hql, new String[] { loanId,
				TransferStatus.WAITCONFIRM });
		if (tas.size() > 0) {
			// 有购买了等待第三方确认的债权，所以不能还款。
			throw new RepayException("有等待第三方确认的债权转让，不能还款！");
		}
		tas = ht.find(hql, new String[] { loanId, TransferStatus.TRANSFERING });
		for (TransferApply ta : tas) {
			transferService.cancel(ta.getId());
		}
	}
	

	public boolean isInRepayPeriod(Date repayDate) {
		repayDate = DateUtil.StringToDate(DateUtil.DateToString(repayDate,
				DateStyle.YYYY_MM_DD_CN));
		Date now = new Date();
		Date upperLimit = DateUtil.addMonth(repayDate, -1);
		repayDate = DateUtil.addMinute(repayDate, 1439);
		// 还款日上推一个月，算是还款期。
		if ((now.before(repayDate)) && (!now.before(upperLimit))) {
			return true;
		}
		return false;
	}
}
