<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" 
    import="java.util.*"
    import="org.apache.commons.lang.StringUtils"
     import="org.apache.wink.json4j.*"
    import="com.intumit.solr.admin.*"
    import="com.intumit.message.MessageUtil"
    import="com.intumit.solr.robot.*"
    import="com.intumit.systemconfig.*"
    import="com.intumit.solr.util.XssHttpServletRequestWrapper"
%>
<%!
static final String MASTER = WiSeReplicationSwitch.MASTER;
static final String SLAVE = WiSeReplicationSwitch.SLAVE;
%>
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();
List<WiSeReplicationSwitch> nodes = WiSeReplicationSwitch.listNodes(null, null);
String msg = xssReq.getParameter("msg");
%>
<!DOCTYPE>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Master/Slave</title>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript">
<%
	Locale local = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	String mxaCore = MessageUtil.getMessage(local, "max.core.num");
	String abstractNum = MessageUtil.getMessage(local, "abstract.num");
%>
</script>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
</head>
<body>
<% if (msg != null) { %>
<div class="alert">
  <button type="button" class="close" data-dismiss="alert alert-success">&times;</button>
  <strong> <%= msg %> </strong>
</div>
<% } %>
<form action="<%= request.getContextPath() %>/wiseadm/UpdateReplicationConfigServlet" method="POST" name="configForm">
<input type="hidden" name="action" value="update">
<table class="table table-bordered">
<tr>
<td><bean:message key='lbModeEnable'/></td>
<td><input type="text" name="lbModeEnable" value="<%=cfg.getLbModeEnable() %>"></td>
<td><bean:message key='lbModeEnable.ex'/></td>
</tr>
<tr>
<td>是否可切換</td>
<td><input type="text" name="lbModeSwitchable" value="<%=cfg.getLbModeSwitchable() %>"></td>
<td></td>
</tr>
<tr>
<td>容錯率</td>
<td><input type="number" name="lbErrorCount" value="<%=cfg.getLbErrorCount() %>"></td>
<td>若Sync錯誤超過Ｎ次，Master</td>
</tr>
</table>
<table class="table table-bordered  table-striped">
	<thead>
		<tr>
      		<th>機器名稱</th>
      		<th>Port</th>
      		<th>狀態</th>
      		<th>操作</th>
    	</tr>
  	</thead>
  	<tbody>
	<% for (WiSeReplicationSwitch node : nodes) { %>
		<tr>
      		<td><%= node.getHost() %></td>
      		<td><%= node.getPort() %></td>
      		<td><%= node.getReplicationStauts() %></td>
      		<td>
      		<% if (node.getReplicationStauts().equals(SLAVE)) { %>
      			<div class="radio">
      				<label><input type="radio" name="<%= node.getHost() %>_replicationStatus" value="<%= MASTER %>"><%= MASTER %></label>
      			</div>
      		<% } %>
      		<input type="number" name="<%= node.getHost() %>_replicationGroupId" value=<%= node.getGroupId() == null ? "1" : node.getGroupId() %> min="1">
      		<br>
      		<a href="<%= request.getContextPath() %>/wiseadm/UpdateReplicationConfigServlet?action=delete&nodeId=<%= node.getId() %>" class='btn btn-danger btnDelete' title="<bean:message key='delete'/>"><bean:message key='delete'/></a>
      		</td>
    	</tr>
	<% } %>
		<tr>
      		<td><input type="text" name="replicationHost"><br>(新機器名稱)</td>
      		<td><input type="text" name="replicationPort"><br>(新機器Port)</td>
      		<td>
	      		<div class="radio">
				  <label><input type="radio" name="replicationStatus" value="<%= MASTER %>"><%= MASTER %></label>
				  <label><input type="radio" name="replicationStatus" value="<%= SLAVE %>"><%= SLAVE %></label>
				</div>
				<input type="number" name="replicationGroupId" value="1" min="1">
      		</td>
      		<td></td>
    	</tr>
	</tbody>
</table>
<tr><td colspan="12">
<input class="btn btn-primary" type="submit" name="save" value="<bean:message key='submit.set'/>"></td></tr>
</form>
<script type="text/javascript">
var masteCount = 0;
$('input[type=radio]').change(function() {
	masteCount = 0;
	$('input[type=radio]:checked').each(function( index ) {
		if ($(this).val() == 'master') {
			masteCount++;
		}
	});
	if (masteCount > 1) {
		alert('一次僅能有一個master被選取');
		$(this).prop("checked", false);
	}
});
</script>
</body>
</html>
<%
// 進系統設定畫面會強制刷新一下目前的 master / slave 狀態
com.intumit.solr.ClusterMembershipListener.getInstance().checkAndSwitchMode();
%>
