package com.intumit.solr.robot.qadialog;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.util.WiSeUtils;

public class SearchableRegexRule extends RegexRule implements SearchableRule {
	String searchType;
	String queryType;
	String targetField;
	boolean partial;

	@Override
	public boolean applySearchCriteria(RuleCheckResult result, SolrQuery query) {
		return new SearchableHelper().applySearchCriteria(result, query, searchType, queryType, targetField, partial);
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
