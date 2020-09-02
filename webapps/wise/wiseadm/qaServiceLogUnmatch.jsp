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
import="org.json.*"
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
	int reviewStatus = 0;
	int noAnswerCount = 0;
	
	public MyPagination(int rows, long total, int pageGap, int reviewStatus, int noAnswerCount, String baseUrl) {
		super(rows, total, pageGap, baseUrl);
		this.reviewStatus = reviewStatus;
		this.noAnswerCount = noAnswerCount;
	}
	
	@Override
	public String makeUrl(int start, int rows) {
		return "qaServiceLogUnmatch.jsp?reviewStatus=" + reviewStatus + "&nac=" + noAnswerCount + "&start=" + start + "";
	}
}
%><%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

Integer reviewStatus = Integer.parseInt(StringUtils.defaultString(request.getParameter("reviewStatus"), "0"));
int start = Integer.parseInt(StringUtils.defaultString(request.getParameter("start"), "0"));
int rows = 50;
int noAnswerCount = Integer.parseInt(StringUtils.defaultString(request.getParameter("nac"), "0"));
int total = Math.min(10000, ServiceLogEntityLite.countBy(t.getId(), "robot:cathay:chat", null, null, reviewStatus, null, null, null, noAnswerCount).intValue());
List<ServiceLogEntityLite> onlineSessions = ServiceLogEntityLite.listBy(t.getId(), "robot:cathay:chat", null, reviewStatus, null, null, null, noAnswerCount, start, rows);
MyPagination pagination = new MyPagination(rows, total, 5, reviewStatus, noAnswerCount, "");
Locale locale = HitHotLocale.determineLocale(request, true, true);
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/assets/javascripts/plugins/datatables/jquery.dataTables.numeric-comma.js"></script>
<link href="<%=request.getContextPath()%>/styles/bootstrap-slider.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bootstrap-slider.js"></script>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<h5 id="brqslm-h" class="">
	<div class="btn-group btnNavBtnGroup" role="group">
	<a href="qaServiceLog.jsp" class="btn btn-default"><bean:message key='qa.service.log'/></a>
	<% if (reviewStatus == 0) { %>
	<a href="qaServiceLogUnmatch.jsp?reviewStatus=-1" class="btn btn-default"><bean:message key='hidden.question.list'/></a>
	<% } else if (reviewStatus == -1) { %>
	<a href="qaServiceLogUnmatch.jsp" class="btn btn-default"><bean:message key='visible.question.list'/></a>
	<% } 
	%>
	</div>
	</h5>
    <br>
	<div class="col-md-12" style="padding-bottom: 10px">
		<div class="col-md-1">Threshold</div>
		<div class="col-md-3">
		<input type="text" id="noAnswerCount"
								name="noAnswerCount" class="col-md-8 slider" value="<%= noAnswerCount %>"
								data-slider-min="0" data-slider-max="50" data-slider-step="1" data-slider-value="<%= noAnswerCount %>" data-slider-handle="circle" data-slider-selection="after" data-slider-tooltip="show"><span id="noAnswerCountView"><%= noAnswerCount %></span><br>
		</div>
		<div class="col-md-6">
	    <%= pagination.toHtml(start, true) %>
		</div>
	</div>
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
        	JSONObject conversation = new JSONObject(entity.getEntity().getConversations());
        	String lastNoAnswerMessage = null;
        	Integer lastNoAnswerMessageIdx = null;

            if (conversation.has("messages")) {
                JSONArray messages = new JSONArray();
                try {
                    messages = conversation.getJSONArray("messages");
                }
                catch (Exception ex) {}

                for (int i=0; i < messages.length(); i++) {
                	Object obj = messages.get(i);
                	
                	if (obj instanceof JSONObject) {
                		JSONObject msg = (JSONObject)obj;
                		System.out.println(msg.toString(2));
                		
                		boolean isTriggeredByEvent = false;
    	            	String question = StringEscapeUtils.escapeXml(msg.optString("originalQuestion", "N/A"));
    	            	String answerType = msg.optString("answerType",  "");
    	            	
    	            	/*
    	            	String a = StringUtils.trimToEmpty(msg.optString("output", "N/A"));
                		String enterDt = msg.optString("originalQuestionTime", "");
    	            	String dt = msg.optString("datetime", "");
    	            	String kid = msg.optString("kid", "");
    	            	String scoreStr = msg.optString("score", null);
    	            	String confidence = msg.optString("confidence", null);
    	            	String confidenceFactor = msg.optString("confidenceFactor", null);
    	            	String confidenceClass = " well";
    	            	boolean negativeKw = msg.optBoolean("negativeKeywords", false);
    	            	boolean forwarded = false;
    	            	
    	            	String currentQaCategory = msg.optString("currentQaCategory",  "");
    	            	String currentCategory = msg.optString("currentCategory",  "");
    	            	String currentQaUse = msg.optString("currentQaUse",  "");
    	            	String eservice = msg.optString("eservice",  "");
    	            	String dialog = msg.optString("DIALOG",  "");
    	            	String userType = msg.optString("userType",  "");
    	            	*/
    	            	
    	            	if (QAContext.ANSWER_TYPE.NO_ANSWER.name().equals(answerType)) {
    	            		lastNoAnswerMessage = question;
    	            		lastNoAnswerMessageIdx = i;
    	            	}
                	}
                }
            }
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
                	<button type="button" data-id="<%= entity.getId() %>#<%= lastNoAnswerMessageIdx %>" class="btn-view-detail btn btn-default btn-success" id="brqslm-i">View</button>
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

	var options = { format: "csv", ed: true };

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

$('.slider').bootstrapSlider();
$('#noAnswerCount').bootstrapSlider().on('slideStop', function(_ev) {
    $('#noAnswerCountView').text(_ev.value);
    window.location='qaServiceLogUnmatch.jsp?start=0&reviewStatus=<%= reviewStatus %>&nac=' + _ev.value;
});
$(".timeago").timeago('locale', '<%= locale.getLanguage() + locale.getCountry() %>');
$(".timeago").timeago();
</script>
</BODY>
</HTML>