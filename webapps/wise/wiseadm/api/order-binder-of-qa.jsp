<%@page import="com.intumit.solr.SearchManager"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="com.intumit.solr.robot.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.lang.math.RandomUtils"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.tenant.*"
%><%!
%><%
	Tenant t = null;
if (request.getParameter("apikey") != null) {
	String apikey = request.getParameter("apikey");
	com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
	t = k != null ? k.getTenant() : null;//Tenant.getTenantByApiKey(apikey);
}

if (t == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");

	out.println(errorOutput.toString(2));

	return;
}
String qaId = request.getParameter("qaId");
String binderIds = request.getParameter("binderIds");
//boolean authorized = (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) != 0;
boolean authorized = true;
JSONObject resp = new JSONObject();
resp.put("authorized", authorized);
if(authorized){
	if (StringUtils.isNotBlank(binderIds)) {
		String[] binderIdArrayWithOrder = StringUtils.split(binderIds, ",");
		try {
	SolrDocument origDoc = QAUtil.getInstance(t).getMainQASolrDocument(Long.valueOf(qaId), true);
	if (origDoc != null){
		QA qa = new QA(origDoc);
		LinkedHashSet<Long> oldBinderIds = qa.getBinderIds();
		oldBinderIds.clear();
		for(String binderId : binderIdArrayWithOrder){
			System.out.println(binderId);
			if(StringUtils.isNotBlank(binderId)) {
				oldBinderIds.add(Long.valueOf(binderId));
			}
		}
		qa.setBinderIds(oldBinderIds);
		SolrServer server = t.getCoreServer4Write();
		server.add(qa);
		server.commit(true, true, false);
		
		try {
			// wait for softCommit
			Thread.sleep(1000);
		} catch (InterruptedException ignore) {
		} 
	}
		} catch (Exception e) {
	e.printStackTrace();
		}
	}
}
%><%= resp.toString(4) %>
