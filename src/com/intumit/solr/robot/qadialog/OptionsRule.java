package com.intumit.solr.robot.qadialog;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;

public class OptionsRule extends Rule {

	List<String> options = null;
	Map<String, String> map = null;

	public OptionsRule() {
	}
	
	public OptionsRule(JSONObject cfg) {
		super(cfg);
	}
	
	@Override
	public void loadConfig(JSONObject cfg) {
		// implement parse options
	}

	@Override
	public void init() {
		map = new HashMap<String, String>();
		for (int i=0; i < options.size(); i+=2) {
			String k = options.get(i);
			String v = options.size() > i+1 ? options.get(i+1) : null;
			
			if (v != null) {
				map.put(k, v);
			}
		}
	}

	public List<String> getOptions() {
		return options;
	}

	public void setOptions(List<String> options) {
		this.options = options;
	}

	@Override
	public String toString() {
		return "OptionsRule [options=" + options + ", map=" + map + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
		List<String> questions = null;
		if (isReconstructQuestion()) {
			questions = (List<String>)ctx.getRequestAttribute(QAContext.REQ_ATTR_RECONSTRUCTED_QUESTION);
			if (!questions.contains(ctx.getCurrentQuestion())) {
				questions.add(0, ctx.getCurrentQuestion());
			}
		}
		else {
			questions = Arrays.asList(new String[] {ctx.getCurrentQuestion()});
		}
		
		for (String question: questions) {
			if (map.containsKey(question)) {
				return new RuleCheckResult(RuleCheckResultStatus.MATCH, 
						QADialog.replaceVariables(ctx, map.get(ctx.getCurrentQuestion())), 
						this);
			}
		}
		return RuleCheckResult.NO_MATCH;
	}

}
