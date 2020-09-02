<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
	import="com.intumit.hithot.*"
	import="com.intumit.solr.user.*"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="java.util.*"
	import="com.intumit.solr.admin.*"
%><%

if (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) {
	return;
}

HitHotLocale locale = request.getParameter("locale") != null ? HitHotLocale.valueOf(request.getParameter("locale")) : null;
String query = request.getParameter("q");

Integer userId = StringUtils.trimToNull(request.getParameter("uid")) == null ? null : new Integer(request.getParameter("uid"));
/*List<String> categories = UserOpinionRule.listCategory(user.getId());
String appendQuery = "";
if (categories != null && categories.size() > 0) {
	appendQuery = "Category_s:(\"" + StringUtils.join(categories, "\" OR \"") + "\")";
}
if (query != null) {
	query += " AND " + appendQuery;
}
else {
	query = appendQuery;
}*/
String gap = request.getParameter("g");
int sizeOfGap = Integer.parseInt(request.getParameter("sg"));

%>[
<% for (int i=0; i < sizeOfGap; i++) {
	if (i > 0) out.print(",");
	out.print("[" + (i-sizeOfGap) + ",0]");
}
%>]

