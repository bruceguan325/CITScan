package com.intumit.solr.robot.qarule;

import groovy.lang.Binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.mas.MASEnvironment;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.QUESTION_TYPE;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.QAUtil.FormalAnswerStickerReplacer;
import com.intumit.solr.robot.function.FunctionCollector;
import com.intumit.solr.robot.function.FunctionUtil;
import com.intumit.solr.robot.qadialog.GroovyUtil;
import com.intumit.solr.robot.qaplugin.ParsedOption;
import com.intumit.solr.robot.qaplugin.QAPlugin;
import com.intumit.solr.robot.qaplugin.QAPlugins;

import flexjson.JSONSerializer;

/**
 * 若使用者輸入要選擇前一題提供的選項，這裡就會 rewrite Question
 * 
 * @author herb
 */
public class ChooseQAOptionRule implements PreQAMatchRule {
	/**
	 * If any other rules, which ran before ChooseQAOptionRule, found a option that user supposed want to choose it, set the option in request attribute
	 */
	public static final String PRESET_OPTION_IN_REQ_ATTR = "PRESET_OPTION"; 
	static Logger infoLog = Logger.getLogger(ChooseQAOptionRule.class.getName());

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		String[] tempQues = ctx.getCurrentQuestion().split("");
		PostRuleCheckResult res = PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		
		if(MASEnvironment.needSkipCodeInChooseQAOptionRuleWhenMatchCurrentQuestion(ctx)) {
			infoLog.info("因CurrentQuestion而特別處理,CurrentQuestion:"+ctx.getCurrentQuestion());
		}else {
			if (StringUtils.length(ctx.getCurrentQuestion()) <= 2 && tempQues.length == 2) {
				// 怕使用者輸入 "1." 這樣的選項？
				if (StringUtils.isNumeric(tempQues[0]) && !StringUtils.isNumeric(tempQues[1])) {
					String newQues = ctx.getCurrentQuestion().replace(tempQues[1], "");
					if(!newQues.equals("0"))
						ctx.setCurrentQuestion(newQues);
				}
			}
		}

		List<OptionMenu> showedMenus = ctx.getAllShowedOptionMenuInLastAnswer();
		if (showedMenus.size() == 0) {
			showedMenus = ctx.getLastShowedOptionMenus();
		}

		Option choosedOpt = (Option)ctx.getRequestAttribute(PRESET_OPTION_IN_REQ_ATTR);
		if (choosedOpt == null) {
    		for (QAContext.OptionMenu m : showedMenus) {
    			for (QAContext.Option o : m.getOptions()) {
        
    				if (o.getValue() != null && o.getValue().matchSentences != null && o.getValue().matchSentences.length() > 0) {
    					// 拿掉空白例句
    					List<String> matchSentencesList = new ArrayList<String>();
    					List<String> matchRegexList = new ArrayList<String>();
    					JSONArray matchSentenceArray = o.getValue().matchSentences;
    					for (int i = 0; i < matchSentenceArray.length(); i++) {
    						String str = StringUtils.trimToNull(matchSentenceArray.optString(i));
    						if (StringUtils.isNotBlank(str)) {
    							if (StringUtils.startsWith("(?regexp)", str)) {
    								matchRegexList.add(str.substring("(?regexp)".length()));
    							}
    							else {
    								matchSentencesList.add(matchSentenceArray.optString(i));
    							}
    						}
    					}
    					String[] matchSentences = matchSentencesList.toArray(new String[0]);
    					if (matchSentences.length > 0) {
    						if (o.getValue().matchSentences.contains(ctx.getCurrentQuestion()) 
    							|| StringUtils.indexOfAny(ctx.getCurrentQuestion(), matchSentences) != -1
    							) {
    							choosedOpt = o;
    						}
    					}
    					for (String regex: matchRegexList) {
    						if (ctx.getCurrentQuestion().matches(regex)) {
    							choosedOpt = o;
    						}
    					}
    				}
        
    				if (choosedOpt == null) {
    					switch (m.getBehavior()) {
    						case FULL_MATCH_TITLE:
    						case NUMBER_OR_FULL_MATCH_TITLE:
    							if (StringUtils.equalsIgnoreCase(StringUtils.lowerCase(o.getName()), ctx.getCurrentQuestion())) {
    								choosedOpt = o;
    							}
    							break;
        
    						case FUZZY_TITLE:
    						case NUMBER_OR_FUZZY_TITLE:
    							if (StringUtils.length(ctx.getCurrentQuestion()) <= 3) {
    								if (StringUtils.containsIgnoreCase(o.getName(), ctx.getCurrentQuestion())) {
    									choosedOpt = o;
    								}
    							}
    							else if (StringUtils.getLevenshteinDistance(StringUtils.lowerCase(o.getName()),
    									ctx.getCurrentQuestion()) <= Math.max(2, o.getName().length() / 2f)) {
    								choosedOpt = o;
    							}
    							break;
        
    						case FUNCTION_CALL:
    							Object retVal = FunctionUtil.collectAndExec(o.getValue().question, ctx);
    							System.out.println("Got return of function call:" + retVal);
        
    							if (retVal != null) {
    								choosedOpt = o;
    							}
    							break;
        
    						default:
    							break;
    					}
    				}
    			}
    		}
    	}

		// 兩位數字以內
		if (choosedOpt == null && StringUtils.isNumeric(ctx.getCurrentQuestion())
				&& StringUtils.length(ctx.getCurrentQuestion()) <= 2) {
			int choosed = -1;
			try {
				choosed = Integer.parseInt(StringUtils.trim(ctx.getCurrentQuestion())) - 1;
			}
			catch (NumberFormatException e) {
			}

			choosedOpt = ctx.getShowedOption(showedMenus, choosed);
		}

		if (choosedOpt != null) {
			ctx.setRequestAttribute("_CHOOSED_OPT", choosedOpt);
			ctx.setEventSourceType(QAContext.EVENT_SOURCE_TYPE_BUTTON);
			OptionMenu menu = choosedOpt.getMenu();
			ctx.setEventSource(menu != null ? menu.getCreateFrom() : QAContext.EVENT_SOURCE_UNKNOWN);
			
			try {
				ctx.setResponseAttribute("_PRE_MATCH_OPT", new JSONObject(new JSONSerializer()
						.exclude("*._*", "*.class", "*.script", "*.matchSentences", "*.extraParams")
						.deepSerialize(choosedOpt)));
			}
			catch (JSONException e1) {
					e1.printStackTrace();
			}
			OptionAction oa = choosedOpt.getAction();
			ParsedOption po = choosedOpt.getValue();
			int infiniteLoopCheck = 0;

			while (oa == OptionAction.REDIRECT_TO_OPTION && StringUtils.trimToNull(po.pipe) != null) {
				if (infiniteLoopCheck++ >= 100) {
					// Error! 不應該有這麼多層的，代表 redirect_to_option 造成無窮回圈
					break;
				}

				if (po != null && StringUtils.isNotEmpty(po.script)) {
					Binding binding = new Binding();
					binding.setProperty("ctx", ctx);
					binding.setProperty("option", po);

					try {
						GroovyUtil.runScript(binding, po.script);
	
						System.out.println("Run script before redirect... ParsedOption after script ran:");
						System.out.println(po);
					}
					catch (Exception ex) {
						System.err.println("ChooseQAOptionRule script error:" + ex.getMessage());
						ex.printStackTrace();
					}
				}
				
				Option o = ctx.findOption(po.pipe);

				// 找到了標的 option，重新 set 一次
				if (o != null) {
					choosedOpt = o;

					ctx.setRequestAttribute("_CHOOSED_OPT", choosedOpt);
					oa = choosedOpt.getAction();
					po = choosedOpt.getValue();
				}
				else {
					break;
				}
			}

			try {
				ctx.setResponseAttribute("_MATCHED_OPT", new JSONObject(new JSONSerializer()
						.exclude("*._*", "*.class", "*.script", "*.matchSentences", "*.extraParams")
						.deepSerialize(choosedOpt)));
			}
			catch (JSONException e1) {
					e1.printStackTrace();
			}

			// 階層問答當中的 extra parameters 都塞入 response 中
			if (po != null && po.extraParams != null) {
				String[] names = JSONObject.getNames(po.extraParams);
				if (names != null) for (String ekey: names) {
					ctx.setResponseAttribute(ekey, po.extraParams.opt(ekey));
				}
			}

			if (po != null && StringUtils.isNotEmpty(po.script)) {
				Binding binding = new Binding();
				binding.setProperty("ctx", ctx);
				binding.setProperty("option", po);

				GroovyUtil.runScript(binding, po.script);

				System.out.println("ParsedOption after script ran:");
				System.out.println(po);
			}

			if (oa == OptionAction.INPUT_NUMBER
					|| oa == OptionAction.INPUT_TEXT) {
				ctx.setCurrentQuestion(po.question);

				if (choosedOpt.getSubMenuKey() != null) {
					ctx.showOptionMenu(choosedOpt.getSubMenuKey());
				}
			}
			else if (oa == OptionAction.REDIRECT_TO_QUESTION) {
				if (StringUtils.trimToNull(po.pipe) != null) {
					Long kid = QAUtil.id2Kid(po.pipe);
					if (kid != null) {
						QA pipe2 = new QA(ctx.getQAUtil().getMainQASolrDocument(kid));
						if (pipe2 != null) {
							ctx.setCurrentQuestion(pipe2.getQuestion());
							ctx.setQaAsAnswer(pipe2);
							return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD,
									QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
						}
					}
				}
			}
			else if (oa == OptionAction.DIRECT_ANSWER) {
				ctx.setQuestionType(QUESTION_TYPE.ANSWER_OPTION);
				ctx.setHasDirectAnswer(true, ANSWER_TYPE.INTERCEPTED);
				ctx.setAnswerText(po.answer);

				if (choosedOpt.getSubMenuKey() != null) {
					ctx.showOptionMenu(choosedOpt.getSubMenuKey());
				}

				res = PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
			else if (oa == OptionAction.PROCESS_BY_QAPLUGIN) {
				if (choosedOpt.getPluginId() != null) {
					QAPlugin p = QAPlugins.get(choosedOpt.getPluginId());
					res = p.onApiChoosedOption(ctx, choosedOpt);
				}
			}
			else if (oa == OptionAction.SUB_MENU_ONLY) {
				String subKey = choosedOpt.getSubMenuKey();
				ctx.showOptionMenu(subKey);
				ctx.setQuestionType(QUESTION_TYPE.ANSWER_OPTION);
				ctx.setHasDirectAnswer(true, ANSWER_TYPE.INTERCEPTED);

				res = PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
		}
		
		ctx.removeOnetimeOptionMenu();
		return res;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

}
