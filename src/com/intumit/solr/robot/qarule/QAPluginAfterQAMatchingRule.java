package com.intumit.solr.robot.qarule;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.qaplugin.QAPlugin;
import com.intumit.solr.robot.qaplugin.QAPlugins;

/**
 * 用來跑 QAPlugin(s)
 * 這個目前很難寫 TestCase
 * 
 * @author herb
 *
 */
public class QAPluginAfterQAMatchingRule implements PostQAMatchRule {

	public QAPluginAfterQAMatchingRule() {
		super();
	}

	public QAPluginAfterQAMatchingRule(Map<String, Object> configs) {
		super();
		init(configs);
	}
	
	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		if (ctx.getQuestionType().equals(QAContext.QUESTION_TYPE.INTERCEPTED))
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		//String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";

		List<QAPlugin> plugins = QAPlugins.list();

		// 此區域為 QAPlugin.onApiAfterProfessionalIndexMatching()
		// 讓所有 QAPlugin 有機會 append 資料到 answer 當中
		for (QAPlugin p : plugins) {
			PostRuleCheckResult pres = p.onApiAfterProfessionalIndexMatching(ctx.getCurrentQuestion(), ctx);
		
			if (p.shouldInterceptNextApi(ctx, null)) {
				if (explainQA) {
					ctx.appendExplain("QAPlugins[" + p.getName() + ".shouldInterceptNextApi]  == true", p);
				}
				ctx.interceptNextQuestion(p, null);
			}
			
			if (pres != null)
				return pres;
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

}
