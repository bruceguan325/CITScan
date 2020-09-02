<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="javax.servlet.ServletConfig"
import="javax.servlet.ServletException"
import="javax.servlet.http.*"
import="java.io.*"
import="java.net.*"
import="java.text.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.commons.httpclient.*"
import="org.apache.commons.httpclient.methods.*"
import="org.apache.commons.httpclient.params.HttpMethodParams"
import="org.apache.lucene.index.*"
import="org.apache.solr.core.*"
import="org.apache.solr.servlet.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.embedded.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.solr.common.cloud.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%!
static class MyPagination extends WiSeUtils.SimplePagination {
	public MyPagination(int rows, long total, int pageGap, String baseUrl) {
		super(rows, total, pageGap, baseUrl);
	}
	
	@Override
	public String makeUrl(int start, int rows) {
		return "qaTextCrmServiceLog.jsp?start=" + start + "";
	}
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

String[] timeRanges = {"today", "yesterday", "thisWeek"};
float[] rates = {0f, 0f, 0f};
String[] ratesColor = {"", "", ""};

int rows = 10;
int start = Integer.parseInt(StringUtils.defaultString(request.getParameter("start"), "0"));

Integer reviewStatus = Integer.parseInt(StringUtils.defaultString(request.getParameter("reviewStatus"), "0"));
List<TextCrmServiceLogEntity> onlineSessions = TextCrmServiceLogEntity.listByReviewStatus(t.getId(), reviewStatus, start, rows);
long totalCount = TextCrmServiceLogEntity.countBy(t.getId(), null, null, reviewStatus, null, null, null).longValue();
MyPagination pagination = new MyPagination(rows, totalCount, 5, "");

%>
<!DOCTYPE html>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/assets/javascripts/plugins/datatables/jquery.dataTables.numeric-comma.js"></script>
<script src='<%= request.getContextPath() %>/script/jquery.fileDownload.js' type='text/javascript'></script>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
    <table class="table table-bordered table-condensed table-striped table-hover" id="brqslm-r">
        <thead>
            <tr>
                <th><small><bean:message key='session.number'/></small></th>
                <th><small><bean:message key='enter.time.record'/></small></th>
                <th><small><bean:message key='end.record.time'/></small></th>
                <th><small>Tag</small></th>
                <th><small>Identity</small></th>
                <th><small><bean:message key='ip.position'/></small></th>
                <th><small>First Message</small></th>
                <th><small><bean:message key='statistics.msgCount.fromUser'/></small></th>
                <th><small><bean:message key='statistics.msgCount.fromRobot'/></small></th>
                <th><small><bean:message key='global.action'/></small></th>
            </tr>
        </thead>
        <tbody>
        <%
        for (TextCrmServiceLogEntity entity: onlineSessions) {
        %>
            <tr>
                <td>#<%= entity.getQaId() %></td>
                <td>
                	<%= entity.getTsCreated() %>
                </td>
                <td><span class="timeagooo" title="<%= entity.getTimestamp() %>"><%= entity.getTimestamp() %></span></td>
                <td><%= entity.getTag() %></td>
                <td><%= entity.getIdentity() %></td>
                <td><%= entity.getClientIp() %></td>
                <td><a class="btn-view-detail" data-id="<%= entity.getId() %>" data-jump-to="LAST"><%= StringUtils.defaultString(StringEscapeUtils.escapeXml(entity.getFirstMessage()), "N/A") %></a></td>
                <td><%= entity.getStatMsgCountFromUser() %></td>
                <td><%= entity.getStatMsgCountFromRobot() %></td>
                <td>
                    <a href="qaTextCrmServiceLogDetail.jsp?id=<%= entity.getId() %>" class="btn btn-success" id="brqslm-i">View</a>
                    <br>
                </td>
            </tr>
        <%
        }
        %>
        </tbody>
    </table>
    <%= pagination.toHtml(start) %>
</div>
<script>
$(".timeago").timeago();

$(".btn-view-detail").click(function() {
	var id = $(this).attr("data-id");
	var jumpTo = $(this).attr("data-jump-to");
	window.location = 'qaTextCrmServiceLogDetail.jsp?id=' + id + (typeof(jumpTo) != 'undefined' ? '&jt=' + jumpTo + '#JT' : '');
});

$('#brqslm-r').DataTable(
	{ 
		"paging": false, 
		"dom": 'iplrt', 
		"order": [[ 2, "desc" ]],
		"columnDefs": [
            { "type": "numeric-comma", targets: 7 },
            { "type": "numeric-comma", targets: 8 }
        ]
	});

</script>
</BODY>
</HTML>