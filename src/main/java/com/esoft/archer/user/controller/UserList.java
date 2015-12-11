package com.esoft.archer.user.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.primefaces.context.RequestContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.common.controller.EntityQuery;
import com.esoft.archer.message.MessageConstants;
import com.esoft.archer.message.exception.MailSendErrorException;
import com.esoft.archer.message.model.InBox;
import com.esoft.archer.message.service.MailService;
import com.esoft.archer.subst.model.Subst;
import com.esoft.archer.system.controller.LoginUserInfo;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.model.Area;
import com.esoft.archer.user.model.Role;
import com.esoft.archer.user.model.User;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.StringManager;

@Component
@Scope(ScopeType.VIEW)
public class UserList extends EntityQuery<User> {

	private static final String COUNT_HQL = "select count(distinct user) from User user left join user.roles role";
	private static final String HQL = "select distinct user from User user left join user.roles role";

	private static StringManager sm = StringManager
			.getManager(UserConstants.Package);

	@Logger
	private static Log log;
	
	@Resource
	MailService mailService;
	
	@Resource
	private LoginUserInfo loginUser;

	private List<User> hasLoanRoleUsers;

	private Date registerTimeStart;
	private Date registerTimeEnd;
	/**注册月份*/
	private String month;
	/**是否实名*/
	private boolean real;
	/**是否为投资用户*/
	private boolean isInvestor;

	public UserList() {
		setCountHql(COUNT_HQL);
		setHql(HQL);
		final String[] RESTRICTIONS = { "user.id like #{userList.example.id}",
				"user.username like #{userList.example.username}",
				"user.mobileNumber like #{userList.example.mobileNumber}",
				"user.status like #{userList.example.status}",
				"user.email like #{userList.example.email}",
				"user.referrer like #{userList.example.referrer}",
				"user.registerTime >= #{userList.registerTimeStart}",
				"user.registerTime <= #{userList.registerTimeEnd}",
				"user.subst.userId like #{userList.example.subst.userId}",
				"user in elements(role.users) and role.id = #{userList.example.roles[0].id}",
				"user.lastLoginArea.parent.id = #{userList.example.lastLoginArea.parent.id}"};
		setRestrictionExpressionStrings(Arrays.asList(RESTRICTIONS));
	}

	private List<User> selectedUsers;

	private String title;
	private String message;
	private String emailTitle;
	private String emailContent;

	@Override
	protected void initExample() {
		User user = new User();
		List<Role> roles = new ArrayList<Role>();
		roles.add(new Role());
		user.setRoles(roles);
		Area pro = new Area();
		Area lastLoginArea = new Area();
		lastLoginArea.setParent(pro);
		user.setLastLoginArea(lastLoginArea);
		Subst subst = new Subst();
		user.setSubst(subst);
		setExample(user);
	}

	@Override
	public User getLazyModelRowData(String rowKey) {
		List<User> users = (List<User>) getLazyModel().getWrappedData();
		for (User user : users) {
			if (user.getId().equals(rowKey)) {
				return user;
			}
		}
		return null;
	}

	@Override
	public Object getLazyModelRowKey(User user) {
		return user.getId();
	}

	/**
	 * 给被选中的用户发站内信。
	 */
	@Transactional(readOnly = false)
	public void sendMessageToSelectedUsers() {
		if (getSelectedUsers().size() == 0) {
			FacesUtil.addErrorMessage("请选择用户！");
		} else {
			for (User user : getSelectedUsers()) {
				InBox inbox = new InBox();
				inbox.setId(IdGenerator.randomUUID());
				inbox.setRecevier(user);
				inbox.setSender(new User("admin"));
				inbox.setReceiveTime(new Date());
				inbox.setContent(message);
				inbox.setStatus(MessageConstants.InBoxConstants.NOREAD);
				inbox.setTitle(title);
				getHt().save(inbox);
			}
			RequestContext.getCurrentInstance().addCallbackParam("sendSuccess",
					true);
			FacesUtil.addInfoMessage("发送成功！");
			getSelectedUsers().clear();
			this.title = null;
			this.message = null;
		}
	}
	
	/**
	 * 给被选中的用户发邮件。
	 */
	@Transactional(readOnly = false)
	public void sendEmailToSelectedUsers() {
		log.debug("sendEmailToSelectedUsers start !");
		//给图片路径添加域名
		//FIXME:有bug，"ps2"那地方
		emailContent=emailContent.replace("/ps2/upload/", FacesUtil.getCurrentAppUrl()+"/upload/");
		if (getSelectedUsers().size() == 0) {
			FacesUtil.addErrorMessage("请选择用户！");
		} else {
			boolean flag  =  false;
			//如果用户邮箱为空，略过
			for (User user : getSelectedUsers()) {
				if (user.getEmail() != null && !"".equals(user.getEmail())) {
					try {
						mailService.sendMail(user.getEmail(), emailTitle, emailContent);
						flag = true;
					} catch (MailSendErrorException e) {
						log.debug(user.getUsername()+"的邮箱"+user.getEmail()+"不存在！");
						FacesUtil.addInfoMessage(user.getUsername()+"的邮箱"+user.getEmail()+"不存在！");
					}	
				}else {
					log.debug(user.getUsername()+"用户邮箱为空！");
					FacesUtil.addInfoMessage(user.getUsername()+"用户邮箱为空！");
				}
			}
			if (flag) {
				RequestContext.getCurrentInstance().addCallbackParam("sendSuccess", true);
				FacesUtil.addInfoMessage("邮件发送成功！");
				getSelectedUsers().clear();
				this.emailTitle = null;
				this.emailContent = null;
			}
			
		}
	}

	/**
	 * 获取新注册的用户，按照注册时间倒序排列
	 * 
	 * @param count
	 *            新注册用户个数
	 * @return
	 */
	public List<User> getNewUsers(int count) {
		DetachedCriteria criteria = DetachedCriteria.forClass(User.class);
		criteria.addOrder(Order.desc("registerTime"));
		getHt().setCacheQueries(true);
		return getHt().findByCriteria(criteria, 0, count);
	}

	/**
	 * 获取有借款权限的用户
	 */
	@SuppressWarnings({ "unchecked" })
	public List<User> allHasLoanRoleUser() {
		List<User> users = getHt().find("from User");
		List<User> hasLoanRoleUser = new ArrayList<User>();
		for (User user : users) {
			for (Role role : user.getRoles()) {
				if (role.getId().equals("LOANER")
						|| role.getId().equals("ADMINISTRATOR")) {
					hasLoanRoleUser.add(user);
				}
			}
		}
		return hasLoanRoleUser;
	}

	public List<User> getHasLoanRoleUsers() {
		if (this.hasLoanRoleUsers == null) {
			this.hasLoanRoleUsers = allHasLoanRoleUser();
		}
		return this.hasLoanRoleUsers;
	}

	/**
	 * @author wanghm 根据用户名模糊查询符合条件的用户，最多返回50条记录
	 */
	@SuppressWarnings("unchecked")
	public List<User> queryUsersByUserName(String username) {
		if (log.isDebugEnabled())
			log.debug("模糊查询用户名，传入的值为：" + username);
		User example = new User();
		example.setUsername("%" + username + "%");
		DetachedCriteria criteria = DetachedCriteria.forClass(getEntityClass());
		criteria.add(Restrictions.like("username", "%" + username + "%"));
		return getHt().findByCriteria(criteria, 0, 50);
	}
	
	/**
	 * @author wanghm 根据userName或者mobile模糊查询符合条件的用户名称，最多返回50条记录
	 */
	@SuppressWarnings("unchecked")
	public List<User> queryUserNamesStrByUserNameOrMobile(String username) {
		if (log.isDebugEnabled())
			log.debug("模糊查询用户名，传入的值为：" + username);
		User example = new User();
		//username query
		example.setUsername("%" + username + "%");
		DetachedCriteria criteria = DetachedCriteria.forClass(getEntityClass());
		criteria.add(Restrictions.like("username", "%" + username + "%"));
		List tempList=getHt().findByCriteria(criteria, 0, 50);
		List nameList=new ArrayList();
		for(int i=0;i<tempList.size();i++){
			User u=(User) tempList.get(i);
			nameList.add(u.getUsername());
		}
		//moible query
		example.setUsername(null);
		example.setMobileNumber("%" + username + "%");
		DetachedCriteria criteriaMobile = DetachedCriteria.forClass(getEntityClass());
		criteriaMobile.add(Restrictions.like("mobileNumber", "%" + username + "%"));
		tempList=getHt().findByCriteria(criteriaMobile, 0, 50);
		for(int i=0;i<tempList.size();i++){
			User u=(User) tempList.get(i);
			nameList.add(u.getUsername());
		}
		return nameList;
	}
	
	/**
	 * @author wanghm 根据用户名模糊查询符合条件的用户，最多返回50条记录
	 */
	@SuppressWarnings("unchecked")
	public List<User> querySubstUsersByUserName(String username) {
		if (log.isDebugEnabled())
			log.debug("模糊查询用户名，传入的值为：" + username);
		String hql = "from User u where u.username like '%" + username
				+ "%' and u.subst.userId='" + loginUser.getLoginUserId() + "'";
		return getHt().find(hql);
	}
	

	public void setHasLoanRoleUsers(List<User> hasLoanRoleUsers) {
		this.hasLoanRoleUsers = hasLoanRoleUsers;
	}

	public List<User> getSelectedUsers() {
		return selectedUsers;
	}

	public void setSelectedUsers(List<User> selectedUsers) {
		this.selectedUsers = selectedUsers;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getRegisterTimeStart() {
		return registerTimeStart;
	}

	public void setRegisterTimeStart(Date registerTimeStart) {
		this.registerTimeStart = registerTimeStart;
	}

	public Date getRegisterTimeEnd() {
		return registerTimeEnd;
	}

	public void setRegisterTimeEnd(Date registerTimeEnd) {
		this.registerTimeEnd = registerTimeEnd;
	}

	public String getEmailTitle() {
		return emailTitle;
	}

	public void setEmailTitle(String emailTitle) {
		this.emailTitle = emailTitle;
	}

	public String getEmailContent() {
		return emailContent;
	}

	public void setEmailContent(String emailContent) {
		this.emailContent = emailContent;
	}

	public String getMonth(){
		return this.month;
	}
	
	public void setMonth(String month) {
		Date date = DateUtil.StringToDate(month, DateStyle.YYYY_MM);
		this.setRegisterTimeStart(date);
		this.setRegisterTimeEnd(DateUtil.addSecond(DateUtil.addMonth(date, 1),-1));
		this.month = month;
	}

	public boolean getReal(){
		return this.real;
	}
	
	public void setReal(boolean real) {
		if(real){
			this.addRestriction("user in elements(role.users) and role.id in ('INVESTOR','LOANER')");
		}
		this.real = real;
	}

	public boolean getIsInvestor() {
		return isInvestor;
	}

	public void setIsInvestor(boolean isInvestor) {
		if(isInvestor){
			this.addRestriction("(select count(i.id) from Invest i where i.user.id = user.id and i.status != 'cancel') > 0");
		}
		this.isInvestor = isInvestor;
	}
	
	

}
