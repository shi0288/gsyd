package com.esoft.archer.user.schedule;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.ObjectNotFoundException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.esoft.archer.config.service.ConfigService;
import com.esoft.core.annotations.Logger;

/**
 * 资金记录篡改后，自动报警
 * 
 * @author Administrator
 * 
 */

@Component
public class TamperAlert implements Job {

	public static final String TEMPER_ALERT = "tamper_alert";

	@Logger
	Log log;

	@Resource(name = "htBackup")
	private HibernateTemplate htBackup;

	@Resource
	private HibernateTemplate ht;

	@Resource
	private ConfigService configService;

	private boolean isOpen = false;

	@Override
	public void execute(JobExecutionContext arg0) throws JobExecutionException {
		try {
			if ("1".equals(configService.getConfigValue(TEMPER_ALERT))) {
				isOpen = true;
			}
		} catch (ObjectNotFoundException e) {
			log.info(e);
		}
		//遍历，检查每个用户的最新一条资金记录信息
		//FIXME:如何优化性能？
	}
	

}
