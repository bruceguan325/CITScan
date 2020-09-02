package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.Calendar;

import com.intumit.solr.robot.qadialog.SortRule.SortOrder;

public class SortRuleCheckResult extends RuleCheckResult implements Serializable {
	public static final SortRuleCheckResult NO_MATCH = new SortRuleCheckResult(RuleCheckResultStatus.NOT_MATCH, null, null, null, null);
	
	Integer rows;

	private SortOrder order;
	
	public SortRuleCheckResult(RuleCheckResultStatus status, String value, Rule rule, SortOrder order, Integer rows) {
		super(status, value, rule);
		this.order = order;
		this.rows = rows;
	}

	public Integer getRows() {
		return rows;
	}

	public void setRows(Integer rows) {
		this.rows = rows;
	}

	public SortOrder getOrder() {
		return order;
	}

	public void setOrder(SortOrder order) {
		this.order = order;
	}

	@Override
	public String toString() {
		return "SortRuleCheckResult [rows=" + rows + ", order=" + order + ", status=" + status
				+ ", value=" + value + ", rule=" + rule + "]";
	}

}
