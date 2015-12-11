package com.esoft.yeepay.invest.service.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.logging.Log;
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
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.MapUtil;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.invest.InvestConstants.TransferStatus;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.model.TransferApply;
import com.esoft.jdp2p.invest.service.TransferService;
import com.esoft.jdp2p.loan.LoanConstants.RepayStatus;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.repay.model.InvestRepay;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.exception.TrusteeshipReturnException;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.cptransaction.YeepayCpTransacionOperation;
import com.esoft.yeepay.cptransaction.YeepayCpTransactionConstant;
import com.esoft.yeepay.cptransaction.requestModel.CPTransaction;
import com.esoft.yeepay.cptransaction.requestModel.Extend;
import com.esoft.yeepay.cptransaction.requestModel.FundDetail;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.exception.YeePayOperationException;
import com.esoft.yeepay.trusteeship.service.impl.YeePayOperationServiceAbs;

@Service
public class YeePayInvestTransferNofreezeOperation extends
		YeePayOperationServiceAbs<Invest> {
	@Resource
	TransferService transferService;
	@Resource
	HibernateTemplate ht;
	@Resource
	UserBillBO userBillBO;
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;
	@Resource
	FeeConfigBO feeConfigBO;
	@Logger
	static Log log;
	@Resource
	YeepayCpTransacionOperation yeepayCpTransacionOperation;
	@Resource
	SystemBillService sbs;
	
	@Override
	@Transactional(rollbackFor=Exception.class)
	public TrusteeshipOperation createOperation(Invest invest, FacesContext fc)
			throws IOException {
		String transferApplyId = invest.getTransferApply().getId();
		// 购买债权人的id
		String userId = invest.getUser().getId();
		// 购买的债权本金 transferCorpus（投资时填写的投资额）
		double transferCorpus = invest.getInvestMoney();
		double remainCorpus = transferService.calculateRemainCorpus(transferApplyId);
		if (transferCorpus <= 0 || transferCorpus > remainCorpus) {
			throw new YeePayOperationException("购买本金必须小于等于" + remainCorpus + "且大于0");
		}
		TransferApply ta = ht.get(TransferApply.class, transferApplyId);
		// 购买的本金占剩余本金的比例corpusRate
		double corpusRate = ArithUtil.div(transferCorpus, remainCorpus);
		// 购买的本金占所有转让本金的比例corpusRateInAll
	    double corpusRateInAll = ArithUtil.div(transferCorpus, ta.getCorpus());
	    // 投资人实际出的钱=债权的购买金额=债权转出价格*corpusRateInAll (债权转出价格=本金-折让金)  
		double buyPrice = ArithUtil.mul(ta.getPrice(), corpusRateInAll, 2);
		
		if(buyPrice > userBillBO.getBalance(userId)){
			throw new YeePayOperationException("余额不足。");
		}

		// 购买时候，扣除手续费，从转让人收到的金额中扣除。费用根据购买价格计算
		double fee = feeConfigBO.getFee(FeePoint.TRANSFER, FeeType.FACTORAGE,
				null, null, buyPrice);
		
		Invest investNew = new Invest();
		investNew.setId(IdGenerator.randomUUID());
		investNew.setMoney(transferCorpus);
		investNew.setStatus(InvestConstants.InvestStatus.CANCEL);
		investNew.setRate(ta.getInvest().getRate());
		investNew.setTransferApply(ta);
		investNew.setLoan(ta.getInvest().getLoan());
		investNew.setTime(new Date());
		investNew.setUser(new User(userId));
		investNew.setInvestMoney(transferCorpus);
		//在这里添加投标状态是手动投标
		investNew.setIsAutoInvest(false);
		ht.save(investNew);
		ht.update(ta);

		
		DecimalFormat currentNumberFormat = new DecimalFormat("#0.00");
		//构建投资参数实体
		CPTransaction cp = new CPTransaction();
		cp.setNotifyUrl(YeePayConstants.ResponseS2SUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.TRANSFER);
		cp.setCallbackUrl(YeePayConstants.ResponseWebUrl.PRE_RESPONSE_URL+YeePayConstants.OperationType.TRANSFER);
		//cp.setExpired(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(DateUtil.addMinute(new Date(), 10)));
		cp.setRequestNo(YeePayConstants.RequestNoPre.TRANSFER + investNew.getId());
		cp.setPlateformNo(YeePayConstants.Config.MER_CODE);
		cp.setPlatformUserNo(userId);
		cp.setBizType(YeepayCpTransactionConstant.CREDIT_ASSIGNMENT.name());
		cp.setUserType("MEMBER");
		

		
		//资金明细
		FundDetail fd = new FundDetail();
		//给转让债权的人的钱
		fd.setAmount(currentNumberFormat.format(buyPrice - fee));
		fd.setTargetPlatformUserNo(ta.getInvest().getUser().getId());
		fd.setTargetUserType("MEMBER");
		fd.setBizType(YeepayCpTransactionConstant.CREDIT_ASSIGNMENT.name());
		cp.getFundDetails().add(fd);
		
		// 平台分润
		if (fee > 0.0) {
			fd = new FundDetail();
			//投资人给平台的钱=fee
			fd.setAmount(currentNumberFormat.format(fee));
			fd.setTargetPlatformUserNo(YeePayConstants.Config.MER_CODE);
			fd.setTargetUserType("MERCHANT");
			fd.setBizType(YeepayCpTransactionConstant.COMMISSION.name());
			cp.getFundDetails().add(fd);
		}
		
		//扩展参数
		Extend extend = new Extend();
		Loan loan = ta.getInvest().getLoan();
		extend.setTenderOrderNo(loan.getId());
		extend.setCreditorPlatformUserNo(ta.getInvest().getUser().getId());
		//判断购买的债券是否是自动投标
		if(ta.getInvest().getIsAutoInvest()){
			extend.setOriginalRequestNo(YeePayConstants.RequestNoPre.AUTO_INVEST+ta.getInvest().getId());			
		}else{
			extend.setOriginalRequestNo(YeePayConstants.RequestNoPre.INVEST+ta.getInvest().getId());
		}
		cp.setExtend(extend);
		log.debug(cp.toString());

		Map<String, String> params = new HashMap<String, String>();
		params.put("req", cp.toString());
		String sign = CFCASignUtil.sign(cp.toString());
		params.put("sign", sign);

		// 保存本地
		TrusteeshipOperation to = new TrusteeshipOperation();
		to.setId(IdGenerator.randomUUID());
		to.setMarkId(investNew.getId());
		to.setOperator(Double.toString(buyPrice)+"&"+Double.toString(corpusRate)+"&"+userId);
		to.setRequestUrl(YeePayConstants.RequestUrl.TRANSFER);
		to.setRequestData(MapUtil.mapToString(params));
		to.setStatus(TrusteeshipConstants.Status.UN_SEND);
		to.setType(YeePayConstants.OperationType.TRANSFER);
		to.setTrusteeship("yeepay");
		trusteeshipOperationBO.save(to);
		try {
			//发送信息
			sendOperation(to.getId(), "utf-8", fc);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;

	}

	
	
	
	@SuppressWarnings("unchecked")
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void receiveOperationPostCallback(ServletRequest request)
			throws TrusteeshipReturnException {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	
		String respXML = request.getParameter("resp");// 响应的参数 为xml格式
		log.debug("债权转让页面回调报文respXML："+respXML.toString());
		String sign = request.getParameter("sign");// 签名
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		
		if(flag){//验签成功
			
			Map<String, String> map = Dom4jUtil.xmltoMap(respXML);
			String code = map.get("code");
			String requestNo = map.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.TRANSFER, "");
			if("1".equals(code)){//易宝冻结金额成功
				
				TrusteeshipOperation to = trusteeshipOperationBO.get(YeePayConstants.OperationType.TRANSFER, requestNo,"yeepay");
				ht.evict(to);
				to = trusteeshipOperationBO.get(YeePayConstants.OperationType.TRANSFER, requestNo,"yeepay");
				ht.evict(to);
				to = ht.get(TrusteeshipOperation.class, to.getId(),LockMode.UPGRADE);
				String[] params = to.getOperator().split("&");
				Invest invest = ht.get(Invest.class, requestNo);//债权购买人购买的债权转换成的一笔投资
				TransferApply ta = ht.get(TransferApply.class, invest.getTransferApply().getId());//债权转让申请
				
				if(TrusteeshipConstants.Status.PASSED.equals(to.getStatus())){
					return ;
				}
				//调用易宝转账确认端口
				boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.TRANSFER+requestNo, "CONFIRM");
				if(result){//易宝转账确认成功
					
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.update(to);
					
					/*************************************平台操作begin***************************************************/
					if(invest != null && ta != null){
						
						invest.setStatus(InvestConstants.InvestStatus.REPAYING);
						invest.setTime(new Date());	// 成交时间
						
						Invest orignInvest = ta.getInvest();//债权转让人转让的一笔投资
						double investReaminCorpus = ArithUtil.sub(orignInvest.getMoney(),invest.getMoney());//该投资债权转让后剩余的投资金额
						if(investReaminCorpus == 0.0){
							orignInvest.setStatus(InvestStatus.COMPLETE);
						}
						orignInvest.setMoney(investReaminCorpus);
						
						double remainCorpus = transferService.calculateRemainCorpus(ta.getId());//未转出的本金
						log.debug("债权未转出的本金："+remainCorpus);
						if(remainCorpus > 0.0){//债权未全部转出
							ta.setStatus(TransferStatus.TRANSFERING);
						}else{//债权全部转出
							ta.setStatus(TransferStatus.TRANSFED);
						}
						
						//债权的购买金额
						double buyPrice =Double.parseDouble(params[0]) ;
						// 购买时候，扣除手续费，从转让人收到的金额中扣除。费用根据购买价格计算
						double fee = feeConfigBO.getFee(FeePoint.TRANSFER, FeeType.FACTORAGE,
								null, null, buyPrice);
						try {
							userBillBO.transferOutFromBalance(invest.getUser().getId(),buyPrice,OperatorInfo.TRANSFER_BUY, "债权：" + invest.getId()+ "购买成功");
							userBillBO.transferIntoBalance(ta.getInvest().getUser().getId(),buyPrice,OperatorInfo.TRANSFER, "债权：" + invest.getId() + "转让成功");
							if(fee > 0.0){
								sbs.transferInto(fee, OperatorInfo.TRANSFER, "购买债权手续费，编号：" + invest.getId());
								userBillBO.transferOutFromBalance(ta.getInvest().getUser().getId(), fee, OperatorInfo.TRANSFER, "债权转让成功手续费，编号：" + ta.getId());
							}
						} catch (InsufficientBalance e) {
							log.debug("postCallback债权转让平台划款时出错！");
							e.printStackTrace();
							//throw new TrusteeshipReturnException("债权转让平台划款时出错！");
						}
						
						ta.setInvest(orignInvest);
						ht.update(invest);
						ht.update(ta);
						
						double corpusRate =Double.parseDouble(params[1]) ;// 购买的本金占剩余本金的比例
						generateTransferRepay(ta.getInvest().getInvestRepays(),invest, corpusRate);// 生成购买债权后的还款数据，调整之前的还款数据
						
						/*************************************平台操作end**********************************************************/
					}
					
				}else{//易宝转账确认失败
					
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
					ht.update(to);
					
					/*************************************平台操作begin**************************************************
					if(invest != null && ta != null){
						invest.setStatus(InvestConstants.InvestStatus.CANCEL);
						ta.setStatus(TransferStatus.TRANSFERING);
						double buyPrice =Double.parseDouble(params[0]) ;//债权的购买金额
						try {
							userBillBO.unfreezeMoney(invest.getUser().getId(),buyPrice,OperatorInfo.TRANSFER, "债权：" + invest.getId()+ "购买失败");
						} catch (InsufficientBalance e) {
							throw new TrusteeshipReturnException("购买债权："+ invest.getId() + "失败，解冻金额时出错！");
						}
						ht.update(invest);
						ht.update(ta);
					}
					************************************平台操作end***************************************************/
					//throw new YeePayOperationException("债权转让易宝转账确认失败");
				}
				
			}
			
		}else{//验签失败
			log.debug("债权转让验签失败");
		}

	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional( rollbackFor = Exception.class)
	public void receiveOperationS2SCallback(ServletRequest request,
			ServletResponse response) {
		try {
			request.setCharacterEncoding("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
		
		String respXML = request.getParameter("notify");// 响应的参数 为xml格式
		log.debug("债权转让s2sCallback respXML："+respXML.toString());
		String sign = request.getParameter("sign");// 签名
		boolean flag = CFCASignUtil.isVerifySign(respXML, sign);
		
		if(flag){//验签成功
			
			Map<String, String> map = Dom4jUtil.xmltoMap(respXML);
			String code = map.get("code");
			String requestNo = map.get("requestNo").replaceFirst(YeePayConstants.RequestNoPre.TRANSFER, "");
			if("1".equals(code)){//易宝冻结金额成功
				
				TrusteeshipOperation to = trusteeshipOperationBO.get(YeePayConstants.OperationType.TRANSFER, requestNo,"yeepay");
				ht.evict(to);
				to = trusteeshipOperationBO.get(YeePayConstants.OperationType.TRANSFER, requestNo,"yeepay");
				ht.evict(to);
				to = ht.get(TrusteeshipOperation.class, to.getId(),LockMode.UPGRADE);
				String[] params = to.getOperator().split("&");
				Invest invest = ht.get(Invest.class, requestNo);//债权购买人购买的债权转换成的一笔投资
				TransferApply ta = ht.get(TransferApply.class, invest.getTransferApply().getId());//债权转让申请
				if(TrusteeshipConstants.Status.PASSED.equals(to.getStatus())){
					return ;
				}
				//调用易宝转账确认端口
				boolean result = yeepayCpTransacionOperation.transactionComform(YeePayConstants.RequestNoPre.TRANSFER+requestNo, "CONFIRM");
				if(result){//易宝转账确认成功
					
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.update(to);
					
					/*************************************平台操作begin***************************************************/
					if(invest != null && ta != null){
						
						invest.setStatus(InvestConstants.InvestStatus.REPAYING);
						invest.setTime(new Date());	// 成交时间
						
						Invest orignInvest = ta.getInvest();//债权转让人转让的一笔投资
						double investReaminCorpus = ArithUtil.sub(orignInvest.getMoney(),invest.getMoney());//原投资债权转让后剩余的投资金额
						if(investReaminCorpus == 0.0){
							orignInvest.setStatus(InvestStatus.COMPLETE);
						}
						orignInvest.setMoney(investReaminCorpus);
						
						double remainCorpus = transferService.calculateRemainCorpus(ta.getId());//未转出的本金
						if(remainCorpus > 0.0){//债权未全部转出
							ta.setStatus(TransferStatus.TRANSFERING);
						}else{//债权全部转出
							ta.setStatus(TransferStatus.TRANSFED);
						}
						//债权的购买金额
						double buyPrice =Double.parseDouble(params[0]) ;
						// 购买时候，扣除手续费，从转让人收到的金额中扣除。费用根据购买价格计算
						double fee = feeConfigBO.getFee(FeePoint.TRANSFER, FeeType.FACTORAGE,
								null, null, buyPrice);
						// 购买时候，扣除手续费，从转让人收到的金额中扣除。费用根据购买价格计算
						try {
							userBillBO.transferOutFromBalance(invest.getUser().getId(),buyPrice,OperatorInfo.TRANSFER_BUY, "债权：" + invest.getId()+ "购买成功");
							userBillBO.transferIntoBalance(ta.getInvest().getUser().getId(),buyPrice,OperatorInfo.TRANSFER, "债权：" + invest.getId() + "转让成功");
							if(fee > 0.0){
								sbs.transferInto(fee, OperatorInfo.TRANSFER, "购买债权手续费，编号：" + invest.getId());
								userBillBO.transferOutFromBalance(ta.getInvest().getUser().getId(), fee, OperatorInfo.TRANSFER, "债权转让成功手续费，编号：" + ta.getId());
							}
						} catch (InsufficientBalance e) {
							log.debug("s2sCallback债权转让平台划款时出错！");
							//throw new RuntimeException("债权转让平台划款时出错！");
						}
						
						ta.setInvest(orignInvest);
						ht.update(invest);
						ht.update(ta);
						
						double corpusRate =Double.parseDouble(params[1]) ;// 购买的本金占剩余本金的比例
						generateTransferRepay(ta.getInvest().getInvestRepays(),invest, corpusRate);// 生成购买债权后的还款数据，调整之前的还款数据
						
						/*************************************平台操作end**********************************************************/
					}
					
				}else{//易宝转账确认失败
					
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
					ht.update(to);
					
					/*************************************平台操作begin**************************************************
					if(invest != null && ta != null){
						invest.setStatus(InvestConstants.InvestStatus.CANCEL);
						ta.setStatus(TransferStatus.TRANSFERING);
						double buyPrice =Double.parseDouble(params[0]) ;//债权的购买金额
						try {
							userBillBO.unfreezeMoney(invest.getUser().getId(),buyPrice,OperatorInfo.TRANSFER, "债权：" + invest.getId()+ "购买失败");
						} catch (InsufficientBalance e) {
							throw new RuntimeException("购买债权："+ invest.getId() + "失败，解冻金额时出错！");
						}
						ht.update(invest);
						ht.update(ta);
					}
					************************************平台操作end***************************************************/
					//throw new YeePayOperationException("债权转让易宝转账确认失败");
				}
				
			}
			
		}else{//验签失败
			log.debug("债权转让验签失败");
		}
		
	}


	public void generateTransferRepay(List<InvestRepay> investRepays,
			Invest invest, double corpusRate) {
		for (Iterator iterator = investRepays.iterator(); iterator.hasNext();) {
			InvestRepay ir =  (InvestRepay) iterator.next();
			if (ir.getStatus().equals(RepayStatus.WAIT_REPAY_VERIFY)
					|| ir.getStatus().equals(RepayStatus.OVERDUE)
					|| ir.getStatus().equals(RepayStatus.BAD_DEBT)) {
				throw new RuntimeException("investRepay with status "
						+ RepayStatus.WAIT_REPAY_VERIFY + "exist!");
			} else if (ir.getStatus().equals(RepayStatus.REPAYING)) {
				// 根据购买本金比例，生成债权还款信息
			  InvestRepay irNew = new InvestRepay();
				try {
					BeanUtils.copyProperties(irNew, ir);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			    irNew.setId(IdGenerator.randomUUID());
				irNew.setCorpus(ArithUtil.mul(ir.getCorpus(), corpusRate, 2));
				irNew.setDefaultInterest(ArithUtil.mul(ir.getDefaultInterest(),
						corpusRate, 2));
				irNew.setFee(ArithUtil.mul(ir.getFee(), corpusRate, 2));
				irNew.setInterest(ArithUtil.mul(ir.getInterest(),
						corpusRate, 2));
				irNew.setInvest(invest);
				// 修改原投资的还款信息
				ir.setCorpus(ArithUtil.sub(ir.getCorpus(),
						irNew.getCorpus()));
				ir.setDefaultInterest(ArithUtil.sub(
						ir.getDefaultInterest(), irNew.getDefaultInterest()));
				ir.setFee(ArithUtil.sub(ir.getFee(), irNew.getFee()));
				ir.setInterest(ArithUtil.sub(ir.getInterest(),
						irNew.getInterest()));
				ht.merge(irNew);
				if (corpusRate == 1) {
					ht.delete(ir);
					iterator.remove();
				}else{
				    ht.update(ir);
				}
			}
		}

	}
}
