<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugins"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugin"%>
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
import="com.intumit.solr.robot.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O8) == 0) {
	String url = request.getRequestURI();
	String qs = request.getQueryString();
	if(StringUtils.isNotBlank(qs)){
		url += "?" + qs;		
	}
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp?r=<%= StringEscapeUtils.escapeJavaScript(URLEncoder.encode(url, "UTf-8")) %>';
	</script>
	<%
	return;
}
%><%! 
String getFieldValue(SolrDocument doc, String name){
	return doc.getFieldValue(name) != null?doc.getFieldValue(name).toString():"";
}
String getFieldValues(SolrDocument doc, String name){
	return doc.getFirstValue(name) != null?StringUtils.join(doc.getFieldValues(name), "\n"):"";
}
public String buildStatusIcon(QAAltBuildQueue.Status buildStatus,Locale locale) {
	String buildStatusText="";
	if (buildStatus == QAAltBuildQueue.Status.PENDING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PENDING) {
		buildStatusText = MessageUtil.getMessage(locale, buildStatus.text);
		return "<h1 class='text-danger'><span class='text-danger glyphicon glyphicon-time'></span>" + buildStatusText + "</h1>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.PROCESSING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PROCESSING) {
		buildStatusText = MessageUtil.getMessage(locale, buildStatus.text);
		return "<h1 class='text-warning'><span class='text-warning glyphicon glyphicon-refresh'></span>" + buildStatusText + "</h1>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.DONE) {
		buildStatusText = MessageUtil.getMessage(locale, buildStatus.text);
		return "<h1 class='text-success'><span class='text-successs glyphicon glyphicon-ok-sign'></span>" + buildStatusText + "</h1>";
	}
	buildStatusText = MessageUtil.getMessage(locale, buildStatus.text);
	return "<h1 class='text-danger'><span class='text-danger glyphicon glyphicon-exclamation-sign'></span>" + buildStatusText + "</h1>";
}
%><%

String id = StringUtils.trimToNull(request.getParameter("id"));
String binderIdParam = request.getParameter("binderId");
Long kid = null;
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

SolrDocument doc = null;

if (id != null) {
	SolrQuery q = new SolrQuery();
	q.setQuery("id:\"" + URLEncoder.encode(id, "UTF-8") + "\"");
	SolrServer server = QAUtil.getCasualServer(t, true);
	SolrDocumentList result = server.query(q).getResults();
	
	if (result.getNumFound() == 0) return;
	
	doc = result.get(0);
	kid = QAUtil.id2Kid(id);
}
else {
	doc = new SolrDocument();	
}
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:9999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
.testScore span {
    margin: 0 5px;
}
.answer-text {
  display: block; 
  max-width: 400px;
  height: 67px; /* Fallback for non-webkit */
  margin: 0 auto;
  font-size: 14px;
  line-height: 1.4;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
</head>
<body>
<%
boolean hasBinderIdParam = StringUtils.isNotBlank(binderIdParam);
boolean hideNavBar = hasBinderIdParam || Boolean.parseBoolean(request.getParameter("hideNavBar"));
%>
<jsp:include page="navbar-qa.jsp"><jsp:param name="hideUI" value="<%= hideNavBar %>" /></jsp:include>

<div class="container">
<BR>
<form id="form" role="form" class="form-horizontal col-md-12" method="post">
<input type="hidden" name="hideNavBar" value=<%= hideNavBar %> />
<input type="hidden" name="height" />
<% if (id != null) { %>
<input type="hidden" name="id" value="<%= StringEscapeUtils.escapeHtml(id) %>" />
<% } 
String binderId = getFieldValues(doc, "BINDER_ID_ml");
if(hasBinderIdParam){
	binderId += (StringUtils.isBlank(binderId)?"":"\n") + binderIdParam;
}
%>
<input type="hidden" name="binderId" value="<%= StringEscapeUtils.escapeHtml(binderId) %>" />
<%
String ques = getFieldValue(doc, "QUESTION_s");
String quesAlt = getFieldValues(doc, "QUESTION_ALT_TPL_ms");
String answer = getFieldValue(doc, "ANSWER_s");
String answerAlts = getFieldValues(doc, "ANSWER_ALT_ms");
%>
<div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='standard.problem'/></label>
    <div class="col-sm-9">
<input type="text" class="form-control" name="question" value="<%= StringEscapeUtils.escapeHtml(ques) %>"></input>
    </div>
    <div class="col-sm-1">
    <button id="check" type="button" class="btn btn-default"><bean:message key='global.detection'/> </button>
    </div>
  </div>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='kinds.of.questions.template'/> </label>
    <div class="col-sm-10">
<textarea class="form-control" rows="5" name="question_alt">
<%= StringEscapeUtils.escapeHtml(quesAlt) %>
</textarea>
    <p class="help-block">
    	<ul>
    	<!-- <li>若欲提供一個問題的多種問法，可用換行提供所有可能的問題文字</li>
    	<li>問句開頭的「我」「我們」「你們」「你」可忽略不用輸入，但「怎麼」「怎樣」「如何」現在必需輸入（否則跟「何謂」無法分別）</li>
    	<li>問句結尾的「了」「嗎」可忽略不用輸入</li>
    	<li>問句內(包含開頭及結尾)的「的」「是」及標點符號可忽略不用輸入</li> -->
    	<bean:message key='question.alt.ex'/>
    	</ul>
    
    	<button id="checkSyn" type="button" class="btn btn-default"><bean:message key='global.detection'/><bean:message key='global.synonyms'/></button>
    </p>
    </div>
  </div>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='standard.answer'/> </label>
    <div class="col-sm-10">
<textarea class="form-control" rows="5" name="answer"><%= StringEscapeUtils.escapeHtml(answer) %></textarea>
    </div>
  </div>
  <div class="form-group">
    <label for="inputAnswerAlt" class="col-sm-2 control-label"><bean:message key='input.answer.alt'/></label>
    <div class="col-sm-10">
<textarea class="form-control" rows="5" name="answerAlts"><%= StringEscapeUtils.escapeHtml(answerAlts) %></textarea>
    </div>
  </div>
  <div class="form-group">
    <div class="col-sm-offset-2 col-sm-10">
<select name="choosedQAPlugIn">
	<option value=""><bean:message key='global.without'/><bean:message key='advanced.answer'/></option>
	<%
	String answerPluginId = (String) doc.getFieldValue("ANSWER_PLUGIN_ID_s");
    // start 相容舊資料
    if(answerPluginId == null){
		String answerLink = getFieldValue(doc, "ANSWER_LINK_s");
		String answerRichtext = getFieldValue(doc, "ANSWER_RICHTEXT_s");
		if(StringUtils.isNotBlank(answerLink)){
			answerPluginId = QAPlugins.LINK_ID;
		}else if(StringUtils.isNotBlank(answerRichtext)){
			answerPluginId = QAPlugins.RICHTEXT_ID;
		}
    }
	// end
	for(QAPlugin p : QAPlugins.list()){
		String pId = p.getId();
		if(p.hasEditorPage()){
	%>
	<option
		<% if(pId.equals(answerPluginId)){ %>selected="selected"<% } %> 
		value="<%= StringEscapeUtils.escapeHtml(pId) %>"><%= StringEscapeUtils.escapeHtml(MessageUtil.getMessage(locale, p.getName())) %></option>
	<%
		}
	}
	%>
</select> 	
<div style="height: 0.5em;"></div>
<%
for(QAPlugin p : QAPlugins.list()){
%>
<div id="qa-plugin-panel-<%= StringEscapeUtils.escapeHtml(p.getId()) %>" style="display:none;">
	<% 
	String pagePath = p.onEditorPageCreate(doc, request);
	if(p.hasEditorPage() && StringUtils.isNotBlank(pagePath)){
	%>
	<jsp:include page="<%= pagePath %>" />
	<%
	}
	%>
</div>
<%
}
%>
  </div>
  </div>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='save.time'/></label>
    <div class="col-sm-3">
      <%
      Date expire = (Date) doc.getFieldValue("EXPIRE_dt");
	  boolean hasExpire = expire != null;
      String formattedExpire = hasExpire?QAUtil.formatDate(expire):"";
      Integer expireDays = (Integer) doc.getFieldValue("EXPIRE_DAYS_i");
      boolean hasExpireDays = expireDays != null; 
      boolean forever = !hasExpire && ExpireDuration.forever.is(expireDays);
      Date created = (Date) doc.getFieldValue("created_dt");
      long createdTime = created == null?System.currentTimeMillis():created.getTime();
      ExpireDuration duration = ExpireDuration.of(expireDays);
      long expireTime = duration.getExpireTime(createdTime);
      %>
      <b><%= forever?MessageUtil.getMessage(locale, "at.present.forever.save"):(MessageUtil.getMessage(locale, "at.present.save.time")+"：" + StringEscapeUtils.escapeHtml(
   		 hasExpire?formattedExpire:QAUtil.formatDate(new Date(expireTime)) + "（" + duration.label + "）"
      ))%></b>
      <% for(ExpireDuration e: ExpireDuration.values()){ %>
       <div class="input-group">
       	  <label style="font-weight: normal;">
		      <input type="radio" name="expireRadio" value="<%= e.days %>" 
		      	<% if(e.is(expireDays)){ %>checked<% } %>> <%= MessageUtil.getMessage(locale, e.label) %>
	      </label> 
	   </div>
	  <% } %>
      <div class="input-group">
	      <span class="input-group-addon">
	        <input type="radio" name="expireRadio" value=""
	        	<% if(!hasExpireDays && hasExpire){ %>checked<% } %>>
	      </span>
	      <input type="text" class="form-control" name="expire" placeholder='<bean:message key="custom.expiration.date"/>'
	        readonly="readonly" style="background-color: transparent;"
	      	value="<%= StringEscapeUtils.escapeHtml(hasExpire?formattedExpire:
      			QAUtil.formatDate(new Date(ExpireDuration.one_month.getExpireTime(createdTime)))) %>">
	   </div><!-- /input-group -->
	 </div>
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='global.label'/></label>
    <div class="col-sm-3">
    	<%
    	String category = getFieldValues(doc, "CATEGORY_ms");
    	%>
	    <textarea class="form-control" rows="5" name="category"><%= StringEscapeUtils.escapeHtml(category) %></textarea>
	    <p class="help-block"><bean:message key='global.label.ex'/></p>
	 </div>
  </div>
  
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='other.set'/></label>
    <div class="col-sm-3">
    <%
    Boolean isNotForMlt = (Boolean)doc.getFirstValue(QA.FN_IS_NOT_FOR_MLT);
    String checked = ( isNotForMlt == null || !isNotForMlt ) ? "checked" : "";
    %>
    <bean:message key='can.recommended'/><input type="checkbox" name="forMlt" <%= checked %> data-size="mini" data-toggle="toggle"><br/>
    <%
    Boolean isNotForSearch = (Boolean)doc.getFirstValue(QA.FN_IS_NOT_FOR_SEARCH);
    checked = ( isNotForSearch == null || !isNotForSearch ) ? "checked" : "";
    %>
    <bean:message key='can.selected'/><input type="checkbox" name="forSearch" <%= checked %> data-size="mini" data-toggle="toggle"><br/>
    <%
    Boolean notAppendMlt = (Boolean)doc.getFirstValue(QA.FN_NOT_APPEND_MLT);
    checked = ( notAppendMlt == null || !notAppendMlt ) ? "checked" : "";
    %>
    <bean:message key='additional.recommendations'/> <input type="checkbox" name="appendMlt" <%= checked %> data-size="mini" data-toggle="toggle"><br/>
    <br/>
    </div>
  </div>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='archive.and.other.functions'/></label>
    <div class="col-sm-5">
    	<div class="row>">
    <%
    Boolean banned = (Boolean)doc.getFirstValue(QA.FN_BANNED);
    checked = ( banned == null || !banned ) ? "checked" : "";
    %>
        <input type="checkbox" name="notBanned" <%= checked %> data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
  		<button id="save" type="button" class="btn btn-danger"><bean:message key='submit'/></button>
  		<button id="preivew" type="button" class="btn btn-primary"><bean:message key='global.preview'/></button>
  		</div>
  		<p></p>
		<% if (id != null) { %>
    	<div class="row">
  		</div>
  		<% } %>
    </div>
    <div class="col-sm-5 text-info">
  		<%
  		Date lastUpdated = (Date) doc.getFieldValue("updated_dt");
  		SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  		if (lastUpdated != null){
  			AdminUser user = null;
  			Integer lateUpdatedAdminUserId = (Integer) doc.get("updatedAdminUserId_i");
  			if(lateUpdatedAdminUserId != null){
  				user = AdminUserFacade.getInstance().get(lateUpdatedAdminUserId);	
  			}
  		%>
  		<bean:message key='last.edit'/>：<br/><%= StringEscapeUtils.escapeHtml(timeFormat.format(lastUpdated) + 
  			(user == null?"":("，" + user.getName() + "（" + user.getLoginName()) + "）"))
  		%>
  		<br/><bean:message key='handle.state'/>：<%= buildStatusIcon(QAAltBuildQueue.getStatus(t.getId(), id),locale) %>
  		<%
  		}
  		%>
    </div>
  </div>
</form>
</div>
<div id="check-dialog" title='<bean:message key='similar.problems'/>' style="display:none;">
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="check-list" class="table table-striped table-hover">
  	</table>
  </div>
</div>
<div id="test-dialog" title='<bean:message key='global.test'/><bean:message key='qa.service.log'/>' style="display:none;">
  <div class="pull-right" style="margin: 10px;">
  <input type="checkbox" id="checkIgnoreFlag0" name="checkIgnoreFlag0" checked><bean:message key='ignore.not.select'/>&nbsp;
  <button id='btnAutoTestAll' data-loading-text="<bean:message key='global.testing'/>" class='btn btn-default'><bean:message key='test.all'/><br><div class='testScore'></div></button>
  </div>
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="test-list" class="table table-striped table-hover">
	<tr>
		<th><bean:message key='global.evaluation'/></th>
		<th><bean:message key='test.question'/></th>
		<th><bean:message key='test.expected.answer.number'/></th>
		<th><bean:message key='robot.answer.number'/></th>
		<th><bean:message key='robot.answer'/></th>
		<th><bean:message key='global.confidence.level'/></th>
		<th><bean:message key='test.time'/></th>
		<th><bean:message key='operation'/></th>
	</tr>
  	</table>
  </div>
</div>
<div id="check-synonym-dialog" title="<bean:message key='global.detection'/><bean:message key='global.synonyms'/>" style="display:none;">
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="check-synonym-list" class="table table-striped table-hover">
  	</table>
  </div>
</div>
<div id="preview-dialog" title="<bean:message key='global.preview'/>" style="display:none; padding:0;">
  <div class="container" style="background-color: white;">
    <div style="padding: 15px 0 0;">
	  	<label style="font-weight: normal;">
	      <input type="radio" name="previewType" value="1024,768" checked="checked">&nbsp;PC
	    </label>&nbsp;&nbsp;
	    <label style="font-weight: normal;">
	      <input type="radio" name="previewType" value="800,600">&nbsp;Mobile 
	    </label>
    </div>
  </div>
</div>
<div id="change-flag-dialog" title="<bean:message key='adjust.test.record'/>" style="display:none; z-index:999; ">
  <center>
  <h1>
	<span data-flag="1" class="glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>
	<span data-flag="2" class="glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span>
	<span data-flag="3" class="glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span>
	<span data-flag="4" class="glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span>
	<span data-flag="5" class="glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span>
	<span data-flag="6" class="glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span>
  </h1>
  </center>
</div>
<div class='loading-panel'></div>
<script>
function getQuestionLink(kid, textClass) {
	if (kid != null && typeof(kid) != 'undefined') {
		var a = $('<a href="qaDataEditor.jsp?id=<%= QAUtil.DATATYPE_COMMON_SENSE %>-' + kid + '" target="_blank"/>');
		if (typeof(textClass) != 'undefined') {
			a.append($('<span/>').addClass(textClass).text(kid));
		}
		else {
			a.text(kid);
		}
		return a;
	}
	
	if (typeof(textClass) != 'undefined') {
		return $('<span/>').addClass(textClass);
	}
	return $('<span/>');
}

$(function() {
	var now = new Date();
	$.datepicker.setDefaults({
		dateFormat: 'yy/mm/dd',
		minDate: now
	});
    $( 'input[name="expire"]' ).datepicker();
    $('[name=choosedQAPlugIn]').change(function(){
    	$('[id^="qa-plugin-panel-"]').hide();
    	var idToShow = $(this).val(); 
    	if(idToShow){
    		$('[id="qa-plugin-panel-' + idToShow + '"]').show();
    	}
    });
    $('[id="qa-plugin-panel-' + $('[name=choosedQAPlugIn]').val() + '"]').show();
    $('#save').click(function(){
		$('.loading-panel').show();
		
		checkIfStillLogin(function(goOn) {
      		  if (goOn) {
	      			$.ajax({
	      				type: 'POST',
	      				dataType: 'json',
	      				url: 'qaEstimateAltCount.jsp',
	      				data: {
	      					question_alt: $('textarea[name=question_alt]').val() 
	      				},
	      				success: function(result) {
	      					if (confirm("<bean:message key='forecast.problem.arry.number'/>：[" + result.estimate.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",") + "] <bean:message key='sure.continue'/>")) {
	      				    	$('#form').attr({
	      				   			action: 'qaDataSave2.jsp?action=save',
	      				   			target: null
	      				  		}).submit();
	      					}
	      					else {
	      						$('.loading-panel').hide();
	      					}
	      				}
	      			});
	      		  }
			  }, true);
    });
    $( "#check-dialog").dialog({
		minWidth: 600,
		minHeight: 300,
		height: Math.max($(document).height() * 0.5, 300),
		autoOpen: false
	});
    $( "#change-flag-dialog").dialog({
		minWidth: 400,
		minHeight: 160,
		height: 160,
		autoOpen: false
	});
    $( "#test-dialog").dialog({
		minWidth: 1000,
		minHeight: 300,
		height: Math.max($(document).height() * 0.5, 300),
		autoOpen: false
	});
    $( "#check-synonym-dialog").dialog({
		minWidth: 600,
		minHeight: 300,
		height: Math.max($(document).height() * 0.5, 300),
		autoOpen: false
	});
    $('#check').click(function(){
    	$( "#check-dialog").dialog('close');
    	$.getJSON('qaDataFindSimilar.jsp', {
    		q: $('#form input[name=question]').val(),
    		casual: true
    	},
    	function(data){
    		var curId = '<%= StringEscapeUtils.escapeJavaScript(id)%>';
    		for(var i=0; i<data.length; i++){
    			if(data[i].id == curId){
    				data.splice(i, 1);
    				break;
    			}
    		}
    		var list = $('#check-list');
    		list.find('tr').remove();
    		if(data.length == 0){
    			list.append('<tr class="text-center"><td><bean:message key="have.no.data"/></td></tr>');
    		}else{
    			for (var i=0; i < data.length; i++) {
    				var r = data[i];
    				list.append('<tr><td><a href="qaDataEditor2.jsp?id=' + r.id + '" target="_blank">' + r.question + '</a></td></tr>');	
    			}
    		}
    		$( "#check-dialog").dialog('open');
    	});
    });
    
    $('#testLogBtn').click(function(){
    	$( "#test-dialog").dialog('close');
    	$.getJSON('qaEvaluationLog-ajax.jsp', {
    		id: $('input[name=id]').val()
    	},
    	function(data){
    		var flagGlyphs = [
'<span data-flag="1" class="glyphicon glyphicon-question-sign" title="<bean:message key="test.no.select.evaluation"/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
	'<span data-flag="1" class="glyphicon glyphicon-star" title="<bean:message key="test.record.well"/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
	'<span data-flag="2" class="glyphicon glyphicon-thumbs-up" title="<bean:message key="test.record.good"/>" aria-hidden="true"></span>',
	'<span data-flag="3" class="glyphicon glyphicon glyphicon-pencil" title="<bean:message key="test.record.no.answer"/>" aria-hidden="true"></span>',
	'<span data-flag="4" class="glyphicon glyphicon-ok" title="<bean:message key="test.record.no.expect"/>" aria-hidden="true"></span>',
	'<span data-flag="5" class="glyphicon glyphicon-thumbs-down" title="<bean:message key="test.record.bad"/>" aria-hidden="true"></span>',
	'<span data-flag="6" class="glyphicon glyphicon-exclamation-sign" title="<bean:message key="test.record.worst"/>" aria-hidden="true"></span>',
    		                  ];
    		
    		var list = $('#test-list');
    		list.find('tr:has(td)').remove();
    		if(data.length == 0){
    			list.append('<tr class="text-center"><td><bean:message key="have.no.data"/></td></tr>');
    		}else{
    			for (var i=0; i < data.length; i++) {
    				var r = data[i];
    				var tr = $('<tr/>');
    				var cv = JSON.parse(r.conversations);
    				var t = moment( r.timestamp );
    				var formatted = t.format("YYYY/MM/DD HH:mm:ss");

    				$('<td name="logId" data-id="' + r.id + '" data-flag="' + r.flag + '"><h3>' + flagGlyphs[r.flag] + '</h3></td>').appendTo(tr);
    				$('<td name="question">' + r.question + '</td>').appendTo(tr);
    				$('<td name="correctAnswerId"></td>').append(getQuestionLink(r.correctAnswerId)).appendTo(tr);
    				$('<td name="robotAnswerId"></td>').append(getQuestionLink(r.robotAnswerId)).appendTo(tr);
    				$('<td>' + cv.output + '</td>').addClass('answer-text').appendTo(tr);
    				$('<td>' + (cv.hasOwnProperty('confidence') ? cv.confidence : '') + '</td>').appendTo(tr);
    				$('<td>' + formatted + '</td>').appendTo(tr);
    				
    				var $tdOpers = $('<td/>');
    				$('<button name="btnAutoTest" class="btn btn-success"><bean:message key="top.test"/></button>').appendTo($tdOpers);
    				
    				<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
    				$('<button name="btnReviewHide" class="btn btn-danger"><bean:message key="global.hidden"/></button>').appendTo($tdOpers);
    				<% } %>
    				
    				<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) > 0) { %>
    				$('<button name="btnReviewChange" class="btn btn-warning"><bean:message key="global.change"/></button>').appendTo($tdOpers);
    				<% } %>
    				
    				$tdOpers.appendTo(tr);
    				list.append(tr);
    			}
    		}
    		$( "#test-dialog").dialog('open');
    	});
    });
    $(document).on('click', 'button[name="btnReviewHide"]', function() {
    	$thisTr = $(this).parent().parent();
    	$logId = parseInt($(this).parent().siblings('td[name="logId"]').attr("data-id"));
    	
    	if (confirm("<bean:message key='sure.hidden.record'/>")) {
    		$('.loading-panel').show();
    	    $.ajax({
    			url: 'qa-log-status-ajax.jsp',
    			dataType: 'json',
    			data: {
    	  		  id: $logId,
    	  		  status: -1
    			},
    			error: function() {
    				alert('<bean:message key="detection.network"/>');
    			},
    			success: function(resp) {
    				$thisTr.remove();
    			},
    			complete: function() {
    				setTimeout(function() {$('.loading-panel').hide();}, 300);
    			}
    	    });
    	}
    });
    $(document).on('click', 'button[name="btnReviewChange"]', function() {
    	$thisTr = $(this).parent().parent();
    	$logId = parseInt($(this).parent().siblings('td[name="logId"]').attr("data-id"));
		$( "#change-flag-dialog").attr("data-log-id", $logId);
    	$( "#change-flag-dialog").dialog('open');
    });

    $(document).on('click', '#change-flag-dialog span', function() {
  	  var $this = $(this);
  	  var logId = $("#change-flag-dialog").attr("data-log-id");
  	  var flag = parseInt($this.attr("data-flag"));
  	  
  	  var thisData = {};
  	  
  	  $this.siblings().removeClass("text-success");
  	  $this.siblings().removeClass("text-danger");
  	  
  	  switch (flag) {
  	  case 1:
  	  case 2:
  		  $this.addClass("text-success");
  		  var correctAns = prompt("<bean:message key='input.robot.answer.num'/>");
  		  if (!correctAns) {

      		  $this.removeClass("text-success");
  			  return;
  		  }
  		  else if (isNaN(parseInt(correctAns))) {
  			  alert("[" + correctAns + "] <bean:message key='is.not.number'/>");
      		  $this.removeClass("text-success");
  			  return;
  		  }
  		  
  		  thisData["robotAnswer"] = correctAns;
  		  break;
  	  case 3:
  		  $this.addClass("text-success");
  		  if (!confirm("<bean:message key='sure.select.this.option'/>")) {

      		  $this.removeClass("text-success");
  			  return;
  		  }
  		  
  		  break;
  	  case 4:
  	  case 5:
  	  case 6:
  		  $this.addClass("text-danger");
  		  var correctAns = prompt("<bean:message key='input.correct.asnwer'/>");
  		  if (!correctAns) {
      		  $this.removeClass("text-danger");
  			  return;
  		  }
  		  else if (isNaN(parseInt(correctAns))) {
  			  alert("[" + correctAns + "] <bean:message key='is.not.number'/>");
      		  $this.removeClass("text-danger");
  			  return;
  		  }
  		  
  		  thisData["correctAnswer"] = correctAns;
  		  break;
  	  }
  	  $this.parent().attr("data-choosed", "true");
		  thisData["flag"] = flag;
  	  
        $.ajax({
      	  type: 'POST',
      	  url: '<%= request.getContextPath() %>/wiseadm/qa-eval-log-ajax.jsp',
      	  dataType: 'json',
      	  data: {
      		  logId: logId,
      		  data: JSON.stringify(thisData)
      	  },
      	  success: function(resp) {
      		  if (resp.needLogin) {
      			  alert("<bean:message key='top.logout.ex2'/>");
      			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
      		  }
      	  }
        });
    });
    function testCallback(event, btn, stat) {
	    var $thisBtn = btn;
    	var $stat = stat;
    	if (typeof(btn) == 'undefined') {
	    	$thisBtn = $(this);
    	}
    	if (typeof($stat) == 'undefined') {
    		$stat = {good:0, bad:0, unknown:0};
    	}
    	
    	if (event != null) $('.loading-panel').show();
       	
        return (function($insertAfterThisTr, $question, $correctAnswerId, $robotAnswerId) {return $.ajax({
       		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
       		dataType: 'json',
       		async: true,
       		data: {
         		  q: $question,
         		  testMode: true,
          		  html: true
       		},
       		error: function() {
       			alert('<bean:message key="detection.network"/>');
       		},
       		success: function(resp) {
       			var t = moment((new Date()).getTime());
   				var formatted = t.format("YYYY/MM/DD HH:mm:ss");
   				var kid = null;
   				var addClass = "text-primary";
   				if (resp.hasOwnProperty("kid")) {
   					kid = resp.kid;
   				}
   				if ((isNaN($correctAnswerId) && $robotAnswerId == kid) || $correctAnswerId == kid) {
   					addClass = "text-success glyphicon glyphicon-ok-circle";
   					$stat.good++;
   				}
   				else if (isNaN($correctAnswerId) && isNaN($robotAnswerId)){
   					addClass = "text-warning glyphicon glyphicon-question-sign";
   					$stat.unknown++;
   				}
   				else {
   					addClass = "text-danger glyphicon glyphicon-remove-circle";
   					$stat.bad++;
   				}
       			
   				var tr = $('<tr/>');
   				$('<td><h3></h3></td>').appendTo(tr);
   				$('<td><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span></td>').appendTo(tr);
   				$('<td></td>').appendTo(tr);
   				$('<td/>').append($('<h3/>').append(getQuestionLink(kid, addClass))).appendTo(tr);
   				$('<td>' + resp.output + '</td>').addClass('answer-text').appendTo(tr);
   				$('<td>' + (resp.hasOwnProperty('confidence') ? resp.confidence : '') + '</td>').appendTo(tr);
   				$('<td class="text-danger timeago" title="' + formatted + '">' + formatted + '</td>').appendTo(tr);
   				$('<td></td>').appendTo(tr);
   				
   				tr.insertAfter($insertAfterThisTr);
         	},
       		complete: function() {
       	    	if (event != null) setTimeout(function() {$('.loading-panel').hide();}, 300);
   				$(".timeago").timeago();
       		}
        });})
        ($thisBtn.parent().parent(), 
        $thisBtn.parent().siblings('td[name="question"]').text(),
        parseInt($thisBtn.parent().siblings('td[name="correctAnswerId"]').text()),
        parseInt($thisBtn.parent().siblings('td[name="robotAnswerId"]').text())
        );
    }
    $(document).on('click', 'button[name="btnAutoTest"]', testCallback);
    $('button[id="btnAutoTestAll"]').on('click', function() {
    	$('.loading-panel').show();
    	
    	$thisBtn = $(this);
    	$thisBtn.button('loading');
    	var igFlag0 = $( "#checkIgnoreFlag0" ).prop( "checked" );
    	var result = {good:0, bad:0, unknown:0};
    	var btns = $('button[name="btnAutoTest"]');
    	
    	var finishAndPublishScore = function() {
        	$thisBtn.button('reset');
        	
        	$div = $thisBtn.find('div');
        	$div.html('');
        	$h3 = $('<h4/>').appendTo($div);
        	$('<span/>').addClass("text-success glyphicon glyphicon-ok-circle").text(result.good).appendTo($h3);
        	$('<span/>').addClass("text-primary glyphicon glyphicon-question-sign").text(result.unknown).appendTo($h3);
        	$('<span/>').addClass("text-danger glyphicon glyphicon-remove-circle").text(result.bad).appendTo($h3);
        	$('.loading-panel').hide();
    	}
    	
    	var deferredAjax = function(index, max) {
    		$btn = $(btns[index]);
    		var skipTest = false;
    		
    		if (igFlag0) {
               	var flag = parseInt($btn.parent().siblings('td[name="logId"]').attr("data-flag"));
               	if (flag == 0) {
               		skipTest = true;
               	}
    		}
    		
    		if (!skipTest) {
    			$.when(
    		    		(function(ievent,ibtn) {
    			    		return testCallback(ievent, ibtn, result);
    		    		})(null, $btn)
    	    		).then(
    	    			function() {
    	    				if (index < max) {
    	    					deferredAjax(index + 1, max);
    	    				}
    	    				else {
    	    					finishAndPublishScore();
    	    				}
    	    			});
    		}
    		else {
    			if (index < max) {
    				deferredAjax(index + 1, max);
    			}
    			else {
    				finishAndPublishScore();
    			}
    		}
    		
    	};
    	
    	deferredAjax(0, btns.length - 1);
    });
    $('#checkSyn').click(function(){
    	$( "#check-synonym-dialog").dialog('close');
    	$.getJSON('qaDataCheckSynonym.jsp', {
    		qalt: $('textarea[name=question_alt]').val(),
    	},
    	function(data){
    		var list = $('#check-synonym-list');
    		list.find('tr').remove();
    		if(data.matchKeyword.length == 0 && data.notMatchKeyword.length == 0){
    			list.append('<tr class="text-center"><td colspan=2><bean:message key="have.no.data"/></td></tr>');
    		}else{
    			for (var i=0; i < data.matchKeyword.length; i++) {
    				var tr = $('<tr/>');
	    			var row = $('<td/>');
    				var r = data.matchKeyword[i];
    				row.append('<button class="btn btn-primary">' + r.keyword + '</button>');
    				row.appendTo(tr);
    				
    				row = $('<td/>');
    				row.append(JSON.stringify(r.synonyms));
    				row.appendTo(tr);
    				tr.appendTo(list);
    			}
    			for (var i=0; i < data.notMatchKeyword.length; i++) {
    				var tr = $('<tr/>');
	    			var row = $('<td/>');
    				var r = data.notMatchKeyword[i];
    				row.append('<button class="btn btn-danger">' + r.userKeyword + '</button>');
    				row.appendTo(tr);
    				
    				row = $('<td/>');
    				var innerTable = $('<table class="table table-bordered"/>');
    				for (var j=0; j < r.possibleSynonyms.length; j++) {
        				var r2 = r.possibleSynonyms[j];
        				
        				var tr2 = $('<tr/>');
    	    			var row2 = $('<td/>');
        				row2.append('<button class="btn btnDoReplace btn-warning" data-user-keyword="' + r.userKeyword + '">' + r2.keyword + '</button>');
        				row2.appendTo(tr2);
        				
        				row2 = $('<td/>');
        				row2.append(JSON.stringify(r2.synonyms));
        				row2.appendTo(tr2);
        				tr2.appendTo(innerTable);
        			}
    				innerTable.appendTo(row);
    				
    				row.appendTo(tr);
    				tr.appendTo(list);
    			}
    		}
    		$( "#check-synonym-dialog").dialog('open');
    	});
    });
    $("#preview-dialog").dialog({
		autoOpen: false,
		resizable: false
	});
    $(document).on("click", ".btnDoReplace", function() {
    	var $this = $(this);
    	var userKw = $this.attr("data-user-keyword");
    	var newKw = $this.text();

    	if (confirm('確認要將所有[' + userKw + ']替換成[' + newKw + ']?')) {

			var t = $('textarea[name="question_alt"]').val();
			t = t.replace(new RegExp('\\(' + userKw + '\\)', 'gi'), '(' + newKw + ')');
			t = t.replace(new RegExp('\\(' + userKw + '\\|', 'gi'), '(' + newKw + '|');
			t = t.replace(new RegExp('\\|' + userKw + '\\|', 'gi'), '|' + newKw + '|');
			t = t.replace(new RegExp('\\|' + userKw + '\\)', 'gi'), '|' + newKw + ')');
			$('textarea[name="question_alt"]').val(t);
		}
	});
    $('input[name=previewType]').click(function(){
    	var size = $(this).val().split(',');
    	var width = parseInt(size[0]);
    	var height = parseInt(size[1]);
    	$(this).closest('.container')
    		.width(width)
    		.find('iframe')
    		.remove()
    		.end()
    		.append('<iframe name="preview" style="width:' + width + 'px;' 
    			+ 'height:' + height + 'px;'
    			+ '; border:none; overflow:hidden;">');
    	$('input[name=height]').val(height);
    	$('#form').attr({
   			action: 'qaDataPreview.jsp',
   			target: 'preview'
  		}).submit();
    	$("#preview-dialog").dialog('option', 'width', width + 40);
    });
    $('#preivew').click(function(){
    	$('input[name=previewType]:checked').click();
    	$( "#preview-dialog").dialog('open');
    });
    $('.js-loading-bar').modal({
   	  backdrop: 'static',
   	  show: false
   	});
});
</script>
</body>
</html>