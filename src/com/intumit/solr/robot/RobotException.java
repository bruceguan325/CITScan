package com.intumit.solr.robot;

import org.apache.wink.json4j.JSONObject;

public class RobotException extends RuntimeException {
	JSONObject error;

	public RobotException(JSONObject error) {
		super();
		this.error = error;
	}

	public JSONObject getError() {
		return error;
	}
}
