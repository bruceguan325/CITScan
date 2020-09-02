<%@ page language="java" contentType="application/json"  pageEncoding="UTF-8" %>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Locale" %>
<%@ page import="com.intumit.message.MessageUtil" %>
<%@ page import="com.intumit.solr.robot.QAAltBuild"%>
<%@ page import="com.intumit.solr.robot.QAAltBuildQueue"%>
<%@ page import="com.intumit.solr.robot.QAAltBuildQueue.Status"%>
<%@ page import="com.intumit.solr.tenant.Tenant"%>
<%@ page import="org.apache.struts.Globals" %>
<%@ page import="org.apache.wink.json4j.JSONObject"%>

<%
  Tenant t = Tenant.getFromSession(session); 
  List<QAAltBuild> currentQueue = QAAltBuild.listByTenant(t.getId());
  JSONObject result = new JSONObject();
  int cnt = 0;
  for(QAAltBuild build : currentQueue) {
	  Status s = QAAltBuildQueue.getStatus(t.getId(), build.getDocId());
	  if(!Status.DONE.equals(s) && !Status.FAILED.equals(s)) {
		  cnt++;
	  }
  }
  Locale loc = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);
  result.put("pass", cnt == 0);
  if(cnt > 0) {
  	result.put("message", MessageUtil.getMessage(loc, "over.maxBatchUploadSize.current.processNumber", new String[] {"" + cnt}));
  }
%>
<%=result.toString(2)%>