package com.intumit.solr.robot.qarule;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.qadialog.QADialog;
import com.intumit.solr.robot.qarule.ForwardToCrmRule.Forward;

/**
 * 
 * 
 * @author herb
 * 
 */
public abstract class QAMatchingRule implements QARule {

	@Override
	public PostRuleCheckResult onPostRuleCheck(QAContext ctx, PostRuleCheckResult result) {
		// 檢查答案文字是否是要轉接的特殊文案
		if (ctx.hasAnswerText()) {
			String answerText = ctx.getAnswerText().toString();
			QAUtil.CrmRedirectTag redirectTag = QAUtil.CrmRedirectTag.match(answerText);
			if (redirectTag != null) {
				if (ctx.getTenant().getEnableQAExplain()) ctx.appendExplain(
						"CrmRedirectTag after answer prepared [before PlugIn answer append].", redirectTag);
				ctx.setRequestAttribute(ForwardToCrmRule.REQ_ATTR_FORCE_CRM_FORWARD, redirectTag.forward);
			}
			else {
			}

			String codeAndVal = TemplateUtil.find(answerText, new QADialog.DlgSpCodeFinder(ctx));

			if (codeAndVal != null) {
				ctx.setAnswerText("");
				ctx.setRequestAttribute(QADialog.DIGSP + StringUtils.substringBefore(codeAndVal, ":"), StringUtils.substringAfter(codeAndVal, ":"));
				return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_DIALOG_CHAIN);
			}
		}

		if (ctx.getRequestAttribute(ForwardToCrmRule.REQ_ATTR_FORCE_CRM_FORWARD) != null
				&& ctx.getTenant().getEnableTextCrm()) {
			Forward forward = (Forward) ctx.getRequestAttribute(ForwardToCrmRule.REQ_ATTR_FORCE_CRM_FORWARD);
			ctx.setAnswerType(QAContext.ANSWER_TYPE.FORWARD);

			if (ctx.getTenant().getEnableQAExplain()) {
				ctx.appendExplain("Add force forward info", forward);
			}

			switch (forward) {
				case PERSONNEL:
					ForwardToCrmRule.CrmRedirectInfo rInfo = ForwardToCrmRule.redirectToCRM(ctx);
					if (!rInfo.forwardDirectly) {
						ctx.setRequestAttribute("listenOffHourCmd", !rInfo.isDepartmentOnline);
						ctx.setRequestAttribute("listenWaitingCmd", rInfo.isDepartmentOnline);
					}
					ctx.setAnswerText(rInfo.output);
					if (rInfo.forwardDirectly) {
						ctx.setResponseAttribute("forward", Forward.PERSONNEL.name());
					}
					ctx.setForwardDep(rInfo.forwardDep);
					break;
				default:
					ctx.setResponseAttribute("forward", forward.name());
			}

			// 要轉就直接結束
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		return result;
	}

}
