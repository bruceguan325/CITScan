package com.intumit.solr.robot;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * A object for converting json string to java object.
 * 
 * @author herb
 */
public class LineConfig implements Serializable {

	String robotChannelCode;
	String channelId;
	String channelSecret;
	String accessToken;
	String version;

	public LineConfig(String robotChannelCode, String jsonStr) {
		super();
		this.robotChannelCode = robotChannelCode;
		
		try {
			JSONObject jo = new JSONObject(jsonStr);
			this.channelId = jo.optString("channelId");
			this.channelSecret = jo.optString("channelSecret");
			this.accessToken = jo.optString("accessToken");
			this.version = jo.optString("version");
		}
		catch (JSONException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot parse json [" + jsonStr + "]");
		}
	}
	
	public LineConfig(String robotChannelCode, String channelId, String channelSecret, String accessToken, String version) {
		super();
		this.robotChannelCode = robotChannelCode;
		this.channelId = channelId;
		this.channelSecret = channelSecret;
		this.accessToken = accessToken;
		this.version = version;
	}
	
	public String getRobotChannelCode() {
		return robotChannelCode;
	}
	public String getChannelId() {
		return channelId;
	}
	public String getChannelSecret() {
		return channelSecret;
	}
	public String getAccessToken() {
		return accessToken;
	}
	public String getVersion() {
		return version;
	}
}
