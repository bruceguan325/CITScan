package com.intumit.solr.robot.qarule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.ReconstructQuestionParameter;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.entity.QAEntityDictionary;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.intent.QAIntentDictionary;
import com.intumit.solr.robot.qadialog.Field;
import com.intumit.solr.robot.qadialog.RuleCheckResult;
import com.intumit.solr.robot.qadialog.RuleCheckResultStatus;

/**
 * 為了效能考量，先準備真的要比對 QA 的一些變數放在 requestAttr 當中
 * 避免之後每一個 rule 都要重新產生
 * 
 * @author herb
 */
public class PrepareForQAMatchingRule extends QAMatchingRule {

	@Override
	public void init(Map<String, Object> configs) {
	}

	@Override
	public PreRuleCheckResult onPreRuleCheck(QAContext ctx) {
		if (ctx.getCurrentQuestion() == null) {
			return PreRuleCheckResult.DEFAULT_SKIP_AND_CONTINUE_RESULT;
		}

		return PreRuleCheckResult.DEFAULT_NORMAL_RESULT;
	}

	@Override
	public PostRuleCheckResult checkRule(QAContext ctx) {
		ctx.addQuestionChangeListener(new Reconstruct4QuestionChangeListener());
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		return super.onPostRuleCheck(ctx, result);
	}

}
