package com.intumit.solr.dataimport;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import org.hibernate.annotations.IndexColumn;

@Entity
public class DataConfig {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	Integer id;
	
	String targetCoreName;
	
	String jdbcDriver;
	String jdbcUri;
	String jdbcUser;
	String jdbcPass;
	
	String entityName;
	@Lob
	String transformers;
	@Lob
	String sqlQuery;
	
	String deltaQueryFrom;
	@Lob
	String deltaQueryWhere;
	
	String deleteQueryFrom;
	@Lob
	String deleteQueryWhere;

	@OneToMany(cascade={CascadeType.ALL}, fetch=FetchType.EAGER)
	@JoinColumn(name="config_id")
	@IndexColumn(name="idx")
	List<DataField> fields;

	public DataConfig() {
		super();
	}
	public DataConfig(String coreName) {
		super();
		this.targetCoreName = coreName;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTargetCoreName() {
		return targetCoreName;
	}

	public void setTargetCoreName(String targetCoreName) {
		this.targetCoreName = targetCoreName;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public void setJdbcDriver(String jdbcDriver) {
		this.jdbcDriver = jdbcDriver;
	}

	public String getJdbcUri() {
		return jdbcUri;
	}

	public void setJdbcUri(String jdbcUri) {
		this.jdbcUri = jdbcUri;
	}

	public String getJdbcUser() {
		return jdbcUser;
	}

	public void setJdbcUser(String jdbcUser) {
		this.jdbcUser = jdbcUser;
	}

	public String getJdbcPass() {
		return jdbcPass;
	}

	public void setJdbcPass(String jdbcPass) {
		this.jdbcPass = jdbcPass;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public String getTransformers() {
		return transformers;
	}

	public void setTransformers(String transformers) {
		this.transformers = transformers;
	}

	public String getSqlQuery() {
		return sqlQuery;
	}

	public void setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
	}

	public List<DataField> getFields() {
		return fields;
	}

	public void setFields(List<DataField> fields) {
		this.fields = fields;
	}
	
	public DataField findField(String columnName) {
		if (fields == null) {
			return null;
		}
		for (DataField f: fields) {
			if (f.getDbColumnName().equals(columnName)) {
				return f;
			}
		}
		return null;
	}
	public String getDeltaQueryFrom() {
		return deltaQueryFrom;
	}
	public void setDeltaQueryFrom(String deltaQueryFrom) {
		this.deltaQueryFrom = deltaQueryFrom;
	}
	public String getDeltaQueryWhere() {
		return deltaQueryWhere;
	}
	public void setDeltaQueryWhere(String deltaQueryWhere) {
		this.deltaQueryWhere = deltaQueryWhere;
	}
	public String getDeleteQueryFrom() {
		return deleteQueryFrom;
	}
	public void setDeleteQueryFrom(String deleteQueryFrom) {
		this.deleteQueryFrom = deleteQueryFrom;
	}
	public String getDeleteQueryWhere() {
		return deleteQueryWhere;
	}
	public void setDeleteQueryWhere(String deleteQueryWhere) {
		this.deleteQueryWhere = deleteQueryWhere;
	}
}
