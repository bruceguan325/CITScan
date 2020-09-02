package com.intumit.solr.robot.qaplugin;

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QASaver;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;

public class CustomQA extends QAPlugin {

	public static final String ID = "4";

	public static final String QA_PATTERN_MKEY_PARAM = "customQaPatternMkey";
	public static final String SUGGEST_QUESTIONS_PARAM = "customQaSuggest";
	public static final String SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_PARAM = "customQaSuggestWhenNoAnswerOnly";
	public static final String QA_PATTERN_KEY_INDEX_FIELD = "CUSTOM_QA_ID_s"; // Should be "CUSTOM_QA_PATTERN_MKEY_s", but keep it for backward compatibility
	public static final String SUGGEST_QUESTIONS_INDEX_FIELD = "CUSTOM_QA_SUGGEST_s";
	public static final String SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_INDEX_FIELD = "CUSTOM_QA_SUGGEST_NO_ANSWER_ONLY_b";

	CustomQA(String id, String name) {
		super(id, name);
	}

	@Override
	public boolean hasEditorPage() {
		return true;
	}

	@Override
	public String onEditorPageCreate(SolrDocument doc, HttpServletRequest req) {
		req.setAttribute("docId", doc.getFieldValue("id"));
		String customQaId = (String)doc.getFieldValue(QA_PATTERN_KEY_INDEX_FIELD);

		if (customQaId != null) {
			req.setAttribute(QA_PATTERN_MKEY_PARAM, customQaId);

/*			String suggest = (String)doc.getFieldValue(SUGGEST_QUESTIONS_INDEX_FIELD);
			if (suggest != null)
				req.setAttribute(SUGGEST_QUESTIONS_PARAM, suggest);

			Boolean suggestNoAnswerOnly = (Boolean)doc.getFieldValue(SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_INDEX_FIELD);
			if (suggestNoAnswerOnly != null)
				req.setAttribute(SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_PARAM, suggestNoAnswerOnly);*/
		}

		return "/wiseadm/qa-plugin/custom-qa-editor.jsp";
	}

	@Override
	public void onEditorPageSave(SolrInputDocument doc, HttpServletRequest req,
			boolean selected) {

		String customQaId = req.getParameter(QA_PATTERN_MKEY_PARAM);
		QASaver.setOrUpdateField(doc, QA_PATTERN_KEY_INDEX_FIELD, (!selected || customQaId == null)?null:customQaId);

		String suggest = StringUtils.trimToNull(req.getParameter(SUGGEST_QUESTIONS_PARAM));
		QASaver.setOrUpdateField(doc, SUGGEST_QUESTIONS_INDEX_FIELD, (!selected || customQaId == null)?null:suggest);

		String suggestNoAnswerOnly = StringUtils.trimToNull(req.getParameter(SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_PARAM));
		if (suggestNoAnswerOnly != null) {
			QASaver.setOrUpdateField(doc, SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_INDEX_FIELD, Boolean.TRUE);
		}
		else {
			QASaver.setOrUpdateField(doc, SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_INDEX_FIELD, Boolean.FALSE);
		}
	}

	@Override
	public String onAnswerPreview(String origAnswer, HttpServletRequest req) {
		String customQaId = req.getParameter(QA_PATTERN_MKEY_PARAM);
		if (customQaId != null) {
			req.setAttribute(QA_PATTERN_MKEY_PARAM, customQaId);

			String suggest = StringUtils.trimToNull(req.getParameter(SUGGEST_QUESTIONS_PARAM));
			if (suggest != null)
				req.setAttribute(SUGGEST_QUESTIONS_PARAM, suggest);

			String suggestNoAnswerOnly = StringUtils.trimToNull(req.getParameter(SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_PARAM));
			if (suggestNoAnswerOnly != null) {
				req.setAttribute(SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_PARAM, Boolean.TRUE);
			}
			else {
				req.setAttribute(SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_PARAM, Boolean.FALSE);
			}
		}
		return "/wiseadm/qa-plugin/custom-qa-preview.jsp";
	}

	@Override
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String originalQuestion, final QAContext qaCtx) {
		QA directAnswer = qaCtx.getCurrentQA();
		if (directAnswer != null) {
			String suggest = (String)directAnswer.getFieldValue(SUGGEST_QUESTIONS_INDEX_FIELD);
			List<String> lSuggest = new ArrayList<String>();

			if (suggest != null)
				lSuggest.addAll(QAUtil.parseMultiValue(suggest));

			int opqLen = lSuggest.size();
			if(opqLen > 0){
				String NL = qaCtx.isClientSupportHtml() ? "<br>" : "\n";

				OptionMenu menu = qaCtx.createOptionMenu(getId(), MenuView.ORDERED_LIST, directAnswer.getId(), MenuSelectionBehavior.NUMBER_OR_FUZZY_TITLE, NL, NL, false, "");

				qaCtx.setRequestAttribute("cdsData", lSuggest);

				for(int i=0; i<opqLen; i++){
					String question = StringUtils.trimToEmpty(lSuggest.get(i));
					String template = null;

					if (question.indexOf("//") != -1) {
						template = StringUtils.trim(StringUtils.substringAfter(question, "//"));
						question = StringUtils.trim(StringUtils.substringBefore(question, "//"));

						if (qaCtx.isClientSupportHtml()) {
							template = template.replaceAll("\\[", "<span class='text-success'>[");
							template = template.replaceAll("\\]", "]</span>");
						}
					}
					Long kid = (Long)directAnswer.getFieldValue("kid_l");

					String href = QAUtil.getContextPath() + "/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-" + kid;
					int optionNum = menu.addOption(ID, null, OptionAction.INPUT_TEXT, question, new ParsedOption(question, question, OptionAction.INPUT_TEXT), kid);

					/*if (qaCtx.isClientSupportHtml() && !qaCtx.isFromApp()) {
						String a = "&nbsp;&nbsp;<a href='" + href + "' target='" + QAUtil.ANSWER_LINK_TARGET + "'>(<bean:message key='num'/>" + kid + ")</a>";
						String clickake = "<span class='question-clickable' data-opnum='" + question + "'>" + question + "</span>";
						String span = NL + "<span class='other-questions-li'"
							+ " style='border-bottom: 0px; padding: 0; padding-left: 10px;'>"
							+ "{{F:ENTER}}<span class='question-cmd'>" + (optionNum) + "</span>." + clickake
							;
						writer.append(span);

						if (template != null) {
							writer.append(" <small>(句型： " + template + ")</small>");
						}

						if (qaCtx.getTenant().getEnableDebug())
							writer.append(a);

						writer.append("</span>");
					}
					else {
						writer.append(NL + "{{F:ENTER}}" + (optionNum) + "." + question);

						if (template != null) {
							writer.append(" (句型：" + template + ")");
						}
					}*/
				}

				/*if (qaCtx.isClientSupportHtml() && !qaCtx.isFromApp()) {
					writer.append("</span>");
				}
				else {
					writer.append(NL);
				}

				if (hasMLT) {
					qaCtx.appendAnswerText(writer.toString());
				}*/
				if (menu.optionsSize() > 0)
					qaCtx.addAndShowOptionMenu(menu);
			}
		}
		
		return null;
	}

	@Override
	public boolean shouldInterceptNextApi(QAContext req, JSONObject curData) {
		boolean should = false;
		return should;
	}

	@Override
	public PostRuleCheckResult onApiChoosedOption(QAContext qaCtx, QAContext.Option option) {
		String origQ = qaCtx.getCurrentQuestion();
		String newQ = option.getValue().toString();

		if (!StringUtils.equalsIgnoreCase(origQ, newQ) && StringUtils.isNotBlank(newQ)) {
			if (qaCtx.getTenant().getEnableQAExplain()) qaCtx.appendExplain(
					this.getClass().getName(),
					"Question replaced by [" + getName() + ".onApiQuestionIntercept] from [" + origQ + "] to [" + newQ + "]");
			qaCtx.setCurrentQuestion(newQ);
			qaCtx.setQuestionType(QAContext.QUESTION_TYPE.INTERCEPTED);
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	public boolean supportAnswerTemplate(){
		return true;
	}


	public static boolean isSuggestWhenNoAnswerOnly(SolrDocument qa) {
		Boolean noAnswerOnly = (Boolean)qa.getFirstValue(SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_INDEX_FIELD);

		if (noAnswerOnly != null)
			return noAnswerOnly.booleanValue();

		return false;
	}

}
