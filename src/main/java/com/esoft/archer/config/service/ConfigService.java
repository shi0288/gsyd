package com.esoft.archer.config.service;

import java.util.List;

import com.esoft.archer.config.model.Config;


/**  
 * Company:     jdp2p <br/> 
 * Copyright:   Copyright (c)2013 <br/>
 * Description:  
 * @author:     wangzhi  
 * @version:    1.0
 * Create at:   2014-1-17 上午10:20:41  
 *  
 * Modification History: <br/>
 * Date         Author      Version     Description  
 * ------------------------------------------------------------------  
 * 2014-1-17      wangzhi      1.0          
 */
public interface ConfigService {
	/**
	 * 通过配置编号得到配置的值
	 * 
	 * @param configId
	 * @return
	 */
	public String getConfigValue(String configId);
	/**
	 * 通过配置类别,获取所有配置信息
	 * @param configType
	 * @return
	 */
	public List<Config> getConfigsByType(String configType);
}
