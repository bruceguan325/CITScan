package com.intumit.solr.robot.qarule;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;

import com.intumit.solr.robot.MultiChannelAnswer;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAChannel;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAOutputTemplate;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.qaplugin.CustomQA;
import com.intumit.solr.util.WiSeUtils;

/**
 * 特殊問答
 * 
 * @先決條件 跑過 PrepareForNotDirectAnswerRule
 * @author herb
 */
public class CustomQARule extends QAMatchingRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {

		// 這個在 PrepareForNotDirectAnswerRule 會準備好
		if ((Boolean)ctx.getRequestAttribute("doSearchOnly", false)) {
			SolrDocument firstCommonSense = (SolrDocument)ctx.getRequestAttribute("firstCommonSense");
			
			if (firstCommonSense == null || firstCommonSense.getFieldValue("CUSTOM_QA_ID_s") != null || ((Number)firstCommonSense.getFirstValue("score")).doubleValue() < ctx.getTenant().getThreshold1()) {
				return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
			}
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
		
		Class outputClazz = QAOutputTemplate.findRegisteredClass("GeneralTextOutput");
		QAOutputTemplate outputTpl;
		try {
			outputTpl = (QAOutputTemplate) outputClazz.newInstance();
			SolrDocument customDoc = null;
			String preCQ = WiSeUtils.dblQuote(QAUtil.removeSymbols(ctx.getCurrentQuestion(), ctx.getTenant().getLocale()));
			if (!preCQ.equals("\" \"")){
				if (ctx.getRequestAttribute("qaTagFilter") != null) {
					qu.setQaTagFilter(ctx.getRequestAttribute("qaTagFilter").toString());
				}
				customDoc = qu.searchDirectCustomQA(ctx.getCurrentQuestion(), outputTpl, ctx);
				qu.setQaTagFilter(null);
				if (explainQA) ctx.appendExplain("qu.searchDirectCustomQA", customDoc);
			}
			if(!ctx.getFirstBankSpecialNext().equals("")){
				return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
			}
			if (customDoc != null) {
			    ctx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
			    if (!CustomQA.isSuggestWhenNoAnswerOnly(customDoc)) {
			     // 使 context 記住本次回答的主體
					ctx.setCurrentQA(new QA(customDoc));

					if (explainQA) {
						ctx.appendExplain("[respDoc != null] so we have a QADoc answer", customDoc);
					}

					Float score = (Float)customDoc.getFirstValue("score");
					
					if (score == null) {
						customDoc.setField("score", 0);
						score = 0f;
					}
					
					Long kid = (Long) customDoc.getFirstValue("kid_l");
					double confidenceFactor = QAUtil.AnswerConfidenceLevel.calConfidenceFactor(score);

					String answer = "";
					if (StringUtils.trimToNull(ctx.getQaChannel()) != null) {
						answer = MultiChannelAnswer.qaRuleGetAnswer(ctx, (String) customDoc.getFieldValue("id"), ctx.getQAChannelInstance(), ctx.getUserType());
					}
					
					if (StringUtils.isEmpty(answer)) {
						answer = (String) customDoc.getFieldValue("ANSWER_s");
					}

					if (ctx.hasAnswerText()) {
						ctx.appendNL().appendAnswerText(ctx.isClientSupportHtml() ? WiSeUtils.nl2br(answer) : answer);
					} else {
						ctx.setAnswerText(ctx.isClientSupportHtml() ? WiSeUtils.nl2br(answer) : answer);
					}
					
					QAChannel ch = ctx.getQAChannelInstance();

					ctx.setResponseAttribute("confidence", QAUtil.AnswerConfidenceLevel.findConfidenceLevel(score).getName());
					ctx.setResponseAttribute("confidenceFactor", confidenceFactor);
					ctx.setResponseAttribute("kid", kid);
					ctx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
					ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
					
					return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
					// end
			    }
			    
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

}
