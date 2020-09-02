package com.intumit.quartz;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.thoughtworks.xstream.XStream;

public class NonIndexSampleJob extends NonIndexJob {

	private static final Logger LOG = LoggerFactory.getLogger(NonIndexSampleJob.class);

	String toXML(Object rsp) {
		XStream outXML = new XStream();
		outXML.autodetectAnnotations(true);
		String out = outXML.toXML(rsp);
		return out;
	}
	
	public void execute(JobExecutionContext context)
			throws JobExecutionException {
		JobDetail jobDetail = context.getJobDetail();
		
		ScheduleLogEntity scheduleLog = new ScheduleLogEntity();
		scheduleLog.setEventSource(jobDetail.getName());
		scheduleLog.setParameters(jobDetail.getJobDataMap().toString());
		Trigger trigger = context.getTrigger();

		LOG.info("Start " + trigger.getFullName() + " - " + new java.util.Date());
		scheduleLog.setStatusMessage("Start @" + new java.util.Date() + " - " + trigger.getFullName());
		ScheduleLogFacade.save(scheduleLog);
		try {
			JobDataMap triggermap = jobDetail.getJobDataMap();
			
			
			// TODO
			// do something here
			
			scheduleLog.setStatistics(""); // TODO
			
			String statusMsg = "End  " + trigger.getFullName() + " - " + new java.util.Date();
			scheduleLog.setStatusMessage(statusMsg);
			
			ScheduleLogFacade.save(scheduleLog);
			LOG.info(statusMsg);
		} catch (Exception e) {
			LOG.error("Error", e);
			String statusMsg = "Exception End  " + trigger.getFullName() + " - " + new java.util.Date();

			LOG.error(statusMsg, e);
			scheduleLog.setStatusMessage(statusMsg);
			scheduleLog.setErrors(e.getMessage());
			
			ScheduleLogFacade.save(scheduleLog);
		}
	}
}
