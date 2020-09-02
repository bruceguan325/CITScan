package com.intumit.solr.robot.qarule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.NO_ANSWER_OPTION;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.AttentionKeywordDictionary;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;

/**
 * 判斷敏感詞，有就直接跳選項（不同客戶可能會改這裡邏輯）
 * 
 * @author herb
 */
public class AttentionKeywordRule implements PreQAMatchRule {
	public static final String REQ_ATTR_ATTENTION_KEYWORDS = "attentionKeywords";
	List<Map<String, Object>> options = null;
	
	public AttentionKeywordRule() {
		super();
		initDefaultForwardOption();
	}

	public AttentionKeywordRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		
		// 敏感詞轉出的選項，目前僅用於敏感詞，也許行銷詞也能用一樣的機制來做
		if (configs.containsKey("options")) {
			options = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> defOptions = (List<Map<String, Object>>)configs.get("options");

			for (Map<String, Object> option: defOptions) {
				String title = (String)option.get("title");
				String naoStr = (String)option.get("NO_ANSWER_OPTION");
				
				Map<String, Object> newOpt = new HashMap<String, Object>();
				newOpt.put("title", title);
				newOpt.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.valueOf(naoStr));
				
				options.add(newOpt);
			}
		}
		else {
			initDefaultForwardOption();
		}
	}
	
	private void initDefaultForwardOption() {
		options = new ArrayList<Map<String, Object>>();
		Map<String, Object> opt1 = new HashMap<String, Object>();
		opt1.put("title", "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}");
		opt1.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.CONTINUE);
		Map<String, Object> opt2 = new HashMap<String, Object>();
		opt2.put("title", "{{F:FORWARD_OPTION_WEBCHAT}}");
		opt2.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.WEB_CHAT);
		
		options.add(opt1);
		options.add(opt2);
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
		
		boolean marketingWord = false;
		boolean negativeWord = false;
		System.out.println(ctx.getTenant().getId() + "/" + ctx.getCurrentQuestion());
		String sensitiveAnswer = "";
		DictionaryDatabase[] akps = AttentionKeywordDictionary.search(ctx.getTenant().getId(), ctx.getCurrentQuestion().toCharArray(), null);
		
		if (akps != null && akps.length > 0) {
			List<String> k = new ArrayList<String>();
			ArrayList<Map<String, Object>> keyword = new ArrayList<Map<String, Object>>();
		
			for (DictionaryDatabase kp: akps) {
				k.add(kp.getKeyword());
				Map<String, Object> keywordData = new HashMap<String, Object>();
				keywordData.put( "keyword", kp.getKeyword() );
				keywordData.put( "flag", kp.getPurposesJson() );
				keyword.add(keywordData);
		
				// 敏感詞直接跳選項，選項在 options 變數裡頭
				if (kp.getPurposeSet().contains(DictionaryDatabase.Purpose.BLACKLIST)) {
					if(sensitiveAnswer.isEmpty()){
						NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
						sensitiveAnswer += "{{F:FORWARD_HEADER_NEGATIVE}}";
						int counter = 1;
						
						/*
						for (Map<String, Object> option: options) {
							String title = (String)option.get("title");
							NO_ANSWER_OPTION nao = (NO_ANSWER_OPTION)option.get("NO_ANSWER_OPTION");
							
							int no = counter; counter++;
							sensitiveAnswer +=  NL + "{{F:ENTER}}" + (no) + ". " + QAUtil.toClickableHtml("" + no, title, ctx.isClientSupportHtml());
						}
						*/
						
						negativeWord = true;
						ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, true);
						ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_HAS_NEGATIVE_KEYWORDS, true);
						ctx.setResponseAttribute(ForwardToCrmRule.REQ_ATTR_HAS_NEGATIVE_KEYWORDS, true);
						//ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_FORWARD_OPTIONS, options);
					}
				}
				
				// 行銷詞目前不是給選項，之後應該也用 options 之類的機制來做
				if (kp.getPurposeSet().contains(DictionaryDatabase.Purpose.MARKETING)) {
					if (sensitiveAnswer.isEmpty()){
						sensitiveAnswer += "{{F:FORWARD_HEADER_MARKETING}}";
						marketingWord = true;
						ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, true);
						ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_HAS_MARKETING_KEYWORDS, true);
						ctx.setResponseAttribute(ForwardToCrmRule.REQ_ATTR_HAS_MARKETING_KEYWORDS, true);
					}
				}
			}
		
			try {
				ctx.setResponseAttribute(REQ_ATTR_ATTENTION_KEYWORDS, new JSONArray(k));
				ctx.setResponseAttribute("keyword", new JSONArray(keyword));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		if (marketingWord || negativeWord) {
			//sensitive word exist
			ctx.setAnswerText(sensitiveAnswer);
			ctx.setAnswerType(QAContext.ANSWER_TYPE.UNKNOWN);
			ctx.setQuestionType(QAContext.QUESTION_TYPE.PREPARE_FORWARD);
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
	
	public static boolean hasMarketingKeywords(QAContext ctx) {
		Boolean b = (Boolean)ctx.getRequestAttribute(ForwardToCrmRule.REQ_ATTR_HAS_MARKETING_KEYWORDS);
		if (b != null && b.booleanValue()) {
			return true;
		}
		
		return false;
	}
	
	public static boolean hasNegativeKeywords(QAContext ctx) {
		Boolean b = (Boolean)ctx.getRequestAttribute(ForwardToCrmRule.REQ_ATTR_HAS_NEGATIVE_KEYWORDS);
		if (b != null && b.booleanValue()) {
			return true;
		}
		
		return false;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

}
