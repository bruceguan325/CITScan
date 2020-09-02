package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;

import com.intumit.solr.robot.QAContext;

import flexjson.JSONDeserializer;

public abstract class Trigger implements Serializable {
	
	boolean doubleConfirm = Boolean.FALSE;
	String doubleConfirmText;
	List<String> contents;
	boolean showCurrentStatus = Boolean.FALSE;
	boolean reconstructQuestion = Boolean.TRUE;
	
	public abstract boolean isTrigger(QAContext ctx);
	public Trigger() {};
	public Trigger(boolean doubleConfirm, String doubleConfirmText,
			List<String> contents, boolean showCurrentStatus) {
		super();
		this.doubleConfirm = doubleConfirm;
		this.doubleConfirmText = doubleConfirmText;
		this.contents = contents;
		this.showCurrentStatus = showCurrentStatus;
	}
	
	public boolean isDoubleConfirm() {
		return doubleConfirm;
	}
	public void setDoubleConfirm(boolean doubleConfirm) {
		this.doubleConfirm = doubleConfirm;
	}
	public String getDoubleConfirmText() {
		return doubleConfirmText;
	}
	public void setDoubleConfirmText(String doubleConfirmText) {
		this.doubleConfirmText = doubleConfirmText;
	}
	public boolean isShowCurrentStatus() {
		return showCurrentStatus;
	}
	public void setShowCurrentStatus(boolean showCurrentStatus) {
		this.showCurrentStatus = showCurrentStatus;
	}
	public boolean isReconstructQuestion() {
		return reconstructQuestion;
	}
	public void setReconstructQuestion(boolean reconstructQuestion) {
		this.reconstructQuestion = reconstructQuestion;
	}
	public List<String> getContents() {
		return contents;
	}
	public void setContents(List<String> contents) {
		this.contents = contents;
	}
	public static Trigger createTrigger(JSONObject config) {
		try {
			String type = config.getString("type");
			/*List<String> contents = jsonArrToStringArr(config.optJSONArray("contents"));
			
			boolean dc = config.optBoolean("doubleConfirm", false);
			String dct = config.optString("doubleConfirmText");
			boolean scs = config.optBoolean("showCurrentStatus", false);*/
			
			if ("sentence".equalsIgnoreCase(type)) {
				return (Trigger)new JSONDeserializer().deserialize(config.toString(), RegexTrigger.class);
			}
			else if ("reqAttr".equalsIgnoreCase(type)) {
				return (Trigger)new JSONDeserializer().deserialize(config.toString(), ReqAttrTrigger.class);
			}
			else if ("lastReqAttr".equalsIgnoreCase(type)) {
				return (Trigger)new JSONDeserializer().deserialize(config.toString(), LastReqAttrTrigger.class);
			}
			else if ("expression".equalsIgnoreCase(type)) {
				return (Trigger)new JSONDeserializer().deserialize(config.toString(), ExpressionTrigger.class);
			}
			else if ("chooseOption".equalsIgnoreCase(type)) {
				return (Trigger)new JSONDeserializer().deserialize(config.toString(), ChooseOptionTrigger.class);
			}
			else if ("rule".equalsIgnoreCase(type)) {
				return (Trigger)new JSONDeserializer().deserialize(config.toString(), RuleTrigger.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	static List<String> jsonArrToStringArr(JSONArray arr) {
		List<String> l = new ArrayList<String>();
		
		if (arr != null)
		for (int i=0; i < arr.length(); i++) {
			try {
				l.add(arr.getString(i));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return l;
	}
}
