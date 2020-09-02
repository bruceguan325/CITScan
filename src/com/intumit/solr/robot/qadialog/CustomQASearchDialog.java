package com.intumit.solr.robot.qadialog;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.config.ColumnNameMappingFacade;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QADataAggregator;
import com.intumit.solr.robot.QAOutputResult;
import com.intumit.solr.robot.QAPattern;
import com.intumit.solr.robot.QAPatternUtil;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.mchange.io.FileUtils;

import flexjson.JSONDeserializer;

public class CustomQASearchDialog extends QADialog {
	List<Trigger> enterTriggers = new ArrayList<Trigger>();
	List<Trigger> quitTriggers = new ArrayList<Trigger>();
	List<Trigger> normalTriggers = new ArrayList<Trigger>();
	String qaPatternMkey = null;
	String quitText = null;
	String showCurrentStatusWhenSearchingText = null;
	boolean showCurrentStatusWhenQuitting = false;
	boolean showCurrentStatusWhenSearching = false;
	boolean runPostMatchChain = false;
	boolean matchQaWhenNothingToDo = false;
	
	Integer currentFieldOffset = null;
	Field currentField = null;
	SolrDocumentList currentDocs = null;

	public CustomQASearchDialog() {
		
	}
	
	InnerStatus status = InnerStatus.INACTIVE;
	static enum InnerStatus {
		JUST_ACTIVATED,
		INACTIVE,
		ASK_QUIT_OR_NOT, 
		ASK_ENTER_OR_NOT, 
		IN_PROGRESS, 
		FIELD_DOUBLE_CONFIRM
	}
	
	@Override
	public void loadConfig() {
		super.loadConfig();
		try {
			quitText  = config.optString("quitText");
			qaPatternMkey  = config.optString("qaPatternMkey");
			runPostMatchChain = config.optBoolean("runPostMatchChain", Boolean.FALSE);
			matchQaWhenNothingToDo = config.optBoolean("matchQaWhenNothingToDo", Boolean.FALSE);
			showCurrentStatusWhenQuitting = config.optBoolean("showCurrentStatusWhenQuitting", Boolean.FALSE);
			showCurrentStatusWhenSearching = config.optBoolean("showCurrentStatusWhenSearching", Boolean.FALSE);
			showCurrentStatusWhenSearchingText  = config.optString("showCurrentStatusWhenSearchingText");
			
			JSONArray quitTriggerJsons = config.getJSONArray("quitTrigger");
			
			for (int i=0; i < quitTriggerJsons.length(); i++) {
				JSONObject trigger = quitTriggerJsons.getJSONObject(i);
				Trigger t = Trigger.createTrigger(trigger);
				
				if (t != null)
					quitTriggers.add(t);
			}
			
			JSONArray normalTriggerJson = config.getJSONArray("normalTrigger");
			
			for (int i=0; i < normalTriggerJson.length(); i++) {
				JSONObject trigger = normalTriggerJson.getJSONObject(i);
				Trigger t = Trigger.createTrigger(trigger);
				
				if (t != null)
					normalTriggers.add(t);
			}
			
			JSONArray fieldsJsons = config.getJSONArray("fields");
			
			for (int i=0; i < fieldsJsons.length(); i++) {
				JSONObject fieldJson = fieldsJsons.getJSONObject(i);
				Field f = (Field)new JSONDeserializer().use(null, Field.class).deserialize(fieldJson.toString());
				f.init();
				fields.add(f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Trigger> getQuitTriggers() {
		return quitTriggers;
	}

	public void setQuitTriggers(List<Trigger> quitTriggers) {
		this.quitTriggers = quitTriggers;
	}

	public List<Trigger> getNormalTriggers() {
		return normalTriggers;
	}

	public void setNormalTriggers(List<Trigger> normalTriggers) {
		this.normalTriggers = normalTriggers;
	}

	public SolrDocumentList getCurrentDocs() {
		return currentDocs;
	}

	public void setCurrentDocs(SolrDocumentList currentDocs) {
		this.currentDocs = currentDocs;
	}

	public String getQaPatternMkey() {
		return qaPatternMkey;
	}

	public void setQaPatternMkey(String qaPatternMkey) {
		this.qaPatternMkey = qaPatternMkey;
	}

	public String getQuitText() {
		return quitText;
	}

	public void setQuitText(String quitText) {
		this.quitText = quitText;
	}

	public boolean isShowCurrentStatusWhenQuitting() {
		return showCurrentStatusWhenQuitting;
	}

	public void setShowCurrentStatusWhenQuitting(
			boolean showCurrentStatusWhenQuitting) {
		this.showCurrentStatusWhenQuitting = showCurrentStatusWhenQuitting;
	}

	public boolean isRunPostMatchChain() {
		return runPostMatchChain;
	}

	public void setRunPostMatchChain(boolean runPostMatchChain) {
		this.runPostMatchChain = runPostMatchChain;
	}

	public boolean isMatchQaWhenNothingToDo() {
		return matchQaWhenNothingToDo;
	}

	public void setMatchQaWhenNothingToDo(boolean matchQaWhenNothingToDo) {
		this.matchQaWhenNothingToDo = matchQaWhenNothingToDo;
	}

	public PostRuleCheckResult innerCheck(QAContext ctx) {
		if (callStack.size() > 0) {
			CallStackData stackData = callStack.peek();
			PostRuleCheckResult r = stackData.to.check(ctx);
			if (!stackData.to.isActive()) {
				callStack.pop();
				PostRuleCheckResult prcr = returnFromDialog(stackData, ctx, r);
				
				if (prcr == PostRuleCheckResult.DEFAULT_RETURN_RESULT)
					return prcr;
			}
			else if (r == PostRuleCheckResult.DEFAULT_RETURN_RESULT)
				return r;
		}
		
		boolean doSearchAndListResult = false;
		boolean forgetIt = false;
		boolean forceQuit = false;
		
		if (status == InnerStatus.ASK_ENTER_OR_NOT) {
			if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
				setHasBeenDoubleConfirmed(true);
				doSearchAndListResult = true;
			}
			else {
				forceQuit = true;
			}
		}
		if (status == InnerStatus.ASK_QUIT_OR_NOT) {
			if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
				forceQuit = true;
			}
			else {
				doSearchAndListResult = true;
			}
		}
		if (status == InnerStatus.JUST_ACTIVATED) {
			if (isEnterDoubleConfirm() && !isHasBeenDoubleConfirmed()) {
				// set Answer
				ctx.setAnswerText(convertText(ctx, getEnterDoubleConfirmText()));
				ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
				ctx.setResponseAttribute("DIALOG", getName());
				status = InnerStatus.ASK_ENTER_OR_NOT;
				return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
			else {
				doSearchAndListResult = true;
			}
		}
		else if (status == InnerStatus.FIELD_DOUBLE_CONFIRM) {
			if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
				doSearchAndListResult = true;
			}
			else {
				forgetIt = true;
			}
		}
		
		String NL = ctx.isClientSupportHtml() ? "<BR>" : "\n";

		if ( status != InnerStatus.INACTIVE ) {
			for (Trigger t: quitTriggers) {
				if (t.isTrigger(ctx)) {
					if (t.isDoubleConfirm()) {
						// set Answer
						ctx.setAnswerText(t.getDoubleConfirmText());
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.setResponseAttribute("DIALOG", getName());
						status = InnerStatus.ASK_QUIT_OR_NOT;
					}
					else {
						// set Answer
						ctx.setAnswerText(showCurrentStatusWhenQuitting ? (showCurrentStatusText() + NL) : "" + StringUtils.trimToEmpty(quitText));
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.setResponseAttribute("DIALOG", getName());
						deactivate();
					}
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
		}
		
		if (doSearchAndListResult || status == InnerStatus.IN_PROGRESS) {
			try {
				boolean hasNewMatch = false;
				
				for (Field f: fields) {
					RuleCheckResult result = f.checkRules(ctx);
					
					if (result.isMatch()) {
						hasNewMatch = true;
						f.setCurrentResult(result);
					}
				}
				
				if (hasNewMatch || doSearchAndListResult || !matchQaWhenNothingToDo) {
					QAPattern qp = QAPattern.getByKey(ctx.getTenant().getId(), qaPatternMkey);
					Class aggregatorClazz = QADataAggregator.findRegisteredClass(qp.getDataAggregator());
					QADataAggregator aggregator = (QADataAggregator) aggregatorClazz.newInstance();
					SolrQuery query = aggregator.generateQuery(null, qp, ctx, new ArrayList<CustomData>(), new AtomicBoolean());
					
					for (Field f: fields) {
						RuleCheckResult result = f.getCurrentResult();
						
						if (result != null) {
							Rule r = result.getRule();
							
							if (r instanceof SearchableRule) {
								SearchableRule sr = (SearchableRule)r;
								sr.applySearchCriteria(result, query);
							}
						}
					}

					String ans = QAPatternUtil.applyQAPatternTemplate(ctx, qp, query);
					if (showCurrentStatusWhenSearching) {
						ans = showCurrentStatusWhenSearchingText(ctx) + NL + ans;
					}
					ctx.setAnswerText(ctx.isClientSupportHtml() ? WiSeUtils.nl2br(ans) : ans);
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.IN_PROGRESS;
					
					if (runPostMatchChain) {
						return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
					}
					else return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
				else {
					return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		
		if (forceQuit) {
			// set Answer
			ctx.setAnswerText(showCurrentStatusWhenQuitting ? (showCurrentStatusText() + NL) : "" + StringUtils.trimToEmpty(quitText));
			ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
			ctx.setResponseAttribute("DIALOG", getName());
			deactivate();
			
			return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
	
	public static void main(String[] args) {
		int tenantId = 6;
		
		try {
			File theDialogJsonFile = new File(new File(WiSeEnv.getHomePath()), "dialogs/" + tenantId + ".json");
			
			String dialogJson = FileUtils.getContentsAsString(theDialogJsonFile, "UTF-8");
			
			if (StringUtils.isNotEmpty(dialogJson)) {
				JSONObject dialogsConfig = new JSONObject(dialogJson);
				dialogsConfig.put("lastModified", theDialogJsonFile.lastModified());
				
				System.out.println("Got Dialogs for tenant[" + tenantId + "]:" + dialogsConfig.toString(2));
				
				JSONArray dialogs = dialogsConfig.getJSONArray("dialogConfig");
				
				for (int i=0; i < dialogs.length(); i++) {
					JSONObject cfg = dialogs.getJSONObject(i);
					QADialog ffd = new CustomQASearchDialog();
					ffd.init(cfg);
					
					for (Field f: ffd.getFields()) {
						for (Rule r: f.getRules()) {
							r.runTest();
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String convertText(QAContext ctx, String text) {
		String tmp = text;
		
		if (tmp.indexOf("${") != -1) {
			for (Field f: fields) {
				if (f.getCurrentValue() != null)
					tmp = tmp.replaceAll("\\$\\{" + f.getName() + "\\}", f.getCurrentValue());
				else 
					tmp = tmp.replaceAll("\\$\\{" + f.getName() + "\\}", "");
			}
		}
		
		return replaceVariables(ctx, tmp);
	}

	@Override
	public String showCurrentStatusText() {
		StringBuilder b = new StringBuilder();
		
		for (Field f: fields) {
			if (f.getCurrentValue() != null) {
				if (b.length() > 0) b.append("，");
			
				b.append(f.getShowName() + ":" + f.getCurrentValue());
			}
			else {
				if (f.getRequire() > 0) {
					if (b.length() > 0) b.append("，");
					
					b.append(f.getShowName() + ":尚未填入");
				}
			}
		}
		return b.toString();
	}

	public String showCurrentStatusWhenSearchingText(QAContext ctx) {

		if (normalTriggers != null && normalTriggers.size() > 0) {
			for (Trigger t: normalTriggers) {
				NormalTrigger nt = (NormalTrigger)t;
				
				if (nt.matchEndpoint("replaceShowCurrentStatusWhenSearchingText")) {
					if (t.isTrigger(ctx)) {
						Map<String, String> action = nt.getAction();
						String type = action.get("type");
						
						if ("replace".equals(type)) {
							String replaceWithText = action.get("replaceWithText");
							System.out.println(replaceWithText);
							JexlContext jctx = new MapContext();
							jctx.set("ctx", ctx);
							
							return (String)JexlUtil.runScript(jctx, replaceWithText);
						}
					}
				}
			}
		}
		
		Pattern p = Pattern.compile("(?si)\\$\\{fields#(?<separator>.*?)#(?<allText>.*?)\\}");
		Matcher m = p.matcher(showCurrentStatusWhenSearchingText);
		String separator = "，";
		String allText = "全部";
		
		if (m.find()) {
			separator = m.group("separator");
			allText = m.group("allText");
			StringBuilder b = new StringBuilder();
			
			for (Field f: fields) {
				if (f.getCurrentValue() != null) {
					if (b.length() > 0) b.append(separator);
				
					b.append(f.getCurrentValue());
				}
			}
			if (b.length() == 0) {
				b.append(allText);
			}
			
			return m.replaceAll(b.toString());
		}
		
		return showCurrentStatusWhenSearchingText;
	}

	@Override
	public PostRuleCheckResult postCheck(QAContext ctx,
			PostRuleCheckResult resultFromCheck) {
		ctx.setCtxAttr("activeDialog", this);
		try {
			PostRuleCheckResult res = (PostRuleCheckResult)checkNormalTrigger("postDialogCheck", ctx, this, normalTriggers);
			if (res != null) {
				return res;
			}
			return super.postCheck(ctx, resultFromCheck);
		}
		finally {
			if (ctx.getCtxAttr("activeDialog") == this)
				ctx.setCtxAttr("activeDialog", null);
		}
	}

	@Override
	public void reset() {
		status = InnerStatus.INACTIVE;
		currentField = null;
		currentFieldOffset = null;
		
		for (Field f: fields) {
			f.setCurrentValue(null);
		}
	}

	@Override
	public void activate() {
		status = InnerStatus.JUST_ACTIVATED;
	}

	@Override
	public void deactivate() {
		status = InnerStatus.INACTIVE;
	}

	@Override
	public boolean isActive() {
		return status != InnerStatus.INACTIVE;
	}
}
