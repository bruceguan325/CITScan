<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.text.*"
import="java.util.*"
import="com.intumit.message.*"
import="com.intumit.solr.robot.*"
import="org.apache.commons.lang.StringUtils"
import="org.json.*"
import="org.jsoup.Jsoup"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String start = request.getParameter("e_start");
System.out.println(start);
String end = request.getParameter("e_end");
System.out.println(end);
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
String url = StringUtils.trimToEmpty(t.getCrmApiBaseUrl()) + "index.php/site_admin/chat/report?timefrom=" + sdf.format(sdf.parse(start)) + "&timeto=" + sdf.format(sdf.parse(end)) + "&hourfrom=00&minsfrom=00&hourto=23&minsto=59&msgShow=true&style_report=chat_messages&doSearch=產生報表";
%>
<body>
<p url=<%=url %> id="url"></p>
</body>
<script>
window.onload = redirect();
function redirect(){
	console.log("On Load");
	console.log(document.getElementById("url").getAttribute("url"));
	location.href = document.getElementById("url").getAttribute("url");
	//setTimeout(function(){window.close();}, 500);
}

</script>