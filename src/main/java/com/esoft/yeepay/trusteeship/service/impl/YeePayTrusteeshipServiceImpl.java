package com.esoft.yeepay.trusteeship.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.hibernate.ObjectNotFoundException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.bankcard.BankCardConstants;
import com.esoft.archer.bankcard.model.BankCard;
import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.UserConstants.RechargeStatus;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.model.Recharge;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.model.WithdrawCash;
import com.esoft.archer.user.service.RechargeService;
import com.esoft.archer.user.service.WithdrawCashService;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.annotations.Logger;
import com.esoft.core.util.ArithUtil;
import com.esoft.core.util.Dom4jUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.jdp2p.invest.InvestConstants;
import com.esoft.jdp2p.invest.InvestConstants.InvestStatus;
import com.esoft.jdp2p.invest.InvestConstants.TransferStatus;
import com.esoft.jdp2p.invest.model.Invest;
import com.esoft.jdp2p.invest.model.TransferApply;
import com.esoft.jdp2p.invest.service.TransferService;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.LoanConstants.RepayStatus;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.repay.exception.AdvancedRepayException;
import com.esoft.jdp2p.repay.exception.NormalRepayException;
import com.esoft.jdp2p.repay.exception.OverdueRepayException;
import com.esoft.jdp2p.repay.model.InvestRepay;
import com.esoft.jdp2p.repay.model.LoanRepay;
import com.esoft.jdp2p.repay.service.RepayService;
import com.esoft.jdp2p.trusteeship.TrusteeshipConstants;
import com.esoft.jdp2p.trusteeship.model.TrusteeshipOperation;
import com.esoft.jdp2p.trusteeship.service.TrusteeshipService;
import com.esoft.jdp2p.trusteeship.service.impl.TrusteeshipOperationBO;
import com.esoft.yeepay.sign.CFCASignUtil;
import com.esoft.yeepay.trusteeship.YeePayConstants;
import com.esoft.yeepay.trusteeship.YeePayConstants.OperationType;

public class YeePayTrusteeshipServiceImpl implements TrusteeshipService {
	@Resource
	TrusteeshipOperationBO trusteeshipOperationBO;

	@Logger
	static Log log;

	@Resource
	private HibernateTemplate ht;

	@Resource
	UserBillBO ubs;

	@Resource
	RepayService repayService;

	@Resource
	WithdrawCashService withdrawCashService;

	@Resource
	RechargeService rechargeService;

	@Resource
	TransferService transferService;
	@Resource
	ConfigService configService;
	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void handleSendedOperations() {
				
//		String freezeMoney;
//		try{
//			freezeMoney = configService.getConfigValue("freeze_money");
//		}catch (ObjectNotFoundException e){
//			freezeMoney = "";
//		}
//		// 查找请求表里面，等待返回的数据，且请求时间在30分钟以外。
//		Date date = DateUtils.addMinutes(new Date(), -30);
//		Date dateRecharge = DateUtils.addDays(new Date(), -1);
//		Date dateInvest = DateUtils.addMinutes(new Date(), -11);
//		//除去充值和投资
//		String hql = "from TrusteeshipOperation to where to.status=? and to.requestTime<? and to.type!=? and to.type!=?";
//		List<TrusteeshipOperation> tos = ht.find(hql,
//				TrusteeshipConstants.Status.SENDED, date,
//				YeePayConstants.OperationType.RECHARGE, YeePayConstants.OperationType.INVEST);
//		// 单独查询recharge，查询一天以前的，因为充值时间可能很长。
//		String hqlRecharge = "from TrusteeshipOperation to where to.status=? and to.requestTime<? and to.type=?";
//		List<TrusteeshipOperation> tosRecharge = ht.find(hqlRecharge,
//				TrusteeshipConstants.Status.SENDED, dateRecharge,
//				YeePayConstants.OperationType.RECHARGE);
//		//单独查询投资，11分钟以前的，因为传给易宝的超时时间是十分钟
//		String hqlInvest = "from TrusteeshipOperation to where to.status=? and to.requestTime<? and to.type=?";
//		List<TrusteeshipOperation> tosInvest = ht.find(hqlInvest,
//				TrusteeshipConstants.Status.SENDED, dateInvest,
//				YeePayConstants.OperationType.INVEST);
//		for (TrusteeshipOperation to : tosRecharge) {
//			// 充值
//		    rechargeRecord(to);
//		}
//		for (TrusteeshipOperation to : tosInvest) {
//			// 投标
//			if(!"0".equals(freezeMoney)){
//				invest(to);
//			}else{
//				//to.setStatus(TrusteeshipConstants.Status.PASSED);
//			}
//		}
//
//		for (TrusteeshipOperation to : tos) {
//			// 遍历，根据请求类型进行主动查询
//			// 如果未找到，则对请求进行失败处理。
//			// 如果找到，则根据返回状态，分别进行处理。
//			if (log.isDebugEnabled()) {
//				log.debug("RefreshTrusteeshipOperation type:" + to.getType());
//			}
//			if (log.isInfoEnabled()) {
//				log.info("RefreshTrusteeshipOperation id:" + to.getId());
//			}
//			if (to.getType().equals(YeePayConstants.OperationType.INVEST)) {
//				// 投标
//				invest(to);
//			} else if (to.getType().equals(YeePayConstants.OperationType.REPAY)) {
//				// 还款
//				repaymentRecord(to);
//			} else if (to.getType().equals(
//					YeePayConstants.OperationType.WITHDRAW_CASH)) {
//				// 提现
//				if(!"0".equals(freezeMoney)){
//					withdrawRecord(to);
//				}else{
//					to.setStatus(TrusteeshipConstants.Status.PASSED);
//				}
//			} else if (to.getType().equals(
//					YeePayConstants.OperationType.GIVE_MOENY_TO_BORROWER)
//					|| to.getType().equals(
//							OperationType.AUTHORIZE_AUTO_TRANSFER)
//					|| to.getType().equals(
//							OperationType.UNBINDING_YEEPAY_BANKCARD)) {
//				// 放款、自动投标授权、解绑银行卡
//				to.setStatus(TrusteeshipConstants.Status.NO_RESPONSE);
//			} else if (to.getType().equals(
//					YeePayConstants.OperationType.CREATE_ACCOUNT)) {
//				// 创建帐户
//				to.setStatus(TrusteeshipConstants.Status.NO_RESPONSE);
//			} else if (to.getType().equals(
//					YeePayConstants.OperationType.BINDING_YEEPAY_BANKCARD)) {
//				bindingCard(to);
//			} else if (to.getType().equals(
//					YeePayConstants.OperationType.ADVANCE_REPAY)) {
//				// 提前还款
//				advanceRepaymentRecord(to);
//			} else if (to.getType().equals(
//					YeePayConstants.OperationType.OVERDUE_REPAY)) {
//				// 逾期还款
//				overdueRepaymentRecord(to);
//			} else if (to.getType().equals(
//					YeePayConstants.OperationType.TRANSFER)) {
//				// 债权转让
//				if(!"0".equals(freezeMoney)){
//					transfer(to);
//				}else{
//					to.setStatus(TrusteeshipConstants.Status.PASSED);
//				}
//			}
//		}
	}

	/**
	 * 投标
	 */
	private void invest(TrusteeshipOperation to) {
		// 投标
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.INVEST + to.getMarkId() + "</requestNo>");
		content.append("<mode>PAYMENT_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		postMethod.setParameter("service", "QUERY");
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.info("trusteesip invest response: "
					+ new String(responseBody, "UTF-8"));
			// 响应信息 xml格式
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			// records 记录列表
			String records = resultMap.get("records");
			// 获取投资记录
			Invest invest = ht.get(Invest.class, to.getMarkId());
			// 返回码为1标识成功
			if (code.equals("1") && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				// FIXME:status 是否加空判断
				if (status != null && status.equals("LOANED")) {
					// 返回状态为Loaned
					if (invest.getStatus().equals(
							InvestConstants.InvestStatus.BID_SUCCESS)) {
						invest.setStatus(InvestConstants.InvestStatus.REPAYING);
						ht.update(invest);
					}
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				} else if (status != null && status.equals("FREEZED")) {
					// 返回状态为FREEZED
					if (invest.getStatus().equals(
							InvestConstants.InvestStatus.WAIT_AFFIRM)) {
						invest.setStatus(InvestConstants.InvestStatus.BID_SUCCESS);
						ht.update(invest);
					}
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				} else if (status != null && status.equals("CANCEL")) {
					// 返回状态为CANCEL
					if (invest.getStatus().equals(
							InvestConstants.InvestStatus.BID_SUCCESS)) {
						invest.setStatus(InvestConstants.InvestStatus.CANCEL);
						ht.update(invest);
						// 解冻投资金额
						try {
							ubs.unfreezeMoney(invest.getUser().getId(),
									invest.getMoney(),
									OperatorInfo.CANCEL_INVEST,
									"投资：" + invest.getId() + "失败，解冻投资金额, 借款ID："
											+ invest.getLoan().getId());
						} catch (InsufficientBalance e) {
							throw new RuntimeException(e);
						}
					}
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				}
			} else {
				// 返回码不为1 可能为0 101 102 103
				if (invest.getStatus().equals(
						InvestConstants.InvestStatus.WAIT_AFFIRM)) {
					invest.setStatus(InvestConstants.InvestStatus.CANCEL);
					// 改项目状态，项目如果是等待复核的状态，改为募集中
					if (invest.getLoan().getStatus()
							.equals(LoanConstants.LoanStatus.RECHECK)) {
						invest.getLoan().setStatus(
								LoanConstants.LoanStatus.RAISING);
						ht.update(invest.getLoan());
					}
					// 解冻投资金额
					try {
						ubs.unfreezeMoney(invest.getUser().getId(),
								invest.getMoney(), OperatorInfo.CANCEL_INVEST,
								"投资：" + invest.getId() + "失败，解冻投资金额, 借款ID："
										+ invest.getLoan().getId());
					} catch (InsufficientBalance e) {
						throw new RuntimeException(e);
					}
				}
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);
			}
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	/**
	 * 债权转让
	 */
	private void transfer(TrusteeshipOperation to) {
		// 投标
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.TRANSFER + to.getMarkId() + "</requestNo>");
		content.append("<mode>PAYMENT_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		postMethod.setParameter("service", "QUERY");
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.info("trusteesip invest response: "
					+ new String(responseBody, "UTF-8"));
			// 响应信息 xml格式
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			// records 记录列表
			String records = resultMap.get("records");
			// 获取投资记录
			Invest invest = ht.get(Invest.class, to.getMarkId());
			TransferApply ta = ht.get(TransferApply.class, invest
					.getTransferApply().getId());
			String salor = invest.getTransferApply().getInvest().getUser()
					.getId();
			String[] params = to.getOperator().split("&");
			double remainCorpus = transferService.calculateRemainCorpus(ta
					.getId());
			double buyPrice = Double.parseDouble(params[0]);
			// 购买的本金占剩余本金的比例。
			double corpusRate = Double.parseDouble(params[1]);
			// 返回码为1标识成功
			if (code.equals("1") && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				// FIXME:status 是否加空判断
				if (status != null && status.equals("LOANED")) {
					// 返回状态为Loaned
					if (invest.getStatus().equals(
							InvestConstants.InvestStatus.WAIT_AFFIRM)) {
						invest.setStatus(InvestConstants.InvestStatus.REPAYING);
						// 成交时间
						invest.setTime(new Date());
						// 减去invest中持有的本金
						ta.getInvest().setMoney(
								ArithUtil.sub(ta.getInvest().getMoney(),
										invest.getMoney()));
						if (ta.getInvest().getMoney() == 0) {
							// 投资全部被转让，则投资状态变为“完成”。
							ta.getInvest().setStatus(InvestStatus.COMPLETE);
						}
						// 判断ta是否都被购买了
						if (remainCorpus == 0) {
							// 债权全部被购买，债权转让完成
							ta.setStatus(TransferStatus.TRANSFED);
						} else {
							ta.setStatus(TransferStatus.TRANSFERING);
						}

						try {
							ubs.transferOutFromFrozen(invest.getUser().getId(),
									buyPrice, "债权：" + invest.getId() + "购买成功",
									"");
							ubs.transferIntoBalance(salor, buyPrice, "债权："
									+ invest.getId() + "转让成功", "");
						} catch (InsufficientBalance e) {
							log.debug(e.getMessage());
						}

						ht.update(invest);
						ht.update(ta);
						// 生成购买债权后的还款数据，调整之前的还款数据
						generateTransferRepay(ta.getInvest().getInvestRepays(),
								invest, corpusRate);
					}
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				} else if (status != null && status.equals("FREEZED")) {
					// 返回状态为FREEZED
					if (invest.getStatus().equals(
							InvestConstants.InvestStatus.WAIT_AFFIRM)) {
						invest.setStatus(InvestConstants.InvestStatus.WAIT_AFFIRM);
						ht.update(invest);
					}
					to.setStatus(TrusteeshipConstants.Status.SENDED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				} else if (status != null && status.equals("CANCEL")) {
					// 返回状态为CANCEL
					if (invest.getStatus().equals(
							InvestConstants.InvestStatus.WAIT_AFFIRM)) {
						invest.setStatus(InvestConstants.InvestStatus.CANCEL);
						ta.setStatus(TransferStatus.TRANSFERING);
						ht.update(invest);
						ht.update(ta);
						// 解冻投资金额
						try {
							ubs.unfreezeMoney(invest.getUser().getId(),
									buyPrice, OperatorInfo.TRANSFER, "债权转让："
											+ invest.getId()
											+ "债权转让失败，解冻投资金额, 借款ID："
											+ invest.getLoan().getId());
						} catch (InsufficientBalance e) {
							throw new RuntimeException(e);
						}

					}
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				}
			} else {
				// 返回码不为1 可能为0 101 102 103
				if (invest.getStatus().equals(
						InvestConstants.InvestStatus.WAIT_AFFIRM)) {
					invest.setStatus(InvestConstants.InvestStatus.CANCEL);
					ta.setStatus(TransferStatus.TRANSFERING);
					ht.update(invest);
					ht.update(ta);
					// 解冻投资金额
					try {
						ubs.unfreezeMoney(invest.getUser().getId(), buyPrice,
								OperatorInfo.CANCEL_INVEST,
								"投资：" + invest.getId() + "失败，解冻投资金额, 借款ID："
										+ invest.getLoan().getId());
					} catch (InsufficientBalance e) {
						throw new RuntimeException(e);
					}
				}
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);
			}
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	/**
	 * 放款
	 */
	@SuppressWarnings("unused")
	private void paymentRecord(TrusteeshipOperation to) {
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.GIVE_MOENY_TO_BORROWER + to.getMarkId() + "</requestNo>");
		content.append("<mode>PAYMENT_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			// records 记录列表
			String records = resultMap.get("records");
			if (log.isDebugEnabled()) {
				log.debug("RefreshTrusteeshipOperation rechage id:"
						+ to.getMarkId() + " code:" + code);
			}
			String status = respXML.selectSingleNode(
					"/response/records/record/status").getStringValue();

			if (code.equals("1") && status != null) {
				to.setStatus(TrusteeshipConstants.Status.PASSED);
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);
			}
			log.debug("RefreshTrusteeshipOperation code:" + code
					+ " description:" + description + "records" + records);
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	public static void main(String[] args) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					"D:/recharge.txt")));
			FileWriter bw = new FileWriter("D:/ssss.txt");
			String line = br.readLine();
			int count = 0;
			int right = 0;
			int error = 0;
			System.out.println("begin");
			bw.write("begin\n\r");
			while (line != null) {
				count += 1;
				String id = line.substring(32, 44);
				String status = line.substring(line.length() - 34,
						line.length() - 26);
				int ss = line.indexOf("success");
				if (ss != -1) {
					boolean b = ddd(id);
					if (!b) {
						error += 1;
						System.out.println(id);
						bw.write(id + "\n\r");
						bw.flush();
					} else {
						right += 1;
					}
				}
				//
				line = br.readLine();
			}
			System.out.println("end");
			System.out.println("right:" + right + "   error:" + error
					+ "   count:" + count);
			bw.write("end\n\r");
			bw.write("right:" + right + "   error:" + error + "   count:"
					+ count + "\n\r");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

	}

	public static boolean ddd(String id) {
		boolean b = false;
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>" + id + "</requestNo>");
		content.append("<mode>RECHARGE_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		try {
			int statusCode = client.executeMethod(postMethod);
			byte[] responseBody = postMethod.getResponseBody();
			// System.out.println(new String(responseBody, "UTF-8"));
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			String records = resultMap.get("records");
			if ("1".equals(code) && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				if (status != null && status.equals("SUCCESS")) {
					b = true;
				} else {
					b = false;
				}
			}

		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return b;
	}

	/**
	 * 还款
	 */
	private void repaymentRecord(TrusteeshipOperation to) {
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.REPAY + to.getMarkId() + "</requestNo>");
		content.append("<mode>REPAYMENT_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			// records 记录列表
			String records = resultMap.get("records");
			LoanRepay repay = ht.get(LoanRepay.class, to.getMarkId());
			if (log.isDebugEnabled()) {
				log.debug("RefreshTrusteeshipOperation rechage id:"
						+ to.getMarkId() + " code:" + code);
			}
			if (code.equals("1") && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				if (status != null && status.equals("SUCCESS")) {
					if (repay.getStatus().equals(
							LoanConstants.RepayStatus.REPAYING)) {
						repayService.normalRepay(to.getMarkId());
						to.setResponseData(respInfo);
						to.setResponseTime(new Date());
						to.setStatus(TrusteeshipConstants.Status.PASSED);
						ht.update(to);
					}
				}
				if (status != null && status.equals("INIT")) {
					if (repay.getStatus().equals(
							LoanConstants.RepayStatus.REPAYING)) {
						to.setResponseData(respInfo);
						to.setResponseTime(new Date());
						to.setStatus(TrusteeshipConstants.Status.PASSED);
						ht.update(to);
					}
				}
			} else {
				if (repay.getStatus()
						.equals(LoanConstants.RepayStatus.REPAYING)) {
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				}
			}
			log.debug("RefreshTrusteeshipOperation code:" + code
					+ " description:" + description + "records" + records);
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} catch (InsufficientBalance e) {
			e.printStackTrace();
		} catch (NormalRepayException e) {
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	/**
	 * 提前还款
	 */
	private void advanceRepaymentRecord(TrusteeshipOperation to) {
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.ADVANCE_REPAY + to.getMarkId() + "</requestNo>");
		content.append("<mode>REPAYMENT_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			// records 记录列表
			String records = resultMap.get("records");
			Loan loan = ht.get(Loan.class, to.getMarkId());
			if (log.isDebugEnabled()) {
				log.debug("RefreshTrusteeshipOperation rechage id:"
						+ to.getMarkId() + " code:" + code);
			}
			if (code.equals("1") && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				if (status != null && status.equals("SUCCESS")) {
					repayService.advanceRepay(to.getMarkId());
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.update(to);

				}
				if (status != null && status.equals("INIT")) {

					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.update(to);

				}
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);

			}
			log.debug("RefreshTrusteeshipOperation code:" + code
					+ " description:" + description + "records" + records);
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} catch (InsufficientBalance e) {
			e.printStackTrace();
		} catch (AdvancedRepayException e) {
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	/**
	 * 逾期还款
	 */
	private void overdueRepaymentRecord(TrusteeshipOperation to) {
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.OVERDUE_REPAY + to.getMarkId() + "</requestNo>");
		content.append("<mode>REPAYMENT_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			// records 记录列表
			String records = resultMap.get("records");
			LoanRepay repay = ht.get(LoanRepay.class, to.getMarkId());
			if (log.isDebugEnabled()) {
				log.debug("RefreshTrusteeshipOperation rechage id:"
						+ to.getMarkId() + " code:" + code);
			}
			if (code.equals("1") && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				if (status != null && status.equals("SUCCESS")) {
					if (repay.getStatus().equals(
							LoanConstants.RepayStatus.OVERDUE)
							|| repay.getStatus().equals(
									LoanConstants.RepayStatus.BAD_DEBT)) {
						repayService.overdueRepay(to.getMarkId());
						to.setResponseData(respInfo);
						to.setResponseTime(new Date());
						to.setStatus(TrusteeshipConstants.Status.PASSED);
						ht.update(to);
					}
				}
				if (status != null && status.equals("INIT")) {
					if (repay.getStatus().equals(
							LoanConstants.RepayStatus.OVERDUE)
							|| repay.getStatus().equals(
									LoanConstants.RepayStatus.BAD_DEBT)) {
						to.setResponseData(respInfo);
						to.setResponseTime(new Date());
						to.setStatus(TrusteeshipConstants.Status.PASSED);
						ht.update(to);
					}
				}
			} else {
				if (repay.getStatus().equals(LoanConstants.RepayStatus.OVERDUE)
						|| repay.getStatus().equals(
								LoanConstants.RepayStatus.BAD_DEBT)) {
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				}
			}
			log.debug("RefreshTrusteeshipOperation code:" + code
					+ " description:" + description + "records" + records);
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} catch (InsufficientBalance e) {
			e.printStackTrace();
		} catch (OverdueRepayException e) {
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	/**
	 * 充值
	 */
	private void rechargeRecord(TrusteeshipOperation to) {
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.RECHARGE + to.getMarkId() + "</requestNo>");
		content.append("<mode>RECHARGE_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			String records = resultMap.get("records");
			if (log.isDebugEnabled()) {
				log.debug("RefreshTrusteeshipOperation rechage id:"
						+ to.getMarkId() + " code:" + code + "description"
						+ description);
			}
			// 只有code为1且记录返回状态不为空时才能对对其进行处理
			if ("1".equals(code) && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				log.debug("status is null?" + status == null ? true : false);
				Recharge r = ht.get(Recharge.class, to.getMarkId());
				if (status != null && status.equals("SUCCESS")) {
					// FIXME:未做测试
					if (r != null) {
						rechargeService.rechargePaySuccess(r.getId());
					}
					to.setStatus(TrusteeshipConstants.Status.PASSED);
				} else {
					if (r != null) {
						r.setStatus(RechargeStatus.FAIL);
						ht.update(r);
					}
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
				}
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);
			} else {
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);
			}
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	/**
	 * 提现
	 */
	private void withdrawRecord(TrusteeshipOperation to) {
		HttpClient client = new HttpClient();
		/* 创建一个post方法 */
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);
		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<requestNo>"+YeePayConstants.RequestNoPre.WITHDRAW_CASH + to.getMarkId() + "</requestNo>");
		content.append("<mode>WITHDRAW_RECORD</mode>");
		content.append("</request>");
		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("service", "QUERY");
		String sign = CFCASignUtil.sign(content.toString());
		postMethod.setParameter("sign", sign);
		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);
			if (statusCode != HttpStatus.SC_OK) {
				log.error("Method failed: " + postMethod.getStatusLine());
			}
			/* 获得返回的结果 */
			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			String description = resultMap.get("description");
			// records 记录列表
			String records = resultMap.get("records");
			if (log.isDebugEnabled()) {
				log.debug("RefreshTrusteeshipOperation rechage id:"
						+ to.getMarkId() + " code:" + code);
			}
			WithdrawCash wc = ht.get(WithdrawCash.class, to.getMarkId());
			// 只有code为1且记录返回状态不为空时才能对对其进行处理
			if (code.equals("1") && records != null && !records.equals("")) {
				String status = respXML.selectSingleNode(
						"/response/records/record/status").getStringValue();
				log.debug("status is null?" + status == null ? true : false);
				// FIXME:需要根据返回状态对充值记录作修改
				if (status.equals("INIT")) {
					withdrawCashService.refuseWithdrawCashApply(wc);
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					to.setResponseData(respInfo);
					to.setResponseTime(new Date());
					ht.update(to);
				}
				if (status.equals("SUCCESS")) {
					if (wc.getStatus().equals(
							UserConstants.WithdrawStatus.WAIT_VERIFY)) {
						withdrawCashService.passWithdrawCashRecheck(wc);
						to.setStatus(TrusteeshipConstants.Status.PASSED);
						to.setResponseData(respInfo);
						to.setResponseTime(new Date());
						ht.update(to);
					}
				}
			} else {
				withdrawCashService.refuseWithdrawCashApply(wc);
				to.setStatus(TrusteeshipConstants.Status.REFUSED);
				to.setResponseData(respInfo);
				to.setResponseTime(new Date());
				ht.update(to);
			}
			log.debug("RefreshTrusteeshipOperation code:" + code
					+ " description:" + description + "records" + records);
		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			/* Release the connection. */
			postMethod.releaseConnection();
		}
	}

	/**
	 * 绑卡主动查询
	 * 
	 * @param to
	 */
	private void bindingCard(TrusteeshipOperation to) {
		HttpClient client = new HttpClient();
		// 创建一个post方法
		PostMethod postMethod = new PostMethod(
				YeePayConstants.RequestUrl.RequestDirectUrl);

		StringBuffer content = new StringBuffer();
		content.append("<?xml version='1.0' encoding='UTF-8' standalone='yes'?>");
		content.append("<request platformNo='"
				+ YeePayConstants.Config.MER_CODE + "'>");
		content.append("<platformUserNo>" + to.getMarkId()
				+ "</platformUserNo>");
		content.append("</request>");
		// 生成密文
		String sign = CFCASignUtil.sign(content.toString());

		postMethod.setParameter("req", content.toString());
		postMethod.setParameter("sign", sign);
		postMethod.setParameter("service", "ACCOUNT_INFO");

		// 执行post方法
		try {
			int statusCode = client.executeMethod(postMethod);

			/* 处理返回信息 */
			byte[] responseBody = postMethod.getResponseBody();
			log.debug(new String(responseBody, "UTF-8"));
			// 响应信息
			String respInfo = new String(new String(responseBody, "UTF-8"));
			Document respXML = DocumentHelper.parseText(respInfo);
			@SuppressWarnings("unchecked")
			Map<String, String> resultMap = Dom4jUtil.xmltoMap(new String(
					responseBody, "UTF-8"));
			String code = resultMap.get("code");
			// String description = resultMap.get("description");
			// String balance = resultMap.get("balance");
			// String availableAmount = resultMap.get("availableAmount");
			// String freezeAmount = resultMap.get("freezeAmount");
			String cardNo = resultMap.get("cardNo");
			String cardStatus = resultMap.get("cardStatus");
			String bankNo = resultMap.get("bank");
			log.debug(new String(responseBody, "UTF-8"));
			if ("1".equals(code)) {
				// 只有在操作成功且银行卡状态为审核通过的情况下才创建银行卡
				if ("VERIFIED".equals(cardStatus)) {
					to.setResponseData(new String(responseBody, "UTF-8"));
					to.setResponseTime(new Date());
					to.setStatus(TrusteeshipConstants.Status.PASSED);
					ht.update(to);
					User user = ht.get(User.class, to.getMarkId());
					List<BankCard> bankCardListbyLoginUser = ht.find(
							"from BankCard where user.id = ? and status != ?",
							new String[] { user.getId(),
									BankCardConstants.BankCardStatus.DELETED });
					BankCard bankCard = null;
					if (bankCardListbyLoginUser == null
							|| bankCardListbyLoginUser.size() < 1) {
						bankCard = new BankCard();
						bankCard.setId(IdGenerator.randomUUID());
					} else {
						bankCard = bankCardListbyLoginUser.get(0);
					}
					bankCard.setCardNo(cardNo);
					bankCard.setBankNo(bankNo);
					bankCard.setBank(rechargeService.getBankNameByNo(bankNo));
					bankCard.setStatus(cardStatus);
					bankCard.setTime(new Date());
					bankCard.setUser(user);
					ht.merge(bankCard);
				} else if ("VERIFYING".equals(cardStatus)) {
					// 银行卡为审核状态的情况下一直执行主动查询
				} else {
					// 卡的状态返回为空时 拒绝下次主动查询
					to.setResponseData(new String(responseBody, "UTF-8"));
					to.setResponseTime(new Date());
					to.setStatus(TrusteeshipConstants.Status.REFUSED);
				}
			} else {
				// 操作失败是不执行操作 避免unknowHost异常
				log.debug(new String(responseBody, "UTF-8"));
			}

		} catch (HttpException e) {
			log.error("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (DocumentException e) {
			log.error("Fatal parseXML error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			postMethod.releaseConnection();
		}
		log.debug("*************************************************************************************");

	}

	public void generateTransferRepay(List<InvestRepay> investRepays,
			Invest invest, double corpusRate) {
		for (Iterator iterator = investRepays.iterator(); iterator.hasNext();) {
			InvestRepay ir = (InvestRepay) iterator.next();
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
				irNew.setDefaultInterest(ArithUtil.mul(ir.getInterest(),
						corpusRate, 2));
				irNew.setFee(ArithUtil.mul(ir.getFee(), corpusRate, 2));
				irNew.setInterest(ArithUtil.mul(ir.getInterest(), corpusRate, 2));
				irNew.setInvest(invest);
				// 修改原投资的还款信息
				ir.setCorpus(ArithUtil.sub(ir.getCorpus(), irNew.getCorpus()));
				ir.setDefaultInterest(ArithUtil.sub(ir.getDefaultInterest(),
						irNew.getDefaultInterest()));
				ir.setFee(ArithUtil.sub(ir.getFee(), irNew.getFee()));
				ir.setInterest(ArithUtil.sub(ir.getInterest(),
						irNew.getInterest()));
				ht.merge(irNew);
				if (corpusRate == 1) {
					ht.delete(ir);
					iterator.remove();
				} else {
					ht.update(ir);
				}

			}
		}
	}
}
