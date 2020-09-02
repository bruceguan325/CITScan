package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;

public class ChainRuleCheckResult extends RuleCheckResult implements Serializable {
	public static final ChainRuleCheckResult NO_MATCH = new ChainRuleCheckResult(RuleCheckResultStatus.NOT_MATCH, null, null, null);
	
	List<RuleCheckResult> results;
	
	public ChainRuleCheckResult(RuleCheckResultStatus status, String value, Rule rule, List<RuleCheckResult> results) {
		super(status, value, rule);
		this.results = results;
	}
	
	public List<RuleCheckResult> getResults() {
		return results;
	}

	public void setResults(List<RuleCheckResult> results) {
		this.results = results;
	}

	@Override
	public String toString() {
		return "ChainRuleCheckResult [results=" + results + ", status="
				+ status + ", value=" + value + ", rule=" + rule + "]";
	}
	
}
