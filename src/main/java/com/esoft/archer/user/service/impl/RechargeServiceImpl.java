package com.esoft.archer.user.service.impl;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.coupon.model.UserCoupon;
import com.esoft.archer.risk.FeeConfigConstants.FeePoint;
import com.esoft.archer.risk.FeeConfigConstants.FeeType;
import com.esoft.archer.risk.service.impl.FeeConfigBO;
import com.esoft.archer.user.UserBillConstants.OperatorInfo;
import com.esoft.archer.user.UserConstants;
import com.esoft.archer.user.UserConstants.RechargeStatus;
import com.esoft.archer.user.model.Recharge;
import com.esoft.archer.user.model.RechargeBankCard;
import com.esoft.archer.user.model.RechargeBankCardImpl;
import com.esoft.archer.user.model.UserBill;
import com.esoft.archer.user.service.RechargeService;
import com.esoft.archer.user.service.UserBillService;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.DateStyle;
import com.esoft.core.util.DateUtil;
 
/**
 * Company: jdp2p <br/>
 * Copyright: Copyright (c)2013 <br/>
 * Description:
 * 
 * @author: wangzhi
 * @version: 1.0 Create at: 2014-1-27 上午10:28:55
 * 
 *           Modification History: <br/>
 *           Date Author Version Description
 *           ------------------------------------------------------------------
 *           2014-1-27 wangzhi 1.0
 */
@Service("rechargeService")
public class RechargeServiceImpl implements RechargeService {

	@Resource
	HibernateTemplate ht;

	@Resource
	RechargeBO rechargeBO;
	
	@Resource
	private UserBillService userBillService;

	@Resource
	private FeeConfigBO feeConfigBO;

	@Override
	public double calculateFee(double amount) {
		return feeConfigBO.getFee(FeePoint.RECHARGE, FeeType.FACTORAGE, null,
				null, amount);
	}

	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void rechargePaySuccess(String rechargeId) {
		Recharge recharge = ht
				.get(Recharge.class, rechargeId);
		ht.evict(recharge);
		recharge = ht
				.get(Recharge.class, rechargeId, LockMode.UPGRADE);
		if (recharge != null
				&& recharge.getStatus().equals(
						UserConstants.RechargeStatus.WAIT_PAY)) {
			rechargeBO.rechargeSuccess(recharge);
		}

	}

	/**
	 * 生成充值id
	 * 
	 * @return
	 */
	private String generateId() {
		String gid = DateUtil.DateToString(new Date(), DateStyle.YYYYMMDD);
		String hql = "select recharge from Recharge recharge where recharge.id = (select max(rechargeM.id) from Recharge rechargeM where rechargeM.id like ?)";
		List<Recharge> contractList = ht.find(hql, gid + "%");
		Integer itemp = 0;
		if (contractList.size() == 1) {
			Recharge recharge = contractList.get(0);
			ht.lock(recharge, LockMode.UPGRADE);
			String temp = recharge.getId();
			temp = temp.substring(temp.length() - 6);
			itemp = Integer.valueOf(temp);
		}
		itemp++;
		gid += String.format("%08d", itemp);
		return gid;
	}

	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public String createOfflineRechargeOrder(Recharge recharge) {
		// 往recharge中插入值。
		recharge.setId(generateId());
		recharge.setFee(calculateFee(recharge.getActualMoney()));
		// 用rechargeWay进行判断，判断是要跳转到银行卡还是支付平台
		// recharge.setRechargeWay("借记卡");
		recharge.setIsRechargedByAdmin(false);
		recharge.setTime(new Date());
		recharge.setStatus(UserConstants.RechargeStatus.WAIT_PAY);
		ht.save(recharge);
		
		return "success";
	}
	
	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public String createRechargeOrder(Recharge recharge) {
		// 往recharge中插入值。
		recharge.setId(generateId());
		recharge.setFee(calculateFee(recharge.getActualMoney()));
		// 用rechargeWay进行判断，判断是要跳转到银行卡还是支付平台
		// recharge.setRechargeWay("借记卡");

		recharge.setIsRechargedByAdmin(false);
		recharge.setTime(new Date());
		recharge.setStatus(UserConstants.RechargeStatus.WAIT_PAY);
		ht.save(recharge);
		return FacesUtil.getHttpServletRequest().getContextPath()
				+ "/to_recharge/" + recharge.getId();
	}

	@Override
	public String getBankNameByNo(final String bankNo){
		List<RechargeBankCard> banks = getBankCardsList();
		for(RechargeBankCard bank : banks){
			if(StringUtils.equals(bank.getNo(), bankNo)){
				return bank.getBankName();
			}
		}
		return "Not found Bank";
	}
	
	private static Properties props = new Properties(); 
	static{
		try {
		    InputStream is=	Thread.currentThread().getContextClassLoader().getResourceAsStream("banks.properties");
		    InputStreamReader inr=new InputStreamReader(is,"UTF-8");			
			props.load(inr);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	@Override
	public List<RechargeBankCard> getBankCardsList() {
		List<RechargeBankCard> bcs = new ArrayList<RechargeBankCard>();
		Iterator<Entry<Object, Object>> it = props.entrySet().iterator();  
		while(it.hasNext()){
			Entry<Object, Object> entry = it.next();  
            String key = (String) entry.getKey();  
            String value = (String) entry.getValue();  
			bcs.add(new RechargeBankCardImpl(key, value));
		}
		
		// bcs.add(new RechargeBankCardImpl("TESTBANK", "测试银行"));
//		bcs.add(new RechargeBankCardImpl("ABC", "农业银行"));
		// bcs.add(new RechargeBankCardImpl("GNXS", "广州市农信社"));
		// bcs.add(new RechargeBankCardImpl("BCCB", "北京银行"));
		// bcs.add(new RechargeBankCardImpl("GZCB", "广州市商业银行"));
		// bcs.add(new RechargeBankCardImpl("BJRCB", "北京农商行"));
		// bcs.add(new RechargeBankCardImpl("HCCB", "杭州银行"));
//		bcs.add(new RechargeBankCardImpl("BOC", "中国银行"));
		// bcs.add(new RechargeBankCardImpl("HKBCHINA", "汉口银行"));
		// bcs.add(new RechargeBankCardImpl("BOS", "上海银行"));
		// bcs.add(new RechargeBankCardImpl("HSBANK", "徽商银行"));
		// bcs.add(new RechargeBankCardImpl("CBHB", "渤海银行"));
		// bcs.add(new RechargeBankCardImpl("HXB", "华夏银行"));
//		bcs.add(new RechargeBankCardImpl("CCB", "建设银行"));
//		bcs.add(new RechargeBankCardImpl("ICBC", "工商银行"));
		// bcs.add(new RechargeBankCardImpl("CCQTGB", "重庆三峡银行"));
		// bcs.add(new RechargeBankCardImpl("NBCB", "宁波银行"));
		// bcs.add(new RechargeBankCardImpl("CEB", "光大银行"));
		// bcs.add(new RechargeBankCardImpl("NJCB", "南京银行"));
		// bcs.add(new RechargeBankCardImpl("CIB", "兴业银行"));
		// bcs.add(new RechargeBankCardImpl("PSBC", "中国邮储银行"));
		// bcs.add(new RechargeBankCardImpl("CITIC", "中信银行"));
		// bcs.add(new RechargeBankCardImpl("SHRCB", "上海农村商业银行"));
//		bcs.add(new RechargeBankCardImpl("CMB", "招商银行"));
		// bcs.add(new RechargeBankCardImpl("SNXS", "深圳农村商业银行"));
		// bcs.add(new RechargeBankCardImpl("CMBC", "民生银行"));
		// bcs.add(new RechargeBankCardImpl("SPDB", "浦东发展银行"));
		// bcs.add(new RechargeBankCardImpl("COMM", "交通银行"));
		// bcs.add(new RechargeBankCardImpl("SXJS", "晋城市商业银行"));
		// bcs.add(new RechargeBankCardImpl("CSCB", "长沙银行"));
		// bcs.add(new RechargeBankCardImpl("SZPAB", "平安银行"));
		// bcs.add(new RechargeBankCardImpl("CZB", "浙商银行"));
		// bcs.add(new RechargeBankCardImpl("UPOP", "银联在线支付"));
		// bcs.add(new RechargeBankCardImpl("CZCB", "浙江稠州商业银行"));
		// bcs.add(new RechargeBankCardImpl("WZCB", "温州市商业银行"));
		// bcs.add(new RechargeBankCardImpl("GDB", "广东发展银行"));
		return bcs;
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public void rechargeByAdmin(UserBill userBill) {
		Recharge r = new Recharge();
		r.setActualMoney(userBill.getMoney());
		r.setFee(0D);
		r.setId(generateId());
		r.setIsRechargedByAdmin(true);
		r.setRechargeWay("admin");
		r.setStatus(RechargeStatus.SUCCESS);
		r.setSuccessTime(new Date());
		r.setTime(new Date());
		r.setUser(userBill.getUser());
		ht.save(r);
		userBillService.transferIntoBalance(userBill.getUser().getId(),
				userBill.getMoney(), OperatorInfo.ADMIN_OPERATION,
				userBill.getDetail());
	}

	@Override
	@Transactional(rollbackFor=Exception.class)
	public void rechargeByAdmin(String rechargeId) {
		rechargePaySuccess(rechargeId);
		Recharge r = ht.get(Recharge.class, rechargeId);
		r.setIsRechargedByAdmin(true);
		ht.update(r);
	}

	@Override
	@Transactional(readOnly = false, rollbackFor = Exception.class)
	public void rechargePayFail(String rechargeId) {
		Recharge recharge = ht
				.get(Recharge.class, rechargeId);
		ht.evict(recharge);
		recharge = ht
				.get(Recharge.class, rechargeId, LockMode.UPGRADE);
		if (recharge != null
				&& recharge.getStatus().equals(
						UserConstants.RechargeStatus.WAIT_PAY)) {
			recharge.setStatus(UserConstants.RechargeStatus.FAIL);
			ht.merge(recharge);
		}
	}

}
