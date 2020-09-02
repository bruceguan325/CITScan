package com.intumit.solr.robot.qarule;

import java.util.Map;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;

/**
 * 判斷知識主題
 * 
 * @author herb
 */
public class SetCurrentKPsRule implements PreQAMatchRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (ctx.getCurrentQuestion() == null) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		DictionaryDatabase[] currentKPs = new DictionaryDatabase[0];
		currentKPs = KnowledgePointDictionary.search(ctx.getTenant().getId(), ctx.getCurrentQuestion().toCharArray(), null);
		ctx.setCurrentKPs(currentKPs);
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

}
