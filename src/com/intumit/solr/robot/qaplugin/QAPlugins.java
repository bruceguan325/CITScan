package com.intumit.solr.robot.qaplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class QAPlugins {

	public static final String LINK_ID = "1";
	
	public static final String RICHTEXT_ID = "2";
	
	public static final String HQA_ID = HierarchicalQA.ID;
	
	protected static final HashMap<String, Registry> registries = new HashMap<>();
	
	/**
	 * Don't use ID=0，it's for testing purpose.
	 */
	static {
		//registries.put(LINK_ID, new Registry(Link.class, LINK_ID, "external.link"));
		//registries.put(RICHTEXT_ID, new Registry(Richtext.class, RICHTEXT_ID, "format.text"));
		registries.put(HQA_ID, new Registry(HierarchicalQA.class, HQA_ID, "hierarchical.qa"));
		registries.put(CustomQA.ID, new Registry(CustomQA.class, CustomQA.ID, "custom.qa"));
		//registries.put("5", new Registry(CreditCardBalance.class, "5", "credit.card.balance"));
		//registries.put("6", new Registry(CreditCardPoint.class, "6", "credit.card.point"));
		//registries.put("7", new Registry(AccountBalance.class, "7", "account.balance"));
		registries.put("8", new Registry(OtherPossibleQuestions.class, OtherPossibleQuestions.ID, "other.possible.questions"));
		registries.put("9", new Registry(QuestionSearch.class, QuestionSearch.ID, "question.search"));
		//registries.put("10", new Registry(CreditCardAccountAmount.class, "10", "credit.card.account.amount"));
		registries.put(DictionarySearch.ID, new Registry(DictionarySearch.class, DictionarySearch.ID, "question.search"));
		//registries.put(QuestionnaireQA.ID, new Registry(QuestionnaireQA.class, QuestionnaireQA.ID, "questionnaire.qa", true));
		registries.put(QADialogPlugin.ID, new Registry(QADialogPlugin.class, QADialogPlugin.ID, "conversational.qa"));
	}
	
	static class Registry {
		
		final Class<? extends QAPlugin> clazz;
		
		final String id;
		
		final String name;
		
		final boolean deprecated;

		<T extends QAPlugin> Registry(Class<T> clazz, String id, String name) {
			this.clazz = clazz;
			this.id = id;
			this.name = name;
			this.deprecated = false;
		}
		
		<T extends QAPlugin> Registry(Class<T> clazz, String id, String name, boolean deprecated) {
			this.clazz = clazz;
			this.id = id;
			this.name = name;
			this.deprecated = deprecated;
		}
	}
	
	/**
	 * List Plugin which is not deprecated.
	 * @return
	 */
	public static List<QAPlugin> list(){
		List<QAPlugin> plugins = new ArrayList<>();
		for (Registry r : registries.values()) {
			if (r.deprecated) continue;
			
			try {
				plugins.add(QAPlugin.newInstance(r.clazz, r.id, r.name));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return plugins;
	}
	
	/**
	 * List all Plugins, including deprecated ones.
	 * @return
	 */
	public static List<QAPlugin> listAll(){
		List<QAPlugin> plugins = new ArrayList<>();
		for (Registry r : registries.values()) {
			try {
				plugins.add(QAPlugin.newInstance(r.clazz, r.id, r.name));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return plugins;
	}
	
	public static QAPlugin get(String pluginId) {
		if (registries.containsKey(pluginId)) {
			Registry r = registries.get(pluginId);
			try {
				return (QAPlugin)QAPlugin.newInstance(r.clazz, r.id, r.name);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return null;
	}
	
	public static JSONArray parseHQAData(String text) throws JSONException {
		JSONArray oData = new JSONArray();
		String json = StringUtils.defaultString(text).replaceAll("\n", "").replaceAll("\r\n", "");
		JSONArray iData = null;
		try {
			iData = new JSONArray("[" + json + "]");
		}
		catch (JSONException ex2) {
			throw ex2;
		}
		for(int i=0; i<iData.length(); i++){
			JSONArray array = iData.optJSONArray(i);
			if(array == null){
				continue;
			}
			Object item = convertHQADataItem(array);
			if(item != null){
				oData.put(item);
			}
		}
		return oData;
	}
	
	static JSONObject convertHQADataItem(JSONArray item) throws JSONException {
		JSONObject rs = null;
		JSONArray arrayItem = (JSONArray) item;
		List<String> attrs = new ArrayList<String>();
		List<JSONArray> childAttrs = new ArrayList<JSONArray>();
		for(int i=0; i<arrayItem.length(); i++){
			Object obj = arrayItem.opt(i);
			if(obj == null){
				continue;
			}
			if(obj instanceof String){
				attrs.add((String)obj);
			}else if(obj instanceof JSONArray){
				childAttrs.add((JSONArray)obj);
			}
		}
		int strValSize = attrs.size();
		int arrayValSize = childAttrs.size();
		String text = null;
		if(strValSize >= 1){
			text = attrs.get(0); 
		}
		String question = null;
		String answer = null;
		if(strValSize >= 2){
			String val = attrs.get(1);
			if(arrayValSize == 0){
				answer = val;
			}else{
				question = val;
			}
		}
		JSONArray children = new JSONArray();
		for(JSONArray array : childAttrs){
			JSONObject child = convertHQADataItem(array);
			if(child != null){
				children.put(child);
			}
		}
		if(StringUtils.isNotBlank(text)){
			rs = new JSONObject();
			String id = UUID.randomUUID().toString();
			rs.put("id", id);
			rs.put("text", StringUtils.defaultString(text));
			rs.put("question", StringUtils.defaultString(question));
			rs.put("answer", StringUtils.defaultString(answer));
			rs.put("children", children);
		}
		return rs;
	}
	
}
