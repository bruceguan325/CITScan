package com.intumit.solr.searchKeywords;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Transient;

import org.hibernate.annotations.Entity;
import org.hibernate.annotations.Index;
import org.hibernate.annotations.SQLInsert;

/**
 * The @SQLInsert annotation are not portable and the sql syntax is for MySQL
 * The other DBMS maybe use the "INSERT LOW_PRIORITY INTO xxxx....." instead.
 * 
 * @author herb
 *
 */
@Entity
@SQLInsert(sql="INSERT DELAYED INTO SearchKeywordLog (name, func, logtime) VALUES (?, ?,?)")
public class SearchKeywordLog implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private Long id;

	@Index(name="name")
	private String name;
	@Index(name="name")
	private String func;
	@Transient
	private long frequency;
	@Index(name="logtime")
	private Timestamp logtime;

	public String getFunc() {
		return func;
	}

	public void setFunc(String func) {
		this.func = func;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setLogtime(Timestamp logtime) {
		this.logtime = logtime;
	}

	public Timestamp getLogtime() {
		return logtime;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	public long getFrequency() {
		return frequency;
	}

}
