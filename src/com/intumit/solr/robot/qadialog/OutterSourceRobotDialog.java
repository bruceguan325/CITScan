package com.intumit.solr.robot.qadialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
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
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.SearchManager;
import com.intumit.solr.config.ColumnNameMappingFacade;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QADataAggregator;
import com.intumit.solr.robot.QAOutputResult;
import com.intumit.solr.robot.QAPattern;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.CustomData;
import com.intumit.solr.robot.entity.QAEntity;
import com.intumit.solr.robot.intent.QAIntent;
import com.intumit.solr.robot.qadialog.FillFormQADialog.InnerStatus;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QADialogRule;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.mchange.io.FileUtils;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import groovy.lang.Binding;

/**
 * 這個是利用存在索引內的 dataType:XXXX 資料來實作
 * (範例資料在 sample/insurance.csv）
 * 
 * @author herb
 */
public class OutterSourceRobotDialog extends QADialog {
	List<Trigger> enterTriggers = new ArrayList<Trigger>();
	List<Trigger> quitTriggers = new ArrayList<Trigger>();
	List<Trigger> normalTriggers = new ArrayList<Trigger>();
	String qaPatternMkey = null;
	String quitText = null;
	String showCurrentStatusWhenSearchingText = null;
	String showSearchResultScript = null;
	String httpSolrServer = null;
	boolean showCurrentStatusWhenQuitting = false;
	boolean showCurrentStatusWhenSearching = false;
	boolean doSearchAndListResult;
	boolean hasNewMatch;
	
	Integer currentFieldOffset = null;
	Field currentField = null;
	Integer currentDocOffset = -1;
	SolrDocumentList currentDocs = null;
	
	List<Filter> currentFilters = new ArrayList<Filter>();
	List<Field> currentFields = new ArrayList<Field>();

	public OutterSourceRobotDialog() {
		doSearchAndListResult = false;
		hasNewMatch = false;
	}
	
	InnerStatus status = InnerStatus.INACTIVE;
	public static enum InnerStatus {
		JUST_ACTIVATED,
		INACTIVE,
		ASK_QUIT_OR_NOT, 
		ASK_ENTER_OR_NOT, 
		FIELD_DOUBLE_CONFIRM,
		CHECK_INTENT_AND_ENTITY, 
		WAIT_FOR_FILTER_VALUE,
	}
	
	@Override
	public void loadConfig() {
		super.loadConfig();
		try {
			quitText  = config.optString("quitText");
			qaPatternMkey  = config.optString("qaPatternMkey");
			showCurrentStatusWhenQuitting = config.optBoolean("showCurrentStatusWhenQuitting", Boolean.FALSE);
			showCurrentStatusWhenSearching = config.optBoolean("showCurrentStatusWhenSearching", Boolean.FALSE);
			showCurrentStatusWhenSearchingText  = config.optString("showCurrentStatusWhenSearchingText");
			showSearchResultScript  = config.optString("showSearchResultScript");
			httpSolrServer  = config.optString("httpSolrServer");
			
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
			
			JSONArray filtersJsons = config.getJSONArray("filters");
			
			for (int i=0; i < filtersJsons.length(); i++) {
				JSONObject filterJson = filtersJsons.getJSONObject(i);
				Filter f = (Filter)new JSONDeserializer().use(null, Filter.class).deserialize(filterJson.toString());
				f.init();
				filters.add(f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getHttpSolrServer() {
		return httpSolrServer;
	}

	public void setHttpSolrServer(String httpSolrServer) {
		this.httpSolrServer = httpSolrServer;
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

	public Integer getCurrentDocOffset() {
		return currentDocOffset != null && currentDocs != null && currentDocs.size() > currentDocOffset
				? currentDocOffset 
				: -1;
	}

	public void setCurrentDocOffset(Integer currentDocOffset) {
		this.currentDocOffset = currentDocOffset;
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

	public String getShowSearchResultScript() {
		return showSearchResultScript;
	}

	public void setShowSearchResultScript(String showSearchResultScript) {
		this.showSearchResultScript = showSearchResultScript;
	}
	
	public boolean isDoSearchAndListResult() {
		return doSearchAndListResult;
	}

	public void setDoSearchAndListResult(boolean doSearchAndListResult) {
		this.doSearchAndListResult = doSearchAndListResult;
	}

	public boolean isHasNewMatch() {
		return hasNewMatch;
	}

	public void setHasNewMatch(boolean hasNewMatch) {
		this.hasNewMatch = hasNewMatch;
	}

	SolrDocumentList allDocsOfThisUser = null;

	public PostRuleCheckResult innerCheck(QAContext ctx) {
		ctx.setCtxAttr("activeDialog", this);
		try {
			hasNewMatch = false;
			doSearchAndListResult = false;
			boolean forceQuit = false;
			
			String NL = ctx.isClientSupportHtml() ? "<BR>" : "\n";
			
			if (callStack.size() > 0) {
				CallStackData stackData = callStack.peek();
				PostRuleCheckResult r = stackData.to.check(ctx);
				if (!stackData.to.isActive()) {
					callStack.pop();
					
					if (stackData.to.getName().equals("AccountVerification")) {
						// 驗身回來還是要檢查一下是否是強制離開
						ctx.setCtxAttr(name, null); // 這個是存入 QAContext 的 CtxAttr，方便存取

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
						
						PostRuleCheckResult prcr = returnFromDialog(stackData, ctx, r);
						
						if (prcr != PostRuleCheckResult.DEFAULT_CONTINUE_RESULT)
							return prcr;
					}
					else {
						PostRuleCheckResult prcr = returnFromDialog(stackData, ctx, r);
						
						if (prcr == PostRuleCheckResult.DEFAULT_RETURN_RESULT)
							return prcr;
					}
				}
				else if (r == PostRuleCheckResult.DEFAULT_RETURN_RESULT)
					return r;
			}
			
			if (status == InnerStatus.ASK_ENTER_OR_NOT) {
				if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
					setHasBeenDoubleConfirmed(true);
					PostRuleCheckResult res = (PostRuleCheckResult)checkNormalTrigger("preEnterDialogCheck", ctx, this, normalTriggers);
					if (res != null && res == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return res;
					
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
					PostRuleCheckResult res = (PostRuleCheckResult)checkNormalTrigger("preEnterDialogCheck", ctx, this, normalTriggers);
					if (res != null && res == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return res;
					
					doSearchAndListResult = true;
				}
			}
			else if (status == InnerStatus.FIELD_DOUBLE_CONFIRM) {
				if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
					doSearchAndListResult = true;
				}
				else {
					// set Answer
					ctx.setAnswerText("請輸入「是」或「否」確認");//convertText(ctx, ctx.getLastAnswerText()));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.FIELD_DOUBLE_CONFIRM;  // 維持
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
	
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
			
			if (status == InnerStatus.WAIT_FOR_FILTER_VALUE) {
				status = InnerStatus.CHECK_INTENT_AND_ENTITY;
			}
			
			if (doSearchAndListResult || status == InnerStatus.CHECK_INTENT_AND_ENTITY) {
				PostRuleCheckResult brpRes = (PostRuleCheckResult)checkNormalTrigger("beforeRuleProcess", ctx, this, normalTriggers);
				if (brpRes != null && brpRes == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return brpRes;

				try {
					if (findAndGatherFilters(ctx, filters, currentFilters)) {
						hasNewMatch = true;
					}
					
					for (Filter filter: filters) {
						if (filter.getRequire() > 0) {
							if (!currentFilters.contains(filter)) {
								// set Answer
								ctx.setAnswerText(convertText(ctx, filter.getDefaultQuestion()));
								ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
								ctx.setResponseAttribute("DIALOG", getName());
								ctx.setResponseAttribute("inputType", filter.getType());
								status = InnerStatus.WAIT_FOR_FILTER_VALUE;
								
								return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
							}
						}
					}
					
					Set<QAEntity> entities = (Set<QAEntity>)ctx.getRequestAttribute("entities");
					List<Field> oldFields = new ArrayList<Field>();
					oldFields.addAll(currentFields);
					int oldFieldsSize = oldFields.size();
					currentFields.clear(); // 目前先每次清掉要查詢的欄位
					
					if (entities != null && entities.size() > 0) {
						hasNewMatch |= findAndGatherTargetFields(ctx, fields, currentFields, entities);
					}

					// 怕之前的設定太寬衝突到，所以多了一些限制，理論上應該要拿掉
					/*if (currentFieldsSize == currentFields.size()) {
						for (Field f: getFields()) {
							if (f.getLocked()) continue; 
							RuleCheckResult result = f.checkRules(ctx);
							
							if (result.isMatch()) {
								f.setCurrentResult(result);
								currentFields.add(f);
							}
						}
					}*/
					
					// 檢查如果同名的 field 已經有 apply filter，就不要顯示這一欄，不然很乾
					Iterator<Field> fItr = currentFields.iterator();
					while (fItr.hasNext()) {
						Field f = fItr.next();
						Filter flt = getFilter(f.getName());
						
						if (flt != null && currentFilters.contains(flt)) {
							fItr.remove();
						}
					}
					
					if (currentFields.size() == 0) {
						// 找不到需要的欄位，就根據上下文（上一次要求的欄位）
						if (oldFieldsSize > 0) {
							currentFields.addAll(oldFields);
						}
						else {
							// 沒有 oldFields，就用 default
							for (Field f: fields) {
								if (f.getDefaultShow() != null && f.getDefaultShow().booleanValue()) {
									currentFields.add(f);
								}
							}
						}
					}
					else {
						Field firstDefaultField = fields.get(0);
						
						if (firstDefaultField != null) {
							boolean alreadyHas = false;
							
							for (Field f: currentFields) {
								if (f.getName().equals(firstDefaultField.getName())) {
									alreadyHas = true;
									break;
								}
							}
							
							if (!alreadyHas) {
								currentFields.add(0, firstDefaultField);
							}
						}
					}
					
					if (oldFieldsSize != currentFields.size()) {
						hasNewMatch = true;
					}
					
					/*if (currentFields.size() == 0 && oldFieldsSize > 0) {
						currentFields = oldFields;
					}*/

					PostRuleCheckResult aRes = (PostRuleCheckResult)checkNormalTrigger("beforeAggregateData", ctx, this, normalTriggers);
					if (aRes != null && aRes == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return aRes;
					
					if (hasNewMatch || doSearchAndListResult) {
						boolean appendDocList = false;
						
						if (!hasNewMatch) {
							String answer = "";
							
							if (currentFields.size() > 0) {
								answer = "請選擇明細資料：(請用前方的數字選擇您要查詢資料)";
								//提供給您全部的保單資料: (請用前方的數字選擇您要查詢的保單明細，或輸入「全部保單」取得全部保單資訊)
								//(1). 保單號碼：9023157990 / 商品名稱：金好康養老保險 / 契約效力：非終身險滿期 / 繳別：月繳 / 下次應繳日：1911-01-01
								//(2). 保單號碼：9022594459 / 商品名稱：金好康養老保險 / 契約效力：非終身險滿期 / 繳別：月繳 / 下次應繳日：1911-01-01
							}
							else {
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
									appendDocList = true;
								}
							}
						}

						if (hasNewMatch || appendDocList) {
							
							try {
								searchAndApplyFilters(ctx);
							} catch (Exception e) {
								ctx.setAnswerText(convertText(ctx, "目前無法取得您的資料，請稍後再試或與客服聯繫，您的錯誤訊息為：" + e.getMessage()));
								ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
								ctx.setResponseAttribute("DIALOG", getName());
								return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
							}
							
							PostRuleCheckResult srRes = (PostRuleCheckResult)checkNormalTrigger("beforeShowingResult", ctx, this, normalTriggers);
							if (srRes != null && srRes == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return srRes;
							
							if (StringUtils.isEmpty(showSearchResultScript) && qaPatternMkey != null) {
								StringWriter buf = new StringWriter();
								QAOutputResult r = new QAOutputResult();
								
								try {
									//取得velocity的上下文context
									VelocityContext context = new VelocityContext();
									try {
										//QueryResponse res = QADataAggregator.getDataSourceServer(qp.getDataSource(), ctx.getTenant()).query(query);
										//SolrDocumentList docs = res.getResults();
										
										if (currentDocs != null && currentDocs.size() > 0) {
											r.setHasResult(true);
											SolrDocument firstDoc = currentDocs.get(0);
											List<String> visibleFn = new ArrayList<String>();
											
											for (String fn: firstDoc.getFieldNames()) {
												//if (!fn.endsWith("_s") && !fn.endsWith("_ms")) continue;
												if (fn.equalsIgnoreCase("dataType_s")) continue;
												visibleFn.add(fn);
											}
											context.put("fields", visibleFn);
										}
										else {
											currentDocs = null;
											r.setHasResult(false);
										}
										
										ctx.setCtxAttr(name, this); // 這個是存入 QAContext 的 CtxAttr，方便存取
										
										//把數據填入上下文
										context.put("docs", currentDocs);
										context.put("currentDocOffset", currentDocOffset);
										context.put("question", ctx.getCurrentQuestion());
										context.put("qaCtx", ctx);
										context.put("math", new MathTool());
										context.put("num", new NumberTool());
										context.put("date", new DateTool());
										context.put("colMapper", ColumnNameMappingFacade.getInstance());
									}
									catch (Exception e) {
										e.printStackTrace();
									}

									// 用特殊問法輸出（這裡的角色幾乎快沒有了）
									QAPattern qp = QAPattern.getByKey(ctx.getTenant().getId(), qaPatternMkey);
									VelocityEngine ve = new VelocityEngine();
				
									// Make Velocity log to Log4J
									ve.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
									ve.setProperty("runtime.log.logsystem.log4j.category", this.getClass().getName());
									
									ve.init();
									ve.evaluate(context, buf, this.getClass().getName(), qp.getAnswerTemplate());
								} catch (Exception e) {
									e.printStackTrace();
								}
									
								String ans = buf.toString();
								if (showCurrentStatusWhenSearching) {
									ans = showCurrentStatusWhenSearchingText(ctx) + NL + ans;
								}
								
								if (appendDocList) {
									ctx.appendAnswerText(NL);
									ctx.appendAnswerText(ctx.isClientSupportHtml() ? WiSeUtils.nl2br(ans) : ans);
								}
								else {
									ctx.setAnswerText(ctx.isClientSupportHtml() ? WiSeUtils.nl2br(ans) : ans);
								}
								ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
								ctx.setResponseAttribute("DIALOG", getName());
								status = InnerStatus.CHECK_INTENT_AND_ENTITY;
							}
							else if (StringUtils.isNotEmpty(showSearchResultScript)) {
								// set Answer
								Binding binding = new Binding();
								binding.setProperty("ctx", ctx);
								binding.setProperty("dlg", this);
								binding.setProperty("docs", currentDocs);
								binding.setProperty("NL", NL);
								binding.setProperty("currentFilters", currentFilters);
								binding.setProperty("currentFields", currentFields);
								
								String answer = (String)GroovyUtil.runScript(binding, getShowSearchResultScript());
								
								if (ctx.hasAnswerText()) {
									ctx.appendAnswerText(NL);
									ctx.appendAnswerText(answer);
								}
								else {
									ctx.setAnswerText(answer);
								}
								ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
								ctx.setResponseAttribute("DIALOG", getName());
								status = InnerStatus.CHECK_INTENT_AND_ENTITY;
							}
							else {
								String newQ = "";
								for (Filter filter: currentFilters) {
									newQ += filter.getCurrentValue();
								}
								
								System.out.println("OutterSourceRobotDialog newQ:[" + newQ + "]");
								
								String respStr = WiSeUtils.getDataFromUrl(ctx.getTenant()
										.getOuterSourceUrl()+"&noRecommand=true&id="+ctx.getContextId()+"&q="+URLEncoder.encode(newQ,"utf-8"));
								JSONObject resp = new JSONObject(respStr);
								if (resp.has("answerType") && resp.get("answerType").equals("PROFESSIONAL") && resp.has("output")) {
									String answer = (String) resp.get("output");

									if (ctx.hasAnswerText()) {
										ctx.appendAnswerText(NL);
									} 
									
									ctx.appendAnswerText(ctx.isClientSupportHtml() ? WiSeUtils.nl2br(answer) : answer);
									ctx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
									ctx.setHasDirectAnswer(true, QAContext.ANSWER_TYPE.PROFESSIONAL);
									return new PostRuleCheckResult(
											PostRuleCheckResult.Status.FORWARD,
											QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
								}else {
									return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
								}
							}
							
							return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_PLUGIN_ONLY_CHAIN);
						}
						else {
							// 沒有答案
							return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_POST_QA_MATCH);
						}
					}
					else {
						return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
					}
				} catch (Exception e) {
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
		}
		finally {
			if (ctx.getCtxAttr("activeDialog") == this)
				ctx.setCtxAttr("activeDialog", null);
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
					QADialog ffd = new OutterSourceRobotDialog();
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
	
	boolean findAndGatherTargetFields(QAContext ctx,
			List<Field> allFields, List<Field> targetFields, Set<QAEntity> entities) {
		boolean hasNewField = false;
		
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
							
							hasNewField = true;
						}
					}
				}
			}
		}
		
		return hasNewField;
	}
	
	boolean findAndGatherFilters(QAContext ctx, List<Filter> allFilters, List<Filter> targetFilters) {
		boolean hasNewFilter = false;
		int currentFiltersSize = targetFilters.size();
		Filter nameFilter = getFilter("insuranceName");
		Filter insuranceNoFilter = getFilter("insuranceNo");
		boolean hasNameFilter = false;
		boolean hasInsuranceNoFilter = false;

		Collection<QAEntity> entityDbs = (Collection<QAEntity>)ctx.getRequestAttribute("entities");
		
		for (QAEntity eid: entityDbs) {
			if (eid.getEntityValues() != null) {
				Filter f = getFilter(eid.getCode());
				
				if (targetFilters.contains(f)) {
					targetFilters.remove(f);
				}
				
				f.setCurrentValue(eid.getEntityValues());
				targetFilters.add(f);
				hasNewFilter = true;
			}
		}
		
		for (Filter f: allFilters) {
			if (f.getLocked()) continue; 
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
		
		for (Filter f: allFilters) {
			if (targetFilters.contains(f)) {
				targetFilters.remove(f);
				targetFilters.add(f);
			}
		}
		
		
		/*if (currentFiltersSize >= targetFilters.size()) {
			// 試著找保單名稱
			String q = StringUtils.trimToEmpty(ctx.getOriginalQuestion());
			
			q = q.replaceAll("^(我想查|那)", "");
			q = q.replaceAll("(哪一?張|那一?張|什麼的|開頭的)(那一?張|保單)?(呢|咧)?$", "");
			
			if (allDocsOfThisUser != null && q.length() >= 2) {
				for (SolrDocument doc: allDocsOfThisUser) {
					String name = (String)doc.getFirstValue("ProductName_s");
					
					if (StringUtils.containsIgnoreCase(name, q)
							|| StringUtils.containsIgnoreCase(q, name)) {
						RuleCheckResult result = new RuleCheckResult(RuleCheckResultStatus.MATCH, name, nameFilter.getRules().get(0));
						nameFilter.setCurrentResult(result);

						if (targetFilters.contains(nameFilter)) {
							targetFilters.remove(nameFilter);
						}
						targetFilters.add(nameFilter);
						hasNameFilter = true;
						hasNewFilter = true;
					}
				}
			}
		}
		
		if (hasNameFilter && targetFilters.contains(insuranceNoFilter)) {
			targetFilters.remove(insuranceNoFilter);
		}
		if (hasInsuranceNoFilter && targetFilters.contains(nameFilter)) {
			targetFilters.remove(nameFilter);
		}*/
		
		return hasNewFilter;
	}
	
	SolrDocumentList searchAndApplyFilters(QAContext ctx) {
		SolrDocumentList docs = new SolrDocumentList();
		if (qaPatternMkey != null) {
			//QueryResponse res = QADataAggregator.getDataSourceServer(qp.getDataSource(), ctx.getTenant()).query(query);
			//SolrDocumentList docs = res.getResults();
			QAPattern qp = QAPattern.getByKey(ctx.getTenant().getId(), qaPatternMkey);
			
			Class aggregatorClazz = QADataAggregator.findRegisteredClass(qp.getDataAggregator());
			try {
				QADataAggregator aggregator = (QADataAggregator) aggregatorClazz.newInstance();
				SolrQuery query = aggregator.generateQuery(null, qp, ctx, new ArrayList<CustomData>(), new AtomicBoolean());
				
				for (Filter f: currentFilters) {
					RuleCheckResult result = f.getCurrentResult();
					
					if (result != null) {
						Rule r = result.getRule();
						
						if (r instanceof SearchableRule) {
							SearchableRule sr = (SearchableRule)r;
							sr.applySearchCriteria(result, query);
						}
						
						for (Rule rr: f.getRules()) {
							if (rr instanceof SortRule) {
								((SortRule) rr).applySearchCriteria(result, query);
							}
						}
					}
					else if (f.getCurrentValue() != null) {
						String val = f.getCurrentValue();
						boolean partial = true;
						if (StringUtils.equalsIgnoreCase("integer", f.getType())) {
							val = val.replaceAll("[^0-9\\-\\+\\.]", "");
							partial = false;
						}
						new SearchableHelper().applySearchCriteria(val, query, "filterQuery", "boolean", f.getTargetIndexFieldName(), partial);
					}
				}
				
				SolrServer server = QADataAggregator.getDataSourceServer(qp.getDataSource(), ctx.getTenant());
				QueryResponse res = server.query(query);
				docs = res.getResults();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		else {
			if (allDocsOfThisUser == null) {
				SolrServer server = null;
	
				if (httpSolrServer == null) {
					server = ctx.getTenant().getCoreServer();
				}
				else {
					server = SearchManager.getRemoteServer(httpSolrServer);
				}
				
				SolrQuery sq = new SolrQuery();
				sq.setQuery("*:*");
				sq.addFilterQuery("dataType_s:" + name);
				sq.setRows(1000);
				
				try {
					allDocsOfThisUser = server.query(sq).getResults();
				} catch (SolrServerException e) {
					e.printStackTrace();
					allDocsOfThisUser = new SolrDocumentList();
				}
			}
			docs.addAll(allDocsOfThisUser);
			
			for (Filter f: currentFilters) {
				RuleCheckResult result = f.getCurrentResult();
				
				if (result != null) {
					Rule r = result.getRule();
					
					if (r instanceof SearchableRule) {
						SearchableRule sr = (SearchableRule)r;
						SolrQuery query = new SolrQuery();
						sr.applySearchCriteria(result, docs);
					}
					
					for (Rule rr: f.getRules()) {
						if (rr instanceof SortRule) {
							((SortRule) rr).applySearchCriteria(result, docs);
						}
					}
				}
				else if (f.getCurrentValue() != null) {
					String val = f.getCurrentValue();
					boolean partial = true;
					if (StringUtils.equalsIgnoreCase("integer", f.getType())) {
						val = val.replaceAll("[^0-9\\-\\+\\.]", "");
						partial = false;
					}
					new SearchableHelper().applySearchCriteria(val, docs, "filterQuery", "boolean", f.getTargetIndexFieldName(), partial);
				}
			}
		}
		
		if (docs != null) {
			currentDocs = docs;
			currentDocs.setNumFound(docs.size());
		}
		if (docs.size() == 1) {
			currentDocOffset = 0;
		}
		
		return docs;
	}

	public String showCurrentStatusWhenSearchingText(QAContext ctx) {
		String res = (String)checkNormalTrigger("replaceShowCurrentStatusWhenSearchingText", ctx, this, normalTriggers);
		if (res != null) return res;
		
		Pattern p = Pattern.compile("(?si)\\$\\{fields#(?<separator>.*?)#(?<allText>.*?)\\}");
		Matcher m = p.matcher(showCurrentStatusWhenSearchingText);
		String separator = "，";
		String allText = "全部";
		
		if (m.find()) {
			separator = m.group("separator");
			allText = m.group("allText");
			StringBuilder b = new StringBuilder();
			
			for (Field f: fields) {
				if (!f.isHidden() && f.getCurrentValue() != null) {
					if (b.length() > 0) b.append(separator);
				
					if (f.getName().equals("DMFlag"))
						b.append("未繳費");
					else 
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

		if (callStack.size() == 0) {
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
		return super.postCheck(ctx, resultFromCheck);
	}

	@Override
	public void reset() {
		currentField = null;
		currentFieldOffset = null;
		
		for (Field f: fields) {
			f.setCurrentValue(null);
			f.setCurrentResult(null);
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
