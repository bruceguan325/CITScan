package com.intumit.solr.robot;

public enum AuditStatus {
	PUBLISH(0), AUDIT(1), REJECT(2), HISTORY(3);
	
	private int value;
	private AuditStatus(int value) {
        this.value = value;
    }
	public int getValue() {
        return value;
    }
}
