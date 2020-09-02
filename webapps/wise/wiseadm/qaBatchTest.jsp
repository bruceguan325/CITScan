<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="flexjson.*"
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
import="org.json.JSONArray"
import="com.intumit.hithot.HitHotLocale"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A2) == 0) {
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<TITLE><bean:message key='question.answer.automatic.test.page'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>
<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-common.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-build-status.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-ml.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/i18n.js"></script>

<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
.test-stat th span {
	font-size: 2em;
	margin: 0 auto;
}
.test-stat td span {
	font-size: 1.5em;
	margin: 0 auto;

}
.modal-backdrop {
   background-color: red;
}

.test-stat td,th{
	text-align: center;
}

.testScore span {
    margin: 0 5px;
}

.typed-cursor{
    opacity: 1;
    -webkit-animation: blink 0.7s infinite;
    -moz-animation: blink 0.7s infinite;
    animation: blink 0.7s infinite;
}
@keyframes blink{
    0% { opacity:1; }
    50% { opacity:0; }
    100% { opacity:1; }
}
@-webkit-keyframes blink{
    0% { opacity:1; }
    50% { opacity:0; }
    100% { opacity:1; }
}
@-moz-keyframes blink{
    0% { opacity:1; }
    50% { opacity:0; }
    100% { opacity:1; }
}
.flagPanel { border: 1px solid; }
.flagPanel .panel-heading { padding: 5px; }
.flagPanel .panel-body { padding: 5px; }
.flagPanel h3 { margin: 0; }
</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">

<%
String questionsStr = request.getParameter("questions");
List<String> questions = QAUtil.parseMultiValue(questionsStr);
Locale locale = HitHotLocale.determineLocale(request, true, true);
%>

<div class="row" style="margin-top: 10px;">
	<div class="col-md-10">
	<h2><bean:message key='automatic.test.result'/></h2>
	<button id='btnTestAll' data-loading-text="<bean:message key='global.testing'/>" class='btn btn-default' style="margin: 0 10px;"><bean:message key='test.all'/><br><div class='testScore'></div></button>
	<h4><bean:message key='test.time'/>：</h4>
	
	<div id="test-all-progress-bar" class="progress" style="margin-top: 10px;">
	  <div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
	    1
	  </div>
	</div>
	</div>
</div>

<div class="row">
<div class="col-md-12">
	<table id="test-list" class="table table-striped table-bordered table-hover">
	<tr>
		<th></th>
		<th><bean:message key='test.question'/></th>
		<th><bean:message key='test.expected.answer.number'/></th>
		<th><bean:message key='robot.answer.number'/></th>
		<th><bean:message key='robot.answer'/></th>
		<th><bean:message key='global.confidence.level'/></th>
		<th><bean:message key='test.time'/></th>
		<th><bean:message key='operation'/></th>
	</tr>
	<%
	int no = 1;
	for (String question: questions) {
		if (no > 500) {
			out.println("Too many questions (" + questions.size() + ").");
			break;
		}
		
		if (StringUtils.trimToNull(question) == null) continue;
		
		Integer elid = null;
		String correctAnswerId = null;
		
		int numOfTabs = StringUtils.countMatches(question, "\t");
		
		if (numOfTabs >= 2) {
			elid = new Integer(StringUtils.substringBefore(question, "\t"));
			question = StringUtils.substringAfter(question, "\t");
		}
		if (numOfTabs > 0) {
			correctAnswerId = StringUtils.substringBefore(question, "\t");
			question = StringUtils.substringAfter(question, "\t");
		}
	%>
	<tr>
		<td name="logId" data-log-id="<%= elid != null ? elid : -1 %>"><%= no++ %></td>
		<td name="question"><%= question %></td>
		<td name="correctAnswerId"><a href="qaDataEditor.jsp?id=<%= QAUtil.DATATYPE_COMMON_SENSE + "-" + StringUtils.trimToEmpty(correctAnswerId) %>" target="_blank"/><%= StringUtils.trimToEmpty(correctAnswerId) %></a></td>
		<td name="robotAnswerId"><bean:message key='not.test.yet'/></td>
		<td name="robotAnswer"><bean:message key='not.test.yet'/></td>
		<td name="confidence"></td>
		<td name="testTime"></td>
		<td name="operators" class="col-md-1">
			<button name="btnAutoTest" class="btn btn-info" data-toggle="tooltip" data-placement="left" title="<bean:message key="test.btn.tooltip"/>"><span class="glyphicon glyphicon-question-sign"></span></button>
			<button class="btn btn-success" name="btnNlp" data-loading-text="<bean:message key="broken.words"/>" data-toggle="tooltip" data-placement="left" title="<bean:message key="global.broken.tooltip"/>"><span class="glyphicon glyphicon-scissors"></span></button>

    		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
    			<button name="btnSim" class="btn btn-warning" data-target-entry-key="" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.similarQuestionButton.tooltip"/>"><span class="glyphicon glyphicon-screenshot"></span></button>
	    		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) { %>
	    		<button name="btnMagic" class="btn btn-primary" data-target-entry-key="" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip"/>"><span class="glyphicon glyphicon-ok-sign"></span></button>
	    		<% } %>
    		<% } %>
	    	
			<% if (elid != null) { %>
	    		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) > 0) { %>
	    		<button name="btnReviewHide" class="btn btn-warning"><bean:message key="global.hidden"/></button>
	    		<% } %>
	    		<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A4) > 0) { %>
	    		<button name="btnReviewChange" class="btn btn-danger"><bean:message key="global.change"/></button>
	    		<% } %>
    		<% } %>
		</td>
	</tr>
	<%
	}
	%>
	</table>
</div>
</div>
<br>
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

<div class='flag-panel-template panel panel-primary hide'>
  <div class="panel-heading"><bean:message key='batch.test.flag.panel.header'/></div>
  <div class="panel-body">
  <h3>
	<span data-flag="1" class="glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>
	<span data-flag="2" class="glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span>
	<span data-flag="3" class="glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span>
	<span data-flag="4" class="glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span>
	<span data-flag="5" class="glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span>
	<span data-flag="6" class="glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span>
  </h3>
  </div>
</div>

<div id="find-similar-dialog" title="<bean:message key='similar.problems'/>" style="display:none;">
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="check-list" class="table table-striped table-hover">
  	</table>
  </div>
</div>

<div id="magic-dialog" title="<bean:message key='robot.magicBtn.title'/>" style="display:none; z-index:999; ">
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
var flagGlyphs = [
	          		'<span data-flag="0" class="text-warning glyphicon glyphicon-question-sign" title="<bean:message key="test.no.select.evaluation"/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
	          		'<span data-flag="1" class="text-success glyphicon glyphicon-star" title="<bean:message key="test.record.well"/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>',
	          		'<span data-flag="2" class="text-success glyphicon glyphicon-thumbs-up" title="<bean:message key="test.record.good"/>" aria-hidden="true"></span>',
	          		'<span data-flag="3" class="text-warning glyphicon glyphicon glyphicon-pencil" title="<bean:message key="test.record.no.answer"/>" aria-hidden="true"></span>',
	          		'<span data-flag="4" class="text-success glyphicon glyphicon-ok" title="<bean:message key="test.record.no.expect"/>" aria-hidden="true"></span>',
	          		'<span data-flag="5" class="text-danger glyphicon glyphicon-thumbs-down" title="<bean:message key="test.record.bad"/>" aria-hidden="true"></span>',
	          		'<span data-flag="6" class="text-danger glyphicon glyphicon-exclamation-sign" title="<bean:message key="test.record.worst"/>" aria-hidden="true"></span>',
	                  ];

var rows = 100;
var start = 0;
var flag;
var timeRange;
var logStat;
var lastTestResult;
var qaCategoryIdx;

function testCallback(event, btn, stat, callbackWhenFinished) {
	var $thisBtn = $(this);
	var $stat = stat;
	if (typeof(btn) != 'undefined') {
		$thisBtn = btn;
	}
	if (typeof($stat) == 'undefined') {
		$stat = {good:0, bad:0, unknown:0, total:1};
	}
	
   	caId = parseInt($thisBtn.parent().siblings('td[name="correctAnswerId"]').text());
   	message = $thisBtn.parent().siblings('td[name="question"]').text();

    return (function($btn, $correctAnwerId, $question) { 
       	
    	$.ajax({
	   		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
	   		dataType: 'json',
	   		data: {
	     		  q: $question,
	     		  testMode: true,
	     		  ftc: false,
	    		  tid: <%= t.getId() %>,
	      		  html: true
	   		},
	   		error: function() {
	   			alert('<bean:message key="detection.network"/>');
	   		},
	   		success: function(resp) {
	   			var t = moment((new Date()).getTime());
					var formatted = t.format("YYYY/MM/DD HH:mm:ss");
					var kid;
					//var tmp = "";
					var addClass = "text-primary";
					if (resp.hasOwnProperty("kid")) {
						kid = resp.kid.toString();
					}
					if ($correctAnwerId == kid) {
						addClass = "text-success";
						$stat.good++;
					}
					else if (isNaN(caId)){
						addClass = "text-warning";
						$stat.unknown++;
					}
					else {
						addClass = "text-danger";
						$stat.bad++;
						//tmp += "<br>(" + $correctAnwerId + "/" + kid + ")";
					}
	
					$btn.parent().siblings('td[name="robotAnswerId"]').html(getQuestionLink(kid, addClass));

					$logId = $btn.parent().siblings('td[name="logId"]').data('log-id');
					$answerBlock = $btn.parent().siblings('td[name="robotAnswer"]');
					$answerBlock.html("<bean:message key='sentence.be.changed'/>：" + resp.originalQuestion + "<BR>");
					
					$robotAnswerOutput = $("<div name='robotAnswerOutput'/>").appendTo($answerBlock);

			        if (typeof(resp.webRM) != 'undefined') {
			        	for (var i=0; i < resp.webRM.messages.length; i++) {
				        	newWebRM = $('<div class="message-row"><div class="msg-content"></div></div>');
				        	newWebRM.find('.body').html("");
			                newWebRM.appendRichMessage(resp.webRM.messages[i]);
			                newWebRM.appendTo($robotAnswerOutput);
						}
			        }
			        else {
			        	$robotAnswerOutput.html(resp.output)
			        }
					
					$flagPanel = $('div.flag-panel-template').clone().removeClass('flag-panel-template').addClass('flagPanel');
					$flagPanel.data('log-id', $logId);
					$flagPanel.appendTo($answerBlock).removeClass('hide');
			        
	          		$('<span>　</span>').appendTo($answerBlock);
	          		
	          		if (kid) {
	          			var btnWhy = $('<button/>');
		          		btnWhy.attr('data-kid', kid);
		          		btnWhy.attr('data-question', $question);
		          		btnWhy.attr('data-loading-text', "<bean:message key='in.query'/>");
		          		btnWhy.attr('class', 'btn btn-xs btn-danger btnWhy');
		          		btnWhy.html("WHY?");
		          		btnWhy.appendTo($answerBlock);
	          		}
					
					$btn.parent().siblings('td[name="confidence"]').html(
							(resp.hasOwnProperty('confidence') ? resp.confidence : '')
							+ (resp.hasOwnProperty('confidenceFactor') ? ' (' + resp.confidenceFactor + ')' : '')
							);
					$btn.parent().siblings('td[name="testTime"]').html('<span class="text-danger timeago" title="' + formatted + '">' + formatted + '</span>');
	     		},
	   		complete: function() {
	   			var pb = $('#test-all-progress-bar .progress-bar');
	   			var percent = Math.round(($stat.good+$stat.bad+$stat.unknown)*100 / $stat.total) + "%";
	   			pb.css({"width": percent});
	   			pb.html('');
	   			pb.append("<span>" + percent + "(" + ($stat.good+$stat.bad+$stat.unknown) + "/" + $stat.total + ")</span>");
	   			$(".timeago").timeago('locale', '<%= locale.getLanguage() + locale.getCountry()%>');
				$(".timeago").timeago();
				finishAndPublishScore($stat);
				
				if (typeof(callbackWhenFinished) == 'function') {
					callbackWhenFinished($stat);
				}
	   		}
	    });
    })($thisBtn, caId, message);
}

var finishAndPublishScore = function(result) {
	$thisBtn = $('#btnTestAll');

	$div = $thisBtn.find('div');
	$div.html('');
	$h3 = $('<h4/>').appendTo($div);
	$('<span/>').addClass("text-success glyphicon glyphicon-ok-circle").text(result.good).appendTo($h3);
	$('<span/>').addClass("text-primary glyphicon glyphicon-question-sign").text(result.unknown).appendTo($h3);
	$('<span/>').addClass("text-danger glyphicon glyphicon-remove-circle").text(result.bad).appendTo($h3);
	$('.loading-panel').hide();
}

var myConfirm = function(title, msg, okCallback) {
	if (!$('#dataConfirmModal').length) {
		$('body').append('<div id="dataConfirmModal" class="modal fade" tabindex="-1" role="dialog"> <div class="modal-dialog"> <div class="modal-content"> <div class="modal-header"> <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button> <h4 class="modal-title">Modal title</h4> </div> <div class="modal-body"> <p>One fine body&hellip;</p> </div> <div class="modal-footer"> <button type="button" class="btn btn-default" data-dismiss="modal">Close</button> <button type="button" class="btn btn-primary" id="dataConfirmOK" >Ok</button> </div> </div><!-- /.modal-content --> </div><!-- /.modal-dialog --> </div><!-- /.modal -->');
	} 
	
	$('#dataConfirmModal').find('.modal-title').text(title);
	$('#dataConfirmModal').find('.modal-body').html(msg);
	$('#dataConfirmModal').modal({show:true});
	
	var onClick = function() {
		$('#dataConfirmModal').modal('toggle');
		okCallback();
	};
	
	$(document).on("click", '#dataConfirmOK', onClick);
	
	$('#dataConfirmModal').on('hidden.bs.modal', function () {
		$(document).off("click", '#dataConfirmOK', onClick);
	});
	
	return false;
}

$(document).on('click', 'button[name="btnAutoTest"]', testCallback );
$('button[id="btnTestAll"]').on('click', function() {
	$('.loading-panel').show();

	$thisBtn = $(this);
	//$thisBtn.button('loading');
	var btns = $('button[name="btnAutoTest"]');
	var result = {good:0, bad:0, unknown:0, total: btns.length};

	var deferredAjax = function(index, max) {
		$btn = $(btns[index]);

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
    					//finishAndPublishScore();
    				}
    			});

	};

	deferredAjax(0, btns.length - 1);
});

$(document).on('click', 'button[name="btnReviewHide"]', function() {
	$thisTr = $(this).parent().parent();
	$logId = parseInt($(this).parent().siblings('td[name="logId"]').attr("data-log-id"));
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
	$logId = parseInt($(this).parent().siblings('td[name="logId"]').attr("data-log-id"));
	$( "#change-flag-dialog").attr("data-log-id", $logId);
	$( "#change-flag-dialog").dialog('open');
});

var checkSimAndShowPanel = function($question, $q4sim, $entryData) {
   	$( "#find-similar-dialog").dialog('close');
	$.getJSON('qaDataFindSimilar.jsp', {
   		q: (!$q4sim) ? $question : $q4sim
   	},
   	function(data){
   		var list = $('#check-list');
   		list.find('tr').remove();
   		list.append(
   				$('<tr class="text-warning"></tr>')
   				.append('<th><bean:message key="num"/></th><th><bean:message key="original.question"/>： <span id="sim-dialog-question">' + $question + '</span></th>')
   				.append($('<th></th>').append('<form name="newQuestionForm" action="qaDataEditor.jsp" method="post" target="_new"><textarea name="fromQ" class="hide">' + $question + '</textarea><input name="firstAlt" type="hidden"><button type="submit" class="btnCreateNewQ btn btn-danger"><span class="glyphicon glyphicon-plus-sign"></span></button></form>')
   						)
   				);
   		
		list.append('<tr><td colspan=2><input type="text" placeholder="<bean:message key='enter.question.or.kp'/>" name="question" class="form-control"></td>'
				+ '<td><button name="btnSearchAndSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>'
				+ '</td>');
		
		if ($q4sim) {
			list.find('input[name=question]').val($q4sim);
		}
		
   		if (data.length == 0){
   			list.append('<tr class="text-danger"><td></td><td><bean:message key="have.no.data"/></td><td></td></tr>');
   		} else {
   			for (var i=0; i < data.length; i++) {
   				var r = data[i];
   				$tr = $('<tr/>')
	   				.append( $('<td name="sim-kid" />').text(r.kid) )
	   				.append( $('<td name="question" />').data('question', r.question).html( $('<a/>').attr('href', 'qaDataEditor.jsp?id=' + r.id).attr('target', "_blank").text(r.question) ) );
	   			
	   			$buttonsTd = $('<td/>').appendTo($tr);
	   			list.append($tr);
	   			
	   			$btnSimFromSim = $('<button name="btnSimFromSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>').appendTo($buttonsTd);
   				if ($entryData) $btnSimFromSim.data('entryData', $entryData);	   			
	   			
   				<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) { %>
   				$btnMagicFromSim = $('<button name="btnMagicFromSim" class="btn btn-primary" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip" />" />').html('<span class="glyphicon glyphicon-ok-sign"></span>').appendTo($buttonsTd);
   				if ($entryData) $btnMagicFromSim.data('entryData', $entryData);
   				<% } %>
   			}
		    $('[data-toggle="tooltip"]').tooltip();
   		}
   		$( "#find-similar-dialog").dialog('open');
   	});
};

$(document).on('click', 'button[name="btnSearchAndSim"]', function() {
   	$question = $('#sim-dialog-question').text();
   	$q4sim = $(this).closest('tr').find('input[name="question"]').val();
   	$entryData = $(this).data('entryData');
   	if (!$entryData) {
   		$entryData = {originalQuestion: $question, output: "尚未測試"};
   	}
   	checkSimAndShowPanel($question, $q4sim, $entryData);
});

$(document).on('click', 'button[name="btnSimFromSim"]', function() {
   	$question = $('#sim-dialog-question').text();
   	$q4sim = $(this).parent().siblings('td[name="question"]').attr('data-question');
   	$entryData = $(this).data('entryData');
   	if (!$entryData) {
   		$entryData = {originalQuestion: $question, output: "尚未測試"};
   	}
   	
   	checkSimAndShowPanel($question, $q4sim, $entryData);
});

$(document).on('click', 'button[name="btnSim"]', function() {
   	$question = $(this).parent().siblings('td[name="question"]').text();
	$robotAnswerId = $(this).parent().siblings('td[name="robotAnswerId"]').text();
	if (isNaN($robotAnswerId)) $robotAnswerId = 0;
	$robotAnswerOutput = $(this).parent().siblings('td[name="robotAnswer"]').find('div[name="robotAnswerOutput"]').html();
	if (!$robotAnswerOutput)
		$robotAnswerOutput = "尚未測試";
   	checkSimAndShowPanel($question, null, {originalQuestion: $question, kid: $robotAnswerId, output: $robotAnswerOutput});
});


$(document).on('click', '.btnCreateNewQ', function() {
	var $form = $(this).closest('form');
   	$question = $form.find('[name=fromQ]').val();

    $.ajax({
			url: 'qa-nlp-test-ajax.jsp',
			dataType: 'json',
			data: {
	    		q: $question,
			  	replaceSyn: true,
	    		ts: Math.random()
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				$form.find('[name=firstAlt]').val(resp.nlp);
				$form.submit();
			},
	    	complete: function() {
	    	}});
    
	return false;
});

$(document).on('click', 'button[name="btnMagicFromSim"]', function() {
   	$question = $('#sim-dialog-question').text();
	$correctAnswerId = $(this).parent().siblings('td[name="sim-kid"]').text();
	$entryData = $(this).data('entryData');
	
	if (!$entryData) {
		$entryData = { originalQuestion: $question };
	}

	$data = {
		q: $question,
		correctKid: $correctAnswerId,
		entryData: $entryData
	};
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>, language: '<%= locale.getLanguage() + locale.getCountry() %>'});

	$ml.start($data, (function(data, btn) {return function(stat) {
		$entryData["correctAnswer"] = $data.correctKid;
		$entryData["flag"] = 5;
		  
		$.ajax({
        	  type: 'POST',
        	  url: 'qa-eval-log-ajax.jsp',
        	  dataType: 'json',
        	  data: {
        		  data: JSON.stringify($entryData)
        	  },
        	  success: function(resp) {
        		if (resp.needLogin) {
        			alert("<bean:message key='top.logout.ex2'/>");
        		}
        		else {
	  				msg = "";
	  				msg += "成功例句數量：" + stat.good + "<br>";
	  				msg += "失敗例句數量：" + stat.bad + "<br>";
	
	  				$dialog = $("#magic-dialog");
	  				
	  				if (stat.good + stat.bad > 1) {
			    			$("<h3 class='text text-danger'></h3>").appendTo($dialog).html(msg);
	  				}
	  				else {
	  					$("<h3/>").appendTo($dialog);
	  				}
	  				
	  				$("<button class='btn btn-default btnCloseMagicDialog'>Close</button>").appendTo($dialog);
        		}
        	}
        });
	};})($data));
});

$(document).on('click', 'button[name="btnMagic"]', function() {
	$thisBtn = $(this);
	$thisTr = $(this).parent().parent();
   	$question = $(this).parent().siblings('td[name="question"]').text();
	$correctAnswerId = $(this).parent().siblings('td[name="robotAnswerId"]').text();
	if (isNaN($correctAnswerId)) $correctAnswerId = 0;
	$userAnswerId = $(this).parent().siblings('td[name="correctAnswerId"]').text();
	$robotAnswerOutput = $(this).parent().siblings('td[name="robotAnswer"]').find('div[name="robotAnswerOutput"]').html();
	if (!$robotAnswerOutput)
		$robotAnswerOutput = "尚未測試";
	$entryData = {originalQuestion: $question, kid: $correctAnswerId, output: $robotAnswerOutput};

	$data = {
		q: $question,
		correctKid: $userAnswerId.trim() != '' ? $userAnswerId : $correctAnswerId,
		entryData: $entryData
	};
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>, language: '<%= locale.getLanguage() + locale.getCountry() %>'});

	$ml.start($data, (function(data, btn) {return function(stat) {
		$entryData["correctAnswer"] = $data.correctKid;
		$entryData["flag"] = 2;

		$.ajax({
			type: 'POST',
			url: 'qa-eval-log-ajax.jsp',
			dataType: 'json',
			data: {
				data: JSON.stringify($entryData)
			},
			success: function(resp) {
				if (resp.needLogin) {
					alert("<bean:message key='top.logout.ex2'/>");
				}
				else {
					msg = "";
					msg += "成功例句數量：" + stat.good + "<br>";
					msg += "失敗例句數量：" + stat.bad + "<br>";
					
					$dialog = $("#magic-dialog");
					if (stat.good + stat.bad > 1) {
						$("<h3 class='text text-danger'></h3>").appendTo($dialog).html(msg);
					}
					else {
						$("<h3/>").appendTo($dialog);
					}
					
					$("<button class='btn btn-default btnCloseMagicDialog'>Close</button>").appendTo($dialog);
				}
			}
        });
	};})($data, $thisBtn));
});

$(document).on('click', ".btnCloseMagicDialog", function(e) {
	$("#magic-dialog").dialog('close');
});

function saveOrUpdateEvaluationLog(logId, flag, entryData, callback) {
	entryData["flag"] = flag;

	$.ajax({
	  type: 'POST',
	  url: '<%= request.getContextPath() %>/wiseadm/qa-eval-log-ajax.jsp',
	  dataType: 'json',
	  data: {
		  logId: logId,
		  data: JSON.stringify(entryData)
	  },
	  success: function(resp) {
		  if (resp.needLogin) {
			  alert("<bean:message key='top.logout.ex2'/>");
			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
		  }
		  else if (callback) {
			  callback(resp);
		  }
	  }
	});
};

function toEvaluationLog($tr) {
   	$question = $tr.find('td[name="question"]').text();
	$robotAnswerId = $tr.find('td[name="robotAnswerId"]').text();
	if (isNaN($robotAnswerId)) $robotAnswerId = 0;
	$correctAnswerId = $tr.find('td[name="correctAnswerId"]').text();
	if (isNaN($correctAnswerId)) $correctAnswerId = 0;
	$robotAnswerOutput = $tr.find('div[name="robotAnswerOutput"]').html();
	if (!$robotAnswerOutput)
		$robotAnswerOutput = "尚未測試";
	$entryData = {originalQuestion: $question, correctAnswer: $correctAnswerId, kid: $robotAnswerId, output: $robotAnswerOutput};
	
	return $entryData;
}

$(document).on('click', '.flagPanel span', function() {
	  var $this = $(this);
	  var logId = $this.closest('.flagPanel').data("log-id");
	  var flag = parseInt($this.attr("data-flag"));
	  var thisData = toEvaluationLog($this.closest('tr'));
	  thisData.correctAnswer = 0; // 這裡的邏輯是預定的，在批次測的介面調整測試記錄時，「使用者預期答案編號」這個欄位基本被忽略

	  $this.siblings().removeClass("text-success");
	  $this.siblings().removeClass("text-danger");

	  switch (flag) {
	  case 1:
	  case 2:
		  $this.addClass("text-success");
		  var correctAns = thisData.kid;
		  if (correctAns == 0) {
			  correctAns = prompt("<bean:message key='input.robot.answer.num'/>");
			  if (!correctAns) {
		  		  $this.removeClass("text-success");
				  return;
			  }
			  else if (isNaN(parseInt(correctAns))) {
				  alert("[" + correctAns + "] <bean:message key='is.not.number'/>");
	  		  	  $this.removeClass("text-success");
				  return;
			  }
		  }

		  thisData["correctAnswer"] = correctAns;
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
		  var correctAns = thisData.correctAnswer;
		  if (correctAns == 0) {
			  correctAns = prompt("<bean:message key='input.correct.asnwer'/>");
			  if (!correctAns) {
	  		      $this.removeClass("text-danger");
				  return;
			  }
			  else if (isNaN(parseInt(correctAns))) {
				  alert("[" + correctAns + "] <bean:message key='is.not.number'/>");
	  		  	  $this.removeClass("text-danger");
				  return;
			  }
		  }

		  thisData["correctAnswer"] = correctAns;
		  break;
	  }
	  $this.parent().attr("data-choosed", "true");
	  thisData["flag"] = flag;

	  saveOrUpdateEvaluationLog(logId, flag, thisData, function(resp) {
		  if (resp.logId) {
			  $this.closest('.flagPanel').data("log-id", resp.logId);
		  }
	  });
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

	  saveOrUpdateEvaluationLog(logId, flag, thisData);
});

$(document).on('click', "button[name=btnNlp]", function(e) {
	var $thisBtn = $(this);
   	$question = $thisBtn.parent().siblings('td[name="question"]').text();

    $.ajax({
			url: 'qa-nlp-test-ajax.jsp',
			dataType: 'json',
			data: {
    		  q: $question,
    		  ts: Math.random()
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				alert(resp.nlp);
			},
	    	complete: function() {
	    	}});
	return false;
});

$(document).ready(function() {
	$( "#change-flag-dialog").dialog({
		minWidth: 400,
		minHeight: 160,
		height: 160,
		autoOpen: false
	});
	
    $( "#magic-dialog").dialog({
		width: 600,
		minHeight: 300,
		position: ['center', 80],
		autoOpen: false
	});

    $( "#find-similar-dialog").dialog({
		width: 800,
		minHeight: 300,
		position: ['center', 50],
		autoOpen: false
	});
});
</script>

</body>
</html>