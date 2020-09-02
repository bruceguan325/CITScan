package com.intumit.quartz;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intumit.solr.dataimport.DihLogEntity;
import com.intumit.solr.dataimport.DihLogFacade;

public class IndexJob implements Job {

	private static final Logger LOG = LoggerFactory.getLogger(IndexJob.class);
	public static final String[] JOB_DETAIL_PROPERTIES = {"core", "importType", "handler"};

	public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDetail jobDetail = context.getJobDetail();
		java.util.Date now = new java.util.Date();

		LOG.info("Start " + jobDetail.getFullName() + " - " + now);
		try {
			Key key = jobDetail.getKey();
			JobDataMap dataMap = jobDetail.getJobDataMap();

			DihClient.doImport(dataMap, key);
			Thread.sleep(10 * 1000);

			while (true) {
				DihLogEntity log = DihLogFacade.getLatestByEventSource(key.getName());

				if (log == null || log.getTimestamp().compareTo(now) > 0) {
					String status = "";

					if (log != null) {
						String statusMsg = log.getStatusMessage().toLowerCase();
						if (statusMsg.contains("completed")) {
							status = "completed";
						}
						else if (statusMsg.contains("aborted")) {
							status = "aborted";
						}
						else if (statusMsg.contains("failed")) {
							status = "failed";
						}
						else {
							if (statusMsg.contains("time taken")) {
								status = "finished";
							}
							else {
								status = "running";
							}
						}
					}
					if ("completed".equals(status) || "finished".equals(status)) {
						DihLogEntity commitLog = new DihLogEntity();
						java.util.Date ts = new java.util.Date();
						commitLog.setEventSource(log.getEventSource());
						commitLog.setCoreName(log.getCoreName());
						commitLog.setParameters("");
						commitLog.setStatusMessage("Completed, doing commit.");
						commitLog.setTimestamp(ts);
						DihLogFacade.save(commitLog);
						DihClient.commit(dataMap);
						break;
					}
					else {
						Thread.sleep(10 * 1000);
					}
				}
				else {
					Thread.sleep(10 * 1000);
				}
			}
			LOG.info("End  " + jobDetail.getFullName() + " - " + new java.util.Date());
		}
		catch (Exception e) {
			LOG.error("Error", e);
			System.err.println(e);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			// StdSchedulerFactory.getDefaultScheduler().start();
			String url = "http://localhost:8080/wise/wiseadm/core0/dataimport?command=delta-import&commit=false&optimize=false";
			// getResponseBody(url, "root", "intumitdemo");
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("end");
	}

}
