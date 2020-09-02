package com.intumit.quartz;

import java.io.IOException;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.utils.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.intumit.solr.dataimport.DihLogEntity;
import com.intumit.solr.dataimport.DihLogFacade;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.systemconfig.WiseSystemConfig;

public class DihClient {
	private static final Logger LOG = LoggerFactory.getLogger(DihClient.class);
	
	public static final String FULL_IMPORT = "full-import";
	public static final String DELTA_IMPORT = "delta-import";
	public static final String DELTA_IMPORT_VIA_FULL_IMPORT = "delta-import-via-full-import";

	public static String getResponseBody(String url) throws IOException {

		LOG.info("run url:" + url);
		HttpClient httpClient = new HttpClient();
		// 認證
		// httpClient.getState().setCredentials(AuthScope.ANY,
		// new UsernamePasswordCredentials(account, password));

		GetMethod getMethod = new GetMethod(WiSeUtils.getDataFromUrl(url));
		getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
				new DefaultHttpMethodRetryHandler());

		try {

			int statusCode = httpClient.executeMethod(getMethod);

			if (statusCode != HttpStatus.SC_OK) {
				System.err.println("Method failed: "
						+ getMethod.getStatusLine());
				return "";
			}

			String responseBody = getMethod.getResponseBodyAsString();
			// LOG.info(responseBody);
			return responseBody;

		} catch (Exception e) {
			LOG.error("Error", e);
		} finally {
			getMethod.releaseConnection();
		}

		return "";
	}

	static String status(String core, String port, String context_path, String keyName) {		
		DihLogEntity log = DihLogFacade.getLatestByEventSource(keyName);
		String status = "idle";
		
		if (log != null) {
			String statusMsg = log.getStatusMessage().toLowerCase();
			if (statusMsg.contains("completed")) {
				status = "completed";
			} else if (statusMsg.contains("aborted")) {
				status = "aborted";
			} else if (statusMsg.contains("failed")) {
				status = "failed";
			} else {
				if (statusMsg.contains("time taken")) {
					status = "finished";
				} else {
					status = "running";
				}
			}
			
			status = "<strong>" + status.toUpperCase() + "</strong>";
			status += "<BR>" + StringUtils.trimToEmpty("" + log.getTimestamp());// + log.getStatusMessage();
			status += "<BR>" + StringUtils.trimToEmpty(log.getStatistics());// + log.getStatusMessage();
		}
		
		return status;
	}

	static void doImport(String context_path, String port, String core, String type, String keyName) throws IOException {
		boolean clean = false;
		String command = null;
		String handler = null;
		if (FULL_IMPORT.equals(type)) {
			command = FULL_IMPORT;
			clean = true;
		}
		else if (DELTA_IMPORT_VIA_FULL_IMPORT.equals(type)) { 
			command = FULL_IMPORT;
			clean = false;
		}
		else if (DELTA_IMPORT.equals(type)) {
			command = DELTA_IMPORT;
			clean = false;
		}
		else {
			command = "status";
		}
		
		if (StringUtils.isEmpty(handler)) {
			handler = "dataimport"; // Default Handler
		}
		
		String Hip = WiseSystemConfig.get().getHostname() != null? 
				WiseSystemConfig.get().getHostname().substring(0,WiseSystemConfig.get().getHostname().length()-1)
				: "http://localhost:" + port;
		
		String url = Hip + context_path + "/wiseadm/" + core 
				+ "/" + handler + "?command=" + command
				+ "&schedulerKeyName=" + keyName
				+ "&commit=false&optimize=false&clean=" + clean;
		LOG.info(url);
		getResponseBody(url);
	}

	static void commit(String core, String port, String context_path) throws IOException {
		String Hip = WiseSystemConfig.get().getHostname() != null? 
				WiseSystemConfig.get().getHostname().substring(0,WiseSystemConfig.get().getHostname().length()-1)
				: "http://localhost:" + port;
				
		String url = Hip + context_path + "/wiseadm/sms?command=commit&coreName=" + core;
		LOG.info(url);
		getResponseBody(url);
	}

	public static void doImport(JobDataMap map, Key key) throws IOException {
		doImport(
				map.getString("context_path"),
				map.getString("port"), // port 
				map.getString(IndexJob.JOB_DETAIL_PROPERTIES[0]), // core 
				map.getString(IndexJob.JOB_DETAIL_PROPERTIES[1]), // importType 
				key.getName()
				);

	}

	public static String status(JobDataMap map, Key key) {
		return status(map.getString("core"), 
				map.getString("port"), 
				map.getString("context_path"),
				key.getName());
	}

	public static void commit(JobDataMap map) throws IOException {
		commit(map.getString("core"), map.getString("port"), map
				.getString("context_path"));
	}
}
