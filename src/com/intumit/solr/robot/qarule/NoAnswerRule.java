package com.intumit.solr.robot.qarule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.NO_ANSWER_OPTION;
import com.intumit.solr.robot.qarule.NoAnswerRule.BehaviorWhenReachLimit;
import com.intumit.solr.robot.QAUtil;

public class NoAnswerRule implements PostQAMatchRule {
	public enum BehaviorWhenReachLimit {
		KEEP_NO_ANSWER, SPECIFIC_QA, CUSTOM_OPTIONS,
	}

	public static int DEFAULT_LIMIT = 3;
	
	int limit = DEFAULT_LIMIT;
	List<Map<String, Object>> options = null;
	BehaviorWhenReachLimit behaviorWhenReachLimit = BehaviorWhenReachLimit.KEEP_NO_ANSWER;
	Long specificQaKid = null;
	
	public NoAnswerRule() {
		super();
		initDefaultForwardOption();
	}

	public NoAnswerRule(Map<String, Object> configs) {
		super();
		init(configs);
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
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("limit")) {
			limit = ((Number)configs.get("limit")).intValue();
		}
		if (configs.containsKey("behaviorWhenReachLimit")) {
			behaviorWhenReachLimit  = BehaviorWhenReachLimit.valueOf((String)configs.get("behaviorWhenReachLimit"));
		}
		else {
			// DEFAULT
			behaviorWhenReachLimit = BehaviorWhenReachLimit.KEEP_NO_ANSWER;
		}
		
		if (configs.containsKey("specificQaKid")) {
			specificQaKid  = new Long((String)configs.get("specificQaKid"));
		}
		if (configs.containsKey("options")) {
			options = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> defOptions = (List<Map<String, Object>>)configs.get("options");

			for (Map<String, Object> option : defOptions) {
				String title = (String) option.get("title");
				String naoStr = (String) option.get("NO_ANSWER_OPTION");

				try {
					Map<String, Object> opt = new HashMap<String, Object>();
					opt.put("title", title);
					opt.put("NO_ANSWER_OPTION", NO_ANSWER_OPTION.valueOf(naoStr));
					if (option.containsKey("question")) {
						opt.put("question", option.get("question"));
					}
					options.add(opt);
				}
				catch (Exception ignoreIt) {}
			}
			
			/* 
			  [
				  {
				   "NO_ANSWER_OPTION": "CONTINUE",
				   "title": "我想問的是信用卡類的問題",
				   "question": "信用卡常見問題",
				   "comment": "當使用者選擇此選項，系統將以question欄位的值當作使用者的提問，把答案輸出給使用者"
				  },
				  {
				   "NO_ANSWER_OPTION": "CONTINUE",
				   "title": "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}",
				   "comment": "這個是一般的「繼續跟機器人對話」，實際上選此選項的回答內容就是 title 欄位的值"
				  },
				  {
				   "NO_ANSWER_OPTION": "WEB_CHAT",
				   "title": "{{F:FORWARD_OPTION_WEBCHAT}}",
				  }
			  ]
			 */
		}
		else {
			initDefaultForwardOption();
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (!ctx.isHasDirectAnswer() || !ctx.hasAnswerText())
			return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
		
		return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if(ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED))
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
		if (explainQA) {
			ctx.appendExplain("There is no any answer", "Ask user if he/she wants to redirect to CRM");
		}
		String output = "";

		ctx.getThenIncNoResultCount(); // 先增加數字
		int noResultCount = ctx.getNoResultCount();

		if (behaviorWhenReachLimit != BehaviorWhenReachLimit.KEEP_NO_ANSWER && noResultCount >= limit){
			NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			
    		switch (behaviorWhenReachLimit) {
    			case CUSTOM_OPTIONS:
    				output += "{{F:FORWARD_HEADER_GENERAL}}";
    				int counter = 1;
    				
    				for (Map<String, Object> option: options) {
    					String title = (String)option.get("title");
    					NO_ANSWER_OPTION nao = (NO_ANSWER_OPTION)option.get("NO_ANSWER_OPTION");
    					
    					int no = counter; counter++;
    					//output +=  NL + "{{F:ENTER}}" + (no) + ". " + QAUtil.toClickableHtml("" + no, title, ctx.isClientSupportHtml());
    				}
    				//	+ NL + "{{F:ENTER}}1." + QAUtil.toClickableHtml("1", "{{F:FORWARD_OPTION_STAY_WITH_ROBOT}}", ctx.isClientSupportHtml())
    				//	+ NL + "{{F:ENTER}}2." + QAUtil.toClickableHtml("2", "{{F:FORWARD_OPTION_MESSAGE_BOARD}}", ctx.isClientSupportHtml())
    				//output += NL + "{{F:FORWARD_FOOTER_GENERAL}}";
    				ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_ASK_USER_REDIRECT_TO_CRM, true);
    				ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_FORWARD_OPTIONS, options);
    				ctx.setAnswerText(output);
    				ctx.setQuestionType(QAContext.QUESTION_TYPE.PREPARE_FORWARD);
    				break;
				case SPECIFIC_QA:
					ctx.setCurrentQuestion(ctx.getQAUtil().getStandardQ(specificQaKid));
					return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
				case KEEP_NO_ANSWER:
					// NOT POSSIBLE HERE
					break;
				default:
					// NOT POSSIBLE HERE
					break;
    		}
		}
		else {
			if (!ctx.isHasDirectAnswer()) {
				if ((ctx.hasAnswerText() || ctx.getShowedOptionMenuSize() > 0) ) {
					output = "{{F:NO_ANSWER_BUT_HAS_RECOMMENDATION}}";
					ctx.setAnswerType(QAContext.ANSWER_TYPE.PROFESSIONAL_PLUGIN);
				}
				else  {
					output = "{{F:NO_ANSWER}}";
					ctx.setAnswerType(QAContext.ANSWER_TYPE.NO_ANSWER);
				}
			}
			if (ctx.hasAnswerText()) {
				ctx.prependAnswerText(NL + NL);
				ctx.prependAnswerText(output);
			}
			else {
				ctx.prependAnswerText(output);
			}
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		// Do nothing
		return result;
	}

}
