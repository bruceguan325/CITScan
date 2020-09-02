package com.intumit.solr.robot.qarule;

import java.util.Date;
import java.util.Map;

import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;

public class KmsDocExpireRule implements PostQAMatchRule {

	private static final long serialVersionUID = -9076781197512860459L;

	@Override
	public void init(Map<String, Object> configs) {

	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		QA directAnswer = ctx.getCurrentQA();
		if(directAnswer != null) {
			Date expire = (Date) directAnswer.getFieldValue("EXPIRE_dt");
			String NL = ctx.isClientSupportHtml() ? "<BR>" : "\n";
			boolean show = false;
			if (expire != null) {
			    Date now = new Date();
			    long day = (expire.getTime()-now.getTime()) / (24*60*60*1000);
			    show |= day < 0;
			}

			Boolean kmsRelateExpiredMemo = (Boolean)directAnswer.getIsKmsRelateExpiredMemo();
		    show |= (kmsRelateExpiredMemo != null && kmsRelateExpiredMemo);
		    if(show) {
		        ctx.prependAnswerText((ctx.isClientSupportHtml() ? "<span style='color:red'>" : "") + "{{F:ANSWER_EXPIRED_MESSAGE}}" + (ctx.isClientSupportHtml() ? "</span>" : "") + NL + NL);
		    }
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return result;
	}

}
