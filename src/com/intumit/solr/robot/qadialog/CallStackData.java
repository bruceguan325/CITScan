package com.intumit.solr.robot.qadialog;

import java.io.Serializable;

public class CallStackData implements Serializable {
	QADialog from;
	QADialog to;
	CallDialogRuleCheckResult crcr;
	String question;
	
	public CallStackData(QADialog from, QADialog to, CallDialogRuleCheckResult crcr, String question) {
		super();
		this.from = from;
		this.to = to;
		this.crcr = crcr;
		this.question = question;
	}
	
	
}
