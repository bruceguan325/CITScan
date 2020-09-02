package com.intumit.solr.robot.qadialog;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.util.WiSeUtils;

public class DateRangeRule extends Rule implements SearchableRule {

	public DateRangeRule() {
	}
	
	public DateRangeRule(JSONObject cfg) {
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
		return "TimeRangeRule [testCases=" + testCases + "]";
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
				Calendar from = Calendar.getInstance();
				Calendar to = Calendar.getInstance();
				Matcher m = p.matcher(question);
				
				if (m.find()) {
					String value = m.group();
					String t = m.group("unit");
					String amountStr = m.group("amount");
					int amount = -1;
					
					if (StringUtils.isNumeric(amountStr)) {
						amount = Integer.parseInt(amountStr);
					}
					else {
						int kw = StringUtils.countMatches(amountStr, "前");
						
						if (kw > 0) {
							amount = kw + 1;
						}
						
						int kw2 = StringUtils.countMatches(amountStr, "上");
						if (kw2 > 0) {
							amount = kw2;
						}
						
						if (StringUtils.containsAny(amountStr, "今本這")) {
							amount = 0;
						}
					}
					int unit = Calendar.MONTH;
					int gap = 1;
					
					if ("年".equals(t) || "旬".equals(t)) {
						unit = Calendar.YEAR;
						
						if (amount == 0) {
							gap = 0;
							unit = Calendar.MONTH;
							from.set(Calendar.DATE, 1);
							to.setTime(from.getTime());
							to.add(Calendar.YEAR, 1);
						}
					}
					else if ("季".equals(t)) {
						unit = Calendar.MONTH;
						gap = 3;
					}
					else if ("個月".equals(t) || "月".equals(t)) {
						unit = Calendar.MONTH;
						gap = 1;
						
						if (amount == 0) {
							gap = 0;
							unit = Calendar.DAY_OF_MONTH;
							amount = 1; // day of month 從 1 開始
						}
					}
					else if ("個禮拜".equals(t) || "禮拜".equals(t) || "週".equals(t)) {
						unit = Calendar.WEEK_OF_YEAR;
						gap = 1;
						
						if (amount == 0) {
							gap = 0;
							unit = Calendar.DAY_OF_WEEK;
						}
					}
					else if ("天".equals(t)) {
						unit = Calendar.DAY_OF_YEAR;
						gap = 1;
						
						if (amount == 0) {
							gap = 0;
							unit = Calendar.HOUR_OF_DAY;
						}
					}
					
					
					if (gap == 0)
						from.set(unit, amount);
					else 
						from.add(unit, -1 * amount * gap);
						
					return new DateRangeRuleCheckResult(RuleCheckResultStatus.MATCH, value, this, from, to);
				}
			}
			
			for (Pattern p: p3) {
				Calendar from = Calendar.getInstance();
				Calendar to = Calendar.getInstance();
				Matcher m = p.matcher(question);
				
				if (m.find()) {
					String t = m.group();
					int amount = -1;
					int unit = Calendar.MONTH;
					int gap = 1;
					
					if ("去年底".equals(t)) {
						from.add(Calendar.YEAR, -1);
						from.set(Calendar.MONTH, 11); // 十二月
						from.set(Calendar.DAY_OF_MONTH, 1);
					}
					else if ("去年".equals(t)) {
						from.add(Calendar.YEAR, -1);
						from.set(Calendar.MONTH, 0); // 一月
						from.set(Calendar.DAY_OF_MONTH, 1);
					}
					else if ("昨天".equals(t)) {
						from.add(Calendar.DAY_OF_MONTH, -1);
					}
					
					return new DateRangeRuleCheckResult(RuleCheckResultStatus.MATCH, t, this, from, to);
				}
			}
		}
		return RuleCheckResult.NO_MATCH;
	}
	
	static Pattern[] p1 = 
			new Pattern[] {
				Pattern.compile("(?si)(近)?(?<amount>\\d+|上+個?|(大|前)?前一?|本|今|這個?)(?<unit>年|個月|週|天|季|旬|周|月|禮拜|個禮拜)"),
				Pattern.compile("(?si)(最近|大約)?(?<amount>\\d+)(?<unit>年|個月|週|天|季|旬|周|月|禮拜|個禮拜)"),
			};
	
/*	static Pattern[] p2 = 
			new Pattern[] {
				Pattern.compile("(?si)(?<type>民國|西元)?(?<year>\\d+)"),
				Pattern.compile("(?si)(?<year>\\d+)(?<unit>年)\\d+月"),
			};*/
	
	static Pattern[] p3 = 
			new Pattern[] {
				Pattern.compile("(?si)(去年底|去年|昨天)"),
			};
		
	String searchType;
	String queryType;
	String targetField;
	boolean partial;

	@Override
	public boolean applySearchCriteria(RuleCheckResult result, SolrQuery query) {
		boolean has = new SearchableHelper().applySearchCriteria(result, query, searchType, queryType, targetField, partial);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		
		if (!has) {
			if ("filterQuery".equals(searchType)) {
				if ("dateRange".equals(queryType) && result instanceof DateRangeRuleCheckResult) {
					DateRangeRuleCheckResult drr = (DateRangeRuleCheckResult)result;
					query.addFilterQuery(targetField 
							+ ":[" + WiSeUtils.toSolrDateStr(drr.getFrom()) 
							+ " TO " + WiSeUtils.toSolrDateStr(drr.getTo()) + "]");
					
					return true;
				}
			}
		}
		
		return has;
	}
	
	@Override
	public boolean applySearchCriteria(RuleCheckResult result, SolrDocumentList docs) {
		boolean has = new SearchableHelper().applySearchCriteria(result, docs, searchType, queryType, targetField, partial);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
		
		if (!has) {
			if ("filterQuery".equals(searchType)) {
				if ("dateRange".equals(queryType) && result instanceof DateRangeRuleCheckResult) {
					DateRangeRuleCheckResult drr = (DateRangeRuleCheckResult)result;
					Iterator<SolrDocument> docItr = docs.iterator();
					boolean dirty = false;
					
					while (docItr.hasNext()) {
						SolrDocument doc = docItr.next();
						
						Object dateObj = doc.getFieldValue(targetField);
						
						if (dateObj != null) {
							Date targetDate = null;
						
							if (dateObj instanceof Date) {
								targetDate = (Date)dateObj;
							}
							else if (dateObj instanceof Long) {
								targetDate = Calendar.getInstance().getTime();
								targetDate.setTime((Long)dateObj);
							}
						
							if (targetDate != null) {
								Calendar targetCal = Calendar.getInstance();
								targetCal.setTime(targetDate);
								
								if (targetCal.before(drr.getFrom()) || targetCal.after(drr.getTo())) {
									docItr.remove();
								}
							}
							else {
								docItr.remove();
							}
						}
						else {
							docItr.remove();
						}
					}
					
					return dirty;
				}
			}
		}
		
		return has;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getQueryType() {
		return queryType;
	}

	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	public String getTargetField() {
		return targetField;
	}

	public void setTargetField(String targetField) {
		this.targetField = targetField;
	}

	public boolean isPartial() {
		return partial;
	}

	public void setPartial(boolean partial) {
		this.partial = partial;
	}
}
