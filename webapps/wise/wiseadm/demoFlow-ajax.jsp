<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="flexjson.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="org.apache.lucene.index.*"
import="org.apache.solr.core.*"
import="org.apache.solr.servlet.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.embedded.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.solr.common.cloud.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.robot.qadialog.*"
import="com.intumit.solr.util.*"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if (user == null || AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) { 
	%>{"needLogin":true}<%
	return;
}

%><%
try {
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	
	if (t != null) {
		DemoFlow df = DemoFlow.getInstance(t.getId());
		
		if (df != null) {
			String action = request.getParameter("action");
			
			if (action.equals("setEnable")) {
				Boolean enable = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("enable"), "false"));
				df.setEnable(enable);
			}
			if (action.equals("setOverwriteMode")) {
				Boolean enable = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("enable"), "false"));
				df.setOverwriteMode(enable);
			}
			if (action.equals("setClientAutoSendOutMode")) {
				Boolean enable = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("enable"), "false"));
				df.setClientAutoSendOutMode(enable);
			}
			if (action.equals("setAutopilotMode")) {
				Boolean enable = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("enable"), "false"));
				df.setAutopilotMode(enable);
			}
			if (action.equals("setSelfRepair")) {
				Boolean enable = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("enable"), "false"));
				df.setSelfRepair(enable);
			}
			if (action.equals("jumpTo")) {
				int offset = Integer.parseInt(request.getParameter("offset"));
				df.setCurrentOffset(offset);
				df.setCurrentTimestamp(new Date().getTime());
			}
			if (action.equals("setQuestions")) {
				String[] questions = request.getParameterValues("questions[]");
				df.setQuestions(Arrays.asList(questions));
				df.setCurrentOffset(-1);
				df.setCurrentTimestamp(new Date().getTime());
			}
			
			JSONSerializer js = new JSONSerializer();
			out.println(js.deepSerialize(df));
		}
	}
}
catch (Exception e) {
	// Ignore DemoFlow Exceptions
}
%>