package com.intumit.solr.robot;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class QADialogContext implements Serializable {

	/**
	 * A map for storing dialog context level attributes, 
	 * scope is restricted to single context. 
	 * It will be cleared when this qa context be recycled.
	 */
	Map<String, Object> ctxAttr = new HashMap<String, Object>();

	public Object getCtxAttr(String key) {
		return ctxAttr.get(key);
	}

	public Map<String, Object> getCtxAttr() {
		return ctxAttr;
	}

	public void setCtxAttr(String key, Object val) {
		this.ctxAttr.put(key, val);
	}

}
