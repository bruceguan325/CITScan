package com.intumit.solr.robot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemoFlow {
	public static Map<Integer, DemoFlow> instance = new HashMap<Integer, DemoFlow>();

	boolean enable = false;
	boolean overwriteMode = false; // 限制在流程內，任何 input 都會直接轉成下一個步驟，類似 cheat mode
	boolean clientAutoSendOutMode = false; // 任何選定的問題會直接強制 test.jsp 送出（這個跟 overwriteMode 互斥，不能同時開啟）
	
	boolean autopilotMode = false; // 是否自動跳到下一題
	boolean selfRepair = false; 	/* 是否自我修復（當 overwriteMode = false才有用），當流程中間有其他手動插入的問句時，系統是否要自動根據後續的問句自動比對而趕上進度 */
							
	int currentOffset = -1;
	long currentTimestamp = -1;
	
	String lastQuestion = null;
	String lastAnswer = null;
	
	List<String> questions = null;
	

	public boolean isOverwriteMode() {
		return overwriteMode;
	}

	public void setOverwriteMode(boolean strictMode) {
		this.overwriteMode = strictMode;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public boolean isClientAutoSendOutMode() {
		return clientAutoSendOutMode;
	}

	public void setClientAutoSendOutMode(boolean clientAutoSendOutMode) {
		this.clientAutoSendOutMode = clientAutoSendOutMode;
	}

	public boolean isAutopilotMode() {
		return autopilotMode;
	}

	public void setAutopilotMode(boolean autopilotMode) {
		this.autopilotMode = autopilotMode;
	}

	public boolean isSelfRepair() {
		return selfRepair;
	}

	public void setSelfRepair(boolean selfRepair) {
		this.selfRepair = selfRepair;
	}

	public int getCurrentOffset() {
		return currentOffset;
	}

	public void setCurrentOffset(int currentOffset) {
		this.currentOffset = currentOffset;
	}

	public long getCurrentTimestamp() {
		return currentTimestamp;
	}

	public void setCurrentTimestamp(long currentTimestamp) {
		this.currentTimestamp = currentTimestamp;
	}

	public String getLastQuestion() {
		return lastQuestion;
	}

	public void setLastQuestion(String lastQuestion) {
		this.lastQuestion = lastQuestion;
	}

	public String getLastAnswer() {
		return lastAnswer;
	}

	public void setLastAnswer(String lastAnswer) {
		this.lastAnswer = lastAnswer;
	}

	public List<String> getQuestions() {
		return questions;
	}

	public void setQuestions(List<String> questions) {
		this.questions = questions;
	}
	

	public static DemoFlow getInstance(Integer tenantId) {
		if (!instance.containsKey(tenantId)) {
			synchronized (DemoFlow.class) {
				if (!instance.containsKey(tenantId)) {
					DemoFlow df = new DemoFlow();
					instance.put(tenantId, df);
				}
			}
		}
		
		return instance.get(tenantId);
	}
}
