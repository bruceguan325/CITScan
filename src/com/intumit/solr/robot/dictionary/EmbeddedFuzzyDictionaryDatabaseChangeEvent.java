package com.intumit.solr.robot.dictionary;

import java.io.Serializable;

import org.apache.commons.lang.builder.ToStringBuilder;

public class EmbeddedFuzzyDictionaryDatabaseChangeEvent implements Serializable {
	
	private static final long serialVersionUID = 735370707101115994L;

	public static enum EventType {
		SAVE, DELETE, UPDATE;
	}
	
	private String source;
	private EventType type;
	private EmbeddedFuzzyDictionaryDatabase efd;
	private EmbeddedFuzzyDictionaryDatabase oldEfd;
	
	public EmbeddedFuzzyDictionaryDatabaseChangeEvent(String source, EventType type, EmbeddedFuzzyDictionaryDatabase efd, EmbeddedFuzzyDictionaryDatabase oldEfd) {
		this.source = source;
		this.type = type;
		this.efd = efd;
		this.oldEfd = oldEfd;
	}
	
	public String getSource() {
		return source;
	}

	public EventType getType() {
		return type;
	}

	public EmbeddedFuzzyDictionaryDatabase getEfd() {
		return efd;
	}
	
	public EmbeddedFuzzyDictionaryDatabase getOldEfd() {
		return oldEfd;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("source", source).append("type", type).append("oldEfd", oldEfd).append("efd", efd).toString();
	}

}
