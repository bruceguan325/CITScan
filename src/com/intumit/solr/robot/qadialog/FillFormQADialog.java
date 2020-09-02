package com.intumit.solr.robot.qadialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.qadialog.SingleDocDialog.InnerStatus;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QADialogRule;
import com.intumit.solr.util.WiSeEnv;
import com.mchange.io.FileUtils;

import flexjson.JSONDeserializer;

public class FillFormQADialog extends QADialog {
	List<Trigger> quitTriggers = new ArrayList<Trigger>();
	List<Trigger> normalTriggers = new ArrayList<Trigger>();
	Integer currentFieldOffset = null;

	public FillFormQADialog() {
		
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

	public Field getField(String fieldName) {
		for (Field f: fields) {
			if (fieldName.equalsIgnoreCase(f.getName())) {
				return f;
			}
		}
		
		return null;
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
			
			String NL = ctx.isClientSupportHtml() ? "<BR>" : "\n";
			boolean forceProgressToNextField = false;
			boolean forceQuit = false;
	
			if ( status != InnerStatus.INACTIVE ) {
				for (Trigger t: quitTriggers) {
					if (t.isTrigger(ctx)) {
						if (t.isDoubleConfirm()) {
							// set Answer
							ctx.setAnswerText(convertText(ctx, t.getDoubleConfirmText()));
							ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
							ctx.setResponseAttribute("DIALOG", getName());
							status = InnerStatus.ASK_QUIT_OR_NOT;
						}
						else {
							// set Answer
							ctx.setAnswerText(showCurrentStatusText() + NL + "已經退出情境");
							ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
							ctx.setResponseAttribute("DIALOG", getName());
							status = InnerStatus.INACTIVE;
						}
						return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
				}
			}
			
			if (status == InnerStatus.ASK_ENTER_OR_NOT) {
				if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
					setHasBeenDoubleConfirmed(true);
					currentFieldOffset = 0;
					Field currentField = fields.get(currentFieldOffset);
					
					// set Answer
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					ctx.setAnswerText(convertText(ctx, currentField.getDefaultQuestion()));
					status = InnerStatus.IN_PROGRESS;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
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
					Field currentField = fields.get(currentFieldOffset);
					
					// set Answer
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					ctx.setAnswerText(convertText(ctx, currentField.getDefaultQuestion()));
					status = InnerStatus.IN_PROGRESS;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
			
			if ( status == InnerStatus.JUST_ACTIVATED ) {
				if (isEnterDoubleConfirm() && !isHasBeenDoubleConfirmed()) {
					// set Answer
					ctx.setAnswerText(convertText(ctx, getEnterDoubleConfirmText()));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.ASK_ENTER_OR_NOT;
				}
				else {
					List<Field> currentFields = new ArrayList<Field>();
					findAndGatherTargetFields(ctx, fields, currentFields, entities);
					
					currentFieldOffset = 0;
					Field currentField = fields.get(currentFieldOffset);
					
					// set Answer
					ctx.setAnswerText(convertText(ctx, currentField.getDefaultQuestion()));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					ctx.setResponseAttribute("inputType", currentField.getType());
					status = InnerStatus.IN_PROGRESS;
				}
				return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
			else if (status == InnerStatus.FIELD_DOUBLE_CONFIRM) {
				if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
					forceProgressToNextField = true;
				}
				else {
					Field currentField = fields.get(currentFieldOffset);
					currentField.setCurrentValue(null);
					
					// set Answer
					ctx.setAnswerText(convertText(ctx, currentField.getDefaultQuestion()));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.IN_PROGRESS;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
			
			if (status == InnerStatus.IN_PROGRESS) {
				PostRuleCheckResult brpRes = (PostRuleCheckResult)checkNormalTrigger("beforeRuleProcess", ctx, this, normalTriggers);
				if (brpRes != null && brpRes == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return brpRes;
				
				if (currentFieldOffset != null) {
					Field currentField = fields.get(currentFieldOffset);
					
					if (currentField != null && currentField.getCurrentValue() == null) {
						RuleCheckResult result = currentField.checkRules(ctx);
						
						if (result.getStatus() == RuleCheckResultStatus.CALL_DIALOG) {
							CallDialogRuleCheckResult crcr = (CallDialogRuleCheckResult)result;
							currentField.setCurrentResult(crcr);
							currentField.setCurrentValue(crcr.getValue());
							QADialog dlg = QADialogRule.getDialogInstance(ctx, crcr.getTargetDialogName());
							ctx.setCtxAttr(crcr.getTargetDialogName(), crcr);
							PostRuleCheckResult res = call(ctx, dlg, crcr);
							return res;
						}
						else if (result.isMatch()) {
							Map<String, Object> invalid = currentField.validate(ctx, this);
							
							if (invalid == null) {
								if (Boolean.TRUE == currentField.getDoubleConfirm()) {
									
									// set Answer
									ctx.setAnswerText(convertText(ctx, currentField.getDoubleConfirmText()));
									ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
									ctx.setResponseAttribute("DIALOG", getName());
									status = InnerStatus.FIELD_DOUBLE_CONFIRM;
									
									return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
								}
								else {
									forceProgressToNextField = true;
								}
							}
							else {
								String errorText = (String)invalid.get("errorText");
								currentField.setCurrentValue(null);
								currentField.setCurrentResult(null);
								
								// set Answer
								ctx.setAnswerText(convertText(ctx, errorText));
								ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
								ctx.setResponseAttribute("DIALOG", getName());
								
								return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
							}
						}
						else {
							// set Answer
							ctx.setAnswerText(convertText(ctx, currentField.getDefaultQuestion()));
							ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
							ctx.setResponseAttribute("DIALOG", getName());
							ctx.setResponseAttribute("inputType", currentField.getType());
							status = InnerStatus.IN_PROGRESS;
							
							return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
						}
					}
					else {
						// 如果 Field.currentValue != null，就往下一欄去
						forceProgressToNextField = true;
					}
				}
			}
			
			if (forceProgressToNextField) {
				if (currentFieldOffset < (fields.size() - 1)) {
					currentFieldOffset++;
					Field currentField = fields.get(currentFieldOffset);
					
					// set Answer
					ctx.setAnswerText(convertText(ctx, currentField.getDefaultQuestion()));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					ctx.setResponseAttribute("inputType", currentField.getType());
					
					status = InnerStatus.IN_PROGRESS;
	
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
				else {
					forceQuit = true;
				}
			}
			
			if (forceQuit) {
				// set Answer
				ctx.setAnswerText(showCurrentStatusText() + NL + "已經退出情境");
				ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
				ctx.setResponseAttribute("DIALOG", getName());
				status = InnerStatus.INACTIVE;
				
				return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
		}
		finally {
			if (ctx.getCtxAttr("activeDialog") == this)
				ctx.setCtxAttr("activeDialog", null);
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}
	
	public static void main(String[] args) {
		int tenantId = 4;
		
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
					FillFormQADialog ffd = new FillFormQADialog();
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
			if (!f.isHidden()) {
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
		}
			
		return b.toString();
	}

	@Override
	public void reset() {
		status = InnerStatus.INACTIVE;
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
	
	void findAndGatherTargetFields(QAContext ctx,
			List<Field> allFields, List<Field> targetFields, List<QAEntity> entities) {
		Map<String, QAEntity> map = new HashMap<>();
		for (QAEntity ed: entities) {
			map.put(ed.getCode(), ed);
		}

		for (Field f: allFields) {
			if (f.getLocked()) continue; 
			
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

	boolean findAndGatherFilters(QAContext ctx, List<Filter> allFilters, List<Filter> targetFilters, Set<QAEntity> entities) {
		boolean hasNewFilter = false;
		int currentFiltersSize = targetFilters.size();
		Filter nameFilter = getFilter("insuranceName");
		Filter insuranceNoFilter = getFilter("insuranceNo");
		boolean hasNameFilter = false;
		boolean hasInsuranceNoFilter = false;
		
		Map<String, QAEntity> map = new HashMap<>();
		for (QAEntity ed: entities) {
			if (ed.getEntityValues() != null)
				map.put(ed.getCode(), ed);
		}

		for (Filter f: allFilters) {
			if (f.getLocked()) continue; 
			
			if (f.getEntities() != null) {
				for (String en: f.getEntities()) {
					if (map.containsKey(en)) {
						if (!targetFilters.contains(f)) {
							QAEntity ed = map.get(en);

							SearchableRegexRule srr = new SearchableRegexRule();
							srr.setMatch(Arrays.asList(StringUtils.split(ed.getEntityValues(), ",")));
							srr.setPartial(true);
							srr.setQueryType("boolean");
							srr.setSearchType("filterQuery");
							srr.setTargetField(f.getTargetIndexFieldName());
							f.setCurrentResult(new RuleCheckResult(RuleCheckResultStatus.MATCH, ed.getEntityValues(), srr));
							targetFilters.add(f);
							hasNewFilter = true;
							
							// 記起來，之後要過濾（兩者不能同時存在，不然沒辦法處理上下文
							if (nameFilter.equals(f)) {
								hasNameFilter = true;
							}
							else if (insuranceNoFilter.equals(f)) {
								hasInsuranceNoFilter = true;
							}
						}
					}
				}
			}
		}
		
		for (Filter f: allFilters) {
			if (f.getLocked()) continue; 
			
			if (targetFilters.contains(f))
				continue;
			
			RuleCheckResult result = f.checkRules(ctx);
			
			if (result.isMatch()) {
				f.setCurrentResult(result);
				
				if (targetFilters.contains(f)) {
					targetFilters.remove(f);
				}
				
				targetFilters.add(f);
				hasNewFilter = true;
				
				// 記起來，之後要過濾（兩者不能同時存在，不然沒辦法處理上下文
				if (nameFilter.equals(f)) {
					hasNameFilter = true;
				}
				else if (insuranceNoFilter.equals(f)) {
					hasInsuranceNoFilter = true;
				}
			}
		}
		
		return hasNewFilter;
	}
}
