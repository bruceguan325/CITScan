package com.intumit.solr.robot.qadialog;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qadialog.CallDialogRuleCheckResult.RewriteType;

public class HouseAgeRule extends Rule {

	public HouseAgeRule() {
	}
	
	public HouseAgeRule(JSONObject cfg) {
		super(cfg);
	}
	
	@Override
	public void loadConfig(JSONObject cfg) {
	}

	@Override
	public void init() {
	}

	@Override
	public String toString() {
		return "HouseAgeRule [testCases=" + testCases + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
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
			for (Pattern p: p1) {
				Matcher m = p.matcher(question);
				
				if (m.find()) {
	
					String t = m.group("unit");
					int age = Integer.parseInt(m.group("age"));
					float unit = 1;
					
					if ("年".equals(t) || "旬".equals(t)) {
						unit = 1;
					}
					else if ("季".equals(t)) {
						unit = 1f/12;
					}
					else if ("個月".equals(t) || "月".equals(t)) {
						unit = 1f/12;
					}
					else if ("個禮拜".equals(t) || "禮拜".equals(t) || "週".equals(t)) {
						unit = 1f/51;
					}
					else if ("天".equals(t)) {
						unit = 1f/365;
					}
					
					age = (int)Math.floor(age*unit);
					return new RuleCheckResult(RuleCheckResultStatus.MATCH, "屋齡" + age + "年", this);
				}
			}
			for (Pattern p: p2) {
				Matcher m = p.matcher(question);
				
				if (m.find()) {
					String type = m.group("type");
					String year = m.group("year");
					
					int acNow = Calendar.getInstance().get(Calendar.YEAR);
					int iYear = Integer.parseInt(year);
					
					if ("民國".equals(type)) {
						iYear += 1911;
					}
					if (type == null) {
						if (iYear <= (acNow-1911)) {
							iYear += 1911;
						}
					}
					
					if (iYear > acNow) {
						iYear = acNow;
					}
					
					return new RuleCheckResult(RuleCheckResultStatus.MATCH, "屋齡" + (acNow - iYear) + "年", this);
				}
			}
			for (Pattern p: p3) {
				Matcher m = p.matcher(question);
				
				if (m.find()) {
					String t = m.group();
					int age = -1;
					
					if ("今年".equals(t)) {
						age = 0;
					}
					else if ("去年".equals(t)) {
						age = 1;
					}
					else if ("前年".equals(t)) {
						age = 2;
					}
					else if ("大前年".equals(t)) {
						age = 3;
					}
					
					if (age >= 0) {
						return new RuleCheckResult(RuleCheckResultStatus.MATCH, "屋齡" + age + "年", this);
					}
				}
			}
			for (Pattern p: ambiguity) {
				Matcher m = p.matcher(ctx.getCurrentQuestion());
	
				if (m.matches()) {
					String year = m.group("year");
					
					return new CallDialogRuleCheckResult(RuleCheckResultStatus.CALL_DIALOG, year, this, "YearDisambiguation", RewriteType.OVERWRITE, year);
				}
			}
		}
		return RuleCheckResult.NO_MATCH;
	}
	
	static Pattern[] ambiguity = 
			new Pattern[] {
				Pattern.compile("(?si)(?<year>\\d+)年"),
				Pattern.compile("(?si)(?<year>\\d+)"),
			};
	
	
	static Pattern[] p1 = 
			new Pattern[] {
				Pattern.compile("(?si)(?<age>\\d+)(?<unit>年|個月|週|天|季|旬|周|月|禮拜|個禮拜)(以前|之前|前)"),
				Pattern.compile("(?si)(?<age>\\d+)(?<unit>年|個月|週|天|季|旬|周|月|禮拜|個禮拜)(以前|之前|前)"),
				Pattern.compile("(?si)(屋齡|蓋好)(不到|大約)?(?<age>\\d+)(?<unit>年|個月|週|天|季|旬|周|月|禮拜|個禮拜)"),
				Pattern.compile("(?si)(不到|大約)?(?<age>\\d+)(?<unit>個月|週|天|季|旬|周|禮拜|個禮拜)"),
			};
	
	static Pattern[] p2 = 
			new Pattern[] {
				Pattern.compile("(?si)(?<type>民國|西元)(?<year>\\d+)年"),
				Pattern.compile("(?si)(?<year>\\d+)(?<unit>年)\\d+月"),
			};
	
	static Pattern[] p3 = 
			new Pattern[] {
				Pattern.compile("(?si)(今年|去年|前年|大前年)"),
			};
		
}
