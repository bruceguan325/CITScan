package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.intumit.solr.robot.qadialog.QAConversationalDialog.DialogNode;
import com.intumit.solr.robot.qadialog.QAConversationalDialog.Perception;
import com.intumit.solr.robot.qadialog.QAConversationalDialog.Reaction;

public class DialogLogEntry implements Serializable {
	String userInput;
	String systemOutput;
	
	DialogNode fromNode;
	DialogNode toNode;
	
	Perception perceptBy;
	List<Reaction> triggeredReactions = new ArrayList<>();
	
	String logDetail = "";
	
	public String getUserInput() {
		return userInput;
	}

	public void setUserInput(String userInput) {
		this.userInput = userInput;
	}

	public DialogNode getFromNode() {
		return fromNode;
	}

	public void setFromNode(DialogNode fromNode) {
		this.fromNode = fromNode;
	}

	public DialogNode getToNode() {
		return toNode;
	}

	public void setToNode(DialogNode toNode) {
		this.toNode = toNode;
	}

	public DialogLogEntry appendNL() {
		logDetail += "\n";
		return this;
	}
	
	public DialogLogEntry appendLog(String text) {
		logDetail += text;
		return this;
	}
	
	public void setPerceptBy(Perception p) {
		perceptBy = p;
	}
	
	public void addTriggeredReaction(Reaction r) {
		triggeredReactions.add(r);
	}

	public String getSystemOutput() {
		return systemOutput;
	}

	public void setSystemOutput(String systemOutput) {
		this.systemOutput = systemOutput;
	}

	public String getLogDetail() {
		return logDetail;
	}

	@Override
	public String toString() {
		return "DialogLogEntry [userInput=" + userInput + ", systemOutput=" + systemOutput + ", fromNode=" + fromNode!=null?fromNode.name:"null"
				+ ", toNode=" + toNode!=null?toNode.name:"null" + ", perceptBy=" + perceptBy + ", triggeredReactions=" + triggeredReactions
				+ ", logDetail=" + logDetail + "]";
	}
	
	
}
