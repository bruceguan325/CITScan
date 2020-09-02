package com.intumit.solr.robot.qarule;

import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.QAContext;

public class ChineseFullToHalfConvertRule implements PreQAMatchRule {
	private static final Logger LOG = LoggerFactory
			.getLogger(ChineseFullToHalfConvertRule.class);

	/**
	 * 
	 * @param str
	 * @return 如果沒有轉換全形的話直接回傳 null
	 */
	public static String convert(String str) {
		boolean dirty = false;
		for (char c : str.toCharArray()) {
			str = str.replaceAll("　", " ");
			if ((int) c >= 65281 && (int) c <= 65374) {
				str = str.replace(c, (char) (((int) c) - 65248));
				dirty = true;
			}
		}
		
		try {
			Matcher m = ChineseNumberParsing.CHINESE_NUMBER_PATTERN.matcher(str);
			StringBuffer sb = null;
			
			while (m.find()) {
				if (sb == null) sb = new StringBuffer();
				String text = m.group();
				Number num = ChineseNumberParsing.parseLong(text);
				
				m.appendReplacement(sb, "" + num.longValue());
			}
			
			if (sb != null) {
				m.appendTail(sb);
				str = sb.toString();
				dirty = true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return dirty ? str : null;
	}

	public ChineseFullToHalfConvertRule() {
		super();
	}

	public ChineseFullToHalfConvertRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {

		String lastInputType = (String)ctx.getLastResponseAttribute("inputType", "string");
		
		if (StringUtils.equals(lastInputType, "password") 
				|| StringUtils.equals(lastInputType, "stringMask")) {
			
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(ctx.getCurrentQuestion()));
		String suggestQ = convert(tmpQ);

		if (suggestQ != null) {
			ctx.setCurrentQuestion(suggestQ);
			LOG.info("Chinese full to half sentence " + tmpQ + " => " + suggestQ);
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
		ChineseFullToHalfConvertRule r = new ChineseFullToHalfConvertRule();
		System.out.println(r.convert("十年前"));
		System.out.println(r.convert("二十三年前"));
		System.out.println(r.convert("一百三十五年前"));
		System.out.println(r.convert("１２３４５年ＡＢＣｄｅｆ前"));
	}
}
