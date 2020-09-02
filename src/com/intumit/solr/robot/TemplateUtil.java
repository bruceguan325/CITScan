package com.intumit.solr.robot;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.*;

import com.intumit.solr.robot.dictionary.CategorizedKeyValuePair;
import com.intumit.solr.robot.dictionary.CustomData;

import java.util.ArrayList;

/** 語法
 * {{NAME:VALUE}}
 * {{NAME}}
 * */
public class TemplateUtil {

	static final Pattern templatePattern = Pattern.compile("\\{\\{([a-zA-Z0-9_/\\s\\$\\+]+?)(:.+?)?\\}\\}");

	static final Pattern wholeNamePattern = Pattern.compile("^\\{\\{([a-zA-Z0-9_/\\s\\$\\+]+?)\\}\\}$");

	public interface Replacer {
		/** 置換字串；若回傳null，則保持原狀 */
		String call(String name, String val);
		/** 為了 localization */
		String call(QAContext ctx, String name, String val);
	}

	public interface Collector {
		/** 收集字串沒有回傳null */
		Map<String, Object> call(String name, String val);
	}
	
	public static class Cleaner implements Replacer {

		@Override
		public String call(String name, String val) {
			return "";
		}

		@Override
		public String call(QAContext ctx, String name, String val) {
			return "";
		}
	}
	
	public static class Replace implements Replacer {

		public final String replaceTo;

		public Replace(String replaceTo) {
			this.replaceTo = replaceTo;
		}

		@Override
		public String call(String name, String val) {
			return replaceTo;
		}

		@Override
		public String call(QAContext ctx, String name, String val) {
			return call(name, val);
	}

	}

	public static class Prepend implements Replacer {

		public final String prefix;

		public Prepend(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public String call(String name, String val) {
			return " " + prefix + val + " ";
		}

		@Override
		public String call(QAContext ctx, String name, String val) {
			return call(name, val);
	}

	}

	public static class PrependAndTransformURL implements Replacer {

		public final String prefix;

		public final String urlPrefix;

		public PrependAndTransformURL(String prefix, String urlPrefix) {
			this.prefix = prefix;
			this.urlPrefix = urlPrefix;
		}

		@Override
		public String call(String name, String val) {
			String out = null;
			try {
				out = " " + urlPrefix + URLEncoder.encode(UrlTransformer.base64Encode(prefix + val), "utf-8") + " ";
			} catch (UnsupportedEncodingException e) {}
			return out;
		}

		@Override
		public String call(QAContext ctx, String name, String val) {
			return call(name, val);
	}

	}

	public static final CustomQAReplacer CUSTOM_QA_REPLACER = new CustomQAReplacer("");
	public static class CustomQAReplacer implements Replacer {
		private String bindToTag;

		public CustomQAReplacer(String bindToTag) {
			this.bindToTag = bindToTag;
		}

		@Override
		public String call(String name, String val) {
			if (StringUtils.startsWith(name, "$")) {
				if (StringUtils.equals(name, "$NUM")) {
					return CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER);
				}
				else if (StringUtils.equals(name, "$DATETIME") || StringUtils.equals(name, "$DATE")) {
					return CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL);
				}
				else {
					String suffix = "";
					if (StringUtils.isNotEmpty(val)) {
						suffix = StringUtils.upperCase(val.replaceAll("[_/\\s\\$\\+]+", ""));
					}

					if (StringUtils.startsWith(name, "$+"))
						return CustomData.toInlineKey(name, StringUtils.split(suffix, ":")[0]);
					else if (StringUtils.isNotEmpty(suffix)) {
						return CustomData.toPartialInlineKey(name, StringUtils.split(suffix, ":")[0]);
					}
				}
			}

			return null;
		}
		
		@Override
		public String call(QAContext ctx, String name, String val) {
			return call(name, val);
		}
	}

	public static String processByTagName(String tpl, final String tagName, final Replacer r){
		return process(tpl, new Replacer() {
			@Override
			public String call(String name, String val) {
				String replaced = null;
				if(name.equals(tagName)){
					replaced = r.call(name, val);
				}
				return replaced;
			}
			
			@Override
			public String call(QAContext ctx, String name, String val) {
				String replaced = null;
				if(name.equals(tagName)){
					replaced = r.call(ctx, name, val);
				}
				return replaced;
			}
		});
	}

	public static String processByTagName(QAContext ctx, String tpl, final String tagName, final Replacer r){
		return process(ctx, tpl, new Replacer() {
			@Override
			public String call(String name, String val) {
				String replaced = null;
				if(name.equals(tagName)){
					replaced = r.call(name, val);
				}
				return replaced;
			}

			@Override
			public String call(QAContext ctx, String name, String val) {
				String replaced = null;
				if(name.equals(tagName)){
					replaced = r.call(ctx, name, val);
				}
				return replaced;
			}
		});
	}

	public static String process(String tpl, Replacer r){
		String out = null;
		if(tpl != null){
			StringBuffer sb = new StringBuffer();
			Matcher matcher = templatePattern.matcher(tpl);
			while (matcher.find()) {
				String name = StringUtils.trim(matcher.group(1));
				String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));

				String replaced = r.call(name, val);
				if(replaced != null){
					replaced = StringUtils.replace(replaced, "$", "\\$");
					matcher.appendReplacement(sb, replaced);
				}

			}
			matcher.appendTail(sb);
			out = sb.toString();
		}
		return out;
	}

	public static String process(QAContext ctx, String tpl, Replacer r){
		String out = null;	
		
		if(tpl != null){
			tpl = tpl.replace("{{Q:","TemporaryReplacementQ");
			tpl = tpl.replace("{{T:","TemporaryReplacementT");

			StringBuffer sb = new StringBuffer();
			Matcher matcher = templatePattern.matcher(tpl);
			while (matcher.find()) {
				String name = StringUtils.trim(matcher.group(1));
				String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));
				if(((StringUtils.equals(name, "ROBOT_NAME") || StringUtils.equals(val, "ROBOT_NAME")) && ctx.getRequestAttribute("replaceName") != null && ctx.getRequestAttribute("replaceName").equals("false"))){
					continue;
				}
				String replaced = r.call(ctx, name, val);
				if(replaced != null){
					replaced = StringUtils.replace(replaced, "$", "\\$");
					matcher.appendReplacement(sb, replaced);
				}

			}
			matcher.appendTail(sb);
			out = sb.toString();
			
			out = out.replace("TemporaryReplacementQ","{{Q:");
			out = out.replace("TemporaryReplacementT","{{T:");	
	
		}
		return out;
	}

	// 為了制式文案內可能還有制式文案（通常是機器人名稱），因此套兩次
	public static String processTwice(QAContext ctx, String tpl, Replacer r) {
		String out = process(ctx, tpl, r);
		out = process(ctx, out, r);
		return out;
	}

	// 為了制式文案內可能還有制式文案（通常是機器人名稱），因此套兩次，並且最後把所有 {{XXX}} 都清乾淨
	public static String processTwiceAndClean(QAContext ctx, String tpl, Replacer r) {
		String out = process(ctx, tpl, r);
		out = process(ctx, out, r);
		out = process(ctx, out, QAUtil.getCleaner());
		return out;
	}
	
	public static String find(String tpl, Replacer r){
		if(tpl != null){
			Matcher matcher = templatePattern.matcher(tpl);
			while (matcher.find()) {
				String name = StringUtils.trim(matcher.group(1));
				String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));

				String replaced = r.call(name, val);
				if(replaced != null){
					replaced = StringUtils.replace(replaced, "$", "\\$");
					return replaced;
				}
			}
		}
		return null;
	}

	public static ArrayList<Map<String, Object>> collectByTagName(String tpl, final String tagName, final Collector c){
		return collect(tpl, new Collector() {
			@Override
			public Map<String, Object> call(String name, String val) {
				Map<String, Object> collected = null;
				if(name.equals(tagName)){
					collected = c.call(name, val);
				}
				return collected;
			}
		});
	}

	public static ArrayList<Map<String, Object>> collect(String tpl, Collector c){
		ArrayList<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
		if(tpl != null){
			Matcher matcher = templatePattern.matcher(tpl);
			while (matcher.find()) {
				String name = StringUtils.trim(matcher.group(1));
				String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));

				Map<String, Object> collected = c.call(name, val);
				if(collected != null){
					//System.out.println( "TemplateUtil.collect : " + new JSONObject(collected).toString() );
					out.add(collected);
				}

			}
		}
		if (out.size()<1)
			out = null;
		return out;
	}
	
	public static int getCollectLength(String tpl){
		int totalLength = 0;
		if(tpl != null){
			Matcher matcher = templatePattern.matcher(tpl);
			while (matcher.find()) {
				String name = StringUtils.trim(matcher.group(1));
				String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));
                totalLength += name.length() +  "{{:}}".length();
				totalLength += val.length();
			}
		}
		
		return totalLength;
	}
	
	public static boolean matchWholeByTagName(String tpl, String tagName){
		Matcher matcher = wholeNamePattern.matcher(StringUtils.trim(StringUtils.defaultString(tpl)));
		int count = 0;
		boolean match = false;
		while (matcher.find()) {
			String name = StringUtils.trim(matcher.group(1));
			if(count == 0 && name.equals(tagName)){
				match = true;
			}
			count++;
		}
		return count == 1 && match;
	}

}
