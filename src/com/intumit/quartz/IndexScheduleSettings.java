package com.intumit.quartz;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.UUID;
import org.json.JSONObject;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import flexjson.JSONSerializer;

public class IndexScheduleSettings extends AbstractScheduleSettings implements Serializable {
	IndexScheduleSettings(String expression) throws ParseException {
		super();
		setTrigger(new CronTrigger());
		setExpression(expression);

	}

	public IndexScheduleSettings(JobDetail jobDetail, CronTrigger trigger) throws ParseException {
		super();
		setTrigger(trigger);
		setJobDetail(jobDetail);
		setExpression(trigger.getCronExpression());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.intumit.quartz.ScheduleSettings#getExecuteStatus()
	 */
	@Override
	public String getExecuteStatus() throws SchedulerException {
		return DihClient.status(jobDetail.getJobDataMap(), jobDetail.getKey());
	}
}
