package com.esoft.core.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletRequest;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

public class HttpClientUtil {

	private static Logger log = Logger.getLogger(HttpClientUtil.class);

	public final static int SUCCESS = 1;

	public final static int FAIL = 0;

	public static String getResponseBodyAsString(String url) {
		GetMethod get = new GetMethod(url);

		HttpClient client = new HttpClient();
		try {
			client.executeMethod(get);
			// System.out.println(url);
			// System.out.println( get.getResponseCharSet() );
			return get.getResponseBodyAsString();

		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	public static String get(String url, Map<String, String> params) {
		StringBuffer sb = new StringBuffer(url);
		sb.append("?");
		for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			sb.append(key);
			sb.append("=");
			sb.append(params.get(key));
			if (iterator.hasNext()) {
				sb.append("&");				
			}
		}
		HttpClient client = new HttpClient();
		GetMethod get = new GetMethod(sb.toString());
		try {
			client.executeMethod(get);
			return get.getResponseBodyAsString();
			
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static int saveHtmlFromRemoteSite(String url, String filePath) {
		File file = new File(filePath);

		return saveHtmlFromRemoteSite(url, file);

	}

	public static int saveHtmlFromRemoteSite(String url, File file) {
		if (!file.exists()) {
			// file.mkdirs();
			try {
				File temp = file.getParentFile();
				if (!temp.exists()) {
					temp.mkdirs();
				}
				file.createNewFile();

			} catch (IOException e) {
				e.printStackTrace();
				return FAIL;
			}
		}

		try {
			final String response = getResponseBodyAsString(url);
			// System.out.println(response);
			// FileUtils.writeStringToFile(file, response , EncodingUtil.UTF8);
			FileUtils.writeByteArrayToFile(file, response.getBytes("utf-8"));
		} catch (IOException e) {
			e.printStackTrace();
			return FAIL;
		}

		return SUCCESS;
	}

	public static String requestParametersToString(ServletRequest request) {
		StringBuffer sb = new StringBuffer();
		Map map = request.getParameterMap();
		for (Object str : map.keySet()) {
			sb.append(str);
			sb.append(":");
			sb.append(request.getParameter(str.toString()));
			sb.append("  ");
		}
		return sb.toString();
	}

	public static String post(String url, Map<String, String> params) {
		HttpClient httpclient = new HttpClient();
		PostMethod post = new PostMethod(url);
		for (String key : params.keySet()) {
			String value = params.get(key);
			if (StringUtils.isNotEmpty(value)) {
				post.setParameter(key, params.get(key));				
			}
		}
		try {
			httpclient.executeMethod(post);
			return post.getResponseBodyAsString();
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return url;
	}

}
