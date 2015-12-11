package com.esoft.jdp2p.repay.service.impl;

import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.annotation.Resource;

import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.risk.FeeConfigConstants.FeePoint;
import com.esoft.archer.risk.FeeConfigConstants.FeeType;
import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.DateUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.invest.InvestConstants.TransferStatus;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.model.TransferApply;
import com.esoft.jdp2p.invest.service.TransferService;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.LoanConstants.CompensationStatus;
import com.esoft.jdp2p.loan.LoanConstants.LoanStatus;
import com.esoft.jdp2p.loan.LoanConstants.RepayStatus;
import com.esoft.jdp2p.loan.service.LoanService;
import com.esoft.jdp2p.repay.exception.NormalRepayException;
import com.esoft.jdp2p.repay.exception.OverdueRepayException;
import com.esoft.jdp2p.repay.exception.RepayException;
import com.esoft.jdp2p.repay.model.InvestRepay;
import com.esoft.jdp2p.repay.model.LoanRepay;
import com.esoft.jdp2p.repay.model.RepayCompensation;
import com.esoft.jdp2p.repay.service.CompensationService;
import com.esoft.jdp2p.repay.service.RepayService;

@Service
public class CompensationServiceImpl implements CompensationService {

	@Resource
	FeeConfigBO feeConfigBO;

	@Resource
	UserBillBO userBillBO;

	@Resource
	SystemBillService systemBillService;
	
	@Resource
	LoanService loanService;	
	
	@Resource
	TransferService transferService;

	@Resource
	HibernateTemplate ht;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void payForCompensation(RepayCompensation rc)
			throws InsufficientBalance {
		// FIXME:判断rc的状态，防止多次还款出现
		if (!rc.getStatus().equals(CompensationStatus.COMPENSATED)) {
			throw new RuntimeException("compensation status is "
					+ rc.getStatus() + "!");
		}
		// 应还本金和利息和手续费
		double money = ArithUtil.add(rc.getLoanRepay().getCorpus(), rc
				.getLoanRepay().getInterest(), rc.getLoanRepay().getFee());
		// 逾期罚金
		double feeToSystem = feeConfigBO.getFee(FeePoint.OVERDUE_REPAY_SYSTEM,
				FeeType.PENALTY, null, null, ArithUtil.mul(money, DateUtil
						.getIntervalDays(rc.getLoanRepay().getRepayDay(),
								new Date())));
		// 给代偿方的罚金
		double feeToCompensator = feeConfigBO.getFee(
				FeePoint.OVERDUE_REPAY_INVESTOR, FeeType.PENALTY, null, null,
				ArithUtil.mul(money, DateUtil.getIntervalDays(rc.getLoanRepay()
						.getRepayDay(), new Date())));

		userBillBO.transferOutFromBalance(rc.getLoanRepay().getLoan().getUser()
				.getId(), ArithUtil.add(money, feeToCompensator, feeToSystem),
				OperatorInfo.PAY_COMPENSTATION, "代偿编号：" + rc.getId());
		userBillBO.transferIntoBalance(rc.getRepayer().getId(),
				ArithUtil.add(money, feeToCompensator),
				OperatorInfo.PAY_COMPENSTATION, "代偿编号：" + rc.getId());
		userBillBO.transferIntoBalance(rc.getRepayer().getId(),
				feeToSystem,
				OperatorInfo.PAY_COMPENSTATION, "代偿编号：" + rc.getId());
		// 状态改为完成
		rc.setStatus(CompensationStatus.COMPLETE);
		rc.setPayCompensationTime(new Date());
		ht.update(rc);
	}
	
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void normalRepay(LoanRepay repay, String repayerId)
			throws InsufficientBalance, NormalRepayException {
		ht.evict(repay);
		repay = ht.get(LoanRepay.class, repay.getId(), LockMode.UPGRADE);
		// 正常还款
		if (!(repay.getStatus().equals(LoanConstants.RepayStatus.REPAYING) || repay
				.getStatus()
				.equals(LoanConstants.RepayStatus.WAIT_REPAY_VERIFY))) {
			// 该还款不处于正常还款状态。
			throw new NormalRepayException("还款：" + repay.getId() + "不处于正常还款状态。");
		}
		List<InvestRepay> irs = ht
				.find("from InvestRepay ir where ir.invest.loan.id=? and ir.period=?",
						new Object[] { repay.getLoan().getId(),
								repay.getPeriod() });

		// TODO:投资的所有还款信息加和，判断是否等于借款的还款信息，如果不相等，抛异常

		// 更改投资的还款信息
		for (InvestRepay ir : irs) {

			ir.setStatus(LoanConstants.RepayStatus.COMPLETE);
			ir.setTime(new Date());
			ht.update(ir);

			userBillBO.transferIntoBalance(
					ir.getInvest().getUser().getId(),
					ArithUtil.add(ir.getCorpus(), ir.getInterest()),
					OperatorInfo.NORMAL_REPAY,
					"投资：" + ir.getInvest().getId() + "收到还款, 还款ID:"
							+ repay.getId() + "  借款ID:"
							+ repay.getLoan().getId() + "  本金："
							+ ir.getCorpus() + "  利息：" + ir.getInterest());
			if (ir.getCorpusToSystem() != null && ir.getCorpusToSystem() != 0) {
				// 系统回收体验金
				systemBillService.transferInto(ir.getCorpusToSystem(),
						OperatorInfo.NORMAL_REPAY, "投资："
								+ ir.getInvest().getId() + "收到还款，回收体验金, 还款ID:"
								+ repay.getId());
			}
			// 投资者手续费
			userBillBO.transferOutFromBalance(ir.getInvest().getUser().getId(),
					ir.getFee(), OperatorInfo.NORMAL_REPAY, "投资："
							+ ir.getInvest().getId() + "收到还款，扣除手续费, 还款ID:"
							+ repay.getId());
			systemBillService.transferInto(ir.getFee(),
					OperatorInfo.NORMAL_REPAY, "投资：" + ir.getInvest().getId()
							+ "收到还款，扣除手续费, 还款ID:" + repay.getId() + ",项目ID:"
							+ ir.getInvest().getLoan().getId());

		}

		try {
			cancelTransfering(repay.getLoan().getId());
		} catch (RepayException e) {
			throw new NormalRepayException(e.getMessage(), e.getCause());
		}

		// 更改借款的还款信息
		double payMoney = ArithUtil.add(
				ArithUtil.add(repay.getCorpus(), repay.getInterest()),
				repay.getFee());
		repay.setTime(new Date());
		repay.setStatus(LoanConstants.RepayStatus.COMPLETE);
		// 记录repayWay信息，还款者id，如果有此id，则为代偿
		repay.setRepayWay(repayerId);

		// 借款者的账户，扣除还款。
		userBillBO.transferOutFromBalance(
				repayerId,
				payMoney,
				OperatorInfo.NORMAL_REPAY,
				"借款：" + repay.getLoan().getId() + "正常还款, 还款ID：" + repay.getId()
						+ " 本金：" + repay.getCorpus() + "  利息："
						+ repay.getInterest() + "  手续费：" + repay.getFee());
		// 借款者手续费
		systemBillService.transferInto(repay.getFee(),
				OperatorInfo.NORMAL_REPAY, "项目ID:" + repay.getLoan().getId()
						+ "正常还款，扣除手续费， 还款ID：" + repay.getId());

		ht.merge(repay);

		// 如果不是自己还款，则产生代偿
		if (!repay.getLoan().getUser().getId().equals(repayerId)) {
			RepayCompensation rc = new RepayCompensation();
			rc.setId(IdGenerator.randomUUID());
			rc.setLoanRepay(repay);
			rc.setRepayer(new User(repayerId));
			rc.setCompensateTime(new Date());
			rc.setStatus(CompensationStatus.COMPENSATED);
			ht.save(rc);
		}

		// 判断是否所有还款结束，更改等待还款的投资状态和还款状态，还有项目状态。
		loanService.dealComplete(repay.getLoan().getId());
	}

	@Transactional(rollbackFor = Exception.class)
	public void overdueRepay(String repayId,String repayerId) throws InsufficientBalance,
			OverdueRepayException {
		LoanRepay lr = ht.get(LoanRepay.class, repayId);
		ht.evict(lr);
		lr = ht.get(LoanRepay.class, repayId, LockMode.UPGRADE);
		if (lr.getStatus().equals(LoanConstants.RepayStatus.OVERDUE)
				|| lr.getStatus().equals(LoanConstants.RepayStatus.BAD_DEBT)
				|| lr.getStatus().equals(RepayStatus.WAIT_REPAY_VERIFY)) {
			List<InvestRepay> irs = ht
					.find("from InvestRepay ir where ir.invest.loan.id=? and ir.period=?",
							new Object[] { lr.getLoan().getId(), lr.getPeriod() });

			double defaultInterest = lr.getDefaultInterest();
			
			HashSet<Invest> invests = new HashSet<Invest>();
			
			// 更改投资的还款信息
			for (InvestRepay ir : irs) {
				ir.setStatus(LoanConstants.RepayStatus.COMPLETE);
				ir.setTime(new Date());
				ht.update(ir);
				invests.add(ir.getInvest());
				userBillBO.transferIntoBalance(
						ir.getInvest().getUser().getId(),
						ArithUtil.add(ir.getCorpus(), ir.getInterest(),
								ir.getDefaultInterest()),
						OperatorInfo.OVERDUE_REPAY,
						"投资：" + ir.getInvest().getId() + "收到还款, 还款ID:"
								+ lr.getId() + "  借款ID:" + lr.getLoan().getId()
								+ "  本金：" + ir.getCorpus() + "  利息："
								+ ir.getInterest() + "  罚息："
								+ ir.getDefaultInterest());
				// 系统回收体验金
				if (ir.getCorpusToSystem() != null
						&& ir.getCorpusToSystem() > 0) {
					systemBillService.transferInto(ir.getCorpusToSystem(),
							OperatorInfo.OVERDUE_REPAY, "投资："
									+ ir.getInvest().getId()
									+ "收到还款，回收体验金, 还款ID:" + ir.getId());
				}

				defaultInterest = ArithUtil.sub(defaultInterest,
						ir.getDefaultInterest());
				// 投资者手续费
				userBillBO.transferOutFromBalance(ir.getInvest().getUser()
						.getId(), ir.getFee(), OperatorInfo.OVERDUE_REPAY,
						"投资：" + ir.getInvest().getId() + "收到还款，扣除手续费, 还款ID:"
								+ lr.getId());
				systemBillService.transferInto(ir.getFee(),
						OperatorInfo.OVERDUE_REPAY, "投资："
								+ ir.getInvest().getId() + "收到还款，扣除手续费, 还款ID:"
								+ lr.getId() + ",项目ID:"
								+ ir.getInvest().getLoan().getId());
			}

			// 更改借款的还款信息
			double payMoney = ArithUtil.add(
					ArithUtil.add(lr.getCorpus(), lr.getInterest()),
					lr.getFee(), lr.getDefaultInterest());
			lr.setTime(new Date());
			lr.setStatus(LoanConstants.RepayStatus.COMPLETE);
			// 记录repayWay信息，还款者id，如果有此id，则为代偿
			lr.setRepayWay(repayerId);

			// 代偿账户，扣除还款。
			userBillBO.transferOutFromBalance(
					repayerId,
					payMoney,
					OperatorInfo.OVERDUE_REPAY,
					"借款：" + lr.getLoan().getId() + "逾期还款, 还款ID：" + lr.getId()
							+ " 本金：" + lr.getCorpus() + "  利息："
							+ lr.getInterest() + "  手续费：" + lr.getFee()
							+ "  罚息：" + lr.getDefaultInterest());
			// 借款者手续费
			systemBillService.transferInto(lr.getFee(),
					OperatorInfo.OVERDUE_REPAY, "项目ID:" + lr.getLoan().getId()
							+ "逾期还款，扣除手续费， 还款ID：" + lr.getId());
			// 罚息转入网站账户
			systemBillService.transferInto(defaultInterest,
					OperatorInfo.OVERDUE_REPAY, "项目ID:" + lr.getLoan().getId()
							+ "逾期还款，扣除罚金， 还款ID：" + lr.getId());
			ht.merge(lr);
			Long count = (Long) ht
					.find("select count(repay) from LoanRepay repay where repay.loan.id=? and (repay.status=? or repay.status=?)",
							lr.getLoan().getId(), RepayStatus.OVERDUE,
							RepayStatus.BAD_DEBT).get(0);
			if (count == 0) {
				// 如果没有逾期或者坏账的还款，则更改借款状态。
				lr.getLoan().setStatus(LoanStatus.REPAYING);
				ht.update(lr.getLoan());
				for (Invest invest : invests) {
					invest.setStatus(InvestStatus.REPAYING);
					ht.update(invest);
				}
			}
			// 如果不是自己还款，则产生代偿
			if (!lr.getLoan().getUser().getId().equals(repayerId)) {
				RepayCompensation rc = new RepayCompensation();
				rc.setId(IdGenerator.randomUUID());
				rc.setLoanRepay(lr);
				rc.setRepayer(new User(repayerId));
				rc.setCompensateTime(new Date());
				rc.setStatus(CompensationStatus.COMPENSATED);
				ht.save(rc);
			}
			// 判断是否所有还款结束，更改等待还款的投资状态和还款状态，还有项目状态。
			loanService.dealComplete(lr.getLoan().getId());
			try {
				cancelTransfering(lr.getLoan().getId());
			} catch (RepayException e) {
				throw new OverdueRepayException(e.getMessage(), e.getCause());
			}
		} else {
			throw new OverdueRepayException("还款不处于逾期还款状态");
		}
	}
	
	/**
	 * 还款时候，取消正在转让的债权
	 * 
	 * @param loanId
	 * @throws RepayException
	 */
	public void cancelTransfering(String loanId) throws RepayException {
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
}
