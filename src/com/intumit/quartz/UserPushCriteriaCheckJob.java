package com.intumit.quartz;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.UserClue;
import com.intumit.solr.robot.push.PushData;
import com.intumit.solr.robot.push.PushService;
import com.intumit.solr.robot.push.SingleUserPushTrigger;
import com.intumit.solr.robot.push.UserPushTrigger;
import com.intumit.solr.robot.push.UserPushTrigger.CriteriaType;
import com.intumit.solr.tenant.Tenant;
import com.thoughtworks.xstream.XStream;

/**
 * 
 * NonIndexJob.JOB_DETAIL_PROPERTIES[0] ==> Tenant ID(s)
 * 
 * @author Herb
 */
public class UserPushCriteriaCheckJob extends NonIndexJob {

	private static final Logger LOG = LoggerFactory.getLogger(UserPushCriteriaCheckJob.class);
	
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

		Date now = new java.util.Date();
		LOG.info("Start " + trigger.getFullName() + " - " + now);
		try {
			String tids = jobDetail.getJobDataMap().getString(NonIndexJob.JOB_DETAIL_PROPERTIES[0]);
			String namespace = StringUtils.trimToNull(jobDetail.getJobDataMap().getString(NonIndexJob.JOB_DETAIL_PROPERTIES[1]));

			String userMsg = "";
			
			for (String tidStr: StringUtils.split(tids, ",")) {
				Tenant tenant = Tenant.get(Integer.parseInt(tidStr));
				List<SingleUserPushTrigger> triggers = SingleUserPushTrigger.getByStatus(tenant.getId(), namespace, UserPushTrigger.Status.inqueue);
				for (SingleUserPushTrigger t: triggers) {
					switch (t.getCriteriaType()) {
						case datetime:
							Date deadline = UserPushTrigger.dateTimeStringToDate(t.getPushCriteria());
							if (now.after(deadline)) {
								userMsg = "A datetime criteria triggerred [" + t.toString() + "]";
								PushData pushData = PushData.get(t.getPushDataId());
								UserClue u = UserClue.get(t.getUserClueId());
								
								if (u.getTenantId() != tenant.getId()) {
									System.out.println("UserClue[" + u.getId() + "]'s tenantId not match pushData[" + pushData.getId() + "]'s tenant id.");
									continue;
								}
								new PushService(tenant).syncPush(pushData, u);
								
								t.incTriggeredTimes();
								
								if (checkStopCriteria(t)) {
									t.setPushStatus(UserPushTrigger.Status.done);
								}
								
								SingleUserPushTrigger.saveOrUpdate(t);
							}
							break;
						case sql:
							// #TODO: 還沒實作
							break;
						case script:
							// #TODO: 還沒實作
							break;
						default:
							break;
					}
					
					System.out.println(userMsg);
				}
			}
			

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

	private boolean checkStopCriteria(SingleUserPushTrigger t) {
		boolean needStop = false;
		
		switch (t.getStopCriteriaType()) {
			case maxTimes:
				int limit = Integer.parseInt(t.getStopCriteria());
				if (t.getTriggeredTimes() >= limit) {
					needStop = true;
				}
				break;
			case datetime:
				Date deadline = UserPushTrigger.dateTimeStringToDate(t.getStopCriteria());
				if (new Date().after(deadline)) {
					needStop = true;
				}
			default:
		}
		
		return needStop;
	}
	
	
}
