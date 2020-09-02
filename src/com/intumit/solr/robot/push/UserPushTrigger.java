package com.intumit.solr.robot.push;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Index;

@MappedSuperclass
public abstract class UserPushTrigger {

	public static final String DATETIME_FORMAT = "yyyy/MM/dd HH:mm:ss";

	public enum Status { inqueue, suspended, done, }
	public enum CriteriaType { 
		datetime, 		// A unix time string，must *1000 to be new from Date object (ex. new Date(new Long(pushCritegra)*1000 )
		sql, 			// A SQL Statement，can have {{XXX}} to be replace by FunctionUtil，if result count > 0, trigger = true, otherwise not trigger。
		script, 			// Groovy script，must return true or false (Trigger or not)
	}
	public enum StopCriteriaType { 
		datetime, 		// A unix time string，must *1000 to be new from Date object (ex. new Date(new Long(pushCritegra)*1000 )
		maxTimes 		// int
	}

	
	@Index(name = "tenantId")
	Integer tenantId;
	
	@Index(name = "pushDataId")
	Integer pushDataId;
	
	@Enumerated(EnumType.STRING)
	@Index(name = "pushStatus") 
	Status pushStatus;

	@Enumerated(EnumType.STRING)
	@Index(name = "criteriaType") 
	CriteriaType criteriaType;
	@Lob
	String pushCriteria;
	
	@Enumerated(EnumType.STRING)
	@Index(name = "stopCriteriaType") 
	StopCriteriaType stopCriteriaType;
	@Lob
	String stopCriteria;
	
	Integer triggeredTimes;
	
	@Index(name = "createdTime")
	Date createdTime;
	
	@Index(name = "updateTime")
	Date updateTime;
	
	@Column(length = 32)
	@Index(name = "namespace")
	String namespace;

	public UserPushTrigger() {
		super();
	}

	public Integer getTenantId() {
		return tenantId;
	}

	public void setTenantId(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getPushDataId() {
		return pushDataId;
	}

	public void setPushDataId(Integer pushDataId) {
		this.pushDataId = pushDataId;
	}
	
	public Status getPushStatus() {
		return pushStatus;
	}

	public void setPushStatus(Status pushStatus) {
		this.pushStatus = pushStatus;
	}

	public CriteriaType getCriteriaType() {
		return criteriaType;
	}

	public void setCriteriaType(CriteriaType criteriaType) {
		this.criteriaType = criteriaType;
	}

	public StopCriteriaType getStopCriteriaType() {
		return stopCriteriaType;
	}

	public void setStopCriteriaType(StopCriteriaType stopCriteriaType) {
		this.stopCriteriaType = stopCriteriaType;
	}

	public Integer getTriggeredTimes() {
		return triggeredTimes != null ? triggeredTimes : 0;
	}

	public void setTriggeredTimes(Integer triggeredTimes) {
		this.triggeredTimes = triggeredTimes;
	}

	public void incTriggeredTimes() {
		this.triggeredTimes = getTriggeredTimes() + 1;
	}
	
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String groupKey) {
		this.namespace = groupKey;
	}

	public static Date dateTimeStringToDate(String dtStr) {
		Date dt = null;

		try {
			dt = new Date(new Long(dtStr) * 1000);
		}
		catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		return dt;
	}

	public Date getCreatedTime() {
		return createdTime;
	}

	public void setCreatedTime(Date createdTime) {
		this.createdTime = createdTime;
	}

	public Date getUpdateTime() {
		return updateTime;
	}
	
	public String getFormattedUpdateTime() {
		return new SimpleDateFormat(DATETIME_FORMAT).format(updateTime);
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getPushCriteria() {
		return pushCriteria;
	}

	public void setPushCriteria(String pushCriteria) {
		this.pushCriteria = pushCriteria;
	}

	public String getStopCriteria() {
		return stopCriteria;
	}

	public void setStopCriteria(String stopCriteria) {
		this.stopCriteria = stopCriteria;
	}

	@Override
	public String toString() {
		return "UserPushTrigger [" + (tenantId != null ? "tenantId=" + tenantId + ", " : "") + (pushDataId != null ? "pushDataId=" + pushDataId + ", " : "")
				+ (pushStatus != null ? "pushStatus=" + pushStatus + ", " : "") + (criteriaType != null ? "criteriaType=" + criteriaType + ", " : "")
				+ (pushCriteria != null ? "pushCriteria=" + pushCriteria + ", " : "")
				+ (stopCriteriaType != null ? "stopCriteriaType=" + stopCriteriaType + ", " : "")
				+ (stopCriteria != null ? "stopCriteria=" + stopCriteria + ", " : "")
				+ (triggeredTimes != null ? "triggeredTimes=" + triggeredTimes + ", " : "") + (createdTime != null ? "createdTime=" + createdTime + ", " : "")
				+ (updateTime != null ? "updateTime=" + updateTime + ", " : "") + (namespace != null ? "namespace=" + namespace : "") + "]";
	}
	
}
