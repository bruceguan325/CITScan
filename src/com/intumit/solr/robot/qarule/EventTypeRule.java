package com.intumit.solr.robot.qarule;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.QUESTION_TYPE;
import com.intumit.solr.robot.QAUtil;

/**
 * 若使用者輸入要選擇前一題提供的選項，這裡就會 rewrite Question
 * 
 * @author herb
 */
public class EventTypeRule implements PreQAMatchRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (ctx.getEventType() == null || "message".equalsIgnoreCase(ctx.getEventType().getCode())) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		QAUtil qu = ctx.getQAUtil();
		boolean tryToTriggerEvent = true;

		if (ctx.getTenant().getEnableQAExplain()) {
			ctx.appendExplain("EventType", ctx.getEventType().getChannel() + ":" + ctx.getEventType().getCode());
		}
		// 對於 SmartRobot， postback 類的 eventData 放在 ctx.requestAttr.eventData 當中
		// 裡頭應該是 URI query 形式（ param1=value1&param2=value2... 這樣的格式）
		// 對於系統內建要處理的目前主要是要抓選單類的按鈕，過去按下去是直接當作 message 送出
		// 若希望能用 postback 抓住（例如 LINE 就可以這樣，不會看到送出的訊息）
		// 這裡就可以用 action=_message&message=[選單欲送出文字] 這樣的格式放入 postback 中
		if ("postback".equalsIgnoreCase(ctx.getEventType().getCode())) {
			ctx.setResponseAttribute("TriggeredByEventType_Channel", ctx.getEventType().getChannel());
			ctx.setResponseAttribute("TriggeredByEventType_Code", ctx.getEventType().getCode());
			
			JSONObject eventData = (JSONObject)ctx.getRequestAttribute("eventData");
			if (eventData != null) {
				try {
					if (ctx.getTenant().getEnableQAExplain()) {
						ctx.appendExplain("EventTypeData", eventData.toString(2));
					}
					String data = eventData.optString("data", eventData.optString("payload"));

					if (data != null) {
						ctx.setResponseAttribute("TriggeredByEventType_Data", eventData);
    					Map<String, List<String>> nvPair = splitQuery(data);
    					
    					if (nvPair != null) {
        					String action = nvPair.get("action").get(0);
        					if (StringUtils.startsWith(action, "_")) {
        						tryToTriggerEvent = false;
        						
        						if ("_message".equals(action)) {
        							String msg = nvPair.get("message").get(0);
        							ctx.setOriginalQuestion(msg);
        							ctx.setCurrentQuestion(msg);
        						}
        						else if ("_option".equals(action)) {
        							String optionStr = nvPair.get("option").get(0);
        							Option option = ctx.findOption(optionStr);
        							
        							if (option != null) {
        								ctx.setRequestAttribute(ChooseQAOptionRule.PRESET_OPTION_IN_REQ_ATTR, option);
        							}
        							else {
        								List<String> fallbackKids = nvPair.get("fallback");
        								
        								if (fallbackKids != null && fallbackKids.size() > 0) {
        									Long kid = new Long(fallbackKids.get(0));
        									QAUtil qautil = ctx.getQAUtil();
        									String msg = qautil.getStandardQ(kid);
        									ctx.setOriginalQuestion(msg);
        									ctx.setCurrentQuestion(msg);
        								}
        								else {
        									// Option not found, forget it and do nothing by default
        									return PostRuleCheckResult.DEFAULT_RETURN_RESULT;	
        								}
        							}
        								
        						}
        					}
    
        					// 正常由 QAUtil.parseRequest 來解析，但 postback 會把 es / est 存在 postback.data 中，因此在這裡 parse
        					if (nvPair.containsKey("es")) {
        						String eventSource = nvPair.get("es").get(0);
        						ctx.setEventSource(eventSource);
        					}
        					if (nvPair.containsKey("est")) {
        						String eventSourceType = nvPair.get("est").get(0);
        						ctx.setEventSourceType(eventSourceType);
        					}
    					}
    					else {
    						// postback 不符合參數格式，就把整個 postback data 當作問句
							ctx.setOriginalQuestion(data);
							ctx.setCurrentQuestion(data);
    					}
					}
					else {
						if (ctx.getTenant().getEnableQAExplain()) {
							ctx.appendExplain("Not triggered", "Event has not 'data' or 'payload' element.");
						}
					}
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		
		if (tryToTriggerEvent) {
			SolrDocumentList docs = qu.triggeredByEventType(ctx.getEventType(), ctx, 5);
			
			if (docs != null && docs.size() > 0) {
				ctx.setResponseAttribute("TriggeredByEventType_Channel", ctx.getEventType().getChannel());
				ctx.setResponseAttribute("TriggeredByEventType_Code", ctx.getEventType().getCode());
				QA newAnswer = new QA(docs.get(0));
				ctx.setQaAsAnswer(newAnswer);
				ctx.setQuestionType(QUESTION_TYPE.UNKNOWN);
				ctx.setHasDirectAnswer(true, ANSWER_TYPE.PROFESSIONAL);

				return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
			}
		}

		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

	public static Map<String, List<String>> splitQuery(String dataStr) throws UnsupportedEncodingException {
		if (StringUtils.indexOfAny(dataStr, "&=") == -1) return null;
		final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		final String[] pairs = dataStr.split("&");
		for (String pair : pairs) {
			final int idx = pair.indexOf("=");
			final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
			if (!query_pairs.containsKey(key)) {
				query_pairs.put(key, new LinkedList<String>());
			}
			final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
			query_pairs.get(key).add(value);
		}
		return query_pairs;
	}
	
	public static void main(String[] args) throws UnsupportedEncodingException {
		System.out.println(splitQuery("a=b&c=d"));
		System.out.println(splitQuery("abc"));
	}
}
