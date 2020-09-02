package com.intumit.solr.robot.qarule;

import java.io.Serializable;
import java.util.Map;

import com.intumit.solr.robot.qarule.PreRuleCheckResult.Status;

public class PostRuleCheckResult implements Serializable {
	public static final PostRuleCheckResult DEFAULT_CONTINUE_RESULT = new PostRuleCheckResult(Status.CONTINUE);
	public static final PostRuleCheckResult DEFAULT_RETURN_RESULT = new PostRuleCheckResult(Status.RETURN);
	
	public static enum Status {
		FORWARD,
		CONTINUE,
		RETURN,
	}
	
	Status status;
	String[] forwardTo;
	Map<String, Object> bundle;
	
	/**
	 * 
	 * @param status
	 * @param forwardTo 改成 String[]，可以指定一次要 forward 到兩個 chain（依序）
	 * @param bundle
	 */
	public PostRuleCheckResult(Status status, String[] forwardTo, Map<String, Object> bundle) {
		super();
		this.status = status;
		this.forwardTo = forwardTo;
		this.bundle = bundle;
	}
	
	/*
	 * forwardTo 改成 String[]，可以指定一次要 forward 到兩個 chain（依序）
	 */
	public PostRuleCheckResult(Status status, String... forwardTo) {
		super();
		this.status = status;
		this.forwardTo = forwardTo;
		this.bundle = null;
	}

	
	public PostRuleCheckResult(Status status) {
		super();
		this.status = status;
		this.forwardTo = null;
		this.bundle = null;
	}

	public PostRuleCheckResult() {
	}

	public Status getStatus() {
		return status;
	}

	public String[] getForwardTo() {
		return forwardTo;
	}

	public Map<String, Object> getBundle() {
		return bundle;
	}
	
}
