package com.intumit.solr.robot.qarule;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.android.search.FuzzyLevel;
import com.intumit.android.search.fuzzy.PhoneticSimilarity;
import com.intumit.solr.robot.QAChannel;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.wivo.WiVoUtil;

public class WiVoSuggestRule implements PreQAMatchRule {
	private static final Logger LOG = LoggerFactory.getLogger(WiVoSuggestRule.class);
	boolean enableAdvancedWiVo = false;
	boolean enableNoToneMode = false;
	FuzzyLevel fuzzyLevel = FuzzyLevel.NOFUZZY;
	PhoneticSimilarity phoneticSimilarity = PhoneticSimilarity.FLEXIBLE;
	
	public WiVoSuggestRule() {
		super();
	}

	public WiVoSuggestRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("enableAdvancedWiVo")) {
			enableAdvancedWiVo = ((Boolean)configs.get("enableAdvancedWiVo")).booleanValue();
		}
		if (configs.containsKey("enableNoToneMode")) {
			enableNoToneMode = ((Boolean)configs.get("enableNoToneMode")).booleanValue();
		}
		else if (configs.containsKey("fuzzyLevel")) {
			fuzzyLevel = FuzzyLevel.valueOf((String)configs.get("fuzzyLevel"));
		}
		else if (configs.containsKey("phoneticSimilarity")) {
			phoneticSimilarity = PhoneticSimilarity.valueOf((String)configs.get("phoneticSimilarity"));
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (!ctx.getTenant().getEnablePhoneticHomonym()) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}
	
	/**
	 * 
	 * @param tenantId
	 * @param channelCode 可以為 null
	 * @param question
	 * @return
	 */
	public String suggest(int tenantId, String channelCode, String question) {
		String suggestQ = null;

		if (enableAdvancedWiVo) {
			WiVoUtil wv = WiVoUtil.getInstance(tenantId);
			
			if (wv == null || wv.getFuzzyLevel() != fuzzyLevel || wv.getPhoneticSimilarity() != phoneticSimilarity || wv.isEnableNoTone() != enableNoToneMode) {
				wv = WiVoUtil.createNewInstance(tenantId, fuzzyLevel, phoneticSimilarity, enableNoToneMode);
			}
			
			if (wv != null) {
				suggestQ = wv.suggestFullQuestion(question, channelCode != null ? channelCode : null);
			}
			else {
				LOG.info("For some unknown reason, we cannot get WiVoUtil instance.");
			}
		}
		else {
			suggestQ = com.intumit.solr.robot.WiVoUtil.suggestFullQuestion(tenantId, question);
		}
		
		return suggestQ;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(ctx.getCurrentQuestion()));

		QAChannel channel = ctx.getQAChannelInstance();
		int tenantId = ctx.getTenant().getId();
		
		String suggestQ = suggest(tenantId, channel != null ? channel.getCode() : null, tmpQ);
						
		if (suggestQ != null) {
			ctx.setCurrentQuestion(suggestQ);
			LOG.info("WiVO adjust sentence " + tmpQ + " => " + suggestQ);
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		// Do nothing
		return result;
	}

	public boolean isEnableAdvancedWiVo() {
		return enableAdvancedWiVo;
	}

	public FuzzyLevel getFuzzyLevel() {
		return fuzzyLevel;
	}

	public PhoneticSimilarity getPhoneticSimilarity() {
		return phoneticSimilarity;
	}

}

