package com.intumit.solr.robot.qadialog;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.intumit.solr.robot.QAContext;

public class RegexTrigger extends NormalTrigger {
	List<Pattern> patterns = new ArrayList<Pattern>();
	boolean partial = false;

	public RegexTrigger() {
		super();
	}

	public RegexTrigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super(doubleConfirm, doubleConfirmText, contents, showCurrentStatus);
		
		for (String regex: contents) {
			patterns.add(Pattern.compile(regex));
		}
	}

	public boolean isPartial() {
		return partial;
	}

	public void setPartial(boolean partial) {
		this.partial = partial;
	}

	void loadPatterns() {
		for (String regex: contents) {
			patterns.add(Pattern.compile(regex));
		}
	}


	@Override
	public boolean isTrigger(QAContext ctx) {
		if (patterns == null || patterns.size() == 0)
			loadPatterns();
		
		for (Pattern p: patterns) {
			if (partial) {
				if (p.matcher(ctx.getCurrentQuestion()).find())
					return true;
			}
			else {
				if (p.matcher(ctx.getCurrentQuestion()).matches())
					return true;
			}
		}
		
		return false;
	}
}
