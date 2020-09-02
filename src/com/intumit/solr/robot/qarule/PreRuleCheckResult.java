package com.intumit.solr.robot.qarule;

import java.io.Serializable;

public class PreRuleCheckResult implements Serializable {
	public static final PreRuleCheckResult DEFAULT_NORMAL_RESULT = new PreRuleCheckResult(Status.NORMAL);
	public static final PreRuleCheckResult DEFAULT_SKIP_AND_CONTINUE_RESULT = new PreRuleCheckResult(Status.SKIP_AND_CONTINUE);

	public static enum Status {
		NORMAL,
		SKIP_AND_CONTINUE,
	}
	
	Status status;

	public PreRuleCheckResult(Status status) {
		super();
		this.status = status;
	}
	
	public Status getStatus() {
		return status;
	}

}
