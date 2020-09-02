package com.intumit.solr;

import java.io.Serializable;

public class NotificationEvent implements Serializable {
	public static enum TargetType {
		TENANT,
		ADMIN_GROUP,
		ADMIN_USER,
	}

	public static enum StackType {
		STACK
	}

	public static enum NotificationType {
		INFO,
		SUCCESS,
		ERROR
	}
	
	String source;
	
	TargetType targetType;
	Object target;
	
	StackType stackType;
	
	NotificationType notificationType;
	String title;
	String content;
	boolean hidden = true;
	
	@Override
	public String toString() {
		return "NotificationEvent [source=" + source + ", targetType="
				+ targetType + ", target=" + target + ", stackType="
				+ stackType + ", notificationType=" + notificationType
				+ ", title=" + title + ", content=" + content + "]";
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public TargetType getTargetType() {
		return targetType;
	}
	public void setTargetType(TargetType targetType) {
		this.targetType = targetType;
	}
	public Object getTarget() {
		return target;
	}
	public void setTarget(Object target) {
		this.target = target;
	}
	public StackType getStackType() {
		return stackType;
	}
	public void setStackType(StackType stackType) {
		this.stackType = stackType;
	}
	public NotificationType getNotificationType() {
		return notificationType;
	}
	public void setNotificationType(NotificationType notificationType) {
		this.notificationType = notificationType;
	}
	public boolean isHidden() {
		return hidden;
	}
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	
}
