package com.intumit.solr.robot.qadialog;

import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;

public class LastReqAttrTrigger extends Trigger {
	private String attrName;
	private String triggerValue;

	public LastReqAttrTrigger() {
		super();
	}

	public LastReqAttrTrigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super(doubleConfirm, doubleConfirmText, contents, showCurrentStatus);
		
		attrName = contents.get(0);
		triggerValue = contents.get(1);
	}

	@Override
	public boolean isTrigger(QAContext ctx) {
		String val = (String)ctx.getLastRequestAttribute(attrName);
		
		if (val != null && StringUtils.equalsIgnoreCase(val, triggerValue))
			return true;
		
		return false;
	}
}
