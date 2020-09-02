<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.admin.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qaplugin.*"
%>
<% 
String customQaPatternMkey = StringUtils.defaultString((String)request.getAttribute(CustomQA.QA_PATTERN_MKEY_PARAM));

String docId = StringUtils.defaultString((String)request.getAttribute("docId"), null);
String suggest = StringUtils.defaultString((String)request.getAttribute(CustomQA.SUGGEST_QUESTIONS_PARAM));
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
%>

<select name="<%= CustomQA.QA_PATTERN_MKEY_PARAM %>">
<% for(QAPattern p : QAPattern.list(t.getId())){ %>
	<option
		<% if (p.getMkey().toString().equals(customQaPatternMkey)){ %>selected="selected"<% } %> 
		value="<%= p.getMkey() %>"><%= StringEscapeUtils.escapeHtml(p.getQuestionTemplate()) %></option>
<% } %>
</select>
<br>
<br>
<%
Boolean noAnswerOnly = (Boolean)request.getAttribute(CustomQA.SUGGEST_QUESTIONS_WHEN_NO_ANSWER_ONLY_PARAM);
String checked = ( noAnswerOnly != null && noAnswerOnly ) ? "checked" : "";
%>
<bean:message key='custom.qa.suggest.noAnswerOnly'/><input type="checkbox" name="customQaSuggestWhenNoAnswerOnly" <%= checked %> data-size="mini" data-toggle="toggle"><br/>
<br>
<bean:message key='custom.qa.suggest'/><br>
<small><bean:message key='custom.qa.suggest.tips'/></small>
<textarea class="form-control" id="customqa_suggest" name="<%= CustomQA.SUGGEST_QUESTIONS_PARAM %>" rows="8"><%= StringEscapeUtils.escapeHtml(suggest) %></textarea>
<%
if (docId != null && AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0
		|| ("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) || 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	%>
	<div class="easterEgg">
	<br>
	<div class="alert alert-danger"><h3>Please save QA before start checking</h3>
		<br>
		<a class="btn btn-primary" target="_checkCustomQA"
			href="qaCustomQaCheck.jsp?id=<%= StringEscapeUtils.escapeJavaScript(docId) %>">Ok, I know. Start check Custom QA</a>
	</div>	
	</div>
	<%
}
%>