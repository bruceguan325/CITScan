<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.intumit.quartz.ScheduleUtils"%>
<%@page import="com.intumit.quartz.Job"%>
<%@page import="com.intumit.solr.dataset.*" %>

<%@page import="org.dom4j.*"%>
<%@page import="org.apache.commons.httpclient.methods.GetMethod"%>
<%@page import="org.apache.commons.httpclient.HttpClient"%>
<%@page import="org.apache.commons.httpclient.auth.AuthScope"%>
<%@page import="org.apache.commons.httpclient.UsernamePasswordCredentials"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>

<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe - Helper Links</TITLE>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery-ui-1.9.2.custom.min.js"></script>
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<script src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/assets/javascripts/plugins/datatables/jquery.dataTables.numeric-comma.js"></script>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<script>
function doSubmit() {
	document.myForm.cmd.value="add";
	document.myForm.submit();
}
function doDelete(theForm) {
	theForm.cmd.value = "delete";
	theForm.submit();
}
function doUpdate(theForm) {
	theForm.cmd.value = "update";
	theForm.submit();
}

</script>

</HEAD>
<BODY>
<%
String action = request.getParameter("action");
String idStr = request.getParameter("id");

if ("create".equalsIgnoreCase(action)) {
	AdminGroupFacade.getInstance().save("New AdminGroup");
	%>
	<%= "New AdminGroup" %> created!
	<%
}
else if ("edit".equalsIgnoreCase(action) || "duplicate".equalsIgnoreCase(action)) {
	AdminGroup admGrp = AdminGroupFacade.getInstance().get(new Integer(idStr));
	%>
	<bean:message key='edit.group'/>
	<form action="groupAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<table class="bordered-table table table-striped">
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td>ID</td>
		<td width="300">
			<input type="hidden" name="id" value="<%= admGrp.getId() %>">
			<%= admGrp.getId() %>
		</td>
		<td>&nbsp;</td>
	</tr>
	<%
	}
	%>
	<tr>
		<td><bean:message key='group.name'/></td>
		<td width="300">
			<input type="text" size="40" name="name" value="<%= admGrp.getName() %>">
		</td>
		<td><bean:message key='group.name.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='top.solr.management'/></td>
		<td width="300">
			<table width="100%">
			<tr><th><bean:message key='global.add'/></th><th><bean:message key='modify'/></th><th><bean:message key='global.brower'/></th><th><bean:message key='delete'/></th></tr>
			<tr>
			<td><input type="checkbox" name="indexAdmin" value="<%= AdminGroup.C %>" <%= (admGrp.getIndexAdminCURD() & AdminGroup.C) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="indexAdmin" value="<%= AdminGroup.U %>" <%= (admGrp.getIndexAdminCURD() & AdminGroup.U) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="indexAdmin" value="<%= AdminGroup.R %>" <%= (admGrp.getIndexAdminCURD() & AdminGroup.R) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="indexAdmin" value="<%= AdminGroup.D %>" <%= (admGrp.getIndexAdminCURD() & AdminGroup.D) > 0 ? "checked" : ""%>></td>
			</tr>
			</table>
		</td>
		<td></td>
	</tr>
	<tr>
		<td><bean:message key='top.data.management'/></td>
		<td width="300">
			<table width="100%">
			<tr><th><bean:message key='global.add'/></th><th><bean:message key='modify'/></th><th><bean:message key='global.brower'/></th><th><bean:message key='delete'/></th></tr>
			<tr>
			<td><input type="checkbox" name="datasetAdmin" value="<%= AdminGroup.C %>" <%= (admGrp.getDataSetAdminCURD() & AdminGroup.C) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="datasetAdmin" value="<%= AdminGroup.U %>" <%= (admGrp.getDataSetAdminCURD() & AdminGroup.U) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="datasetAdmin" value="<%= AdminGroup.R %>" <%= (admGrp.getDataSetAdminCURD() & AdminGroup.R) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="datasetAdmin" value="<%= AdminGroup.D %>" <%= (admGrp.getDataSetAdminCURD() & AdminGroup.D) > 0 ? "checked" : ""%>></td>
			</tr>
			</table>
		</td>
		<td></td>
	</tr>
	<tr>
		<td><bean:message key='top.marketing.management'/></td>
		<td width="300">
			<table width="100%">
			<tr><th><bean:message key='global.add'/></th><th><bean:message key='modify'/></th><th><bean:message key='global.audit'/></th><th><bean:message key='delete'/></th></tr>
			<tr>
			<td><input type="checkbox" name="adphraseAdmin" value="<%= AdminGroup.C %>" <%= (admGrp.getAdphraseAdminCURD() & AdminGroup.C) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="adphraseAdmin" value="<%= AdminGroup.U %>" <%= (admGrp.getAdphraseAdminCURD() & AdminGroup.U) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="adphraseAdmin" value="<%= AdminGroup.E1 %>" <%= (admGrp.getAdphraseAdminCURD() & AdminGroup.E1) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="adphraseAdmin" value="<%= AdminGroup.D %>" <%= (admGrp.getAdphraseAdminCURD() & AdminGroup.D) > 0 ? "checked" : ""%>></td>
			</tr>
			</table>
		</td>
		<td></td>
	</tr>
	<tr>
		<td><bean:message key='global.robot.mamager'/></td>
		<td class="col-md-10">
			<table class="table table-bordered">
			<tr>
				<th><bean:message key='knowledge.tester'/><br><bean:message key='knowledge.test'/></th>
				<th><bean:message key='knowledge.trainer'/></th>
				<th><bean:message key='knowledge.trainer.batch.test'/></th>
				<th><bean:message key='knowledge.trainer.batch.wordSeg'/></th>
				<th><bean:message key='global.robot.mamager.exportLog'/></th>
				<th><bean:message key='export.eat.permission'/></th>
				<th><bean:message key='knowledge.export'/></th>
			</tr>
			<tr>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.R %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.R) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.A1 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.A1) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.A2 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.A2) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.A3 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.A3) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.A4 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.A4) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O6 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O6) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.E5 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.E5) > 0 ? "checked" : ""%>></td>
			</tr>
			<tr>
				<th><bean:message key='general.knowledge.maintenance'/><br><bean:message key='edit.knowledge'/></th>
				<th><bean:message key='high.knowledge.maintenance'/><br>(<bean:message key='global.synonyms'/>、<bean:message key='global.dictionary'/>)<br>(<bean:message key='global.standard.answer'/>、<bean:message key='global.test.record'/>)</th>
				<th><bean:message key='advanced.knowledge.maintenance'/><br>(<bean:message key='global.compulsory.break.word'/>、<bean:message key='global.special.answer'/>)</th>
				<th><bean:message key='super.knowledge.maintenance'/><br>(<bean:message key='global.batch.import'/>、<bean:message key='knowledge.trainer.change.result'/>、<bean:message key='global.hot.problems'/>)</th>
				<th><bean:message key='knowledge.audit'/><br></th>
				<th>知識瀏覽者</th>
				<th><bean:message key='life.langue.use'/><br></th>
				<th>WiVO管理</th>
				<th></th>
			</tr>
			<tr>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O1 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O1) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O2 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O2) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O3 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O4 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O4) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O5 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O5) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O7 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O7) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.O8 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.O8) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="adphraseAdmin" value="<%=AdminGroup.O1%>" <%= (admGrp.getAdphraseAdminCURD() & AdminGroup.O1) > 0 ? "checked" : ""%>></td>
			</tr>
			<tr>
				<th><bean:message key='top.statistical.analysis'/></th>
				<th><bean:message key='global.robot.mamager.scriptAdmin'/></th>
				<th><bean:message key='scenario.management'/></th>
				<th><bean:message key='wisesystemconfig.set'/><br>（<bean:message key='wisesystemconfig.set'/>、<bean:message key='global.scheduling'/>）</th>
				<th><bean:message key='top.authority.management'/></th>
			</tr>
			<tr>
			<td><input type="checkbox" name="statisticsAdmin" value="<%= AdminGroup.R %>" <%= (admGrp.getStatisticsAdminCURD() & AdminGroup.R) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.E1 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.E1) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.E2 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.E2) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.E3 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.E3) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="systemAdmin" value="<%= AdminGroup.E4 %>" <%= (admGrp.getSystemAdminCURD() & AdminGroup.E4) > 0 ? "checked" : ""%>></td>
			</tr>
			</table>
		</td>
		<td></td>
	</tr>
	<tr>
		<td><bean:message key='top.authority.management'/></td>
		<td width="300">
			<table width="100%">
			<tr><th><bean:message key='global.add'/></th><th><bean:message key='modify'/></th><th><bean:message key='global.brower'/></th><th><bean:message key='delete'/></th></tr>
			<tr>
			<td><input type="checkbox" name="adminAdmin" value="<%= AdminGroup.C %>" <%= (admGrp.getAdminAdminCURD() & AdminGroup.C) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="adminAdmin" value="<%= AdminGroup.U %>" <%= (admGrp.getAdminAdminCURD() & AdminGroup.U) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="adminAdmin" value="<%= AdminGroup.R %>" <%= (admGrp.getAdminAdminCURD() & AdminGroup.R) > 0 ? "checked" : ""%>></td>
			<td><input type="checkbox" name="adminAdmin" value="<%= AdminGroup.D %>" <%= (admGrp.getAdminAdminCURD() & AdminGroup.D) > 0 ? "checked" : ""%>></td>
			</tr>
			</table>
		</td>
		<td></td>
	</tr>

	<tr>
		<td colspan="3"><bean:message key='data.group.management'/></td>
	</tr>
	<%
	  List<DataSet> list = DataSetFacade.getInstance().listAll();
	  List<Integer> adminGroupIds = new ArrayList<Integer>();
	  adminGroupIds.add(admGrp.getId());

      for (int i=0; i < list.size(); i++) {
	     DataSet ds = list.get(i);
	     int id = ds.getId();
	     int acl = AdminGroupFacade.getInstance().getDataSetViewAcl(adminGroupIds, id);
	 %>
	<tr>
		<td><%=ds.getName()%>(id:<%=id%>)</td>
		<td width="300">
			<table width="100%">
			<tr><th><bean:message key='global.manage'/></th></tr>
			<tr>

			<td><input type="checkbox" name="datasetViewAdmin<%= i %>" value="<%= GroupDataSet.O4 %>" <%= (acl & GroupDataSet.O4) > 0 ? "checked" : ""%>></td>
			</tr>
			</table>
		</td>
		<td></td>
	</tr>
	<%
	   }
	%>
	<tr>
		<td></td>
		<td>
			<input class="btn btn-danger" type="submit" value="<bean:message key='submit'/>">
		</td>
		<td></td>
	</tr>
	</table>
	</form>
	<%
}
else if ("save".equalsIgnoreCase(action)) {
	AdminGroup admGrp = new AdminGroup();
	if (idStr != null)
		admGrp.setId(new Integer(idStr));
	admGrp.setName(request.getParameter("name"));

	String[] checks = null;
	int totalPerm = 0;

	checks = request.getParameterValues("coreAdmin");
	totalPerm = 0;
	for (int i=0; checks != null && i < checks.length; i++) {
		int perm = Integer.parseInt(checks[i]);
		totalPerm |= perm;
	}
	admGrp.setCoreAdminCURD(totalPerm);

	checks = request.getParameterValues("indexAdmin");
	totalPerm = 0;
	for (int i=0; checks != null && i < checks.length; i++) {
		int perm = Integer.parseInt(checks[i]);
		totalPerm |= perm;
	}
	admGrp.setIndexAdminCURD(totalPerm);

	checks = request.getParameterValues("datasetAdmin");
	totalPerm = 0;
	for (int i=0; checks != null && i < checks.length; i++) {
		int perm = Integer.parseInt(checks[i]);
		totalPerm |= perm;
	}
	admGrp.setDataSetAdminCURD(totalPerm);

	checks = request.getParameterValues("statisticsAdmin");
	totalPerm = 0;
	for (int i=0; checks != null && i < checks.length; i++) {
		int perm = Integer.parseInt(checks[i]);
		totalPerm |= perm;
	}
	admGrp.setStatisticsAdminCURD(totalPerm);

	checks = request.getParameterValues("adphraseAdmin");
	totalPerm = 0;
	for (int i=0; checks != null && i < checks.length; i++) {
		int perm = Integer.parseInt(checks[i]);
		totalPerm |= perm;
	}
	admGrp.setAdphraseAdminCURD(totalPerm);

	checks = request.getParameterValues("systemAdmin");
	totalPerm = 0;
	for (int i=0; checks != null && i < checks.length; i++) {
		int perm = Integer.parseInt(checks[i]);
		totalPerm |= perm;
	}
	admGrp.setSystemAdminCURD(totalPerm);

	checks = request.getParameterValues("adminAdmin");
	totalPerm = 0;
	for (int i=0; checks != null && i < checks.length; i++) {
		int perm = Integer.parseInt(checks[i]);
		totalPerm |= perm;
	}
	admGrp.setAdminAdminCURD(totalPerm);


	  GroupDataSet grpDataSet = new GroupDataSet();
	  List<GroupDataSet> grpDataSetList = new ArrayList<GroupDataSet>();
	  List<DataSet> dataSetList = DataSetFacade.getInstance().listAll();
      for (int j=0; j < dataSetList.size(); j++) {
	     DataSet ds = dataSetList.get(j);
	     int id = ds.getId();

		grpDataSet = new GroupDataSet();
		checks = request.getParameterValues("datasetViewAdmin"+j);
		totalPerm = 0;
		for (int i=0; checks != null && i < checks.length; i++) {
			int perm = Integer.parseInt(checks[i]);
			totalPerm |= perm;
		}
		grpDataSet.setDataSetViewAdminCURD(totalPerm);
		grpDataSet.setDataSetId(ds.getId());
		grpDataSet.setAdminGroupId(admGrp.getId());
		grpDataSetList.add(grpDataSet);
      }

      AdminGroupFacade.getInstance().saveOrUpdate(admGrp, grpDataSetList);
	%>
	<bean:message key='group'/>(<%= admGrp.getName() %>)<bean:message key='already.submit'/>
	<%
}
else if ("sudo".equalsIgnoreCase(action)) {
	AdminGroup admGrp = AdminGroupFacade.getInstance().get(new Integer(idStr));
	AdminGroupFacade.getInstance().setSession(session, admGrp);
	%>
	<bean:message key='already.transfiguration'/>(<%= admGrp.getName() %>)<bean:message key='group'/>!
	<script>
	window.parent.topFrame.location.reload();
	</script>
	<%
}
else if ("delete".equalsIgnoreCase(action)) {
	AdminGroupFacade.getInstance().delete(new Integer(idStr));
	%>
	<bean:message key='group'/>(<%= idStr %>)<bean:message key='already.remove'/>
	<%
}
%>
<TABLE width="100%" class="table table-striped" id="groups">
	<THEAD>
	<TR>
		<TH valign="top"><bean:message key='item'/></TH>
		<TH valign="top"><bean:message key='global.group.name'/></TH>
		<TH valign="top"><bean:message key='operation'/></TH>
	</TR>
	</THEAD>
	<TBODY>
<%
List<AdminGroup> list = AdminGroupFacade.getInstance().listAll();
for (int i=0; i < list.size(); i++) {
	AdminGroup admGrp = list.get(i);
	int id = admGrp.getId();
%>
	<TR>
		<TD align="center" valign="top" width="45"><%= admGrp.getId() %></TD>
		<TD align="center" valign="top"><%= admGrp.getName() %></TD>
		<TD align="center" valign="top" width="300">
			<a class="btn btn-success" href="groupAdmin.jsp?action=SUDO&id=<%= id %>"><bean:message key='transfiguration'/></a>
			<a class="btn btn-primary" href="groupAdmin.jsp?action=EDIT&id=<%= id %>"><bean:message key='modify'/></a>
			<a class="btn btn-danger" onclick="return confirm('<bean:message key="sure.del.group"/>');" href="groupAdmin.jsp?action=DELETE&id=<%= id %>"><bean:message key='delete'/></a>
			<a class="btn" onclick="return confirm('<bean:message key="sure.copy.group"/>');" href="groupAdmin.jsp?action=DUPLICATE&id=<%= id %>"><bean:message key='copy'/></a>
		</TD>
	</TR>
<%
}
%>
	</TBODY>
</TABLE>

<br>
<A class="btn btn-danger" target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/groupAdmin.jsp?action=create"><bean:message key="create.new.group"/></A>
<script>

$(document).ready(function() {

	$('#groups').DataTable(
		{ 
			"paging": false, 
			"dom": 'fiplrtif', 
			"order": [[ 1, "desc" ]],
			"columnDefs": [
			 { type: "numeric-comma", targets: 0 }
	        ]
		});
});
</script>
</BODY>
</HTML>
