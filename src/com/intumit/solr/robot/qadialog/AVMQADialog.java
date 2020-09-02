package com.intumit.solr.robot.qadialog;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.android.search.util.TaiwanAddressNormalizeUtil;
import com.intumit.solr.robot.NaverLineAnswerTransformer;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.qadialog.CallDialogRuleCheckResult.RewriteType;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QADialogRule;
import com.intumit.solr.util.WiSeUtils;

public class AVMQADialog extends FillFormQADialog {
	InnerStatus status = InnerStatus.INACTIVE;
	static enum InnerStatus {
		JUST_ACTIVATED,
		INACTIVE,
		ASK_QUIT_OR_NOT, 
		ASK_ENTER_OR_NOT, 
		IN_PROGRESS, 
		FIELD_CHECK_AGAIN,
		FIELD_DOUBLE_CONFIRM
	}

	public AVMQADialog() {
		
	}
	
	@Override
	public void loadConfig() {
		super.loadConfig();
	}

	@Override
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
			boolean callApi = false;
	
			for (Trigger t: quitTriggers) {
				if (t.isTrigger(ctx)) {
					if (t.isDoubleConfirm()) {
						// set Answer
						String doubleConfirmText = convertText(ctx, t.getDoubleConfirmText());
						
						JSONArray ma = new JSONArray();
						ma.put(NaverLineAnswerTransformer.confirmMessage("請確認？", "是的", "是的", "再想想", "不好"));
						ctx.setResponseAttribute("messages", ma);

						ctx.setAnswerText(doubleConfirmText);
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.setResponseAttribute("DIALOG", getName());
						status = InnerStatus.ASK_QUIT_OR_NOT;
					}
					else {
						// set Answer
						ctx.setAnswerText(showCurrentStatusText() + NL + "已經退出AVM估價");
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.setResponseAttribute("DIALOG", getName());
						status = InnerStatus.INACTIVE;
					}
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
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
					ctx.setAnswerText(currentField.getDefaultQuestion());
					status = InnerStatus.IN_PROGRESS;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
				else if (QAUtil.isConfirmWithNo(ctx.getCurrentQuestion())) {
					forceQuit = true;
				}
				else {
					// set Answer
					ctx.setAnswerText(ctx.getLastAnswerText());
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
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
					ctx.setAnswerText(currentField.getDefaultQuestion());
					status = InnerStatus.IN_PROGRESS;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
			if ( status == InnerStatus.JUST_ACTIVATED ) {
				if (isEnterDoubleConfirm() && !isHasBeenDoubleConfirmed()) {
					// set Answer
					String doubleConfirmText = convertText(ctx, getEnterDoubleConfirmText());
					
					JSONArray ma = new JSONArray();
					ma.put(NaverLineAnswerTransformer.confirmMessage("請確認？", "好", "好", "不用了", "不好"));
					ctx.setResponseAttribute("messages", ma);

					ctx.setAnswerText(doubleConfirmText);
					ctx.setAnswerText(convertText(ctx, getEnterDoubleConfirmText()));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.ASK_ENTER_OR_NOT;
				}
				else {
					currentFieldOffset = 0;
					Field currentField = fields.get(currentFieldOffset);
					
					// set Answer
					ctx.setAnswerText(currentField.getDefaultQuestion());
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.IN_PROGRESS;
				}
				return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
			else if (status == InnerStatus.FIELD_DOUBLE_CONFIRM) {
				if (QAUtil.isConfirmWithYes(QAUtil.simpleClean(ctx.getCurrentQuestion(), ctx.getTenant()))) {
					forceProgressToNextField = true;
				}
				else if (QAUtil.isConfirmWithNo(ctx.getCurrentQuestion())) {
					Field currentField = fields.get(currentFieldOffset);
					currentField.setCurrentValue(null);
					
					// set Answer
					ctx.setAnswerText(currentField.getDefaultQuestion());
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.IN_PROGRESS;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
				else {
					// set Answer
					ctx.setAnswerText(ctx.getLastAnswerText());
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
			
			if (status == InnerStatus.IN_PROGRESS || status == InnerStatus.FIELD_CHECK_AGAIN) {
				if (currentFieldOffset != null) {
					Field currentField = fields.get(currentFieldOffset);
					
					if (currentField != null && (currentField.getCurrentValue() == null || status == InnerStatus.FIELD_CHECK_AGAIN)) {
						RuleCheckResult result = currentField.checkRules(ctx);
						
						if (result.getStatus() == RuleCheckResultStatus.CALL_DIALOG) {
							CallDialogRuleCheckResult crcr = (CallDialogRuleCheckResult)result;
							currentField.setCurrentResult(crcr);
							currentField.setCurrentValue(crcr.getValue());
							
							// 提早判斷避免都問完區域及詳細地址之後才跟人家說不玩了
							if (currentField.getName().equals("address")) {
								TaiwanAddressNormalizeUtil anu = getAddressNormalizeUtil(StringUtils.upperCase(currentField.getCurrentValue()));
								System.out.println(currentField.getCurrentValue() + "=>" + anu.getFormatAddress());
								
								if (anu.getAdministrative_area() != null && !StringUtils.equals(anu.getAdministrative_area(), "台北市")) {
									currentField.setCurrentValue(null);
									currentField.setCurrentResult(null);
									ctx.setAnswerText(anu.getAdministrative_area() + "目前不提供自動估價試算，請重新輸入房屋地址。");
									ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
									ctx.setResponseAttribute("DIALOG", getName());
									
									return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
								}
							}

							QADialog dlg = QADialogRule.getDialogInstance(ctx, crcr.getTargetDialogName());
							ctx.setCtxAttr(crcr.getTargetDialogName(), crcr);
							PostRuleCheckResult res = call(ctx, dlg, crcr);
							return res;
						}
						else if (result.isMatch()) {
							Map<String, Object> invalid = currentField.validate(ctx, this);
							
							if (invalid == null) {
								if (currentField.getName().equals("address")) {
									TaiwanAddressNormalizeUtil anu = getAddressNormalizeUtil(StringUtils.upperCase(currentField.getCurrentValue()));
									System.out.println(currentField.getCurrentValue() + "=>" + anu.getFormatAddress());
									
									if (!anu.getAdministrative_area().equals("台北市") && !anu.getAdministrative_area().equals("北市")) {
										currentField.setCurrentValue(null);
										currentField.setCurrentResult(null);
										ctx.setAnswerText(anu.getAdministrative_area() + "目前不提供自動估價試算，請重新輸入房屋地址。");
										ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
										ctx.setResponseAttribute("DIALOG", getName());
										
										return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
									}
									else {
										currentField.setCurrentValue(anu.getFormatAddress());
										if (anu.getFloor() != null) {
											Field floor = getField("floor");
											int floorN = anu.getIntFloor();
											// 處理負數就是地下室 BX
											floor.setCurrentValue(floorN > 0 ? "" + floorN : "B" + Math.abs(floorN));
										}
									}
								}
								if (Boolean.TRUE.equals(currentField.getDoubleConfirm())) {
									
									// set Answer
									String doubleConfirmText = convertText(ctx, currentField.getDoubleConfirmText());
									
									JSONArray ma = new JSONArray();
									ma.put(NaverLineAnswerTransformer.confirmMessage("請確認資訊是否正確？", "是", "是", "不是", "不對"));
									ctx.setResponseAttribute("messages", ma);

									ctx.setAnswerText(doubleConfirmText);
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
							ctx.setAnswerText(currentField.getDefaultQuestion());
							ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
							ctx.setResponseAttribute("DIALOG", getName());
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
					
					if (currentField.getCurrentValue() != null && Boolean.TRUE.equals(currentField.getDoubleConfirm())) {
						
						// set Answer
						String doubleConfirmText = convertText(ctx, currentField.getDoubleConfirmText());
						
						JSONArray ma = new JSONArray();
						ma.put(NaverLineAnswerTransformer.confirmMessage("請確認資訊是否正確？", "是", "是", "不是", "不對"));
						ctx.setResponseAttribute("messages", ma);

						ctx.setAnswerText(doubleConfirmText);
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.setResponseAttribute("DIALOG", getName());
						status = InnerStatus.FIELD_DOUBLE_CONFIRM;
						
						return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
					else {
						// set Answer
						if (currentField.getExtraParams() != null) {
							Map<String, Object> extraParams = currentField.getExtraParams();
							JSONObject extraParamJson = new JSONObject(extraParams);
							if (extraParamJson.has("lineMessages")) {
								ctx.setResponseAttribute("messages", extraParamJson.get("lineMessages"));
							}
							if (extraParamJson.has("botframeworkMessages")) {
								JSONObject bfObj = extraParamJson.getJSONObject("botframeworkMessages");
								
								for (Object keyObj: bfObj.keySet()) {
									String key = (String)keyObj;
									if (bfObj.has(key)) {
										ctx.setResponseAttribute(key, bfObj.get(key));
									}
								}
							}
						}
						ctx.setAnswerText(convertText(ctx, currentField.getDefaultQuestion()));
						ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
						ctx.setResponseAttribute("DIALOG", getName());
						status = InnerStatus.IN_PROGRESS;
	
						return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
					}
				}
				else {
					callApi = true;
				}
			}
			
			if (callApi) {
				int amount = 2000;
				double rate = 0.8d;
				
				// https://202.154.204.113/cxlpoc/sb/Invest/PredictPrice?id=API0002&client_id=0fad548e-e92a-4d9d-a76f-07d9cb543bce
				JSONObject args = new JSONObject();
				try {
					TaiwanAddressNormalizeUtil anu = getAddressNormalizeUtil(StringUtils.upperCase(getField("address").getCurrentValue()));
					args.put("ADDRESS", anu.getFormatAddress());
					args.put("AREA", anu.getLocality());
					args.put("HOUSE_TYPE", getField("houseType").getCurrentValue());
					args.put("TRANSF_FLOOR", getField("floor").getIntValue());
					args.put("HOUSE_YEAR", getField("age").getCurrentValue());
					
					double square = getField("square").getDoubleValue() * 3.3058d;
					args.put("ADJUST_BUILD_AREA", square);
					
					System.out.println(args.toString(2));
					
					String apiUrl = "https://mydomain/Invest/PredictPrice?id=API0002&client_id=0fad548e-e92a-4d9d-a76f-07d9cb543bce&args=";
					
					apiUrl += URLEncoder.encode( 
							URLEncoder.encode(args.toString(), "UTF-8"),
							"UTF-8");
					
					System.out.println(apiUrl);
					
					//String result = null;//WiSeUtils.getDataFromUrl(apiUrl);
					//JSONObject resultJson = new JSONObject(result);
					//double pricePerSquare = resultJson.getDouble("PRICE");
					amount = 2000;//(int)Math.round(pricePerSquare * square / 10000d);
					rate = 0.7;//resultJson.getDouble("LOAN");
					
					String ans = "感謝您耐心的等候，您在${address}的房屋估價大約是" + amount + "萬，可貸款成數為" + (int)(rate*10) + "成"
							+ NL + "，我們現在有推出超低利房貸優惠方案，是否有興趣進一步瞭解？";
					
					
					JSONArray ma = new JSONArray();
					ma.put(NaverLineAnswerTransformer.confirmMessage("我對超低利房貸有興趣", "有興趣", "超低利房貸介紹", "不用了", "再見"));
					ctx.setResponseAttribute("messages", ma);

					// set Answer
					ctx.setAnswerText(convertText(ctx, ans));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.INACTIVE;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				} catch (Exception e) {
					e.printStackTrace();
					
					String ans = "感謝您耐心的等候，目前無法從估價系統取得結果（通常這是後端估價系統異常，一般恢復時間需要15~30分鐘）。"
							+ "錯誤訊息為（" + e.getMessage() + "）。";
					
					// set Answer
					ctx.setAnswerText(convertText(ctx, ans));
					ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
					ctx.setResponseAttribute("DIALOG", getName());
					status = InnerStatus.INACTIVE;
					
					return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
				}
			}
			
			if (forceQuit) {
				// set Answer
				ctx.setAnswerText(showCurrentStatusText() + NL + "已經取消房貸AVM估價");
				ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG);
				ctx.setResponseAttribute("DIALOG", getName());
				status = InnerStatus.INACTIVE;
				
				return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		finally {
			if (ctx.getCtxAttr("activeDialog") == this)
				ctx.setCtxAttr("activeDialog", null);
		}
		
		return PostRuleCheckResult.DEFAULT_CONTINUE_RESULT;
	}

	static TaiwanAddressNormalizeUtil getAddressNormalizeUtil(String address) {
		TaiwanAddressNormalizeUtil tan = new TaiwanAddressNormalizeUtil(address);
		return tan;
	}

	@Override
	public void reset() {
		super.reset();
		status = InnerStatus.INACTIVE;
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

	@Override
	public PostRuleCheckResult returnFromDialog(CallStackData stackData, QAContext ctx, PostRuleCheckResult r) {
		List<Field> fields2 = stackData.to.getFields();
		Field targetF = null;
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
					
					if (targetF == null)
						targetF = f1;
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
				
				fields.add(f2);
			}
		}
		
		ctx.setCurrentQuestion(targetF != null ? targetF.getCurrentValue() : stackData.question);
		
		if (targetF != null && !targetF.getCurrentValue().equals(stackData.question))
			status = InnerStatus.FIELD_CHECK_AGAIN;
		
		PostRuleCheckResult prcr = check(ctx);
		
		return prcr;
	}
}
