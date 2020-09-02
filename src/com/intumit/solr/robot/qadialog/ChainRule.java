package com.intumit.solr.robot.qadialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;

/**
 * ChainRule 跟 PipeRule 一樣，會依序執行每一個 rule
 * 差別在於 ChainRule 會把每一個 rule 的執行結果存到 ChainRuleCheckResult 當中，但不會影響 currentQuestion
 * 
 * @author herb
 *
 */
public class ChainRule extends Rule implements SearchableRule {

	List<Rule> rules = null;

	public ChainRule() {
	}
	
	public ChainRule(JSONObject cfg) {
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
		return "ChainRule [rules=" + rules + "]";
	}

	@Override
	public RuleCheckResult check(QAContext ctx) {
		List<RuleCheckResult> childResults = new ArrayList<RuleCheckResult>();
		RuleCheckResult lastMatchedResule = null;
		
		for (Rule rule: rules) {
			RuleCheckResult result = rule.check(ctx);
			
			if (result.isMatch()) {
				lastMatchedResule = result;
				
				if (result.getValue() != null) {
					// ctx.setCurrentQuestion(result.getValue());
				}
			}
			
			childResults.add(result);
		}
		
		if (lastMatchedResule != null)
			return new ChainRuleCheckResult(lastMatchedResule.status, lastMatchedResule.value, this, childResults);
		
		return ChainRuleCheckResult.NO_MATCH;
	}

	@Override
	public boolean applySearchCriteria(RuleCheckResult result, SolrQuery query) {
		boolean dirty = false;
		
		ChainRuleCheckResult crcr = (ChainRuleCheckResult)result;
		List<RuleCheckResult> childResults = crcr.getResults();
		
		for (int i=0; i < rules.size(); i++) {
			Rule childRule = rules.get(i);
			
			if (childRule instanceof SearchableRule) {
				RuleCheckResult childResult = childResults.get(i);
				
				if (childResult != null && childResult.isMatch()) {
					SearchableRule sr = (SearchableRule)childRule;
					
					if (sr.applySearchCriteria(childResult, query)) {
						dirty = true;
					}
				}
			}
		}
		return dirty;
	}

	@Override
	public boolean applySearchCriteria(RuleCheckResult result, SolrDocumentList docs) {
		boolean dirty = false;
		
		ChainRuleCheckResult crcr = (ChainRuleCheckResult)result;
		List<RuleCheckResult> childResults = crcr.getResults();
		
		for (int i=0; i < rules.size(); i++) {
			Rule childRule = rules.get(i);
			
			if (childRule instanceof SearchableRule) {
				RuleCheckResult childResult = childResults.get(i);
				
				if (childResult != null && childResult.isMatch()) {
					SearchableRule sr = (SearchableRule)childRule;
					
					if (sr.applySearchCriteria(childResult, docs)) {
						dirty = true;
					}
				}
			}
		}
		return dirty;
	}

}
