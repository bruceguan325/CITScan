package com.intumit.solr.dataimport;

public enum Transformer {
	Trim("com.intumit.solr.TrimTransformer", "trim", null),
	Clob("ClobTransformer", "clob", null),
	Html("HTMLStripTransformer", "stripHTML", null)
	;
	
	String clazzName;
	String attrName;
	String specialValue;
	
	Transformer(String clazzName, String attrName, String specialValue) {
		this.clazzName = clazzName;
		this.attrName = attrName;
		this.specialValue = specialValue;
	}

	public String getClazzName() {
		return clazzName;
	}

	public String getAttrName() {
		return attrName;
	}

	public String getSpecialValue() {
		return specialValue;
	}
	
}
