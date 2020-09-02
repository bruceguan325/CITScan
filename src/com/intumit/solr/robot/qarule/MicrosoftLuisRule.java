package com.intumit.solr.robot.qarule;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.wink.json4j.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.MultiChannelAnswer;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qadialog.GroovyUtil;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeUtils;

import groovy.lang.Binding;

/**
 * 透過在 Rule 中設定 
   {
      enable: true,
      appId: XXXX-XXXX-XXXX-XXXX,
      subscrptionKey: XXXXX,
      endpointUri: XXXXXX, // ends with "/", but don't append appId
      matchMethod: 'INTENT_ENTITY_ONLY' or 'INTENT_TO_ESSENTIAL_KEYWORD' or 'INTENT_TO_STANDARD_QUESTION'
      tw2cnOnQuestion: true,
      cn2twOnResposne: false,
   }
 * @author herb
 *
 */
public class MicrosoftLuisRule extends QAMatchingRule {
	private static final Logger LOG = LoggerFactory.getLogger(MicrosoftLuisRule.class);
	
	public static class LuisConfig {
		boolean enable = false;
		String appId;
		String subscriptionKey;
		String endpointUri;
		String matchMethod;
		boolean tw2cnOnQuestion = true;
		boolean cn2twOnResponse = false;
		
		@Override
		public String toString() {
			return "LuisConfig [enable=" + enable + ", " + (appId != null ? "appId=" + appId + ", " : "")
					+ (subscriptionKey != null ? "subscriptionKey=" + subscriptionKey + ", " : "")
					+ (endpointUri != null ? "endpointUri=" + endpointUri + ", " : "") + (matchMethod != null ? "matchMethod=" + matchMethod + ", " : "")
					+ "tw2cnOnQuestion=" + tw2cnOnQuestion + ", cn2twOnResponse=" + cn2twOnResponse + "]";
		}
	}
	
	LuisConfig cfg = null;

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (ctx.isHasDirectAnswer()) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		if (ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED) && ctx.getAnswerText() != null) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		Tenant t = ctx.getTenant();

		if (cfg == null || !cfg.enable) {
			LOG.info("Luis config is null or disabled, continue to next rule...");
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		}

		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		JSONObject luisResp = callLuis(cfg, ctx.getCurrentQuestion());

		if (explainQA) {
			ctx.appendExplain("callLuisResp", luisResp);
		}
		
		if (luisResp == null) {
			LOG.info("Luis response is null, continue to next rule...");
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		}
		
		String topIntentStr = luisResp.optJSONObject("topScoringIntent", new JSONObject()).optString("intent");
		if (StringUtils.isEmpty(topIntentStr)) {
			LOG.info("No top scoring intent, continue to next rule...");
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		}
		SolrDocument directAnswer = null;
		
		if ("INTENT_TO_STANDARD_QUESTION".equals(cfg.matchMethod)) {
			ctx.setCurrentQuestion(topIntentStr);
			directAnswer = ctx.getQAUtil().searchDirectAnswer(topIntentStr, ctx);
		}
		else if ("INTENT_TO_ESSENTIAL_KEYWORD".equals(cfg.matchMethod)) {
			List<String> ekws = Arrays.asList(new String[] {topIntentStr});
			directAnswer = ctx.getQAUtil().searchDirectAnswerByEssentialKeyword(ekws, ctx);

			if (explainQA) {
				ctx.appendExplain("qu.essentialKeywordAnswer", directAnswer);
			}
		}
		else if ("INTENT_ENTITY_ONLY".equals(cfg.matchMethod)) {
			// Not Implement Yet
		}

		if (directAnswer != null) {
			String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";
			// 使 context 記住本次回答的主體
			ctx.setCurrentQA(new QA(directAnswer));

			if (explainQA) {
				ctx.appendExplain("[respDoc != null] so we have a QADoc answer", directAnswer);
			}

			String answer = "";
			String customScript = (String) directAnswer.getFieldValue("CUSTOM_SCRIPT_s");
			if (StringUtils.isNotEmpty(customScript)) {
				Binding binding = new Binding();
				binding.setProperty("ctx", ctx);
				binding.setProperty("currentQA", directAnswer);
				answer = (String) GroovyUtil.runScript(binding, customScript);
			}

			if (StringUtils.isNotEmpty(answer) && StringUtils.endsWith(answer, "{{...}}}")) {
				ctx.setAnswerText(StringUtils.substringBeforeLast(answer, "{{{...}}}"));
				answer = "";
			}

			if (StringUtils.isEmpty(answer)) {
				if (StringUtils.trimToNull(ctx.getQaChannel()) != null) {
					answer = MultiChannelAnswer.qaRuleGetAnswer(ctx,
							(String) directAnswer.getFieldValue("id"), ctx.getQAChannelInstance(), ctx.getUserType());
				}

				if (StringUtils.isEmpty(answer)) {
					answer = (String) directAnswer.getFieldValue("ANSWER_s");
				}
			}

			if (ctx.hasAnswerText()) {
				ctx.appendAnswerText(NL);
				ctx.appendAnswerText(answer);
			}
			else {
				ctx.appendAnswerText(answer);
			}

			if (StringUtils.isNotEmpty(answer)) {
				ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
			}
			ctx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
			// end

			ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);

			return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
	
	public MicrosoftLuisRule() {
		super();
	}

	public MicrosoftLuisRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		cfg = new LuisConfig();

		cfg.appId = (String)configs.get("appId");
		cfg.subscriptionKey = (String)configs.get("subscriptionKey");
		cfg.endpointUri = (String)configs.get("endpointUri");
		
		if (configs.containsKey("enable")) {
			cfg.enable = ((Boolean)configs.get("enable")).booleanValue();
		}
		if (configs.containsKey("tw2cnOnQuestion")) {
			cfg.tw2cnOnQuestion = ((Boolean)configs.get("tw2cnOnQuestion")).booleanValue();
		}
		if (configs.containsKey("cn2twOnResponse")) {
			cfg.cn2twOnResponse = ((Boolean)configs.get("cn2twOnResponse")).booleanValue();
		}
		if (configs.containsKey("matchMethod")) {
			cfg.matchMethod = (String)configs.get("matchMethod");
		}
		else {
			cfg.matchMethod = "INTENT_TO_STANDARD_QUESTION";
		}
	}

	public JSONObject callLuis(LuisConfig cfg, String question) {
		HttpClient httpclient = HttpClients.createDefault();

		try {
			if (cfg.tw2cnOnQuestion) {
				question = WiSeUtils.tw2cn(question);
			}
			if (LOG.isDebugEnabled()) {
				LOG.info("LUIS request: " + cfg.toString());
				LOG.info("		QUESTION:" + question);
			}
			else {
				LOG.info("LUIS request (" + question + "), " + cfg.toString());
			}
			URIBuilder builder = new URIBuilder(cfg.endpointUri + cfg.appId + "?");

			builder.setParameter("q", question);
			builder.setParameter("timezoneOffset", "+8");	// Should be configurable?
			builder.setParameter("verbose", "false");  		// Should be configurable?
			builder.setParameter("spellCheck", "false");
			builder.setParameter("staging", "false");
			//builder.setParameter("subscription-key", cfg.subscriptionKey);

			URI uri = builder.build();
			
			HttpGet request = new HttpGet(uri);
			request.setHeader("Ocp-Apim-Subscription-Key", cfg.subscriptionKey);

			HttpResponse response = httpclient.execute(request);
			HttpEntity entity = response.getEntity();

			if (entity != null) {
				String respStr = EntityUtils.toString(entity);
				if (cfg.cn2twOnResponse) {
					respStr = WiSeUtils.cn2tw(respStr);
				}
				JSONObject resultJson = new JSONObject(respStr);

				if (LOG.isDebugEnabled()) {
					LOG.info("LUIS response: " + cfg.toString());
					LOG.info("		QUESTION:" + question);
					LOG.info("		RESPONSE:" + resultJson.toString(2));
				}
				else {
					LOG.info("LUIS response:" + respStr);
				}
				return resultJson;
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			//e.printStackTrace();

			if (LOG.isDebugEnabled()) {
				LOG.info("LUIS response: " + cfg.toString());
				LOG.info("		QUESTION:" + question);
				LOG.info("		RESPONSE ERROR:", e);
			}
			else {
				LOG.info("		RESPONSE ERROR:" + e.getMessage());
			}
		}
		
		return null;
	}

/* sample JSON response from LUIS
{
  "query": "forward to frank 30 dollars through HSBC",
  "topScoringIntent": {
    "intent": "give",
    "score": 0.3964121
  },
  "entities": [
    {
      "entity": "30",
      "type": "builtin.number",
      "startIndex": 17,
      "endIndex": 18,
      "resolution": {
        "value": "30"
      }
    },
    {
      "entity": "frank",
      "type": "frank",
      "startIndex": 11,
      "endIndex": 15,
      "score": 0.935219169
    },
    {
      "entity": "30 dollars",
      "type": "builtin.currency",
      "startIndex": 17,
      "endIndex": 26,
      "resolution": {
        "unit": "Dollar",
        "value": "30"
      }
    },
    {
      "entity": "hsbc",
      "type": "Bank",
      "startIndex": 36,
      "endIndex": 39,
      "resolution": {
        "values": [
          "BankeName"
        ]
      }
    }
  ],
  "sentimentAnalysis": {
    "label": "neutral",
    "score": 0.5
  }
}

*/

}
