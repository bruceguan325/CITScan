<%@page import="org.json.JSONObject"%>
<%@page import="org.json.JSONArray"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="java.util.regex.*"
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
import="com.intumit.solr.synonymKeywords.*"
import="com.intumit.solr.config.ColumnNameMappingFacade"
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
%>
<%!
%><%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
JSONArray jsonRs1 = new JSONArray();
JSONArray jsonRs2 = new JSONArray();
String[] alts = request.getParameter("qalt").split("\\r?\\n");
Pattern expandBlock = Pattern.compile("\\((.+?)\\)");
Set<String> set = new HashSet<String>();

for (String alt: alts) {
	Matcher m = expandBlock.matcher(alt);
	
	while (m.find()) {
		int start = m.start();
		int end = m.end();
		
		String before = StringUtils.substring(alt, 0, start);
		String after = StringUtils.substring(alt, end);
		String inners = m.group(1);
		
		String[] innerArr = StringUtils.splitPreserveAllTokens(inners, "|");
		
		for (String inner: innerArr) {
	if (set.contains(inner)) 
		continue;
	
	List<String> synonyms = SynonymKeywordFacade.getInstance().getSynonyms(t.getId(), inner, true);
	synonyms.remove(inner);
	
	if (synonyms.size() > 0) {
		set.add(inner);
		JSONObject obj = new JSONObject();
		obj.put("keyword", inner);
		obj.put("synonyms", new JSONArray(synonyms));
		jsonRs1.put(obj);
	}
	else {
		List<SynonymKeyword> synonymsAll = SynonymKeywordFacade.getInstance().listByQueryWhereReverseIsTrue(t.getId(), inner);
		
		if (synonymsAll.size() > 0) {
			set.add(inner);
			JSONObject obj = new JSONObject();
			obj.put("userKeyword", inner);
			
			JSONArray arr = new JSONArray();
			for (SynonymKeyword sk: synonymsAll) {
				JSONObject obj2 = new JSONObject();
				obj2.put("keyword", sk.getKeyword());
				obj2.put("synonyms", new JSONArray(StringUtils.split(sk.getSynonymKeyword(), ",")));
				arr.put(obj2);
			}
			obj.put("possibleSynonyms", arr);
			jsonRs2.put(obj);
		}
	}
		}
	}
}
%>
{"matchKeyword":<%= jsonRs1 %>,"notMatchKeyword":<%= jsonRs2 %>}