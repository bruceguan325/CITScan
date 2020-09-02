package com.intumit.quartz;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.beanutils.BeanUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.intumit.message.MessageUtil;

import flexjson.JSON;

public abstract class AbstractScheduleSettings implements ScheduleSettings, Serializable {

	private static final SimpleDateFormat sdf = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");

	public abstract String getExecuteStatus() throws SchedulerException;

	private static final SimpleDateFormat daydf = new SimpleDateFormat(
				"yyyy-MM-dd");
	private transient CronTrigger trigger;
	protected transient JobDetail jobDetail;
	private String expression;
	private String seconds = "";

	public String getExpression() {
		return expression;
	}

	public void setExpression(String expression) throws ParseException {
		this.expression = expression;
		String summary = trigger.getExpressionSummary();
		System.out.println(summary);
		System.out.println(trigger);
		System.out.println(getPreviousFireTime());
		System.out.println(getNextFireTime());
	
		for (String p : summary.split("\n")) {
			String[] pkv = p.split(":");
			try {
				BeanUtils.setProperty(this, pkv[0], pkv[1].trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
	
		for (String p : TOP_LEVEL_JOB_DETAIL_PROPERTIES) {
			try {
				String s = trigger.getJobDataMap().getString(p);
	
				System.out.println(p + ":" + s);
				BeanUtils.setProperty(this, p, s.trim());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
	}

	Scheduler scheduler() {
		try {
			return StdSchedulerFactory.getDefaultScheduler();
		} catch (SchedulerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	
	@JSON(include = false)
	public JobDetail getJobDetail() {
		return jobDetail;
	}

	
	public void setJobDetail(JobDetail jobDetail) {
		this.jobDetail = jobDetail;
	}

	
	public String getId() {
		return jobDetail.getName();
	}

	
	public void setId(String id) {
		// this.id = id;
	}

	
	@JSON(include = false)
	public CronTrigger getTrigger() {
		return trigger;
	}

	
	public void setTrigger(CronTrigger trigger) {
		this.trigger = trigger;
	
	}

	private String minutes = "";
	private String hours = "";
	private String daysOfMonth = "";
	private String months = "";
	private String daysOfWeek = "";
	private String years = "";
	private String lastExDate = "";
	private JobDataMap jobDataMap = null; 

	public AbstractScheduleSettings() {
		super();
	}

	
	public void setJobDataMap(JobDataMap dataMap) {
		// DO NOTHING
	}

	
	public JobDataMap getJobDataMap() {
		return jobDetail.getJobDataMap();
	}

	
	public String getDescription() {
		return jobDetail.getDescription();
	}

	
	public void setDescription(String description) {
		jobDetail.setDescription(description);
	}

	
	public String getName() {
		return jobDetail.getName();
	}

	
	public void setName(String name) {
		jobDetail.setName(name);
	}

	
	public String getType() {
		return jobDetail.getJobDataMap().getString("type");
	}

	
	public void setType(String type) {
		// No use, immutable
	}

	
	public String getSeconds() {
		return seconds;
	}

	
	public void setSeconds(String seconds) {
		this.seconds = seconds;
	}

	
	public String getHours() {
		return hours;
	}

	
	public void setHours(String hours) {
		this.hours = hours;
	}

	
	public String getDaysOfMonth() {
		return daysOfMonth;
	}

	
	public void setDaysOfMonth(String daysOfMonth) {
		this.daysOfMonth = daysOfMonth;
	}

	
	public String getMonths() {
		return months;
	}

	
	public void setMonths(String months) {
		this.months = months;
	}

	
	public String getDaysOfWeek() {
		return daysOfWeek;
	}

	
	public void setDaysOfWeek(String daysOfWeek) {
		this.daysOfWeek = daysOfWeek;
	}

	
	public String getYears() {
		return years;
	}

	
	public void setYears(String years) {
		this.years = years;
	}

	
	public String getLastExDate() {
		return lastExDate;
	}

	
	public void setLastExDate(String lastExDate) {
		this.lastExDate = lastExDate;
	}

	
	public void setMinutes(String minutes) {
		this.minutes = minutes;
	}

	
	public String getMinutes() {
		return minutes;
	}

	
	public String getEndTime() {
		return format(trigger.getEndTime());
	}

	public String format(Date d) {
		if (d == null)
			return "";
		return sdf.format(d);
	}

	
	public String getScheduleStatus() throws SchedulerException {
		int status = scheduler().getTriggerState(jobDetail.getName(),
				jobDetail.getGroup());
		if (status == Trigger.STATE_NORMAL)
			return MessageUtil.getMessage(Locale.getDefault(), "scheduling.process");
		else if (status == Trigger.STATE_ERROR)
			return MessageUtil.getMessage(Locale.getDefault(), "global.exception");
		else if (status == Trigger.STATE_PAUSED)
			return MessageUtil.getMessage(Locale.getDefault(), "global.pause");
		else if (status == Trigger.STATE_COMPLETE)
			return MessageUtil.getMessage(Locale.getDefault(), "global.end");
		return "?";
	}

	
	public String getFinalFireTime() {
		return format(trigger.getFinalFireTime());
	}

	
	public String getNextFireTime() {
		return format(trigger.getNextFireTime());
	}

	
	public String getPreviousFireTime() {
		return format(trigger.getPreviousFireTime());
	}

	
	public String getStartTime() {
		return format(trigger.getStartTime());
	}

	
	public void setEndTime(String endTime) {
		trigger.setEndTime(parse(endTime));
	}

	private Date parse(String s) {
		if (s == null || "".equals(s))
			return null;
		try {
			return sdf.parse(s);
		} catch (ParseException e) {
			try {
				return daydf.parse(s);
			} catch (ParseException e2) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public void setNextFireTime(String nextFireTime) {
		trigger.setNextFireTime(parse(nextFireTime));
	}

	public void setPreviousFireTime(String previousFireTime) {
		trigger.setPreviousFireTime(parse(previousFireTime));
	}
	
	public void setStartTime(String startTime) {
		trigger.setStartTime(parse(startTime));
	}

}
