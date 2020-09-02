package com.intumit.solr.robot.qarule;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.MultiChannelAnswer;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qadialog.GroovyUtil;

import groovy.lang.Binding;

public class EssentialKeywordRule extends QAMatchingRule {


	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (!ctx.getTenant().getEnableEssentialKeywordMode() || ctx.isHasDirectAnswer()) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		
		if (ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED) && ctx.getAnswerText() != null)
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		
		SolrDocument essentialKeywordAnswer = ctx.getQAUtil().searchDirectAnswerByEssentialKeyword(ctx);

		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		if (explainQA) {
			ctx.appendExplain("qu.essentialKeywordAnswer", essentialKeywordAnswer);
		}
		
		if (essentialKeywordAnswer != null) {
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			// 使 context 記住本次回答的主體
			ctx.setCurrentQA(new QA(essentialKeywordAnswer));

			if (explainQA) {
				ctx.appendExplain("[respDoc != null] so we have a QADoc answer", essentialKeywordAnswer);
			}

			String answer = "";
			String customScript = (String) essentialKeywordAnswer.getFieldValue("CUSTOM_SCRIPT_s");
			if (StringUtils.isNotEmpty(customScript)) {
				Binding binding = new Binding();
				binding.setProperty("ctx", ctx);
				binding.setProperty("currentQA", essentialKeywordAnswer);
				answer = (String) GroovyUtil.runScript(binding, customScript);
			}

			if (StringUtils.isNotEmpty(answer) && StringUtils.endsWith(answer, "{{...}}}")) {
				ctx.setAnswerText(StringUtils.substringBeforeLast(answer, "{{{...}}}"));
				answer = "";
			}

			if (StringUtils.isEmpty(answer)) {
				if (StringUtils.trimToNull(ctx.getQaChannel()) != null) {
					answer = MultiChannelAnswer.qaRuleGetAnswer(ctx,
							(String) essentialKeywordAnswer.getFieldValue("id"), ctx.getQAChannelInstance(), ctx.getUserType());
				}

				if (StringUtils.isEmpty(answer)) {
					answer = (String) essentialKeywordAnswer.getFieldValue("ANSWER_s");
				}
			}

			if (ctx.hasAnswerText()) {
				ctx.appendAnswerText(NL);
				ctx.appendAnswerText(answer);
			} else {
				ctx.appendAnswerText(answer);
			}

			if (StringUtils.isNotEmpty(answer)) {
				ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
			}
			ctx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
			// end

			ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
			
			return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public void init(Map<String, Object> configs) {
		
	}
	
	

}
