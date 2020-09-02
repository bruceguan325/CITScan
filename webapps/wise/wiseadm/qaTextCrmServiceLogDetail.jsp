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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.dictionary.*"
import="com.intumit.solr.robot.*"
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
					else if ("pushback".equals(at)) {
						buf.append("<button class='form-control'>" + action.optString("label", action.optString("text")) + "</button><br>");
					}
					else if ("uri".equals(at)) {
						buf.append("<a class='btn btn-default form-control' target='_new' href='" + action.optString("uri", action.optString("linkUri")) + "'>" + action.optString("label", action.optString("text", "LINK")) + "</a><br>");
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
TextCrmServiceLogEntity entity = TextCrmServiceLogEntity.get(new Integer(request.getParameter("id")));
if (entity.getTenantId() != t.getId()) return;

boolean noActionBtn = Boolean.parseBoolean(StringUtils.defaultString(request.getParameter("nab"), "false"));
String jumpTo = request.getParameter("jt");

%>
<!DOCTYPE html>
<HTML>
<HEAD>
<TITLE><bean:message key='session.record.detail'/></TITLE>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<link href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.4/css/select2.min.css" rel="stylesheet" />
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/dataTables.bootstrap.min.js"></script>
<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-common.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-build-status.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-ml.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.4/js/select2.min.js"></script>
<script>
$qaEntryData = {};
$qaDialogDatas = [];
</script>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<form method="post">
<input type="hidden" name="id" value="<%= entity.getId() %>">
<div class="container">
	<h3 id="brqslm-h" class=""><bean:message key='session.record.detail'/> : <%= entity.getQaId() %>
	<% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) > 0) { %>
	<button name="btnReviewHide" data-log-id="<%= entity.getId() %>" class="btn btn-warning"><bean:message key="global.hidden"/></button>
	<% } %>
	&nbsp;
	<button name="btnSave" data-log-id="<%= entity.getId() %>" class="btn btn-danger"><bean:message key="save"/></button>
	</h3>

    <table class="table table-striped table-bordered table-hover" id="mainTable">
        <thead>
			<th></th>
			<th class="col-md-1">From</th>
			<th>Message</th>
			<th><bean:message key='robot.answer.number'/></th>
			<th><bean:message key='global.confidence.level'/></th>
			<th><bean:message key='test.time'/></th>
			<th>知識主題</th>
			<th><bean:message key='global.action'/></th>
        </thead>
        <tbody>
        <%
        String conversations = entity.getConversations();
		List<QAAltTemplate> qaAltTemplates = QAAltTemplate.list(t.getId());
		request.setAttribute("qaAltTemplates", qaAltTemplates);
		
		List<DictionaryDatabase> dicts = DictionaryDatabase.listByTenantId(t.getId());
		List<DictionaryDatabase> allKps = new ArrayList<DictionaryDatabase>();
		for (DictionaryDatabase dd: dicts) {
			if (dd.getPurposeSet().contains(DictionaryDatabase.Purpose.KNOWLEDGE_POINT)) {
				allKps.add(dd);
			}
		}
		request.setAttribute("allKps", allKps);

        if (conversations != null) {
            JSONArray messages = new JSONArray(conversations);
        		boolean dirty = false;

            for (int i=0; i < messages.length(); i++) {
	            	Object obj = messages.get(i);
	            	
	            	if (obj instanceof JSONObject) {
	            		JSONObject msg = (JSONObject)obj;
	            		System.out.println(msg.toString(2));
	            		
		            	String identity = msg.has("identity") ? StringEscapeUtils.escapeXml(msg.getString("identity")) : "N/A";
		            	String q = msg.has("message") ? StringUtils.trimToEmpty(msg.getString("message")) : "N/A";
		            	String entryKey = "" + (entity != null ? entity.getId() : -1) + "#" + i;
		            	
		            	DictionaryDatabase[] kps = KnowledgePointDictionary.search(t.getId(), q.toCharArray(), null);
		            	String dt = msg.has("timestamp") ? msg.getString("timestamp") : "";
		            	String kid = msg.has("kid") ? msg.getString("kid") : "";
	            		boolean isBot = TextCrmServiceLogEntity.isBotName(identity);
	            		
	            		// 分析師可以操作的欄位
		            int evaluationLogId = msg.optInt("evaluationLogId", 0);
		            	String messageType = msg.optString("messageType");
		            	String choosedQaAltTemplateMkey = msg.optString("qaAtlTplMkey");
		            	String choosedKeywords = msg.optString("choosedKeywords");
		            	String suggestQuestion = msg.optString("suggestQuestion");
		            	String tplMkeyReqParamKey = "choosedQaTemplate-" + i;
		            	String tplKeywordsReqParamKey = "choosedKeywords-" + i;
		            	String preselectedQaTemplateMkey = messageType;
		            	JSONArray preselectedQaTemplateKeywords = null;
		            	boolean dirtyThisMessage = false;
		            	
		            String tmp = request.getParameter(tplMkeyReqParamKey);
		            if (tmp != null) {
			            	dirty = dirtyThisMessage = true;	
		            		if ("_NONE".equals(tmp)) {
			            		messageType = tmp;
			            		choosedQaAltTemplateMkey = null;
		            		}
		            		else if ("_IGNORE".equals(tmp)) {
			            		messageType = tmp;
			            		choosedQaAltTemplateMkey = null;
		            		}
		            		else if ("_CONTEXT_OR_NEW_QUESTION".equals(tmp)) {
			            		messageType = tmp;
			            		choosedQaAltTemplateMkey = null;
		            		}
		            		else {
		            			messageType = "_QA_ALT_MKEY";
		            			choosedQaAltTemplateMkey = tmp;
		            		}
		            		preselectedQaTemplateMkey = tmp;
		            		
		            		msg.put("messageType", messageType);
		            		if (choosedQaAltTemplateMkey == null) {
		            			msg.remove("qaAtlTplMkey");
		            		}
		            		else {
		            			msg.put("qaAtlTplMkey", choosedQaAltTemplateMkey);
		            		}
		            }
		            else {
		            		// 如果本次沒有選擇，JSON 內的資料要判斷如果 messageType == _QA_ALT_MKEY 就應該是 value = choosedQaAltTemplateMkey
			            	if ("_QA_ALT_MKEY".equals(messageType)) {
			            		preselectedQaTemplateMkey = choosedQaAltTemplateMkey;
		            		}
		            		if (StringUtils.trimToNull(preselectedQaTemplateMkey) == null) {
		            			preselectedQaTemplateMkey = "_NONE";
		            		}
		            }
		            
		            String tmps[] = request.getParameterValues(tplKeywordsReqParamKey);
		            if (tmps != null) {
		            		dirty = dirtyThisMessage = true;
		            		preselectedQaTemplateKeywords = new JSONArray(tmps);
			            
			            if ("_CONTEXT_OR_NEW_QUESTION".equals(messageType)) {
			            		choosedKeywords = null;
			            		suggestQuestion = tmps.length > 0 ? tmps[0] : "";
		            		}
		            		else if ("_QA_ALT_MKEY".equals(messageType)) {
			            		choosedKeywords = StringUtils.join(tmps, ",");
			            		suggestQuestion = null;
		            		}
		            		else {
		            			choosedKeywords = null;
			            		suggestQuestion = null;
		            		}
			            
		            		if (choosedKeywords == null) {
		            			msg.remove("choosedKeywords");
		            		}
		            		else {
		            			msg.put("choosedKeywords", choosedKeywords);
		            		}
			            
		            		if (suggestQuestion == null) {
		            			msg.remove("suggestQuestion");
		            		}
		            		else {
		            			msg.put("suggestQuestion", suggestQuestion);
		            		}
		            }
		            else {
			            if ("_CONTEXT_OR_NEW_QUESTION".equals(messageType)) {
			            		preselectedQaTemplateKeywords = new JSONArray();
			            		preselectedQaTemplateKeywords.put(suggestQuestion);
		            		}
		            		else if ("_QA_ALT_MKEY".equals(messageType)) {
		            			preselectedQaTemplateKeywords = new JSONArray(StringUtils.split(choosedKeywords, ","));
		            		}
		            }
		            
		            if (dirtyThisMessage) {
		            		boolean shouldHaveEvalLog = false;
		            		String questionForEvalLog = null;
		            		
			            if ("_CONTEXT_OR_NEW_QUESTION".equals(messageType)) {
				            	shouldHaveEvalLog = true;
				            	questionForEvalLog = suggestQuestion;
			            }
		            		else if ("_QA_ALT_MKEY".equals(messageType)) {
				            	shouldHaveEvalLog = true;
		            			questionForEvalLog = q;
		            		}
			            
			            if (shouldHaveEvalLog) {
				            	EvaluationLogEntity elog = null;
			            		if (evaluationLogId == 0) {
			            			elog = EvaluationLogEntity.createEvaluationLogAndSave(request, 3, null, questionForEvalLog, "N/A", null);
			            			msg.put("evaluationLogId", elog.getId());
			            		}
			            		else {
			            			elog = EvaluationLogEntity.get(evaluationLogId);
			            			
			            			// 也許應該判斷 flag != 3 的話就不再給改動了才對
			            			elog.setQuestion(questionForEvalLog);
			            		}
			            		
			            		elog.setLogSource("TEXT_CRM");
			            		elog.setSourceRef(entryKey);
			            		
			            		if ("_QA_ALT_MKEY".equals(messageType)) {
				            		elog.setSuggestQaTemplateMkey(choosedQaAltTemplateMkey);
				            		elog.setSuggestQaTemplateKeywords(choosedKeywords);
			            		}
			            		
			            		EvaluationLogEntity.update(elog);
			            }
		            }
		
	            		// 以下這些都是原先 qaServiceLogEntity 會存的，但這裡應該是沒有這些
		            	boolean negativeKw = msg.optBoolean("negativeKeywords", false);
	            		String enterDt = msg.has("originalQuestionTime") ? msg.getString("originalQuestionTime") : "";
		            	String scoreStr = msg.has("score") ? msg.getString("score") : null;
		            	String confidence = msg.has("confidence") ? msg.getString("confidence") : null;
		            	String confidenceFactor = msg.has("confidenceFactor") ? msg.getString("confidenceFactor") : null;
		            	String confidenceClass = " well";
		            	String currentQaCategory = msg.has("currentQaCategory") ? msg.getString("currentQaCategory") : "";
		            	String currentCategory = msg.has("currentCategory") ? msg.getString("currentCategory") : "";
		            	String currentQaUse = msg.has("currentQaUse") ? msg.getString("currentQaUse") : "";
		            	String eservice = msg.has("eservice") ? msg.getString("eservice") : "";
		            	String answerType = msg.has("answerType") ? msg.getString("answerType") : "";
		            	String dialog = msg.has("DIALOG") ? msg.getString("DIALOG") : "";
		            	String userType = msg.has("userType") ? msg.getString("userType") : "";
		            	
		            %>
		            <tr>
		                <td name="logId" data-entry-key="<%= entryKey %>" data-log-id="<%= entity != null ? entity.getId() : -1 %>"  data-qa-offset="<%= i %>"><%= (i+1) %>
		                <a name="<%= i %>"></a>
		                <% if ("LAST".equalsIgnoreCase(jumpTo) && (i+1) == messages.length()
		                			|| "NK".equalsIgnoreCase(jumpTo) && negativeKw
		                			) { %>
		                <a name="JT"></a>
		                <% } else { %>
		                <% } %>
		                </td>
						<td name="question" data-question='<%= q %>'><%= identity %></td>
						<td name="robotAnswer">
							<div>
						    	<div class="msj<%= isBot ? "-rta" : "" %> macro">
						    <div class="chat-text chat-text-<%= isBot ? "r" : "l" %>"><p><%= q %></p><p><small title="<%= dt %>" class="timeago"><%= dt %></small></p></div>
						    	</div>
							</div>
		                	<%
		                	if (!isBot) {
							%>
		                    	<select name='<%= tplMkeyReqParamKey %>' style='width: 40%;'>
		                    	<optgroup label="其他">
		                			<option value="_NONE">-- 請選擇 --</option>
		                			<option value="_IGNORE">生活聊天或者不需處理的對話</option>
		                			<option value="_CONTEXT_OR_NEW_QUESTION">非獨立問句或不存在範本</option>
		                    	</optgroup>
		                	    	<optgroup label="範本">
		                		    	<c:forEach var="et" items="${ qaAltTemplates }">
		                				<option value="${ et.mkey }">${ et.name } (${ et.mkey })</option>
		                		    	</c:forEach>
		                	    	</optgroup>
		                    	</select>
		                    	<select name='<%= tplKeywordsReqParamKey %>' style='width: 50%;' multiple="multiple">
		                		    	<c:forEach var="et" items="${ allKps }">
		                				<option value="${ et.keyword }">${ et.keyword } (${ et.category })</option>
		                		    	</c:forEach>
		                    	</select>
		                    	<%
		                	}
		                	%>
						<script>
						$(document).ready(function() {
							$("[name=<%= tplMkeyReqParamKey %>]").select2();
							$("[name=<%= tplKeywordsReqParamKey %>]").select2( { tags:true }); // tags:true 讓我們可以增加不在清單內的 keywords
							
							// 為了讓 select2 不會改動我們加入 keyword 的順序，所以需要下列 code
							$("[name^=choosedKeywords]").on("select2:select", function (evt) {
								  var element = evt.params.data.element;
								  var $element = $(element);
								  
								  $element.detach();
								  $(this).append($element);
								  $(this).trigger("change");
							});
							
							<% if (preselectedQaTemplateMkey != null) { %>
								$("[name=<%= tplMkeyReqParamKey %>]").val('<%= preselectedQaTemplateMkey %>').trigger('change');
							<% } %>
							
							<% if (preselectedQaTemplateKeywords != null) { %>
								arr = <%= preselectedQaTemplateKeywords.toString() %>;
								for (var kwIdx in arr) {
									<%-- 要自己判讀哪些選項不在 option 當中，沒有的要加上去，不然後面 .val() 會沒用 --%>
									var kw = arr[kwIdx];
									if ($("[name=<%= tplKeywordsReqParamKey %>]").find("option[value='" + kw + "']").length<=0) {
								          $("[name=<%= tplKeywordsReqParamKey %>]").append(new Option(kw, kw));
								    }
								}
								$("[name=<%= tplKeywordsReqParamKey %>]").val(<%= preselectedQaTemplateKeywords.toString() %>).trigger('change');
							<% } %>
							
						});
						</script>
						</td>
						<td name="robotAswerId"><a href="qaDataEditor.jsp?id=<%= QAUtil.DATATYPE_COMMON_SENSE + "-" + StringUtils.trimToEmpty(kid) %>" target="_blank"/><%= StringUtils.trimToEmpty(kid) %></a></td>
						<td name="confidence"><%= StringUtils.defaultString(confidence, QAUtil.AnswerConfidenceLevel.NO_ANSWER.getName()) %></td>
						<td name="datetime"><%= dt %></td>
						<td name="currentCategory">
							<% 
							if (kps != null && kps.length > 0) {
								for (DictionaryDatabase kp: kps) {
									%><%= kp.getKeyword() %>&nbsp;<%
								}
							}
							%>
						</td>
						<td name="operators" class="col-md-1">
							<div class="dropdown">
							  <button class="btn btn-primary dropdown-toggle" type="button" data-toggle="dropdown">操作<span class="caret"></span></button>
							  <ul class="dropdown-menu">
								<% if (!noActionBtn) { %>
							    		<li><button name="btnAutoTest" class="btn btn-info" data-toggle="tooltip" data-placement="left" title="<bean:message key="test.btn.tooltip"/>"><span class='glyphicon glyphicon-question-sign'></span><bean:message key="test.btn.tooltip"/></button></li>
								<% } %>
							    	<% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
							    		<li><button name="btnSim" class="btn btn-warning" data-target-entry-key="<%= entryKey %>" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.similarQuestionButton.tooltip"/>"><span class='glyphicon glyphicon-screenshot'></span><bean:message key="robot.similarQuestionButton.tooltip"/></button></li>
							    		<% if (!noActionBtn && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) { %>
							    		<li><button name="btnMagic" class="btn btn-primary" data-target-entry-key="<%= entryKey %>" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip"/>"><span class='glyphicon glyphicon-ok-sign'></span><bean:message key="robot.magicButton.tooltip"/></button></li>
							    		<% } %>
							    	<% } %>
								<% if (!noActionBtn) { %>
									<li><button class="btn btn-success" name="btnNlp" data-loading-text="<bean:message key='broken.words'/>" data-toggle="tooltip" data-placement="left" title="<bean:message key='global.broken.tooltip'/>"><span class='glyphicon glyphicon-scissors'></span><bean:message key='global.broken.tooltip'/></button></li>
								<% } %>
							    <li></li>
							    <li></li>
							  </ul>
							</div>
							<script>
							$qaEntryData["<%= entryKey %>"] = <%= msg.toString() %>;
							</script>
						</td>
		            </tr>
	            <%
	        	
	            }
            }
            
            if (dirty) {
	            	entity.setConversations(messages.toString(2));
	            	TextCrmServiceLogEntity.save(entity);
            }
        }
        %>
        </tbody>
    </table>
</div>
</form>

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
$(document).ready(function() {
	$(".timeago").timeago();
});

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
			q: $thisBtn.closest('td').siblings('td[name="question"]').attr('data-question'),
			correctKid: parseInt($thisBtn.closest('td').siblings('td[name="robotAnswerId"]').text())
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

					$answerBlock = $('<div><div class="msj-rta macro"><div class="chat-text chat-text-r"> ' + resp.output + ' </div></div></div>');

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
	   				$answerBlock.appendTo(tr);
	   				$('<td/>').append($('<h3/>').append(getQuestionLink(kid, addClass))).appendTo(tr);
	   				$('<td>' + (resp.hasOwnProperty('confidence') ? resp.confidence : '') + '</td>').appendTo(tr);
	   				$('<td class="text-danger timeago" title="' + formatted + '">' + formatted + '</td>').appendTo(tr);
	   				$('<td></td>').appendTo(tr);
	   				$('<td></td>').appendTo(tr);

	   				tr.insertAfter($btn.closest('tr'));
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
	if (false) {//confirm("<bean:message key='sure.hidden.record'/>")) {
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
				window.location="<%= request.getContextPath() %>/wiseadm/qaTextCrmServiceLog.jsp";
			},
			complete: function() {
				setTimeout(function() {$('.loading-panel').hide();}, 300);
			}
	    });
	}
});

$(document).on('click', 'button[name="btnSave"]', function() {
	$('form').submit();
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

$(document).on('click', 'button[name="btnSim"]', function() {
	$simEntryData = $qaEntryData[$(this).attr('data-target-entry-key')];
   	$question = $(this).closest('td').siblings('td[name="question"]').attr('data-question');

   	$( "#find-similar-dialog").dialog('close');
   	$.getJSON('qaDataFindSimilar.jsp', {
   		q: $question
   	},
   	function(data){
   		var list = $('#check-list');
   		list.find('tr').remove();
   		list.append(
   				$('<tr class="text-warning"></tr>')
   				.append('<th>編號</th><th>原始問句： <span id="sim-dialog-question">' + $question + '</span></th>')
   				.append($('<th></th>').append('<form name="newQuestionForm" action="qaDataEditor.jsp" method="post" target="_new"><textarea name="fromQ" class="hide">' + $question + '</textarea><input name="firstAlt" type="hidden"><button type="submit" class="btnCreateNewQ btn btn-danger"><span class="glyphicon glyphicon-plus-sign"></span></button></form>')
   						)
   				);
   		if(data.length == 0){
   			list.append('<tr class="text-danger"><td></td><td><bean:message key="have.no.data"/></td><td></td></tr>');
   		}else{
   			for (var i=0; i < data.length; i++) {
   				var r = data[i];
   				list.append('<tr><td name="sim-kid">' + r.kid + '</td><td><a href="qaDataEditor.jsp?id=' + r.id + '" target="_blank">' 
   						+ r.question 
   						+ '</a></td><td><%if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.A1) > 0) {%><button name="btnMagicFromSim" class="btn btn-primary" data-toggle="tooltip" data-placement="left" title="<bean:message key="robot.magicButton.tooltip"/>"><span class="glyphicon glyphicon-ok-sign"></span></button><% } %></td></tr>');
   			}
		    $('[data-toggle="tooltip"]').tooltip();
   		}
   		$( "#find-similar-dialog").dialog('open');
   	});
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
	$correctAnswerId = $(this).closest('td').siblings('td[name="sim-kid"]').text();

	$data = {
		q: $question,
		correctKid: $correctAnswerId,
		entryData: $simEntryData
	};
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>});

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

  				/*myConfirm("學習結果", msg, function() {
  					$("#magic-dialog").dialog('close');
  				});*/
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
	$thisTr = $(this).closest('tr');
	//console.log($('td[data-entry-key="' + $(this).attr('data-target-entry-key') + '"]')[0]);
	$entryData = $qaEntryData[$(this).attr('data-target-entry-key')];
   	$question = $(this).closest('td').siblings('td[name="question"]').attr('data-question');
	$correctAnswerId = $(this).closest('td').siblings('td[name="robotAnswerId"]').text();
	//console.log($entryData);

	$data = {
		q: $question,
		correctKid: $correctAnswerId,
		entryData: $entryData
	};
	
	$ml = createQALearningMachine({tenantId: <%= t.getId() %>});

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

  				/*myConfirm("學習結果", msg, function() {
  					$("#magic-dialog").dialog('close');
  				});*/

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
   	$question = $thisBtn.closest('td').siblings('td[name="question"]').attr('data-question');

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
				myConfirm("斷詞結果", resp.nlp, function() {});
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
