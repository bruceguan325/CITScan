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
import com.intumit.solr.robot.dictionary.OtherPossibleQuestionsBySearchDictionary;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.util.WiSeUtils;

public class DummyOptionsQAPlugin extends QAPlugin {
	public final static String ID = "0";

	protected DummyOptionsQAPlugin(String id, String name) {
		super(id, name);
	}
	
	@Override
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String question, QAContext qaCtx) {

		String NL = qaCtx.isClientSupportHtml() ? "<br>" : "\n";
		OptionMenu menu = qaCtx.createOptionMenu(getId(), MenuView.ORDERED_LIST, "_CLS_" + getClass().getName(), MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE, NL, NL, false, "");
		int optionNum = menu.addOption(ID, null, OptionAction.INPUT_TEXT, question, new ParsedOption(question, question, OptionAction.INPUT_TEXT), null);

		if (menu.optionsSize() > 0)
			qaCtx.addAndShowOptionMenu(menu);
		
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
