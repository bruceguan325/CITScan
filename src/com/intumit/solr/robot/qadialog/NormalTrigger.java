package com.intumit.solr.robot.qadialog;

import java.util.List;
import java.util.Map;

import com.intumit.solr.robot.QAContext;

public class NormalTrigger extends Trigger {
	List<String> endpoint = null;
	Map<String, String> action;

	public NormalTrigger() {
	}
	
	public NormalTrigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super(doubleConfirm, doubleConfirmText, contents, showCurrentStatus);
	}
	
	public List<String> getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(List<String> endpoint) {
		this.endpoint = endpoint;
	}
	
	public boolean matchEndpoint(String ep) {
		if (endpoint != null && endpoint.contains(ep))
			return true;
		
		return false;
	}

	public Map<String, String> getAction() {
		return action;
	}

	public void setAction(Map<String, String> action) {
		this.action = action;
	}

	@Override
	public boolean isTrigger(QAContext ctx) {
		return false;
	}
}
