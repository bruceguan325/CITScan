package com.intumit.solr.robot.qadialog;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlExpression;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
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

import com.ibm.icu.util.BytesTrie.Iterator;
import com.intumit.solr.config.ColumnNameMappingFacade;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QADataAggregator;
import com.intumit.solr.robot.QAOutputResult;
import com.intumit.solr.robot.QAPattern;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.qadialog.CustomQASearchDialog.InnerStatus;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QADialogRule;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.mchange.io.FileUtils;

import flexjson.JSONDeserializer;
import groovy.lang.Binding;

public class SingleDocDialog extends QADialog {
	private static final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
	List<Trigger> quitTriggers = new ArrayList<Trigger>();
	List<Trigger> normalTriggers = new ArrayList<Trigger>();
	String qaPatternMkey = null;
	String quitText = null;
	String showCurrentStatusWhenSearchingText = null;
	boolean quitImmediately = true;
	boolean showCurrentStatusWhenQuitting = false;
	boolean showCurrentStatusWhenSearching = false;
	boolean runPostMatchChain = false;
	boolean matchQaWhenNothingToDo = false;
	
	Integer currentFieldOffset = null;
	Field currentField = null;
	String currentDocExpr = null;
	SolrDocument currentDoc = null;

	public SingleDocDialog() {
		
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
			currentDocExpr  = config.optString("currentDocExpr");
			quitImmediately = config.optBoolean("quitImmediately", Boolean.TRUE);
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

	public SolrDocument getCurrentDoc() {
		return currentDoc;
	}

	public void setCurrentDocs(SolrDocument currentDoc) {
		this.currentDoc = currentDoc;
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
		ctx.setCtxAttr("activeDialog", this);
		try {
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
			boolean forceQuit = false;
	
			if (status == InnerStatus.ASK_ENTER_OR_NOT) {
				if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
					getCurrentDocFromExpr(ctx);
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
					getCurrentDocFromExpr(ctx);
					doSearchAndListResult = true;
				}
			}
			else if (status == InnerStatus.FIELD_DOUBLE_CONFIRM) {
				if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
					doSearchAndListResult = true;
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
				
				if ("全部保單".equals(ctx.getCurrentQuestion())) {
					// set Answer
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					ctx.setCtxAttr(getName() + ".returnType", "ALL_INSURANCE_FEE");
					deactivate();
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
				else if ("不是這張保單".equals(ctx.getCurrentQuestion())) {
					// set Answer
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					ctx.setCtxAttr(getName() + ".returnType", "ALL_INSURANCE_FEE");
					deactivate();
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
			
			if (doSearchAndListResult || status == InnerStatus.IN_PROGRESS) {
				try {
					if (currentDoc != null) {
						QAPattern qp = QAPattern.getByKey(ctx.getTenant().getId(), qaPatternMkey);
						Class aggregatorClazz = QADataAggregator.findRegisteredClass(qp.getDataAggregator());
						QADataAggregator aggregator = (QADataAggregator) aggregatorClazz.newInstance();
						StringWriter buf = new StringWriter();
						List<Field> targetFields = new ArrayList<Field>();
						
						Set<QAEntity> entities = (Set<QAEntity>)ctx.getRequestAttribute("entities");
						if (entities != null && entities.size() > 0) {
							findAndGatherTargetFields(ctx, fields, targetFields, entities);
						}
						
						if (targetFields.size() == 0) {
							for (Field f: fields) {
								RuleCheckResult result = f.checkRules(ctx);
								
								if (result.isMatch()) {
									f.setCurrentResult(result);
									targetFields.add(f);
								}
							}
						}
						
						if (targetFields.size() > 0) {
							int counter = 0;
							
							for (Field f: targetFields) {
								RuleCheckResult result = f.getCurrentResult();
								
								if (result != null) {
									Object value = currentDoc.getFirstValue(f.getTargetIndexFieldName());
									if (counter > 0) 
										buf.append("，");
									
									Map<String, Object> bundle = new HashMap<String, Object>();
									bundle.put("currentDoc", currentDoc);
									bundle.put("field", f);
									bundle.put("fieldValue", value);
									counter++;
									
									if (f.getOutputExpr() != null)
										buf.append(this.getFieldOutputString(ctx, f, bundle));
									else 
										buf.append(value.toString());
								}
							}
						}
						else {
							if (doSearchAndListResult) {
								QAOutputResult r = new QAOutputResult();
								
								try {
									//取得velocity的上下文context
									VelocityContext context = new VelocityContext();
									SolrDocumentList docs = new SolrDocumentList();
									docs.add(currentDoc);
									r.setHasResult(true);
									SolrDocument firstDoc = docs.get(0);
									List<String> visibleFn = new ArrayList<String>();
									
									for (String fn: firstDoc.getFieldNames()) {
										if (!fn.endsWith("_s") && !fn.endsWith("_ms")) continue;
										if (fn.equalsIgnoreCase("dataType_s")) continue;
										visibleFn.add(fn);
									}
									context.put("fields", visibleFn);
									//把數據填入上下文
									context.put("docs", docs);
									context.put("firstDoc", currentDoc);
									context.put("question", ctx.getCurrentQuestion());
									context.put("qaCtx", ctx);
									context.put("math", new MathTool());
									context.put("num", new NumberTool());
									context.put("date", new DateTool());
									context.put("colMapper", ColumnNameMappingFacade.getInstance());
				
									//轉換輸出
									VelocityEngine ve = new VelocityEngine();
				
									// Make Velocity log to Log4J
									ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
									ve.setProperty("runtime.log.logsystem.log4j.category", this.getClass().getName());
				
									ve.init();
									ve.evaluate(context, buf, this.getClass().getName(), qp.getAnswerTemplate());
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
							else {
								String answer = "";
								
								if (getNoMatchScript() != null) {
									// set Answer
									Binding binding = new Binding();
									binding.setProperty("ctx", ctx);
									binding.setProperty("dlg", this);
									
									answer = (String)GroovyUtil.runScript(binding, getNoMatchScript());
									answer += NL;
								}
								
								if (getInstructionText() != null) {
									answer += getInstructionText();
								}
								
								if (StringUtils.isNotEmpty(answer)) {
									ctx.setAnswerText(convertText(ctx, answer));
									ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
									ctx.setResponseAttribute("DIALOG", getName());
									return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
								}
							}
						}
							
						String ans = buf.toString();
						if (showCurrentStatusWhenSearching) {
							ans = showCurrentStatusWhenSearchingText() + ans;
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
				ctx.setCtxAttr(getName() + ".returnType", "QUIT");
				deactivate();
				
				return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
		}
		finally {
			if (ctx.getCtxAttr("activeDialog") == this)
				ctx.setCtxAttr("activeDialog", null);
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
	
	void findAndGatherTargetFields(QAContext ctx,
			List<Field> allFields, List<Field> targetFields, Set<QAEntity> entities) {
		Map<String, QAEntity> map = new HashMap<>();
		for (QAEntity ed: entities) {
			map.put(ed.getCode(), ed);
		}

		for (Field f: allFields) {
			if (f.getEntities() != null) {
				for (String en: f.getEntities()) {
					if (map.containsKey(en)) {
						if (!targetFields.contains(f)) {
							QAEntity ed = map.get(en);
							f.setCurrentResult(new RuleCheckResult(RuleCheckResultStatus.MATCH, ed.getEntityValues(), null));
							targetFields.add(f);
						}
					}
				}
			}
		}
	}

	private void getCurrentDocFromExpr(QAContext ctx) {
		if (currentDocExpr != null) {
			JexlContext jctx = new MapContext();
			jctx.set("ctx", ctx);
			currentDoc = (SolrDocument)JexlUtil.runScript(jctx, currentDocExpr);
			System.out.println("************" + currentDoc);
		}
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
					QADialog ffd = new SingleDocDialog();
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

	public String showCurrentStatusWhenSearchingText() {
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
