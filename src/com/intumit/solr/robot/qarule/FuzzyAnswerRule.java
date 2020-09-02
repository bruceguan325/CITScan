package com.intumit.solr.robot.qarule;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.MultiChannelAnswer;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAOutputTemplate;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.qaplugin.CustomQA;
import com.intumit.solr.util.WiSeUtils;

/**
 * 用比較 Fuzzy 的方式試圖找出答案
 * 
 * @先決條件 跑過 PrepareForNotDirectAnswerRule
 * @author herb
 */
public class FuzzyAnswerRule extends QAMatchingRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		// 這個在 PrepareForNotDirectAnswerRule 會準備好
		if ((Boolean)ctx.getRequestAttribute("doSearchOnly", false)) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if(ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED) && ctx.getAnswerText() != null)
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
		QAUtil qu = ctx.getQAUtil();
		
		QA directAnswer = ctx.getCurrentQA();
		SolrDocumentList mlts = (SolrDocumentList)ctx.getRequestAttribute("mlts");
		SolrDocumentList searched = (SolrDocumentList)ctx.getRequestAttribute("searched");
		SolrDocument firstCommonSense = (SolrDocument)ctx.getRequestAttribute("firstCommonSense");
		Float respScore = 0f;

		if (explainQA) {
			ctx.appendExplain("qu.mltCommonSense",  mlts != null ? mlts.clone() : null);
			ctx.appendExplain("qu.searchCommonSense", searched != null ? searched.clone() : null);
			ctx.appendExplain("firstCommonSense", firstCommonSense);
		}
	
		if (mlts.size() > 0) {
			if (firstCommonSense != null) {
				if (explainQA) ctx.appendExplain("FirstCommonSense Not Null, push to mlts", firstCommonSense);
				mlts.add(0, firstCommonSense);
			}
		}
		else if (searched.size() > 0) {
			if (explainQA) ctx.appendExplain("Searched result > 0, replace mlts", searched.clone());
			mlts = searched;
		}
		if (explainQA) ctx.appendExplain("Now going to scan mlts for best answer", mlts.clone());

		if (mlts.size() > 0) {
			for (SolrDocument mydoc : mlts) {
				float score = (Float) mydoc.getFirstValue("score");
				System.out.println("***** Oh My God *****" + mydoc + " **** " + score);
				respScore = score;

				if (score > ctx.getTenant().getThreshold2()) {
					if (explainQA) {
						ctx.appendExplain(String.format("score > %d, this is our best answer, break loop", ctx.getTenant().getThreshold2()), mydoc);
					}
					mlts.remove(0);
					directAnswer = new QA(mydoc);
					break;
				}
				else {
					if (explainQA) {
						ctx.appendExplain(ctx.getMessage("no.high.accuracy.results") + ", break loop", mydoc);
					}
					break;
				}
			}
		}
		else {
			if (explainQA) {
				ctx.appendExplain("mlts.size() == 0 && searched.size() == 0", mlts != null ? mlts.clone() : mlts);
			}
			respScore = 0f;
		}
	
		if (explainQA) ctx.appendExplain("Trying to find otherPossibleQuestions...", mlts != null ? mlts.clone() : mlts);
		// 沒有主回答也還是有可能有建議問題
		JSONArray otherPossibleQuestions = new JSONArray();
		Set<String> dupCheck = new HashSet<String>();
		
		for (SolrDocument doc : mlts) {
			if (((Float) doc.getFirstValue("score")) > ctx.getTenant().getThreshold3()/2) {
				String id = (String)doc.getFirstValue("id");
	
				if (dupCheck.contains(id)) {
					continue;
				}
	
				if (directAnswer != null && id.equals(directAnswer.getId())) {
					ctx.appendExplain(String.format("score > %d, this is possible other question, but is skipped bcz equals to firstCommonSense", ctx.getTenant().getThreshold3()), doc);
					continue;
				}
	
				if (Boolean.TRUE.equals(doc.getFirstValue(QA.FN_IS_NOT_FOR_MLT))) {
					ctx.appendExplain(String.format("This is possible other question, but is skipped bcz equals to not for MLT==true"), doc);
					continue;
				}
	
				if (explainQA) {
					ctx.appendExplain(String.format("score > %d, this is possible other question", ctx.getTenant().getThreshold3()), doc);
				}
	
				try {
					dupCheck.add(id);
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

		System.out.println("-----FuzzyAnswerRule:-----\n" + otherPossibleQuestions + "\n-----END-----");
		
		ctx.setRequestAttribute("otherPossibleQuestions", otherPossibleQuestions);
		
		if (directAnswer != null) {
			// 使 context 記住本次回答的主體
			ctx.setCurrentQA(directAnswer);

			if (explainQA) {
				ctx.appendExplain(
						"[respDoc != null] so we have a QADoc answer",
						directAnswer);
			}
			
			if(directAnswer.getFieldValue("score") == null)
				return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);

			float score = (Float)directAnswer.getFieldValue("score");
			Long kid = (Long) directAnswer.getFieldValue("kid_l");
			double confidenceFactor = QAUtil.AnswerConfidenceLevel.calConfidenceFactor(score);

			String answer = "";
			if (StringUtils.trimToNull(ctx.getQaChannel()) != null) {
				answer = MultiChannelAnswer.qaRuleGetAnswer(ctx, (String) directAnswer.getFieldValue("id"), ctx.getQAChannelInstance(), ctx.getUserType());
			}
			
				if (StringUtils.isEmpty(answer)) {
					answer = (String) directAnswer.getFieldValue("ANSWER_s");
				}

			if (ctx.hasAnswerText()) {
				ctx.appendAnswerText(NL);
				ctx.appendAnswerText(answer);
			} else {
				ctx.appendAnswerText(answer);
			}

			ctx.setResponseAttribute("confidence", QAUtil.AnswerConfidenceLevel.findConfidenceLevel(score).getName());
			ctx.setResponseAttribute("confidenceFactor", confidenceFactor);
			ctx.setResponseAttribute("kid", kid);
			ctx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
			// end

			ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
			
			return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

}
