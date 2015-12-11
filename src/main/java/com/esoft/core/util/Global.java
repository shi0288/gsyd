package com.esoft.core.util;

import java.io.File;
/**
 * 
 * 获取项目根路径
 * @author ada
 *
 */
public class Global {
	 private static  String sysRootPath="";
	  private static String classpath = "";
	  static{
		  classpath = Global.class.getResource("/").getPath();
		  classpath = classpath.substring(1,classpath.length()-1);
			  if (!classpath.substring(0, 1)
		              .equals("/")
		         && !classpath.substring(1, 2)
		                     .equals(":")) {
				  classpath = "/" + classpath;
			  }
			  /**
			   * 系统根路径
			   */
			  sysRootPath = classpath;
				if(sysRootPath != null){
					File f = new File(sysRootPath);
					sysRootPath = f.getParentFile().getParent();
					sysRootPath = sysRootPath.replaceAll("\\\\","/");
				}
	  }
	  public static String getClassPath(){
		  return classpath;
	  }
	  
	  public static String getSysRootPath(){
		  return sysRootPath;
	  }
	  
	  
	  public static void main(String args[] )
	  {
		  System.out.println(getClassPath());
		  System.out.println(getSysRootPath());
	  }
}
