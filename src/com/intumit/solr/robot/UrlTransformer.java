package com.intumit.solr.robot;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAUtil.AutoLink;

public class UrlTransformer {
	
	public interface Transform {
		/** 置換字串；若回傳null，則保持原狀 */
		String call(String url); 
	}
	
	public static class Base64UrlEncodeAndPrepend implements Transform {
		
		public final String prefix;

		public Base64UrlEncodeAndPrepend(String prefix) {
			this.prefix = prefix;
		}
		
		@Override
		public String call(String url) {
			String out = null;
			try {
				return " " + prefix + URLEncoder.encode(base64Encode(url), "utf-8") + " ";
			} catch (UnsupportedEncodingException e) {}
			return out;
		}
		
	}
	
	public static String base64Encode(String url){
		String out = null;
		try {
			out = Base64.encodeBase64String(url.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {}
		return out;
	}
	
	public static String base64UrlEncodeAndPrepend(String text, String prefix){
		return process(text, new Base64UrlEncodeAndPrepend(prefix));
	}
	
	public static String process(String text, Transform tranform){
		
		String out = null;
		if(text != null){
			StringBuffer sb = new StringBuffer();
			Matcher matcher = AutoLink.patternUrl.matcher(text);
			while (matcher.find()) {
				String url = StringUtils.trim(matcher.group(0));
				String transformed = tranform.call(url);
				if(transformed != null){
					matcher.appendReplacement(sb, transformed);
				}
			}
			matcher.appendTail(sb);
			out = sb.toString();
		}
		return out;
	}
	
}
