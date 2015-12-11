package com.esoft.yeepay.loan.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.coupon.CouponConstants.UserCouponStatus;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.ArithUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.cptransaction.YeepayCpTransacionOperation;
import com.esoft.yeepay.trusteeship.YeePayConstants;

@Service("yeePayCancelInvestOperation")
public class YeePayCancelInvestOperation{
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	@Logger
	static Log log;

	@Resource
	private HibernateTemplate ht;

	@Resource
	UserBillBO ubs;
	
	@Resource
	YeepayCpTransacionOperation yeepayCpTransacionOperation;
	
	@Transactional(rollbackFor = Exception.class)
	public void createOperation(String loanId, String operatorId) {
		Loan loan = ht.get(Loan.class, loanId);
		ht.evict(loan);// 防止并发出现
		loan = ht.get(Loan.class, loanId, LockMode.UPGRADE);
		loan.setStatus(LoanConstants.LoanStatus.CANCEL);
		loan.setCancelTime(new Date());
		String hql = "from Invest invest where invest.status = ? and invest.loan.id = ?";
		List<Invest> invests = ht.find(hql, InvestStatus.BID_SUCCESS, loan.getId());

		//易宝取消转账并解冻
		boolean flag =false;//每笔投资账单是否取消成功
		boolean success = false;//是否存在一笔流标失败
		for(Invest invest : invests){
			//判断是否是自动投资，自动投资和普通投资的账单流水号前缀不同，普通投标为03，自动投标为13
			if(invest.getIsAutoInvest()){
				flag = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.AUTO_INVEST+invest.getId(), "CANCEL");
			}else{
				flag = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.INVEST+invest.getId(), "CANCEL");
			}
			if(!flag){
				log.debug("转账授权订单【"+invest.getId()+"】放款确认失败");
				success = true;
			}else{
				try {
					ubs.unfreezeMoney(invest.getUser().getId(),
							invest.getCouponMoney() == null ? invest.getMoney() : ArithUtil.sub(invest.getMoney(), invest.getCouponMoney()),
							OperatorInfo.CANCEL_LOAN,"借款" + loan.getId()
							+ "流标，解冻投资金额"+invest.getMoney() );
					//修改红包状态
					if(invest.getUserCoupon() != null && invest.getUserCoupon().getDeadline().after(new Date())){
						invest.getUserCoupon().setStatus(UserCouponStatus.UNUSED);
						invest.getUserCoupon().setUsedTime(null);
						ht.update(invest.getUserCoupon());
					}
					invest.setStatus(InvestConstants.InvestStatus.CANCEL);
					ht.update(invest);
				} catch (InsufficientBalance e) {
					throw new RuntimeException(e);
				}
			}
		}
		log.debug("流标失败（失败为true）" + success);
		// FIXME:如果有一笔没有没有取消流标成功的话 ,下面是否继续处理
		try {
			if (!success) {
				ubs.unfreezeMoney(loan.getUser().getId(), loan.getDeposit(),OperatorInfo.CANCEL_LOAN
						,"借款" + loan.getId() + "流标，解冻保证金"+loan.getDeposit());
				log.debug("借款人id：" + loan.getUser().getId()+ " 解冻保证金金额：" + loan.getDeposit());
			}
		} catch (InsufficientBalance ib) {
			throw new RuntimeException(ib);
		}
		ht.update(loan);
	}
	
}
