package com.intumit.solr.searchKeywords;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.Index;

@Entity
public class SearchKeywordLogStatisticsHourly implements Serializable {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private Long id;
	private String name;
	private String func;
	
	@Index(name="logtime")
	private Timestamp logtime;
	private int logYear;
	private int logMonth;
	private int logDay;
	private int logHour;
	private long frequency;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFunc() {
		return func;
	}
	public void setFunc(String func) {
		this.func = func;
	}
	public Timestamp getLogtime() {
		return logtime;
	}
	public void setLogtime(Timestamp logtime) {
		this.logtime = logtime;
	}
	public int getLogYear() {
		return logYear;
	}
	public void setLogYear(int logYear) {
		this.logYear = logYear;
	}
	public int getLogMonth() {
		return logMonth;
	}
	public void setLogMonth(int logMonth) {
		this.logMonth = logMonth;
	}
	public int getLogDay() {
		return logDay;
	}
	public void setLogDay(int logDay) {
		this.logDay = logDay;
	}
	public int getLogHour() {
		return logHour;
	}
	public void setLogHour(int logHour) {
		this.logHour = logHour;
	}
	public long getFrequency() {
		return frequency;
	}
	public void setFrequency(long frequency) {
		this.frequency = frequency;
	}

	
}
