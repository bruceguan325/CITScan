package com.intumit.solr.robot.qaplugin;

import org.apache.commons.lang.BooleanUtils;
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
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.OtherPossibleQuestionsBySearchDictionary;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.util.WiSeUtils;

class OtherPossibleQuestions extends QAPlugin {

	static final String ID = "8";

	OtherPossibleQuestions(String id, String name) {
		super(id, name);
	}
	
	@Override
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String origQuestion, final QAContext qaCtx) {

		if ((Boolean)qaCtx.getRequestAttribute("noRecommand", false)) {
			return null;
		}
		
		QA directAnswer = qaCtx.getCurrentQA();
		
		if (directAnswer == null || (directAnswer != null && !BooleanUtils.isNotFalse((Boolean)directAnswer.getFieldValue(QA.FN_NOT_APPEND_MLT))) ) {
			String preCleaned = QAUtil.preReconstructClean(origQuestion);
			
			DictionaryDatabase[] kws = OtherPossibleQuestionsBySearchDictionary.search(qaCtx.getTenant().getId(), preCleaned.toCharArray());
			StringBuffer sb = new StringBuffer();
			StringBuffer forView = new StringBuffer();
			
			for (DictionaryDatabase kw: kws) {
				if (sb.length() > 0) {
					sb.append(" ");
					forView.append("、");
				}
				
				sb.append(WiSeUtils.dblQuote(kw.getKeyword()));
				forView.append(kw.getKeyword());
			}

			String sbStr = StringUtils.trimToNull(sb.toString());
			JSONArray dsData = new JSONArray();
			
			if (sbStr != null) {
				QAUtil qu = QAUtil.getInstance(qaCtx.getTenant());
				SolrDocumentList searched = qu.searchCommonSense(sbStr, qaCtx, true, false, false, 0, qaCtx.getTenant().getOtherMltQANum());
	
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
			}
			
//			dsData = otherPossibleQuestions;
			JSONArray otherPossibleQuestions = (JSONArray) qaCtx.getRequestAttribute("otherPossibleQuestions");
			if (otherPossibleQuestions != null) dsData.addAll(otherPossibleQuestions);
			
			System.out.println("-----OtherPossibleQuestions:-----\n" + otherPossibleQuestions + "\n-----END-----");
			
			int opqLen = dsData.length();
			if(opqLen > 0){
				String NL = qaCtx.isClientSupportHtml() ? "<br>" : "\n";
				String INDENT_OR_SPLITTER = qaCtx.isClientSupportHtml() ? "&nbsp;&nbsp;" : "  ";
				boolean hasAnswerText = qaCtx.hasAnswerText();
				String header = "";
				if (hasAnswerText) {
					header = NL + NL;
				}
				
				if (hasAnswerText)  {
					if(qaCtx.getQaChannel().equals("webline")) {
						header += "{{F:YOU_MAY_WANT_TO_KNOW_BY_WEBLINE}}";
					}else{
						header += "{{F:OR_YOU_CAN}}{{F:YOU_MAY_WANT_TO_KNOW}}";
					}
				}else{
					if(qaCtx.getQaChannel().equals("webline")) {
						header += "{{F:YOU_MAY_WANT_TO_KNOW_BY_WEBLINE}}";
					}else{
						header += "{{F:YOU_MAY_WANT_TO_KNOW}}";
					}
					qaCtx.setAnswerType(ANSWER_TYPE.MLT_ONLY);
				}
				
				header += NL;
				
				OptionMenu menu = qaCtx.createOptionMenu(getId(), MenuView.ORDERED_LIST, "_CLS_" + getClass().getName(), MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE, header, NL, false, INDENT_OR_SPLITTER);
				qaCtx.setRequestAttribute("opqData", dsData);
				
				boolean hasMLT = false;
				
				for(int i=0; i<opqLen && i < qaCtx.getTenant().getOtherMltQANum(); i++){
					try {
						JSONObject qObj = dsData.getJSONObject(i);
						String question = StringUtils.trimToEmpty(qObj.optString("question"));
						String brieflyQuestion = StringUtils.trimToEmpty(qObj.optString("briefly_question"));
						Long kid = qObj.optLong("kid");

						if (directAnswer != null && directAnswer.getFieldValue("kid_l").equals(kid))
							continue;
						
						if (qaCtx.isDuplicateShowedOption(question, question) || menu.duplicateOption(question, question)) continue;
						menu.addOption(ID, null, OptionAction.REDIRECT_TO_QUESTION, brieflyQuestion, new ParsedOption(question, question, OptionAction.REDIRECT_TO_QUESTION,QAUtil.kid2Id(kid)), kid);
					}
					catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				if (menu.optionsSize() > 0)
					qaCtx.addAndShowOptionMenu(menu);
			}
		}
		
		return null;
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
}
