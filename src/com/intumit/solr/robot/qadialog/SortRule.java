package com.intumit.solr.robot.qadialog;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.util.WiSeUtils;

public class SortRule extends Rule implements SearchableRule {
	public static enum SortOrder {
		ASC, DESC;
	}

	public SortRule() {
	}
	
	public SortRule(JSONObject cfg) {
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
		return "SortRule [testCases=" + testCases + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
		List<String> questions = null;
		if (isReconstructQuestion()) {
			questions = (List<String>)ctx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
		}
		else {
			questions = Arrays.asList(new String[] {ctx.getCurrentQuestion()});
		}
		
		for (String question: questions) {
			for (Pattern p: sortDescAndSetRows) {
				Matcher m = p.matcher(question);
				
				if (m.find()) {
					String value = m.group();
					String rowsStr = m.group("rows");
					
					Integer rows = rowsStr == null ? 1 : new Integer(rowsStr);
						
					return new SortRuleCheckResult(RuleCheckResultStatus.MATCH, value, this, SortOrder.DESC, rows);
				}
			}
			for (Pattern p: sortAscAndSetRows) {
				Matcher m = p.matcher(question);
				
				if (m.find()) {
					String value = m.group();
					String rowsStr = m.group("rows");
					
					Integer rows = rowsStr == null ? 1 : new Integer(rowsStr);
						
					return new SortRuleCheckResult(RuleCheckResultStatus.MATCH, value, this, SortOrder.ASC, rows);
				}
			}
		}
		
		return RuleCheckResult.NO_MATCH;
	}
	
	static Pattern[] sortDescAndSetRows = 
			new Pattern[] {
				Pattern.compile("(?si)(上|最近|最後|最新)(?<rows>\\d+)?(次|遍|回)"),
			};
	
	static Pattern[] sortAscAndSetRows = 
			new Pattern[] {
				Pattern.compile("(?si)(最遠|最早|最舊)(?<rows>\\d+)?(次|遍|回)"),
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
			if ("sort".equals(searchType)) {
				SortRuleCheckResult srr = (SortRuleCheckResult)result;
				if (srr.getOrder() == SortOrder.ASC) {
					query.addSort(targetField, SolrQuery.ORDER.asc);
				}
				else {
					query.addSort(targetField, SolrQuery.ORDER.desc);
				}
				
				if (srr.getRows() != null) {
					query.setRows(srr.getRows());
				}
				
				return true;
			}
		}
		
		return has;
	}

	@Override
	public boolean applySearchCriteria(RuleCheckResult result, SolrDocumentList docs) {
		return new SearchableHelper().applySearchCriteria(result, docs, searchType, queryType, targetField, partial);
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
