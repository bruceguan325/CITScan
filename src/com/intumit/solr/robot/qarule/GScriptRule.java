package com.intumit.solr.robot.qarule;

import groovy.lang.Binding;

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
import com.intumit.solr.robot.QAContext.QUESTION_TYPE;
import com.intumit.solr.robot.qadialog.GroovyUtil;
import com.intumit.solr.robot.QAUtil;

/**
 * 一個 Global groovy script filter，這裡可以在流程特定時點執行 script
 * 可能常用於特定的判斷邏輯。
 * 
 * @author herb
 */
public class GScriptRule implements PreQAMatchRule {

	private String script = null;

	public GScriptRule() {
		super();
	}

	public GScriptRule(Map<String, Object> configs) {
		super();
		init(configs);
	}

	@Override
	public void init(Map<String, Object> configs) {
		if (configs.containsKey("script")) {
			String script = (String)configs.get("script");
			
			if (StringUtils.trimToNull(script) != null) {
				this.script  = script;
			}
		}
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (script == null) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}
		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		try {
			Binding binding = new Binding();
			binding.setProperty("ctx", ctx);
	
			return (PostRuleCheckResult)GroovyUtil.runScript(binding, script);
		}
		catch (Exception continueAnyway) {
			continueAnyway.printStackTrace();
			return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
		}
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx,
			PostRuleCheckResult result) {
		return result;
	}
}
