package com.intumit.solr.robot.qarule;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.solr.common.SolrDocument;

import com.intumit.solr.robot.CasualKeyword;
import com.intumit.solr.robot.CasualKeywordDictionary;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;

/**
 * 判斷知識主題
 * 
 * @author herb
 */
public class FuzzyCasualRule extends QAMatchingRule {
	Float threshold = null;

	public FuzzyCasualRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("threshold")) {
			threshold = ((Number)configs.get("threshold")).floatValue();
		}
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
		
		if (ctx.hasAnswerText())
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if(ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED))
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		SolrDocument directAnswer = ctx.getQAUtil().searchCasual(ctx.getCurrentQuestion(), ctx, threshold);
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		if (explainQA) {
			ctx.appendExplain("qu.fuzzyCasual", directAnswer);
		}
		
		if (directAnswer == null) {
			Set<CasualKeyword> casualKeywords = CasualKeywordDictionary.search(ctx.getTenant().getId(), ctx.getCurrentQuestion().toCharArray());
			System.out.println("Found casual keywords of " + ctx.getCurrentQuestion() + ":" + casualKeywords);
			if (casualKeywords.size() > 0) {
				int totalLen = 0;
				CasualKeyword rck = null;
				CasualKeyword[] arr = casualKeywords.toArray(new CasualKeyword[0]);
				int maxLen = 0;
				
				for (CasualKeyword ck: casualKeywords) {
					int len = ck.getValue().length();
					totalLen += len;
					
					if (len > maxLen) {
						maxLen = len;
					}
				}
				
				int r = new Random().nextInt(casualKeywords.size());
				while (true && maxLen >= 2) {
					rck = arr[r];
					
					if (rck.getValue().length() >= 2) {
						break;
					}
					else {
						r = new Random().nextInt(casualKeywords.size());
					}
				}
				
				if (rck != null && ctx.getCurrentQuestion().length() - totalLen < 4) {
					directAnswer = ctx.getQAUtil().searchDirectCasual(rck.getValue(), ctx);
					System.out.println("New direct casual answer by (" + rck.getValue() + ")" + directAnswer);
				}
				else {
					System.out.println("The len of (" + ctx.getCurrentQuestion() + ") much longer than totalCasualKeyword(" + totalLen + ")");
				}
			}
		}
		
		if (directAnswer != null) {
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			String answer = (String) directAnswer.getFirstValue("ANSWER_s");
			if (directAnswer.getFieldValue("id").equals("CASUAL-1")){
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
				ctx.appendExplain("Use fuzzy casual answer", directAnswer);
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
