package com.intumit.solr.robot.dto;

public class MultiChannelAnswerDtoBuilder {
	
	private MultiChannelAnswerDto dto;
	
	public MultiChannelAnswerDtoBuilder() {
		dto = new MultiChannelAnswerDto();
	}
	
	public MultiChannelAnswerDtoBuilder setId(Integer id) {
		dto.setId(id);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setTenantId(Integer tenantId) {
		dto.setTenantId(tenantId);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setQaId(String qaId) {
		dto.setQaId(qaId);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setChannel(String channel) {
		dto.setChannel(channel);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setUserType(String userType) {
		dto.setUserType(userType);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setAnswer(String answer) {
		dto.setAnswer(answer);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setAnswerType(String answerType) {
		dto.setAnswerType(answerType);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setAnswerVoice(String answerVoice) {
		dto.setAnswerVoice(answerVoice);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setAnswerMood(String answerMood) {
		dto.setAnswerMood(answerMood);
		return this;
	}
	
	public MultiChannelAnswerDtoBuilder setLineMKey(String mKey) {
		dto.setLineMKey(mKey);
		return this;
	}
	
	
	public MultiChannelAnswerDtoBuilder setChannelTypeConfig(String channelTypeConfig) {
		dto.setChannelTypeConfig(channelTypeConfig);
		return this;
	}

	public MultiChannelAnswerDto get() {
		return dto;
	}

}
