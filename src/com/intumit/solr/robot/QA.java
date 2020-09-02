package com.intumit.solr.robot;

import java.io.Serializable;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.robot.qaplugin.QAPlugin;
import com.intumit.solr.robot.qaplugin.QAPlugins;
import com.intumit.solr.tenant.Tenant;

public class QA extends SolrInputDocument implements Serializable {
	public static final String FN_QUESTION = "QUESTION_s";
	public static final String FN_BRIEFLY_QUESTION = "Briefly_QUESTION_s";
	public static final String FN_ANSWER = "ANSWER_s";
	public static final String FN_QUESTION_ALT_TPL = "QUESTION_ALT_TPL_ms";
	public static final String FN_QUESTION_EXCLUDE_ALT_TPL = "QUESTION_EXCLUDE_ALT_TPL_ms";
	public static final String FN_TRIGGERED_BY_EVENT_TYPES = "TRIGGERED_BY_EVENT_TYPE_ms";
	public static final String FN_EXPAND_ENTITY_CODES = "EXPAND_ENTITY_CODES_ms";
	public static final String FN_EXPAND_INTENT_TAGS = "EXPAND_INTENT_TAGS_ms";
	public static final String FN_ESSENTIAL_SENTENCES = "ESSENTIAL_SENTENCES_ms";
	public static final String FN_ESSENTIAL_KEYWORDS = "ESSENTIAL_KEYWORDS_ms";
	public static final String FN_QA_LOCALES = "QA_LOCALE_ms";
	public static final String FN_EXTRA_PARAMS = "EXTRA_PARAM_s";
	public static final String FN_IS_NOT_FOR_MLT = "IsNotForMlt_b";
	public static final String FN_IS_NOT_FOR_SEARCH = "IsNotForSearch_b";
	public static final String FN_NOT_APPEND_MLT = "IsNotAppendMlt_b";
	public static final String FN_BANNED = "IsBanned_b";
	public static final String FN_AUDIT = "AUDIT_s";
	public static final String FN_IS_KMS_RELATE_EXPIRED_MEMO = "IsKmsRelateExpiredMemo_b";
	public static final String FN_QA_CATEGORY = "QA_CATEGORY_s";
	public static final String FN_INHERITANT_ALT_TEMPLATES = "InheritantAltTemplate_ms";
	public static final String FN_INHERITANT_ALT_TEMPLATE_MKEYS = "InheritantAltTemplateMkeys_ms";
	public static final String FN_PLUGIN_ID = "ANSWER_PLUGIN_ID_s";
	public static final String FN_RICH_MESSAGE_TYPE = "RichMessageType_ms";
	public static final String FN_STATUS = "Status_s";
	public static final String FN_REJECT = "Reject_s";
	
	static void addFieldByPrefix(SolrInputDocument doc, String prefix, boolean isMV, Object val){
		String[] names = ProcessQADataServlet.getFieldName(prefix, isMV);

		for(String name:names){
			QASaver.setOrUpdateField(doc, name, val);
		}
	}

	static List<String> getMultiValueParameters(String param, String name){
		return QAUtil.parseMultiValue(param);
	}



	public QA() {
		super();
	}
	public QA(Map<String, SolrInputField> fields) {
		super(fields);
	}
	public QA(SolrDocument origDoc) {
		for (String fn: origDoc.getFieldNames()) {
			if (fn.startsWith("_"))
				continue;

			setField(fn, origDoc.getFieldValue(fn));
		}
	}

	public void setId(String val) {
		setField("id", val);
	}
	public void setKid(Long val) {
		setField("kid_l", val);
	}
	public void setDataType(String val) {
		setField("dataType_s", val);
	}
	public void setQuestion(String val) {
		addFieldByPrefix(this, "QUESTION", false, val);
	}
	public void setBrieflyQuestion(String val) {
		addFieldByPrefix(this, "Briefly_QUESTION", false, val);
	}
	public void setStatus(String val) {
		addFieldByPrefix(this, "Status", false, val);
	}
	public void setReject(String val) {
		addFieldByPrefix(this, "Reject", false, val);
	}
	public void setAnswer(String val) {
		addFieldByPrefix(this, "ANSWER", false, val);
	}
	public void setAnswerPhone(String val) {
		addFieldByPrefix(this, "ANSWERPHONE", false, val);
	}
	public void setAndSavePlugin(String pluginId, HttpServletRequest request) {
		QASaver.setOrUpdateField(this, FN_PLUGIN_ID, pluginId);

		for(QAPlugin p : QAPlugins.list()){
			p.onEditorPageSave(this, request, p.getId().equals(pluginId));
		}
	}
	public void setAndSaveExpire(String expireType, HttpServletRequest request) throws ParseException {
		String expireRadio = request.getParameter("expireRadio");
		Date expireDate = null;
		Integer expireDays = null;
		if(StringUtils.isBlank(expireRadio)){
			String expire = request.getParameter("expire");
			expireDate = StringUtils.isBlank(expire)?null:QAUtil.parseDate(expire);
		}else{
			expireDays = Integer.valueOf(expireRadio);
			ExpireDuration duration = ExpireDuration.of(expireDays);

			if (duration != ExpireDuration.forever) {
				Calendar c = Calendar.getInstance();
			    long expireTime = duration.getExpireTime(c.getTime().getTime());
				expireDate = new Date(expireTime);
			}
		}
		QASaver.setOrUpdateField(this, "EXPIRE_dt", expireDate);
		QASaver.setOrUpdateField(this, "EXPIRE_DAYS_i", expireDays);
	}
	public void setAndSaveCategory(String qaCategoryType, HttpServletRequest request) throws ParseException {
		String qaCategory = request.getParameter("qaCategoryRadio");
		QASaver.setOrUpdateField(this, FN_QA_CATEGORY, qaCategory);
	}
	
	public String getQaCategory() {
		return getRealFieldValue(FN_QA_CATEGORY) == null ? null : (String)getRealFieldValue(FN_QA_CATEGORY); 
	}
	public void setTags(List<String> tags) {
		addFieldByPrefix(this, "CATEGORY", true, tags);
	}
	public void setLocales(List<String> tags) {
		QASaver.setOrUpdateField(this, FN_QA_LOCALES, tags);
	}
	public void setUpdateInfo(Date date, AdminUser user) {
		QASaver.setOrUpdateField(this, "updated_dt", date);

		if (user != null)
			QASaver.setOrUpdateField(this, "updatedAdminUserId_i", user.getId());
	}
	public void setAltRebuildDate(Date date) {
		setField("altRebuildDate_dt", date);
	}
	public void setAltRebuildTimeCost(long tc) {
		setField("altRebuildTimeCost_l", tc);
	}
	public void setCustomScript(String customScript) {
		QASaver.setOrUpdateField(this, "CUSTOM_SCRIPT_s", customScript);
	}
	public void setCreateInfo(Date date, AdminUser user) {
		// if already have created_dt, never overwrite it
		if (getFieldValue("created_dt") == null){
			QASaver.setOrUpdateField(this, "created_dt", date);

			if (user != null)
				QASaver.setOrUpdateField(this, "createdAdminUserId_i", user.getId());
		}
	}
	public void setAndSaveEnable(HttpServletRequest request) throws ParseException {
		String enableRadio = request.getParameter("enableRadio");
		Date enableDate = null;
		Integer enableDays = null;
		if(StringUtils.isBlank(enableRadio)){
			String enable = request.getParameter("enableDate");
			enableDate = StringUtils.isBlank(enable)?null:QAUtil.parseDate(enable);
		}else{
			enableDays = Integer.valueOf(enableRadio);
			LocalDateTime ldt = LocalDateTime.ofInstant((new Date()).toInstant(), ZoneId.systemDefault()).plusDays(enableDays);
			enableDate = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
		}
		QASaver.setOrUpdateField(this, "ENABLE_dt", enableDate);
		QASaver.setOrUpdateField(this, "ENABLE_DAYS_i", enableDays);
	}
	public void setQuestionAltTemplates(List<String> qaAltTpls) {
		Map<String, String> altMap = new HashMap<String, String>();
		
		if (qaAltTpls != null)
		for(String qaAltTpl : qaAltTpls){
			Map<String, String> altData = QA.parseQAAlt(qaAltTpl);
			if(!altMap.containsKey(StringUtils.trimToEmpty(altData.get("alt")))){
				altMap.put(StringUtils.trimToEmpty(altData.get("alt")), qaAltTpl);
			}
		}
		QASaver.setOrUpdateField(this, FN_QUESTION_ALT_TPL, new ArrayList<String>(altMap.values()));
		QASaver.setOrUpdateField(this, FN_EXPAND_ENTITY_CODES, findAllEntityCode());
		QASaver.setOrUpdateField(this, FN_EXPAND_INTENT_TAGS, findAllIntentTag());
	}
	public void setExcludeQuestionAltTemplates(List<String> exAltTpls) {
		QASaver.setOrUpdateField(this, FN_QUESTION_EXCLUDE_ALT_TPL, exAltTpls);
		QASaver.setOrUpdateField(this, FN_EXPAND_ENTITY_CODES, findAllEntityCode());
	}
	public void setTriggeredByEventTypes(List<String> eventTypes) {
		QASaver.setOrUpdateField(this, FN_TRIGGERED_BY_EVENT_TYPES, eventTypes);
	}
	public void setEssentialSentences(List<String> leks) {
		QASaver.setOrUpdateField(this, FN_ESSENTIAL_SENTENCES, leks);
	}
	public void setEssentialKeywords(Collection<String> eks) {
		QASaver.setOrUpdateField(this, FN_ESSENTIAL_KEYWORDS, eks);
	}
	public void setAltCount(int altCount) {
		setField("AltCount_i", altCount);
	}
	public String getQuestion() {
		return (String)getRealFieldValue(FN_QUESTION);
	}
	public String getBrieflyQuestion() {
		return (String)getRealFieldValue(FN_BRIEFLY_QUESTION);
	}
	public String getStatus() {
		return (String)getRealFieldValue(FN_STATUS);
	}
	public String getReject() {
		return (String)getRealFieldValue(FN_REJECT);
	}
	public void setExtraParams(String val) {
		setField(FN_EXTRA_PARAMS, val);
	}
	public void setExtraParams(JSONObject val) {
		QASaver.setOrUpdateField(this, FN_EXTRA_PARAMS, val);
	}
	public void setOrUpdateField(String fn, Object val) {
		QASaver.setOrUpdateField(this, fn, val);
	}
	public JSONObject getExtraParams() {
		String jsonStr = (String)getRealFieldValue(FN_EXTRA_PARAMS);
		
		if (jsonStr != null && StringUtils.isNotEmpty(jsonStr)) {
			try {
				return new JSONObject(jsonStr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	public List<String> getQuestionAltTemplates() {
		Object obj = getRealFieldValue(FN_QUESTION_ALT_TPL);

		if (obj != null) {
			if (obj instanceof List) {
				return (List<String>)getRealFieldValue(FN_QUESTION_ALT_TPL);
			}
			else {
				return Arrays.asList(new String[] {getRealFieldValue(FN_QUESTION_ALT_TPL).toString()});
			}
		}
		return new ArrayList<String>();
	}
	public List<String> getTriggeredByEventTypes() {
		Object obj = getRealFieldValue(FN_TRIGGERED_BY_EVENT_TYPES);

		if (obj != null) {
			if (obj instanceof List) {
				return (List<String>)getRealFieldValue(FN_TRIGGERED_BY_EVENT_TYPES);
			}
			else {
				return Arrays.asList(new String[] {getRealFieldValue(FN_TRIGGERED_BY_EVENT_TYPES).toString()});
			}
		}
		return new ArrayList<String>();
	}
	public List<String> getExcludeQuestionAltTemplates() {
		Object obj = getRealFieldValue(FN_QUESTION_EXCLUDE_ALT_TPL);

		if (obj != null) {
			if (obj instanceof List) {
				return (List<String>)getRealFieldValue(FN_QUESTION_EXCLUDE_ALT_TPL);
			}
			else {
				return Arrays.asList(new String[] {getRealFieldValue(FN_QUESTION_EXCLUDE_ALT_TPL).toString()});
			}
		}
		return new ArrayList<String>();
	}
	
	public void setAndSaveAudit(String qaAudit) throws ParseException {
		QASaver.setOrUpdateField(this, FN_AUDIT, qaAudit);
	}

	public Boolean getIsBanned() {
		Boolean b = (Boolean)getRealFieldValue(FN_BANNED);
		return b == null ? Boolean.FALSE : b;
	}
	public void setIsBanned(boolean b) {
		setField(FN_BANNED, b);
	}

	public Boolean getIsNotForMLT() {
		Boolean b = (Boolean)getRealFieldValue(FN_IS_NOT_FOR_MLT);
		return b == null ? Boolean.FALSE : b;
	}
	public void setIsNotForMLT(boolean b) {
		setField(FN_IS_NOT_FOR_MLT, b);
	}

	public Boolean getIsNotForSearch() {
		Boolean b = (Boolean)getRealFieldValue(FN_IS_NOT_FOR_SEARCH);
		return b == null ? Boolean.FALSE : b;
	}
	public void setIsNotForSearch(boolean b) {
		setField(FN_IS_NOT_FOR_SEARCH, b);
	}

	public Boolean getNotAppendMLT() {
		Boolean b = (Boolean)getRealFieldValue(FN_NOT_APPEND_MLT);
		return b == null ? Boolean.FALSE : b;
	}
	public void setNotAppendMLT(boolean b) {
		setField(FN_NOT_APPEND_MLT, b);
	}

	public String getId() {
		Object obj = getRealFieldValue("id");
		if (obj != null) {
			if (obj instanceof String) {
				return (String)obj;
			}
		}

		return null;
	}

	public Boolean getIsKmsRelateExpiredMemo() {
		Boolean b = (Boolean)getRealFieldValue(FN_IS_KMS_RELATE_EXPIRED_MEMO);
		return b == null ? Boolean.FALSE : b;
	}
	public void setIsKmsRelateExpiredMemo(boolean b) {
		setField(FN_IS_KMS_RELATE_EXPIRED_MEMO, b);
	}

	public Long getKid() {
		Object obj = getRealFieldValue("kid_l");
		if (obj != null) {
			if (obj instanceof Long) {
				return (Long)getRealFieldValue("kid_l");
			}
		}

		return null;
	}

	public List<String> getTags() {
		Object obj = getRealFieldValue("CATEGORY_ms");

		if (obj != null) {
			if (obj instanceof List) {
				return (List<String>)getRealFieldValue("CATEGORY_ms");
			}
			else {
				return Arrays.asList(new String[] {getRealFieldValue("CATEGORY_ms").toString()});
			}
		}
		return new ArrayList<String>();
	}

	Object getRealFieldValue(String name) {
		Object val = this.containsKey(name) ? this.get(name).getValue() : null;

		if (val instanceof Map) {
			Map m = (Map)val;

			if (m.containsKey("set")) {
				return m.get("set");
			}
			else if (m.containsKey("add")) {
				return m.get("add");
			}
			else {
				throw new RuntimeException("Unknown SolrInputDocument Field:" + m);
			}
		}
		else {
			return val;
		}
	}

	public void setBinderIds(Set<Long> binderIds) {
		QASaver.setOrUpdateField(this, "BINDER_ID_ml", binderIds);
	}

	public LinkedHashSet<Long> getBinderIds() {
		List<Long> list = new ArrayList<>();
		Object obj = getRealFieldValue("BINDER_ID_ml");
		if (obj != null) {
			if (obj instanceof List) {
				list = (List<Long>)getRealFieldValue("BINDER_ID_ml");
			}
			else if(obj instanceof Set) {
				list.addAll((Set<Long>)getRealFieldValue("BINDER_ID_ml"));
			}
			else {
				list = Arrays.asList(new Long[] {(Long)getRealFieldValue("BINDER_ID_ml")});
			}
		}
		return new LinkedHashSet<Long>(list);
	}
	
	public static String parseAndGetQAAltOnly(String alt) {
		String a = null;
		
		if (StringUtils.contains(alt, "//")) {
			a = StringUtils.trimToEmpty(StringUtils.substringBeforeLast(alt, "//"));
		}
		else {
			a = StringUtils.trimToEmpty(alt);
		}
		
		return a;
	}
	
	public static Map<String, String> parseQAAlt(String alt) {
		Map<String, String> map = new HashMap<String, String>(4);
		String a = null;
		String ae = null;
		String t = null;
		String te = null;
		
		if (StringUtils.contains(alt, "//")) {
			a = StringUtils.trimToEmpty(StringUtils.substringBeforeLast(alt, "//"));
			String cmt = StringUtils.trimToEmpty(StringUtils.substringAfterLast(alt, "//"));
			
			try {
				JSONObject j = new JSONObject(cmt);
				ae = j.optString("alt.editor");
				t = j.optString("testCase");
				te = j.optString("testCase.editor");
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			a = alt;
		}
		
		map.put("alt", a);
		map.put("alt.editor", ae);
		map.put("testCase", t);
		map.put("testCase.editor", te);
		
		return map;
	}
	

	public static String joinQAAlt(Map<String, String> altMetaMap) {
		String a = altMetaMap.get("alt");
		String ae = altMetaMap.get("alt.editor");
		String t = altMetaMap.get("testCase");
		String te = altMetaMap.get("testCase.editor");
		
		return joinQAAlt(a, ae != null ? new Integer(ae) : null, t, te != null ? new Integer(te) : null);
	}
	
	public static String joinQAAlt(String alt, Integer altEditorId, String testCase, Integer testCaseEditorId) {
		JSONObject obj = new JSONObject();
		
		try {
			if (altEditorId != null) obj.put("alt.editor", altEditorId);
			if (testCase != null) obj.put("testCase", testCase);
			if (testCaseEditorId != null) obj.put("testCase.editor", testCaseEditorId);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
		String altTplLine = alt;
		if (obj.keySet().size() > 0) 
			altTplLine += " // " + obj.toString();
		
		return altTplLine;
	}
	
	
	/**
	 * 從斷句及排除斷句中找出所有用到的 Intent
	 * 
	 * @return
	 */
	public Collection<String> findAllIntentTag() {
		Collection<String> qmExpaned = getQuestionAltTemplates();
		if (qmExpaned == null) {
			qmExpaned = new ArrayList<>();
		}
		Set<String> set = new HashSet<String>();

		for (String alt: qmExpaned) {
			String t = parseAndGetQAAltOnly(alt);
			Matcher m = QAUtil.intentBlock.matcher(t);
				
			while (m.find()) {
				String g = m.group(1);
				
				if (StringUtils.isNotEmpty(g)) {
					for (String inner: StringUtils.split(g, "|")) {
						if (StringUtils.startsWith(inner, "@")) {
							set.add(StringUtils.substring(inner, 1));
						}
					}
				}
			}
		}
		
		return set.size() > 0 ? set : null;
	}
	
	/**
	 * 從斷句及排除斷句中找出所有用到的 Entity
	 * 
	 * @return
	 */
	public Collection<String> findAllEntityCode() {
		Set<String> set = new HashSet<String>();

		Collection<String> qmExpaned = getQuestionAltTemplates();
		if (qmExpaned != null) {
			for (String alt: qmExpaned) {
				String t = parseAndGetQAAltOnly(alt);
				Matcher m = QAUtil.entityBlock.matcher(t);
					
				while (m.find()) {
					String g = m.group(1);
					
					if (StringUtils.isNotEmpty(g)) {
						for (String inner: StringUtils.split(g, "|")) {
							if (StringUtils.startsWith(inner, "#")) {
								set.add(StringUtils.substring(inner, 1));
		}
						}
					}
				}
			}
		}

		// 排除句 有用到 Entity 的話也要加入清單
		qmExpaned = getExcludeQuestionAltTemplates();
		if (qmExpaned != null) {
		for (String alt: qmExpaned) {
			String t = parseAndGetQAAltOnly(alt);
			Matcher m = QAUtil.entityBlock.matcher(t);
				
			while (m.find()) {
				String g = m.group(1);
				
				if (StringUtils.isNotEmpty(g)) {
					for (String inner: StringUtils.split(g, "|")) {
						if (StringUtils.startsWith(inner, "#")) {
							set.add(StringUtils.substring(inner, 1));
						}
					}
				}
			}
		}
		}
		
		return set.size() > 0 ? set : null;
	}
}
