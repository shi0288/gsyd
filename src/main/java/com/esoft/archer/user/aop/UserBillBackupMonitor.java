package com.esoft.archer.user.aop;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.annotation.AfterReturning;
import org.hibernate.LockMode;
import org.springframework.orm.hibernate3.HibernateTemplate;

import com.esoft.archer.user.model.UserBill;
import com.esoft.archer.user.model_backup.DBOperationBak;
import com.esoft.archer.user.service.impl.UserBillBO;
import com.esoft.core.util.GsonUtil;
import com.esoft.core.util.IdGenerator;
import com.esoft.core.util.ThreeDES;

//@Component
//@Aspect
public class UserBillBackupMonitor {

	static Log log = LogFactory.getLog(UserBillBackupMonitor.class);

	private static Properties props;
	/**
	 * 3des base64 key
	 */
	public static String THREE_DES_BASE64_KEY;
	/**
	 * 3des iv(向量)
	 */
	public static String THREE_DES_IV;
	/**
	 * 3des 加密方法／运算模式／填充模式
	 */
	public static String THREE_DES_ALGORITHM;
	/** 是否开启防篡改提醒 */
//	private static boolean isOpen = false;
	
//	String tmpStr = props.getProperty("is_open");
//	if (StringUtils.isNotEmpty(tmpStr)) {
//		isOpen = Boolean.parseBoolean(tmpStr);
//	}

	static {
		props = new Properties();
		try {
			props.load(Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("tamper_alert.properties"));
			THREE_DES_BASE64_KEY = props.getProperty("three_des_base64_key");
			THREE_DES_IV = props.getProperty("three_des_iv");
			THREE_DES_ALGORITHM = props.getProperty("three_des_algorithm");
		} catch (FileNotFoundException e) {
			log.info("找不到tamper_alert.properties文件", e);
		} catch (IOException e) {
			log.info("读取tamper_alert.properties文件出错", e);
		} catch (NullPointerException e) {
			log.info("读取tamper_alert.properties文件出错", e);
		}
	}

	@Resource(name = "htBackup")
	private HibernateTemplate ht;
	
	@Resource
	private UserBillBO userBillBO;

	/**
	 * 用户资金冻结
	 */
	@AfterReturning(argNames = "userId,money,operatorInfo,operatorDetail", value = "execution(public void com.esoft.archer.user.service.impl.UserBillBO.freezeMoney(..)) "
			+ "&& args(userId,money,operatorInfo,operatorDetail)")
	public void freezeMoney(String userId, double money, String operatorInfo,
			String operatorDetail) {
		if (log.isDebugEnabled()) {
			log.debug("freezeMoney backup:" + userId + " " + money);
		}
		saveLog(userId);
	}

	private void saveLog(String userId) {
		UserBill ub = userBillBO.getLastestBill(userId);
		if (ub != null) {
			ht.lock(ub, LockMode.UPGRADE);
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("userId", userId);
		map.put("balance", ub.getBalance());
		map.put("detail", ub.getDetail());
		map.put("frozenMoney", ub.getFrozenMoney());
		map.put("id", ub.getId());
		map.put("money", ub.getMoney());
		map.put("seqNum", ub.getSeqNum());
		map.put("time", ub.getTime());
		map.put("type", ub.getType());
		map.put("typeInfo", ub.getTypeInfo());
		
		DBOperationBak dbob = new DBOperationBak();
		dbob.setId(IdGenerator.randomUUID());
		dbob.setData(ThreeDES.encrypt(GsonUtil.toJson(map),
				THREE_DES_BASE64_KEY, THREE_DES_IV, THREE_DES_ALGORITHM));
		dbob.setOperation("insert");
		dbob.setTable("user_bill");
		dbob.setTime(new Date());
		ht.save(dbob);
	}

	/**
	 * 从用户冻结金额中转出
	 */
	@AfterReturning(argNames = "userId,money,operatorInfo,operatorDetail", value = "execution(public void com.esoft.archer.user.service.impl.UserBillBO.transferOutFromFrozen(..)) "
			+ "&& args(userId,money,operatorInfo,operatorDetail)")
	public void transferOutFromFrozen(String userId, double money,
			String operatorInfo, String operatorDetail) {
		if (log.isDebugEnabled()) {
			log.debug("transferOutFromFrozen backup:" + userId + " " + money);
		}
		saveLog(userId);
	}

	/**
	 * 用户资金解冻
	 */
	@AfterReturning(argNames = "userId,money,operatorInfo,operatorDetail", value = "execution(public void com.esoft.archer.user.service.impl.UserBillBO.unfreezeMoney(..)) "
			+ "&& args(userId,money,operatorInfo,operatorDetail)")
	public void unfreezeMoney(String userId, double money, String operatorInfo,
			String operatorDetail) {
		if (log.isDebugEnabled()) {
			log.debug("unfreezeMoney backup:" + userId + " " + money);
		}
		saveLog(userId);
	}

	/**
	 * 从用户余额中转出
	 */
	@AfterReturning(argNames = "userId,money,operatorInfo,operatorDetail", value = "execution(public void com.esoft.archer.user.service.impl.UserBillBO.transferOutFromBalance(..)) "
			+ "&& args(userId,money,operatorInfo,operatorDetail)")
	public void transferOutFromBalance(String userId, double money,
			String operatorInfo, String operatorDetail) {
		if (log.isDebugEnabled()) {
			log.debug("transferOutFromBalance backup:" + userId + " " + money);
		}
		saveLog(userId);
	}

	/**
	 * 转入到用户余额
	 */
	@AfterReturning(argNames = "userId,money,operatorInfo,operatorDetail", value = "execution(public void com.esoft.archer.user.service.impl.UserBillBO.transferIntoBalance(..)) "
			+ "&& args(userId,money,operatorInfo,operatorDetail)")
	public void transferIntoBalance(String userId, double money,
			String operatorInfo, String operatorDetail) {
		if (log.isDebugEnabled()) {
			log.debug("transferIntoBalance backup:" + userId + " " + money);
		}
		saveLog(userId);
	}

}
