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
import="com.intumit.util.DesUtil"
%><%!
%><%
boolean authorized = false;
JSONObject resp = new JSONObject();
if(AdminUserFacade.getInstance().getFromSession(session) == null) {
	String encodedUser = request.getParameter("code");
	if(StringUtils.isNotBlank(encodedUser)) {
		String[] userInfo = DesUtil.decrypt(encodedUser).split("@@");
		if(userInfo.length == 2 && (System.currentTimeMillis() - 5*60*1000) <= Long.valueOf(userInfo[1])) {
			AdminUser user = AdminUserFacade.getInstance().getByLoginName(userInfo[0]);
			if(user != null)
				AdminUserFacade.getInstance().setSession(session, user);
		}
	}
}
authorized = (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) != 0;
resp.put("authorized", authorized);
%><%= resp.toString(4) %>
