package com.intumit.solr.recommendKeywords;

import java.io.Serializable;
import java.sql.Timestamp;

public class RecommendKeyword implements Serializable {

	private Long id;
	private String keyword;
	private Integer sort;
	private String url;
	private String os;
	private Timestamp saveTime;
	private String target;
	
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
	public void setSort(Integer sort) {
		this.sort = sort;
	}
	public Integer getSort() {
		return sort;
	}
	public void setOs(String os) {
		this.os = os;
	}
	public String getOs() {
		return os;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}
	public void setSaveTime(Timestamp saveTime) {
		this.saveTime = saveTime;
	}
	public Timestamp getSaveTime() {
		return saveTime;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getTarget() {
		return target;
	}


}
