package com.intumit.solr.dataimport;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.EventListener;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.intumit.hibernate.HibernateUtil;

public class DihEventListener implements EventListener {
	
	@Override
	public void onEvent(Context ctx) {
		System.out.println("**** DihEventListener Begin ****");
		System.out.println(ctx.getRequestParameters());
		System.out.println(ctx.getStats());
		System.out.println(ctx.getStatusMessages());
		System.out.println("**** DihEventListener End ****");
		//System.out.println("****");
		//System.out.println(ctx.getErrorDetails());
		
		DihLogEntity log = new DihLogEntity();
		log.setCoreName(ctx.getSolrCore().getName());
		log.setEventSource(
				ctx.getRequestParameters().containsKey("schedulerKeyName")
				? ctx.getRequestParameters().get("schedulerKeyName").toString()
				: "manual");
		log.setParameters(ctx.getRequestParameters().toString());
		
		
		// Inject dih event type
		HashMap statusMap = new HashMap();
		if (statusMap != null) {
			statusMap.putAll(ctx.getStatusMessages());
		}
		statusMap.put("DihEventListener", this.getClass().getName());
		
		log.setStatistics(ctx.getStats().toString());
		log.setStatusMessage(statusMap.toString());
		log.setErrors(ctx.getErrorDetails().toString());
		log.setTimestamp(new Date());
		
		DihLogFacade.save(log);
	}
}
