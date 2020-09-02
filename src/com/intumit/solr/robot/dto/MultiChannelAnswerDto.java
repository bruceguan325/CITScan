package com.intumit.solr.robot.dto;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Lob;

import org.hibernate.annotations.Index;

public class MultiChannelAnswerDto implements Serializable {
	
    private int id;
	
	private int tenantId;
	
	private String qaId;
	
	private String channel;
	
	private String userType;
	
	private String answer;
	
	private String answerType;
	
	private String answerVoice;
	
	private String answerMood;
	
	private String lineMKey;
	
	private String channelTypeConfig;

	public int getId() {
		return id;
	}

	protected void setId(int id) {
		this.id = id;
	}

	public int getTenantId() {
		return tenantId;
	}

	protected void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public String getQaId() {
		return qaId;
	}

	protected void setQaId(String qaId) {
		this.qaId = qaId;
	}

	public String getChannel() {
		return channel;
	}

	protected void setChannel(String channel) {
		this.channel = channel;
	}

	public String getUserType() {
		return userType;
	}

	protected void setUserType(String userType) {
		this.userType = userType;
	}

	public String getAnswer() {
		return answer;
	}

	protected void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getAnswerType() {
		return answerType;
	}

	protected void setAnswerType(String answerType) {
		this.answerType = answerType;
	}

	public String getAnswerVoice() {
		return answerVoice;
	}

	protected void setAnswerVoice(String answerVoice) {
		this.answerVoice = answerVoice;
	}

	public String getAnswerMood() {
		return answerMood;
	}

	protected void setAnswerMood(String answerMood) {
		this.answerMood = answerMood;
	}

	public String getLineMKey() {
		return lineMKey;
	}

	protected void setLineMKey(String lineMKey) {
		this.lineMKey = lineMKey;
	}

	public String getChannelTypeConfig() {
		return channelTypeConfig;
	}

	public void setChannelTypeConfig(String channelTypeConfig) {
		this.channelTypeConfig = channelTypeConfig;
	}
	
}
