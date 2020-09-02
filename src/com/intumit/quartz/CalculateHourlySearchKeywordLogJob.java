package com.intumit.quartz;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.thoughtworks.xstream.XStream;

public class CalculateHourlySearchKeywordLogJob extends NonIndexJob {

	private static final Logger LOG = LoggerFactory.getLogger(CalculateHourlySearchKeywordLogJob.class);
	
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
			
			int period = jobDetail.getJobDataMap().getIntegerFromString(NonIndexJob.JOB_DETAIL_PROPERTIES[0]);
			
			SearchKeywordLogFacade.getInstance().calculateStatisicHourly("2013-01-01 00:00:00", "2013-12-31 23:59:59");
			
			scheduleLog.setStatistics("Finished"); // TODO
			
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
