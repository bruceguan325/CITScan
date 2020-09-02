package com.intumit.solr.robot.qadialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
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
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QAMatchRuleController;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.mchange.io.FileUtils;

import flexjson.JSONDeserializer;
import flexjson.JSONSerializer;
import groovy.lang.Binding;

/**
 * 這個是利用存在索引內的 dataType:insurance 資料來實作
 * (範例資料在 sample/insurance.csv）
 * 
 * @author herb
 */
public class InsuranceFeeListSearchDialog extends QADialog {
	List<Trigger> enterTriggers = new ArrayList<Trigger>();
	List<Trigger> quitTriggers = new ArrayList<Trigger>();
	List<Trigger> normalTriggers = new ArrayList<Trigger>();
	String qaPatternMkey = null;
	String quitText = null;
	String showCurrentStatusWhenSearchingText = null;
	String showSearchResultScript = null;
	boolean showCurrentStatusWhenQuitting = false;
	boolean showCurrentStatusWhenSearching = false;
	boolean runPostMatchChain = false;
	boolean continueQAruleWhenNothingToDo = false;
	boolean doSearchAndListResult;
	boolean hasNewMatch;
	
	Integer currentFieldOffset = null;
	Field currentField = null;
	Integer currentDocOffset = -1;
	SolrDocumentList currentDocs = null;

	List<Filter> currentFilters = new ArrayList<Filter>();
	List<Field> currentFields = new ArrayList<Field>();
	
	String[] userNameList = new String[] {
		"陳小姐",
		"陳先生",
		"黃小姐",
		"黃先生",
		"李小姐",
		"林先生",
		"方小姐",
		"鐘先生",
		"鐘小姐",
		"方先生",
		"江小姐",
		"江先生",
		"蔣小姐",
		"蔣先生",
		"楊小姐",
		"楊先生",
		"張小姐",
		"張先生",
		"蘇小姐",
		"蘇先生",
		"邱小姐",
		"邱先生",
		"王小姐",
		"王先生",
		"周小姐",
		"周先生",
		"宋小姐",
		"宋先生",
		"鄭小姐",
		"鄭先生",
		"趙小姐",
		"趙先生",
		"吳小姐",
		"吳先生",
		"許小姐",
		"許先生",
	};
	SingleDocDialog detailDialogForCheckRule = null;
	
	Map<String, String> fieldNameMapping = new HashMap<String, String>();
	{
		fieldNameMapping.put("PREM_TRN_CODE", "PaymentMethod_s");
		fieldNameMapping.put("SEC_ISD_NM", "Applicant_s");
		fieldNameMapping.put("INSD_NM", "Cestuiquevie_s");
		fieldNameMapping.put("PAY_TIMES", "Number_of_payments_s");
		fieldNameMapping.put("PROD_ID", "ProductId_s");
		fieldNameMapping.put("PAY_FREQ", "period_s");
		fieldNameMapping.put("POLICY_NO", "InsuranceNo_s");
		fieldNameMapping.put("EFT_CODE", "effectiveness_s");
		
		// 日期類
		fieldNameMapping.put("ISSUE_DATE", "IssueDate_dt");
		fieldNameMapping.put("MNXT_PAY_DATE", "NextPaymentDate_dt");
		fieldNameMapping.put("LST_PAY_DATE", "LastPaymentDate_dt");
	}
	
	Map<String, String> periodMapping = new HashMap<>();
	{
		periodMapping.put("1", "月繳");
		periodMapping.put("2", "季繳");
		periodMapping.put("3", "半年繳");
		periodMapping.put("4", "年繳");
		periodMapping.put("5", "躉繳");
		periodMapping.put("6", "彈性繳");
	}
	
	Map<String, String> effectivenessMapping = new HashMap<>();
	{
		effectivenessMapping.put("0", "正常");
		effectivenessMapping.put("1", "停效");
		effectivenessMapping.put("2", "終身險滿期");
		effectivenessMapping.put("4", "解約辦理中(投資型)");
		effectivenessMapping.put("13", "發函解除");
		effectivenessMapping.put("14", "死殘辦理中");
		effectivenessMapping.put("20", "展期件");
		effectivenessMapping.put("21", "展期滿期件");
		effectivenessMapping.put("23", "繳清件");
		effectivenessMapping.put("24", "繳清滿期件");
		effectivenessMapping.put("30", "解約");
		effectivenessMapping.put("31", "合意解除");
		effectivenessMapping.put("32", "契約終止件(墊繳)");
		effectivenessMapping.put("33", "契約終止件(貸款)");
		effectivenessMapping.put("34", "契撤");
		effectivenessMapping.put("35", "契約終止(99 or 105歲滿期)");
		effectivenessMapping.put("36", "非終身險滿期");
		effectivenessMapping.put("37", "註銷");
		effectivenessMapping.put("38", "終止");
		effectivenessMapping.put("40", "主被保險人身故(壽險)");
		effectivenessMapping.put("41", "次被保險人身故(年金)");
		effectivenessMapping.put("42", "主被保險人身故(年金)");
		effectivenessMapping.put("43", "主被保險人重大疾病契約終止");
		effectivenessMapping.put("44", "主被保險人殘廢契約終止");
		effectivenessMapping.put("45", "主被保險人生命末期契約終止");
		effectivenessMapping.put("46", "主被保險人醫療契約終止");
		effectivenessMapping.put("47", "防癌主約雙親型本人死亡");
		effectivenessMapping.put("48", "死殘通知件");
		effectivenessMapping.put("50", "主被保險人全殘");
		effectivenessMapping.put("51", "次被保險人全殘(年金)");
		effectivenessMapping.put("52", "主被保險人全殘(年金)");
		effectivenessMapping.put("53", "長期看護");
		effectivenessMapping.put("54", "重大疾病");
		effectivenessMapping.put("61", "貸款停效");
		effectivenessMapping.put("62", "保價返還解約");
	}

	public InsuranceFeeListSearchDialog() {
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
	}
	
	@Override
	public void loadConfig() {
		super.loadConfig();
		try {
			quitText  = config.optString("quitText");
			qaPatternMkey  = config.optString("qaPatternMkey");
			runPostMatchChain = config.optBoolean("runPostMatchChain", Boolean.FALSE);
			continueQAruleWhenNothingToDo = config.optBoolean("continueQAruleWhenNothingToDo", Boolean.FALSE);
			showCurrentStatusWhenQuitting = config.optBoolean("showCurrentStatusWhenQuitting", Boolean.FALSE);
			showCurrentStatusWhenSearching = config.optBoolean("showCurrentStatusWhenSearching", Boolean.FALSE);
			showCurrentStatusWhenSearchingText  = config.optString("showCurrentStatusWhenSearchingText");
			showSearchResultScript  = config.optString("showSearchResultScript");
			
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

	public boolean isRunPostMatchChain() {
		return runPostMatchChain;
	}

	public void setRunPostMatchChain(boolean runPostMatchChain) {
		this.runPostMatchChain = runPostMatchChain;
	}

	public boolean isContinueQAruleWhenNothingToDo() {
		return continueQAruleWhenNothingToDo;
	}

	public void setContinueQAruleWhenNothingToDo(boolean matchQaWhenNothingToDo) {
		this.continueQAruleWhenNothingToDo = matchQaWhenNothingToDo;
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
	private Field idField;

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
					
					if (stackData.to.getName().equals("InsuranceFeeDetail")) {
						String returnType = (String)ctx.getCtxAttr("InsuranceFeeDetail.returnType");
						
						if ("QUIT".equals(returnType) || returnType == null) {
						ctx.setCtxAttr(name, null); // 這個是存入 QAContext 的 CtxAttr，方便存取
		
						// set Answer
						ctx.setAnswerText(showCurrentStatusWhenQuitting ? (showCurrentStatusText() + NL) : "" + StringUtils.trimToEmpty(quitText));
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.setResponseAttribute("DIALOG", getName());
						deactivate();
						
						return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
						else if ("ALL_INSURANCE_FEE".equals(returnType)) {
							ctx.setCtxAttr(name, null); // 這個是存入 QAContext 的 CtxAttr，方便存取
			
							for (Field f: fields) {
								if (!f.getLocked()) {
									f.setCurrentValue(null);
									f.setCurrentResult(null);
								}
							}
							doSearchAndListResult = true;
							status = InnerStatus.CHECK_INTENT_AND_ENTITY;
							ctx.setIntents(findIntent(ctx, "FIND_INSURANCE_FEE"));
							ctx.setEntities(findEntity(ctx, name, "ALL_INSURANCE"));
						}
					}
					else if (stackData.to.getName().equals("AccountVerification")) {
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
						
						idField = stackData.to.getField("id");
						
						// 如果沒有 trigger quit，就走老路徑
						ctx.setIntents(findIntent(ctx, "SEARCH"));
						ctx.setEntities(findEntity(ctx, name, "ALL_INSURANCE"));
						ctx.setAnswerText("");
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
			
			if (doSearchAndListResult || status == InnerStatus.CHECK_INTENT_AND_ENTITY) {
				PostRuleCheckResult brpRes = (PostRuleCheckResult)checkNormalTrigger("beforeRuleProcess", ctx, this, normalTriggers);
				if (brpRes != null && brpRes == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return brpRes;

				try {
					Set<QAEntity> entities = (Set<QAEntity>)ctx.getRequestAttribute("entities");
					
					if (findAndGatherFilters(ctx, filters, currentFilters, entities)) {
						hasNewMatch = true;
					}
					
					List<Field> oldFields = new ArrayList<Field>();
					oldFields.addAll(currentFields);
					
					currentFields.clear(); // 目前先每次清掉要查詢的欄位
					int currentFieldsSize = currentFields.size();
					
					if (entities != null && entities.size() > 0) {
						findAndGatherTargetFields(ctx, fields, currentFields, entities);
					}

					// 怕之前的設定太寬衝突到，所以多了一些限制，理論上應該要拿掉
					if (currentFieldsSize == currentFields.size()) {
						for (Field f: getFields()) {
							if (f.getLocked()) continue; 
						RuleCheckResult result = f.checkRules(ctx);
						
						if (result.isMatch()) {
								f.setCurrentResult(result);
								currentFields.add(f);
							}
						}
					}
					
					// 檢查如果同名的 field 已經有 apply filter，就不要顯示這一欄，不然很乾
					Iterator<Field> fItr = currentFields.iterator();
					while (fItr.hasNext()) {
						Field f = fItr.next();
						Filter flt = getFilter(f.getName());
						
						if (flt != null && currentFilters.contains(flt)) {
							fItr.remove();
						}
					}
					
					if (currentFieldsSize < currentFields.size()) {
							hasNewMatch = true;
						}
					
					if (currentFields.size() == 0 && oldFields.size() > 0) {
						currentFields = oldFields;
					}

					PostRuleCheckResult aRes = (PostRuleCheckResult)checkNormalTrigger("beforeAggregateData", ctx, this, normalTriggers);
					if (aRes != null && aRes == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return aRes;
					
					if (hasNewMatch || doSearchAndListResult || !continueQAruleWhenNothingToDo) {
						boolean appendDocList = false;
						
						if (!hasNewMatch) {
							String answer = "";
							
							if (currentFields.size() > 0) {
								answer = "請選擇一張保單查詢明細資料：(請用前方的數字選擇您要查詢的保單明細，或輸入「全部保單」取得全部保單資訊)";
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
								ctx.setAnswerText(convertText(ctx, "目前無法取得您的保單資料，請稍後再試或與客服聯繫，您的錯誤訊息為：" + e.getMessage()));
								ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
								ctx.setResponseAttribute("DIALOG", getName());
								return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
						}
							
							PostRuleCheckResult srRes = (PostRuleCheckResult)checkNormalTrigger("beforeShowingResult", ctx, this, normalTriggers);
							if (srRes != null && srRes == PostRuleCheckResult.DEFAULT_RETURN_RESULT) return srRes;
							
							if (StringUtils.isEmpty(showSearchResultScript)) {
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
												//if (fn.equalsIgnoreCase("dataType_s")) continue;
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
							else {
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
						
						if (runPostMatchChain) {
								return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_PLUGIN_ONLY_CHAIN);
							}
							else {
								return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
							}
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
					QADialog ffd = new InsuranceFeeListSearchAPIDialog();
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

	void findAndGatherTargetFields(QAContext ctx,
			List<Field> allFields, List<Field> targetFields, Set<QAEntity> entities) {
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
		
		if (currentFiltersSize >= targetFilters.size()) {
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
		}
		
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
				server = ctx.getTenant().getCoreServer();
				
				SolrQuery sq = new SolrQuery();
				sq.setQuery("*:*");
				sq.addFilterQuery("UserId_s:" + idField.getCurrentValue().toUpperCase());
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
	
	

	
	public static Map<String, String> prdNameMap = new HashMap<String, String>();
	static {
		prdNameMap.put("AA","新傷特死殘");
		prdNameMap.put("AB","新傷特住院");
		prdNameMap.put("AC","新傷特門診");
		prdNameMap.put("AD","新家特死殘");
		prdNameMap.put("AE","新家特住院");
		prdNameMap.put("AF","新家特本人門診");
		prdNameMap.put("AG","新家特眷屬門診");
		prdNameMap.put("AH","住院醫療（小護士）");
		prdNameMap.put("AI","住院醫療（小護士）");
		prdNameMap.put("AJ","住院醫療（小護士）");
		prdNameMap.put("AK","定期特約（第二回）");
		prdNameMap.put("AL","配偶定期特約（二）");
		prdNameMap.put("AM","防癌終身－個人型");
		prdNameMap.put("AN","防癌終身－單親型");
		prdNameMap.put("AO","防癌終身－雙親型");
		prdNameMap.put("AP","定期特約（第三回）");
		prdNameMap.put("AQ","配偶定期（第三回）");
		prdNameMap.put("AR","高額住院醫療本人");
		prdNameMap.put("AS","高額住院醫療－配偶");
		prdNameMap.put("AT","高額住院醫療－子女");
		prdNameMap.put("AU","新傷特限額");
		prdNameMap.put("AV","新家特限額");
		prdNameMap.put("AW","新主人翁－死殘");
		prdNameMap.put("AX","新主人翁－住院");
		prdNameMap.put("AY","住院醫療日額");
		prdNameMap.put("AZ","傷害失能附約");
		prdNameMap.put("BA","溫心住院");
		prdNameMap.put("BB","平安附約－死殘");
		prdNameMap.put("BC","平安附約－住院");
		prdNameMap.put("BD","新兒童傷害－甲型");
		prdNameMap.put("BE","新兒童傷害－乙型");
		prdNameMap.put("BF","全意住院");
		prdNameMap.put("BG","全心住院日額");
		prdNameMap.put("BH","新全意住院");
		prdNameMap.put("BI","金平安傷害附約死殘");
		prdNameMap.put("BJ","金平安住院");
		prdNameMap.put("BK","金平安每次醫療限額");
		prdNameMap.put("BM","新寶貝傷害－Ａ型");
		prdNameMap.put("BN","定期１００壽險附約");
		prdNameMap.put("BO","全方位死殘");
		prdNameMap.put("BP","全方位傷害醫療");
		prdNameMap.put("BQ","全方位醫限－有社保");
		prdNameMap.put("BR","全方位醫限－無社保");
		prdNameMap.put("BS","全方位傷害醫療擇優");
		prdNameMap.put("BT","定期１００");
		prdNameMap.put("BU","新定期１００附約");
		prdNameMap.put("BV","好骨力傷害保險附約");
		prdNameMap.put("BW","乖寶貝健康附約");
		prdNameMap.put("BX","全安心附約");
		prdNameMap.put("BY","全安心附約");
		prdNameMap.put("BZ","全安心附約");
		prdNameMap.put("CA","熱火青春傷害保險附約");
		prdNameMap.put("CB","鍾安特定傷病定期保險");
		prdNameMap.put("CE","快樂ＧＯ交通意外附約");
		prdNameMap.put("CF","新全方位死殘");
		prdNameMap.put("CG","新全方位傷害醫療");
		prdNameMap.put("CH","新全方位醫限－有健保");
		prdNameMap.put("CJ","新全方位醫限－無健保");
		prdNameMap.put("CK","新全方位傷害醫療擇優");
		prdNameMap.put("CL","新康愛防癌附約");
		prdNameMap.put("CM","新ＧＯ保障１００附約");
		prdNameMap.put("CN","真全意住院");
		prdNameMap.put("CO","特定處置附加條款");
		prdNameMap.put("CP","好事成雙");
		prdNameMap.put("CQ","永健住院日額");
		prdNameMap.put("CR","樂活照護定期健康附約");
		prdNameMap.put("CS","樂活照護提前給付條款");
		prdNameMap.put("CT","新乖寶貝健康附約");
		prdNameMap.put("CU","ＧＯ安心美元附約");
		prdNameMap.put("CV","新真全意住院");
		prdNameMap.put("CW","新快樂ＧＯ傷害附約");
		prdNameMap.put("CX","新熱火青春傷害保險附");
		prdNameMap.put("CY","心鑫相連");
		prdNameMap.put("CZ","ＧＯ保障１００附約");
		prdNameMap.put("DF","真乖寶貝健康附約");
		prdNameMap.put("EA","養老保險");
		prdNameMap.put("EB","子女教育保險");
		prdNameMap.put("EC","福祿終身");
		prdNameMap.put("ED","幸福養老");
		prdNameMap.put("EE","長壽７４╱２╱１日前");
		prdNameMap.put("EF","福安");
		prdNameMap.put("EG","夫妻儲蓄壽險");
		prdNameMap.put("EI","福壽７４╱２╱１前");
		prdNameMap.put("EJ","萬福７４╱２╱１前");
		prdNameMap.put("EK","萬全（幸安）");
		prdNameMap.put("EL","萬安");
		prdNameMap.put("EM","萬壽");
		prdNameMap.put("EN","增值分紅養老");
		prdNameMap.put("EO","兩全得利利變");
		prdNameMap.put("EP","青春險（甲）");
		prdNameMap.put("EQ","青春險（乙）");
		prdNameMap.put("ER","婦女");
		prdNameMap.put("ES","增添采終身壽險");
		prdNameMap.put("ET","教育年金");
		prdNameMap.put("EU","珍鑫福終身壽險");
		prdNameMap.put("EV","歡喜年年終身保險");
		prdNameMap.put("EW","增值１２３養老年滿");
		prdNameMap.put("EX","ＧＯ健康住院醫療定期");
		prdNameMap.put("EY","貸款償還－平準型");
		prdNameMap.put("EZ","貸款償還－遞減型");
		prdNameMap.put("FA","增值年金");
		prdNameMap.put("FB","龍鳳教育年金");
		prdNameMap.put("FC","１２３增值");
		prdNameMap.put("FD","鑽石");
		prdNameMap.put("FE","第二回長春");
		prdNameMap.put("FF","２１世紀終身");
		prdNameMap.put("FG","新安家終身");
		prdNameMap.put("FH","新青春險（甲）");
		prdNameMap.put("FI","新青春險（乙）");
		prdNameMap.put("FJ","萬代福１０１");
		prdNameMap.put("FK","新遨遊千里傷害醫療");
		prdNameMap.put("FL","萬代福２１１");
		prdNameMap.put("FM","新婦女增額");
		prdNameMap.put("FN","第二回定期保險");
		prdNameMap.put("FO","第三回長春");
		prdNameMap.put("FP","第三回新青春險（甲）");
		prdNameMap.put("FQ","第三回新青春險（乙）");
		prdNameMap.put("FR","定期保險（第三回）");
		prdNameMap.put("FS","第三回貸款償還平準型");
		prdNameMap.put("FT","第三回貸款償還遞減型");
		prdNameMap.put("FU","新安心醫療帳戶終身保");
		prdNameMap.put("FV","新真安心住院醫療終身");
		prdNameMap.put("FX","真守護平安終身保險");
		prdNameMap.put("FY","心鑫相連壽險");
		prdNameMap.put("FZ","金鍾照特定傷病定期");
		prdNameMap.put("GA","富貴增值年金");
		prdNameMap.put("GB","龍鳳育英壽險");
		prdNameMap.put("GC","富貴１２３增額保險");
		prdNameMap.put("GD","澳利富外幣變額年金");
		prdNameMap.put("GE","長青保險");
		prdNameMap.put("GF","年年春終身壽險");
		prdNameMap.put("GG","簡利養老");
		prdNameMap.put("GH","主人翁");
		prdNameMap.put("GJ","樂轉健康住院醫療終身");
		prdNameMap.put("GK","樂轉人生（年給付）");
		prdNameMap.put("GL","添壽增額終身");
		prdNameMap.put("GM","添福增額終身");
		prdNameMap.put("GN","美事年年利變");
		prdNameMap.put("GO","美利多利變美元");
		prdNameMap.put("GP","Ｅ路平安");
		prdNameMap.put("GQ","Ｅ把罩定期壽險");
		prdNameMap.put("GR","佳美年年利變");
		prdNameMap.put("GS","儲蓄計劃保險");
		prdNameMap.put("GT","ＥＡＳＹ保終身保險");
		prdNameMap.put("GU","樂享人生變額年金");
		prdNameMap.put("GV","醫定愛保本住院");
		prdNameMap.put("GW","新主人翁兒童保險");
		prdNameMap.put("GX","醫定讚住院醫療終身");
		prdNameMap.put("GY","順心３６５醫療終身");
		prdNameMap.put("HA","美滿人生１０１終身");
		prdNameMap.put("HB","美滿人生３１２");
		prdNameMap.put("HC","萬代福２０１");
		prdNameMap.put("HD","樂轉守護長期看護");
		prdNameMap.put("HE","添喜增額終身壽險");
		prdNameMap.put("HF","防癌終身個人型");
		prdNameMap.put("HG","添吉增額終身");
		prdNameMap.put("HH","添祥增額終身");
		prdNameMap.put("HI","保本１０１終身");
		prdNameMap.put("HJ","萬代福２０２終身");
		prdNameMap.put("HK","美滿人生２０２終身");
		prdNameMap.put("HL","鍾愛一生３１３");
		prdNameMap.put("HM","保本１１１終身");
		prdNameMap.put("HN","富貴保本三福");
		prdNameMap.put("HO","新兩全得利利變");
		prdNameMap.put("HP","一代情１１０養老保險");
		prdNameMap.put("HQ","富貴喜福終身");
		prdNameMap.put("HR","新兩全其美利變美元");
		prdNameMap.put("HS","新美事年年利變");
		prdNameMap.put("HT","長期看護終身壽險");
		prdNameMap.put("HU","遞延年金平準型");
		prdNameMap.put("HV","新美利多利變美元");
		prdNameMap.put("HW","遞延年金－遞增");
		prdNameMap.put("HX","新鑫美利利變養老保險");
		prdNameMap.put("HY","新金采滿利利變");
		prdNameMap.put("HZ","新鑫中國利變保險");
		prdNameMap.put("IB","月月享利外幣變額年金");
		prdNameMap.put("IC","樂享人生外幣變額年金");
		prdNameMap.put("ID","樂享人生變額壽險");
		prdNameMap.put("IE","超利多利變養老保險");
		prdNameMap.put("IF","金健康養老保險");
		prdNameMap.put("IG","呵護久久殘廢照護");
		prdNameMap.put("IH","躍健康終身保險");
		prdNameMap.put("II","多金富利變額壽險");
		prdNameMap.put("IJ","多金富利外幣變額年金");
		prdNameMap.put("IK","增添利利變");
		prdNameMap.put("IL","美利鍾樂美元終身保險");
		prdNameMap.put("IM","美夢成真養老保險");
		prdNameMap.put("IN","美夢成真養老歲滿");
		prdNameMap.put("IO","美利人生利變養老保險");
		prdNameMap.put("IP","新好骨力傷害保險附約");
		prdNameMap.put("IQ","珍美利２１５利變");
		prdNameMap.put("IR","新添采終身壽險＿定期");
		prdNameMap.put("IS","鑫經典１０１美元＿定");
		prdNameMap.put("IT","新美事年年利變月給付");
		prdNameMap.put("IU","好事年年終身保險");
		prdNameMap.put("IV","月月康利變額年金");
		prdNameMap.put("IW","月月康利外幣年金");
		prdNameMap.put("IX","享樂８８外幣變額壽險");
		prdNameMap.put("IY","享樂８８外幣變額年金");
		prdNameMap.put("IZ","富貴得利");
		prdNameMap.put("JA","真情１０１");
		prdNameMap.put("JB","鍾愛終身");
		prdNameMap.put("JC","兒童終身");
		prdNameMap.put("JE","富貴年年終身壽險");
		prdNameMap.put("JF","添尊增額");
		prdNameMap.put("JG","得意還本終身壽險");
		prdNameMap.put("JH","鍾愛還本終身壽險");
		prdNameMap.put("JI","親子教育甲型");
		prdNameMap.put("JJ","定期保險（第三回）");
		prdNameMap.put("JK","簡利養老");
		prdNameMap.put("JL","儲蓄計劃保險");
		prdNameMap.put("JM","新主人翁");
		prdNameMap.put("JN","長期看護終身壽險");
		prdNameMap.put("JO","鍾愛一生３１３");
		prdNameMap.put("JQ","住院醫療終身健康保險");
		prdNameMap.put("JR","一代情１１０養老保險");
		prdNameMap.put("JU","金如意養老保險");
		prdNameMap.put("JV","六六大順還本養老險");
		prdNameMap.put("JX","福本１１１終身");
		prdNameMap.put("JY","長壽１１１終身壽險");
		prdNameMap.put("JZ","添樂１１１");
		prdNameMap.put("KD","添寶養老");
		prdNameMap.put("KE","吉祥年年");
		prdNameMap.put("KH","長利終身壽險");
		prdNameMap.put("KJ","福壽增額終身");
		prdNameMap.put("KP","利變型年金（甲型）");
		prdNameMap.put("KQ","利變型年金（乙型）");
		prdNameMap.put("KR","國泰鴻運年年終身險");
		prdNameMap.put("KS","雙囍年年");
		prdNameMap.put("KU","新定期保險（第三回）");
		prdNameMap.put("KV","新鍾愛一生３１３");
		prdNameMap.put("KW","新一代情１１０");
		prdNameMap.put("KY","新鍾愛還本終身");
		prdNameMap.put("KZ","新長看");
		prdNameMap.put("LA","新添寶");
		prdNameMap.put("LB","新親子教育甲型");
		prdNameMap.put("LC","新住院醫療終身");
		prdNameMap.put("LD","新鍾愛終身");
		prdNameMap.put("LE","新金如意養老保險");
		prdNameMap.put("LF","新美滿人生１０１");
		prdNameMap.put("LG","新兒童終身");
		prdNameMap.put("LH","新添尊增額");
		prdNameMap.put("LI","新添樂１１１終身");
		prdNameMap.put("LJ","新吉祥年年");
		prdNameMap.put("LK","真情１０１");
		prdNameMap.put("LL","雙囍年年");
		prdNameMap.put("LM","鴻運年年");
		prdNameMap.put("LN","褔本１１１");
		prdNameMap.put("LO","長壽１１１終身壽險");
		prdNameMap.put("LP","新萬代褔１０１");
		prdNameMap.put("LS","雙褔年年");
		prdNameMap.put("LT","新簡利");
		prdNameMap.put("LW","新遞延年金平準");
		prdNameMap.put("LY","新遞延年金遞增");
		prdNameMap.put("MA","新雙囍年年終身壽險");
		prdNameMap.put("MB","新富貴年年終身壽險");
		prdNameMap.put("MC","真意世紀終身壽險");
		prdNameMap.put("MD","鍾意終身壽險");
		prdNameMap.put("ME","鍾意１０１終身壽險");
		prdNameMap.put("MF","全福１０１終身");
		prdNameMap.put("MG","添榮增值終身");
		prdNameMap.put("MH","添貴增值終身壽險");
		prdNameMap.put("MI","新得意還本終身壽險");
		prdNameMap.put("MJ","金滿意養老保險");
		prdNameMap.put("MK","安家長期看護終身壽險");
		prdNameMap.put("ML","得意年年終身壽險");
		prdNameMap.put("MM","金富裕１１０養老");
		prdNameMap.put("MO","添富養老保險");
		prdNameMap.put("MP","美意年年終身壽險");
		prdNameMap.put("MQ","鍾意３１３終身壽險");
		prdNameMap.put("MR","全祿定期保險");
		prdNameMap.put("MS","３１２還本終身壽險");
		prdNameMap.put("MT","安康住院醫療");
		prdNameMap.put("MV","長富遞延年金平準");
		prdNameMap.put("MX","長富遞延年金遞增");
		prdNameMap.put("MZ","金吉利養老保險");
		prdNameMap.put("NA","富本１１１終身壽險");
		prdNameMap.put("NB","新世紀遞延（甲型）");
		prdNameMap.put("NC","福本１０１終身");
		prdNameMap.put("ND","福祿增額終身壽險");
		prdNameMap.put("NE","雙運年年終身壽險");
		prdNameMap.put("NF","添寵終身壽險");
		prdNameMap.put("NG","添寵終身壽險");
		prdNameMap.put("NH","金富貴１１０養老保險");
		prdNameMap.put("NI","輕鬆還定期壽險");
		prdNameMap.put("NJ","新世紀變額遞延乙型");
		prdNameMap.put("NK","快樂人生定期生存保險");
		prdNameMap.put("NL","真心生活扶助終身壽險");
		prdNameMap.put("NM","生活扶助定期壽險");
		prdNameMap.put("NP","醫療帳戶終身");
		prdNameMap.put("NS","婦女新貴終身壽險");
		prdNameMap.put("NT","金好意１１０養老保險");
		prdNameMap.put("NW","金好意１１０");
		prdNameMap.put("NX","添利增值終身壽險");
		prdNameMap.put("NY","創世紀甲型");
		prdNameMap.put("NZ","創世紀乙型");
		prdNameMap.put("OA","添佑終身壽險");
		prdNameMap.put("OB","創世紀丙型");
		prdNameMap.put("OC","安和住院醫療終身");
		prdNameMap.put("OE","保本定期保險");
		prdNameMap.put("OF","富貴養老保險");
		prdNameMap.put("OG","雙好還本終身");
		prdNameMap.put("OH","長發年年終身保險");
		prdNameMap.put("OI","真意扶助還本");
		prdNameMap.put("OJ","達康１０１終身");
		prdNameMap.put("OK","達康１０１終身繳");
		prdNameMap.put("OL","松柏長期看護");
		prdNameMap.put("OM","添愛終身壽險");
		prdNameMap.put("ON","金美意養老保險");
		prdNameMap.put("OO","十全養老保險");
		prdNameMap.put("OP","利本年年終身壽險");
		prdNameMap.put("OQ","雙星還本終身");
		prdNameMap.put("OS","婦女尊貴終身壽險");
		prdNameMap.put("OT","國泰定期１００壽險");
		prdNameMap.put("OU","住得安定期壽險");
		prdNameMap.put("OV","真愛一生防癌");
		prdNameMap.put("OW","金順意養老保險");
		prdNameMap.put("OX","金美意養老保險");
		prdNameMap.put("OY","添意終身壽險");
		prdNameMap.put("OZ","添順不分紅定期壽險");
		prdNameMap.put("PA","國光－長壽");
		prdNameMap.put("PB","國光家福三年");
		prdNameMap.put("PC","新住院醫療終身");
		prdNameMap.put("PD","國光幸福養老");
		prdNameMap.put("PE","OIU年年給力壽險");
		prdNameMap.put("PF","ＯＩＵ好給利利變壽險");
		prdNameMap.put("PH","鍾心１０１終身分期");
		prdNameMap.put("PI","鍾心１０１終身終身繳");
		prdNameMap.put("PJ","鍾情終身壽險分期繳");
		prdNameMap.put("PK","月月康利變額年金");
		prdNameMap.put("PL","祥福１０１終身");
		prdNameMap.put("PN","富貴保本甲型");
		prdNameMap.put("PO","圓滿人生利變年金");
		prdNameMap.put("PP","益本養老保險");
		prdNameMap.put("PQ","國泰天才寶貝保險");
		prdNameMap.put("PR","護本養老保險");
		prdNameMap.put("PS","輕鬆理財（甲型）");
		prdNameMap.put("PT","輕鬆理財（乙型）");
		prdNameMap.put("PU","開運年年終身");
		prdNameMap.put("PV","幸福一生分紅終身壽險");
		prdNameMap.put("PW","金得利養老保險");
		prdNameMap.put("PX","鍾祝福重大疾病定期");
		prdNameMap.put("PY","固本養老保險");
		prdNameMap.put("PZ","長發年年終身保險");
		prdNameMap.put("QC","定期１００");
		prdNameMap.put("QD","富貴保本甲型");
		prdNameMap.put("QE","創甲保費費用");
		prdNameMap.put("QF","如意還本一六八");
		prdNameMap.put("QG","雙好還本終身");
		prdNameMap.put("QH","添愛終身保險");
		prdNameMap.put("QI","鍾美重大疾病");
		prdNameMap.put("QJ","真好意１１０養老險");
		prdNameMap.put("QK","添運終身");
		prdNameMap.put("QL","鍾情終身壽險");
		prdNameMap.put("QM","松柏長期看護");
		prdNameMap.put("QN","國泰婦女尊貴終身");
		prdNameMap.put("QO","達康１０１終身繳");
		prdNameMap.put("QP","國泰益本養老");
		prdNameMap.put("QQ","護本養老保險");
		prdNameMap.put("QR","達康１０１終身");
		prdNameMap.put("QS","保本定期");
		prdNameMap.put("QT","好運年年終身保險");
		prdNameMap.put("QU","金順利養老保險");
		prdNameMap.put("QV","醫療帳戶終身");
		prdNameMap.put("QW","真好意一一○養老保險");
		prdNameMap.put("QX","長鴻遞延年滿期");
		prdNameMap.put("QY","長鴻遞延歲滿期");
		prdNameMap.put("QZ","有巢氏定期壽險");
		prdNameMap.put("RA","國泰寵愛寶貝保險");
		prdNameMap.put("RB","國泰寵愛寶貝保險");
		prdNameMap.put("RC","利率變動型終身");
		prdNameMap.put("RD","慶本年年終身");
		prdNameMap.put("RE","金鑽還本利變");
		prdNameMap.put("RF","豐本養老保險");
		prdNameMap.put("RG","真得益養老");
		prdNameMap.put("RH","有巢氏定期壽險");
		prdNameMap.put("RI","真得益養老");
		prdNameMap.put("RJ","輕鬆購終身壽險");
		prdNameMap.put("RK","新輕鬆理財（甲型）");
		prdNameMap.put("RL","新輕鬆理財（乙型）");
		prdNameMap.put("RM","新利變年金（甲型）");
		prdNameMap.put("RN","新利變型年金（乙型）");
		prdNameMap.put("RO","金歡喜一一○養老");
		prdNameMap.put("RP","金有利３１２終身");
		prdNameMap.put("RQ","金祥瑞利變養老");
		prdNameMap.put("RR","金享利１１０養老保險");
		prdNameMap.put("RS","創甲保費費用");
		prdNameMap.put("RT","金歡喜一一○養老");
		prdNameMap.put("RU","超滿意年金（甲型）");
		prdNameMap.put("RV","超滿意年金（乙型）");
		prdNameMap.put("RW","理想變額甲型年滿");
		prdNameMap.put("RX","超輕鬆理財（甲型）");
		prdNameMap.put("RY","超輕鬆理財（乙型）");
		prdNameMap.put("RZ","開喜年年終身保險");
		prdNameMap.put("SA","居得利定期壽險");
		prdNameMap.put("SB","居得利定期壽險");
		prdNameMap.put("SC","居得安定期壽險");
		prdNameMap.put("SD","居得安定期壽險");
		prdNameMap.put("SF","樂事年年終身保險");
		prdNameMap.put("SG","添旺終身壽險");
		prdNameMap.put("SH","富利年年終身保險");
		prdNameMap.put("SI","長豐年年終身保險");
		prdNameMap.put("SJ","吉富３１２終身");
		prdNameMap.put("SK","鴻本年年終身");
		prdNameMap.put("SL","國泰安本養老");
		prdNameMap.put("SM","吉富３１２終身");
		prdNameMap.put("SN","金佳鴻分紅養老");
		prdNameMap.put("SO","有巢氏（三年）");
		prdNameMap.put("SP","金多益利變養老");
		prdNameMap.put("SQ","金佳福分紅養老");
		prdNameMap.put("SR","多順利變壽險");
		prdNameMap.put("SS","多鑽還本利變");
		prdNameMap.put("ST","美年添鑫美元終身保險");
		prdNameMap.put("SU","康順１０１終身");
		prdNameMap.put("SV","有巢氏（三年）");
		prdNameMap.put("SW","有巢氏（五年）");
		prdNameMap.put("SX","有巢氏（五年）");
		prdNameMap.put("SY","超滿意年金（甲型）");
		prdNameMap.put("SZ","超滿意年金（乙型）");
		prdNameMap.put("TA","超滿意年金（甲型）");
		prdNameMap.put("TB","超滿意年金（乙型）");
		prdNameMap.put("TD","理財高手");
		prdNameMap.put("TE","創世紀丁型");
		prdNameMap.put("TF","創世紀甲型");
		prdNameMap.put("TG","創甲保費費用");
		prdNameMap.put("TH","創世紀乙型");
		prdNameMap.put("TI","美利開鑫利變美元");
		prdNameMap.put("TJ","創世紀丙型");
		prdNameMap.put("TK","美年開鑫美元終身保險");
		prdNameMap.put("TL","飛翔人生變額甲型年滿");
		prdNameMap.put("TM","創甲保費費用");
		prdNameMap.put("TN","開利年年");
		prdNameMap.put("TO","多利還本利變");
		prdNameMap.put("TP","金添利養老保險");
		prdNameMap.put("TT","如意醫療養老");
		prdNameMap.put("TU","新定期１００");
		prdNameMap.put("TV","新康順１０１");
		prdNameMap.put("TW","新鍾情終身壽險");
		prdNameMap.put("TX","新寵愛寶貝保險");
		prdNameMap.put("TY","新寵愛寶貝保險");
		prdNameMap.put("TZ","國泰新婦女尊貴終身");
		prdNameMap.put("UA","鍾宜重大疾病");
		prdNameMap.put("UB","守護保本定期保險");
		prdNameMap.put("UC","鍾愛青春定期傷害保險");
		prdNameMap.put("UE","金得利９５養老");
		prdNameMap.put("UF","新超輕鬆理財（甲型）");
		prdNameMap.put("UG","新超輕鬆理財（乙型）");
		prdNameMap.put("UH","金集利");
		prdNameMap.put("UI","新超滿意年金（甲型）");
		prdNameMap.put("UJ","新超滿意年金（乙型）");
		prdNameMap.put("UK","金滿利養老保險");
		prdNameMap.put("UL","金添喜養老保險");
		prdNameMap.put("UM","長樂年年終身保險");
		prdNameMap.put("UN","康樂防癌");
		prdNameMap.put("UO","新有巢氏定期壽險");
		prdNameMap.put("UP","新有巢氏（三年）");
		prdNameMap.put("UQ","新超輕鬆理財（甲型）");
		prdNameMap.put("UR","新超輕鬆理財（乙型）");
		prdNameMap.put("US","新超滿意年金（甲型）");
		prdNameMap.put("UT","新超滿意年金（乙型）");
		prdNameMap.put("UU","如意傷害養老險");
		prdNameMap.put("UV","國泰新安本養老");
		prdNameMap.put("UW","超吉利年金（甲型）");
		prdNameMap.put("UX","超吉利年金（乙型）");
		prdNameMap.put("UY","多采多益美元利變");
		prdNameMap.put("UZ","富世紀甲型");
		prdNameMap.put("VA","福氣滿堂（滿期件）");
		prdNameMap.put("VB","新醫療帳戶終身");
		prdNameMap.put("VC","新如意醫療養老");
		prdNameMap.put("VD","世界通－美元");
		prdNameMap.put("VE","樂活年年");
		prdNameMap.put("VF","樂活年年");
		prdNameMap.put("VG","新超吉利年金（甲型）");
		prdNameMap.put("VH","樂福人生變額壽險");
		prdNameMap.put("VI","樂福人生變額年金");
		prdNameMap.put("VJ","樂福人生外幣年金");
		prdNameMap.put("VK","享樂８８變額年金");
		prdNameMap.put("VL","金得意養老保險");
		prdNameMap.put("VM","三高鑫安定期健康保險");
		prdNameMap.put("VO","俏媽咪養老保險");
		prdNameMap.put("VP","遨遊千里傷害住院醫療");
		prdNameMap.put("VQ","富世紀乙型");
		prdNameMap.put("VR","安心保住院醫療終身保");
		prdNameMap.put("VS","守護一生長期看護");
		prdNameMap.put("VT","國泰新安本養老歲滿");
		prdNameMap.put("VU","福氣滿堂（教職員工）");
		prdNameMap.put("VV","添鑫終身壽險");
		prdNameMap.put("VW","增美滿３１２");
		prdNameMap.put("VX","金好康養老保險");
		prdNameMap.put("VY","超利ＨＩＧＨ年金甲型");
		prdNameMap.put("VZ","超鑫動年金（甲型）");
		prdNameMap.put("WA","新鑫添鑫終身壽險");
		prdNameMap.put("WP","保險費豁免附約");
		prdNameMap.put("YA","金元滿利變養老保險");
		prdNameMap.put("YB","金多多利變養老保險");
		prdNameMap.put("YC","新增美滿３１２");
		prdNameMap.put("YD","世界通－澳幣");
		prdNameMap.put("YE","新富利年年終身保險");
		prdNameMap.put("YF","金佳意養老保險");
		prdNameMap.put("YG","如意心安傷害養老險");
		prdNameMap.put("YH","寵愛女人");
		prdNameMap.put("YI","安順手術醫療終身");
		prdNameMap.put("YJ","多益年年");
		prdNameMap.put("YK","豐彩１０１");
		prdNameMap.put("YL","新俏媽咪養老保險");
		prdNameMap.put("YM","金美利美元養老保險");
		prdNameMap.put("YN","集富人生變額年金年滿");
		prdNameMap.put("YO","有ＧＯ讚養老保險");
		prdNameMap.put("YP","金有ＧＯ讚養老保險");
		prdNameMap.put("YQ","ＧＯ保障１００");
		prdNameMap.put("YR","ＧＯ保障１００");
		prdNameMap.put("YT","新守護保本定期保險");
		prdNameMap.put("YU","新添運終身");
		prdNameMap.put("YV","添富年年終身保險");
		prdNameMap.put("YW","飛帆人生變額甲型０％");
		prdNameMap.put("YX","幸福貸定期壽險");
		prdNameMap.put("YZ","多益年年");
		prdNameMap.put("ZA","簡單愛");
		prdNameMap.put("ZB","輕鬆守護長期照顧保險");
		prdNameMap.put("ZC","鍾心呵護重大傷病定期");
		prdNameMap.put("ZD","鑫添鑫終身壽險");
		prdNameMap.put("ZE","ＧＯ青春定期保險");
		prdNameMap.put("ZF","ＯＩＵ美添給利變壽險");
		prdNameMap.put("ZG","鍾幸福特定傷病終身保");
		prdNameMap.put("ZH","守護公教長照健康保險");
		prdNameMap.put("ZI","兩全滿利利變");
		prdNameMap.put("ZJ","添利年年終身保險");
		prdNameMap.put("ZK","新富貴保本甲型");
		prdNameMap.put("ZL","超有利年金（乙型）");
		prdNameMap.put("ZM","添美年年美元２、６年");
		prdNameMap.put("ZN","添美年年美元");
		prdNameMap.put("ZO","超ＧＯ利年金（甲型）");
		prdNameMap.put("ZP","金好鑽養老保險");
		prdNameMap.put("ZQ","兩全美滿利變美元");
		prdNameMap.put("ZR","金福氣養老保險");
		prdNameMap.put("ZT","富世紀丙型");
		prdNameMap.put("ZU","優世紀");
		prdNameMap.put("ZV","新豐彩１０１");
		prdNameMap.put("ZW","富樂人生變額年金");
		prdNameMap.put("ZX","平安保本終身保險");
		
		prdNameMap.put("8AA", "超越（甲型）");
		prdNameMap.put("8AB", "超越（乙型）");
		prdNameMap.put("8AC", "優越（甲型）");
		prdNameMap.put("8AD", "優越（乙型）");
		prdNameMap.put("8AE", "意外傷害保險附約");
		prdNameMap.put("8AF", "意外傷害保險附約");
		prdNameMap.put("8AG", "意外傷害保險附約");
		prdNameMap.put("8AH", "意外傷害保險附約");
		prdNameMap.put("8AI", "意外傷害保險附約");
		prdNameMap.put("8AJ", "意外傷害保險附約");
		prdNameMap.put("8AK", "意外傷害保險附約");
		prdNameMap.put("8AL", "意外傷害保險附約");
		prdNameMap.put("8BA", "新祥鑽還本終身保險");
		prdNameMap.put("8BB", "幸福長壽還本終身保險");
		prdNameMap.put("8BC", "幸福長壽還本終身保險");
		prdNameMap.put("8CA", "精湛還本終身保險");
		prdNameMap.put("8CB", "精湛還本終身保險");
		prdNameMap.put("8CC", "幸福長壽還本終身保險");
		prdNameMap.put("8CD", "幸福長壽還本終身保險");
		prdNameMap.put("8CE", "幸福人壽幸福長樂終身");
		prdNameMap.put("8CF", "幸福人壽幸福長樂終身");
		prdNameMap.put("8CG", "一路挺你終身保險");
		prdNameMap.put("8CH", "幸福長發還本終身保險");
		prdNameMap.put("8CI", "如意還本終身保險（甲");
		prdNameMap.put("8CJ", "如意還本終身保險（甲");
		prdNameMap.put("8CK", "如意還本終身﹝乙型﹞");
		prdNameMap.put("8CL", "如意還本終身﹝乙型﹞");
		prdNameMap.put("8CM", "如意還本終身壽丙型");
		prdNameMap.put("8CN", "如意還本終身壽丙型");
		prdNameMap.put("8CO", "福祿還本終身壽險");
		prdNameMap.put("8CP", "福祿還本終身壽險");
		prdNameMap.put("8CQ", "金如意還本壽險");
		prdNameMap.put("8CR", "幸福一生還本終身壽險");
		prdNameMap.put("8CS", "幸福一生尊貴還本終身");
		prdNameMap.put("8CT", "幸福一生重疾還本終身");
		prdNameMap.put("8CU", "幸福２０００還本終身");
		prdNameMap.put("8CV", "幸福２０００還本終身");
		prdNameMap.put("8CW", "一世情還本終身壽險");
		prdNameMap.put("8CX", "幸福一生還本終身壽險");
		prdNameMap.put("8CY", "幸福一生尊貴還本終身");
		prdNameMap.put("8CZ", "幸福一生重疾還本終身");
		prdNameMap.put("8DA", "幸福一生還本終身壽險");
		prdNameMap.put("8DB", "幸福一生尊貴還本終身");
		prdNameMap.put("8DC", "幸福２０００還本終身");
		prdNameMap.put("8DD", "一世情還本終身壽險");
		prdNameMap.put("8DE", "幸福一生還本終身壽險");
		prdNameMap.put("8DF", "幸福一生尊貴還本終身");
		prdNameMap.put("8DG", "幸福２０００還本終身");
		prdNameMap.put("8DH", "一世情還本終身壽險");
		prdNameMap.put("8DI", "精典還本終身保險");
		prdNameMap.put("8DJ", "精典還本終身附約");
		prdNameMap.put("8DK", "幸福一生還本終身壽險");
		prdNameMap.put("8DL", "幸福人壽防癌附約");
		prdNameMap.put("8DM", "幸福人壽防癌附約");
		prdNameMap.put("8DN", "幸福人壽防癌附約");
		prdNameMap.put("8DO", "幸福人壽防癌附約");
		prdNameMap.put("8DP", "幸福人壽防癌附約");
		prdNameMap.put("8DQ", "幸福人壽防癌附約");
		prdNameMap.put("8DR", "幸福一生利變年金");
		prdNameMap.put("8DS", "幸福一生利變年金");
		prdNameMap.put("8DT", "幸福一生利變年金");
		prdNameMap.put("8DU", "幸福達樂（甲型）");
		prdNameMap.put("8DV", "幸福一生利變年金");
		prdNameMap.put("8EA", "遞減型定期壽（Ｂ型）");
		prdNameMap.put("8EB", "天天幸福特定重疾終身");
		prdNameMap.put("8EC", "天天幸福特定重疾終身");
		prdNameMap.put("8ED", "天生幸福特定重疾終身");
		prdNameMap.put("8EE", "天生幸福特定重疾終身");
		prdNameMap.put("8EF", "幸福一百特定傷病終身");
		prdNameMap.put("8EG", "幸福一百特定傷病終身");
		prdNameMap.put("8EH", "重大疾病終身健康保險");
		prdNameMap.put("8EI", "幸福照護終身保險");
		prdNameMap.put("8EJ", "幸福照護終身保險");
		prdNameMap.put("8EK", "幸福照護終身保險");
		prdNameMap.put("8EL", "珍愛幸福照護終身保險");
		prdNameMap.put("8EM", "珍愛幸福照護終身保險");
		prdNameMap.put("8EN", "珍愛幸福照護終身保險");
		prdNameMap.put("8EO", "遞減型定期壽（Ｂ型）");
		prdNameMap.put("8EP", "富裕利變型終身壽險");
		prdNameMap.put("8EQ", "利率變動型終身");
		prdNameMap.put("8ER", "利率變動型終身");
		prdNameMap.put("8ES", "幸福２０００增額養老");
		prdNameMap.put("8ET", "幸福２０００還本養老");
		prdNameMap.put("8EU", "如意養老壽險");
		prdNameMap.put("8EV", "美滿儲蓄壽險");
		prdNameMap.put("8EW", "美滿儲蓄壽險");
		prdNameMap.put("8EX", "白金人生養老壽險");
		prdNameMap.put("8EY", "鑽石人生養老壽險");
		prdNameMap.put("8EZ", "金鑽養老壽險");
		prdNameMap.put("8FA", "鑽石人生養老壽險");
		prdNameMap.put("8FB", "今生有約養老保險");
		prdNameMap.put("8FC", "富麗人生養老保險");
		prdNameMap.put("8FD", "富億人生養老壽險");
		prdNameMap.put("8FE", "白金人生養老壽險");
		prdNameMap.put("8FF", "富億人生養老壽險");
		prdNameMap.put("8FG", "幸福人生養老險");
		prdNameMap.put("8FH", "幸福一世養老保險");
		prdNameMap.put("8FI", "新幸福人生養老險");
		prdNameMap.put("8FJ", "新幸福人生養老險");
		prdNameMap.put("8FK", "白金人生養老保險");
		prdNameMap.put("8GA", "吉祥養老壽險（甲型）");
		prdNameMap.put("8GB", "吉祥養老壽險（甲型）");
		prdNameMap.put("8GC", "吉祥養老壽險（甲型）");
		prdNameMap.put("8GD", "吉祥養老壽險（乙型）");
		prdNameMap.put("8GE", "吉祥養老壽險（乙型）");
		prdNameMap.put("8GF", "吉祥養老壽險（乙型）");
		prdNameMap.put("8GG", "吉祥養老壽險（乙型）");
		prdNameMap.put("8GH", "幸福一生增額養老壽險");
		prdNameMap.put("8HA", "意外傷害住院醫療日額");
		prdNameMap.put("8HB", "意外傷害住院醫療日額");
		prdNameMap.put("8HC", "意外傷害住院醫療日額");
		prdNameMap.put("8HD", "意外傷害住院醫療日額");
		prdNameMap.put("8HE", "意外傷害醫療保險附約");
		prdNameMap.put("8HF", "意外傷害醫療保險附約");
		prdNameMap.put("8HG", "新傷害醫療保險附約");
		prdNameMap.put("8HH", "新傷害醫療保險附約");
		prdNameMap.put("8HI", "新傷害醫療保險附約");
		prdNameMap.put("8IA", "長春藤限額終身醫療");
		prdNameMap.put("8IB", "帳戶型終身醫療");
		prdNameMap.put("8IC", "帳戶型終身醫療");
		prdNameMap.put("8ID", "綜合住院醫療終身健康");
		prdNameMap.put("8IE", "帳戶型終身醫療");
		prdNameMap.put("8IF", "綜合住院醫療終身健康");
		prdNameMap.put("8IG", "住院醫療日額給付保險");
		prdNameMap.put("8IH", "住院醫療日額給付保險");
		prdNameMap.put("8II", "住院醫療日額給付保險");
		prdNameMap.put("8IJ", "住院醫療日額給付保險");
		prdNameMap.put("8IK", "住院醫療健康保險附約");
		prdNameMap.put("8JA", "富泰增值終身（甲型）");
		prdNameMap.put("8JB", "富泰增值終身壽險（乙");
		prdNameMap.put("8JC", "富泰增值終身壽險（乙");
		prdNameMap.put("8JD", "富貴增值終身壽險（甲");
		prdNameMap.put("8JE", "富貴增值終身壽險（甲");
		prdNameMap.put("8JF", "富貴增值終身壽險（甲");
		prdNameMap.put("8JG", "富貴增值終身壽險（甲");
		prdNameMap.put("8JH", "富貴增值（乙型）");
		prdNameMap.put("8JI", "富貴增值（乙型）");
		prdNameMap.put("8JJ", "富貴增值（乙型）");
		prdNameMap.put("8JK", "富貴增值（乙型）");
		prdNameMap.put("8JL", "金富泰增值壽險");
		prdNameMap.put("8JM", "金富貴增值壽險");
		prdNameMap.put("8JN", "金富貴增值壽險");
		prdNameMap.put("8JO", "福臨增值壽險（甲型）");
		prdNameMap.put("8JP", "福臨增值壽險（乙型）");
		prdNameMap.put("8JQ", "祥瑞增值還本壽險");
		prdNameMap.put("8JR", "幸福一生增額終身壽險");
		prdNameMap.put("8JS", "幸福人壽二十一世紀增");
		prdNameMap.put("8JT", "傳承增額終身壽險");
		prdNameMap.put("8JU", "傳承增額終身壽險");
		prdNameMap.put("8JV", "幸福典藏增額終身壽險");
		prdNameMap.put("8JW", "幸福典藏增額終身壽險");
		prdNameMap.put("8JX", "超群增額終身壽險");
		prdNameMap.put("8KA", "幸福一生終身壽險");
		prdNameMap.put("8KB", "超群增額終身壽險");
		prdNameMap.put("8LA", "愛彌兒教育壽險");
		prdNameMap.put("8LB", "愛彌兒創業壽險");
		prdNameMap.put("8LC", "愛彌兒創業壽險");
		prdNameMap.put("8LD", "愛彌兒成長壽險");
		prdNameMap.put("8LE", "幸福一生兒童終身壽");
		prdNameMap.put("8LF", "幸福一生兒童終身壽");
		prdNameMap.put("8LG", "Ｅ世代兒童終身壽險");
		prdNameMap.put("8LH", "幸福人壽優質終身壽險");
		prdNameMap.put("8LI", "幸福一生重疾終身壽");
		prdNameMap.put("8LJ", "幸福一生重疾終身壽");
		prdNameMap.put("8LK", "新優質終身壽險");
		prdNameMap.put("8LL", "新優質終身壽險");
		prdNameMap.put("8LM", "新優質終身壽險");
		prdNameMap.put("8LN", "新優質終身壽險");
		prdNameMap.put("8LO", "幸福一生重疾終身壽");
		prdNameMap.put("8LP", "幸福一生重疾終身壽");
		prdNameMap.put("8LQ", "幸福一生重疾終身壽");
		prdNameMap.put("8LR", "幸福一生重疾終身壽");
		prdNameMap.put("8LS", "幸福一生重疾終身壽");
		prdNameMap.put("8LT", "新優質終身壽險");
		prdNameMap.put("8LU", "新優質終身壽險");
		prdNameMap.put("8LV", "新優質終身壽險");
		prdNameMap.put("8LW", "幸福一生重大疾病終身");
		prdNameMap.put("8LX", "幸福一生重大疾病終身");
		prdNameMap.put("8LY", "幸福一生重大疾病終身");
		prdNameMap.put("8LZ", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8MA", "ＳＦ定期壽險（歲滿期");
		prdNameMap.put("8MB", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8MC", "ＳＦ定期壽險（歲滿期");
		prdNameMap.put("8MD", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8ME", "ＳＦ定期壽險（歲滿期");
		prdNameMap.put("8MF", "新定期壽險附約");
		prdNameMap.put("8MG", "新定期壽險附約");
		prdNameMap.put("8MH", "新定期壽險附約");
		prdNameMap.put("8MI", "新定期壽險附約");
		prdNameMap.put("8MJ", "新定期壽險附約");
		prdNameMap.put("8MK", "新定期壽險附約");
		prdNameMap.put("8ML", "重疾定期壽險附約");
		prdNameMap.put("8MM", "重疾定期壽險附約");
		prdNameMap.put("8MN", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8MO", "新定期壽險附約");
		prdNameMap.put("8MP", "收入保障定期壽險附約");
		prdNameMap.put("8MQ", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8MR", "新定期壽險附約");
		prdNameMap.put("8MS", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8MT", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8MU", "ＳＦ定期壽險（歲滿期");
		prdNameMap.put("8MV", "幸福安家定期壽險甲型");
		prdNameMap.put("8MW", "幸福安家定期壽險乙型");
		prdNameMap.put("8MX", "幸福安家定期壽險乙型");
		prdNameMap.put("8MY", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8MZ", "ＳＦ定期壽險（歲滿期");
		prdNameMap.put("8NA", "美滿定期壽險");
		prdNameMap.put("8NB", "美滿定期壽險");
		prdNameMap.put("8NC", "幸福安家定期壽險甲型");
		prdNameMap.put("8ND", "定期壽險附約");
		prdNameMap.put("8NE", "定期壽險附約");
		prdNameMap.put("8NF", "幸福安家定期壽險甲型");
		prdNameMap.put("8NG", "幸福安家定期壽險乙型");
		prdNameMap.put("8NH", "幸福安家定期壽險乙型");
		prdNameMap.put("8NI", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8NJ", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8NK", "ＳＦ定期壽險（歲滿期");
		prdNameMap.put("8NL", "ＳＦ定期壽險（年滿期");
		prdNameMap.put("8NM", "ＳＦ定期壽險（歲滿期");
		prdNameMap.put("8OA", "重大疾病終身壽險");
		prdNameMap.put("8OB", "重大疾病終身壽險附約");
		prdNameMap.put("8OC", "重大疾病終身壽險附約");
		prdNameMap.put("8OD", "金美滿壽險");
		prdNameMap.put("8OE", "金美滿壽險");
		prdNameMap.put("8OF", "金美滿壽險");
		prdNameMap.put("8OG", "金美滿壽險");
		prdNameMap.put("8OH", "幸福一生終身壽險");
		prdNameMap.put("8OI", "幸福一生重疾終身壽");
		prdNameMap.put("8OJ", "幸福２０００終身壽險");
		prdNameMap.put("8OK", "幸福一生終身壽險");
		prdNameMap.put("8OL", "幸福一生重疾終身壽");
		prdNameMap.put("8OM", "美滿終身壽險－期繳");
		prdNameMap.put("8ON", "美滿終身壽險－歲滿期");
		prdNameMap.put("8OO", "幸福２０００終身壽險");
		prdNameMap.put("8OP", "幸福一生終身壽險");
		prdNameMap.put("8OQ", "幸福一生重疾終身壽");
		prdNameMap.put("8OR", "幸福２０００終身壽險");
		prdNameMap.put("8OS", "幸福一生終身壽險");
		prdNameMap.put("8OT", "幸福一生重疾終身壽");
		prdNameMap.put("8OU", "幸福２０００終身壽險");
		prdNameMap.put("8OV", "精選終身壽險");
		prdNameMap.put("8OW", "精選終身壽險附約");
		prdNameMap.put("8OX", "幸福人壽優質終身壽險");
		prdNameMap.put("8OY", "幸福一生終身壽險");
		prdNameMap.put("8OZ", "幸福一生重疾終身壽");
		prdNameMap.put("8PA", "幸福一生終身壽險");
		prdNameMap.put("8PB", "幸福一生終身壽險");
		prdNameMap.put("8QA", "幸福人壽富貴萬能");
		prdNameMap.put("8QB", "幸福人壽防癌附約");
		prdNameMap.put("8QC", "幸福人壽防癌附約");
		prdNameMap.put("9AA", "永泰終身保險");
		prdNameMap.put("9AB", "永泰終身保險");
		prdNameMap.put("9AC", "康福養老保險（９０）");
		prdNameMap.put("9AD", "日額型住院終身保險");
		prdNameMap.put("9AE", "日額型住院終身保險");
		prdNameMap.put("9BA", "國寶人壽定期壽險");
		prdNameMap.put("9BB", "國寶人壽定期壽險");
		prdNameMap.put("9BC", "國寶人壽定期壽險");
		prdNameMap.put("9BD", "國寶理財保證收益壽險");
		prdNameMap.put("9BE", "定期保險附約（９６）");
		prdNameMap.put("9CA", "安心終身附約");
		prdNameMap.put("9CB", "福星養老保險（９２）");
		prdNameMap.put("9CC", "快樂一生壽險（Ｂ型）");
		prdNameMap.put("9CD", "傷害保險附約");
		prdNameMap.put("9CE", "傷害保險附約");
		prdNameMap.put("9CF", "傷害保險附約");
		prdNameMap.put("9DA", "安家豁免（９６）");
		prdNameMap.put("9EA", "美好人生終身附約");
		prdNameMap.put("9EB", "國寶人壽定期壽險");
		prdNameMap.put("9EC", "永泰終身保險");
		prdNameMap.put("9ED", "新終身保險特約");
		prdNameMap.put("9HA", "寶本增值終身壽險Ｃ型");
		prdNameMap.put("9HB", "寶本增值終身壽險Ｆ型");
		prdNameMap.put("9KA", "雲流萍蹤終身壽險");
		prdNameMap.put("9KB", "雲流萍蹤終身壽險");
		prdNameMap.put("9KC", "雲流萍蹤終身壽險");
		prdNameMap.put("9KD", "奔向陽光防癌終身附約");
		prdNameMap.put("9LA", "日額型住院終身保險");
		prdNameMap.put("9PA", "寶本終身壽險Ｂ型");
		prdNameMap.put("9PB", "寶本終身壽險Ｃ型");
		prdNameMap.put("9PC", "寶本終身壽險Ｄ型");
		prdNameMap.put("9PD", "寶本終身壽險Ｅ型");
		prdNameMap.put("9PE", "寶本終身壽險Ｆ型");
		prdNameMap.put("9PF", "寶本終身壽險Ａ型");
		prdNameMap.put("9PG", "寶本終身壽險Ａ型");
		prdNameMap.put("9PH", "寶本終身壽險Ｂ型");
		prdNameMap.put("9PI", "寶本終身壽險Ｃ型");
		prdNameMap.put("9PJ", "寶本終身壽險Ｄ型");
		prdNameMap.put("9PK", "寶祿一三一保險");
		prdNameMap.put("9PL", "寶祿一三一保險");
		prdNameMap.put("9PM", "寶祿一三一保險");
		prdNameMap.put("9PN", "寶祥養老保險");
		prdNameMap.put("9QA", "寶貝儲蓄保險");
		prdNameMap.put("9QB", "寶利終身保險（躉）");
		prdNameMap.put("9VA", "樂透人生終身保險Ａ型");
		prdNameMap.put("9YA", "人身傷害醫限－無社保");
		prdNameMap.put("9YB", "人身傷害醫限－有社保");
	}
}
