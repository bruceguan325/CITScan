<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="org.apache.wink.json4j.*" %>
<%@ page import="com.intumit.solr.robot.*" %>
<%@ page import="com.intumit.solr.tenant.*" %>
<%@ page import="com.intumit.syslog.OperationLogEntity" %>
<%@ page import="java.text.*" %>
<%@ page import="com.intumit.hithot.HitHotLocale" %>
<%@ page import="com.intumit.message.MessageUtil" %>
<%
JSONObject result = new JSONObject();

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) { 
	result.put("status", "denied");
	
	out.println(result);
	return;
}
%>
<%
Locale locale = HitHotLocale.determineLocale(request, false, false);
String action = request.getParameter("action");
Integer opLogId = (Integer) request.getAttribute("opLogId");
OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
if (log != null && action != null && action.equals("update")) {
	
try {
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	
	t = Tenant.get(t.getId());
	JSONArray faqJsonArray = new JSONArray(t.getFaqJson());
	
	if (faqJsonArray == null || faqJsonArray.isEmpty()) {
		faqJsonArray = new JSONArray();
	}
	ArrayList<Map<String, Object>> datalist = (ArrayList<Map<String, Object>>) faqJsonArray;
	
	String channel = request.getParameter("channel");
	String hot = request.getParameter("hot");
	Map<String, Object> data = new HashMap<String, Object>();
	data.put("channel", channel );
	if (hot !=null && !hot.equals("")) {
		data.put("questions", new JSONArray(QAUtil.parseMultiValue(hot)) );
	} else {
		data.put("questions", new JSONArray(new ArrayList<String>()) );
	}
	Calendar rightNow = Calendar.getInstance();
	SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	String modifyTime = timeFormat.format(rightNow.getTime());
	data.put("modifyTime", modifyTime );
	data.put("modifyUser", user.getName() );
	
	boolean hasModify = false;
	int index = 0;
	for (Object tmp : datalist) {
		Map<String, Object> tmpData = (Map<String, Object>)tmp;
		if ( channel.equals(tmpData.get("channel")) ) {
			datalist.set(index, data);
			hasModify = true;
			break;
		}
		index++;
	}
	
	if (!hasModify)
		datalist.add(data);
	
	JSONArray jsArray = new JSONArray(datalist);
	String oldFaq = t.getFaqJson();
	t.setFaqJson(jsArray.toString());
	Tenant.saveOrUpdate(t);
	
	result.put("status", MessageUtil.getMessage(locale, "hot.problems.save.success"));
	result.put("modifyTime", modifyTime);
	result.put("modifyUser", user.getName());
	//result.put("status", "channel : " + request.getParameter("channel") + " hot : " + request.getParameter("hot"));
	//result.put("status", jsArray.toString());
	log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
}
catch (Exception e) {
	result.put("status", MessageUtil.getMessage(locale, "hot.problems.save.error"));
	result.put("errorMsg", e.getMessage());
	e.printStackTrace();
	log.setStatusMessage(OperationLogEntity.Status.FAILED);
	log.appendToMoreDetails(e.toString());
}
log.update();
}
%><%= result %>
