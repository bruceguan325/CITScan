package com.intumit.solr.blackKeywords;

import java.io.Serializable;
import java.sql.Timestamp;

import org.hibernate.annotations.Index;

import flexjson.JSONSerializer;

public class BlackKeyword implements Serializable {

	private Long id;
	@Index(name="keyword")
	private String keyword;
	@Index(name="saveTime")
	private Timestamp saveTime;

	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public void setSaveTime(Timestamp saveTime) {
		this.saveTime = saveTime;
	}
	public Timestamp getSaveTime() {
		return saveTime;
	}
	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
}
