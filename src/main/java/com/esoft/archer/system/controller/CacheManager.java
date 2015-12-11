package com.esoft.archer.system.controller;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;

import com.esoft.archer.system.SystemConstants;
import com.esoft.core.annotations.Logger;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
import com.esoft.core.util.StringManager;

@Component
@Scope(ScopeType.REQUEST)
public class CacheManager {

	static StringManager sm = StringManager.getManager(SystemConstants.Package);
	@Logger
	static Log log;

	@Resource
	HibernateTemplate ht;

	public void clearCache() {
		SessionFactory sf = ht.getSessionFactory();

		sf.getCache().evictCollectionRegions();
		sf.getCache().evictEntityRegions();
		sf.getCache().evictQueryRegions();
		sf.getCache().evictDefaultQueryRegion();
		final String message = sm.getString("log.clearCacheSuccess");
		if (log.isDebugEnabled()) {
			log.debug(message);
		}
		FacesUtil.addInfoMessage(message);
	}
}
