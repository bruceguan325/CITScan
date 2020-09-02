package com.intumit.solr.robot.qarule;

import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.RobotFormalAnswers;

public class TooManyRepeatQuestionRule implements PreQAMatchRule {
	public static int DEFAULT_LIMIT = 5;
	
	int limit = DEFAULT_LIMIT;
	
	public TooManyRepeatQuestionRule() {
		super();
	}

	public TooManyRepeatQuestionRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("limit")) {
			limit = ((Number)configs.get("limit")).intValue();
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		// 檢查問題是否重複，有的話計算重複次數
		if (StringUtils.equalsIgnoreCase(ctx.getCurrentQuestion(), ctx.getLastQuestion())) {
			ctx.incThenGetRepeatSameQuestionCount();
		}
		else {
			ctx.setRepeatSameQuestionCount(1);
		}
		
		if (ctx.getRepeatSameQuestionCount() >= limit) {
			if (ctx.getTenant().getEnableQAExplain()) {
				ctx.appendExplain("Too Many Repeat", "使用者重複輸入問題");
			}
			String defaultAnswer = ctx.getMessage("default.answer");
			String output = RobotFormalAnswers.giveMeRandomAnswer(ctx.getTenant().getId(), "MANY_REPEAT_SAME_QUESTION", defaultAnswer);
			ctx.setQuestionType(QAContext.QUESTION_TYPE.REPEAT_TOO_MANY_TIMES);
			ctx.setAnswerType(QAContext.ANSWER_TYPE.REPEAT_TOO_MANY_TIMES);
			ctx.setAnswerText(output);
			
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		// Do nothing
		return result;
	}

}
