package com.intumit.solr.robot.qarule;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.util.WiSeUtils;

/**
 * 利用 Google Translate 翻譯使用者的 Question 
 * 翻譯到 targetLocale 後用來比對各種問法（通常這個會是 zh-TW）
 * 
 * config 還可以指定 forceTranslate（每一個 Question 都轉，無論 QAContext.locale 是啥）
 * 如果 forceTranslate == true，可以有 sourceLocale，代表無視 QAContext.locale 一律把使用者的 Q 視為 sourceLocale 語系
 * 若 forceTranslate == true，卻沒有 sourceLocale，則會使用 GoogleTranslate 的自動猜測語系（不是很準）
 * 
 * @author herb
 */
public class GoogleTranslateQuestionRule implements PreQAMatchRule {
	String apikey = null;
	boolean forceTranslate = false;
	String targetLocale = "zh-TW";
	String sourceLocale = null;

	public GoogleTranslateQuestionRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("apikey")) {
			apikey = (String)configs.get("apikey");
		}
		if (configs.containsKey("forceTranslate")) {
			forceTranslate = (Boolean)configs.get("forceTranslate");
		}
		if (configs.containsKey("targetLocale")) {
			targetLocale = (String)configs.get("targetLocale");
		}
		if (configs.containsKey("sourceLocale")) {
			sourceLocale = (String)configs.get("sourceLocale");
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		try {
			String source = null;
			if (forceTranslate) {
				if (sourceLocale != null) {
					source = sourceLocale;
				}
			}
			else {
				String currentLocale = HitHotLocale.toGoogleLang(ctx.getLocale());
				
				// 跟目前 locale 一樣就不翻譯
				if (StringUtils.equalsIgnoreCase(targetLocale, currentLocale)) {
					return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
				}
				else {
					source = currentLocale;
				}
			}
			String translateUrl = "https://www.googleapis.com/language/translate/v2?key=" + apikey
					+ "&q=" + java.net.URLEncoder.encode(ctx.getCurrentQuestion(), "UTF-8") 
					+ (source != null ? "&source=" + source : "") // 沒有給 source ，Google 會自動 guess
					+ "&target=" + targetLocale;
			
			String tResult = WiSeUtils.getDataFromUrl(translateUrl);
			JSONObject tJson = new JSONObject(tResult);
			String tText = tJson.getJSONObject("data").getJSONArray("translations").getJSONObject(0).getString("translatedText");
			
			if (StringUtils.trimToNull(tText) != null && !StringUtils.equalsIgnoreCase(ctx.getCurrentQuestion(), tText)) {
				ctx.setCurrentQuestion(tText);
			}
		}
		catch (Exception ignore) {
			ignore.printStackTrace();
		}
		
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

}
