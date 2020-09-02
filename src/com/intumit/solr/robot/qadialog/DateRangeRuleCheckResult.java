package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.Calendar;

public class DateRangeRuleCheckResult extends RuleCheckResult implements Serializable {
	public static final DateRangeRuleCheckResult NO_MATCH = new DateRangeRuleCheckResult(RuleCheckResultStatus.NOT_MATCH, null, null, null, null);
	
	Calendar from;
	Calendar to;
	
	public DateRangeRuleCheckResult(RuleCheckResultStatus status, String value, Rule rule, Calendar from, Calendar to) {
		super(status, value, rule);
		this.from = from;
		this.to = to;
	}
	public Calendar getFrom() {
		return from;
	}
	public void setFrom(Calendar from) {
		this.from = from;
	}
	public Calendar getTo() {
		return to;
	}
	public void setTo(Calendar to) {
		this.to = to;
	}
	@Override
	public String toString() {
		return "DateRangeRuleCheckResult [from=" + from + ", to=" + to
				+ ", status=" + status + ", value=" + value + ", rule=" + rule
				+ "]";
	}
}
