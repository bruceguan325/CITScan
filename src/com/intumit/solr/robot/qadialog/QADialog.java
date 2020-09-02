package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.TemplateUtil.Replacer;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.qadialog.CallDialogRuleCheckResult.RewriteType;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QADialogRule;

import flexjson.JSONDeserializer;
import groovy.lang.Binding;

public abstract class QADialog implements Serializable {
	public static final String UTTERANCE_INDEX_DATATYPE = "DIALOG_TRAINED_UTTERANCE";
	public static final String DIGSP = "DLGSPCODE";
	JSONObject config;
	String mkey;
	String name;
	String description;
	
	Stack<CallStackData> callStack = new Stack<>();
	List<Field> fields = new ArrayList<Field>();
	List<Filter> filters = new ArrayList<Filter>();
	
	List<QAIntent> intents = new ArrayList<>();
	List<QAEntity> entities = new ArrayList<>();
	
	String instructionText;
	String noMatchScript;
	boolean enterDoubleConfirm;
	String enterDoubleConfirmText;
	boolean hasBeenDoubleConfirmed;
	boolean reconstructQuestion = true;
	JSONObject customScripts;
	
	public void init(JSONObject config) {
		this.config = config;
		loadConfig();
	}

	public void loadConfig() {
		try {
			mkey = config.getString("mkey");
			name = config.getString("dialogName");
			description = config.getString("dialogDesc");
			reconstructQuestion = config.optBoolean("reconstructQuestion", Boolean.TRUE);
			enterDoubleConfirm = config.optBoolean("enterDoubleConfirm", Boolean.FALSE);
			enterDoubleConfirmText = config.optString("enterDoubleConfirmText");
			hasBeenDoubleConfirmed = false;
			instructionText = config.optString("instructionText");
			noMatchScript = config.optString("noMatchScript");
			
			JSONDeserializer jd = new JSONDeserializer();
			
			if (config.containsKey("intents")) {
				intents = (List<QAIntent>)jd.deserialize(config.optString("intents"));
			}
			if (config.containsKey("entities")) {
				entities = (List<QAEntity>)jd.deserialize(config.optString("entities"));
			}
			
			if (config.containsKey("customScripts")) {
				customScripts = config.getJSONObject("customScripts");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public static QAIntent findIntent(QAContext ctx, String tag) {
		QAIntent intent = QAIntent.get(ctx.getTenant().getId(), tag, null);
		
		return intent;
	}
	
	public static QAEntity findEntity(QAContext ctx, String dialogName, String code) {
		QAEntity e = QAEntity.get(ctx.getTenant().getId(), code, dialogName, null);
		
		return e;
	}
	
	public boolean isEnterDoubleConfirm() {
		return enterDoubleConfirm;
	}

	public void setEnterDoubleConfirm(boolean enterDoubleConfirm) {
		this.enterDoubleConfirm = enterDoubleConfirm;
	}

	public String getEnterDoubleConfirmText() {
		return enterDoubleConfirmText;
	}

	public void setEnterDoubleConfirmText(String enterDoubleConfirmText) {
		this.enterDoubleConfirmText = enterDoubleConfirmText;
	}

	public boolean isHasBeenDoubleConfirmed() {
		return hasBeenDoubleConfirmed;
	}

	public void setHasBeenDoubleConfirmed(boolean hasBeenDoubleConfirmed) {
		this.hasBeenDoubleConfirmed = hasBeenDoubleConfirmed;
	}

	public boolean isReconstructQuestion() {
		return reconstructQuestion;
	}

	public void setReconstructQuestion(boolean reconstructQuestion) {
		this.reconstructQuestion = reconstructQuestion;
	}

	public String getInstructionText() {
		return instructionText;
	}

	public void setInstructionText(String instructionText) {
		this.instructionText = instructionText;
	}

	public String getNoMatchScript() {
		return noMatchScript;
	}

	public void setNoMatchScript(String noMatchScript) {
		this.noMatchScript = noMatchScript;
	}

	public PostRuleCheckResult check(QAContext ctx) {
		
		PostRuleCheckResult r = innerCheck(ctx);
		
		if (ctx.getTenant().getEnableDebug()) {
			if (this instanceof QAConversationalDialog) {
				DialogLogEntry log = ((QAConversationalDialog)this).currentLog;
				System.out.println("*** Conversation Log Detail [" + ctx.getOriginalQuestion() + "]: ***");
				System.out.println(StringUtils.trimToEmpty(log.getLogDetail()));
				System.out.println("*** Conversation Log Detail [END] ***");
				
				ctx.setRequestAttribute("DialogLog", log);
			}
		}
		r = postCheck(ctx, r);
		
		return r;
	}
	
//	public boolean findTrainedUtterance(QAContext ctx) {
//		QAUtil qautil = QAUtil.getInstance(ctx.getTenant());
//		SolrDocument doc = qautil.searchDirectDialogUtterance(ctx.getCurrentQuestion(), this.getName(), ctx);
//		
//		if (doc != null) {
//			System.out.println("Found trained utterance:" + doc);
//			
//			Collection<Object> intentTags = doc.getFieldValues("INTENT_ms");
//			Collection<Object> entityTags = doc.getFieldValues("ENTITY_ms");
//			String entityValuesStr = StringUtils.trimToNull((String)doc.getFirstValue("ENTITY_VALUES_JSON_s"));
//			JSONObject entityValuesJson = null;
//			if (entityValuesStr != null) {
//				try {
//					entityValuesJson = new JSONObject(entityValuesStr);
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//			}
//			if (entityValuesStr == null) {
//				entityValuesJson = new JSONObject();
//			}
//			
//			if (intentTags != null || entityTags != null) {
//				Set<QAIntent> intents = new HashSet<QAIntent>();
//				
//				if (intentTags != null) {
//					for (Object t: intentTags) {
//						String tag = (String)t;
//						QAIntent i = findIntent(ctx, tag);
//						
//						if (i != null) {
//							intents.add(i);
//						}
//					}
//				}
//				
//				ctx.setIntents(intents);
//				
//				Set<QAEntity> entities = new HashSet<QAEntity>();
//
//				if (entityTags != null) {
//					for (Object t: entityTags) {
//						String code = (String)t;
//						QAEntity entity = findEntity(ctx, name, code);
//						
//						if (entity != null) {
//							entity = entity.getCopy();
//							entities.add(entity);
//							
//							if (entityValuesJson.has(code)) {
//								try {
//									entity.setEntityValues(entityValuesJson.getString(code));
//								} catch (JSONException e) {
//									e.printStackTrace();
//								}
//							}
//							else {
//								entity.setEntityValues(null);
//							}
//						}
//						
//					}
//				}
//				
//				ctx.setEntities(entities);
//				
//				return true;
//			}
//		}
//		
//		return false;
//	}
	

	public static boolean findTrainedUtterance(QAContext ctx, String dialogName) {
		QAUtil qautil = QAUtil.getInstance(ctx.getTenant());
		SolrDocument doc = qautil.searchDialogUtterance(ctx.getCurrentQuestion(), dialogName, ctx);
		
		if (doc != null) {
			System.out.println("Found trained utterance:" + doc);
			
			Collection<Object> intentTags = doc.getFieldValues("INTENT_ms");
			Collection<Object> entityTags = doc.getFieldValues("ENTITY_ms");
			String entityValuesStr = StringUtils.trimToNull((String)doc.getFirstValue("ENTITY_VALUES_JSON_s"));
			JSONObject entityValuesJson = null;
			if (entityValuesStr != null) {
				try {
					entityValuesJson = new JSONObject(entityValuesStr);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			if (entityValuesStr == null) {
				entityValuesJson = new JSONObject();
			}
			
			if (intentTags != null || entityTags != null) {
				Set<QAIntent> intents = new HashSet<QAIntent>();
				
				if (intentTags != null) {
					for (Object t: intentTags) {
						String tag = (String)t;
						QAIntent i = findIntent(ctx, tag);
						
						if (i != null) {
							intents.add(i);
						}
					}
				}
				
				ctx.setIntents(intents);
				
				Set<QAEntity> entities = new HashSet<QAEntity>();

				if (entityTags != null) {
					for (Object t: entityTags) {
						String code = (String)t;
						QAEntity entity = findEntity(ctx, null, code);
						
						if (entity != null) {
							entity = entity.getCopy();
							entities.add(entity);
							
							if (entityValuesJson.has(code)) {
								try {
									entity.setEntityValues(entityValuesJson.getString(code));
								} catch (JSONException e) {
									e.printStackTrace();
								}
							}
							else {
								entity.setEntityValues(null);
							}
						}
						
					}
				}
				
				ctx.setEntities(entities);
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * 進入 check 之後首要工作是
	 * 1. 將 ctx.ctxAttr 的 "activeDialog" 設定為 this
	 * 2. 檢查本身的 call Stack，是否要把訊息往下傳遞，或者要取回相關資訊
	 * 
	 * @param ctx
	 * @return
	 */
	public abstract PostRuleCheckResult innerCheck(QAContext ctx);
	public abstract String showCurrentStatusText();
	public abstract String convertText(QAContext ctx, String text);

	public abstract boolean isActive();
	public abstract void activate();
	public abstract void deactivate();
	public abstract void reset();
	
	public PostRuleCheckResult postCheck(QAContext ctx, PostRuleCheckResult resultFromCheck) {
		ctx.setCtxAttr("activeDialog", this);
		try {
			return resultFromCheck;
		}
		finally {
			if (ctx.getCtxAttr("activeDialog") == this)
				ctx.setCtxAttr("activeDialog", null);
		}
	}
	
	public Field getField(String name) {
		for (Field f: fields) {
			if (StringUtils.equals(f.getName(), name))
				return f;
		}
		return null;
	}
	
	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}
	
	public Filter getFilter(String name) {
		for (Filter f: filters) {
			if (StringUtils.equals(f.getName(), name))
				return f;
		}
		return null;
	}

	public List<Filter> getFilters() {
		return filters;
	}

	public void setFilters(List<Filter> filters) {
		this.filters = filters;
	}

	public String getMkey() {
		return mkey;
	}

	public void setMkey(String mkey) {
		this.mkey = mkey;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/*	public QADialog peek(QAContext ctx) {
		if (callStack.size() > 0)
			return callStack.peek();
		
		return null;
	}
	
	public QADialog pop(QAContext ctx) {
		if (callStack.size() > 0)
			return callStack.pop();
		
		return null;
	}
	
	public int stackSize(QAContext ctx) {
		return callStack.size();
	}
*/	
	public static String generateDlgSpCode(String code, String trigger) {
		return "{{" + DIGSP + ":" + code + ":" + trigger + "}}";
	}
	
	public static class DlgSpCodeFinder implements Replacer {
		private String bindToTag;

		public DlgSpCodeFinder(QAContext ctx) {
			this.bindToTag = DIGSP;
		}

		@Override
		public String call(String name, String val) {
			
			if (StringUtils.equals(name, bindToTag)) {
				String code = StringUtils.trimToNull(StringUtils.substringBefore(val, ":"));
				String trigger = StringUtils.substringAfter(val, ":");
				
				return code + ":" + trigger;
			}

			return null;
		}
		
		@Override
		public String call(QAContext ctx, String name, String val) {
			return call(name, val);
		}

	}
	
	
	public static Object checkNormalTrigger(String endpoint, QAContext ctx, QADialog dlg, List<Trigger> normalTriggers) {

		if (normalTriggers != null && normalTriggers.size() > 0) {
			for (Trigger t: normalTriggers) {
				NormalTrigger nt = (NormalTrigger)t;
				
				if (nt.matchEndpoint(endpoint)) {
					if (t.isTrigger(ctx)) {
						Map<String, String> action = nt.getAction();
						String type = action.get("type");
						
						if ("replace".equals(type)) {
							String replaceWithText = action.get("replaceWithText");
							Binding binding = new Binding();
							binding.setProperty("ctx", ctx);
							binding.setProperty("dlg", dlg);
							
							return GroovyUtil.runScript(binding, replaceWithText);
						}
						else if ("open".equals(type)) {
							String targetType = action.get("targetType");
							String targetName = action.get("targetName");
							if ("dialog".equals(targetType)) {
								return dlg.call(ctx, QADialogRule.getDialogInstance(ctx, targetName), null);
							}
						}
						else if ("runScript".equals(type)) {
							try {
								String scriptText = null;
								if (action.containsKey("scriptText")) {
									scriptText = action.get("scriptText");
								}
								else if (action.containsKey("scriptRef")) {
									scriptText = dlg.customScripts.getJSONObject(action.get("scriptRef")).optString("scriptText");
								}
								Binding binding = new Binding();
								binding.setProperty("ctx", ctx);
								binding.setProperty("dlg", dlg);
								
								return GroovyUtil.runScript(binding, scriptText);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
		
		return null;
	}

	static final Pattern variablePattern = Pattern.compile("\\{\\{([a-zA-Z0-9_/\\s\\$\\+]+?)(:.+?)?\\}\\}");
	public static String replaceVariables(QAContext ctx, String str) {
		String out = null;
		if (str != null){
			StringBuffer sb = new StringBuffer();
			Matcher matcher = variablePattern.matcher(str);
			
			while (matcher.find()) {
				String name = StringUtils.trim(matcher.group(1));
				String val = StringUtils.trimToNull(StringUtils.substring(matcher.group(2), 1));

				String replaced = null;
				Object property = null;
				if (StringUtils.equals(name, "ReqAttr")) {
					String key = StringUtils.trimToNull(StringUtils.substringBefore(val, ":"));
					String propertyName = StringUtils.substringAfter(val, ":");
					
					Object bean = ctx.getRequestAttribute(key);
					try {
						property = BeanUtils.getProperty(bean, propertyName);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				else if (StringUtils.equals(name, "CtxAttr")) {
					String key = StringUtils.trimToNull(StringUtils.substringBefore(val, ":"));
					String propertyName = StringUtils.substringAfter(val, ":");
					
					if (propertyName.indexOf(":") != -1) {
						propertyName = StringUtils.replaceChars(propertyName, ':', '.');
					}
					
					Object bean = ctx.getCtxAttr(key);
					try {
						property = BeanUtils.getNestedProperty(bean, propertyName);
					} catch (Exception e) {
						e.printStackTrace();
					}
					
				}
				
				replaced = property == null ? null : property.toString();
				
				if (replaced != null){
					replaced = StringUtils.replace(replaced, "$", "\\$");
					matcher.appendReplacement(sb, replaced);
				}
				else {
					matcher.appendReplacement(sb, "");
				}
			}
			matcher.appendTail(sb);
			out = sb.toString();
		}
		return out;
	}
	
	public String getFieldOutputString(QAContext ctx, Field field, Object bundle) {
		if (StringUtils.isNotEmpty(field.getOutputExpr())) {
			JexlContext jctx = new MapContext();
			jctx.set("ctx", ctx);
			jctx.set("dlg", this);
			jctx.set("field", field);
			jctx.set("bundle", bundle);
			jctx.set("sdf", new SimpleDateFormat("yyyy-MM-dd"));
			String result = (String)JexlUtil.runExpr(jctx, field.getOutputExpr());
			
			return StringUtils.trimToEmpty(result);
		}
		
		return field.getShowName() + "：" + field.getCurrentValue();
	}
	
	public String getFieldSimpleOutputString(QAContext ctx, Field field, Object bundle) {
		if (StringUtils.isNotEmpty(field.getSimpleOutputExpr())) {
			JexlContext jctx = new MapContext();
			jctx.set("ctx", ctx);
			jctx.set("dlg", this);
			jctx.set("field", field);
			jctx.set("bundle", bundle);
			jctx.set("sdf", new SimpleDateFormat("yyyy-MM-dd"));
			String result = (String)JexlUtil.runExpr(jctx, field.getSimpleOutputExpr());
			
			return StringUtils.trimToEmpty(result);
		}
		
		return field.getShowName() + "：" + field.getCurrentValue();
	}
	
	public PostRuleCheckResult call(QAContext ctx, QADialog beCall, CallDialogRuleCheckResult crcr) {
		callStack.push(new CallStackData(this, beCall, crcr, ctx.getCurrentQuestion()));
		beCall.activate();
		return beCall.check(ctx);
	}
	
	public PostRuleCheckResult returnFromDialog(CallStackData stackData, QAContext ctx, PostRuleCheckResult r) {
		List<Field> fields2 = stackData.to.getFields();
		RewriteType rt = RewriteType.OVERWRITE;
		if (stackData.crcr != null) {
			rt = stackData.crcr.getRewriteType();
		}

		for (Field f2 : fields2) {
			boolean existedInF1 = false;
			
			for (int i=0; i < fields.size(); i++) {
				Field f1 = fields.get(i);
				
				if (StringUtils.equals(f1.getName(), f2.getName())) {
					existedInF1 = true;
					f1.setCurrentResult(f2.getCurrentResult());
					
					if (rt == RewriteType.OVERWRITE)
						f1.setCurrentValue(StringUtils.trimToEmpty(f2.getCurrentValue()));
					else if (rt == RewriteType.PREPEND)
						f1.setCurrentValue(StringUtils.trimToEmpty(f2.getCurrentValue()) + StringUtils.trimToEmpty(f1.getCurrentValue()));
					else if (rt == RewriteType.APPEND)
						f1.setCurrentValue(StringUtils.trimToEmpty(f1.getCurrentValue()) + StringUtils.trimToEmpty(f2.getCurrentValue()));
				}
			}
			
			if (!existedInF1) {
				Field nf = new Field();
				nf.setName(f2.getName());
				nf.setShowName(f2.getShowName());
				nf.setMultivalue(f2.multivalue);
				nf.setRequire(f2.require);
				nf.setType(f2.type);
				nf.setHidden(f2.isHidden());
				nf.setCurrentResult(f2.getCurrentResult());
				nf.setCurrentValue(f2.getCurrentValue());
				nf.setLocked(true);
				nf.setNoContext(f2.getNoContext());
				
				fields.add(nf);
			}
		}
		
		ctx.setCurrentQuestion(stackData.question);
		PostRuleCheckResult prcr = check(ctx);
		
		return prcr;
	}
}
