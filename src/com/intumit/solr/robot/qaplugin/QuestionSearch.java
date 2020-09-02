package com.intumit.solr.robot.qaplugin;

import java.io.StringWriter;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;

public class QuestionSearch extends QAPlugin {

	static final String ID = "9";

	protected QuestionSearch(String id, String name) {
		super(id, name);
	}

	@Override
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String question, QAContext qaCtx) {

		if ((Boolean)qaCtx.getRequestAttribute("noRecommand", false)) {
			return null;
		}

		QA directAnswer = qaCtx.getCurrentQA();

		QAUtil qu = QAUtil.getInstance(qaCtx.getTenant());
		String[][] postTags = (String[][])qaCtx.getRequestAttribute("postTags");
		StringWriter writer = new StringWriter();
		String preCleaned = QAUtil.preReconstructClean(question);

		if (!qu.shouldTryingToFindAnswer(preCleaned, postTags, null) && !qaCtx.hasAnswerText() && !(qaCtx.getShowedOptionMenuSize() > 0)) {
			SolrDocumentList searched = qu.searchCommonSense(question, qaCtx, true, false, true, 0, 5);
			JSONArray qsData = new JSONArray();

			for (SolrDocument doc: searched) {
				if (directAnswer != null && directAnswer.getFieldValue("id").equals(doc.getFieldValue("id")))
					continue;

				if (((Float)doc.getFirstValue("score")) > qaCtx.getTenant().getThreshold3()/2 && !Boolean.TRUE.equals(doc.getFirstValue(QA.FN_IS_NOT_FOR_SEARCH))) {
					try {
						JSONObject qObj = new JSONObject();
						qObj.put("question", doc.getFirstValue("QUESTION_s"));
                        qObj.put("briefly_question", doc.getFirstValue("Briefly_QUESTION_s"));
						qObj.put("score", doc.getFirstValue("score"));
						qObj.put("kid", doc.getFirstValue("kid_l"));
						qsData.put(qObj);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}

			System.out.println("-----QuestionSearch:-----\n" + qsData.toString() + "\n-----END-----");
			
			if (qsData.length() > 0) {
				qaCtx.setRequestAttribute("qsData", qsData);
				qaCtx.setHasDirectAnswer(true, ANSWER_TYPE.SEARCH);

				String NL = qaCtx.isClientSupportHtml() ? "<br>" : "\n";
				String INDENT_OR_SPLITTER = qaCtx.isClientSupportHtml() ? "&nbsp;&nbsp;" : "  ";

				String header = null;
				if (qaCtx.hasAnswerText()) {
//					writer.append(NL);
//					writer.append(NL);
					header = NL + NL;
				}

				if (directAnswer != null) {
					header = "{{F:OR_YOU_CAN}}{{F:YOU_MAY_WANT_TO_KNOW}}" + NL;
				}
				else {
					if (qaCtx.isClientSupportHtml()) {
						//writer.append("{{F:SEARCH_TEXT_1}}" + forView.toString() + "{{F:SEARCH_TEXT_2_HTML}}");
						header = "{{F:SEARCH_TEXT_1}}" + question + "{{F:SEARCH_TEXT_2_HTML}}";
					}
					else {
						//writer.append("{{F:SEARCH_TEXT_1}}" + forView.toString() + "{{F:SEARCH_TEXT_2_PLAIN}}");
						header = "{{F:SEARCH_TEXT_1}}" + question + "{{F:SEARCH_TEXT_2_PLAIN}}";
					}
				}

				OptionMenu menu = qaCtx.createOptionMenu(getId(), MenuView.ORDERED_LIST, "_CLS_" + getClass().getName(), MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE, header + NL, NL, false, INDENT_OR_SPLITTER);
				
				for (int i=0; i<qsData.length(); i++) {
					try {
						JSONObject qObj = qsData.getJSONObject(i);
						String q = StringUtils.trimToEmpty(qObj.optString("question"));

						if (qaCtx.isDuplicateShowedOption(q, q) || menu.duplicateOption(q, q)) continue;

						Long kid = qObj.optLong("kid");
						int optionNum = menu.addOption(ID, null, OptionAction.INPUT_TEXT, q, new ParsedOption(q, q, OptionAction.INPUT_TEXT), kid);

						/*if (qaCtx.isClientSupportHtml() && !qaCtx.isFromApp()) {
							writer.append(NL + "{{F:ENTER}}<span class='question-cmd'>" + (optionNum) + "</span>.<span class='question-clickable other-questions-li' data-opnum='" + q + "'>"
								+ q + "</span>");

							if (qaCtx.getTenant().getEnableDebug()) {
								long kid = qObj.optLong("kid");
								String href = QAUtil.getContextPath() + "/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-" + kid;
								String a = "&nbsp;&nbsp;<a href='" + href + "' target='" + QAUtil.ANSWER_LINK_TARGET + "'>(<bean:message key='num'/>" + kid + ")</a>";
								writer.append(a);
							}
						}
						else {
							writer.append(NL + "{{F:ENTER}}" + (optionNum) + "." + q);
						}*/
					}
					catch (JSONException e) {
						e.printStackTrace();
					}
				}

				if (menu.optionsSize() > 0)
					qaCtx.addAndShowOptionMenu(menu);
				//qaCtx.appendAnswerText(writer.toString());
			}
		}
		
		return null;
	}

	@Override
	public PostRuleCheckResult onApiChoosedOption(QAContext qaCtx, QAContext.Option option) {
		String origQ = qaCtx.getCurrentQuestion();
		String newQ = option.getValue().toString();

		if (!StringUtils.equalsIgnoreCase(origQ, newQ) && StringUtils.isNotBlank(newQ)) {
			if (qaCtx.getTenant().getEnableDebug()) qaCtx.appendExplain(
					this.getClass().getName(),
					"Question replaced by [" + getName() + ".onApiQuestionIntercept] from [" + origQ + "] to [" + newQ + "]");
			qaCtx.setCurrentQuestion(newQ);
			qaCtx.setQuestionType(QAContext.QUESTION_TYPE.INTERCEPTED);
		}
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
}
