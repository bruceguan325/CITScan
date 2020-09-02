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

@SuppressWarnings("serial")
public class CommonScheduleSettings extends AbstractScheduleSettings implements Serializable {
	CommonScheduleSettings(String expression) throws ParseException {
		super();
		setTrigger(new CronTrigger());
		setExpression(expression);

	}

	public CommonScheduleSettings(JobDetail jobDetail, CronTrigger trigger) throws ParseException {
		super();
		setTrigger(trigger);
		setJobDetail(jobDetail);
		setExpression(trigger.getCronExpression());
	}

	/* (non-Javadoc)
	 * @see com.intumit.quartz.ScheduleSettings#getExecuteStatus()
	 */
	@Override
	public String getExecuteStatus() throws SchedulerException {
		ScheduleLogEntity log = ScheduleLogFacade.getLatestByEventSource(jobDetail.getName());
		String status = "IDLE";
		
		if (log != null) {
			String statusMsg = log.getStatusMessage();
			status = statusMsg.replaceAll(" - ", "<BR>");
		}
		
		return status;
	}
}
