package com.intumit.solr.robot.connector.line;

/**
 * 只是列出哪些action type需要實作重組的功能
 * 
 * @author Rock
 */
public enum NaverLineActionType {
	postback("data"),
	uri("uri"),
	message("text"),
	datetimepicker("data"),
	camera(""),
	cameraRoll(""),
	location(""),
	call("call"),
	;
	
	String content;
	
	private NaverLineActionType(String content) {
		this.content = content;
	}
	
	public String getContent() {
		return content;
	}
}
