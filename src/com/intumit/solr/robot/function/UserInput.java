package com.intumit.solr.robot.function;

public class UserInput {
	String input;
	
	public UserInput(String input) {
		this.input = input;
	}

	public String getInput() {
		return input;
	}

	public void setInput(String input) {
		this.input = input;
	}

	@Override
	public String toString() {
		return input;
	}
	
}
