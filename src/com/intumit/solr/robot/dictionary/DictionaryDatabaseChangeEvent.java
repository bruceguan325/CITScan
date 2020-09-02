package com.intumit.solr.robot.dictionary;

import java.io.Serializable;

public class DictionaryDatabaseChangeEvent implements Serializable {
	
	private static final long serialVersionUID = 5772638127716409665L;

	public DictionaryDatabaseChangeEvent(String source, DictionaryDatabase oldOne, DictionaryDatabase newOne, EventType type) {
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
	DictionaryDatabase oldOne;
	DictionaryDatabase newOne;
	EventType type;
	
	@Override
	public String toString() {
		return "DictionaryDatabaseChangeEvent [source=" + source + ", oldOne="
				+ oldOne + ", newOne=" + newOne + ", type=" + type + "]";
	}
}
