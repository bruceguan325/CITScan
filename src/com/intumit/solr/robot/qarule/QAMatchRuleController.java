package com.intumit.solr.robot.qarule;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.ConstructorUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.QAChannel;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;

import flexjson.JSONDeserializer;

public class QAMatchRuleController {
	public static final String RULE_CHAIN_NAME_PRE_QA_MATCH = "_PRE_MATCH_CHAIN";
	public static final String RULE_CHAIN_NAME_QA_MATCHING = "_MATCHING_CHAIN";
	public static final String RULE_CHAIN_NAME_POST_QA_MATCH = "_POST_MATCH_CHAIN";
	public static final String RULE_CHAIN_NAME_DIALOG_CHAIN = "_DIALOG_CHAIN";
	public static final String RULE_CHAIN_NAME_PLUGIN_ONLY_CHAIN = "_PLUGIN_ONLY_CHAIN";
	public static final String RULE_CHAIN_NAME_EVENT_TYPE_CHAIN = "_EVENT_TYPE_CHAIN";

	private static final Logger LOG = LoggerFactory.getLogger(QAMatchRuleController.class);
	
	public static boolean clear(String key) {
		if (map.containsKey(key)) {
			map.remove(key);
			return true;
		}
		
		return false;
	}
	
	public static Map<String, QAMatchRuleController> map = new HashMap<String, QAMatchRuleController>();
	
	public static QAMatchRuleController getInstance(Tenant tenant) {
		return getInstance("tenant:" + tenant.getId());
	}
	public static QAMatchRuleController getInstance(QAChannel ch) {
		return getInstance("channel:" + ch.getId());
	}
	
	/**
	 * key 為 "tenant:XX" (XX為公司編號） 或者 "channel:OO" (OO 為頻道編號）
	 * 當用 channel:OO 時，若該指定 channel 沒有設定 flow，會看 channel.defaultChannelCode 的，會遞廻直到最上層
	 * 若都沒有，就會拿 tenant 的。如果也還是沒有，就用預設（srbt-config.json 當中所設定的 default）
	 * 
	 * @param key
	 * @return
	 */
	public static QAMatchRuleController getInstance(String key) {
		QAMatchRuleController instance = map.get(key);
		
		if (instance == null) {
			synchronized (map) {
				if (map.get(key) == null) {
					instance = new QAMatchRuleController();
					
					JSONObject tQaCtrlFlowJson = null;
					int id = Integer.parseInt(StringUtils.substringAfter(key, ":"));
					
					if (StringUtils.startsWith(key, "tenant:")) {
						Tenant t = Tenant.get(id);
						try {
							tQaCtrlFlowJson = new JSONObject(t.getQaMatchCtrlFlow());
						}
						catch (Exception ignore) {}
					}
					else if (StringUtils.startsWith(key, "channel:")) {
						QAChannel ch = QAChannel.get(id);
						int tenantId = ch.getTenantId();
						
						// 想辦法看有沒有 channel 自訂的 flow，若沒有就找上層 channel 看有沒有
						while (true) {
							try {
								tQaCtrlFlowJson = new JSONObject(ch.getQaMatchCtrlFlow());
							}
							catch (Exception fail2default) {
								ch = ch.getDefaultChannel();
							}
							
							if (tQaCtrlFlowJson != null || ch == null) break;
						}
						
						// 若所有上層 channel 都沒有 flow，就拿公司別的
						if (tQaCtrlFlowJson == null) {
							Tenant t = Tenant.get(tenantId);
							try {
								tQaCtrlFlowJson = new JSONObject(t.getQaMatchCtrlFlow());
							}
							catch (Exception ignore) {}
						}
					}
					
					if (tQaCtrlFlowJson == null) {
						// 如果連設定檔都沒有，這裡也還是會拿到 null
						tQaCtrlFlowJson = QAMatchRuleController.getDefaultQAMatchControllerFlow();
					}
					
					JSONDeserializer jds = new JSONDeserializer();
					
					// 一樣往回相容，為了沒有設定檔的可以運行，這裡是判斷有 json 的才用 json，沒有的用系統預設
					if (tQaCtrlFlowJson != null) {
						String[] ruleType = new String[] {"PreQAMatchRule", "QAMatchingRule", "PostQAMatchRule"};
						String[] ruleChaneName = new String[] {QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH, QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH};
						
						for (int i=0; i < ruleType.length; i++) {
							List<JSONObject> rules = QAMatchRuleController.parseQAMatchControllerFlowJson(tQaCtrlFlowJson, ruleType[i]);
							
							for (JSONObject ruleJson: rules) {
								try {
									Map<String, Object> cfg = null;
									String clazzName = ruleJson.getString("name");
									
									if (clazzName.indexOf(".") == -1) {
										clazzName = "com.intumit.solr.robot.qarule." + clazzName;
									}
									
									if (ruleJson.has("config")) {
										cfg = (Map<String, Object>)jds.deserialize(ruleJson.getJSONObject("config").toString());
									}
									
									if (cfg == null) {
										cfg = new HashMap<String, Object>();
									}
								
									Class clazz = Class.forName(clazzName);
									Class[] parameters = new Class[] {Map.class};
									Constructor cotr = ConstructorUtils.getAccessibleConstructor(clazz, parameters);
									if (cotr != null) {
										instance.addRule((QARule)cotr.newInstance(cfg), ruleChaneName[i]);
									}
									else {
										cotr = ConstructorUtils.getAccessibleConstructor(clazz, new Class[] {});
										instance.addRule((QARule)cotr.newInstance(), ruleChaneName[i]);
									}
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
					else {
						Map<String, Object> cfg = new HashMap<String, Object>();
						cfg.put("limit", 5);

						instance.addRule(new EventTypeRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
						instance.addRule(new ChineseFullToHalfConvertRule(cfg), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
						instance.addRule(new FirstbankPOCWiVoAdjustRule(cfg), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);

						instance.addRule(new TooManyRepeatQuestionRule(cfg), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
						instance.addRule(new ForwardToCrmRule(cfg), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);

						instance.addRule(new ChooseQAOptionRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);

						instance.addRule(new SetCurrentKPsRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
						instance.addRule(new SetRestrictToQaCategoryRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
						instance.addRule(new AttentionKeywordRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
						instance.addRule(new QAPluginRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);

						instance.addRule(new PrepareForQAMatchingRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);

						instance.addRule(new DirectAnswerRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
						instance.addRule(new QADialogRule(cfg), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);

						instance.addRule(new DirectCasualRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);

						instance.addRule(new PrepareForNotDirectAnswerRule(),
								QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
						instance.addRule(new CustomQARule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
						instance.addRule(new FuzzyAnswerRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
						instance.addRule(new OuterSourceRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);

						instance.addRule(new KmsDocExpireRule(), QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
						instance.addRule(new QAPluginAfterQAMatchingRule(),
								QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
						instance.addRule(new NoAnswerRule(cfg), QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
					}
					
					
					// DIALOG_CHAIN for direct forwardTo
					Map<String, Object> cfg = new HashMap<String, Object>();
					instance.addRule(new QADialogRule(cfg), RULE_CHAIN_NAME_DIALOG_CHAIN);
					
					// QAPluginAfterQAMatchingRule only
					instance.addRule(new QAPluginAfterQAMatchingRule(), RULE_CHAIN_NAME_PLUGIN_ONLY_CHAIN);
					
					map.put(key, instance);
				}
			}
		}
		
		return instance;
	}
	
	public static QAMatchRuleController getTempInstanceForMakeAnswer(Integer tenantId) {
		QAMatchRuleController instance = new QAMatchRuleController();

		//instance.addRule(new EventTypeRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
		//instance.addRule(new QAPluginRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);

		instance.addRule(new SetCurrentKPsRule(), QAMatchRuleController.RULE_CHAIN_NAME_PRE_QA_MATCH);
		instance.addRule(new PrepareForQAMatchingRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
		instance.addRule(new DirectAnswerRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
		instance.addRule(new PrepareForNotDirectAnswerRule(), QAMatchRuleController.RULE_CHAIN_NAME_QA_MATCHING);
		instance.addRule(new QAPluginAfterQAMatchingRule(), QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
		
		return instance;
	}
	
	protected QAMatchRuleController() {
		// for Testing
	}
	
	List<QARule> preRuleChain = new ArrayList<QARule>();
	List<QARule> ruleChain = new ArrayList<QARule>();
	List<QARule> postRuleChain = new ArrayList<QARule>();
	List<QARule> dialogChain = new ArrayList<QARule>();
	List<QARule> pluginOnlyChain = new ArrayList<QARule>();
	
	public void addRule(QARule rule, String chainName) {
		List<QARule> chain = getRuleChainByName(chainName);
		
		if (chain != null) {
			chain.add(rule);
		}
	}
	
	public QARule getRuleByClassName(String chainName, String clazzName) {
		List<QARule> rules = getRuleChainByName(chainName);
		
		if (rules != null) {
			for (QARule r: rules) {
				if (clazzName.equals(r.getClass().getName())) {
					return r;
				}
			}
		}
		
		return null;
	}
	
	List<QARule> getRuleChainByName(String chainName) {
		List<QARule> chain = null;
		
		if (RULE_CHAIN_NAME_PRE_QA_MATCH.equals(chainName)) {
			chain = preRuleChain;
		}
		else if (RULE_CHAIN_NAME_QA_MATCHING.equals(chainName)) {
			chain = ruleChain;
		}
		else if (RULE_CHAIN_NAME_POST_QA_MATCH.equals(chainName)) {
			chain = postRuleChain;
		}
		else if (RULE_CHAIN_NAME_DIALOG_CHAIN.equals(chainName)) {
			chain = dialogChain;
		}
		else if (RULE_CHAIN_NAME_PLUGIN_ONLY_CHAIN.equals(chainName)) {
			chain = pluginOnlyChain;
		}
		
		return chain;
	}
	
	public void check(QAContext ctx) {
		List<Object[]> pathLog = new ArrayList<Object[]>(); 
		/* Object[] contains 3 element, 
			[0] is rule name, 
			[1] is path key (unique for each rule in each request), 
			[2] is PreRuleCheckResult, 
			[3] is PostRuleCheckResult (if has)
			[4] is hasAnswer
			[5] is answerText
		*/
		
		// 標準流程
		// 先 pre match chain -> matching chain -> post match chain
		String[] flow = {
				RULE_CHAIN_NAME_PRE_QA_MATCH, 
				RULE_CHAIN_NAME_QA_MATCHING, 
				RULE_CHAIN_NAME_POST_QA_MATCH
				};
		
		PostRuleCheckResult forwardOrReturn = null;
		for (String defaultForwardTo: flow) {
			forwardOrReturn = checkRuleChain(ctx, defaultForwardTo, pathLog);
			
			if (forwardOrReturn != null) {
				// 一旦有 status 為 RETURN 或者 FORWARD
				// 就不再走原有流程，即便結束也不會接回來
				break;
			}
		}
			
		if (forwardOrReturn != null && forwardOrReturn.status == PostRuleCheckResult.Status.FORWARD) {
			int forwardCount = 0;
			do {
				forwardCount++; // 基本防止無窮回圈的措施
				
				for (String forwardTo: forwardOrReturn.forwardTo) {
					forwardOrReturn = checkRuleChain(ctx, forwardTo, pathLog);
					
					if (forwardOrReturn != null) {
						break;
					}
				}
			}
			while (forwardOrReturn != null 
					&& forwardOrReturn.status == PostRuleCheckResult.Status.FORWARD 
					&& forwardCount < 10);
			
			if (forwardCount == 10 && forwardOrReturn != null && forwardOrReturn.forwardTo != null) {
				LOG.error("There is possible infinite qa forward loop occurred.", ctx);
			}
		}

		ctx.setRequestAttribute("_PATH_KEY", "__rule_checked__");
		ctx.appendExplain(QAMatchRuleController.class.getName() + ".pathLog", pathLog);
	}
	
	PostRuleCheckResult checkRuleChain(QAContext ctx, String chainName, List<Object[]> pathLog) {
		List<QARule> chain = getRuleChainByName(chainName);
		
		boolean explain = ctx.getTenant().getEnableQAExplain();
		if (explain) {
			ctx.appendExplain(QAMatchRuleController.class.getName(), "Begin check rule chain [" + chainName + "]");
		}
		
		int verifyCode = 100;
		
		for (QARule r: chain) {
			String pathKey = "" + System.currentTimeMillis() + ("" + verifyCode++);
			ctx.setRequestAttribute("_PATH_KEY", pathKey);
			PreRuleCheckResult preR = r.onPreRuleCheck(ctx);
			
			if (explain) {
				ctx.appendExplain(QAMatchRuleController.class.getName(), " --> preCheck rule [" + r.getClass().getName() + "] status [" + preR.status + "]");
			}
			
			if (preR.status == PreRuleCheckResult.Status.NORMAL) {
				PostRuleCheckResult postR = r.checkRule(ctx);
				
				if (explain) {
					ctx.appendExplain(QAMatchRuleController.class.getName(), " --> check rule [" + r.getClass().getName() + "] status [" + postR.status + "]");
				}
			
				postR = r.onPostRuleCheck(ctx, postR);
				
				if (explain) {
					ctx.appendExplain(QAMatchRuleController.class.getName(), " --> postCheck rule [" + r.getClass().getName() + "] status [" + postR.status + "]");
				}
				pathLog.add(new Object[] {r.getClass().getCanonicalName(), pathKey, preR, postR, ctx.hasAnswerText(), ctx.getAnswerText()});
				
				if (postR.status == PostRuleCheckResult.Status.FORWARD) {
					LOG.debug("Forward from chain [" + chainName + "] to chain [" + StringUtils.join(postR.forwardTo, ", ") + "]");
					return postR;
				}
				else if (postR.status == PostRuleCheckResult.Status.RETURN) {
					LOG.debug("Direct return in chain [" + chainName + "]");
					return postR;
				}
			}
			else {
				pathLog.add(new Object[] {r.getClass().getCanonicalName(), pathKey, preR, null, ctx.hasAnswerText(), ctx.getAnswerText()});
			}
		}
		
		return null;
	}
	
	public static List<JSONObject> parseQAMatchControllerFlowJson(JSONObject json, String ruleType) {
		List<JSONObject> allRules = new ArrayList<JSONObject>();
		try {
			if (json.has(ruleType)) {
				JSONArray arr = json.getJSONArray(ruleType);
				
				for (int i=0; i < arr.length(); i++) {
					JSONObject obj = arr.optJSONObject(i);
					
					if (obj == null) {
						obj = new JSONObject();
						obj.put("name", arr.getString(i));
					}
					allRules.add(obj);
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return allRules;
	}
	
	public static JSONObject getQAMatchControllerFlowConfig() {
		JSONObject srbtJson = null;
		try {
			srbtJson = new JSONObject(FileUtils.readFileToString(new File(WiSeEnv.getHomePath() + "/srbt-config.json"), "UTF-8"));
			return srbtJson;
		}
		catch (Exception ignore) {
			System.out.println("Cannot read config from srbt-config.json:" + ignore.getMessage());
		}
		
		return null;
	}
	
	public static JSONObject getDefaultQAMatchControllerFlow() {
		JSONObject srbtJson = null;
		try {
			srbtJson = new JSONObject(FileUtils.readFileToString(new File(WiSeEnv.getHomePath() + "/srbt-config.json"), "UTF-8"));
			return srbtJson.getJSONObject("DefaultQAMatchFlow");
		}
		catch (Exception ignore) {
			System.out.println("Cannot read config from srbt-config.json:" + ignore.getMessage());
		}
		
		return null;
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
	}

}
