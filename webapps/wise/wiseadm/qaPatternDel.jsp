<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"

import="com.intumit.solr.robot.qaplugin.CustomQA"
import="com.intumit.message.MessageUtil"
import="org.apache.struts.Globals"
import="com.intumit.syslog.OperationLogEntity"

%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);
Integer opLogId = (Integer) request.getAttribute("opLogId");
OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
if (log != null && request.getParameter("id") != null) {
	QAPattern qp = QAPattern.get(new Integer(request.getParameter("id")));
	String answerPluginConfig = "\"" + CustomQA.QA_PATTERN_KEY_INDEX_FIELD + "\":\"MKEY\"";
	List<MultiChannelAnswer> mcas = MultiChannelAnswer.listByPlugin(t.getId(), CustomQA.ID, QAUtil.DATATYPE_COMMON_SENSE, answerPluginConfig.replace("MKEY", qp.getMkey()), false);
	if (mcas.size() == 0) {
		QAPattern.delete(t.getId(), new Integer(request.getParameter("id")));
		System.out.println("**********");
		System.out.println("QAPattern Delete : " + qp.getId() + ",at time : " + new Date());
		System.out.println("**********");
		log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
		log.update();
	} else {
		log.setStatusMessage(OperationLogEntity.Status.FAILED);
		log.appendToMoreDetails("Mkey Exist");
		log.update();
		response.getWriter().println("<script>alert('" + MessageUtil.getMessage(locale, "custom.qa.mkey.exist.in.answer") + "');window.history.go(-1);</script>");
		return;
	}
}

response.sendRedirect("qaPatternList.jsp");
%>
