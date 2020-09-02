package com.intumit.solr.robot.qarule;

import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.util.WiSeUtils;

/**
 * 外部來源QA
 * 
 * @author herb
 */
public class OuterSourceRule extends QAMatchingRule {
	Set<String> allowedAnswerTypes = new HashSet<>();
	
	public OuterSourceRule() {
		super();
	}
	
	void loadDefaultOptions() {
		allowedAnswerTypes.add(ANSWER_TYPE.PROFESSIONAL.name());
		allowedAnswerTypes.add(ANSWER_TYPE.CASUAL.name());
	}

	public OuterSourceRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {

		if (configs.containsKey("allowedAnswerTypes")) {
			List<Object> allowed = (List<Object>)configs.get("allowedAnswerTypes");
			
			for (Object o: allowed) {
				if (o instanceof String) {
					allowedAnswerTypes.add((String)o);
				}
			}
		}
		else {
			loadDefaultOptions();
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (ctx.getTenant().getOuterSourceUrl() == null || ctx.getTenant().getOuterSourceUrl().equals("") || ctx.hasAnswerText() || KnowledgePointDictionary.search(ctx.getTenant().getId(), ctx.getCurrentQuestion().toCharArray(), ctx.getRestrictToQaCategory()).length > 0) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		try {
			String respStr = WiSeUtils.getDataFromUrl(ctx.getTenant()
					.getOuterSourceUrl() + "&noRecommand=true&html=" + ctx.isClientSupportHtml() + "&id=" + ctx.getContextId() + "&q=" + URLEncoder.encode(ctx.getCurrentQuestion(),"utf-8"));
			
			if (explainQA) {
				ctx.appendExplain("Got data from Outter source", "" + respStr);
			}
			
			JSONObject resp = new JSONObject(respStr);
			//boolean robotDefaultVoice = false;
			
			if (resp.has("answerType") && allowedAnswerTypes.contains(resp.get("answerType")) && resp.has("output")) {
				String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
				String answer = (String) resp.get("output");
				
				if (explainQA) {
					ctx.appendExplain("Accept the answer from Outter source", "" + answer);
				}

				if (resp.has("outputVoice")) {
					ctx.setResponseAttribute("outputVoice", resp.getString("outputVoice"));
				}

				if (ctx.hasAnswerText()) {
					ctx.appendAnswerText(NL);
					ctx.appendAnswerText(answer);
				} else {
					ctx.setAnswerText(answer);
				}
				ctx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
				ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
				return new PostRuleCheckResult(
						PostRuleCheckResult.Status.FORWARD,
						QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
			}
		} catch (Exception e) {
			if (explainQA) {
				ctx.appendExplain("Error when getting data from Outter source", "" + e.getMessage());
			}
			e.printStackTrace();
		}

		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

}
