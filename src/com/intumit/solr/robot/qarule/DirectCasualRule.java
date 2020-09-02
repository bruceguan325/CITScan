package com.intumit.solr.robot.qarule;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.solr.common.SolrDocument;

import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;

/**
 * 判斷知識主題
 * 
 * @author herb
 */
public class DirectCasualRule extends QAMatchingRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		// QAUtil.SVOParser svo = new QAUtil.SVOParser(postTags);
		Set<String> forceSearchSet = DictionaryDatabase.getDictionary(ctx
				.getTenant().getId(), DictionaryDatabase.Purpose.SEARCH);
		Set<String> dontSearchSet = DictionaryDatabase.getDictionary(ctx
				.getTenant().getId(), DictionaryDatabase.Purpose.DONT_SEARCH);

		if (forceSearchSet.contains(ctx.getCurrentQuestion()) && !dontSearchSet.contains(ctx.getCurrentQuestion())) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if(ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED) || !ctx.getLastFirstBankSpecialNext().equals(""))
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		SolrDocument directAnswer = ctx.getQAUtil().searchDirectCasual(ctx.getCurrentQuestion(), ctx);
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		if (explainQA) {
			ctx.appendExplain("qu.directCasual", directAnswer);
		}
		
		if (directAnswer != null) {
			
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			String answer = (String) directAnswer.getFirstValue("ANSWER_s");
			if(directAnswer.getFieldValue("id").equals("CASUAL-1")){
				answer = ctx.getMessage("casual.special.answer.1");
			}
			List<String> answerAlts = (List<String>) directAnswer.getFieldValue("ANSWER_ALT_ms");
		
			if (answerAlts != null && answerAlts.size() > 0) {
				int r = RandomUtils.nextInt(answerAlts.size() + 1);
		
				if (r > 0) {
					answer = (String)answerAlts.get(r - 1);
				}
			}
		
			ctx.setAnswerText(answer);
			ctx.setQuestionType(QAContext.QUESTION_TYPE.CASUAL);
			ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.CASUAL);
		
			if (explainQA) {
				ctx.appendExplain("Use Casual answer", directAnswer);
			}
			
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

}
