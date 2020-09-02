package com.intumit.solr.robot.qarule;

import java.util.Map;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAContext.QUESTION_TYPE;

public class DummyAnswerRule extends QAMatchingRule {
	Map<String, String> qaPair = null;

	public DummyAnswerRule() {
		super();
	}

	public DummyAnswerRule(Map<String, Object> configs) {
		super();
		init(configs);
	}
	
	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (qaPair == null) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if (qaPair.containsKey(ctx.getCurrentQuestion())) {
			ctx.setAnswerText(qaPair.get(ctx.getCurrentQuestion()));
			ctx.setQuestionType(QUESTION_TYPE.PROFESSIONAL);
			ctx.setHasDirectAnswer(true, ANSWER_TYPE.PROFESSIONAL);
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

	@Override
	public void init(Map<String, Object> configs) {
		
		if (configs.containsKey("qaPair")) {
			qaPair = (Map<String, String>) configs.get("qaPair");
		}
	}

}
