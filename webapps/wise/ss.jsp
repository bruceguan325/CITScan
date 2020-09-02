<%@ include file="/commons/taglib.jsp"%><%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="java.util.List"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.hithot.HitHotLocale"
	import="com.intumit.message.MessageUtil"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.util.WiSeUtils"
	import="com.intumit.systemconfig.*"
	import="org.apache.struts.Globals"
%><%

String r = request.getParameter("uk");
Long ucId = new Long(StringUtils.defaultString(request.getParameter("u"), "0"));
UserClue uc = null;
UrlShortener us = UrlShortener.getByMkey(r);
String goodUrl = null;

if (ucId != 0L) {
	uc = UserClue.get(ucId);
}

if (us != null) {
	goodUrl = us.getFullUrl();
	
	System.out.println("Got ShortenUrl ["+ r + "]: " + goodUrl);
	if (StringUtils.contains(goodUrl, "{{USERID}}")) {
		goodUrl = StringUtils.replace(goodUrl, "{{USERID}}", "" + ucId);
		System.out.println("Replace user id in shortendUrl[" + goodUrl + "]");
	}
	
	if (uc != null) {
		UserClueTag.addTagToUser(uc, us.getTag(), us.getAdditionalInfo());
	}
}

if (goodUrl != null) {
//	response.sendRedirect(goodUrl);
}
%>
<script>
document.location='<%= StringUtils.defaultString(goodUrl, "#") %>'
</script>