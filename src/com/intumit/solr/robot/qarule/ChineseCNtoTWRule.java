package com.intumit.solr.robot.qarule;

import java.util.Map;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.util.WiSeUtils;

/**
 * 繁簡轉換的 Rule
 * 
 * @author herb
 */
public class ChineseCNtoTWRule implements PreQAMatchRule {
	private static final Logger LOG = LoggerFactory
			.getLogger(ChineseCNtoTWRule.class);

	private String convert;

	public ChineseCNtoTWRule() {
		super();
	}

	public ChineseCNtoTWRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {

		if (configs.containsKey("convert")) {
			convert = StringUtils.defaultString(StringUtils.trimToNull((String)configs.get("convert")),
					"none"
					);
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (StringUtils.equals(convert, "none")) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		String tmpQ = StringUtils.lowerCase(StringUtils.trimToEmpty(ctx.getCurrentQuestion()));
		String suggestQ = convert(tmpQ);
		
		if (!StringUtils.equals(tmpQ, suggestQ)) {
			ctx.setCurrentQuestion(suggestQ);
			LOG.info("Chinese " + convert + " " + tmpQ + " => " + suggestQ);
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	private String convert(String tmpQ) {
		if ("cn2tw".equals(convert)) {
			tmpQ = WiSeUtils.cn2tw(tmpQ);
		}
		else if ("tw2cn".equals(convert)) {
			tmpQ = WiSeUtils.tw2cn(tmpQ);
		}
		return tmpQ;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		// Do nothing
		return result;
	}
	
	public static void main(String[] args) {
		ChineseCNtoTWRule r = new ChineseCNtoTWRule();
		r.convert = "cn2tw";
		System.out.println(r.convert("找操作手册"));  // 應為「冊」
		System.out.println(r.convert("搜寻关连关键字"));
	}
}
