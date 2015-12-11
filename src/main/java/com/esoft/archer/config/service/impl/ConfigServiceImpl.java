package com.esoft.archer.config.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.hibernate.ObjectNotFoundException;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Service;

import com.esoft.archer.config.model.Config;
import com.esoft.archer.config.service.ConfigService;

/**
 * Company: jdp2p <br/>
 * Copyright: Copyright (c)2013 <br/>
 * Description:
 * 
 * @author: wangzhi
 * @version: 1.0 Create at: 2014-1-17 上午10:21:22
 * 
 *           Modification History: <br/>
 *           Date Author Version Description
 *           ------------------------------------------------------------------
 *           2014-1-17 wangzhi 1.0
 */
@Service("configService")
public class ConfigServiceImpl implements ConfigService {

	@Resource
	HibernateTemplate ht;

	@Override
	public String getConfigValue(String configId) {
		Config config = ht.get(Config.class, configId);
		if (config != null) {
			return config.getValue();
		}
		throw new ObjectNotFoundException(Config.class, "config ID:"
				+ configId + "对象为空！");
	}

	@Override
	public List<Config> getConfigsByType(String configType) {
		List<Config> list_configs = ht.find("from Config cfg where cfg.configType.id=?", configType);
		return list_configs;
	}

}
