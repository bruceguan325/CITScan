package com.intumit.solr.config;

import javax.persistence.Entity;
import javax.persistence.Id;

import flexjson.JSONSerializer;

@Entity
public class ColumnNameMapping {

	@Id
	private String columnName;
	private String mappingName;

	public ColumnNameMapping(String fieldName, String mapping) {
		columnName = fieldName;
		mappingName = mapping;
	}
	public ColumnNameMapping() {
	}
	public void setColumnName(String column) {
		this.columnName = column;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setMappingName(String name) {
		this.mappingName = name;
	}

	public String getMappingName() {
		return mappingName;
	}

	public String serializeToJsonString() {
		JSONSerializer serializer = new JSONSerializer();
		return serializer.serialize(this);
	}
}
