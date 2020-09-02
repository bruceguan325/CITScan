package com.intumit.quartz;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intumit.solr.SearchManager;
import com.intumit.solr.robot.QAAltBuildQueue;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.tenant.Tenant;
import com.thoughtworks.xstream.XStream;

/**
 * 
 * NonIndexJob.JOB_DETAIL_PROPERTIES[0] ==> Additional Filter Query
 * 
 * @author Yao
 */
public class BuildQAAltJob extends NonIndexJob {

	private static final Logger LOG = LoggerFactory.getLogger(BuildQAAltJob.class);
	
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
			Integer tenantId = jobDetail.getJobDataMap().getIntegerFromString(NonIndexJob.JOB_DETAIL_PROPERTIES[0]);
			Tenant t = Tenant.get(tenantId);
			SolrServer server = t.getCoreServer();
			SolrQuery sqry = new SolrQuery();
			sqry.setQuery("*:*");
			sqry.setRows(100);
			sqry.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
			sqry.addFilterQuery("-isPart_i:[2 TO *]");
			
			String additionalFilterQuery = StringUtils.trimToNull(jobDetail.getJobDataMap().getString(NonIndexJob.JOB_DETAIL_PROPERTIES[1]));
			if (additionalFilterQuery != null) {
				sqry.addFilterQuery(additionalFilterQuery);
			}
			
			sqry.setSort("kid_l", SolrQuery.ORDER.asc);
			
			int cursor = 0;
			do {
				sqry.setStart(cursor);
				QueryResponse response = server.query(sqry);
				SolrDocumentList docList = response.getResults();
				for (SolrDocument sdoc: docList) {
					cursor++;
					String id = (String)sdoc.getFieldValue("id");
					String question = (String)sdoc.getFieldValue("QUESTION_s");
					Long kid = (Long)sdoc.getFieldValue("kid_l");
					@SuppressWarnings("unchecked")
					List<String> qaTPL = (List<String>)sdoc.getFieldValue("QUESTION_ALT_TPL_ms");
					QAAltBuildQueue.add(t.getId(), id, kid, qaTPL, "admin");
				}
				if (docList == null || cursor >= docList.getNumFound() || docList.size() == 0) {
						break;
				}
			} while (true);
		    
			
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
