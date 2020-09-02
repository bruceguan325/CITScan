package com.intumit.solr.tenant;

import java.util.HashMap;
import java.util.Map;

public class TenantScopeDataMap {

	static Map<Integer, Map<String, Object>> all = new HashMap<>();
	
	public static Map<String, Object> getMap(int tenantId) {
		if (!all.containsKey(tenantId)) {
			all.put(tenantId, new HashMap<String, Object>());
		}
		
		return all.get(tenantId);
	}
	
	public static Object optValue(int tenantId, String key, Object defaultVal) {
		Map<String, Object> m = getMap(tenantId);
		
		Object val = null;
		if (m.containsKey(key)) {
			val = m.get(key);
		}
		
		if (val == null) {
			return defaultVal;
		}
		
		return val;
	}
	
	public static void setValue(int tenantId, String key, Object val) {
		getMap(tenantId).put(key, val);
	}
}
