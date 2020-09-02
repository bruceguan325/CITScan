<%@page import="org.apache.solr.common.SolrDocument"%>
<%@page import="org.apache.solr.common.SolrDocumentList"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.admin.*"
	import="java.util.Set"
	import="java.util.HashSet"
%><%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String curId = StringUtils.trimToNull(request.getParameter("id"));
String q = StringUtils.trim(request.getParameter("term"));
JSONArray jArray = new JSONArray();
Set<Long> existedIds = new HashSet<Long>();
if(StringUtils.isNotBlank(q)){
	QAContext qaCtx = new QAContext();
	qaCtx.setTenant(t);
	DictionaryDatabase[] currentKPs = new DictionaryDatabase[0];
	currentKPs = KnowledgePointDictionary.search(qaCtx.getTenant().getId(), q.toCharArray(), null);
	if(currentKPs.length > 0){
		qaCtx.setRestrictToQaCategory(currentKPs[0].getCategory());
	}
	SolrDocumentList docs = QAUtil.getInstance(t).searchCommonSense(q, qaCtx, true, false, true, 0, 10);
	for(SolrDocument d : docs){
		JSONObject jObj = new JSONObject();
		String id = (String) d.getFieldValue("id");
		Long kid = (Long) d.getFieldValue("kid_l");
		if(curId == null){
			jObj.put("id", QAUtil.DATATYPE_COMMON_SENSE + "-" + kid);
			jObj.put("label", d.getFieldValue("QUESTION_s"));
			jArray.put(jObj);
		}else if(!kid.equals(QAUtil.id2Kid(curId)) && !existedIds.contains(kid)){
			existedIds.add(kid);
			jObj.put("id", QAUtil.DATATYPE_COMMON_SENSE + "-" + kid);
			jObj.put("label", d.getFieldValue("QUESTION_s"));
			jArray.put(jObj);
		}
	}
}
%>
<%= jArray %>