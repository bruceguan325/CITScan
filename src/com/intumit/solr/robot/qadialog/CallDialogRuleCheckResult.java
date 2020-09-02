package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.Calendar;

public class CallDialogRuleCheckResult extends RuleCheckResult implements Serializable {
	public static enum RewriteType { OVERWRITE, APPEND, PREPEND, DONOTHING}
	
	String targetDialogName;
	RewriteType rewriteType;
	Object bundle;
	
	public CallDialogRuleCheckResult(RuleCheckResultStatus status, String value, Rule rule, String targetDialogName, RewriteType rewriteType, Object bundle) {
		super(status, value, rule);
		this.targetDialogName = targetDialogName;
		this.rewriteType = rewriteType;
		this.bundle = bundle;
	}

	public String getTargetDialogName() {
		return targetDialogName;
	}

	public void setTargetDialogName(String targetDialogName) {
		this.targetDialogName = targetDialogName;
	}

	public RewriteType getRewriteType() {
		return rewriteType;
	}

	public void setRewriteType(RewriteType rewriteType) {
		this.rewriteType = rewriteType;
	}

	public Object getBundle() {
		return bundle;
	}

	public void setBundle(Object bundle) {
		this.bundle = bundle;
	}

	@Override
	public String toString() {
		return "CallDialogRuleCheckResult [targetDialogName="
				+ targetDialogName + ", bundle=" + bundle + "]";
	}

}
