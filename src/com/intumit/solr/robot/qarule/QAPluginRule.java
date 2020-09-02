package com.intumit.solr.robot.qarule;

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
public class QAPluginRule implements PreQAMatchRule {
	
	public static enum InterceptMode {
		QUESTION,
		ANSWER,
		BOTH
	}
	
	InterceptMode mode = InterceptMode.BOTH;

	public QAPluginRule() {
		super();
	}

	public QAPluginRule(Map<String, Object> configs) {
		super();
		init(configs);
	}
	
	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("mode")) {
			mode = (InterceptMode)configs.get("mode");
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		Set<String> interceptorIds = ctx.getInterceptedPluginIds();
		if(ctx.isFromKeep()){
			interceptorIds = ctx.getInterceptedPluginIdsKeep();
		}
		String interceptedAnswer = "";
		boolean explainQA = ctx.getTenant().getEnableQAExplain();
		String NL = ctx.isClientSupportHtml() ? "<br>" : "\n";

		for (String pId : interceptorIds) {
			QAPlugin p = QAPlugins.get(pId);
			if (explainQA) ctx.appendExplain("Trigger Plugin (" + mode + ")", p);
		
			JSONObject pluginData = ctx.getInterceptedPluginData(pId);
			if(ctx.isFromKeep()){
				pluginData = ctx.getInterceptedPluginDataKeep(pId);
			}
			
			if (mode == InterceptMode.QUESTION || mode == InterceptMode.BOTH) {
				String newQuestion = StringUtils.defaultString(StringUtils.trim(p.onApiQuestionIntercept(ctx, ctx.getCurrentQuestion(), pluginData)));
				if (!StringUtils.equalsIgnoreCase(newQuestion, ctx.getCurrentQuestion()) && StringUtils.isNotBlank(newQuestion)) {
					if (explainQA) ctx.appendExplain("Question replaced by [" + p.getName() + ".onApiQuestionIntercept]", "[" + ctx.getCurrentQuestion() + "] to [" + newQuestion + "]");
					ctx.setCurrentQuestion(newQuestion);
					ctx.setQuestionType(QAContext.QUESTION_TYPE.INTERCEPTED);
				}
			}
			if (mode == InterceptMode.ANSWER || mode == InterceptMode.BOTH) {
				String answer = StringUtils.trim(p.onApiAnswerIntercept(ctx, pluginData));
				if (StringUtils.isNotBlank(answer)) {
					if (explainQA) ctx.appendExplain("Answer by [" + p.getName() + ".onApiAnswerIntercept]", "[" + answer + "]");
			
					// @TODO: 應該改成每個 QAPlugin 自行修改 Answer，這裡不用替他們做 append。
					if (StringUtils.isNotBlank(interceptedAnswer)) {
						interceptedAnswer += NL + NL;
					}
					interceptedAnswer += answer;
					ctx.setAnswerText(interceptedAnswer);
				}
			}
			
			if (p.shouldInterceptNextApi(ctx, pluginData)) {
				if (explainQA) ctx.appendExplain("[" + p.getName() + ".shouldInterceptNextApi] pluginData[" + pluginData + "]", Boolean.TRUE);
				ctx.interceptNextQuestion(p, pluginData);
			}
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}

}
