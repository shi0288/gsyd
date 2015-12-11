package com.esoft.archer.config.controller;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.esoft.archer.common.controller.EntityHome;
import com.esoft.archer.config.model.Config;
import com.esoft.archer.config.model.ConfigType;
import com.esoft.archer.config.service.ConfigService;
import com.esoft.core.annotations.ScopeType;
import com.esoft.core.jsf.util.FacesUtil;
/**
 * 
 * 用户安全配置
 * @author ada
 *
 */
@Component
@Scope(ScopeType.VIEW)
public class UserSafeConfigHome extends EntityHome<Config> implements
java.io.Serializable {
	@Resource
	ConfigService configService;
	//同一类别的配置信息
	List<Config> configs = null; 
	//类别标识
	private String configType;
	//特殊配置id[如:user_safe.user_disable_time]
	private String specialProperty;

	public void configDetail(){
		//获取该类别下的所有配置信息
		configs = configService.getConfigsByType(configType);
		//特殊处理[自动解锁时间]
		specialTimes();
	}
	/**
	 * 特殊处理[自动解锁时间]
	 */
	public void specialTimes(){
		if(null!=specialProperty&&!specialProperty.equals("")){
			String times = null;
			for(Config config:configs){
				if(null!=config.getId()&&specialProperty.equals(config.getId())){
					times = config.getValue();
					//容器中删除特殊数据
					configs.remove(config);
					break;
				}
			}
			//拆分毫秒为天\小时\分钟\秒钟\毫秒,并传递到前台
			if(null!=times&&!"".equals(times)){
				Long times_ = Long.valueOf(times);
				long[] times_array =  formatTime(times_*1000);
				FacesUtil.setRequestAttribute("days_time", times_array[0]);
				FacesUtil.setRequestAttribute("hours_time", times_array[1]);
				FacesUtil.setRequestAttribute("mins_time", times_array[2]);
				FacesUtil.setRequestAttribute("cons_time", times_array[3]);
			}
		}
	}
	/**
	 * 保存\编辑配置信息
	 */
	@Transactional(readOnly=false)
	public void configSave() {
		//更新数据
		for(Config config:configs){
			super.getBaseService().saveOrUpdate(config);
		}
		//特殊处理
		specialSaveOption();
		FacesUtil.addInfoMessage("操作成功!");
	}
	/**
	 * 特殊处理[自动解锁时间]
	 */
	public void specialSaveOption(){
		//特殊处理
		if(null!=specialProperty&&!"".equals(specialProperty)){
			//获取表单中特殊处理数据[自动解锁时间]
			String days_time = FacesUtil.getParameter("days_time");
			String hours_time = FacesUtil.getParameter("hours_time");
			String mins_time = FacesUtil.getParameter("mins_time");
			String cons_time = FacesUtil.getParameter("cons_time");
			//强制转换成对应的时间
			long days_times = Integer.valueOf(days_time)*60*60*24;
			long hours_times = Integer.valueOf(hours_time)*60*60;
			long mins_times = Integer.valueOf(mins_time)*60;
			long cons_times = Integer.valueOf(cons_time);
			//特殊处理[自动解锁时间]
			Config config = new Config();
			config.setId(specialProperty);
			ConfigType type = new ConfigType();
			type.setId(configType);
			config.setConfigType(type);
			config.setValue((days_times+hours_times+mins_times+cons_times)+"");
			super.getBaseService().saveOrUpdate(config);
		}
	}
	/**
	 * 毫秒转化为天\小时\分钟\秒钟\毫秒
	 * @param ms
	 * @return 
	 * 天=long[0];小时=long[1];分钟=long[2];秒钟=long[3];毫秒=long[4]
	 */
	public static long[] formatTime(long ms) {
		//方法返回值
		long[] times = new long[5];
		int ss = 1000;
		int mi = ss * 60;
		int hh = mi * 60;
		int dd = hh * 24;
		//将毫秒转为天\小时\分钟\秒钟\毫秒
		long day = ms / dd;
		long hour = (ms - day * dd) / hh;
		long minute = (ms - day * dd - hour * hh) / mi;
		long second = (ms - day * dd - hour * hh - minute * mi) / ss;
		long milliSecond = ms - day * dd - hour * hh - minute * mi - second * ss;
		//将毫秒转为天\小时\分钟\秒钟\毫秒对应的字符
		String strDay = day < 10 ? "0" + day : "" + day; //天
		String strHour = hour < 10 ? "0" + hour : "" + hour;//小时
		String strMinute = minute < 10 ? "0" + minute : "" + minute;//分钟
		String strSecond = second < 10 ? "0" + second : "" + second;//秒
		String strMilliSecond = milliSecond < 10 ? "0" + milliSecond : "" + milliSecond;//毫秒
		strMilliSecond = milliSecond < 100 ? "0" + strMilliSecond : "" + strMilliSecond;
		//将转化后的天\小时\分钟\秒钟\毫秒,存放到数组.
		times[0]=Long.valueOf(strDay);
		times[1]=Long.valueOf(strHour);
		times[2]=Long.valueOf(strMinute);
		times[3]=Long.valueOf(strSecond);
		times[4]=Long.valueOf(strMilliSecond);
		return times;
	}

	public List<Config> getConfigs() {
		return configs;
	}

	public void setConfigs(List<Config> configs) {
		this.configs = configs;
	}

	public String getConfigType() {
		return configType;
	}

	public void setConfigType(String configType) {
		this.configType = configType;
	}

	public String getSpecialProperty() {
		return specialProperty;
	}

	public void setSpecialProperty(String specialProperty) {
		this.specialProperty = specialProperty;
	}
}
