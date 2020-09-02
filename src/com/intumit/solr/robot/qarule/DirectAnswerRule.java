package com.intumit.solr.robot.qarule;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.MultiChannelAnswer;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.qadialog.GroovyUtil;

import groovy.lang.Binding;

/**
 * 判斷知識主題
 * 
 * @author herb
 */
public class DirectAnswerRule extends QAMatchingRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		Set<String> forceSearchSet = DictionaryDatabase.getDictionary(ctx.getTenant().getId(),
				DictionaryDatabase.Purpose.SEARCH);
		Set<String> dontSearchSet = DictionaryDatabase.getDictionary(ctx.getTenant().getId(),
				DictionaryDatabase.Purpose.DONT_SEARCH);

		if (forceSearchSet.contains(ctx.getCurrentQuestion()) && !dontSearchSet.contains(ctx.getCurrentQuestion())) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if (ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED) && ctx.getAnswerText() != null)
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		
		SolrDocument directAnswer = ctx.getQAUtil().searchDirectAnswer(ctx.getCurrentQuestion(), ctx);

		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		if (explainQA) {
			ctx.appendExplain("qu.directAnswer", directAnswer);
		}
		if (directAnswer != null) {
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			// 使 context 記住本次回答的主體
			ctx.setCurrentQA(new QA(directAnswer));

			if (explainQA) {
				ctx.appendExplain("[respDoc != null] so we have a QADoc answer", directAnswer);
			}

			String answer = "";
			String customScript = (String) directAnswer.getFieldValue("CUSTOM_SCRIPT_s");
			if (StringUtils.isNotEmpty(customScript)) {
				Binding binding = new Binding();
				binding.setProperty("ctx", ctx);
				binding.setProperty("currentQA", directAnswer);

				answer = (String) GroovyUtil.runScript(binding, customScript);
			}

			if (StringUtils.isNotEmpty(answer) && StringUtils.endsWith(answer, "{{...}}}")) {
				ctx.setAnswerText(StringUtils.substringBeforeLast(answer, "{{{...}}}"));
				answer = "";
			}

			if (StringUtils.isEmpty(answer)) {
				if (StringUtils.trimToNull(ctx.getQaChannel()) != null) {
					answer = MultiChannelAnswer.qaRuleGetAnswer(ctx,
							(String) directAnswer.getFieldValue("id"), ctx.getQAChannelInstance(), ctx.getUserType());
				}

				if (StringUtils.isEmpty(answer)) {
					answer = (String) directAnswer.getFieldValue("ANSWER_s");
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

			// 準備 OtherPossibleQuestions
			SolrDocumentList mlts = ctx.getQAUtil().mltCommonSense(ctx.getCurrentQuestion(), ctx,
					ctx.getTenant().getOtherMltQANum());
			if (explainQA)
				ctx.appendExplain("We have direct answer, now trying to find otherPossibleQuestions...",
						mlts != null ? mlts.clone() : mlts);
			JSONArray otherPossibleQuestions = new JSONArray();

			for (SolrDocument doc : mlts) {
				if (((Float) doc.getFirstValue("score")) > ctx.getTenant().getThreshold3() / 2) {
					Object id = doc.getFirstValue("id");

					if (directAnswer != null && id.equals(directAnswer.getFirstValue("id"))) {
						ctx.appendExplain(String.format(
								"score > %d, this is possible other question, but is skipped bcz equals to firstCommonSense",
								ctx.getTenant().getThreshold3()), doc);
						continue;
					}

					if (Boolean.TRUE.equals(doc.getFirstValue(QA.FN_IS_NOT_FOR_MLT))) {
						ctx.appendExplain(
								String.format(
										"This is possible other question, but is skipped bcz equals to not for MLT==true"),
								doc);
						continue;
					}

					if (explainQA) {
						ctx.appendExplain(String.format("score > %d, this is possible other question",
								ctx.getTenant().getThreshold3() / 2), doc);
					}

					try {
						JSONObject otherQ = new JSONObject();
						otherQ.put("question", doc.getFirstValue("QUESTION_s"));
						otherQ.put("briefly_question", doc.getFirstValue("Briefly_QUESTION_s"));
						otherQ.put("score", doc.getFirstValue("score"));
						otherQ.put("kid", doc.getFirstValue("kid_l"));

						otherPossibleQuestions.put(otherQ);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}

			if (otherPossibleQuestions.length() > 0) {
				ctx.setRequestAttribute("otherPossibleQuestions", otherPossibleQuestions);
			}

			ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
			return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD,
					QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

}
