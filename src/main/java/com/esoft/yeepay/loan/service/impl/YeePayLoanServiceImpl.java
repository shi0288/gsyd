package com.esoft.yeepay.loan.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.risk.service.SystemBillService;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.ArithUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.exception.BorrowedMoneyTooLittle;
import com.esoft.jdp2p.loan.exception.ExistWaitAffirmInvests;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.impl.LoanServiceImpl;
import com.esoft.jdp2p.repay.service.RepayService;

@Service("yeePayLoanService")
public class YeePayLoanServiceImpl extends LoanServiceImpl {
	
	@Logger
	private static Log log;

	@Resource
	HibernateTemplate ht;

	@Resource
	UserBillBO userBillBO;

	@Resource
	SystemBillService sbs;

	@Resource
	RepayService repayService;
	
	@Resource
	ConfigService configService;
	/*@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void giveMoneyToBorrower(String loanId)
			throws ExistWaitAffirmInvests, BorrowedMoneyTooLittle {
		Loan loan = ht.get(Loan.class, loanId);
		ht.evict(loan);
		loan=  ht.get(Loan.class, loanId, LockMode.UPGRADE);

		// 更改项目状态，放款。
		loan.setStatus(LoanConstants.LoanStatus.REPAYING);
		// 获取当前日期
		Date dateNow = new Date();
		// 设置放款日期
		loan.setGiveMoneyTime(dateNow);

		// 实际到借款账户的金额
		double actualMoney = 0D;

		List<Invest> investSs = Lists.newArrayList(Collections2.filter(
				loan.getInvests(), new Predicate<Invest>() {
					public boolean apply(Invest invest) {
						return invest
								.getStatus()
								.equals(InvestConstants.InvestStatus.WAIT_LOANING_VERIFY);
					}
				}));
		for (Invest invest : investSs) {
			try {
				ubs.transferOutFromFrozen(invest.getUser().getId(),
						invest.getMoney(), OperatorInfo.GIVE_MONEY_TO_BORROWER,
						"投资成功，取出投资金额, 借款ID：" + loan.getId());
				actualMoney = ArithUtil.add(actualMoney,
						invest.getInvestMoney());
			} catch (InsufficientBalance e) {
				throw new RuntimeException(e);
			}
			// 更改投资状态
			invest.setStatus(InvestConstants.InvestStatus.REPAYING);
			ht.update(invest);
		}
		// 设置借款实际借到的金额
		loan.setMoney(actualMoney);
		// 根据借款期限产生还款信息
		repayService.generateRepay(loan.getId());

		// 把借款转给借款人账户
		ubs.transferIntoBalance(loan.getUser().getId(), actualMoney,
				OperatorInfo.GIVE_MONEY_TO_BORROWER,
				"借款到账, 借款ID：" + loan.getId());
		try {
			ubs.unfreezeMoney(loan.getUser().getId(), loan.getDeposit(),
					OperatorInfo.GIVE_MONEY_TO_BORROWER, "借款成功，解冻借款保证金, 借款ID："
							+ loan.getId());
			ubs.transferOutFromBalance(loan.getUser().getId(),
					loan.getLoanGuranteeFee(),
					OperatorInfo.GIVE_MONEY_TO_BORROWER, "取出借款管理费, 借款ID："
							+ loan.getId());
			sbs.transferInto(loan.getLoanGuranteeFee(),
					OperatorInfo.GIVE_MONEY_TO_BORROWER,
					"借款管理费, 借款ID：" + loan.getId());
		} catch (InsufficientBalance e) {
			throw new RuntimeException(e);
		}
		ht.merge(loan);
	}
*/
	
	
	/**
	 * 放款操作
	 * */
	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void giveMoneyToBorrower(String loanId)
			throws ExistWaitAffirmInvests, BorrowedMoneyTooLittle {
		if (log.isInfoEnabled()) {
			log.info("放款" + loanId);
		}
		Loan loan = ht.get(Loan.class, loanId);
		ht.evict(loan);
		loan = ht.get(Loan.class, loanId, LockMode.UPGRADE);
		// FIXME:loan不存在
		// 有两种放款，一种是项目募集完成了，放款；一种是项目未募集满额，得根据项目的实际募集金额，修改项目借款金额，然后进行放款。

		// 更改项目状态，放款。
		loan.setStatus(LoanConstants.LoanStatus.REPAYING);
		// 获取当前日期
		Date dateNow = new Date();
		// 设置放款日期
		loan.setGiveMoneyTime(dateNow);
		if (loan.getInterestBeginTime() == null) {
			loan.setInterestBeginTime(dateNow);
		}

		// 实际到借款账户的金额
		double actualMoney = 0D;

		List<Invest> invests = loan.getInvests();
		double actualFee = 0d;
		for (Invest invest : invests) {
			if (invest.getStatus().equals(
					InvestConstants.InvestStatus.WAIT_AFFIRM)) {
				// 放款时候，需要检查是否要等待确认的投资，如果有，则不让放款。
				throw new ExistWaitAffirmInvests("investID:" + invest.getId());
			}
			if (invest.getStatus().equals(
					InvestConstants.InvestStatus.BID_SUCCESS)) {
				// 放款时候，需要只更改BID_SUCCESS 的借款。
				try {
					// investMoney-代金券金额
					if (invest.getUserCoupon() != null) {
						//realMoney为投资人真正转出的钱
						double realMoney = ArithUtil.sub(invest.getMoney(),
								invest.getUserCoupon().getCoupon().getMoney());
						if (realMoney < 0) {
							realMoney = 0;
						}
						userBillBO.transferOutFromFrozen(invest.getUser().getId(),
								realMoney, OperatorInfo.GIVE_MONEY_TO_BORROWER,
								"投资成功，取出投资金额, 借款ID：" + loan.getId());
						actualMoney = ArithUtil.add(actualMoney, realMoney);
					} else {
						userBillBO.transferOutFromFrozen(invest.getUser().getId(),
								invest.getMoney(),
								OperatorInfo.GIVE_MONEY_TO_BORROWER,
								"投资成功，取出投资金额, 借款ID：" + loan.getId());
						actualMoney = ArithUtil.add(actualMoney,
								invest.getInvestMoney());
					}

					double guranteeFee = loan.getLoanGuranteeFee()==null?0.0:loan.getLoanGuranteeFee();
					double fee = ArithUtil.round(ArithUtil.mul(guranteeFee, ArithUtil.div(invest.getInvestMoney(), loan.getLoanMoney())), 2);
					actualFee += fee;
				} catch (InsufficientBalance e) {
					log.error(e);
					throw new RuntimeException(e);
				}
				// 更改投资状态
				invest.setStatus(InvestConstants.InvestStatus.REPAYING);
				ht.update(invest);
			}
		}
//		String isYeepay2 = "0";
//		try{
//			isYeepay2 = configService.getConfigValue("yeepay.is_yeepay_2");
//		}catch(ObjectNotFoundException e){}
//		if(!"1".equals(isYeepay2)){
//			actualFee = loan.getLoanGuranteeFee();
//		}
		// 设置借款实际借到的金额
		loan.setMoney(actualMoney);
		// 根据借款期限产生还款信息
		repayService.generateRepay(loan.getId());

		// 借款手续费-借款保证金
		double subR = ArithUtil.sub(actualFee, loan.getDeposit());

		double tooLittle = ArithUtil.sub(actualMoney, subR);
		// 借到的钱，可能不足以支付借款手续费
		if (tooLittle <= 0) {
			throw new BorrowedMoneyTooLittle("actualMoney：" + tooLittle);
		}
		
		try {
			// 把借款转给借款人账户
			userBillBO.transferIntoBalance(loan.getUser().getId(), actualMoney,
					OperatorInfo.GIVE_MONEY_TO_BORROWER,
					"借款到账, 借款ID：" + loan.getId());
			
			userBillBO.unfreezeMoney(loan.getUser().getId(), loan.getDeposit(),
					OperatorInfo.GIVE_MONEY_TO_BORROWER, "借款成功，解冻借款保证金, 借款ID："
							+ loan.getId());
			userBillBO.transferOutFromBalance(loan.getUser().getId(),
					actualFee,
					OperatorInfo.GIVE_MONEY_TO_BORROWER, "取出借款管理费, 借款ID："
							+ loan.getId());
			sbs.transferInto(actualFee,
					OperatorInfo.GIVE_MONEY_TO_BORROWER,
					"借款管理费, 借款ID：" + loan.getId());
		} catch (InsufficientBalance e) {
			log.error(e);
			throw new RuntimeException(e);
		}catch(Exception e){
			log.debug(e);
			e.printStackTrace();
		}
		ht.merge(loan);
	}
}
