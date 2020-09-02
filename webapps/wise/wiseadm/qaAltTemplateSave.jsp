<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.util.*"
import="org.apache.solr.common.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.XssHttpServletRequestWrapper"
import="com.intumit.syslog.OperationLogEntity"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);

if (log == null) {
	response.getWriter().println("<script>alert('OperationLogEntity Missing');window.history.go(-1);</script>");
	return;
}

String action = xssReq.getParameter("action");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

if (action.equals("delete") && request.getParameter("id") != null) {
	QAAltTemplate.delete(t.getId(), new Integer(request.getParameter("id")));
	log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
} else if (action.equals("save")) {
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	
	QAUtil qautil = QAUtil.getInstance(t);
	QAAltTemplate p = new QAAltTemplate();
	boolean isUpdate = false;
	boolean isDirty = false;
	
	if (request.getParameter("id") != null) {
		p = QAAltTemplate.get(new Integer(request.getParameter("id")));
		if (p.getTenantId() != t.getId())
			return;
		
		isUpdate = true;
		log.setEvent("update");
	}
	else {
		p.setTenantId(t.getId());
	}
	p.setName(StringUtils.trimToNull(request.getParameter("name")));
	p.setDescription(StringUtils.trimToNull(request.getParameter("description")));
	p.setSuggestPatterns(StringUtils.trimToNull(request.getParameter("suggestPatterns")));
	
	String mkey = request.getParameter("mkey");
	if (isUpdate && !StringUtils.equals(p.getMkey(), mkey)) {
		isDirty = true;
		p.setMkey(StringUtils.trimToNull(mkey));
	}
	else if(!isUpdate){
		QAAltTemplate sameMkey = QAAltTemplate.getByKey(t.getId(), mkey);
		if(sameMkey == null) {
			p.setMkey(StringUtils.trimToNull(mkey));
		}
	}
	
	if (isUpdate && !StringUtils.equals(p.getTemplate(), request.getParameter("template"))) {
		isDirty = true;
		p.setTemplate(StringUtils.trimToNull(request.getParameter("template")));
	}else if(!isUpdate) {
		p.setTemplate(StringUtils.trimToNull(request.getParameter("template")));
	}
	
	int newPriority = new Integer(StringUtils.defaultString(request.getParameter("priority"), "0"));
	if (isUpdate && (p.getPriority() != newPriority)) {
		isDirty = true;
		p.setPriority(newPriority);
	}
	
	try {
		QAAltTemplate.saveOrUpdate(p);
		log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
	} catch (Exception e) {
		log.setStatusMessage(OperationLogEntity.Status.FAILED);
		log.appendToMoreDetails(e.toString());
	}
	
	if (isDirty) {
		QAAltTemplate.updateEffectedQA(t, p, user.getLoginName());
	}

}
log.update();

response.sendRedirect("qaAltTemplateList.jsp");
%>
