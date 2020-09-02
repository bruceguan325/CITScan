package com.intumit.solr.synonymKeywords;

import java.io.Serializable;

public class SynonymKeywordChangeEvent implements Serializable {
	
	public SynonymKeywordChangeEvent(String source, SynonymKeyword oldOne,
			SynonymKeyword newOne, EventType type) {
		super();
		this.source = source;
		this.oldOne = oldOne;
		this.newOne = newOne;
		this.type = type;
	}
	
	public static enum EventType {
		SAVE, UPDATE, DELETE;
	}

	String source;
	SynonymKeyword oldOne;
	SynonymKeyword newOne;
	EventType type;
	
	@Override
	public String toString() {
		return "SynonymKeywordChangeEvent [source=" + source + ", oldOne="
				+ oldOne + ", newOne=" + newOne + ", type=" + type + "]";
	}
}
