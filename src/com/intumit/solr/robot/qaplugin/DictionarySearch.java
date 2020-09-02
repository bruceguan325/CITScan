package com.intumit.solr.robot.qaplugin;

import java.io.StringWriter;
import java.util.HashSet;
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
import com.intumit.solr.robot.dictionary.OtherPossibleQuestionsBySearchDictionary;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.util.WiSeUtils;

public class DictionarySearch extends QAPlugin {

	public static final String ID = "11";

	protected DictionarySearch(String id, String name) {
		super(id, name);
	}

	@Override
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String question, QAContext qaCtx) {

		if ((Boolean)qaCtx.getRequestAttribute("noRecommand", false)) {
			return null;
		}
		
		QAUtil qu = QAUtil.getInstance(qaCtx.getTenant());
		String preCleaned = QAUtil.preReconstructClean(question);

		if (!qaCtx.hasAnswerText() && !(qaCtx.getShowedOptionMenuSize() > 0)) {

			QA directAnswer = qaCtx.getCurrentQA();

			// DictionarySearch 如果在有 direct answer 的情況下，要判斷該問答給不給附加答案
			if (directAnswer == null || !Boolean.TRUE.equals(directAnswer.getFieldValue(QA.FN_NOT_APPEND_MLT))) {
				DictionaryDatabase[] kws = OtherPossibleQuestionsBySearchDictionary.search(qaCtx.getTenant().getId(), preCleaned.toCharArray());
				StringBuffer sb = new StringBuffer();
				StringBuffer forView = new StringBuffer();
				Set<String> dup = new HashSet<>();
	
				for (DictionaryDatabase kw: kws) {
					String key = kw.getKeyword();
					
					if (sb.length() > 0) {
						sb.append(" ");
					}
					sb.append(WiSeUtils.dblQuote(key));
					
					if (dup.contains(key)) {
						continue;
					}
					
					if (forView.length() > 0) {
						forView.append("、");
					}
	
					forView.append(key);
					dup.add(key);
				}
	
				String sbStr = StringUtils.trimToNull(sb.toString());
	
				if (sbStr != null) {
					SolrDocumentList searched = qu.searchCommonSense(sbStr, qaCtx, true, false, false, 0, 5);
					JSONArray dsData = new JSONArray();
	
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
								dsData.put(qObj);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
					
					System.out.println("-----DictionarySearch:-----\n" + dsData.toString() + "\n-----END-----");
	
					if (dsData.length() > 0) {
						qaCtx.setRequestAttribute("dsData", dsData);
						qaCtx.setHasDirectAnswer(true, ANSWER_TYPE.SEARCH);
						String NL = qaCtx.isClientSupportHtml() ? "<br>" : "\n";
						String INDENT_OR_SPLITTER = qaCtx.isClientSupportHtml() ? "&nbsp;&nbsp;" : "  ";
	
						String header = null;
						if (qaCtx.hasAnswerText()) {
	//						writer.append(NL);
	//						writer.append(NL);
							header = NL + NL;
						}
	
						if (directAnswer != null) {
							header = "{{F:OR_YOU_CAN}}{{F:YOU_MAY_WANT_TO_KNOW}}";
						}
						else {
							if (qaCtx.isClientSupportHtml()) {
								//writer.append("{{F:SEARCH_TEXT_1}}" + forView.toString() + "{{F:SEARCH_TEXT_2_HTML}}");
								header = "{{F:SEARCH_TEXT_1}}" + forView.toString() + "{{F:SEARCH_TEXT_2_HTML}}";
							}
							else {
								//writer.append("{{F:SEARCH_TEXT_1}}" + forView.toString() + "{{F:SEARCH_TEXT_2_PLAIN}}");
								header = "{{F:SEARCH_TEXT_1}}" + forView.toString() + "{{F:SEARCH_TEXT_2_PLAIN}}";
							}
						}
	
						OptionMenu menu = qaCtx.createOptionMenu(getId(), MenuView.ORDERED_LIST, "_CLS_" + getClass().getName(), MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE, header + NL, NL, false, INDENT_OR_SPLITTER);
	
						for (int i=0; i < dsData.length() && i < qaCtx.getTenant().getOtherMltQANum(); i++) {
							try {
								JSONObject qObj = dsData.getJSONObject(i);
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
