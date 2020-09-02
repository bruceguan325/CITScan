<%@page import="flexjson.JSONSerializer"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java" 
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.json.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.util.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%! 
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

JSONObject map = new JSONObject();
String question = request.getParameter("question");
String currentIhAltTpls = StringUtils.trimToNull(request.getParameter("currentIhAltTpls"));

if (StringUtils.trimToNull(question) != null) {
	DictionaryDatabase[] kps = KnowledgePointDictionary.search(t.getId(), question.toCharArray(), null);
	
	if (kps.length > 0) {
		map.put("keywords", kps[0].getKeyword());
	}
}

List<QAAltTemplate> patterns = QAAltTemplate.list(t.getId());
Set<String> dup = new HashSet<String>();
JSONArray forOutput = new JSONArray();

if (currentIhAltTpls != null) {
	JSONArray arr = new JSONArray(currentIhAltTpls);
	
	for (int i=0; i < arr.length(); i++) {
		String str = arr.getString(i);
		String mkey = StringUtils.substringBefore(str, ":");
		String keywords = StringUtils.substringAfter(str, ":");
			
		for (QAAltTemplate tpl: patterns) {
			if (mkey.equals(tpl.getMkey())) {
				JSONSerializer jss = new JSONSerializer();
				jss.exclude("class", "priority");
				JSONObject obj = new JSONObject(jss.deepSerialize(tpl));
				obj.put("enabled", true);
				obj.put("keywords", keywords);
				obj.put("applied", new JSONArray(tpl.splitAppliedTemplate(StringUtils.split(keywords, ","))));
				forOutput.put(obj);
				dup.add(mkey);
			}
		}
	}
}

for (QAAltTemplate tpl: patterns) {
	if (!dup.contains(tpl.getMkey())) {
		JSONSerializer jss = new JSONSerializer();
		jss.exclude("class", "priority");
		JSONObject obj = new JSONObject(jss.deepSerialize(tpl));
		obj.put("applied", new JSONArray());//tpl.splitAppliedTemplate(StringUtils.split((String)map.get("keyword"), ","))));
		forOutput.put(obj);
		dup.add(tpl.getMkey());
	}
}

if (!map.has("keywords"))
	map.put("keywords", "");

map.put("tpls", forOutput);

out.println(map.toString(2));
%>