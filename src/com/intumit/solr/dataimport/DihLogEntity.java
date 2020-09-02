package com.intumit.solr.dataimport;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import org.hibernate.annotations.Index;

@Entity
public class DihLogEntity {

	@Id @GeneratedValue(strategy=GenerationType.AUTO)
	private int id;


	@Column(length = 32)
	@Index(name = "coreNameIdx")
	private String coreName;

	@Column(length = 64)
	@Index(name = "eventSrcIdx")
	private String eventSource;

	@Column(length = 512, name = "stats")
	private String statistics;

	@Column(length = 512)
	private String parameters;

	@Lob
	private String statusMessage;

	@Lob
	private String errors;

	@Index(name = "timestampIdx")
	private Date timestamp;

	public String getCoreName() {
		return coreName;
	}

	public void setCoreName(String coreName) {
		this.coreName = coreName;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getEventSource() {
		return eventSource;
	}

	public void setEventSource(String eventSource) {
		this.eventSource = eventSource;
	}

	public String getStatistics() {
		return statistics;
	}

	public void setStatistics(String stats) {
		this.statistics = stats;
	}

	public String getParameters() {
		return parameters;
	}

	public void setParameters(String parameters) {
		this.parameters = parameters;
	}

	public String getErrors() {
		return errors;
	}

	public void setErrors(String errors) {
		this.errors = errors;
	}

	public DihLogEntity() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

}
