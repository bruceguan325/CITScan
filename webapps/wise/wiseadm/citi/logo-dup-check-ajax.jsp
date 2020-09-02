<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.citi.CitiDeep"
%><%@ page import="com.intumit.solr.admin.*" %><%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

if (user == null || AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) { 
	%>{"needLogin":true}<%
}
else {
	HashSet<String> hs=new HashSet<String>();
    /*
    List<String> citi = CitiDeep.logos(null,null);
	for(String line: citi)
    {
	    String[] split_line = line.split(";");
        for(String s: split_line)
        {
            hs.add(s);
        }
    }
    */
    String dupLogo = "";
	String oldlogo = StringUtils.trimToNull(request.getParameter("oldlogo"));
	String newlogo = StringUtils.trimToNull(request.getParameter("newlogo"));
	String alllogos = StringUtils.trimToNull(request.getParameter("alllogos"));
	String[] split_oldlogo = oldlogo.split(";");
	String[] split_newlogo = newlogo.split(";");
	String[] split_alllogos = alllogos.split(";");
	for(String s: split_alllogos)
    {
        hs.add(s);
    }
	for(String s: split_oldlogo)
    {
        hs.remove(s);
    }
	for(String s: split_newlogo)
    {
        if(hs.contains(s))
        {
            dupLogo = s;
            break;
        }
    }
	
	if (StringUtils.isNotEmpty(dupLogo)) {
		%>{"exists":true, "logo":<%= dupLogo %>}<%
	}
	else {
		%>{"exists":false}<%
	}
}
%>