package com.intumit.solr.robot.qadialog;

import java.io.Serializable;

public class RuleCheckResult implements Serializable {
	public static final RuleCheckResult NO_MATCH = new RuleCheckResult(RuleCheckResultStatus.NOT_MATCH, null, null);
	RuleCheckResultStatus status;
	String value;
	String valueForShow;
	Rule rule;
	
	public RuleCheckResult(RuleCheckResultStatus status, String value, Rule rule) {
		super();
		this.status = status;
		this.value = value;
		this.valueForShow = value;
		this.rule = rule;
	}
	
	public RuleCheckResult(RuleCheckResultStatus status, String value, String valueForShow, Rule rule) {
		super();
		this.status = status;
		this.value = value;
		this.valueForShow = valueForShow;
		this.rule = rule;
	}
	
	public boolean isMatch() {
		return status == RuleCheckResultStatus.MATCH;
	}
	public RuleCheckResultStatus getStatus() {
		return status;
	}

	public void setStatus(RuleCheckResultStatus status) {
		this.status = status;
	}

	public String getValue() {
		return value;
	}
	public String getValueForShow() {
		return valueForShow != null ? valueForShow : value;
	}

	public Rule getRule() {
		return rule;
	}
	public void setRule(Rule rule) {
		this.rule = rule;
	}

	@Override
	public String toString() {
		return "RuleCheckResult [status=" + status + ", value=" + value
				+ ", rule=" + rule + "]";
	}
	
}
