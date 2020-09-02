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
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) { 
	return;
}
%><%! 
void addFieldByPrefix(SolrInputDocument doc, String prefix, boolean isMV, Object val){
	String[] names = ProcessQADataServlet.getFieldName(prefix, isMV);

	for(String name:names){
		addField(doc, name, val);
	}
}
void addField(SolrInputDocument doc, String name, Object val){
	Map map = new HashMap();
	map.put("set", val);
	doc.addField(name, map);
}
List<String> getMultiValueParameters(HttpServletRequest request, String name){
	List<String> vals = new ArrayList<String>();
	String param = request.getParameter(name);
	if(StringUtils.isNotBlank(param)){
		String[] toks = param.split("\n");
		for(String t:toks){
			String val = StringUtils.trim(t);
			if(StringUtils.isNotBlank(val)){
				vals.add(val);	
			}
		}
	}
	return vals.isEmpty()?null:vals;
}
%><%

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

List<String> qaAtlTpls = getMultiValueParameters(request, "question_alt");
long begin = System.currentTimeMillis();
long altCount = qaAtlTpls == null ? 0 : QASaver.estimateAltCount(t.getId(), qaAtlTpls, false);
long timecost = System.currentTimeMillis() - begin;

%>{"estimate":<%=altCount%>}