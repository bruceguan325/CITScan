package com.intumit.solr.robot.qadialog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;

/**
 * PipeRule 跟 ChainRule 一樣，會依序執行每一個 rule
 * 差別在於 PipeRule 會把每一個 rule 的執行結果設定到 currentQuestion（等於修改 question）
 * 
 * @author herb
 *
 */
public class PipeRule extends Rule {

	List<Rule> rules = null;

	public PipeRule() {
	}
	
	public PipeRule(JSONObject cfg) {
		super(cfg);
	}
	
	@Override
	public void loadConfig(JSONObject cfg) {
		// implement parse options
	}

	@Override
	public void init() {
		for (Rule r: rules) {
			r.init();
		}
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	@Override
	public String toString() {
		return "PipeRule [rules=" + rules + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
		RuleCheckResult lastResult = null;
		String origQ = ctx.getCurrentQuestion();
		
		for (Rule rule: rules) {
			lastResult = rule.check(ctx);
			
			if (lastResult.isMatch()) {
				ctx.setCurrentQuestion(lastResult.getValue());
			}
		}
		
		//ctx.setCurrentQuestion(origQ);
		
		if (lastResult != null)
			return lastResult;
		
		return RuleCheckResult.NO_MATCH;
	}

}
