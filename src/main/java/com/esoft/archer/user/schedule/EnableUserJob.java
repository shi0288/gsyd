package com.esoft.archer.user.schedule;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

import com.esoft.archer.user.exception.UserNotFoundException;
import com.esoft.archer.user.service.UserService;

/**
 * 解锁用户
 * 
 * @author Administrator
 * 
 */

@Component
public class EnableUserJob implements Job {

	public static final String USER_ID = "user_id";

	Logger log = Logger.getLogger(EnableUserJob.class);

	@Resource
	private UserService userService;

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		String userId = context.getJobDetail().getJobDataMap()
				.getString(USER_ID);
		if (log.isDebugEnabled()) {
			log.debug("enable user id:"+userId);			
		}
		try {
			userService.enableUser(userId);
		} catch (UserNotFoundException e) {
			log.info(e);
		}
	}

}
