package com.esoft.jdp2p.loan.controller;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.logging.Log;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.archer.config.ConfigConstants;
import com.esoft.archer.config.service.ConfigService;
import com.esoft.archer.node.model.NodeAttr;
import com.esoft.archer.notice.model.Notice;
import com.esoft.archer.notice.model.NoticePool;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.system.event.AuditEvent;
import com.esoft.archer.system.model.Audit;
import com.esoft.archer.user.exception.InsufficientBalance;
import com.esoft.archer.user.exception.UserNotFoundException;
import com.esoft.archer.user.model.User;
import com.esoft.archer.user.service.UserService;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;
import com.esoft.jdp2p.loan.LoanConstants;
import com.esoft.jdp2p.loan.exception.BorrowedMoneyTooLittle;
import com.esoft.jdp2p.loan.exception.ExistWaitAffirmInvests;
import com.esoft.jdp2p.loan.exception.InvalidExpectTimeException;
import com.esoft.jdp2p.loan.exception.InvalidPublishTimeException;
import com.esoft.jdp2p.loan.model.Loan;
import com.esoft.jdp2p.loan.service.LoanService;
import com.esoft.jdp2p.schedule.ScheduleConstants;
import com.esoft.jdp2p.schedule.job.AutoInvestAfterLoanPassed;
import com.esoft.jdp2p.schedule.job.AutoPublishAfterLoanAdd;

/**
 * Filename: LoanHome.java Description: Copyright: Copyright (c)2013 Company:
 * jdp2p c
 * 
 * @author: yinjunlu
 * @version: 1.0 Create at: 2014-1-11 下午4:28:49
 * 
 *           Modification History: Date Author Version Description
 *           ------------------------------------------------------------------
 *           2014-1-11 yinjunlu 1.0 1.0 Version
 */
@Component
@Scope(ScopeType.VIEW)
public class LoanHome extends EntityHome<Loan> implements Serializable {

	private boolean ispass = false;
	@Resource
	private LoanService loanService;
	@Resource
	private LoginUserInfo loginUser;
	@Resource
	private UserService userService;

	@Resource
	NoticePool noticePool;

	@Resource
	ConfigService configService;

	@Logger
	Log log;

	@Resource
	StdScheduler scheduler;

	@Autowired
	ApplicationContext applicationContext;

	/**
	 * 后台借款列表
	 */
	public final static String loanListUrl = "/admin/loan/loanList";

	public void initVerify(Loan loan) {
		this.setInstance(loan);
		if (LoanConstants.LoanVerifyStatus.PASSED.equals(this.getInstance()
				.getVerified())) {
			ispass = true;
		}
	}

	public boolean getIspass() {
		return ispass;
	}

	public void setIspass(boolean ispass) {
		this.ispass = ispass;
	}

	/**
	 * 借款审核
	 */
	public String verify() {
		if (ispass) {
			if (this.getInstance().getExpectTime() == null) {
				FacesUtil.addErrorMessage("请填写预计执行时间。");
				ispass = false;
				return null;
			}
			if (LoanConstants.LoanActivityType.DX.equals(this.getInstance()
					.getLoanActivityType())
					&& LoanConstants.LoanInvestPassword.PSWD.equals(this
							.getInstance().getInvestPassword())) {
				FacesUtil.addErrorMessage("投资密码不能设置为0,请重新输入!");
				return null;
			}
			try {
				this.getInstance().setVerifyUser(
						new User(loginUser.getLoginUserId()));
				loanService.passApply(this.getInstance());
				List<NodeAttr> loanAttrs = this.getInstance().getLoanAttrs();
				if (LoanConstants.LoanActivityType.PT.equals(getInstance()
						.getLoanActivityType())) {
					for (NodeAttr nodeAttr : loanAttrs) {
						if ("auto".equals(nodeAttr.getId())) {
							addAutoInvestJob(getInstance());
							break;
						}
					}
				}
			} catch (InvalidPublishTimeException e) {
				FacesUtil.addErrorMessage("预计发布时间必须在当前时间之后。"); // XXX:guoyw,20150313
				ispass = false;
				return null;
			} catch (InvalidExpectTimeException e) {
				if (null != this.getInstance().getPublishTime()) {
					FacesUtil.addErrorMessage("预计执行时间必须在当前时间和预计发布时间之后。");
				} else {
					FacesUtil.addErrorMessage("预计执行时间必须在当前时间之后。");
				}
				ispass = false;
				return null;
			} catch (InsufficientBalance e) {
				FacesUtil.addErrorMessage("余额不足，无法支付借款保证金。");
				ispass = false;
				return null;
			}
			FacesUtil.addInfoMessage("通过借款申请");
			noticePool.add(new Notice("借款：" + getInstance().getId() + "通过申请"));

			// 审计
			applicationContext.publishEvent(new AuditEvent(new Audit(loginUser
					.getLoginUserId(), FacesUtil.getRequestIp(FacesUtil
					.getHttpServletRequest()), "借款申请审核通过", "借款ID:"
					+ getInstance().getId(), "成功", null)));
		} else {
			loanService.refuseApply(this.getInstance().getId(), this
					.getInstance().getVerifyMessage(), loginUser
					.getLoginUserId());
			FacesUtil.addInfoMessage("拒绝借款申请");
			noticePool.add(new Notice("借款：" + getInstance().getId() + "申请被拒绝"));
			// 审计
			applicationContext.publishEvent(new AuditEvent(new Audit(loginUser
					.getLoginUserId(), FacesUtil.getRequestIp(FacesUtil
					.getHttpServletRequest()), "借款申请审核拒绝", "借款ID:"
					+ getInstance().getId(), "成功", null)));
		}
		return FacesUtil.redirect(loanListUrl);
	}

	/**
	 * 项目自动发布调度，达自动发布时间自动改变状态
	 * 
	 * @param loan
	 * @author guoyw 20150314
	 */
	public void addAutoPublishLoanJob(Loan loan) {
		JobDetail jobDetail2 = JobBuilder
				.newJob(AutoPublishAfterLoanAdd.class)
				.withIdentity(loan.getId(),
						ScheduleConstants.JobGroup.AUTO_PUBLISH_AFTER_LOAN_ADD)
				.build();
		jobDetail2.getJobDataMap().put(AutoPublishAfterLoanAdd.LOAN_ID,
				loan.getId());
		// FIXME:需判断DELAY_TIME是否大于0
		SimpleTrigger trigger2 = TriggerBuilder
				.newTrigger()
				.withIdentity(
						loan.getId(),
						ScheduleConstants.TriggerGroup.AUTO_PUBLISH_AFTER_LOAN_ADD)
				.forJob(jobDetail2)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.startAt(loan.getPublishTime()).build();
		try {
			scheduler.scheduleJob(jobDetail2, trigger2);
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
		if (log.isDebugEnabled())
			log.debug("添加[自动发布]调度成功，项目编号["
					+ loan.getId()
					+ "]，时间："
					+ DateUtil.DateToString(loan.getPublishTime(),
							DateStyle.YYYY_MM_DD_HH_MM_SS_CN));
	}

	/**
	 * 申请借款
	 */
	public String save() {
		try {
			// 获取当前登录用户
			User user = userService.getUserById(loginUser.getLoginUserId());
			// 保存申请人
			getInstance().setUser(user);
			if (null == getInstance().getLoanActivityType()) {
				getInstance().setLoanActivityType(
						LoanConstants.LoanActivityType.PT);
			}
			loanService.applyLoan(getInstance());
			FacesUtil.addInfoMessage("发布借款成功，请填写个人基本信息。");
			return "pretty:loanerPersonInfo";
		} catch (InsufficientBalance e) {
			FacesUtil.addErrorMessage(e.getMessage());
			e.printStackTrace();
		} catch (UserNotFoundException e) {
			FacesUtil.addErrorMessage("用户未登录！");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 延期
	 * 
	 * @return
	 */
	public String delay() {
		try {
			loanService.delayExpectTime(getInstance().getId(), getInstance()
					.getExpectTime());
		} catch (InvalidExpectTimeException e) {
			FacesUtil.addErrorMessage("预计执行时间必须在当前时间之后。");
			return null;
		}
		FacesUtil.addInfoMessage("项目延期成功，募集期延至"
				+ DateFormatUtils.format(getInstance().getExpectTime(),
						"yyyy年MM月dd日 HH:mm:ss"));
		noticePool.add(new Notice("借款：" + getInstance().getId() + "延期"));
		return FacesUtil.redirect(loanListUrl);
	}

	/**
	 * 放款
	 */
	public String recheck() {
		try {
			loanService.giveMoneyToBorrower(this.getInstance().getId());
		} catch (ExistWaitAffirmInvests e) {
			FacesUtil.addInfoMessage("放款失败，存在等待第三方资金托管确认的投资。");
			return null;
		} catch (BorrowedMoneyTooLittle e) {
			FacesUtil.addInfoMessage("放款失败，募集到的资金太少。");
			return null;
		}
		FacesUtil.addInfoMessage("放款成功");
		noticePool.add(new Notice("借款：" + getInstance().getId() + "放款成功"));
		return FacesUtil.redirect(loanListUrl);
	}

	/**
	 * 流标
	 * 
	 * @return
	 */
	public String failByManager() {
		try {
			loanService.fail(this.getInstance().getId(),
					loginUser.getLoginUserId());
		} catch (ExistWaitAffirmInvests e) {
			FacesUtil.addInfoMessage("流标失败，存在等待第三方资金托管确认的投资。");
			return null;
		}
		FacesUtil.addInfoMessage("流标成功");
		noticePool.add(new Notice("借款：" + getInstance().getId() + "流标"));
		return FacesUtil.redirect(loanListUrl);
	}

	/**
	 * 更新借款，只能更新不影响流程的字段
	 */
	public String update() {
		loanService.update(getInstance());
		FacesUtil.addInfoMessage("项目修改成功！");
		return FacesUtil.redirect(loanListUrl);
	}

	/**
	 * 管理员手动添加借款。
	 * 
	 * @return
	 */
	public String createAdminLoan() {
		Loan loan = this.getInstance();
		if (!userService.hasRole(loan.getUser().getId(), "INVESTOR")
				&& !userService.hasRole(loan.getUser().getId(), "LOANER")) {
			FacesUtil.addErrorMessage("用户：" + loan.getUser().getId()
					+ "未实名认证，不能发起借款！");
			return null;
		}
		if (LoanConstants.LoanActivityType.DX
				.equals(loan.getLoanActivityType())
				&& LoanConstants.LoanInvestPassword.PSWD.equals(loan
						.getInvestPassword())) {
			FacesUtil.addErrorMessage("投资密码不能设置为0,请重新输入!");
			return null;
		}
		try {
			loanService.createLoanByAdmin(loan);
		} catch (InsufficientBalance e) {
			FacesUtil.addErrorMessage("余额不足，无法支付借款保证金。");
			return null;
		} catch (InvalidExpectTimeException e) {
			FacesUtil.addErrorMessage("预计执行时间必须在当前时间之后。");
			return null;
		}

		FacesUtil.addInfoMessage("发布借款成功，请进行借款初审。");
		return FacesUtil.redirect("/admin/loan/loanList");
	}

	/**
	 * 判断一个loan是否有某个属性
	 * 
	 * @param loanId
	 * @param attrId
	 *            属性id
	 * @return
	 * @author liuchun
	 */
	public boolean hasAttr(String loanId, String attrId) {
		String hql = "select loan from Loan loan left join loan.loanAttrs attr where loan.id=? and attr.id = ?";
		return getBaseService().find(hql, new String[] { loanId, attrId })
				.size() > 0;
	}

	// public void cancelPublish(Loan loan){
	// try {
	// SimpleTrigger trigger = (SimpleTrigger) scheduler.getTrigger(TriggerKey
	// .triggerKey(
	// loan.getId(),
	// ScheduleConstants.TriggerGroup.AUTO_INVEST_AFTER_LOAN_PASSED));
	// if(trigger != null){
	// scheduler.deleteJob(trigger.getJobKey());
	// }
	//
	// } catch (SchedulerException e) {
	// e.printStackTrace();
	// }
	// }

	/**
	 * 调度，自动投标--wangxiao 2014-4-28
	 * 
	 * @param loan
	 */
	public void addAutoInvestJob(Loan loan) {
		JobDetail jobDetail1 = JobBuilder
				.newJob(AutoInvestAfterLoanPassed.class)
				.withIdentity(
						loan.getId(),
						ScheduleConstants.JobGroup.AUTO_INVEST_AFTER_LOAN_PASSED)
				.build();
		jobDetail1.getJobDataMap().put(AutoInvestAfterLoanPassed.LOAN_ID,
				loan.getId());
		// FIXME:需判断DELAY_TIME是否大于0
		Date startDate1 = DateUtil
				.addMinute(
						(loan.getPublishTime() != null ? loan.getPublishTime()
								: new Date()),
						Integer.parseInt(configService
								.getConfigValue(ConfigConstants.AutoInvest.DELAY_TIME)));
		SimpleTrigger trigger1 = TriggerBuilder
				.newTrigger()
				.withIdentity(
						loan.getId(),
						ScheduleConstants.TriggerGroup.AUTO_INVEST_AFTER_LOAN_PASSED)
				.forJob(jobDetail1)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule())
				.startAt(startDate1).build();
		try {
			scheduler.scheduleJob(jobDetail1, trigger1);
			if (log.isDebugEnabled())
				log.debug("添加[自动投标]调度成功，项目编号["
						+ loan.getId()
						+ "]，时间："
						+ DateUtil.DateToString(startDate1,
								DateStyle.YYYY_MM_DD_HH_MM_SS_CN));
		} catch (SchedulerException e) {
			throw new RuntimeException(e);
		}
	}

}
