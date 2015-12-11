package com.esoft.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONObject;

public class IPUtil {
	public static void main(String[] args) {
		System.out.println(GetAddressByIp("116.22.252.81"));
	}
	/**
	 * 根据IP地址获取地域编码
	 * 当为直辖市时，返回任取下一级
	 * 不会报错，所有错误均返回空字符串 ""
	 * @param IP
	 * @return
	 */
	public static String GetAddressByIp(String IP) {
		String resout = "";
		try {
			String str = getJsonContent("http://ip.taobao.com/service/getIpInfo.php?ip=" + IP);
			if(str == null || "".equals(str)){
				return "";
			}
			JSONObject obj = new JSONObject(str);
			JSONObject obj2 = obj.getJSONObject("data");
			int code = obj.getInt("code");
			if (code == 0) {
				resout = obj2.getString("city_id");
				//北京
				if("110000".equals(resout)){
					resout = "110100";
				}
				//天津
				if("120000".equals(resout)){
					resout = "120100";
				}
				//上海
				if("310000".equals(resout)){
					resout = "310100";
				}
				//天津
				if("500000".equals(resout)){
					resout = "500100";
				}
			} else {
				resout = "";
			}
		} catch (Exception e) {
			resout = "";
		}
		return resout;

	}

	public static String getJsonContent(String urlStr) {
		try {// 获取HttpURLConnection连接对象
			URL url = new URL(urlStr);
			HttpURLConnection httpConn = (HttpURLConnection) url
					.openConnection();
			// 设置连接属性
			httpConn.setConnectTimeout(3000);
			httpConn.setDoInput(true);
			httpConn.setRequestMethod("GET");
			// 获取相应码
			int respCode = httpConn.getResponseCode();
			if (respCode == 200) {
				return ConvertStream2Json(httpConn.getInputStream());
			}
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}
		return "";
	}

	private static String ConvertStream2Json(InputStream inputStream) {
		String jsonStr = "";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		// 将输入流转移到内存输出流中
		try {
			while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
				out.write(buffer, 0, len);
			}
			jsonStr = new String(out.toByteArray());
		} catch (IOException e) {
		}
		return jsonStr;
	}
}