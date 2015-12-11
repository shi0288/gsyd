package com.esoft.yeepay.sign;

import java.io.File;
import java.io.UnsupportedEncodingException;

import com.esoft.yeepay.trusteeship.YeePayConstants;

/**
 * 生成密钥 校验密钥
 * @author 林志明
 */
public class CFCASignUtil {
	public static void main(String[] args) {
		String a=sign("aa");
	}
	
	/**
	 * 生成密文  加密    P2P --> 易宝
	 */
	public static String sign(String source) {
		if(source == null) {
			return "";
		}
		String path = CFCASignUtil.class.getClassLoader().getResource(YeePayConstants.Config.CERTIFICATE_NAME).getPath();
		try {
			path = java.net.URLDecoder.decode(path,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} 
		return SignUtil.sign(source, path, YeePayConstants.Config.PASS_WORD);
//		return "test";
	}
	
	/**
	 * 校验密文   解密   易宝 --> P2P
	 */
	public static boolean isVerifySign(String source, String sign) {
//		return true;
		try {
			SignUtil.verifySign(source, sign, "yeepay.com");
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}
}
