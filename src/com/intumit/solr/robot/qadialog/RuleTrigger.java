package com.intumit.solr.robot.qadialog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.intumit.solr.robot.QAContext;

public class RuleTrigger extends NormalTrigger {
	List<Rule> rules = new ArrayList<Rule>();

	public RuleTrigger() {
		super();
	}

	public RuleTrigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super(doubleConfirm, doubleConfirmText, contents, showCurrentStatus);
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	@Override
	public boolean isTrigger(QAContext ctx) {
		for (Rule r: rules) {
			RuleCheckResult res = r.check(ctx);
			if (res != null && res.isMatch())
				return true;
		}
		
		return false;
	}
}
