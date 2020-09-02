<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="java.util.List"
	import="com.intumit.solr.admin.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qaplugin.*"
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
String selectedMkey = (String)request.getAttribute(QADialogPlugin.QA_MKEY_PARAM);
Boolean dlgQaUseDraft = (Boolean)request.getAttribute(QADialogPlugin.QA_USE_DRAFT_PARAM);
if (dlgQaUseDraft == null) dlgQaUseDraft = true;

List<QADialogConfig> dlgCfgs = QADialogConfig.list(t.getId());
%>
<div class='form-inline'>
<select id="<%= QADialogPlugin.QA_MKEY_PARAM %>" name="<%= QADialogPlugin.QA_MKEY_PARAM %>" class='form-control'>
<% for (QADialogConfig dlgCfg: dlgCfgs) { %>
	<% boolean selected = StringUtils.equals(dlgCfg.getMkey(), selectedMkey); %>
	<option <%= selected ? "selected" : "" %> value="<%= dlgCfg.getMkey() %>" data-id="<%= dlgCfg.getId() %>">(<%= dlgCfg.getMkey() %>) <%= dlgCfg.getDialogName() %></option>
<% } %>
</select>&nbsp;&nbsp; 
<% if ((admGrp.getSystemAdminCURD() & AdminGroup.E2) > 0) { %>
<a target="_dialog" class="btn btn-md btn-success btnOpenVisualUI" title="開啟視覺化編輯"><span class='glyphicon glyphicon-wrench'></span></a>
<% } %>
</div>
<br>
<div class='form-inline'>
<input type="checkbox" name="<%= QADialogPlugin.QA_USE_DRAFT_PARAM %>" <%= dlgQaUseDraft ? "checked" : "" %> data-toggle="toggle"> <bean:message key='global.use'/><bean:message key='global.draft'/>
</div>
<br>
<script>
$('.btnOpenVisualUI').click(function() {
	$this = $(this);
	$id = $('#<%= QADialogPlugin.QA_MKEY_PARAM %>').find(":selected").attr('data-id');
	$this.attr('href', 'qaDialogEditor.jsp?id=' + $id);
	
});
</script>
<br>
<br>