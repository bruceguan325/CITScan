<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page isELIgnored ="false" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="com.intumit.solr.robot.entity.*" %>
<%@ page import="com.intumit.solr.robot.intent.*" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getIndexAdminCURD() == 0) { 
	return;
}
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
%>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>展示流程設定</title>
	<jsp:include page="header-qa.jsp"></jsp:include>
	<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
	<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>
	
	<!--// Loading bootstrap again (after jquery-ui) bcz there is a conflicts using button loading state if bootstrap load before jquery-ui -->
	<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
  </head>
  <body>
    <div class='container'>
    <div class="hide">
	<jsp:include page="navbar-qa.jsp"></jsp:include>
	</div>
    <div class='row'>
      <div class='col-md-12'>
        <h1>展示流程 (<%= t.getName() %> <%= t.getNotes() %> (<%= t.getId() %>))</h1>
        <h2><div id="msg"></div></h2>
      </div>
      <div class='col-md-12'>
	      <div class='col-md-4'>
	        <h3>啟用：
	        <input type="checkbox" id="dfEnableToggle" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
	        </h3>
	      </div>
	      <div class='col-md-4'>
	        <h3>覆寫模式：
	        <input type="checkbox" id="dfOverwriteModeToggle" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
	        </h3>
	      </div>
	      <div class='col-md-4'>
	        <h3>自動下一題
	        <input type="checkbox" id="dfAutopilotModeToggle" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
	        </h3>
	      </div>
	      <div class='col-md-4'>
	        <h3>強制送出模式：
	        <input type="checkbox" id="dfClientAutoSendOutModeToggle" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
	        </h3>
	      </div>
	      <div class='col-md-4'>
	        <p><font size='+1'>自我修復</font>：
	        <input type="checkbox" id="dfSelfRepairToggle" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
	        <span class='text-warning'>（暫時拿來判斷是否覆寫一次就關閉，啟用的話會自動關閉覆寫）</span></p>
	      </div>
      </div>
    </div>
    <div class='row' style='display: none;'>
      <div class='col-md-12'>
	      <div class='col-md-6'>
	        <h3>最後問題：<span id='dfLastQuestion'></span></h3>
	      </div>
	      <div class='col-md-6'>
	        <h3>最後回答：<span id='dfLastAnswer'></span></h3>
	      </div>
      </div>
    </div>
    
    <div id="questions">
    </div>
    
    <hr/>

    <div class='row'>
    <form>
    <textarea rows="30" cols="100" name="questions2"></textarea>
    <button id="btnSetQuestions">重設問題</button>
    </form>
    </div>
    </div>
    
    <div class='row question-row-template' style="display: none;">
      <div class='col-md-12' name="question-row">
	      <div class='col-md-1'>
	        <h4><span name="no"></span></h4>
	      </div>
	      <div class='col-md-8'>
	        <h4><button class='btn btn-default btnJumpTo'>跳</button>&nbsp;&nbsp;&nbsp; <span name='question'></span></h4>
	      </div>
	      <div class='col-md-2'>
	      </div>
      </div>
	</div>
    
  <script>
  var currentOffset = -1;
  
  $('#btnSetQuestions').click(function() {
	 $.ajax({
		 url: 'demoFlow-ajax.jsp',
		 data: { action: "setQuestions", questions: $('[name="questions2"]').val().split("\n")},
	 	 success: function(dfObj) {
	 		 $('#dfEnable').text(dfObj.enable);
	 		 $('#dfOverwriteMode').text(dfObj.overwriteMode);
	 		 $('#dfClientAutoSendOutMode').text(dfObj.clientAutoSendOutMode);
	 		 $('#dfAutopilotMode').text(dfObj.autopilotMode);
	 		 $('#dfSelfRepair').text(dfObj.selfRepair);
	 		 $('#dfLastQuestion').text(dfObj.lastQuestion);
	 		 $('#dfLastAnswer').text(dfObj.lastAnswer);
//	 		 $('#questions').text(dfObj.questions);
	 		 
	 		redrawQuestion(dfObj.questions);
	 	 }
	 }); 
	 
	 return false;
  });
  
  $('#dfEnableToggle').change(function() {
	  clearInterval(dfTimer);
      $('#msg').html('Toggle: ' + $(this).prop('checked') + new Date().getTime());
      
      $.ajax({
 		 url: 'demoFlow-ajax.jsp',
 		 data: { action: "setEnable", enable: $(this).prop('checked')},
 	 	 success: function(dfObj) {
 	 		 dfTimer = setInterval(demoFlowStatus, 1000);
 	 	 }
 	 }); 
  });
  
  $('#dfOverwriteModeToggle').change(function() {
	  clearInterval(dfTimer);
      $('#msg').html('Toggle: ' + $(this).prop('checked') + new Date().getTime());
      
      $.ajax({
 		 url: 'demoFlow-ajax.jsp',
 		 data: { action: "setOverwriteMode", enable: $(this).prop('checked')},
 	 	 success: function(dfObj) {
 	 		 dfTimer = setInterval(demoFlowStatus, 1000);
 	 	 }
 	 }); 
  });
  
  $('#dfClientAutoSendOutModeToggle').change(function() {
	  clearInterval(dfTimer);
      $('#msg').html('Toggle: ' + $(this).prop('checked') + new Date().getTime());
      
      $.ajax({
 		 url: 'demoFlow-ajax.jsp',
 		 data: { action: "setClientAutoSendOutMode", enable: $(this).prop('checked')},
 	 	 success: function(dfObj) {
 	 		 dfTimer = setInterval(demoFlowStatus, 1000);
 	 	 }
 	 }); 
  });
  
  $('#dfAutopilotModeToggle').change(function() {
	  clearInterval(dfTimer);
      $('#msg').html('Toggle: ' + $(this).prop('checked') + new Date().getTime());
      
      $.ajax({
 		 url: 'demoFlow-ajax.jsp',
 		 data: { action: "setAutopilotMode", enable: $(this).prop('checked')},
 	 	 success: function(dfObj) {
 	 		 dfTimer = setInterval(demoFlowStatus, 1000);
 	 	 }
 	 }); 
  });
  
  $('#dfSelfRepairToggle').change(function() {
	  clearInterval(dfTimer);
      $('#msg').html('Toggle: ' + $(this).prop('checked') + new Date().getTime());
      
      $.ajax({
 		 url: 'demoFlow-ajax.jsp',
 		 data: { action: "setSelfRepair", enable: $(this).prop('checked')},
 	 	 success: function(dfObj) {
 	 		 dfTimer = setInterval(demoFlowStatus, 1000);
 	 	 }
 	 }); 
  });
  
  var jumpTo = function(targetOffset) {
      $('#msg').html('JumpTo: ' + targetOffset + " //// " + new Date().getTime());
      
      $.ajax({
 		 url: 'demoFlow-ajax.jsp',
 		 data: { action: "jumpTo", offset: targetOffset},
 	 	 success: function(dfObj) {
 	 		 currentOffset = targetOffset;
 	 	 }
 	 }); 
  }
  
  $(document).on('click', '.btnJumpTo', function() {
      $this = $(this);
      offset = $this.parents("div[name='question-row']").attr('questionOffset');
      jumpTo(offset);
  });
  
  function demoFlowStatus(redraw) {
	 $.ajax({
		 url: 'demoFlow-ajax.jsp',
		 data: { action: "status" },
	 	 success: function(dfObj) {
	 		 $('#dfEnable').text(dfObj.enable);
	 		 if (dfObj.enable ^ $("#dfEnableToggle").prop("checked")) {
	 		 	$('#dfEnableToggle').bootstrapToggle(dfObj.enable ? 'on' : 'off');
	 		 }
	 		 
	 		 $('#dfOverwriteMode').text(dfObj.overwriteMode);
	 		 if (dfObj.overwriteMode ^ $("#dfOverwriteModeToggle").prop("checked")) {
	 		 	$('#dfOverwriteModeToggle').bootstrapToggle(dfObj.overwriteMode ? 'on' : 'off');
	 		 }
	 		 
	 		 $('#dfClientAutoSendOutMode').text(dfObj.clientAutoSendOutMode);
	 		 if (dfObj.clientAutoSendOutMode ^ $("#dfClientAutoSendOutModeToggle").prop("checked")) {
	 		 	$('#dfClientAutoSendOutModeToggle').bootstrapToggle(dfObj.clientAutoSendOutMode ? 'on' : 'off');
	 		 }
	 		 
	 		 $('#dfAutopilotMode').text(dfObj.autopilotMode);
	 		 if (dfObj.autopilotMode ^ $("#dfAutopilotModeToggle").prop("checked")) {
	 		 	$('#dfAutopilotModeToggle').bootstrapToggle(dfObj.autopilotMode ? 'on' : 'off');
	 		 }
	 		 
	 		 $('#dfSelfRepair').text(dfObj.selfRepair);
	 		 if (dfObj.selfRepair ^ $("#dfSelfRepairToggle").prop("checked")) {
	 		 	$('#dfSelfRepairToggle').bootstrapToggle(dfObj.selfRepair ? 'on' : 'off');
	 		 }
	 		 
	 		 $('#dfLastQuestion').text(dfObj.lastQuestion);
	 		 $('#dfLastAnswer').text(dfObj.lastAnswer);
	 		 //$('#questions').text(dfObj.questions);
	 		 
	 		 if (dfObj.currentOffset != -1) {
	 			 $('div[questionOffset]').find('.alert-danger').removeClass('alert-danger');
	 			 $('div[questionOffset="' + dfObj.currentOffset + '"]').find('.col-md-8').addClass('alert-danger');
	 		 }
	 		 else {
	 			 $('div[questionOffset]').find('.alert-danger').removeClass('alert-danger');
	 		 }
	 		 
	 		 if (redraw) {
	 			redrawQuestion(dfObj.questions);
	 		 }
	 	 }
	 }); 
  }
  
  function redrawQuestion(questions) {
	$('#questions').html("");
	
	if (!questions) {
		questions = [];
	}
	
	for (var i=0; i < questions.length; i++) {
		q = questions[i];
		
		qr = $(".question-row-template").clone();
		qr.removeClass("question-row-template");

		qr.find("div[name='question-row']").attr("questionOffset", i);
		qr.find("span[name='no']").text(i+1);
		qr.find("span[name='question']").text(q);
		
		$('#questions').append(qr);
		qr.show();
	}
  }
  
  var dfTimer;
  
  $(document).ready(function() {
	  demoFlowStatus(true);
	  dfTimer = setInterval(demoFlowStatus, 1000);
  });
  
  $(document).keydown(function(e) {
	    switch(e.which) {
	        case 37: // left
	        break;

	        case 38: // up
		        jumpTo(--currentOffset);
	        break;

	        case 39: // right
	        break;

	        case 40: // down
	        	jumpTo(++currentOffset);
	        break;

	        default: return; // exit this handler for other keys
	    }
	    e.preventDefault(); // prevent the default action (scroll / move caret)
	});
  </script>
  </body>
</html>