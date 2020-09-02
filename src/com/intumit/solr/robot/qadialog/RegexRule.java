package com.intumit.solr.robot.qadialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;

public class RegexRule extends Rule {

	List<Pattern> p = null;
	List<String> match;
	String rewrite = null;
	String rewriteExpr = null;
	Map<String, Object> rewriteMapping = null;
	Boolean partial = null;

	public RegexRule() {
	}
	
	public RegexRule(JSONObject cfg) {
		super(cfg);
	}
	
	@Override
	public void loadConfig(JSONObject cfg) {
		try {
			if (cfg.has("match")) {
				match = cfg.getJSONArray("match");
			}
			if (cfg.has("rewrite")) {
				rewrite = cfg.getString("rewrite");
			}
			if (cfg.has("rewriteExpr")) {
				rewriteExpr = cfg.getString("rewriteExpr");
			}
			if (cfg.has("partial")) {
				partial = cfg.getBoolean("partial");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
		if (match != null) {
			p = new ArrayList<>();
			
			for (String m: match) {
				p.add(Pattern.compile(m, Pattern.DOTALL | Pattern.CASE_INSENSITIVE));
			}
		}
	}

	public List<String> getMatch() {
		return match;
	}

	public void setMatch(List<String> match) {
		this.match = match;
	}

	public Boolean getPartial() {
		return partial != null ? partial : Boolean.FALSE;
	}

	public void setPartial(Boolean partial) {
		this.partial = partial;
	}

	public String getRewrite() {
		return rewrite;
	}

	public void setRewrite(String rewrite) {
		this.rewrite = rewrite;
	}

	public Map<String, Object> getRewriteMapping() {
		return rewriteMapping;
	}

	public void setRewriteMapping(Map<String, Object> rewriteMapping) {
		this.rewriteMapping = rewriteMapping;
	}

	public String getRewriteExpr() {
		return rewriteExpr;
	}

	public void setRewriteExpr(String rewriteExpr) {
		this.rewriteExpr = rewriteExpr;
	}

	@Override
	public String toString() {
		return "RegexRule [p=" + p + ", match=" + match + ", rewrite="
				+ rewrite + ", rewriteMapping=" + rewriteMapping + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
		if (p != null) {
			for (Pattern pt: p) {
				List<String> questions = null;
				if (isReconstructQuestion()) {
					questions = (List<String>)ctx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
					if (!questions.contains(ctx.getCurrentQuestion())) {
						questions.add(0, ctx.getCurrentQuestion());
					}
				}
				else {
					questions = Arrays.asList(new String[] {ctx.getCurrentQuestion()});
				}
				
				for (String question: questions) {
					Matcher m = pt.matcher(question);
					
					if (getPartial() ? m.find() : m.matches()) {
						String rewrited = replace(m, rewrite);
						String valueForShow = null;
						
						if (rewriteMapping != null) {
							if (rewriteMapping.containsKey(rewrited)) {
								rewrited = rewriteMapping.get(rewrited).toString();
								
								if (rewrited.indexOf("(?<show>") != -1) {
									valueForShow = StringUtils.substringAfter(rewrited, "(?<show>");
									valueForShow = StringUtils.substringBefore(valueForShow, ")?");
								}
								else {
									valueForShow = rewrited;
								}
							}
						}
						
						return new RuleCheckResult(RuleCheckResultStatus.MATCH, rewrited, valueForShow, this);
					}
				}
			}
		}
		return RuleCheckResult.NO_MATCH;
	}
	
	static Pattern backrefPattern = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)\\}");
	/**
	 * 本來是想用 String.replaceAll 處理，但遇到未知問題
	 * 
	 * String patternStr = "(?is).*?(?<payInterval>年繳|半年繳|季繳|月繳).*?";
	 * Pattern p = Pattern.compile(patternStr);
	 * Matcher m = p.matcher("有哪些是年繳的?");
	 * m.matches();
	 * String part = m.group();
	 * part.replaceAll(patternStr, "${payInterval}");
	 * 
	 * 上述程式本來預期應該結果是「年繳」，但偏偏會是「年繳的」，所以改自己寫
	 * 
	 * @param m
	 * @param rewrite
	 * @return
	 */
	public static String replace(Matcher m, String rewrite) {
		Matcher bm = backrefPattern.matcher(rewrite);
		
		while (bm.find()) {
			String allGrp = bm.group();
			String name = bm.group(1);
			
			String val = StringUtils.trimToEmpty(m.group(name));
			rewrite = rewrite.replace(allGrp, val);
			
			bm = backrefPattern.matcher(rewrite);
		}
		
		return rewrite;
	}
	
	public static void main(String[] args) {
		Pattern p = Pattern.compile("(?is).*?(?<payInterval>年繳|半年繳|季繳|月繳).*?");
		Matcher m = p.matcher("有哪些是年繳的?");
		
		m.matches();
		String part = m.group();
		String rewrited = replace(m, "${payInterval}");//part.replaceAll("(?is).*?(?<payInterval>年繳|半年繳|季繳|月繳).*?", "${payInterval}");
		
		System.out.println(part + "//" + rewrited + "//" + m.group(1) + "//" + m.group(0));
	}
}
