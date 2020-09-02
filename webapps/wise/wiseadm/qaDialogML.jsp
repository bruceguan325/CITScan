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
import="org.apache.wink.json4j.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.robot.entity.*"
import="com.intumit.solr.robot.intent.*"
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
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
ServiceLogEntity entity = ServiceLogEntity.get(new Integer(request.getParameter("id")));
if (entity.getTenantId() != t.getId()) return;
int offset = Integer.parseInt(request.getParameter("offset"));

JSONObject conversation = new JSONObject(entity.getConversations());
JSONObject msg = null;

if (conversation.has("messages")) {
    JSONArray messages = new JSONArray();
    try {
        messages = conversation.getJSONArray("messages");
    }
    catch (Exception ex) {}

    if (offset < messages.length()) {
	    	msg = messages.getJSONObject(offset);
    }
}

if (msg == null) {
	msg = new JSONObject();
}
%>
<HTML>
<HEAD>
<TITLE>情境問句學習</TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>
<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/select2/select2.js"></script>
<link href="<%=request.getContextPath()%>/assets/stylesheets/plugins/select2/select2.css" rel="stylesheet">
<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:9999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
</style>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
    <div class='row'>
      <div class='medium-12 columns'>
        <h1>情境問句學習</h1>
        <button class="btn btn-danger btnTrain">訓練</button>
        <br>
        <h2 class="text-danger" id="msg">
        </h2>
      </div>
    </div>
    <div class='row'>
    	<div class='col-md-12'>
    	<h3>情境</h3>
    	<select id="dialog" class="col-md-8">
    	<%
	List<QADialogConfig> patterns = QADialogConfig.list(t.getId());
	int no = 1;
	for (QADialogConfig p: patterns) {
    	%>
    	<option value="<%= p.getMkey() %>"><%= p.getDialogName() %></option>
    	<%
	}
    	%>
    	</select>
    	</div>
    	<div class='col-md-6'>
    	<h3>問句</h3>
    	<input id="originalQuestion" class="col-md-12" value="<%= msg.optString("originalQuestion", "") %>">
    	</div>
    	<div class='col-md-6'>
    	<h3>回答</h3>
    	<textarea id="output" class="col-md-12" disabled=true><%= StringEscapeUtils.escapeHtml(msg.optString("output", "")) %></textarea>
    	</div>
    </div>
    <div class='row'>
    	<div class='col-md-6' style="min-height:300px;">
    	<h3>意圖</h3>
		<select multiple="multiple" name="intents" class="col-md-12 select2-multiple-intents">
		<%
		List<QAIntent> allIntents = QAIntent.listByTenantId(t.getId());
				
		for (QAIntent id: allIntents) {
			String tag = id.getTag();
			%><option value="<%= tag %>"><%= tag %></option><%
		}
		%>
		</select>
    	</div>
	<div class='col-md-6' style="min-height:300px;">
    	<h3>實體</h3>
		<select multiple="multiple" name="entities" class="col-md-12 select2-multiple-entities">
		<%
		List<QAEntity> allEntities = QAEntity.listByTenantId(t.getId());

		for (QAEntity ed: allEntities) {
			String code = ed.getCode();
			%><option value="<%= code %>"><%= code %> (<%= ed.getName() %>)</option><%
		}
		%>
		</select>
		<BR>
		<h5>實體值</h5>
		<div class='col-md-12' id="entity-values-div" style="border: 1px solid black; min-height: 100px; padding: 10px;">
		
		</div> 
	    </div>
    </div>
</div>

<div class='loading-panel'></div>
<script>
var qaDialogData = JSON.parse(localStorage.getItem('qaDialogData'));

console.log(qaDialogData);
$intentsSelect2 = $('.select2-multiple-intents').select2();
$entitiesSelect2 = $('.select2-multiple-entities').select2();

$('#originalQuestion').val(qaDialogData.originalQuestion);

if (qaDialogData.hasOwnProperty("output"))
	$('#output').val(qaDialogData.output);
	
if (qaDialogData.hasOwnProperty("DIALOG"))
	$('#dialog').val(qaDialogData.DIALOG);

function refreshEntityValueInputs() {
	for (i in $entitiesSelect2.val()) {
		code = $entitiesSelect2.val()[i];
		targetId = code + "-values";
		
		if ($('#' + targetId).length == 0) {
			valuesInput = $('<input type="text" id="' + targetId + '" class="col-md-12" placeHolder="' + code + '"><br>');
			$('#entity-values-div').append(valuesInput);
		}
	}
}
$entitiesSelect2.on("change", function(e) {
	refreshEntityValueInputs();
});

$intentsSelect2.val($.map(qaDialogData.intents, function(item, i) { return item.tag; })).trigger("change");
$entitiesSelect2.val($.map(qaDialogData.entities, 
	function(item, i) { 
		targetId = item.code + "-values";
		if ($('#' + targetId).length == 0) {
			valuesInput = $('<input type="text" id="' + targetId + '" class="col-md-12" placeHolder="' + item.code + '">');
			valuesInput.val(item.entityValues);
			$('#entity-values-div').append(valuesInput);
			$('#entity-values-div').append($('<br>'));
		}
		return item.code; 
	})
	).trigger("change");

$(document).ready(function() {
	refreshEntityValueInputs();
});

$('.btnTrain').click(function() {
	$this = $(this);
	$('.loading-panel').show();
	
	entityValues = {};
	for (i in $entitiesSelect2.val()) {
		code = $entitiesSelect2.val()[i];
		targetId = code + "-values";
		
		if ($('#' + targetId).length != 0) {
			vals = $('#' + targetId).val();
			
			if (typeof(vals) != "undefined" && vals != '') {
				entityValues[code] = vals;
			}
		}
	}
	
	console.log({
		originalQuestion: $('#originalQuestion').val(),
		dialog: $('#dialog').val(),
		intents: $intentsSelect2.val(),
		entities: $entitiesSelect2.val(),
		entityValues: entityValues,
	});
	
	$.ajax({
		url: 'qa-dialog-ml-train-ajax.jsp',
		data: {
			originalQuestion: $('#originalQuestion').val(),
			dialog: $('#dialog').val(),
			intents: $intentsSelect2.val(),
			entities: $entitiesSelect2.val(),
			entityValues: JSON.stringify(entityValues),
		},
		success: function(resp) {
			$("#msg").text("成功！");
			$('.loading-panel').hide();
		}
	});
});
</script>
</BODY>
</HTML>
