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
import="com.intumit.hithot.HitHotLocale"
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
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%!
String getHitRatesTextColorClass(float rate) {
	if (rate < 0.5f) {
		return "text-danger";
	}
	else if (rate < 0.85f) {
		return "text-warning";
	}
	return "text-success";
}

static class MyPagination extends WiSeUtils.SimplePagination {
	public MyPagination(int rows, long total, int pageGap, String baseUrl) {
		super(rows, total, pageGap, baseUrl);
	}
	
	@Override
	public String makeUrl(int start, int rows) {
		return "qaServiceLog.jsp?start=" + start + "";
	}
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

String[] timeRanges = {"today", "yesterday", "thisWeek"};
float[] rates = {0f, 0f, 0f};
String[] ratesColor = {"", "", ""};

for (int i=0; i < timeRanges.length; i++) {
	String timeRange = timeRanges[i];
	Date[] timeRangePair = WiSeUtils.getTimeRangePair(timeRange);
	System.out.println(timeRangePair[0]);
	System.out.println(timeRangePair[1]);

	try {
		float rate = ServiceLogEntity.hitrateStatistic(timeRangePair[0], timeRangePair[1], t.getId())[3];
		rates[i] = rate;
		ratesColor[i] = getHitRatesTextColorClass(rate);
	}
	catch (Exception e) {
		float rate = 0;
		rates[i] = rate;
		ratesColor[i] = getHitRatesTextColorClass(rate);
	}
}

Integer reviewStatus = Integer.parseInt(StringUtils.defaultString(request.getParameter("reviewStatus"), "0"));

int start = Integer.parseInt(StringUtils.defaultString(request.getParameter("start"), "0"));
int rows = 50;
int total = Math.min(10000, ServiceLogEntityLite.countBy(t.getId(), "robot:cathay:chat", null, null, reviewStatus, null, null, null).intValue());
List<ServiceLogEntityLite> onlineSessions = ServiceLogEntityLite.listBy(t.getId(), "robot:cathay:chat", null, reviewStatus, null, null, null, start, rows);
MyPagination pagination = new MyPagination(rows, total, 5, "");
Locale locale = HitHotLocale.determineLocale(request, true, true);
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
<script src='<%= request.getContextPath() %>/wiseadm/js/My97DatePicker/WdatePicker.js' type='text/javascript'></script>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<h3 id="brqslm-h" class="">
		<bean:message key='hitrates.today'/>：<span class="<%= ratesColor[0] %>"><%= String.valueOf(rates[0] * 100) %>%</span> ,
		<bean:message key='hitrates.yesterday'/>：<span class="<%= ratesColor[1] %>"><%= String.valueOf(rates[1] * 100) %>%</span> ,
		<bean:message key='hitrates.thisweek'/>：<span class="<%= ratesColor[2] %>"><%= String.valueOf(rates[2] * 100) %>%</span>
	</h3>
	<h5 id="brqslm-h" class="">
	<div class="btn-group btnNavBtnGroup" role="group">
	<a href="qaServiceLogUnmatch.jsp" class="btn btn-default"><bean:message key='no.answered.question.list'/></a>
	<% if (reviewStatus == 0) { %>
	<a href="qaServiceLog.jsp?reviewStatus=-1" class="btn btn-default"><bean:message key='hidden.question.list'/></a>
	<% } else if (reviewStatus == -1) { %>
	<a href="qaServiceLog.jsp" class="btn btn-default"><bean:message key='visible.question.list'/></a>
	<% } 
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O6) > 0) {
	%>
	<button class="btn btn-danger" id="btnExport" target="_download"><bean:message key='export.eat.all'/></button>
	<%
	}
	%>
	</div>
	</h5>
	<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) > 0) { %>
	<%
	// 來算一下今日起迄，不然每次在那邊點日期跟時間太痛苦
	Calendar todayStart = Calendar.getInstance();
	todayStart.set(Calendar.HOUR_OF_DAY, 0);
	todayStart.set(Calendar.MINUTE, 0);
	todayStart.set(Calendar.SECOND, 0);
	Calendar todayEnd = Calendar.getInstance();
	todayEnd.set(Calendar.HOUR_OF_DAY, 23);
	todayEnd.set(Calendar.MINUTE, 59);
	todayEnd.set(Calendar.SECOND, 59);
	
	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	String startVal = sdf.format(todayStart.getTime());
	String endVal = sdf.format(todayEnd.getTime());
	%>
	<form name="robotResult" action="exportRobotResult.jsp" onsubmit="return validateForm()" method="post">
	  <bean:message key='export.cvs'/>&nbsp&nbsp:&nbsp&nbsp<bean:message key='time.range'/>
	  <input type="text" name="start" class="Wdate" id="d1" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', maxDate:'#F{$dp.$D(\'d2\')}'})" value="<%= startVal %>" required />&nbsp-&nbsp
	  <input type="text" name="end" class="Wdate" id="d2" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', minDate:'#F{$dp.$D(\'d1\')}', maxDate:'%y-%M-%d 23:59:59'})" value="<%= endVal %>" required />
	  <input type="hidden" name="r" value="<%= System.currentTimeMillis() %>">
	  <input type="submit" class="btn btn-default btn-sm" value="<bean:message key='global.submit'/>">
	</form>
	<form name="evaluationResult" action="exportEvaluationResult.jsp" onsubmit="return validateForm()" method="post">
	  <bean:message key='global.evaluation'/><bean:message key='export.cvs'/>&nbsp&nbsp:&nbsp&nbsp<bean:message key='time.range'/>
	  <input type="text" name="e_start" class="Wdate" id="d3" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', maxDate:'#F{$dp.$D(\'d4\')}'})" value="<%= startVal %>" required />&nbsp-&nbsp
	  <input type="text" name="e_end" class="Wdate" id="d4" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', minDate:'#F{$dp.$D(\'d3\')}', maxDate:'%y-%M-%d 23:59:59'})" value="<%= endVal %>" required />
	  <input type="hidden" name="r" value="<%= System.currentTimeMillis() %>">
	  <input type="submit" class="btn btn-default btn-sm" value="<bean:message key='global.submit'/>">
	</form>
	<form name="hitratesResult" action="exportHitratesResult.jsp" onsubmit="return validateForm()" method="post">
      <bean:message key='hitrates.report'/><bean:message key='export.cvs'/>&nbsp&nbsp:&nbsp&nbsp<bean:message key='time.range'/>
	  <input type="text" name="e_start" class="Wdate" id="d5" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', maxDate:'#F{$dp.$D(\'d6\')}'})" value="<%= startVal %>" required />&nbsp-&nbsp
	  <input type="text" name="e_end" class="Wdate" id="d6" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', minDate:'#F{$dp.$D(\'d5\')}', maxDate:'%y-%M-%d 23:59:59'})" value="<%= endVal %>" required />
	  <input type="hidden" name="r" value="<%= System.currentTimeMillis() %>">
      <input type="submit" class="btn btn-default btn-sm" value="<bean:message key='global.submit'/>">
    </form>
    <% if (t.getEnableTextCrm()) { %>
    <form name="satisfactionResult" action="exportSatisfactionResult.jsp" onsubmit="return validateForm()" method="get" target="_blank">
      <bean:message key='satisfaction.report'/><bean:message key='export.cvs'/>&nbsp&nbsp:&nbsp&nbsp<bean:message key='time.range'/>
	  <input type="text" name="e_start" class="Wdate" id="d7" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', maxDate:'#F{$dp.$D(\'d8\')}'})" value="<%= startVal %>" required />&nbsp-&nbsp
	  <input type="text" name="e_end" class="Wdate" id="d8" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', minDate:'#F{$dp.$D(\'d7\')}', maxDate:'%y-%M-%d 23:59:59'})" value="<%= endVal %>" required />
      <input type="submit" class="btn btn-default btn-sm" value="<bean:message key='global.submit'/>">
    </form>
    <% } %>
    <% if (t.getEnableScenarioCrm()) { %>
    <form name="robot_satisfactionResult" action="robot_exportSatisfactionResult.jsp" onsubmit="return validateForm()" method="post">
      <bean:message key='scenario.satisfaction.export'/><bean:message key='export.cvs'/>&nbsp&nbsp:&nbsp&nbsp<bean:message key='time.range'/>
	  <input type="text" name="start" class="Wdate" id="d7" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', maxDate:'#F{$dp.$D(\'d8\')}'})" value="<%= startVal %>" required />&nbsp-&nbsp
	  <input type="text" name="end" class="Wdate" id="d8" onclick="WdatePicker({skin:'whyGreen',dateFmt:'yyyy-MM-dd HH:mm:ss', lang:'<%=locale.getLanguage() + "-" +  locale.getCountry() %>', minDate:'#F{$dp.$D(\'d7\')}', maxDate:'%y-%M-%d 23:59:59'})" value="<%= endVal %>" required />
      <input type="submit" class="btn btn-default btn-sm" value="<bean:message key='global.submit'/>">
    </form>
    <% } %>
    <% } %>
    
    <br>
    <%= pagination.toHtml(start, true) %>
    <br>
    <table class="table table-bordered table-condensed table-striped table-hover" id="brqslm-r">
        <thead>
            <tr>
                <th><small><bean:message key='session.number'/></small></th>
                <th><small><bean:message key='enter.time.record'/></small></th>
                <th><small><bean:message key='end.record.time'/></small></th>
                <th><small><bean:message key='ip.position'/></small></th>
                <th><small><bean:message key='last.message'/></small></th>
                <th><small><bean:message key='last.qaCategory'/></small></th>
                <th><small><bean:message key='statistics.msgCount.fromUser'/></small></th>
                <th><small><bean:message key='statistics.msgCount.hasAnswer'/></small></th>
                <th><small><bean:message key='statistics.msgCount.noAnswer'/></small></th>
                <th><small><bean:message key='global.action'/></small></th>
            </tr>
        </thead>
        <tbody>
        <%
        for (ServiceLogEntityLite entity: onlineSessions) {
        %>
            <tr>
                <td>#<%= entity.getIdentity() %></td>
                <td>
                	<%= entity.getTsCreated() %>
                </td>
                <td><span class="timeago" title="<%= entity.getTimestamp() %>"><%= entity.getTimestamp() %></span></td>
                <td><%= entity.getClientIp() %></td>
                <td><a class="btn-view-detail" data-id="<%= entity.getId() %>" data-jump-to="LAST"><%= StringUtils.defaultString(StringEscapeUtils.escapeXml(entity.getLastMessage()), "N/A") %></a></td>
                <td><%= StringUtils.defaultString(entity.getLastQaCategory(), "N/A") %></td>
                <td><%= entity.getStatMsgCountFromUser() %></td>
                <td><%= entity.getStatMsgCountHasAnswer() %></td>
                <td><%= entity.getStatMsgCountNoAnswer() %>
                	<%= (entity.getStatForward() & 1) > 0 ? "&nbsp;<span class='text-warning'>(轉)</span>" : "" %>
                	<%= (entity.getStatForward() & 2) > 0 ? "&nbsp;<a class='btn-view-detail' data-id='" + entity.getId() + "' data-jump-to='NK'><span class='text-danger'>(敏)</span></a>" : "" %>
                </td>
                <td>
                    <button type="button" data-id="<%= entity.getId() %>" class="btn-view-detail btn btn-success" id="brqslm-i">View</button>
                    <br>
                </td>
            </tr>
        <%
        }
        %>
        </tbody>
    </table>
    
    <%= pagination.toHtml(start, true) %>
    <br>
</div>
<script>
$(".btn-view-detail").click(function() {
	var id = $(this).attr("data-id");
	var jumpTo = $(this).attr("data-jump-to");
	var theUrl = 'qaServiceLogDetail.jsp?id=' + id + (typeof(jumpTo) != 'undefined' ? '&jt=' + jumpTo + '#JT' : '');
	window.open(theUrl, '_ServiceLogDetail');
});

$('#brqslm-r').DataTable(
	{ 
		"paging": false, 
		"dom": 'plrt', 
		"order": [[ 2, "desc" ]],
		"columnDefs": [
            { "type": "numeric-comma", targets: 6 },
            { "type": "numeric-comma", targets: 7 },
            { "type": "numeric-comma", targets: 8 }
        ]
	});
	
$('#btnExport').click(function() {
	var url = "qaEvaluationLog-ajax.jsp";

	var options = { format: "xlsx", ed: true };

	var dq = true;

	if(typeof(dq) != 'undefined') {
		options.dq = dq;
	}

	if (typeof(flag) != 'undefined') {
		options.flag = flag;
	}
	if (typeof(timeRange) != 'undefined') {
		options.timeRange = timeRange;
	}
	if (typeof(lastTestResult) != 'undefined') {
		options.ltr = lastTestResult;
	}
	var admId = -1;
	if(typeof(admId) != 'undefined') {
		options.admId = admId;
	}

	$.fileDownload(url,
		{
	        preparingMessageHtml: "We are preparing your report, please wait...",
	        failMessageHtml: "There was a problem generating your report, please try again.",
	        httpMethod: "GET",
	        data: options
	    });
});

$(".timeago").timeago('locale', '<%= locale.getLanguage() + locale.getCountry() %>');
$(".timeago").timeago();
</script>
</BODY>
</HTML>