package com.intumit.solr.util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.intumit.solr.tenant.Tenant;
import com.intumit.systemconfig.WiseSystemConfig;

import flexjson.JSONDeserializer;

public class AnalyticsUtil {
	Tenant t;
	String name;
	JSONObject cfg;
	List<String> facets = null;
	Map<String, Object> fieldNameToResourceKey = null;
	Map<String, Object> paramNameToFieldName = null;
	Map<String, Object> paramNameToResourceKey = null;
	
	List<String> analyticTypes = null;
	Map<String, Map<String, Object>> atConfigMap = null;
	
	boolean init = false;
	
	public AnalyticsUtil(Tenant t, String name) {
		try {
			this.t = t;
			JSONObject setting = new JSONObject(FileUtils.readFileToString(new File(WiSeEnv.getHomePath() + "/analytics.settings.json"), "UTF-8"));
			this.name = name;
			cfg = setting.getJSONObject(name);
			
			init();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	public String getTargetCore() {
		return cfg.optString("targetCore");
	}
	
	public String getFieldWeight() {
		return cfg.optString("fieldWeight");
	}
	
	public String getFieldHighlight() {
		return cfg.optString("fieldHighlight");
	}
	
	void init() {
		if (!init) {
			try {
				facets = new ArrayList<>();
				JSONArray arr = cfg.getJSONArray("facets");
				
				for (int i=0; i < arr.length(); i++) {
					facets.add(arr.getString(i));
				}
				System.out.println(facets);;
				
				fieldNameToResourceKey = (Map)new JSONDeserializer().deserialize(cfg.getJSONObject("fieldNameToResourceKey").toString());
				paramNameToFieldName = (Map)new JSONDeserializer().deserialize(cfg.getJSONObject("paramNameToFieldName").toString());
				paramNameToResourceKey = (Map)new JSONDeserializer().deserialize(cfg.getJSONObject("paramNameToResourceKey").toString());
				
				System.out.println(fieldNameToResourceKey);

				JSONArray arr2 = cfg.getJSONArray("analyticTypes");
				analyticTypes = new ArrayList<>();
				atConfigMap = new HashMap<>();
				
				for (int i=0; i < arr2.length(); i++) {
					JSONObject atCfg = arr2.getJSONObject(i);
					String name = atCfg.getString("name");
					Map<String, Object> mm = (Map)new JSONDeserializer().deserialize(atCfg.toString());
					
					analyticTypes.add(name);
					atConfigMap.put(name, mm);
				}
				System.out.println(atConfigMap);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			init = true;
		}
	}
	
	public String getName() {
		return name;
	}

	public JSONObject getCfg() {
		return cfg;
	}

	public String getDefaultAnalyticType() {
		return cfg.optString("defaultAnalyticType");
	}

	public String getAdditionalFQs() {
		String fqs = cfg.optString("additionalFQs");
		if (StringUtils.isEmpty(fqs)) {
			fqs = "+TenantId_i:" + t.getId();
		}
		else {
			fqs += ",+TenantId_i:" + t.getId();
		}
		return fqs;
	}

	public List<String> getFacets() {
		return facets;
	}


	public Map<String, Object> getFieldNameToResourceKey() {
		return fieldNameToResourceKey;
	}

	public String getFieldNameResourceKey(String fn) {
		return (String)fieldNameToResourceKey.get(fn);
	}

	public Map<String, Object> getParamNameToFieldName() {
		return paramNameToFieldName;
	}

	public String findFieldNameByParam(String param) {
		return (String)paramNameToFieldName.get(param);
	}

	public Map<String, Object> getParamNameToResourceKey() {
		return paramNameToResourceKey;
	}

	public String getParamNameResourceKey(String fn) {
		return (String)paramNameToResourceKey.get(fn);
	}


	public List<String> getAnalyticTypes() {
		return analyticTypes;
	}


	public Map<String, Map<String, Object>> getAnalyticTypeConfigMap() {
		return atConfigMap;
	}


	public Map<String, Object> getAnalyticTypeConfig(String name) {
		return atConfigMap.get(name);
	}

	public boolean isInit() {
		return init;
	}


	public static void main(String[] args) {
		AnalyticsUtil au = new AnalyticsUtil(WiseSystemConfig.get().getDefaultTenant(), "searchLog");
		au.init();
	}
}
