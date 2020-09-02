package com.intumit.solr.robot.qarule;

import java.util.Map;
import java.util.Set;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAOutputTemplate;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.qaplugin.CustomQA;
import com.intumit.solr.util.WiSeUtils;

/**
 * 為了效能考量，當確定沒有直接答案時，先搜尋並準備好需要的變數放在 requestAttr 當中
 * 避免之後每一個 rule 都要重新搜尋
 * 
 * @author herb
 */
public class PrepareForNotDirectAnswerRule extends QAMatchingRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		// QAUtil.SVOParser svo = new QAUtil.SVOParser(postTags);
		Set<String> forceSearchSet = DictionaryDatabase.getDictionary(ctx.getTenant().getId(), DictionaryDatabase.Purpose.SEARCH);
		Set<String> dontSearchSet = DictionaryDatabase.getDictionary(ctx.getTenant().getId(), DictionaryDatabase.Purpose.DONT_SEARCH);

		if (forceSearchSet.contains(ctx.getCurrentQuestion()) && !dontSearchSet.contains(ctx.getCurrentQuestion())) {
			ctx.setRequestAttribute("doSearchOnly", true);
			
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if(ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED) && ctx.getAnswerText() != null)
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		QAUtil qu = ctx.getQAUtil();
		
		SolrDocumentList mlts = qu.mltCommonSense(ctx.getCurrentQuestion(), ctx, ctx.getTenant().getOtherMltQANum());
		SolrDocumentList searched = qu.searchCommonSense(ctx.getCurrentQuestion(), ctx, false, false, false, 0, 5);
		SolrDocument firstCommonSense = searched.size() > 0 ? searched.get(0) : null;
		SolrDocument firstMltCommonSense = mlts.size() > 0 ? mlts.get(0) : null;

		if (firstCommonSense == null) {
			firstCommonSense = firstMltCommonSense;
			ctx.appendExplain("firstCommonSense from MltCommonSense",  firstCommonSense);
		}
		else if (firstMltCommonSense != null && ((Number)firstMltCommonSense.getFirstValue("score")).floatValue() > ((Number)firstCommonSense.getFirstValue("score")).floatValue()) {
			firstCommonSense = firstMltCommonSense;
			ctx.appendExplain("firstCommonSense from MltCommonSense",  firstCommonSense);
		}

		if (explainQA) {
			ctx.appendExplain("qu.mltCommonSense",  mlts != null ? mlts.clone() : null);
			ctx.appendExplain("qu.searchCommonSense", searched != null ? searched.clone() : null);
			ctx.appendExplain("firstCommonSense", firstCommonSense);
		}
		
		ctx.setRequestAttribute("mlts", mlts);
		ctx.setRequestAttribute("searched", searched);
		ctx.setRequestAttribute("firstCommonSense", firstCommonSense);
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

}
