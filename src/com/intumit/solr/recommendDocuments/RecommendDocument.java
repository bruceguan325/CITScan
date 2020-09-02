package com.intumit.solr.recommendDocuments;

import java.io.Serializable;
import java.util.Date;

public class RecommendDocument implements Serializable {

	private String core;
	private String id;
	private String title;
	private String description;
	private String url;
	private Date date;


	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getTitle() {
		return title;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getDescription() {
		return description;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public Date getDate() {
		return date;
	}
	public void setCore(String core) {
		this.core = core;
	}
	public String getCore() {
		return core;
	}


}
