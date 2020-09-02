package com.intumit.solr.dataimport;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.StringUtils;

@Entity
public class DataField {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id")
	Integer id;

	@ManyToOne
	@JoinColumn(name="config_id", nullable=true)
	DataConfig dataConfig;
	
	String dbColumnName;
	String dbColumnType;

	@Column(length = 128)
	String comments;
	
	String indexFieldName;
	String appliedTransformers;
	
	boolean searchable;
	boolean facetable;
	
	boolean primaryKey;
	boolean ignored;
	boolean mainTitle;
	boolean mainText;
	boolean mainDate;
	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public DataConfig getDataConfig() {
		return dataConfig;
	}
	public void setDataConfig(DataConfig dataConfig) {
		this.dataConfig = dataConfig;
	}
	public String getDbColumnName() {
		return dbColumnName;
	}
	public void setDbColumnName(String dbColumnName) {
		this.dbColumnName = dbColumnName;
	}
	public String getDbColumnType() {
		return dbColumnType;
	}
	public void setDbColumnType(String dbColumnType) {
		this.dbColumnType = dbColumnType;
	}
	public String getComments() {
		return comments;
	}
	public void setComments(String comments) {
		this.comments = comments;
	}
	public String getIndexFieldName() {
		return indexFieldName;
	}
	public void setIndexFieldName(String indexFieldName) {
		this.indexFieldName = indexFieldName;
	}
	public String getAppliedTransformers() {
		return appliedTransformers;
	}
	public void setAppliedTransformers(String appliedTransformers) {
		this.appliedTransformers = appliedTransformers;
	}
	public boolean isSearchable() {
		return searchable;
	}
	public void setSearchable(boolean searchable) {
		this.searchable = searchable;
	}
	public boolean isFacetable() {
		return facetable;
	}
	public void setFacetable(boolean facetable) {
		this.facetable = facetable;
	}
	public boolean isPrimaryKey() {
		return primaryKey;
	}
	public void setPrimaryKey(boolean primaryKey) {
		this.primaryKey = primaryKey;
	}
	public boolean isIgnored() {
		return ignored;
	}
	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}
	public boolean isMainTitle() {
		return mainTitle;
	}
	public void setMainTitle(boolean mainTitle) {
		this.mainTitle = mainTitle;
	}
	public boolean isMainText() {
		return mainText;
	}
	public void setMainText(boolean mainText) {
		this.mainText = mainText;
	}
	public boolean isMainDate() {
		return mainDate;
	}
	public void setMainDate(boolean mainDate) {
		this.mainDate = mainDate;
	}
	
	public boolean hasTransformer(Transformer t) {
		String[] tfNames = StringUtils.split(appliedTransformers, ",");
		
		for (String tfName: tfNames) {
			if (tfName.equals(t.name()))
				return true;
		}
		
		return false;
	}
	public String toConfigElement(String indexFiledName) {
		String s = "<field ";
		s += " column=\"" + this.getDbColumnName() + "\"";
		s += " name=\"" + indexFiledName + "\"";
		
		String[] tfNames = StringUtils.split(appliedTransformers, ",");
		
		for (String tfName: tfNames) {
			Transformer t = Transformer.valueOf(tfName);
			String attrVal = StringUtils.defaultIfEmpty(t.getSpecialValue(), "true");
			s += " " + t.getAttrName() + "=\"" + attrVal + "\"";
		}
		
		s += "/>";
		
		return s;
	}
}
