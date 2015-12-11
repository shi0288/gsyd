package com.esoft.core.util;

import org.apache.commons.codec.binary.Base64;

/**
 * @ClassName: Base64Coder
 * @Description: Base64加密解密
 * @author 李哲
 * @date 2015-1-21 下午12:00:21
 */
public class Base64Coder {

	/**
	 * base64解密
	 * 
	 * @param key
	 * @return
	 * @throws Exception
	 */
	public static byte[] decryptBase64(String key) {
		int len, n = 1, m = 0;
		len = key.length();
		String strs1 = key.substring(0, len / 2);
		String strs2 = key.substring(len / 2);
		key = "";
		for (int i = 0; i < len; i++) {
			if (i == 0) {
				key += strs1.charAt(0);
			} else if (i == (len - 1)) {
				key += strs2.charAt(len - (len / 2 + 1));
			} else if (i % 2 == 0) {
				key += strs1.charAt(n++);
			} else {
				key += strs2.charAt(m++);
			}
		}
		return Base64.decodeBase64(key);
	}

}
