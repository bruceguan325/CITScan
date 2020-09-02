package com.intumit.solr.robot.qarule;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.RobotFormalAnswers;

public class WelcomeMessageRule implements PreQAMatchRule {

	private static final long serialVersionUID = 2621041985606022762L;

	private String key = "WELCOME_MSG";
	private String fixQ = "welcomemessage";

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("key")) {
			key = (String)configs.get("key");
		}
		if(configs.containsKey("fixQuestion")) {
			fixQ = (String) configs.get("fixQuestion");
		}
	}

	public WelcomeMessageRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if(StringUtils.equalsIgnoreCase(ctx.getCurrentQuestion(), fixQ)) {
			return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		String output = RobotFormalAnswers.giveMeRandomAnswer(ctx, key, "Welcome!");
		ctx.setQuestionType(QAContext.QUESTION_TYPE.WELCOME);
		ctx.setAnswerType(QAContext.ANSWER_TYPE.WELCOME);
		ctx.setAnswerText(output);

		return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return result;
	}

}
