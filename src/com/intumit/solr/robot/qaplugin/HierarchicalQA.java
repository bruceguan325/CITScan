package com.intumit.solr.robot.qaplugin;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.SearchManager;
import com.intumit.solr.robot.QA;
import com.intumit.solr.robot.QAContext;
import com.intumit.solr.robot.QAContext.MenuSelectionBehavior;
import com.intumit.solr.robot.QAContext.OptionAction;
import com.intumit.solr.robot.QAContext.MenuView;
import com.intumit.solr.robot.QAContext.Option;
import com.intumit.solr.robot.QAContext.OptionMenu;
import com.intumit.solr.robot.QASaver;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.QAUtil.FormalAnswerReplacer;
import com.intumit.solr.robot.qadialog.GroovyUtil;
import com.intumit.solr.robot.TemplateUtil;
import com.intumit.solr.robot.qarule.PostRuleCheckResult;
import com.intumit.solr.tenant.Tenant;

import groovy.lang.Binding;

public class HierarchicalQA extends QAPlugin {

	public static final String HIERARCHICAL_QA_FIELD_NAME = "HIERARCHICAL_QA_s";
	public static final String HIERARCHICAL_QA_REDIRECT_TO_QA_IDS = "HIERARCHICAL_QA_REDIRECT_IDS_ms";

	public static final String ID = "3";

	public static final String REQ_ATTR_QA_KID = "_hQaKid";
	public static final String REQ_ATTR_ID_QA_MAP = "_hIdQaMap";
	public static final String REQ_ATTR_QA_DATA = "_hQaData";
	
	/**
	 * 這兩個選項應該移到 HIERARCHICAL_QA_FIELD_NAME 這個欄位的 JSONObject 根節點當中變成 properties
	 */
	public static final String PERSONALIZED_QA = "_hQaIsPersonalized";
	public static final String APPEND_PARENT_OPTIONS = "_hQaAppendParentOptions";

	public static final String[] JSON_PROPS = new String[] {"id", "text", "optionAction", "question", "answer", "expiry", "pipe", "children", "script", "inputType", "menuView", "matchSentences", "extraParams"};

	HierarchicalQA(String id, String name) {
		super(id, name);
	}
	
	public static JSONObject getDataFromDocument(SolrDocument doc) {
		JSONObject o = getDataFromString((String) doc.getFieldValue(HIERARCHICAL_QA_FIELD_NAME));
		return o;
	}
	
	public static JSONObject getDataFromDocument(SolrInputDocument doc) {
		JSONObject o = getDataFromString((String) doc.getFieldValue(HIERARCHICAL_QA_FIELD_NAME));
		return o;
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
		JSONObject data = getDataFromDocument(doc);
		Tenant tenant = Tenant.getFromSession(req.getSession());
		try {
			Map<String, String> id2QuestionMap = getIdToQuestionMap(tenant, getQaIds(data));
			removeOrphanQa(data, id2QuestionMap);
			req.setAttribute(REQ_ATTR_QA_DATA, data);
			req.setAttribute(REQ_ATTR_ID_QA_MAP, id2QuestionMap);
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return "/wiseadm/qa-plugin/hierarchical-qa-editor.jsp";
	}

	@Override
	public void onEditorPageSave(SolrInputDocument doc, HttpServletRequest req,
			boolean selected) {
		JSONObject data = new JSONObject();
		JSONArray _1stLvChildren = null;
		try {
			_1stLvChildren = getChildrenDataFromRequest(req, "h_qa");
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		if (_1stLvChildren == null)
			_1stLvChildren = new JSONArray();
		try {
			data.put("children", _1stLvChildren);
			
			String entryPoint = StringUtils.trimToNull(req.getParameter("entryPoint"));
			if (entryPoint != null) {
				data.put("entryPoint", entryPoint);
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		
		QASaver.setOrUpdateField(doc, HIERARCHICAL_QA_FIELD_NAME, (!selected || data == null) ? null : data.toString());
		
		// 儲存此階層當中有引用到的其他 QA ID 到索引當中，方便找關連
		if (data != null) {
			Set<String> redirectToQaIds = getQaIds(data);
			QASaver.setOrUpdateField(doc, HIERARCHICAL_QA_REDIRECT_TO_QA_IDS, redirectToQaIds);
		}
		else {
			QASaver.setOrUpdateField(doc, HIERARCHICAL_QA_REDIRECT_TO_QA_IDS, null);
		}
	}

	@Override
	public String onAnswerPreview(String origAnswer, HttpServletRequest req) {
		JSONObject data = new JSONObject();
		try {
			data.put("children", getChildrenDataFromRequest(req, "h_qa"));
			removeExpiredQa(data);
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
		Tenant tenant = Tenant.getFromSession(req.getSession());
		req.setAttribute("answer", origAnswer);
		req.setAttribute(REQ_ATTR_QA_DATA, data);
		req.setAttribute(REQ_ATTR_ID_QA_MAP, getIdToQuestionMap(tenant, getQaIds(data)));

		String b = StringUtils.trimToNull(req.getParameter(PERSONALIZED_QA));
		if (b != null) req.setAttribute(PERSONALIZED_QA, b);
		b = StringUtils.trimToNull(req.getParameter(APPEND_PARENT_OPTIONS));
		if (b != null) req.setAttribute(APPEND_PARENT_OPTIONS, b);

		return "/wiseadm/qa-plugin/hierarchical-qa-preview.jsp";
	}

	@Override
	public PostRuleCheckResult onApiAfterProfessionalIndexMatching(String question, final QAContext ctx) {

		QA directAnswer = ctx.getCurrentQA();
		JSONObject data = new JSONObject();
		JSONArray firstLvChildren = null;
		Long currentKid = null;

		try {
			if (directAnswer != null) {
				data = getDataFromDocument(directAnswer);

				Boolean b = (Boolean) directAnswer.getFieldValue("HIERARCHICAL_QA_PERSONALIZE_b");
				if (b != null) ctx.setRequestAttribute(PERSONALIZED_QA, b);
				else ctx.setRequestAttribute(PERSONALIZED_QA, Boolean.FALSE);

				b = (Boolean) directAnswer.getFieldValue("HIERARCHICAL_QA_APPEND_PARENT_OPT_b");
				if (b != null) ctx.setRequestAttribute(APPEND_PARENT_OPTIONS, b);
				else ctx.setRequestAttribute(APPEND_PARENT_OPTIONS, Boolean.FALSE);

				currentKid = directAnswer.getKid();
				ctx.setRequestAttribute(REQ_ATTR_QA_KID, currentKid);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, String> id2QuestionMap = getIdToQuestionMap(ctx.getTenant(), getQaIds(data));
		try {
			removeOrphanQa(data, id2QuestionMap);
			removeExpiredQa(data);
			firstLvChildren = data.optJSONArray("children");
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		ctx.setRequestAttribute(REQ_ATTR_QA_DATA, data);
		ctx.setRequestAttribute(REQ_ATTR_ID_QA_MAP, id2QuestionMap);

		String ep = data.optString("entryPoint");
		Long entryPointId = null;
		if (StringUtils.trimToNull(ep) != null) {
			entryPointId = new Long(ep);
		}

		String NL = ctx.getQAChannelInstance().getUseHtmlNewline() ? "<br>" : "\n";
		boolean personalized = Boolean.TRUE.equals((Boolean) ctx.getRequestAttribute(PERSONALIZED_QA));
		boolean appendParentOpt = Boolean.TRUE.equals((Boolean) ctx.getRequestAttribute(APPEND_PARENT_OPTIONS));
		BuildMenuData md = new BuildMenuData(new HashSet<String>());
		OptionMenu m = buildHierachicalOptionMenu(ctx, currentKid, MenuView.ORDERED_LIST,
				MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE, NL, NL, firstLvChildren, id2QuestionMap, entryPointId, personalized, "QA-"
						+ currentKid, null, appendParentOpt, md);

		
		// buildHierachicalOptionMenu 不會把 menu 加入 qa context 當中，所以要自己加入
		if (m != null && m.optionsSize() > 0) {
			ctx.addOptionMenu(m);
		}
		if (md.entryMenu != null) {
			m = md.entryMenu;
		}
		if (md.entryOption != null) {
			ParsedOption po = md.entryOption.getValue();
			
			// 如果進入點也有 script，要在這裡執行，不然沒地方會跑
			if (po != null && StringUtils.isNotEmpty(po.script)) {
				Binding binding = new Binding();
				binding.setProperty("ctx", ctx);
				binding.setProperty("option", po);
				GroovyUtil.runScript(binding, po.script);
			}
		}
		// 不管上面有沒有換成 md.entryMenu，這裡都要把 menu 秀出來
		if (m != null && m.optionsSize() > 0) {
			ctx.addAndShowOptionMenu(m);
		}
		
		return null;
	}

	public static ParsedOption parseOption(JSONObject json) {

		ParsedOption o = new ParsedOption();
		try {
			//System.out.println(json.toString(2));
			o.id = json.optLong("id");
			o.title = StringUtils.trimToNull(json.optString("text"));

			try {
				o.optionAction = OptionAction.valueOf(StringUtils.trimToNull(json.optString("optionAction")));
			}
			catch (Exception ignore) {
			}
			o.question = StringUtils.trimToNull(json.optString("question"));
			o.answer = StringUtils.trimToNull(json.optString("answer"));
			o.matchSentences = json.optJSONArray("matchSentences");
			o.expiry = StringUtils.trimToNull(json.optString("expiry"));
			o.pipe = StringUtils.trimToNull(json.optString("pipe"));
			o.script = StringUtils.trimToNull(json.optString("script"));
			o.extraParams = json.optJSONObject("extraParams");
			try {
				o.inputType = MenuSelectionBehavior.valueOf(StringUtils.trimToNull(json.optString("inputType")));
			}
			catch (Exception ignore) {
			}
			try {
				o.menuView = MenuView.valueOf(StringUtils.trimToNull(json.optString("menuView")));
			}
			catch (Exception ignore) {
			}
			o.children = json.optJSONArray("children");

			if (o.optionAction == null) {
				// 往前相容，所以根據舊的資料來判斷可能的 optionAction
				boolean hasChildren = o.children != null && o.children.length() > 0;
				
				if (!hasChildren && o.answer != null) {
					o.optionAction = OptionAction.DIRECT_ANSWER;
				}
				else if (o.pipe != null) {
					o.optionAction = OptionAction.REDIRECT_TO_QUESTION;
				}
				else if (hasChildren) {
					o.optionAction = OptionAction.SUB_MENU_ONLY;
				}
				else {
					o.optionAction = OptionAction.INPUT_TEXT;
				}
			}

			if (o.inputType == null) {
				o.inputType = MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE;
			}

			if (o.menuView == null) {
				o.menuView = MenuView.ORDERED_LIST;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

		return o;
	}

	public static JSONObject parsedOptionToJSONObject(ParsedOption o) {
		JSONObject json = new JSONObject();
		
		try {
			json.put("id", o.id);
			json.put("text", o.title);

			if (o.optionAction == null) {
				boolean hasChildren = o.children != null && o.children.length() > 0;
				
				// 往前相容，所以根據舊的資料來判斷可能的 optionAction
				if (!hasChildren && o.answer != null) {
					o.optionAction = OptionAction.DIRECT_ANSWER;
				}
				else if (o.pipe != null) {
					o.optionAction = OptionAction.REDIRECT_TO_QUESTION;
				}
				else if (hasChildren) {
					o.optionAction = OptionAction.SUB_MENU_ONLY;
				}
				else {
					o.optionAction = OptionAction.INPUT_TEXT;
				}
			}

			json.put("optionAction", o.optionAction.name());
			
			if (o.question != null) json.put("question", o.question);
			if (o.answer != null) json.put("answer", o.answer);
			if (o.matchSentences != null) json.put("matchSentences", o.matchSentences);
			if (o.expiry != null) json.put("expiry", o.expiry);
			if (o.pipe != null) json.put("pipe", o.pipe);
			if (o.script != null) json.put("script", o.script);
			if (o.extraParams != null) json.put("extraParams", o.extraParams);
			
			if (o.inputType != null) json.put("inputType", o.inputType.name());
			else json.put("inputType", MenuSelectionBehavior.NUMBER_OR_FULL_MATCH_TITLE.name());
			
			if (o.menuView != null) json.put("menuView", o.menuView.name());
			else json.put("menuView", MenuView.ORDERED_LIST.name());

			if (o.children != null) json.put("children", o.children);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

		return json;
	}
	
	private class BuildMenuData {
		Set<String> proccessed;
		Option entryOption = null;
		OptionMenu entryMenu = null;
		public BuildMenuData(Set<String> proccessed) {
			super();
			this.proccessed = proccessed;
		}
	}

	/**
	 * 
	 * @param ctx
	 * @param kid
	 * @param view
	 * @param behavior
	 * @param header
	 * @param footer
	 * @param data
	 * @param idQaMap
	 * @param entryPointId
	 * @param doPersonalize
	 * @param key
	 * @param parentMenu
	 * @param appendParentOpt
	 * @param menuData
	 * @return OptionMenu
	 */
	private OptionMenu buildHierachicalOptionMenu(QAContext ctx, Long kid, MenuView view,
			MenuSelectionBehavior behavior, String header, String footer,
			JSONArray data, Map<String, String> idQaMap, Long entryPointId,
			boolean doPersonalize, String key, OptionMenu parentMenu, boolean appendParentOpt, BuildMenuData menuData) {

		if (data.length() > 0) {
			FormalAnswerReplacer far = ctx.getQAUtil().getFormalAnswerReplacer(ctx);
			String NL = ctx.getQAChannelInstance().getUseHtmlNewline() ? "<br>" : "\n";
			String INDENT_OR_SPLITTER = ctx.isClientSupportHtml() ? "&nbsp;&nbsp;" : "  ";

			OptionMenu menu = ctx.createOptionMenu(key, view, QAUtil.DATATYPE_COMMON_SENSE + "-" + kid, behavior, header, footer, doPersonalize,
					INDENT_OR_SPLITTER);

			for (int i = 0; i < data.length(); i++) {
				Object obj = data.opt(i);
				ParsedOption o = null;

				if (obj instanceof String) {
					// 理論上應該不會再進入這區，每一個 JSONArray 當中的都應該要是 JSON Object
					String text = StringUtils.trimToEmpty(idQaMap.get((String) obj));

					if (StringUtils.isNotEmpty(text)) {
						text = TemplateUtil.process(ctx, text, far);
					}

					o = new ParsedOption(text, text, OptionAction.INPUT_TEXT);
				}
				else if (obj instanceof JSONObject) {
					JSONObject json = (JSONObject) obj;
					o = parseOption(json);
				}

				try {
					Long refToKid = kid;
					
					if (StringUtils.isNotEmpty(o.title)) {
						o.title = TemplateUtil.process(ctx, o.title, far);
					}

					if (StringUtils.trimToNull(o.pipe) != null) {
						// 如果有 pipe，代表是一個 pipe 到現有其他問答的選項，直接從 idQaMap 取
						if (OptionAction.REDIRECT_TO_QUESTION == o.optionAction ) {
							o.question = idQaMap.get(o.pipe);
							refToKid = QAUtil.id2Kid(o.pipe);
						}
						else {
							// 這裡是一個 hack，假設 key 為 "QA-1001001-0-1"，這裡希望取得的是 QA-1001001（實際上 key 的邏輯是 QA-{KID}-{Level0 IDX}-{Level1 IDX}...）
							int secondDashPos = key.indexOf("-", 3); // 3 就是跳過 "QA-"
							String topLevelKey = secondDashPos != -1 ? key.substring(0, secondDashPos) : key;
							
							// 利用 "::" 來代表 scope，利用 scope 限制避免不同問答所建置的階層有同樣的 id（用複製功能的時候很常發生）
							// QAContext.findOption 的時候會用到 scope
							o.pipe = topLevelKey + "::" + o.pipe;
						}
					}
					else if (StringUtils.isNotEmpty(o.question)) {
						try {
							o.question = TemplateUtil.process(ctx, o.question, far);
						}
						catch (Exception ex) {
							System.out.println("TemplateUtil process question error [" + o.question + "]");
							ex.printStackTrace();
						}
					}

					if (o.children != null && o.children.length() > 0) {
						// 先留空位，因為特殊原因我們必需採用 BFS 方式建立選單，不能走 DFS
						String subMenuKey = key + "-" + i;
						menu.addOption(QAPlugins.HQA_ID, subMenuKey, o.optionAction, o.title, o, refToKid);
					}
					else {
						menu.addOption(QAPlugins.HQA_ID, appendParentOpt ? key : null, o.optionAction, o.title, o, refToKid);
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}

			// 重新掃一次，補上 subMenu，為了 BFS 所以這樣做
			for (QAContext.Option option : menu.getOptions()) {
				if (option.getSubMenuKey() != null) {
					OptionMenu subMenu = ctx.findOptionMenu(option.getSubMenuKey());

					if (!menuData.proccessed.contains(option.getSubMenuKey())) {
						ParsedOption o = (ParsedOption) option.getValue();
						//System.out.println("*****" + entryPointId + "/[" + o.id + "]");
						//System.out.println(o);
						subMenu = buildHierachicalOptionMenu(ctx, kid, o.menuView, o.inputType,
								StringUtils.isNotEmpty(o.question) ? o.question + NL : NL, 
								(OptionAction.DIRECT_ANSWER != o.optionAction && StringUtils.isNotEmpty(o.answer)) ? o.answer + NL : NL, 
								o.children, idQaMap, entryPointId, 
								false, option.getSubMenuKey(), menu, appendParentOpt, menuData);

						if (subMenu != null && subMenu.optionsSize() > 0) {
							if (entryPointId != null && entryPointId.equals(o.id)) {
								menuData.entryMenu = subMenu;
								menuData.entryOption = option;
							}
						
							ctx.addOptionMenu(subMenu);
						}

						menuData.proccessed.add(option.getSubMenuKey());
					}
				}
			}

			if (appendParentOpt && parentMenu != null) {
				// 加入上層選單
				menu.appendOptions(parentMenu.getOptions());
			}

			if (menu.optionsSize() > 0) {
				return menu;
			}
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
						node.put("children", getChildrenDataFromRequest(req, "h_qa_" + id));
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

	/**
	 * 遞迴爬過所有 array，把所有 REDIRECT_TO_QUESTION 的 pipe 拿出來
	 * 
	 * @param data
	 * @return
	 */
	static Set<String> getQaIds(JSONObject data) {
		Set<String> ids = new HashSet<String>();
		try {
			JSONArray children = data.optJSONArray("children");
			if (children != null) {
				for (int i = 0; i < children.length(); i++) {
					Object obj = children.opt(i);
					if (obj instanceof String) {
						ids.add((String) obj);
					}
					else if (obj instanceof JSONObject) {
						JSONObject json = (JSONObject) obj;

						String oaName = json.optString("optionAction");
						OptionAction oa = null;
						if (StringUtils.trimToNull(oaName) != null) {
							oa = OptionAction.valueOf(oaName);
						}

						if (OptionAction.REDIRECT_TO_QUESTION == oa) {
							ids.add(json.optString("pipe"));
						}
						ids.addAll(getQaIds(json));
					}
				}
			}
		}
		catch (JSONException e) {
			e.printStackTrace();
		}
		return ids;
	}

	static void removeOrphanQa(JSONObject data, Map<String, String> hIdQaMap)
			throws JSONException {
		JSONArray children = data.optJSONArray("children");
		JSONArray newChildren = new JSONArray();
		if (children != null) {
			for (int i = 0; i < children.length(); i++) {
				Object obj = children.opt(i);
				
				if (obj instanceof String) {
					String id = (String) obj;
					if (hIdQaMap.containsKey(id)) {
						newChildren.put(id);
					}
				}
				else if (obj instanceof JSONObject) {
					JSONObject json = (JSONObject) obj;
					json = parsedOptionToJSONObject(parseOption(json));
					JSONObject newJson = new JSONObject(json, JSON_PROPS);
					removeOrphanQa(newJson, hIdQaMap);
					newChildren.put(newJson);
				}
				
			}
		}
		data.put("children", newChildren);
	}

	static void removeExpiredQa(JSONObject data)
			throws JSONException {
		JSONArray children = data.optJSONArray("children");
		JSONArray newChildren = new JSONArray();
		if (children != null) {
			for (int i = 0; i < children.length(); i++) {
				Object obj = children.opt(i);
				if (obj instanceof JSONObject) {
					JSONObject json = (JSONObject) obj;
					if (!QAUtil.isDateExpired(json.optString("expiry"))) {
						JSONObject newJson = new JSONObject(json, JSON_PROPS);
						removeExpiredQa(newJson);
						newChildren.put(newJson);
					}
				}
				else if (obj instanceof String) {
					String id = (String) obj;
					newChildren.put(id);
				}
			}
		}
		data.put("children", newChildren);
	}

	static Map<String, String> getIdToQuestionMap(Tenant tenant, Set<String> hQaIds) {
		Map<String, String> rs = new HashMap<String, String>();
		if (CollectionUtils.isNotEmpty(hQaIds)) {
			String query = "";
			for (String id : hQaIds) {
				try {
					if (id != null) {
						query += (StringUtils.isEmpty(query) ? "" : " OR ") + "id:\""
								+ URLEncoder.encode(id, "UTF-8") + "\"";
					}
				}
				catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			SolrQuery q = new SolrQuery();
			q.setQuery(query);
			q.addFilterQuery("-isPart_i:[2 TO *]");
			q.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
			q.setRows(hQaIds.size());

			SolrServer server = tenant.getCoreServer();
			try {
				SolrDocumentList docs = server.query(q).getResults();
				for (SolrDocument doc : docs) {
					String id = (String) doc.getFieldValue("id");
					String ques = (String) doc.getFieldValue("QUESTION_s");
					rs.put(id, ques);
				}
			}
			catch (SolrServerException e) {
				e.printStackTrace();
			}
		}
		return rs;
	}

	public boolean supportAnswerTemplate() {
		return true;
	}

}
