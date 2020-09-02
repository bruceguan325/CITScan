<%@page import="com.intumit.solr.SearchManager"%>
<%@ page contentType="application/json" pageEncoding="UTF-8" language="java"
import="com.intumit.solr.robot.*"
import="com.intumit.smartwiki.util.*"
import="com.intumit.solr.util.*"
import="java.io.*"
import="java.util.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.lang.math.RandomUtils"
import="org.apache.wink.json4j.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.tenant.*"
import="java.text.DateFormat"
import="java.text.SimpleDateFormat"
import="java.text.ParseException"
%><%!
static DateFormat yyyyMMdd = new SimpleDateFormat("yyyyMMdd");
static DateFormat yyyyMMddHHmmssS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
static {
	yyyyMMddHHmmssS.setTimeZone(TimeZone.getTimeZone("UTC"));
}

String getExpireDateQueryString(String date) throws ParseException {
	Date day = yyyyMMdd.parse(date);
	String query = String.format("EXPIRE_dt:[ %s TO %s ]", yyyyMMddHHmmssS.format(floor(day)), yyyyMMddHHmmssS.format(ceiling(day)));
	System.out.println(query);
	return query;
}

Date ceiling(Date date) {
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
	cal.set(Calendar.HOUR, 23);
	cal.set(Calendar.MINUTE, 59);
	cal.set(Calendar.SECOND, 59);
	cal.set(Calendar.MILLISECOND, 999);
	return cal.getTime();
}

Date floor(Date date) {
	Calendar cal = Calendar.getInstance();
	cal.setTime(date);
	cal.set(Calendar.HOUR, 0);
	cal.set(Calendar.MINUTE, 0);
	cal.set(Calendar.SECOND, 0);
	cal.set(Calendar.MILLISECOND, 0);
	return cal.getTime();
}
%><%
	Tenant t = null;
if (request.getParameter("apikey") != null) {
	String apikey = request.getParameter("apikey");
	com.intumit.solr.tenant.Apikey k = com.intumit.solr.tenant.Apikey.getByApiKey(apikey);
	t = k != null ? k.getTenant() : null;//Tenant.getTenantByApiKey(apikey);
}

if (t == null) {
	JSONObject errorOutput = new JSONObject();
	errorOutput.put("errorCode", 400);
	errorOutput.put("errorMessage", "Cann't determine skill type.");
	out.println(errorOutput.toString(2));
	return;
}
String expiredDate = request.getParameter("expiredDate");
JSONObject resp = new JSONObject();
if (StringUtils.isNotBlank(expiredDate)) {
	SolrQuery sq = new SolrQuery()
	.setRequestHandler("/browse")
	.addFilterQuery("dataType_s:COMMON_SENSE")
	.addFilterQuery("{!tag=DF}-isPart_i:[2 TO *]")
	.addFilterQuery(getExpireDateQueryString(expiredDate))
	.setStart(0)
	.setRows(Integer.MAX_VALUE);
	QueryResponse qr = t.getCoreServer().query(sq);
	SolrDocumentList docs = qr.getResults();
	for(SolrDocument doc : docs){
		SolrDocument oriDoc = QAUtil.getInstance(t).getMainQASolrDocument(Long.valueOf((Long)doc.getFirstValue("kid_l")));
		QA qa = new QA(oriDoc);
		qa.setIsKmsRelateExpiredMemo(true);
		SolrServer server = t.getCoreServer();
		server.add(qa);
		server.commit();
		JSONObject r = new JSONObject();
		r.put("id", doc.getFirstValue("id"));
		r.put("question", doc.getFirstValue("QUESTION_s"));
		Integer updateAdminId = (Integer)doc.getFieldValue("updatedAdminUserId_i");
		AdminUser updateAdmin = updateAdminId == null ? null : AdminUserFacade.getInstance().get(updateAdminId);
		if(updateAdmin != null) {
	String account = updateAdmin.getLoginName();
	JSONArray qaInfos;
	if(resp.has(account)) {
		qaInfos = resp.getJSONArray(account);
		qaInfos.put(r);
	}
	else {
		qaInfos = new JSONArray();
		qaInfos.put(r);
	}
	resp.put(account, qaInfos);
		}
	}
}
%><%= resp.toString(4) %>
