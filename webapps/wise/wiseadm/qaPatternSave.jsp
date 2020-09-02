<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
import="java.security.SecureRandom"

import="com.intumit.solr.robot.qaplugin.CustomQA"
import="com.intumit.message.MessageUtil"
import="java.util.Locale"
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
	QAPattern p = new QAPattern();
	
	Integer opLogId = (Integer) request.getAttribute("opLogId");
	OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
	if (log == null) {
		response.getWriter().println("<script>alert('OperationLogEntity Missing');window.history.go(-1);</script>");
		return;
	}
	
	String action = request.getParameter("action");
	if (request.getParameter("id") != null && OperationLogEntity.DELETE.equalsIgnoreCase(action)) {
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
		response.sendRedirect("qaPatternList.jsp");
		} else {

	boolean keepEdit = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("keepEdit"), "false"));

	if (request.getParameter("id") != null) {
		p = QAPattern.get(new Integer(request.getParameter("id")));
		if (p.getTenantId() != t.getId()) {
			log.setStatusMessage(OperationLogEntity.Status.FAILED);
			log.appendToMoreDetails("Tenant Error");
			log.update();
			return ; // 資安考量
		}
	}

	String mkey = StringUtils.trimToNull(request.getParameter("mkey"));
	String back = "window.history.go(-1);";
	if (mkey == null) {
		log.setStatusMessage(OperationLogEntity.Status.FAILED);
		log.appendToMoreDetails("Mkey Empty");
		log.update();
		response.getWriter().println("<script>alert('" + MessageUtil.getMessage(locale, "custom.qa.without.mkey") + "');" + back + "</script>");
		return;
	}
	mkey = mkey.trim();
	QAPattern pa = QAPattern.getByKey(t.getId(), mkey);
	if (pa != null && pa.getId() != p.getId()) {
		log.setStatusMessage(OperationLogEntity.Status.FAILED);
		log.appendToMoreDetails("Mkey Repeat");
		log.update();
		response.getWriter().println("<script>alert('" + MessageUtil.getMessage(locale, "custom.qa.mkey.repeat") + "');" + back + "</script>");
		return;
	} 
	p.setTenantId(t.getId());
	if (p.getMkey() != null && !p.getMkey().equals(mkey)) {
		int result = MultiChannelAnswer.updateCfgByMkey(p.getMkey(), mkey, CustomQA.ID, t.getId());
		if (result < 0) {
			log.setStatusMessage(OperationLogEntity.Status.FAILED);
			log.appendToMoreDetails("Update Mkey Failed");
			log.update();
			response.getWriter().println("<script>alert('" + MessageUtil.getMessage(locale, "custom.qa.mkey.update.error") + "');" + back + "</script>");
			return;
		}
	}
	p.setMkey(mkey);
	p.setDataSource(QAPattern.DataSource.valueOf(request.getParameter("dataSource")));
	p.setQuestionTemplate(StringUtils.trimToNull(request.getParameter("questionTemplate")));
	p.setAnswerTemplate(StringUtils.trimToNull(request.getParameter("answerTemplate")));
	p.setAnswerVoice(StringUtils.trimToNull(request.getParameter("answerVoice")));
	p.setMaxMatched(new Integer(StringUtils.defaultString(request.getParameter("maxMatched"), "1")));
	p.setDataAggregator(StringUtils.trimToNull(request.getParameter("dataAggregator")));
	p.setSpecialRestriction(StringUtils.trimToNull(request.getParameter("specialRestriction")));
	p.setPreviewParameters(StringUtils.trimToNull(request.getParameter("previewParameters")));
	p.setStaticAnswer(StringUtils.trimToNull(request.getParameter("staticAnswer")));
	p.setAnswerPeriod(StringUtils.trimToNull(request.getParameter("answerPeriod")));
	
	QAPattern.saveOrUpdate(p);
	
	SecureRandom secRandom = new SecureRandom();
	int rand = secRandom.nextInt();
	String target = "?rnd=" + rand;
	
	log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
	log.update();

	if (keepEdit) {
		response.sendRedirect("qaPatternEdit.jsp?id=" + request.getParameter("id"));
	}
	else {
		response.sendRedirect("qaPatternList.jsp");
	}  
}
%>

