package com.intumit.solr.robot.qarule;

import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.android.search.FuzzyLevel;
import com.intumit.solr.robot.KuromojiUtil;
import com.intumit.solr.robot.QAContext;
import com.mariten.kanatools.KanaConverter;

public class JapaneseKataAndHiraHalfToFullConvertRule implements PreQAMatchRule {
	private static final Logger LOG = LoggerFactory
			.getLogger(JapaneseKataAndHiraHalfToFullConvertRule.class);
	
	KuromojiUtil util = new KuromojiUtil();
	boolean enableKataAndHira = false;
	String asciiRule = "DONOTHING";
	Integer op = null;

	public JapaneseKataAndHiraHalfToFullConvertRule() {
		super();
	}

	public JapaneseKataAndHiraHalfToFullConvertRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("enableKataAndHira")) {
			enableKataAndHira = ((Boolean)configs.get("enableKataAndHira")).booleanValue();
		}
		else if (configs.containsKey("enableAsciiConvert")) {
			asciiRule = (String)configs.get("enableAsciiConvert");
		}
		
		if (enableKataAndHira) {
			op = KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA | KanaConverter.OP_HAN_KATA_TO_ZEN_HIRA;
		}
		
		if ("TO_HALF".equals(asciiRule)) {
			if (op == null) {
				op = KanaConverter.OP_ZEN_ASCII_TO_HAN_ASCII;
			}
			else {
				op |= KanaConverter.OP_ZEN_ASCII_TO_HAN_ASCII;
			}
		}
		else if ("TO_FULL".equals(asciiRule)) {
			if (op == null) {
				op = KanaConverter.OP_HAN_ASCII_TO_ZEN_ASCII;
			}
			else {
				op |= KanaConverter.OP_HAN_ASCII_TO_ZEN_ASCII;
			}
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {

		String lastInputType = (String)ctx.getLastResponseAttribute("inputType", "string");
		
		if (op == null || StringUtils.equals(lastInputType, "password") 
				|| StringUtils.equals(lastInputType, "stringMask")) {
			
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(ctx.getCurrentQuestion()));
		String suggestQ = util.convert(tmpQ, op);

		if (!StringUtils.equals(tmpQ, suggestQ)) {
			ctx.setCurrentQuestion(suggestQ);
			LOG.info(String.format("Japanese full to half sentence (kata and hira:%b, ascii: %s) %s => %s", enableKataAndHira, asciiRule, tmpQ, suggestQ));
		}

		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		// Do nothing
		return result;
	}
	
	public static void main(String[] args) {
		JapaneseKataAndHiraHalfToFullConvertRule r = new JapaneseKataAndHiraHalfToFullConvertRule();
		System.out.println(r.util.convert("ﾃｽﾃｨﾝｸﾞ　１−２−３", KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA | KanaConverter.OP_HAN_KATA_TO_ZEN_HIRA | KanaConverter.OP_ZEN_ASCII_TO_HAN_ASCII));
		System.out.println(r.util.convert("ﾃｽﾃｨﾝｸﾞ　１−２−３", KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA | KanaConverter.OP_HAN_KATA_TO_ZEN_HIRA));
		System.out.println(r.util.convertAsciiHanToZen("東京都北区赤羽６−３０−１　赤羽ﾋﾙｽﾞ"));
		System.out.println(r.util.convert("一百三十五年前", KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA | KanaConverter.OP_HAN_KATA_TO_ZEN_HIRA | KanaConverter.OP_ZEN_ASCII_TO_HAN_ASCII));
		System.out.println(r.util.convert("１２３４５年ＡＢＣｄｅｆ前", KanaConverter.OP_ZEN_ASCII_TO_HAN_ASCII));
	}
}
