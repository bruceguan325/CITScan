package com.intumit.solr.robot.qadialog;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;

public class ReqAttrTrigger extends Trigger {
	private String attrName = null;
	private String triggerValue = null;

	public ReqAttrTrigger() {
		super();
	}

	public ReqAttrTrigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super(doubleConfirm, doubleConfirmText, contents, showCurrentStatus);
		init();
	}
	
	void init() {
		attrName = contents.get(0);
		triggerValue = contents.get(1);
	}

	@Override
	public boolean isTrigger(QAContext ctx) {
		if (attrName == null) init();
		String val = (String)ctx.getRequestAttribute(attrName);
		
		if (val != null && StringUtils.equalsIgnoreCase(val, triggerValue))
			return true;
		
		return false;
	}
}
