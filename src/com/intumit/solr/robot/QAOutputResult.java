package com.intumit.solr.robot;

public class QAOutputResult {
	
	String output;
	boolean hasResult;
	boolean autoCompleted;
	
	public QAOutputResult() {
		super();
	}
	
	public String getOutput() {
		return output;
	}
	
	public void setOutput(String output) {
		this.output = output;
	}
	
	public boolean isHasResult() {
		return hasResult;
	}
	
	public void setHasResult(boolean hasResult) {
		this.hasResult = hasResult;
	}
	
	public boolean isAutoCompleted() {
		return autoCompleted;
	}
	
	public void setAutoCompleted(boolean autoCompleted) {
		this.autoCompleted = autoCompleted;
	}
}
