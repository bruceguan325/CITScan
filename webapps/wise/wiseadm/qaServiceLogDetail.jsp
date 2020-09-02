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
import="org.json.*"
import="com.intumit.hithot.HitHotLocale"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.*"
import="com.intumit.message.*"
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
static Collection<String> POSSIBLE_FIELDNAMES = Arrays.asList(new String[] {"id", "Name_t", "QUESTION_s", "ANSWER_s", "Title_t", "Date_dt", "TopCategoryName_s", "FirstCategoryName_s", "TopCategory_ms", "Name", "LogTime"});
public String makeUrl(String coreName, HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/" + jsp + "?coreName=" + coreName + (query == null ? "" : ("&" + (query)));
	return url;
}


void lineJsonToOutput(StringBuilder buf, JSONArray msgs, int lv) {

	for (int em=0; em < msgs.length(); em++) {
		try {
			buf.append("<div class='alert " + (lv > 1 ? " alert-info" : "") + "'>");
			JSONObject extraMsg = msgs.getJSONObject(em);
			
			String type = extraMsg.optString("type", "text");
			
			if (StringUtils.equalsIgnoreCase(type, "text")) {
				if (extraMsg.has("thumbnailImageUrl")) {
					buf.append("<div class='thumbnail'>");
					buf.append("<img src='" + extraMsg.getString("thumbnailImageUrl") + "'>");
					buf.append("<br>");
				}
				if (extraMsg.has("title")) {
					buf.append("<h3>");
					buf.append(extraMsg.optString("title", ""));
					buf.append("</h3>");
				}
				buf.append(extraMsg.optString("text", ""));
				buf.append("<br>");
				if (extraMsg.has("thumbnailImageUrl")) {
					buf.append("</div>");
				}
			}
			else if (StringUtils.equalsIgnoreCase(type, "imagemap")) {
				buf.append("<div class='thumbnail'>");
				buf.append("<img src='" + extraMsg.getString("baseUrl") + "/1040'>");
				buf.append("</div>");
			}
			else if (StringUtils.equalsIgnoreCase(type, "template")) {
				buf.append(extraMsg.optString("altText", ""));
				JSONObject tplObj = extraMsg.getJSONObject("template");
				String tplType = tplObj.optString("type", "text");
					
				if ("carousel".equalsIgnoreCase(tplType)) {
					JSONArray carousels = tplObj.optJSONArray("columns");
					lineJsonToOutput(buf, carousels, lv+1);
				} else if ("buttons".equalsIgnoreCase(tplType)) {
					if (tplObj.has("thumbnailImageUrl")) {
						buf.append("<div class='thumbnail'>");
						buf.append("<img src='" + tplObj.getString("thumbnailImageUrl") + "'>");
						buf.append("<br>");
					}
					if (tplObj.has("title")) {
						buf.append("<h3>");
						buf.append(tplObj.optString("title", ""));
						buf.append("</h3>");
					}
					buf.append(tplObj.optString("text", ""));
					buf.append("<br>");
					if (tplObj.has("thumbnailImageUrl")) {
						buf.append("</div>");
					}
				}
			}
			
			if (extraMsg.has("actions")) {
				JSONArray actions = extraMsg.optJSONArray("actions");
				
				for (int aaa=0; aaa < actions.length(); aaa++) {
					JSONObject action = actions.getJSONObject(aaa);
					String at = action.optString("type", "message");
					
					if ("message".equals(at)) {
						buf.append("<button class='form-control'>" + action.optString("label", action.optString("text")) + "</button><br>");
					}
					else if ("postback".equals(at)) {
						buf.append("<button class='form-control'>" + action.optString("label", action.optString("text")) + "</button><br>");
					}
					else if ("uri".equals(at)) {
						buf.append("<a class='btn btn-default form-control' target='_new' href='" + action.optString("uri", action.optString("linkUri")) + "'>" + action.optString("label", action.optString("text", "LINK")) + "</a><br>");
					}
				}
			} else {
				JSONObject tplObj = extraMsg.optJSONObject("template");
				if (tplObj != null && tplObj.has("actions")) {
					JSONArray actions = tplObj.optJSONArray("actions");
					if (actions != null) {
						for (int aaa=0; aaa < actions.length(); aaa++) {
							JSONObject action = actions.getJSONObject(aaa);
							String at = action.optString("type", "message");
							if ("message".equals(at)) {
								buf.append("<button class='form-control'>" + action.optString("label", action.optString("text")) + "</button><br>");
							}
							else if ("postback".equals(at)) {
								buf.append("<button class='form-control'>" + action.optString("label", action.optString("text")) + "</button><br>");
							}
							else if ("uri".equals(at)) {
								buf.append("<a class='btn btn-default form-control' target='_new' href='" + action.optString("uri", action.optString("linkUri")) + "'>" + action.optString("label", action.optString("text", "LINK")) + "</a><br>");
							}
						}
					}
				}
			}
	
			buf.append("</div>");
		}
		catch (Exception jex) {
			jex.printStackTrace();
		}
	}
}
%><%

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
ServiceLogEntity entity = ServiceLogEntity.get(new Integer(request.getParameter("id")));
if (entity.getTenantId() != t.getId()) return;

boolean noActionBtn = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("nab"), "false"));
String jumpTo = request.getParameter("jt");
Locale locale = HitHotLocale.determineLocale(request, true, true);

%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key='session.record.detail'/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<link href="<%= request.getContextPath() %>/wiseadm/line/css/line-preview.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/dataTables.bootstrap.min.js"></script>
<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-common.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-build-status.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-ml.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/i18n.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/line/js/line-preview.js"></script>
<script>
$qaEntryData = {};
$qaDialogDatas = [];
</script>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<h3 id="brqslm-h" class=""><bean:message key='session.record.detail'/> : <%= entity.getUserId() %>
	<% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) > 0) { %>
	<button name="btnReviewHide" data-log-id="<%= entity.getId() %>" class="btn btn-warning"><bean:message key="global.hidden"/></button>
	<% } %>
	</h3>

    <table class="table table-striped table-bordered table-hover" id="mainTable">
        <thead>
			<th></th>
			<th class="col-md-2"><bean:message key='test.question'/></th>
			<th><bean:message key='robot.answer.number'/></th>
			<th><bean:message key='robot.answer'/></th>
			<th><bean:message key='global.time'/></th>
			<th><bean:message key="category"/></th>
			<th><bean:message key='global.label'/></th>
			<th><bean:message key='global.eservice'/></th>
			<th><bean:message key='user.type'/></th>
			<th><bean:message key='global.action'/></th>
        </thead>
        <tbody>
        <%
        JSONObject conversation = new JSONObject(entity.getConversations());

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
	            	String alteredQ = StringEscapeUtils.escapeXml(msg.optString("currentQuestion", "N/A"));
	            	String a = StringUtils.trimToEmpty(msg.optString("output", "N/A"));
            		String enterDt = msg.optString("originalQuestionTime", "");
	            	String kid = msg.optString("kid", "");
	            	String scoreStr = msg.optString("score", null);
	            	String confidence = msg.optString("confidence", MessageUtil.getMessage(locale, "global.no.answer"));
	            	String confidenceFactor = msg.optString("confidenceFactor", null);
	            	String confidenceFactors ="(" + confidenceFactor + ")";
	            	String confidenceClass = " well";
	            	double smartBertScore = msg.optDouble("SmartBertScore", 0);
	            	boolean negativeKw = msg.optBoolean("negativeKeywords", false);
	            	boolean forwarded = false;
	            	String currentQaCategory = msg.optString("currentQaCategory",  "");
	            	String currentCategory = msg.optString("currentCategory",  "");
	            	String eservice = msg.optString("eservice",  "");
	            	String answerType = msg.optString("answerType",  "");
	            	String dialog = msg.optString("DIALOG",  "");
	            	String userType = msg.optString("userType",  "");

	            	String triggeredByEventType_Code = msg.optString("TriggeredByEventType_Code", null);
	            	String triggeredByEventType_Channel = msg.optString("TriggeredByEventType_Channel",  null);
	            	String triggeredByEventType_Data = msg.optString("TriggeredByEventType_Data",  "");
	            	String eventSource = msg.optString("eventSource", null);
	            	String eventSourceType = msg.optString("eventSourceType",  null);
	            	
	            	String entryKey = "" + (entity != null ? entity.getId() : -1) + "#" + i;
	
	            	if (scoreStr != null && StringUtils.isNumeric(scoreStr)) {
	            		float score = Float.parseFloat(scoreStr);
	            		QAUtil.AnswerConfidenceLevel acl = QAUtil.AnswerConfidenceLevel.findConfidenceLevel(score);
	
	            		if (acl == QAUtil.AnswerConfidenceLevel.HIGH) {
	            			confidenceClass = " badge alert-success";
	            		}
	            		else if (acl == QAUtil.AnswerConfidenceLevel.MEDIUM) {
	            			confidenceClass = " badge alert-success";
	            		}
	            		else if (acl == QAUtil.AnswerConfidenceLevel.LOW) {
	            			confidenceClass = " badge alert-warning";
	            		}
	            		else if (acl == QAUtil.AnswerConfidenceLevel.NO_ANSWER) {
	            			confidenceClass = " badge alert-danger";
	            		}
	            	}
	            	else {
	            		if (QAUtil.AnswerConfidenceLevel.HIGH.getName().equals(confidence)) {
	            			confidenceClass = " badge alert-success";
	            		}
	            		else if (QAUtil.AnswerConfidenceLevel.MEDIUM.getName().equals(confidence)) {
	            			confidenceClass = " badge alert-success";
	            		}
	            		else if (QAUtil.AnswerConfidenceLevel.LOW.getName().equals(confidence)) {
	            			confidenceClass = " badge alert-warning";
	            		}
	            		else if (MessageUtil.getMessage(locale,QAUtil.AnswerConfidenceLevel.NO_ANSWER.getName()).equals(confidence)) {
	            			confidenceClass = " badge alert-danger";
	            		}
	            		else {
	            			confidenceClass = " badge alert-default";
	            		}	            		
	            	}
	            %>
	            <tr>
	                <td name="logId" data-entry-key="<%= entryKey %>" data-log-id="<%= entity != null ? entity.getId() : -1 %>"  data-qa-offset="<%= i %>"><%= (i+1) %>
	                <a name="<%= i %>"></a>
	                <% if ("LAST".equalsIgnoreCase(jumpTo) && (i+1) == messages.length()
	                			|| "NK".equalsIgnoreCase(jumpTo) && negativeKw
	                			|| "FW".equalsIgnoreCase(jumpTo) && forwarded
	                			) { %>
	                <a name="JT"></a>
	                <% } else { %>
	                <% } %>
	                </td>
					<td name="question" data-question='<%= question %>'>
						<% if (question != null) { %>
						<%= question %>
						<% } %>
						<span class="easterEgg"><br>Modified Q: (<%= alteredQ %>)</span>
						<% if (triggeredByEventType_Code != null) { %>
						<br>Event: (<%= triggeredByEventType_Channel %>:<%= triggeredByEventType_Code %> <span class="easterEgg">::[<%= triggeredByEventType_Data %>]</span>)
						<% } %>
						<% if (eventSource != null && eventSourceType != null && !"keyin".equalsIgnoreCase(eventSourceType) && !"InputBox".equalsIgnoreCase(eventSource)) { %>
						<span class="easterEgg"><br>Source: (<%= eventSourceType %> :: <%= eventSource %>)</span>
						<% } %>
						<% if((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
						<br><button class='btn btn-default btnTts' data-loading-text='<bean:message key="load.and.play..."/>' data-pronounce='<%=question%>'><span class='glyphicon glyphicon-bullhorn'></span><bean:message key="speak.out"/></button>
						<% } %>
					</td>
					<td name="robotAnswerId">
					  <% if((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
					  <a href="qaDataEditor.jsp?id=<%= QAUtil.DATATYPE_COMMON_SENSE + "-" + StringUtils.trimToEmpty(kid) %>" target="_blank"/><%= StringUtils.trimToEmpty(kid) %></a>
					  <% } else { %>
					     <%= StringUtils.trimToEmpty(kid) %>
					  <% } %>
					  <% if (StringUtils.isNotEmpty(confidence)) { %>
					  <br><div class="badge <%= confidenceClass %> "><span class='glyphicon glyphicon-scale'></span> <bean:message key='global.confidence.level'/> :<%= confidence %><%= smartBertScore > 0 ? ", Bert:" + smartBertScore : "" %><span class="hide">(<%= confidenceFactor == null ? "" : confidenceFactor %>)</span></div>
					  <% } %>
		              <br>
					</td>
					<td name="robotAnswer">
	                	<% if (!msg.has("webRM")) { %>
	                    <%= a %>
	                	<% } %>
	                	
	                	<%
	                	if (msg.has("webRM")) {
	                		// 處理 Web圖文
	                		JSONArray extraMsgs = msg.optJSONObject("webRM").optJSONArray("messages");
	                		%>
	                		<div id="webRM<%= i+1 %>"></div>
	                		<script>
	                		$(document).ready(
		                		function() {
			                		var webRMs<%= i+1 %> = <%= extraMsgs.toString() %>;
			                		for (var i=0; i < webRMs<%= i+1 %>.length; i++) {
						                newWebRM = $(".webRM-message-template-robot").clone();
						                newWebRM.removeClass("webRM-message-template-robot");
						                newWebRM.appendRichMessage(webRMs<%= i+1 %>[i]);
						                newWebRM.appendTo($('#webRM<%= i+1 %>'));
									}
		                		});
	                		</script>
							<%
	                	}
	                	else if (msg.has("line") || msg.has("messages")) {
	                		// 處理通訊軟體額外的內容，先處理 LINE 格式，BOT FRAMEWORK 之後再說
	                		JSONArray extraMsgs = msg.has("line") ? msg.optJSONObject("line").optJSONArray("messages")
	                											  : msg.optJSONArray("messages");
	                		//StringBuilder buf = new StringBuilder();
	                		//lineJsonToOutput(buf, extraMsgs, 1);
	                		//out.println(buf.toString());
	                		%>
	                		<div id="lineRM<%= i+1 %>"></div>
	                		<script>
	                		var lineRMs<%= i+1 %> = <%= extraMsgs.toString() %>;
	                		$(document).ready(
		                		function() {
			                		for (var i=0; i < lineRMs<%= i+1 %>.length; i++) {
						                newWebRM = $(".line-message-template-robot").clone();
						                newWebRM.removeClass("line-message-template-robot");
						                newWebRM.appendLineRichMessage(lineRMs<%= i+1 %>[i]);
						                newWebRM.appendTo($('#lineRM<%= i+1 %>'));
									}
		                		});
	                		</script>
							<%
	                	}
	                	
	                	%>
					</td>
					<td name="enterDt"><%= enterDt %></td>
					<td name="currentQaCategory"><%= currentQaCategory %></td>
					<td name="currentCategory"><%= currentCategory %></td>
					<td name="eservice"><%= eservice %></td>
					<td name="userType"><%= userType %></td>
					<td name="operators" class="col-md-1">
						<% if (StringUtils.isNotEmpty(question)) { %>
						<% if (!noActionBtn) { %>
						<button name="btnAutoTest" class="btn btn-info" data-toggle="tooltip" data-placement="left" title="<bean:message key="test.btn.tooltip"/>"><span class='glyphicon glyphicon-question-sign'></span></button>
						<% } %>
	
					    	<% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
					    		<button name="btnSim" class="btn btn-warning" data-target-entry-key="<%= entryKey %>" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.similarQuestionButton.tooltip"/>"><span class='glyphicon glyphicon-screenshot'></span></button>
					    		<% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) { %>
					    		<button name="btnMagic" class="btn btn-primary" data-target-entry-key="<%= entryKey %>" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip"/>"><span class='glyphicon glyphicon-ok-sign'></span></button>
					    		<% } %>
					    	<% } %>
				    	
						<% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) > 0) { %>
						<button class="btn btn-success" name="btnNlp" data-loading-text="<bean:message key='broken.words'/>" data-toggle="tooltip" data-placement="left" title="<bean:message key='global.broken.tooltip'/>"><span class='glyphicon glyphicon-scissors'></span></button>
						<% } %>
						
					    <% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0 && t.getEnableScenario()) { %>
					    	<button name="btnScenario" class="btn btn-danger" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.dialogButton.tooltip"/>"><span class='glyphicon glyphicon-list-alt'></span></button>
					    	<% } %>
					    	<% } %>
						<script>
						$qaEntryData["<%= entryKey %>"] = <%= msg.toString() %>;
						</script>
					</td>
	            </tr>
            <%
        	
            }
            }
        }
        %>
        </tbody>
    </table>
</div>

<div style="display: none;">
	<div class="webRM-message-template-robot message-row by-robot">
        <div class="msg-content"></div>
    </div>
    
	<div class="line-message-template-robot line-message-row by-robot">
        <div class="msg-content"></div>
    </div>
</div>

<div id="find-similar-dialog" title="<bean:message key='similar.problems'/>" style="display:none;">
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="check-list" class="table table-striped table-hover">
  	</table>
  </div>
</div>

<div class='loading-panel'></div>

<div id="divAudio_Player">
    <audio id="audio_player" src=""></audio>
</div>
<script>
$(".timeago").timeago('locale', '<%= locale.getLanguage() + locale.getCountry()%>');
$(".timeago").timeago();

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

var btn_why = 0;
var $simEntryData;

function testCallback(event, btn, data, stat, callbackWhenFinished) {
	var $thisBtn = $(this);
	var $stat = stat;
	var $data = data;
	if (typeof(btn) != 'undefined') {
		$thisBtn = btn;
	}
	if (typeof($stat) == 'undefined') {
		$stat = {good:0, bad:0, unknown:0, total:1};
	}
	if (typeof($data) == 'undefined') {
		$data = {
			q: $thisBtn.parent().siblings('td[name="question"]').attr('data-question'),
			correctKid: parseInt($thisBtn.parent().siblings('td[name="robotAnswerId"]').text())
		};
	}

   	return (function($btn, data) {
    	$.ajax({
	   		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
	   		dataType: 'json',
	   		data: {
	     		  q: data.q,
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
					if (!data.hasOwnProperty("correctKid") || isNaN(data.correctKid)){
						addClass = "text-warning";
						$stat.unknown++;
					}
					else if (data.correctKid == kid) {
						addClass = "text-success";
						$stat.good++;
					}
					else {
						addClass = "text-danger";
						$stat.bad++;
						//tmp += "<br>(" + $correctAnwerId + "/" + kid + ")";
					}

					//$btn.parent().siblings('td[name="robotAnswerId"]').html(getQuestionLink(kid, addClass));
					//$btn.parent().siblings('td[name="robotAnswerId"]').append(tmp);

					$answerBlock = $('<td> </td>');
					$answerBlock.html(resp.output);

	          		$('<span>　</span>').appendTo($answerBlock);

	          		if (kid) {
	          			var btnWhy = $('<button/>');
	          			btnWhy.attr('id', kid+''+btn_why);
		          		btnWhy.attr('data-kid', kid);
		          		btnWhy.attr('data-question', data.q);
		          		btnWhy.attr('data-loading-text', "<bean:message key='in.query'/>");
		          		btnWhy.attr('class', 'btn btn-xs btn-danger btnWhy');
		          		btnWhy.html("WHY?");
		          		btnWhy.appendTo($answerBlock);
	          		}

					/*$btn.parent().siblings('td[name="confidence"]').html(
							(resp.hasOwnProperty('confidence') ? resp.confidence : '')
							+ (resp.hasOwnProperty('confidenceFactor') ? ' (' + resp.confidenceFactor + ')' : '')
							);
					$btn.parent().siblings('td[name="testTime"]').html('<span class="text-danger timeago" title="' + formatted + '">' + formatted + '</span>');*/

	   				var tr = $('<tr/>');
	   				$('<td><h3></h3></td>').appendTo(tr);
	   				$('<td><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span><span class="glyphicon glyphicon-arrow-up" aria-hidden="true"></span></td>').appendTo(tr);
	   				$('<td/>').append($('<h3/>').append(getQuestionLink(kid, addClass))).appendTo(tr);
	   				$answerBlock.addClass('answer-text').appendTo(tr);
	   				$('<td>' + (resp.hasOwnProperty('confidence') ? resp.confidence : '') + '</td>').appendTo(tr);
	   				$('<td class="text-danger timeago" title="' + formatted + '">' + formatted + '</td>').appendTo(tr);
	   				$('<td></td>').appendTo(tr);
	   				$('<td></td>').appendTo(tr);
	   				$('<td colspan=3></td>').appendTo(tr);

	   				tr.insertAfter($btn.parent().parent());
					if (kid) {
						//document.getElementById(kid+''+btn_why).click(); // 「學」完自動 why 一次，這個先關掉
						btn_why = btn_why + 1;
					}
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
    })($thisBtn, $data);
}

var finishAndPublishScore = function(result) {
	$thisBtn = $('#btnTestAll');
	$thisBtn.button('reset');

	$div = $thisBtn.find('div');
	$div.html('');
	$h3 = $('<h4/>').appendTo($div);
	$('<span/>').addClass("text-success glyphicon glyphicon-ok-circle").text(result.good).appendTo($h3);
	$('<span/>').addClass("text-primary glyphicon glyphicon-question-sign").text(result.unknown).appendTo($h3);
	$('<span/>').addClass("text-danger glyphicon glyphicon-remove-circle").text(result.bad).appendTo($h3);
	$('.loading-panel').hide();
};

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
		    		return testCallback(ievent, ibtn, null, result);
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
	$logId = parseInt($(this).attr("data-log-id"));
	if (confirm("<bean:message key='sure.hidden.record'/>")) {
		$('.loading-panel').show();
	    $.ajax({
			url: 'qa-service-log-review-status-ajax.jsp',
			dataType: 'json',
			data: {
	  		  id: $logId,
	  		  reviewStatus: -1
			},
			error: function() {
				alert('<bean:message key="detection.network"/>');
			},
			success: function(resp) {
				window.location="<%= request.getContextPath() %>/wiseadm/qaServiceLog.jsp";
			},
			complete: function() {
				setTimeout(function() {$('.loading-panel').hide();}, 300);
			}
	    });
	}
});


$(document).on('click', 'button[name="btnScenario"]', function() {
	$thisBtn = $(this);
	$thisTr = $(this).parent().parent();
   	$question = $(this).parent().siblings('td[name="question"]').attr('data-question');
   	$confidence = $(this).parent().siblings('td[name="confidence"]').text();
	$correctAnswerId = $(this).parent().siblings('td[name="robotAnswerId"]').text();
   	$qaDialogDataOffset = $(this).parent().siblings('td[name="logId"]').attr("data-qa-offset");
   	window.open('qaDialogML.jsp?id=<%= entity.getId() %>&offset=' + $qaDialogDataOffset);
});

var checkSimAndShowPanel = function($question, $q4sim) {
   	$( "#find-similar-dialog").dialog('close');
	$.getJSON('qaDataFindSimilar.jsp', {
   		q: typeof($q4sim) == 'undefined' ? $question : $q4sim
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
		
		if (typeof($q4sim) != 'undefined') {
			list.find('input[name=question]').val($q4sim);
		}
		
   		if(data.length == 0){
   			list.append('<tr class="text-danger"><td></td><td><bean:message key="have.no.data"/></td><td></td></tr>');
   		}else{
   			for (var i=0; i < data.length; i++) {
   				var r = data[i];
   				list.append('<tr><td name="sim-kid">' + r.kid + '</td><td name="question" data-question="' + r.question + '"><a href="qaDataEditor.jsp?id=' + r.id + '" target="_blank">' 
   						+ r.question 
   						+ '</a></td>'
   						+ '<td><button name="btnSimFromSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>'
   						+ '<%if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) {%><button name="btnMagicFromSim" class="btn btn-primary" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip"/>"><span class="glyphicon glyphicon-ok-sign"></span></button><% } %></td></tr>');
   			}
		    $('[data-toggle="tooltip"]').tooltip();
   		}
   		$( "#find-similar-dialog").dialog('open');
   	});
};

$(document).on('click', 'button[name="btnSearchAndSim"]', function() {
   	$question = $('#sim-dialog-question').text();
   	$q4sim = $(this).closest('tr').find('input[name="question"]').val();
   	checkSimAndShowPanel($question, $q4sim);
});

$(document).on('click', 'button[name="btnSimFromSim"]', function() {
   	$question = $('#sim-dialog-question').text();
	$simEntryData = $qaEntryData[$(this).attr('data-target-entry-key')];
   	$q4sim = $(this).parent().siblings('td[name="question"]').attr('data-question');
   	checkSimAndShowPanel($question, $q4sim);
});

$(document).on('click', 'button[name="btnSim"]', function() {
	$simEntryData = $qaEntryData[$(this).attr('data-target-entry-key')];
   	$question = $(this).parent().siblings('td[name="question"]').attr('data-question');
   	checkSimAndShowPanel($question);
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

	$data = {
		q: $question,
		correctKid: $correctAnswerId,
		entryData: $simEntryData
	};
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>, language: '<%=locale.getLanguage() + locale.getCountry()%>'});

	$ml.start($data, (function(data, btn) {return function(stat) {
		$simEntryData["correctAnswer"] = $data.correctKid;
		$simEntryData["flag"] = 5;
		  
		$.ajax({
        	  type: 'POST',
        	  url: 'qa-eval-log-ajax.jsp',
        	  dataType: 'json',
        	  data: {
        		  data: JSON.stringify($simEntryData)
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
	$entryData = $qaEntryData[$(this).attr('data-target-entry-key')];
   	$question = $(this).parent().siblings('td[name="question"]').attr('data-question');
	$correctAnswerId = $(this).parent().siblings('td[name="robotAnswerId"]').text().trim();

	$data = {
		q: $question,
		correctKid: $correctAnswerId,
		entryData: $entryData
	};
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>, language: '<%=locale.getLanguage() + locale.getCountry()%>'});

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

var systemNlp = '';

$(document).on('click', ".btnCloseMagicDialog", function(e) {
	$("#magic-dialog").dialog('close');
});

$(document).on('click', "button[name=btnNlp]", function(e) {
	var $thisBtn = $(this);
   	$question = $thisBtn.parent().siblings('td[name="question"]').attr('data-question');

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
				myConfirm("<bean:message key='global.broken.result'/>", resp.nlp, function() {});
				systemNlp = resp.nlp;
			},
	    	complete: function() {
	    	}});
	return false;
});

$(document).on('click', '.btnTts', function() {
	  $(this).button('loading');
	  var pronounce = $(this).attr('data-pronounce');
	  var ts = new Date().getTime();
	  
	  $.get('<%= request.getContextPath() %>/tts?pronounce=' + pronounce + '&ts=' + ts, function(data){
		  
		  var audio = new Audio();
          audio.src = '<%= request.getContextPath() %>/commons/' + data + '.wav';
          audio.load();
          audio.play();
          
          audio.addEventListener('ended', function() {
	    	  $('.btnTts').button('reset');
	      });
	  });
});

$(document).ready(function() {
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
    
    $('[data-toggle="tooltip"]').tooltip();
});


$('#mainTable').DataTable({ "paging": false, "dom": 'fiplrtif' });
</script>
</BODY>
</HTML>
