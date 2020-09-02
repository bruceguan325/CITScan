package com.intumit.solr.robot.qaplugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.ANSWER_TYPE;
import com.intumit.solr.robot.QASaver;
import com.intumit.solr.robot.qadialog.QADialog;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.robot.qarule.QADialogRule;
import com.intumit.solr.robot.qarule.QAMatchRuleController;

public class QADialogPlugin extends QAPlugin {
	public static final String QA_MKEY_PARAM = "dlgQaMkey";
	public static final String QA_KEY_INDEX_FIELD = "CONVERSATIONAL_QA_ID_s";
	public static final String QA_USE_DRAFT_PARAM = "dlgQaUseDraft";
	public static final String QA_USE_DRAFT_INDEX_FIELD = "CONVERSATIONAL_QA_USE_DRAFT_b";

	public static final String ID = "13";

	public static final String REQ_ATTR_QA_KID = "_dlgQaKid";
	public static final String REQ_ATTR_ID_QA_MAP = "_dlgIdQaMap";
	public static final String REQ_ATTR_QA_DATA = "_dlgQaData";
	
	QADialogPlugin(String id, String name) {
		super(id, name);
	}
	
	public static JSONObject getDataFromString(String dataStr) {
		JSONObject o = null;
		try {
			o = new JSONObject(StringUtils.defaultString(dataStr, "{}"));
			
			if (!o.has("children")) {
				o.put("children", new JSONArray());
			}
		}
		catch (JSONException e) {
			if (o == null)
				o = new JSONObject();
			JSONArray children = null;
			try {
				children = new JSONArray(StringUtils.defaultString(dataStr, "[]"));
			}
			catch (JSONException e2) {
				e.printStackTrace();
			}
			
			if (children == null)
				children = new JSONArray();
			
			try {
				o.put("children", children);
			}
			catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
		
		return o;
	}

	@Override
	public boolean hasEditorPage() {
		return true;
	}

	@Override
	public String onEditorPageCreate(SolrDocument doc, HttpServletRequest req) {
		req.setAttribute("docId", doc.getFieldValue("id"));
		String customQaId = (String)doc.getFieldValue(QA_KEY_INDEX_FIELD);

		if (customQaId != null) {
			req.setAttribute(QA_MKEY_PARAM, customQaId);
		}
		
		Boolean useDraft = (Boolean)doc.getFieldValue(QA_USE_DRAFT_INDEX_FIELD);
		if (useDraft != null)
			req.setAttribute(QA_USE_DRAFT_PARAM, useDraft);
		
		return "/wiseadm/qa-plugin/conversational-qa-editor.jsp";
	}

	@Override
	public void onEditorPageSave(SolrInputDocument doc, HttpServletRequest req,
			boolean selected) {

		String mkey = req.getParameter(QA_MKEY_PARAM);
		QASaver.setOrUpdateField(doc, QA_KEY_INDEX_FIELD, (!selected || mkey == null)?null:mkey);
		
		String useDraft = StringUtils.trimToNull(req.getParameter(QA_USE_DRAFT_PARAM));
		if (useDraft != null) {
			QASaver.setOrUpdateField(doc, QA_USE_DRAFT_INDEX_FIELD, Boolean.TRUE);
		}
		else {
			QASaver.setOrUpdateField(doc, QA_USE_DRAFT_INDEX_FIELD, Boolean.FALSE);
		}
	}
	
	public static JSONObject buildConfigFromRequest(HttpServletRequest req, boolean getMkeyFromReq) {
		JSONObject data = new JSONObject();
		JSONArray _1stLvChildren = null;
		try {
			_1stLvChildren = getChildrenDataFromRequest(req, "dlg_qa");
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		if (_1stLvChildren == null)
			_1stLvChildren = new JSONArray();
		try {
			data.put("children", _1stLvChildren);

			setJSONPropFromRequest(data, req, "mkey", JSONObject.NULL);
			setJSONPropFromRequest(data, req, "dialogName", JSONObject.NULL);
			setJSONPropFromRequest(data, req, "dialogDesc", JSONObject.NULL);
			setJSONPropFromRequest(data, req, "entryPoint", JSONObject.NULL);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
	static void setJSONPropFromRequest(JSONObject obj, HttpServletRequest req, String propName, Object defaultIfNull) {
		try {
			String data = req.getParameter(propName);
			if (data != null) {
				obj.put(propName, data);
			}
			else {
				if (defaultIfNull != null)
					obj.put(propName, defaultIfNull);
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String onAnswerPreview(String origAnswer, HttpServletRequest req) {
		JSONObject data = new JSONObject();
		try {
			data.put("children", getChildrenDataFromRequest(req, "dlg_qa"));
		}
		catch (JSONException e) {
			try {
				data.put("children", new JSONArray());
				e.printStackTrace();
			}
			catch (JSONException e2) {
				e2.printStackTrace();
			}
		}
		req.setAttribute("answer", origAnswer);

		return "/wiseadm/qa-plugin/conversational-qa-preview.jsp";
	}

	@Override
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String question, final QAContext ctx) {

		QA directAnswer = ctx.getCurrentQA();

		try {
			if (directAnswer != null) {
				String mkey = (String)directAnswer.getFieldValue(QA_KEY_INDEX_FIELD);
				Boolean useDraft = (Boolean)directAnswer.getFieldValue(QA_USE_DRAFT_INDEX_FIELD);
				
				if (mkey != null) {
					QADialog dlg = QADialogRule.findRunningDialog(ctx, mkey);
					
					if (dlg == null) {
						//QADialogConfig dlgCfg = QADialogConfig.getByKey(ctx.getTenant().getId(), mkey);
						dlg = QADialogRule.getDialogInstance(ctx, mkey, useDraft != null && useDraft.booleanValue());
						List<QADialog> dialogs = (List<QADialog>)ctx.getCtxAttr("dialogs"); // 目前已經進行中的 dialog
						
						if (dialogs == null) {
							dialogs = new ArrayList<QADialog>();
							ctx.setCtxAttr("dialogs", dialogs);
						}
						
						if (dialogs.size() > 0) {
							QADialog runningDlg = dialogs.get(0);
							ctx.setAnswerText("您正在" + runningDlg.getName() + "情境當中，是否要結束然後進入" + dlg.getName() + "情境？");
							ctx.setHasDirectAnswer(true, ANSWER_TYPE.DIALOG_SWITCH);
							ctx.setCtxAttr("goingToSwitchToDialog", mkey);
							ctx.setCtxAttr("goingToSwitchToDialogQuestion", ctx.getCurrentQuestion());

							return PostRuleCheckResult.DEFAULT_RETURN_RESULT;
						}
						dialogs.add(0, dlg);
						
						if (dlg != null) {
							dlg.activate();
						}
						
						return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_DIALOG_CHAIN);
					}
					else {
						//dlg.activate();
						return new PostRuleCheckResult(PostRuleCheckResult.Status.FORWARD, QAMatchRuleController.RULE_CHAIN_NAME_DIALOG_CHAIN);
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}


	@Override
	public boolean shouldInterceptNextApi(QAContext req, JSONObject curData) {
		boolean should = false;
		if (curData == null) {
			JSONObject qaData = (JSONObject) req.getRequestAttribute(REQ_ATTR_QA_DATA);
			try {
				should = qaData != null && qaData.has("children") && qaData.getJSONArray("children").length() > 0;
			}
			catch (JSONException ignore) {
				ignore.printStackTrace();
			}
		}
		else {
			try {
				JSONArray options = curData.optJSONArray("nextOptions");
				should = options != null && options.length() > 0;
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return should;
	}

	@Override
	public JSONObject prepareInterceptorData(QAContext ctx, JSONObject curData) {
		JSONObject data = curData;
		if (data == null) {
			JSONObject qaData = (JSONObject) ctx.getRequestAttribute(REQ_ATTR_QA_DATA);
			@SuppressWarnings("unchecked")
			Map<String, String> idQaMap = (Map<String, String>) ctx.getRequestAttribute(REQ_ATTR_ID_QA_MAP);
			if (qaData.length() > 0) {
				data = new JSONObject();
				try {
					data.put("qaData", qaData);
					data.put("idQaMap", idQaMap);
				}
				catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return data;
	}

	@Override
	public PostRuleCheckResult onApiChoosedOption(QAContext ctx, QAContext.Option option) {
		/*
		 * 階層式問答較為複雜，目前暫時維持原有的 onApi(Answer+Question)Intercept
		 */
		return super.onApiChoosedOption(ctx, option);
	}

	static JSONArray getChildrenDataFromRequest(HttpServletRequest req, String param)
			throws JSONException {
		JSONArray data = new JSONArray();
		String[] hQaParams = req.getParameterValues(param);
		if (ArrayUtils.isNotEmpty(hQaParams)) {
			for (String p : hQaParams) {
				if (StringUtils.isNotBlank(p)) {
					try {
						JSONObject node = new JSONObject(p);
						String id = node.optString("id");
						// 忘了這邊全部把欄位再重新 trim 一次的目的是為了怕以後 NullPointerException?
						node.put("text", StringUtils.trim(node.optString("text")));
						node.put("optionAction", StringUtils.trim(node.optString("optionAction")));
						node.put("question", StringUtils.trim(node.optString("question")));
						node.put("answer", StringUtils.trim(node.optString("answer")));
						node.put("expiry", StringUtils.trim(node.optString("expiry")));
						node.put("script", StringUtils.trim(node.optString("script")));
						node.put("inputType", StringUtils.trim(node.optString("inputType")));
						node.put("menuView", StringUtils.trim(node.optString("menuView")));
						if (node.has("pipe")) {
							node.put("pipe", StringUtils.trim(node.optString("pipe")));
						}
						node.put("children", getChildrenDataFromRequest(req, "dlg_qa_" + id));
						data.put(node);
					}
					catch (JSONException e) {
						data.put(p);
					}
				}
			}
		}
		return data;
	}

	public boolean supportAnswerTemplate() {
		return true;
	}

}
