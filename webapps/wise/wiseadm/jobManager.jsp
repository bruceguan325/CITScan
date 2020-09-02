<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@page import="com.intumit.quartz.Job"%>
<%@page import="com.intumit.quartz.ScheduleUtils"%>
<%@ page import="java.io.*" %>

<%!
	private static final String FACET_RESULTS_JSP = "coresAdmin.jsp";


%>
<% 
String cmd=request.getParameter("cmd");
String webappsPath = request.getContextPath();
if (webappsPath.equals("/")) 
	webappsPath = "/ROOT";

Job job =new Job();
job.setUrl(request.getParameter("url") );
job.setCron(request.getParameter("cron") );

ScheduleUtils s = new ScheduleUtils();
if("add".equals(cmd)){
	job.setName(request.getParameter("name") +"_"+request.getParameter("core"));
s.addJobSchedule(new File("../tomcat/webapps" + webappsPath + "/WEB-INF/jobSchedule.xml").getCanonicalPath(),job);
}else if("delete".equals(cmd)){
	job.setName(request.getParameter("name"));
s.deleteJobSchedule(new File("../tomcat/webapps" + webappsPath + "/WEB-INF/jobSchedule.xml").getCanonicalPath(),job);	
}else if("update".equals(cmd)){
	job.setName(request.getParameter("name"));
	s.deleteJobSchedule(new File("../tomcat/webapps" + webappsPath + "/WEB-INF/jobSchedule.xml").getCanonicalPath(),job);	
	s.addJobSchedule(new File("../tomcat/webapps" + webappsPath + "/WEB-INF/jobSchedule.xml").getCanonicalPath(),job);
}



request.getRequestDispatcher(FACET_RESULTS_JSP + "?t=schedule&coreName=" + request.getParameter("core")).forward(request,response);
%>
