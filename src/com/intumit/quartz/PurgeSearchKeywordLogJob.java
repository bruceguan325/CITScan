package com.intumit.quartz;

import java.util.Calendar;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.thoughtworks.xstream.XStream;

public class PurgeSearchKeywordLogJob extends NonIndexJob {

	private static final Logger LOG = LoggerFactory.getLogger(PurgeSearchKeywordLogJob.class);
	
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
		try {
			JobDataMap triggermap = jobDetail.getJobDataMap();

			int beforeHowManyDays = jobDetail.getJobDataMap().getIntegerFromString(NonIndexJob.JOB_DETAIL_PROPERTIES[0]);
			
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1 * beforeHowManyDays);
			
			SearchKeywordLogFacade.getInstance().deleteLogtimeBefore(cal.getTime());
			scheduleLog.setStatistics("Purge SearchKeywordLog older than " + beforeHowManyDays + " days...Finished");
			
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
