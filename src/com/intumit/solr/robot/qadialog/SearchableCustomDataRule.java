package com.intumit.solr.robot.qadialog;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.robot.dictionary.CustomDataDictionary;

public class SearchableCustomDataRule extends Rule implements SearchableRule {

	String dataType = null;
	String restrictToField = null;

	public SearchableCustomDataRule() {
	}
	
	public SearchableCustomDataRule(JSONObject cfg) {
		super(cfg);
	}
	
	@Override
	public void loadConfig(JSONObject cfg) {
		try {
			if (cfg.has("dataType")) {
				dataType = cfg.getString("dataType");
			}
			if (cfg.has("restrictToField")) {
				restrictToField = cfg.getString("restrictToField");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void init() {
	}

	public String getDataType() {
		return dataType;
	}

	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

	public String getRestrictToField() {
		return restrictToField;
	}

	public void setRestrictToField(String restrictToField) {
		this.restrictToField = restrictToField;
	}

	@Override
	public String toString() {
		return "SearchableCustomDataRule [dataType=" + dataType + "]";
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
			Set<CustomData>[] result = CustomDataDictionary.search(ctx.getTenant().getId(), question.toCharArray(), dataType, restrictToField);
			if (result != null && result.length > 0) {
				return new RuleCheckResult(RuleCheckResultStatus.MATCH, result[0].iterator().next().getValue(), this);
			}
		}
		return RuleCheckResult.NO_MATCH;
	}
	
	
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
