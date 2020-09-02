package com.intumit.quartz;

import java.text.ParseException;

import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import flexjson.JSON;

public interface ScheduleSettings {

	public static final String[] TOP_LEVEL_JOB_DETAIL_PROPERTIES = { "type" };

	public abstract String getExpression();

	public abstract void setExpression(String expression) throws ParseException;

	@JSON(include = false)
	public abstract JobDetail getJobDetail();

	public abstract void setJobDetail(JobDetail jobDetail);

	public abstract String getId();

	public abstract void setId(String id);

	@JSON(include = false)
	public abstract CronTrigger getTrigger();

	public abstract void setTrigger(CronTrigger trigger);

	// private String scheduleStatus = "";
	// private String executeStatus = "待命中";
	public abstract void setJobDataMap(JobDataMap dataMap);

	public abstract JobDataMap getJobDataMap();

	public abstract String getDescription();

	public abstract void setDescription(String description);

	public abstract String getName();

	public abstract void setName(String name);

	public abstract String getType();

	public abstract void setType(String type);

	public abstract String getSeconds();

	public abstract void setSeconds(String seconds);

	public abstract String getHours();

	public abstract void setHours(String hours);

	public abstract String getDaysOfMonth();

	public abstract void setDaysOfMonth(String daysOfMonth);

	public abstract String getMonths();

	public abstract void setMonths(String months);

	public abstract String getDaysOfWeek();

	public abstract void setDaysOfWeek(String daysOfWeek);

	public abstract String getYears();

	public abstract void setYears(String years);

	public abstract String getLastExDate();

	public abstract void setLastExDate(String lastExDate);

	public abstract void setMinutes(String minutes);

	public abstract String getMinutes();

	public abstract String getEndTime();

	public abstract String getExecuteStatus() throws SchedulerException;

	public abstract String getScheduleStatus() throws SchedulerException;

	public abstract String getFinalFireTime();

	public abstract String getNextFireTime();

	public abstract String getPreviousFireTime();

	public abstract String getStartTime();

	public abstract void setEndTime(String endTime);

	public abstract void setNextFireTime(String nextFireTime);

	public abstract void setPreviousFireTime(String previousFireTime);

	public abstract void setStartTime(String startTime);

}
