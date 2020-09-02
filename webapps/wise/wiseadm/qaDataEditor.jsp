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
import="org.json.*"
import="org.apache.commons.text.*"
import="org.apache.commons.lang.StringUtils"
import="org.apache.commons.lang3.StringEscapeUtils"
import="org.apache.commons.beanutils.PropertyUtils"
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
import="com.intumit.solr.robot.qaplugin.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.solr.robot.MultiChannelAnswer"
import="com.intumit.solr.robot.dictionary.DictionaryDatabase"
import="org.apache.struts.Globals"
import="com.intumit.solr.robot.connector.line.RichMessage"
import="com.intumit.solr.tenant.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
	String url = request.getRequestURI();
	String qs = request.getQueryString();
	if(StringUtils.isNotBlank(qs)){
		url += "?" + qs;
	}
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp?r=<%= StringEscapeUtils.escapeEcmaScript(URLEncoder.encode(url, "UTf-8")) %>';
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
%><%

AdminUser currentAdmin = AdminUserFacade.getInstance().getFromSession(session);
String id = StringUtils.trimToNull(request.getParameter("id"));
String binderIdParam = request.getParameter("binderId");
Long kid = null;

SolrDocument doc = null;
QA qa = null;
SolrDocument auditDoc = null;

if (id != null) {
	SolrQuery q = new SolrQuery();
	q.setQuery("id:\"" + URLEncoder.encode(id, "UTF-8") + "\"");
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	SolrServer server = t.getCoreServer4Write();
	SolrDocumentList result = server.query(q).getResults();

	if (result.getNumFound() == 0) return;

	doc = result.get(0);
	kid = QAUtil.id2Kid(id);
	
	if(id.contains(QAUtil.DATATYPE_COMMON_SENSE)){
		auditDoc = QAUtil.getInstance(t).getFilterQASolrDocument(kid, true, QAUtil.DATATYPE_QA_AUDIT, false);
	}
}
else {
	doc = new SolrDocument();
	
	// 從問句建立新問答，有想像空間，目前只會先填預設問句及一個問法
	if (request.getParameter("fromQ") != null) {
		String fromQ = request.getParameter("fromQ");
		doc.setField("QUESTION_s", fromQ);
		
		// 填充第一個問法
		String firstAlt = request.getParameter("firstAlt");
		
		if (firstAlt != null) {
			JSONObject j = new JSONObject();
			j.put("alt.editor", currentAdmin.getId());
			j.put("testCase", fromQ);
			j.put("testCase.editor", currentAdmin.getId());
			
			List<String> alts = new ArrayList<String>();
			alts.add(firstAlt + " // " + j.toString());
			doc.setField("QUESTION_ALT_TPL_ms", alts);
		}
	}
}
String audit = (String)doc.getFirstValue(QA.FN_AUDIT);
if (audit != null && audit.equals("auditProcess") && (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O5) == 0) {
	%>
	<script>
	alert("<bean:message key='qa.audit.permission.denied'/>");
	window.history.back();
	</script>
	<%
	return;
}

qa = new QA(doc);
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">

<link href="<%= request.getContextPath() %>/wiseadm/css/bootstrap-editable.css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/wiseadm/css/jquery-confirm.min.css" rel="stylesheet"/>
<link rel="stylesheet" href="<%= request.getContextPath() %>/wiseadm/css/codemirror.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/stylesheets/plugins/select2/select2.css">

<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.mode.simple.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/groovy.js"></script>

<!--// Loading bootstrap again (after jquery-ui) bcz there is a conflicts using button loading state if bootstrap load before jquery-ui -->
<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jsoneditor.js"></script>

<script src="<%= request.getContextPath() %>/wiseadm/js/bootstrap-editable.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery-confirm.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/dataTables.bootstrap.min.js"></script>
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/select2/select2.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-common.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-build-status.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-ml.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/srbt-qa-util.js"></script>

<script>
// Set the default CSS theme and icon library globally
JSONEditor.defaults.theme = 'bootstrap3';
JSONEditor.defaults.iconlib = 'bootstrap3';
var saveHook = [];
</script>

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

.nav-tabs {
    padding-left: 20px;
    margin-bottom: 0px;
}

.popover-content {
	<%-- 讓各種問法的編輯彈出視窗更美觀 --%>
	margin: 0 10px;
}

.tabBorder{
	border:1px solid #ddd;
	border-radius: 4px;
	padding: 15px;
	padding-left: 25px;
	padding-right: 25px;
	margin-bottom: 20px;
}

.line-message-ul {
    border:3px dotted Gray;
    padding: 10px;
    height:300px;
    display:inline-block;
    vertical-align:top;
}
.line-message-li {
    background-color:Azure;
    border:1px solid Black;
    margin: 2px;
    padding: 2px;
}
.line-message-li-selected {
    background-color:GoldenRod
}

.ui-dialog { z-index: 1000 !important ;}
.form-horizontal .form-group {
    margin-right: 0px;
    margin-left: 0px;
}

.CodeMirror {
  background: rgba(0, 0, 0, 0);
  height: auto;
}
.CodeMirror-question {
  border: 1px solid #aaa;
  height: auto;
}
.CodeMirror-gutters { z-index: 0}

.mouse1:hover{
cursor: n-resize;
background-color:#FFFF77;
}

.mouse2:hover{
cursor: n-resize;
background-color:#FFFF77;
}
</style>
</head>
<body>
<%
boolean hasBinderIdParam = StringUtils.isNotBlank(binderIdParam);
boolean hideNavBar = hasBinderIdParam || Boolean.parseBoolean(request.getParameter("hideNavBar"));
Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String ques = getFieldValue(doc, "QUESTION_s");
String briefques = getFieldValue(doc, "Briefly_QUESTION_s");
String quesAlt = getFieldValues(doc, "QUESTION_ALT_TPL_ms");
String customScript = getFieldValue(doc, "CUSTOM_SCRIPT_s");
String locales = getFieldValues(doc, QA.FN_QA_LOCALES);

UserClue uc = UserClue.getByAdminUserId(t.getId(), currentAdmin.getId());
boolean isAlreadyBinded = uc != null && uc.getLineUserId() != null;

List<DictionaryDatabase> dicts = DictionaryDatabase.listByTenantId(t.getId());
String kpRegex = "碩網資訊";
for (DictionaryDatabase dd: dicts) {
	if (dd.getPurposeSet().contains(DictionaryDatabase.Purpose.KNOWLEDGE_POINT)) {
		kpRegex += "|" + dd.getKeyword().replaceAll("\\n", "");
	}
}

kpRegex = "(" + kpRegex + ")";
/*
String answer = "";
String answerPhone = "";
String answerVip = "";
String answerPhoneVip = "";
String answerVoice = "";
String answerVoiceVip = "";
System.out.println( "edit id : " + id );
if (id != null) {
	answer = MultiChannelAnswer.channelToAnswer(id, MultiChannelAnswer.WEB_CHANNEL, MultiChannelAnswer.DEFAULT_USERTYPE);
	answerVip = MultiChannelAnswer.channelToAnswer(id, MultiChannelAnswer.WEB_CHANNEL, MultiChannelAnswer.VIP_USERTYPE);
	answerPhone = MultiChannelAnswer.channelToAnswer(id, MultiChannelAnswer.APP_CHANNEL, MultiChannelAnswer.DEFAULT_USERTYPE);
	answerPhoneVip = MultiChannelAnswer.channelToAnswer(id, MultiChannelAnswer.APP_CHANNEL, MultiChannelAnswer.VIP_USERTYPE);
	answerVoice = MultiChannelAnswer.channelToAnswer(id, MultiChannelAnswer.VOICE_CHANNEL, MultiChannelAnswer.DEFAULT_USERTYPE);
	answerVoiceVip = MultiChannelAnswer.channelToAnswer(id, MultiChannelAnswer.VOICE_CHANNEL, MultiChannelAnswer.VIP_USERTYPE);
}
answer = getFieldValue(doc, "ANSWER_s");
answerPhone = getFieldValue(doc, "ANSWERPHONE_s");
*/

Date created = (Date) doc.getFieldValue("created_dt");
long createdTime = created == null?System.currentTimeMillis():created.getTime();
%>
<jsp:include page="navbar-qa.jsp"><jsp:param name="hideUI" value="<%= hideNavBar %>" /></jsp:include>
<div class="container">
<ul class="nav nav-tabs" id="myTabs" role="tablist" style="margin: 10px 0 0 10px;"> 
	<li role="presentation" class="active">
		<a href="#home-tab" id="home" role="tab" data-toggle="tab" aria-controls="home" aria-expanded="true"><bean:message key='global.basicInfo'/></a>
	</li> 
	<li role="presentation" class="">
		<a href="#answers-tab" role="tab" id="answers" class='tab-iframe' data-toggle="tab" aria-controls="answers" aria-expanded="false"><bean:message key='global.answer'/></a>
	</li> 
	<li role="presentation" class="">
		<a href="#sentences-tab" id="sentences" role="tab" data-toggle="tab" aria-controls="sentences" aria-expanded="false"><bean:message key='qa.testCase'/></a>
	</li> 
	<li role="presentation" class="">
		<a href="#inheritance-tab" id="inheritance" role="tab" data-toggle="tab" aria-controls="inheritance" aria-expanded="false"><bean:message key='qa.inheritance'/></a>
	</li> 
	<li role="presentation" class="">
		<a href="#others-tab" role="tab" id="others" class='tab-iframe' data-toggle="tab" aria-controls="others" aria-expanded="false"><bean:message key='global.other'/></a>
	</li> 
	<%-- >li role="presentation" class="dropdown"> 
		<a href="#" class="dropdown-toggle" id="myTabDrop1" data-toggle="dropdown" aria-controls="myTabDrop1-contents">其他 <span class="caret"></span></a> 
		<ul class="dropdown-menu" aria-labelledby="myTabDrop1" id="myTabDrop1-contents"> 
			<li><a href="#dropdown1" role="tab" id="dropdown1-tab" data-toggle="tab" aria-controls="dropdown1">@fat</a></li> 
			<li><a href="#dropdown2" role="tab" id="dropdown2-tab" data-toggle="tab" aria-controls="dropdown2">@mdo</a></li> 
		</ul> 
	</li --%> 
	<li class="pull-right">
	    <div class="btn-group">
	  		<button type="button" class="btn btn-danger btn-qa-save" title='<bean:message key='submit'/>' <%= auditDoc == null ? "" : "disabled" %>>
	  		<span class='glyphicon glyphicon-floppy-disk'></span>
	  		</button>
	  		<%-- button type="button" class="btn btn-default btn-qa-cancel" title='<bean:message key='global.cancel'/>'><span class='glyphicon glyphicon-repeat'></span></button --%>
	  	</div>
	</li>
	<li class="pull-right" id="qaStatusOnTab" style="margin:8px;"></li>
</ul>
<BR>
<form id="form" role="form" class="form-horizontal col-md-12" method="post">
<input type="hidden" name="hideNavBar" value=<%= hideNavBar %> />
<input type="hidden" name="height" />
<input type="hidden" name="batchNlpReplaceSyn" value="false">
<input type="hidden" name="id" value="<%= StringUtils.trimToEmpty(StringEscapeUtils.escapeHtml4(id)) %>" />
<%
String binderId = getFieldValues(doc, "BINDER_ID_ml");
if(hasBinderIdParam){
	binderId += (StringUtils.isBlank(binderId)?"":"\n") + binderIdParam;
}
%>
<input type="hidden" name="binderId" value="<%= StringEscapeUtils.escapeHtml4(binderId) %>" />

<div class="tab-content tabBorder" id="qaDataEditorTabContent"> 
<div class="tab-pane fade active in" role="tabpanel" id="home-tab" aria-labelledby="home">

  <div class="form-group">
  <% String decideAudit = auditDoc == null ? "" : auditDoc.getFieldValue("AUDIT_s").toString(); %>
  <% if ( decideAudit.equals("auditEdit") ) { %>
	   <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='knowledge.num'/><span style='color:red'>(<bean:message key='qa.editing'/>)</span></label> 
  <% } else if ( decideAudit.equals("auditProcess") ) { %>
	   <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='knowledge.num'/><span style='color:red'>(<bean:message key='qa.auditing'/>)</span></label> 
  <%  } else { %>
	  <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='knowledge.num'/></label> 
  <% } %>    
    <div class="col-sm-9">
    	<h4 name="kid-label"><%= kid != null ? kid : MessageUtil.getMessage(locale, "random.generation") %></h4>
    </div>
    <div class="col-sm-1">
    	<button id="toggle-set-kid-panel" type="button" class="btn btn-default"><bean:message key='global.recommend'/></button>
    </div>
  </div>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='standard.problem'/></label>
    <div class="col-sm-9">
		<input type="text" class="form-control" id="question" name="question" value="<%= StringEscapeUtils.escapeHtml4(ques) %>"></input>
    	<input type="hidden" name="question_hide" value="<%= StringEscapeUtils.escapeHtml4(ques) %>">
    </div>
    <div class="col-sm-1">
    	<button id="check" type="button" class="btn btn-default"><bean:message key='global.detection'/></button>
    </div>
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='recommended.display.problem'/></label>
    <div class="col-sm-9">
		<input type="text" class="form-control" id="briefly_question" name="briefly_question" value="<%= StringEscapeUtils.escapeHtml4(briefques) %>"></input>
    	<input type="hidden" name="briefly_question_hide" value="<%= StringEscapeUtils.escapeHtml4(briefques) %>">
    </div>
  </div>
  <% if (t.getEnableMultiLocale()) { %>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='qa.locales'/></label>
    <div class="col-sm-4">
		<input type="text" class="form-control" name="locales" value="<%= StringEscapeUtils.escapeHtml4(locales) %>"></input>
    </div>
  </div>
  <% } %>

    <div class="form-group">
  	<label for="chooseEnable" class="col-sm-2 control-label"><bean:message key='enable.time'/></label>
    <div class="col-sm-3">
      <%
      Date enableDt = (Date) doc.getFieldValue("ENABLE_dt");
      boolean hasEnableDt = enableDt != null;
      String formattedEnableDt = hasEnableDt?QAUtil.formatDate(enableDt):"";
      Integer enableDays = (Integer) doc.getFieldValue("ENABLE_DAYS_i");
      boolean hasEnableDays = enableDays != null;
      %>
	  <div class="input-group">
	     <label style="font-weight: normal;">
			 <input type="radio" name="enableRadio" value="0"
			 <% if(!hasEnableDt ||
				   (hasEnableDays && enableDays.equals(0))){ %>checked<% } %>><bean:message key='enable.rightNow'/> (<bean:message key='default'/>)
		 </label>
	  </div>
	  <div class="input-group">
		 <span class="input-group-addon">
		     <input type="radio" name="enableRadio" value=""
		     <% if(hasEnableDt && !hasEnableDays){ %>checked<% } %>>
		 </span>
		 <input type="text" class="form-control" name="enableDate" placeholder='<bean:message key="custom.enable.date"/>'
		        readonly="readonly" style="background-color: transparent;"
		      	value="<%= StringEscapeUtils.escapeHtml4((hasEnableDt && !hasEnableDays)?formattedEnableDt:
		      			QAUtil.formatDate(new Date())) %>">
	  </div>
    </div>
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='global.label'/></label>
    <div class="col-sm-3">
    	<%
    	String category = getFieldValues(doc, "CATEGORY_ms");
    	%>
	    <textarea class="form-control" rows="5" name="category"><%= StringEscapeUtils.escapeHtml4(category) %></textarea>
	    <p class="help-block"><bean:message key='global.label.ex'/></p>
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
      List<String> ihAltTpls = (List<String>) doc.getFieldValue("InheritantAltTemplate_ms");
      List<String> exAltTpls = (List<String>) doc.getFieldValue(QA.FN_QUESTION_EXCLUDE_ALT_TPL);
      boolean hasExpireDays = expireDays != null;
      boolean forever = !hasExpire && ExpireDuration.forever.is(expireDays);
      ExpireDuration duration = ExpireDuration.of(expireDays);
      long expireTime = duration.getExpireTime(createdTime);
      %>
      <b><%= forever?MessageUtil.getMessage(locale, "at.present.forever.save"):(MessageUtil.getMessage(locale, "at.present.save.time")+"：" + StringEscapeUtils.escapeHtml4(
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
	      	value="<%= StringEscapeUtils.escapeHtml4(hasExpire?formattedExpire:
      			QAUtil.formatDate(new Date(ExpireDuration.one_month.getExpireTime(createdTime)))) %>">
	   </div><!-- /input-group -->
	 </div>
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key="category"/></label>
    <div class="col-sm-3">
      <div class="input-group">
       	  <label style="font-weight: normal;">
       	  <%
	       	  String qaCategory = null;
	       	  if(doc.getFieldValue("QA_CATEGORY_s") != null){
	       		qaCategory = (String) doc.getFieldValue("QA_CATEGORY_s");
	       	  }
	      %>
       	  <%
       	  String[] qaCategorys = null;
       	  if(t.getQaCategory() != null){
       		qaCategorys = t.getQaCategory().split(",");
       	  }

       	  for (int i = 0; qaCategorys != null && i < qaCategorys.length; i++) {
       		if(qaCategorys[i].equals("全部")){
       	    %>
       			<input type="radio" name="qaCategoryRadio" value="<%=qaCategorys[i]%>" <% if(qaCategory == null || qaCategory.equals(qaCategorys[i])){ %>checked<% } %> ><%=qaCategorys[i]%><br>
       	    <%
       		}else{
       		%>
       			<input type="radio" name="qaCategoryRadio" value="<%=qaCategorys[i]%>" <% if(qaCategory != null && qaCategory.equals(qaCategorys[i])){ %>checked<% } %> ><%=qaCategorys[i]%><br>
       	    <%
       		}
		  }
       	  %>
	      </label>
	   </div>
	 </div>
  </div>

  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='other.set'/></label>
    <div class="col-sm-3">
    <%
    Boolean isNotForMlt = (Boolean)doc.getFirstValue(QA.FN_IS_NOT_FOR_MLT);    
    String checked = ( isNotForMlt == null || !isNotForMlt ) ? "checked" : "";
    %>
    <input type="checkbox" name="forMlt" <%= checked %> data-size="mini" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>">&nbsp;<bean:message key='can.recommended'/> <br/>
    <%
    Boolean isNotForSearch = (Boolean)doc.getFirstValue(QA.FN_IS_NOT_FOR_SEARCH);
    checked = ( isNotForSearch == null || !isNotForSearch ) ? "checked" : "";
    %>
    <input type="checkbox" name="forSearch" <%= checked %> data-size="mini" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>">&nbsp;<bean:message key='can.selected'/> <br/>
    <%
    Boolean notAppendMlt = (Boolean)doc.getFirstValue(QA.FN_NOT_APPEND_MLT);
    checked = ( notAppendMlt == null || !notAppendMlt ) ? "checked" : "";
    %>
    <input type="checkbox" name="appendMlt" <%= checked %> data-size="mini" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>">&nbsp;<bean:message key='additional.recommendations'/><br/>
  	<%
    		Boolean kmsRelateExpiredMemo = (Boolean)doc.getFirstValue(QA.FN_IS_KMS_RELATE_EXPIRED_MEMO);
    		checked = ( kmsRelateExpiredMemo != null && kmsRelateExpiredMemo ) ? "checked" : "";
    	%>
  	<input type="checkbox" name="kmsRelateExpiredMemo" <%= checked %> data-size="mini" data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
  	&nbsp;<bean:message key='kms.relate.expired.memo'/>
    <br/>
    </div>
  </div>
<% if(t.getEnableQaAudit()){ %>
  <div class="form-group">
  	<label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='global.audit.status'/></label>
  	<div class="col-sm-5">
  		<div class="row>">
  			<%
    		if(audit != null && audit.equals("auditEdit") || kid == null){
  				checked = "checked" ;
  			} else {
  				checked = "" ;
  			}
  			%>
  			<%
  			if (audit == null || audit.equals("auditEdit")){ 
  			%>
  			<input type="radio" name="qaAudit" value="auditEdit" <%= checked %>><bean:message key='qa.audit.edit'/>
  			<%
  			}
  			%>
  			<%
  			if (audit != null && audit.equals("auditProcess")) {
  				checked = "checked" ;
  			} else {
  				checked = "" ;
  			}
    		%>
    		<%
  			if (audit == null || audit.equals("auditEdit")){ 
  			%>
  			<input type="radio" name="qaAudit" value="auditProcess" <%= checked %>><bean:message key='qa.audit.process'/>
  			<%
  			}
  			%>
  			<%
  			if(id != null){
  				boolean idstartsWith = id.startsWith("QA_AUDIT");
  	  			if((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O5) > 0
  						&& id.startsWith("QA_AUDIT") && audit.equals("auditProcess")){ 
  	    		%>
  	  				<input type="radio" name="qaAudit" value="auditPass"><bean:message key='qa.audit.pass'/>
  	  				<input type="radio" name="qaAudit" value="auditReject" ><bean:message key='global.audit.reject'/>
  	  			<%
  	  			}
  			}
    		%>
  		</div>
  	</div>
  </div>
  <% } %>  
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='archive.and.other.functions'/></label>
    <div class="col-sm-5">
    	<div class="row>">
    <%
    Boolean banned = (Boolean)doc.getFirstValue(QA.FN_BANNED);
    checked = ( banned == null || !banned ) ? "checked" : "";
    %>
        <input type="checkbox" name="notBanned" <%= checked %> data-toggle="toggle" data-on="<bean:message key='global.able'/>" data-off="<bean:message key='global.disable'/>" data-onstyle="success">
  		<input type="checkbox" name="notBanned_hide" <%= checked %> style="display: none;"/>
  		<button id="save" type="button" class="btn btn-danger" <%= auditDoc == null ? "" : "disabled" %> ><bean:message key='submit'/></button>
  		<button id="preivew" type="button" class="btn btn-primary" disabled><bean:message key='global.preview'/></button>
  		</div>
  		<p></p>
		<% if (id != null) { %>
	    	<div class="row>">
  		<button id="testLogBtn" type="button" class="btn btn-info"><bean:message key='global.test.record'/> (<%= kid == null ? 0 : EvaluationLogEntity.countByKid(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, kid) %>)</button>
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
  		<bean:message key='last.edit'/>：<br/><%= StringEscapeUtils.escapeHtml4(timeFormat.format(lastUpdated) + (user == null?"":("，" + user.getName() + "（" + user.getLoginName()) + "）"))%>
  		<br/><bean:message key='handle.state'/>：<h1 id="qaStatus"><%= QAAltBuildQueue.getStatus(t.getId(), id) %></h1>
  		<%
  		}
  		%>
    </div>
  </div>
</div> <%-- Tab Home End --%>

<div class="tab-pane fade in" role="tabpanel" id="sentences-tab" aria-labelledby="sentences">
  <div class="form-group">
    <h4 class="col-sm-12">
    	<bean:message key='kinds.of.questions.template'/>
    	<button id="save_copy" type="button" class="btn btn-danger" <%= auditDoc == null ? "" : "disabled" %>><bean:message key='submit'/></button>
		<a class="btn btn-success btnAddAltRow" href="#"><bean:message key='global.add'/></a>

    	<div class="btn-group">
    	<button id="checkSyn" type="button" class="btn btn-default"><bean:message key='global.detection'/><bean:message key='global.synonyms'/></button>
    	<button id="suggestTemplateBtn" type="button" class="btn btn-default"><bean:message key='apply.template'/></button>
    	<button id="toggleBatchNlpPanel" type="button" class="btn btn-default"><bean:message key='execute.batchNlp'/></button>
    	<button id="autoGenerateAltBtn" type="button" class="btn btn-default easterEgg"><bean:message key='execute.autoGenerateAlt'/></button>
    	</div>
    	<%
    	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0) {
    	%>
    	<button id="btnAdvAltEditor" type="button" class="btn btn-default pull-right"><span class="glyphicon glyphicon-pencil"></span></button>
    	<%
    	}
    	%>
    </h4>
<%
List<String> altTpls = QAUtil.parseMultiValue(StringUtils.trimToEmpty(quesAlt));
Set<String> sortedAlts = new TreeSet<String>();
if (altTpls != null) {
	sortedAlts.addAll(altTpls);
}

JSONArray altDataTableArray = new JSONArray();
int offset = 0;

for (String alt: sortedAlts) {
	Map<String, String> altData = QA.parseQAAlt(alt);
	String a = StringUtils.trimToEmpty(altData.get("alt"));
	String ae = StringUtils.trimToEmpty(altData.get("alt.editor"));
	String tc = StringUtils.trimToEmpty(altData.get("testCase"));
	String tce = StringUtils.trimToEmpty(altData.get("testCase.editor"));

	JSONObject row = new JSONObject();
	row.put("offset", offset++);
	row.put("alt", a);
	row.put("alt.editor", ae);
	row.put("testCase", tc);
	row.put("testCase.editor", tce);

	altDataTableArray.put(row);
}
%>
    <div class="col-sm-12">
	<div class="form-group">
		<table class="table table-striped table-bordered altTable" width="100%">
		<thead>
			<tr>
			<th><bean:message key='qa.alts.template'/></th>
			<th><bean:message key='qa.alts.testCase'/></th>
			<th><bean:message key='global.action'/></th>
			</tr>
		</thead>
		</table>
		<textarea class="form-control" rows="10" id="question_alt" name="question_alt" style="display: none;"><%= StringEscapeUtils.escapeHtml4(StringUtils.join(sortedAlts, "\n")) %></textarea>
		<textarea class="form-control" rows="10" name="for_batch_nlp" style="display: none;"></textarea>

    <p class="help-block">
    	<ul>
    	<bean:message key='question.alt.ex'/>
    	</ul>
    </p>
    </div>
    </div>
    <div class="col-sm-12">
    <h3><bean:message key='question.exclude.alt'/></h3>
    <textarea class='form-control' name='exAltTpls' id='exAltTpls' rows=5><%= StringUtils.join(exAltTpls != null ? exAltTpls : new HashSet<String>(), "\r\n") %></textarea>
    </div>
  </div>
</div> <%-- Tab Sentences End --%>

<div class="tab-pane fade in" role="tabpanel" id="inheritance-tab" aria-labelledby="inheritance">
  <div class="form-group">
    <div class="col-sm-12">
  	<table id="inheritance-template-list" class="table table-striped table-hover" style="width:100%">
  	<thead>
	<tr>
		<th class="col-sm-3"><bean:message key='template.name'/></th>
		<th class="hide"><bean:message key='is.enabled'/></th>
		<th class="col-sm-3"><bean:message key='template.description'/></th>
		<th class="col-sm-3"><bean:message key='knowledge.point'/></th>
		<th class="col-sm-3"><bean:message key='operation'/></th>
	</tr>
	</thead>
	<tbody>
	</tbody>
  	</table>
    </div>
	<script id="applied-sentence" type="x-tmpl-mustache">
	  <tr>
		<td>{{no}}</td>
		<td>
			<div class="excludeSentence" style="display:none;">{{sentence}}</div>
			<div class="showCode" style="width:100%;"></div>
		</td>
		<td>
			{{#checked}}
			<input type="checkbox" class="toggleExcludeSentence" data-width="100" data-size="mini" checked data-toggle="toggle" data-on="使用" data-off="<bean:message key="global.excluded"/>">
			{{/checked}}
			{{^checked}}
			<input type="checkbox" class="toggleExcludeSentence" data-width="100" data-size="mini" data-toggle="toggle" data-on="使用" data-off="<bean:message key="global.excluded"/>">
			{{/checked}}
		</td>
	  </tr>
	</script>
	<script id="ihTplRow" type="x-tmpl-mustache">
		<tr>
		<td name="ihTplName" data-id="{{r.id}}"><strong>{{r.name}} ({{r.mkey}})</strong></td>
		<td>{{r.enabled}}</td>
		<td name="description">{{r.description}}</td>
		<td>
			{{#r.keywords}}
			<input type="text" name="ihTplKeywords{{r.mkey}}" data-id="{{r.id}}" value="{{r.keywords}}">
			{{/r.keywords}}
			{{^r.keywords}}
			<input type="text" name="ihTplKeywords{{r.mkey}}" data-id="{{r.id}}" value="{{globalKeywords}}">
			{{/r.keywords}}
		</td>
		<td>
			<button type='button' class='btn btn-primary btnPreviewAutoAlt' data-id="{{r.id}}"><bean:message key="global.fineTune"/></button>
			<button type='button' class='btn btn-warning btnToggleExcludeSentencesRow' data-id="{{r.id}}" data-target="#ihTplExcludeSentencesTr{{r.id}}"><bean:message key="global.excluded"/> <span class='badge' id='ihTplExcludeSentencesBadge{{r.id}}'>0</span></button>
			<input type="checkbox" {{isChecked}} 
					name="ihToggleTpl{{r.mkey}}" data-id="{{r.id}}" data-toggle="toggle" data-on="<bean:message key="global.apply"/>" data-off="<bean:message key="global.disable"/>" data-onstyle="success">
			<!--tr id='ihTplExcludeSentencesTr{{r.id}}' class='collapse'><td colspan="4" data-id="{{r.id}}" name="ihTplExcludeSentences{{r.id}}"></td></tr-->
		</td>
		</tr>
	</script>
	<script id="ihTplExcludeSentences" type="x-tmpl-mustache">
		{{#sentences}}
		<div class="ihTplExcludeSentenceDiv">
			<div class="matchedExcludeSentence" style="display:none;">{{.}}</div>
			<div class="showCode"></div>
		</div>
		{{/sentences}}
	</script>
    <script>
    var currentIhAltTpls = <%= ihAltTpls != null ? new JSONArray(ihAltTpls) : "[]"  %>;
    var allAltTplsData = {};
    var refreshExcludeSentenceCodeMirror = function(target, targetBadge) {
    		if (typeof(target) == 'undefined') {
    			target = '';
    		}
    		else {
    			target += ' ';
    		}
    		
    		$targets = $(target + '.matchedExcludeSentence');
    		$size = $targets.length;
    		
    		if (typeof(targetBadge) != 'undefined') {
			$(targetBadge).text($size);    			
    		}
    		else {
    			for (var i=0; typeof(tplData) != 'undefined' && i < tplData.length; i++) {
    				tpl = tplData[i];
    				if (tpl.hasOwnProperty('excludes'))
    					$('#ihTplExcludeSentencesBadge' + tpl.id).text( tpl.excludes.length );    	
    			}
    		}
    		
    		$targets.each(function() {
			$this = $(this);
			$code = $this.text();

			$this.parent().find('.showCode').html("");
	    		CodeMirror($this.parent().find('.showCode')[0], {
		        value: $code.trim(),
		        mode: 'qaalt',
		        lineNumbers: false, lineWrapping: true,
		        readOnly: true
		    });
		});
    };
    var addInToArrayButAvoidDuplicate = function(targetArr, fromArr) {
    		for (var i=0; i < fromArr.length; i++) {
    			a = fromArr[i];
    			
    			if ($.inArray(a, targetArr) == -1) {
    				targetArr.push(a);
    			}
    		}
    };
    var updateExcludeSentences = function(tplId, sentences) {
		/*var dataForRender = {sentences: sentences};
		var template = $('#ihTplExcludeSentences').html();
		Mustache.parse(template);
		var sentencesHtml = Mustache.render(template, dataForRender);
		$('td[name=ihTplExcludeSentences' + tplId + ']').html(sentencesHtml);*/
    };
    $.post('qaAltTemplateSuggest.jsp', {
		format: 'json',
		question: $('#question').val(),
		currentIhAltTpls: JSON.stringify(currentIhAltTpls)
	},
	function(data) {
		exAltTpls = typeof(exAltTplsEditor) != 'undefined' ? exAltTplsEditor.getDoc().getValue().trim().split(/\r?\n/g) : $('#exAltTpls').val().trim().split(/\r?\n/g);
		//console.log(exAltTpls);
		var list = $('#inheritance-template-list tbody');
		list.find('tr:has(td)').remove();
		if (data.tpls.length == 0) {
			<%-- 另外塞個 table，才不會讓 inheritance-template-list 在沒資料的時候會卡住無法 save QA --%>
			$('<table class="" style="width:100%;"><tr class="text-center"><td><bean:message key="have.no.data"/></td></tr></table>').insertAfter($('#inheritance-template-list'));
		} else {
			for (var i=0; i < data.tpls.length; i++) {
				var r = data.tpls[i];
				var excludes = [];
				
				if (r.enabled) {
					var applied = r.applied;
					for (var t=0; t < exAltTpls.length; t++) {
						if ($.inArray(exAltTpls[t], applied) > -1) {
							excludes.push( exAltTpls[t] );
						}
					}
				}
				var dataForRender = {r: r, globalKeywords: data.keywords, isChecked: r.enabled?' checked':''};

				var template = $('#ihTplRow').html();
				Mustache.parse(template);
				var rowHtml = Mustache.render(template, dataForRender);
				list.append($(rowHtml));
				
				r["excludes"] = excludes;
				
				allAltTplsData[r.id] = r;
				//updateExcludeSentences(r.id, excludes);
				//refreshExcludeSentenceCodeMirror('#ihTplExcludeSentencesTr' + r.id, '#ihTplExcludeSentencesBadge' + r.id);
			}

			tplData = data.tpls;
			//console.log(tplData);
			$('input[name="toggleTpl"]').bootstrapToggle();
			$('#inheritance-template-list').DataTable({ 
				page: true,
				order: [[ 1, "desc" ]],
		        columnDefs: [
		            {
		                "targets": [ 1 ],
		                "visible": false,
		                "searchable": false
		            }
		        ],
				drawCallback: function( settings ) {
					$('#inheritance-template-list input[type="checkbox"]').bootstrapToggle('enable');
					refreshExcludeSentenceCodeMirror();
			    }
			});
		}
	}, "json");
    
    $(document).on('click', '.btnToggleExcludeSentencesRow', function() {
    		var tplId = $(this).attr('data-id');
    		var dt = $('#inheritance-template-list').DataTable();
    		//var targetTr = $(this).attr('data-target');
    		//$(targetTr).toggle(200);
    		//refreshExcludeSentenceCodeMirror(targetTr, '#ihTplExcludeSentencesBadge' + tplId);
    		
    		var tr = $(this).closest('tr');
        var row = dt.row( tr );
     
        // 顯示 child row
        if ( row.child.isShown() ) {
        		// 顯示中的話就隱藏
            row.child.hide();
            tr.removeClass('shown');
        }
        else {
        		// 顯示排除詞
	    		tplData = allAltTplsData[tplId];
	    		var dataForRender = {sentences: tplData.hasOwnProperty('excludes')?tplData.excludes:[]};
	    		var template = $('#ihTplExcludeSentences').html();
	    		Mustache.parse(template);
	    		var sentencesHtml = Mustache.render(template, dataForRender);
            row.child( sentencesHtml ).show();
            
            refreshExcludeSentenceCodeMirror('.ihTplExcludeSentenceDiv', '#ihTplExcludeSentencesBadge' + tplId);
            tr.addClass('shown');
        }
            
    });
    
    $(document).on('click', '.btnApplyAltTemplate', function() {
	    	var tplId = $(this).attr('data-id');
    		sentences = [];
    		$dialog = $('#autoGenerateAlt-dialog');
    		$dialog.find('input:checkbox:not(:checked)').each(function() {
	    		$this = $(this);
	    		var t = $this.closest('tr').find('.excludeSentence').text();
	    		sentences.push(t);
	    	});
		//updateExcludeSentences(tplId, sentences);
		allExcludes = [];

		for (var j=0; j < tplData.length; j++) {
			if (tplData[j].id == tplId) {
				tplData[j]["excludes"] = sentences;
			}
			
			//console.log(allExcludes);
			addInToArrayButAvoidDuplicate(allExcludes, tplData[j].excludes);
		}
		exAltTplsEditor.getDoc().setValue(allExcludes.join("\r\n"));

		refreshExcludeSentenceCodeMirror('.ihTplExcludeSentenceDiv' + tplId, '#ihTplExcludeSentencesBadge' + tplId);
	    	$dialog.dialog('close');
    });
    
    $(document).on('click', '.btnPreviewAutoAlt', function() {
	    	$dialog = $("#autoGenerateAlt-dialog");
		$dialog.dialog('option', 'title', '斷句範本預覽及排除設定');
		tplId = $(this).attr('data-id');
		tr = $(this).closest('tr');
		kw = tr.find('input[name^=ihTplKeywords]').val();
		
		for (var j=0; j < tplData.length; j++) {
			if (tplData[j].id == tplId && tplData[j].template != null) {
				kwArr = kw.split(',');
				var applied = tplData[j].template;
			    	tbl = $('<table class="table table-striped table-bordered" ></table>');
			    	
	    			for (var i=0; i < kwArr.length; i++) {
					if (i == 0) {
						applied = applied.replace(new RegExp("{{KEYWORD}}", "g"), kwArr[i]);
					}
					applied = applied.replace(new RegExp("{{KEYWORD" + (i+1) + "}}", "g"), kwArr[i]);
	    			}
			    	
			    	lines = applied.split(/\r?\n/g);
			    	for (var i=0; i < lines.length; i++) {
	    				dataForRender = {
		    					no: (i+1),
		    					sentence: lines[i],
	    						checked: true
		    			};
	    				
					if ($.inArray(lines[i], tplData[j].excludes) > -1) {
						dataForRender.checked = false;
					}
					
	    				var template = $('#applied-sentence').html();
	    				Mustache.parse(template);
	    				var panelBody = Mustache.render(template, dataForRender);
	    				
	    				tbl.append($(panelBody));
			    	}
			    	
		    		$dialog.html(tbl);	
				$dialog.dialog('open');
		    		$('.toggleExcludeSentence').bootstrapToggle();
		    		
		    		$('.excludeSentence').each(function() {
		    			$this = $(this);
		    			$code = $this.text();
		    			
			    		CodeMirror($this.parent().find('.showCode')[0], {
				        value: $code.trim(),
				        mode: 'qaalt',
				        lineNumbers: false, lineWrapping: true,
				        readOnly: true
				    });
		    		});
		    		
		    		<%-- 前後各放一個按鈕 --%>
		    		$('<tr><td colspan=3><button data-id="' + tplId + '" class="btn btn-danger btnApplyAltTemplate"><bean:message key="save"/></button></td></tr>').prependTo(tbl);
		    		$('<tr><td colspan=3><button data-id="' + tplId + '" class="btn btn-danger btnApplyAltTemplate"><bean:message key="save"/></button></td></tr>').appendTo(tbl);
			}
		}
		
		return false;
    });
    </script>
  </div>
</div> <%-- Tab Inheritance End --%>

<div class="tab-pane fade in" role="tabpanel" id="answers-tab" aria-labelledby="answers">
<div class="col-sm-12">
<%
QAChannel.checkData(t.getId());
QAUserType.checkData(t.getId());
List<QAChannel> channels = QAChannel.list(t.getId());
List<QAUserType> userTypes = QAUserType.list(t.getId());
%>
<!-- tab start -->
  <div class="form-group">
	<ul id="myTab" class="nav nav-tabs">
		<% for (QAChannel c: channels) { boolean active = StringUtils.equalsIgnoreCase(c.getCode(), "web"); %>
		   <li <%= active ? "class='active'" : "" %>><a href="#<%= c.getCode() %>" data-toggle="tab"><%= c.getName() %></a></li>
		<% } %>
	</ul>
  </div>
<!-- tab end -->
<!-- tab-content start -->
<div class="tab-content">

<%
for (QAChannel c: channels) {
	boolean isWeb = StringUtils.equalsIgnoreCase(c.getCode(), "web");
	%>
	<div class="tab-pane fade <%= isWeb ? "in active" : "" %>" id="<%= c.getCode() %>">
	<%
	for (QAUserType ut: userTypes) {
		String contextPath = request.getContextPath();
		boolean isWebAndUnknown = isWeb && StringUtils.equalsIgnoreCase(ut.getCode(), "unknown");
		String concatCode = "answer_" + c.getCode() + "_" + ut.getCode();
    	String type = StringUtils.lowerCase(c.getType().name());
    	String typeSuffix = "_" + type + "type";
    	String rmItemNamePrefix = type + "MessageConfig";
    	
    	String editorDir = (c.getType() == QAChannelType.PLAIN_TEXT || c.getType() == QAChannelType.RICH_TEXT && c.getCode().equals("web")) ? "webRM" : "webLine";
    	
		MultiChannelAnswer mca = MultiChannelAnswer.get(t.getId(), id, c.getCode(), ut.getCode());
		String answer = MultiChannelAnswer.channelToAnswer(t.getId(), id, c.getCode(), ut.getCode());

		if (isWebAndUnknown && StringUtils.trimToNull(answer) == null) {
			// 往前相容，所以 default 從索引拿
			answer = getFieldValue(doc, "ANSWER_s");
		}
%>
  <div class="form-group">
		<label for="inputQuestion" class="col-sm-2 control-label"><p class="text-left"><%= MessageUtil.returnKeyIfNotExist(locale, ut.getName()) %></p></label>
  </div>
  <div class="form-group">
<%
		QAChannelType ctype = c.getType();
		JSONObject ctcfg = null;
		if (mca != null && StringUtils.isNotEmpty(mca.getChannelTypeConfig())) {
			ctcfg = new JSONObject(mca.getChannelTypeConfig());
		}
		
		if (QAChannelType.PLAIN_TEXT == ctype || QAChannelType.RICH_TEXT == ctype) {
		    if (QAChannelType.RICH_TEXT == ctype) {
				String oldAnswerRichtext = getFieldValue(doc, "ANSWER_RICHTEXT_s");
				
				if (StringUtils.isEmpty(answer) && oldAnswerRichtext != null) {
					answer = oldAnswerRichtext; // 試著往回相容
				}
		    }
			%>
		    <div class="col-sm-12">
				<%-- 選擇要原始輸入 or 單圖文 or 多圖文 --%>
				<% if (c.getSupportMultiRichMessages()) { %>
					<input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= type %>_answer_text" <%= (mca == null || ((StringUtils.isNotBlank(mca.getAnswer()) || StringUtils.isBlank(mca.getRichMessageMKey())) && ctcfg == null)) ? "checked" : "" %>> <bean:message key="answer.type.original"/>
					&nbsp;&nbsp;&nbsp;&nbsp;<input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= type %>_answer_rich_message" <%= (mca != null && ctcfg == null && StringUtils.isNotBlank(mca.getRichMessageMKey())) ? "checked" : ""  %>> <bean:message key="answer.type.richmessage"/>
					&nbsp;&nbsp;&nbsp;&nbsp;<input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= type %>_answer_multiple_rich_messages" <%= (mca != null && ctcfg != null) ? "checked" : ""  %>> <bean:message key="answer.type.multiple.richmessages"/>
				<% } %>
				
			    <div id="<%= concatCode %>_original" style="<%= c.getSupportMultiRichMessages() ? "display:none;" : "" %> margin-bottom: 20px;">
				    <%-- 原有的輸入區 --%>
					<textarea class="form-control" id="<%= concatCode %>" name="<%= concatCode %>" rows="8"><%= StringEscapeUtils.escapeHtml4(answer) %></textarea>
	    			<input type="hidden" name="<%= concatCode %>_hide" value="<%= StringEscapeUtils.escapeHtml4(answer) %>">	
    			</div>		  
			  	<div id="<%= concatCode %>_richmessage" style="display:none; margin-bottom: 20px;" class="row">
			  	<%-- 花旗 判斷 webline 與 web 圖文--%>
			  	    <%if(c.getCode().equals("webline")) {;%>
				    <%-- 單圖文輸入區 --%>
				    <div class="col-md-7">
				    <input type="hidden" name="chCode" value="webLine">
					    <select name="<%= concatCode %>_mkey" class="form-control">
					      <option value=""><bean:message key='global.select'/></option>
					      <% List<com.intumit.solr.robot.connector.webline.RichMessage> allRM = com.intumit.solr.robot.connector.webline.RichMessage.list(t.getId(), false);
					    	 com.intumit.solr.robot.connector.webline.RichMessage currRm = null;
					    	 String link = "";
					         for(com.intumit.solr.robot.connector.webline.RichMessage rm : allRM) {
					        	boolean isCurrRm = (mca != null && rm.getMkey().equals(mca.getRichMessageMKey()));
					      		if (isCurrRm) {
					      			currRm = rm;
					      			link = request.getContextPath() + "/wiseadm/" + editorDir + "/qaRichMessageList.jsp#" + currRm.getId();
					      		}
					      	 %>
					         <option data-id="<%= rm.getId() %>" value="<%=rm.getMkey()%>" <%= isCurrRm ? "selected" : ""%>><%=StringEscapeUtils.escapeHtml3(rm.getMsgName())%></option>
					      <% } %>
					    </select>
				    </div>
				    <div class="col-md-5">
					  <a target="_blank" href="<%=link%>" rel="noopener noreferrer" class="btn btn-md btn-success btn<%= rmItemNamePrefix %>OpenEditor" title="開啟視覺化編輯"><span class='glyphicon glyphicon-wrench'></span></a>   	
					</div>
			  	</div>
			  	<div id="<%= concatCode %>_multiple_richmessages" style="display:none; margin-bottom: 20px;">
				    <%-- 多圖文輸入區 --%>
				  	<textarea name="<%= concatCode %>_multiple_richmessages" data-code="<%= concatCode %>" style="display:none;"><%= ctcfg == null ? "{}" : ctcfg.toString(2) %></textarea>
				    <div class="row">
				    	<div class="col-md-12">
				    	<ul class="line-message-ul col-md-6">
				    		<% for (int li=0; li < 5; li++) { %>
				    			<li class="line-message-li" data-code="<%= concatCode %>" data-message-idx="<%= li %>" >訊息<%= (li+1) %>. <span class='text'></span><a class="btnRMConfig<%= typeSuffix %>"><span class="glyphicon glyphicon-cog pull-right" aria-hidden="true"></span></a></li>
				    		<% } %>
				    	</ul>
				    	</div>
				    </div>
			  	</div>  	    
			  	    <%}else{ %>
			  	     <%-- 單圖文輸入區 --%>
				    <div class="col-md-7">
				    	<input type="hidden" name="chCode" value="webRM">
					    <select name="<%= concatCode %>_mkey" class="form-control">
					      <option value=""><bean:message key='global.select'/></option>
					      <% List<com.intumit.solr.robot.connector.web.RichMessage> allRM = com.intumit.solr.robot.connector.web.RichMessage.list(t.getId(), false);
				        	 com.intumit.solr.robot.connector.web.RichMessage currRm = null;
					    	 String link = "";   
					    	 for(com.intumit.solr.robot.connector.web.RichMessage rm : allRM) {
					    		boolean isCurrRm = (mca != null && rm.getMkey().equals(mca.getRichMessageMKey()));
						     	if (isCurrRm) {
						      		currRm = rm;
						      		link = request.getContextPath() + "/wiseadm/" + editorDir + "/qaRichMessageList.jsp#" + currRm.getId();
						      	}
					      	 %>
					         <option data-id="<%= rm.getId() %>" value="<%=rm.getMkey()%>" <%= isCurrRm ? "selected" : ""%>><%=StringEscapeUtils.escapeHtml3(rm.getMsgName())%></option>
					      <% } %>
					    </select>
				    </div>
				    <div class="col-md-5">
					   <a target="_blank" href="<%=link%>" rel="noopener noreferrer" class="btn btn-md btn-success btn<%= rmItemNamePrefix %>OpenEditor" title="開啟視覺化編輯"><span class='glyphicon glyphicon-wrench'></span></a>  	
					</div>
			  	</div>
			  	<div id="<%= concatCode %>_multiple_richmessages" style="display:none; margin-bottom: 20px;">
				    <%-- 多圖文輸入區 --%>
				  	<textarea name="<%= concatCode %>_multiple_richmessages" data-code="<%= concatCode %>" style="display:none;"><%= ctcfg == null ? "{}" : ctcfg.toString(2) %></textarea>
				    <div class="row">
				    	<div class="col-md-12">
				    	<ul class="line-message-ul col-md-6">
				    		<% for (int li=0; li < 5; li++) { %>
				    			<li class="line-message-li" data-code="<%= concatCode %>" data-message-idx="<%= li %>" >訊息<%= (li+1) %>. <span class='text'></span><a class="btnRMConfig<%= typeSuffix %>"><span class="glyphicon glyphicon-cog pull-right" aria-hidden="true"></span></a></li>
				    		<% } %>
				    	</ul>
				    	</div>
				    </div>
			  	</div>
			  	<%} %>
    		</div>
    		<%
		    if (QAChannelType.RICH_TEXT == ctype) {
    		%>
				<script src="<%=request.getContextPath()%>/ckeditor/ckeditor.js"></script>
				<script>
				CKEDITOR.config.baseHref = '<%= StringEscapeUtils.escapeEcmaScript(contextPath) %>/';
				CKEDITOR.config.language = '<%=locale.getLanguage()%>';
				CKEDITOR.config.extraPlugins='simpleuploads';
				//CKEDITOR.config.removeButtons = "addFile";
				CKEDITOR.replace('<%= concatCode %>', {
					filebrowserUploadUrl : '<%= StringEscapeUtils.escapeEcmaScript(contextPath) %>/wiseadm/ckeditorUpload.jsp'
				});
				CKEDITOR.on('dialogDefinition', function(ev) {
					var dialogName = ev.data.name;
					var dialogDefinition = ev.data.definition;
					if(dialogName == 'link') {
						var targetTab = dialogDefinition.getContents('target');
						var advancedTab = dialogDefinition.getContents('advanced');
						var linkTargetField = targetTab.get('linkTargetType');
						var advCssField = advancedTab.get('advCSSClasses');
						linkTargetField['default'] = '_blank';
						advCssField['default'] = 'answer-link';
					}
				});
				$(document).ready(function() {
					// A hack for update textarea when using ajax to save QA
					saveHook.push(function() {
						$textarea = $('#<%= concatCode %>');
						$textarea.val(CKEDITOR.instances['<%= concatCode %>'].getData());
					});
				});
				</script>
			<%
		    }
		} else if (QAChannelType.ROBOT == ctype) {
%>
			<div class="col-sm-12">
			<input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= MultiChannelAnswer.ROBOT_ANSWER_GENERAL %>" 
			<% if(mca == null || (mca != null && (mca.getAnswerType() == null || 
				(mca.getAnswerType() != null && mca.getAnswerType().equals(MultiChannelAnswer.ROBOT_ANSWER_GENERAL))))) { %> checked <% } %>
			><bean:message key='robot.answer.general'/>
			<input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= MultiChannelAnswer.ROBOT_ANSWER_ADVANCE %>"
			<% if(mca != null && mca.getAnswerType() != null && mca.getAnswerType().equals(MultiChannelAnswer.ROBOT_ANSWER_ADVANCE)) { %> checked <% } %>
			><bean:message key='robot.answer.advance'/>
			<br>
			<br>
			<div id="div_<%= concatCode %>_voice" 
			<% if(!(mca != null && mca.getAnswerType() != null && mca.getAnswerType().equals(MultiChannelAnswer.ROBOT_ANSWER_ADVANCE))) { %> style="display:none" <% } %>
			>
			<%
			String answerVoice = mca == null ? "" : mca.getAnswerVoice() == null ? "" : mca.getAnswerVoice();
			%>
				<bean:message key='robot.answer.content.voice'/>
				<br>
				<textarea class="form-control" rows="1" name="<%= concatCode %>_voice"><%= StringEscapeUtils.escapeHtml4(answerVoice) %></textarea>
				<br>
			</div>
			<bean:message key='robot.answer.content'/>	
			<textarea class="form-control" rows="5" name="<%= concatCode %>"><%= StringEscapeUtils.escapeHtml4(answer) %></textarea>
			<input type="hidden" name="<%= concatCode %>_hide" value="<%= StringEscapeUtils.escapeHtml4(answer) %>">
			<div id="div_<%= concatCode %>_mood" 
			<% if(!(mca != null && mca.getAnswerType() != null && mca.getAnswerType().equals(MultiChannelAnswer.ROBOT_ANSWER_ADVANCE))) { %> style="display:none" <% } %>
			>
				<br>
				<bean:message key='robot.answer.content.mood'/><br>
				<input type="radio" name="<%= concatCode %>_mood" value="<%= MultiChannelAnswer.ROBOT_MOOD_CURIOUS %>"
				<% if(mca == null || (mca != null && (mca.getAnswerMood() == null || 
					(mca.getAnswerMood() != null && mca.getAnswerMood().equals(MultiChannelAnswer.ROBOT_MOOD_CURIOUS))))) { %> checked <% } %>
				><bean:message key='robot.answer.mood.curious'/>
				<input type="radio" name="<%= concatCode %>_mood" value="<%= MultiChannelAnswer.ROBOT_MOODR_HAPPY %>"
				<% if(mca != null && mca.getAnswerMood() != null && mca.getAnswerMood().equals(MultiChannelAnswer.ROBOT_MOODR_HAPPY)) { %> checked <% } %>
				><bean:message key='robot.answer.mood.happy'/>
				<input type="radio" name="<%= concatCode %>_mood" value="<%= MultiChannelAnswer.ROBOT_MOOD_JOY %>"
				<% if(mca != null && mca.getAnswerMood() != null && mca.getAnswerMood().equals(MultiChannelAnswer.ROBOT_MOOD_JOY)) { %> checked <% } %>
				><bean:message key='robot.answer.mood.joy'/>
			</div>
			</div>
<%
		} else if (QAChannelType.PLAIN_TEXT_WITH_VOICE == ctype) {
			String answerVoice = mca == null ? "" : mca.getAnswerVoice() == null ? "" : mca.getAnswerVoice();
			%>
			<div class="col-sm-12">
				<bean:message key='robot.answer.content.voice'/>
				<br>
				<textarea class="form-control" rows="1" name="<%= concatCode %>_voice"><%= StringEscapeUtils.escapeHtml4(answerVoice) %></textarea>
				<br>
				<bean:message key='robot.answer.content'/>	
				<textarea class="form-control" rows="5" name="<%= concatCode %>"><%= StringEscapeUtils.escapeHtml4(answer) %></textarea>
				<input type="hidden" name="<%= concatCode %>_hide" value="<%= StringEscapeUtils.escapeHtml4(answer) %>">
			</div>
			<%
		}
		else if (QAChannelType.LINE == ctype) {
			%>
			<div class="col-sm-12">
			  <%-- 選擇要原始輸入 or 單圖文 or 多圖文 --%>
			  <% if (c.getSupportMultiRichMessages()) { %>
			  <input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= type %>_answer_text" <%= (mca == null || ((StringUtils.isNotBlank(mca.getAnswer()) || StringUtils.isBlank(mca.getRichMessageMKey())) && ctcfg == null)) ? "checked" : "" %>> <bean:message key="line.answer.type.plaintext"/>
			  &nbsp;&nbsp;&nbsp;&nbsp;<input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= type %>_answer_rich_message" <%= (mca != null && ctcfg == null && StringUtils.isNotBlank(mca.getRichMessageMKey())) ? "checked" : ""  %>> <bean:message key="line.answer.type.richmessage"/>
			  &nbsp;&nbsp;&nbsp;&nbsp;<input type="radio" name="<%= concatCode + typeSuffix %>" value="<%= type %>_answer_multiple_rich_messages" <%= (mca != null && ctcfg != null) ? "checked" : ""  %>> <bean:message key="line.answer.type.multiple.richmessages"/>
			  <% } %>
			  
			  <div id="<%= concatCode %>_original" style="display:none; margin-bottom: 20px;">
			    <%-- 原有的輸入區 --%>
			    <textarea class="form-control" rows="8" name="<%= concatCode %>"><%= mca == null ? "" : StringEscapeUtils.escapeHtml3(mca.getAnswer()) %></textarea>
			  </div>
			  
			  <div id="<%= concatCode %>_richmessage" style="display:none; margin-bottom: 20px;" class="row">
			    <%-- 單圖文輸入區 --%>
			    <div class="col-md-7">
				    <select name="<%= concatCode %>_mkey" class="form-control">
				      <option value=""><bean:message key='global.select'/></option>
				      <% List<RichMessage> allRM = RichMessage.list(t.getId(), false);
				         for(RichMessage rm : allRM) {
				      	 %>
				         <option data-id="<%= rm.getId() %>" value="<%=rm.getMkey()%>" <%= (mca != null && rm.getMkey().equals(mca.getRichMessageMKey())) ? "selected" : ""%>><%=StringEscapeUtils.escapeHtml3(rm.getMsgName())%></option>
				      <% } %>
				    </select>
			    </div>
			    <div class="col-md-5">
				   	<a target="_blank" rel="noopener noreferrer" class="btn btn-md btn-success btn<%= rmItemNamePrefix %>OpenEditor" title="開啟視覺化編輯"><span class='glyphicon glyphicon-wrench'></span></a>
				</div>
			  </div>
			  
			  <div id="<%= concatCode %>_multiple_richmessages" style="display:none; margin-bottom: 20px;">
			    <%-- 多圖文輸入區 --%>
			  	<textarea name="<%= concatCode %>_multiple_richmessages" data-code="<%= concatCode %>" style="display:none;"><%= ctcfg == null ? "{}" : ctcfg.toString(2) %></textarea>
			    <div class="row">
			    	<div class="col-md-12 alert alert-danger">
			    		<bean:message key="line.answer.type.multiple.richmessages.notice" />
			    	</div>
			    </div>
			    <div class="row">
			    	<div class="col-md-12">
			    	<ul class="line-message-ul col-md-6">
			    		<% for (int li=0; li < 5; li++) { %>
			    			<li class="line-message-li" data-code="<%= concatCode %>" data-message-idx="<%= li %>" >訊息<%= (li+1) %>. <span class='text'></span><a class="btnRMConfig<%= typeSuffix %>"><span class="glyphicon glyphicon-cog pull-right" aria-hidden="true"></span></a></li>
			    		<% } %>
			    	</ul>
			    	</div>
			    </div>
			  </div>
              <br>
              <div><a class="btn btn-info" onclick='sendToMeLINE("<%=c.getCode() %>")' <%if (!isAlreadyBinded) { %> disabled <% } %>>傳送到我的LINE</a></div>
			</div>
			<br>
			<%
		}	
		else {
%>
		    <div class="col-sm-12">
				<textarea class="form-control" id="<%= concatCode %>" name="<%= concatCode %>" rows="8"><%= StringEscapeUtils.escapeHtml4(answer) %></textarea>
    			<input type="hidden" name="<%= concatCode %>_hide" value="<%= StringEscapeUtils.escapeHtml4(answer) %>">	
    		</div>
<%
		}
  
    	String extraParamsStr = StringUtils.trimToNull(mca != null ? mca.getExtraParameters() : null);
    	%>
		<div class="col-sm-12">
	    	<textarea class="form-control hide" rows="5" id="<%= concatCode %>_extraParams" name="<%= concatCode %>_extraParams"><%= StringEscapeUtils.escapeHtml4(extraParamsStr) %></textarea>
	
	    	<div id='<%= concatCode %>_extra_params_editor_holder' class='row'></div>
		    <p class="help-block"><bean:message key='qa.extra.params.ex'/></p>
		    <script>
		    (function(myeditor) {
		      // Initialize the editor
		      myeditor.on('ready', function() {
		    	  myeditor.on('change',function() {
			          $('#<%= concatCode %>_extraParams').text(JSON.stringify(myeditor.getValue()));
			      });
		      });
		    })(new JSONEditor(document.getElementById('<%= concatCode %>_extra_params_editor_holder'),{
		        // Enable fetching schemas via ajax
		        ajax: true,
		        theme: 'bootstrap3',
		        // The schema for the editor
		        schema: {
		          $ref: "get-jsonschema-ajax.jsp?type=qa-extra",
		        },
	
		        // Seed the form with a starting value
		        startval: <%= extraParamsStr != null ? extraParamsStr : "{}" %>
		      }));
		    </script>
		</div>
  </div>
	<hr/>
	<%
	}
	%>
    </div>
<%
}
%>
</div>
<!-- tab-content end -->
</div> <%-- Tab Border End --%>

  <div class="form-group">
    <div class="col-sm-offset-1 col-sm-9">
    <div class="col-sm-12">
	<select name="choosedQAPlugIn" class="form-control">
	<option value=""><bean:message key='global.without'/><bean:message key='advanced.answer'/></option>
	<%
	String answerPluginId = (String) doc.getFieldValue(QA.FN_PLUGIN_ID);
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
			value="<%= StringEscapeUtils.escapeHtml4(pId) %>"><%= StringEscapeUtils.escapeHtml4(MessageUtil.getMessage(locale, p.getName())) %></option>
			<%
		}
	}
	%>
	</select>
	<div style="height: 0.5em;"></div>
	<%
	for(QAPlugin p : QAPlugins.list()){
	%>
	<div id="qa-plugin-panel-<%= StringEscapeUtils.escapeHtml4(p.getId()) %>" style="display:none;">
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
  </div>

  
</div> <%-- Tab Answers END --%>
  
<div class="tab-pane fade in" role="tabpanel" id="others-tab" aria-labelledby="others">
  <%
    	String extraParamsStr = (String) doc.getFieldValue(QA.FN_EXTRA_PARAMS);
  %>
  <div class="form-group">
    <div class="col-sm-offset-1 col-sm-9">
  
    	<textarea class="form-control hide" rows="5" id="extraParams" name="extraParams"><%= StringEscapeUtils.escapeHtml4(extraParamsStr) %></textarea>

    	<div id='extra_params_editor_holder' class='row'></div>
	    <p class="help-block"><bean:message key='qa.extra.params.ex'/></p>
	    <script>
	      var starting_value = <%= extraParamsStr != null ? extraParamsStr : "{}" %>;
	      // Initialize the editor
	      var editor = new JSONEditor(document.getElementById('extra_params_editor_holder'),{
	        // Enable fetching schemas via ajax
	        ajax: true,
	        theme: 'bootstrap3',
	        iconlib: 'bootstrap3',

	        // The schema for the editor
	        schema: {
	          $ref: "get-jsonschema-ajax.jsp?type=qa-extra",
	        },

	        // Seed the form with a starting value
	        startval: starting_value
	      });

	      editor.on('ready', function() {
	      editor.on('change',function() {
	          $('#extraParams').text(JSON.stringify(editor.getValue()));
	      });
	      });
	    </script>
	 </div>
  </div>
  <br>
  <br>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='triggered.by.events'/></label>
    <div class="col-sm-10">
    
    	<select id='choosedEventTypes' name='choosedEventTypes' style='width: 100%;' multiple="multiple">
    	<%
		List<EventType> crossChannelEts = EventType.listByChannel(t.getId(), "", null);
		JSONArray triggerVals = new JSONArray(qa.getTriggeredByEventTypes());
    	%>
    	<optgroup label="全頻道">
    		<% for (EventType et: crossChannelEts) {%>
			<option value="<%= et.getCode() %>"><%= et.getCode() %> (全頻道)</option>
	    	<% } %>
    	</optgroup>
    	<%
    	for (QAChannel ch: QAChannel.list(t.getId())) {
    		List<EventType> ets = EventType.listByChannel(t.getId(), ch.getCode(), null);
	    	%>
	    	<optgroup label="<%= ch.getName() %>">
	    		<% for (EventType et: ets) {%>
				<option value="<%= et.getCode() %>"><%= et.getCode() %> (<%= et.getChannel() %>)</option>
		    	<% } %>
	    	</optgroup>
	    	<%
    	}
    	%>
    	</select>
    	<script>
    	$("#choosedEventTypes").select2();
    	$("#choosedEventTypes").val(<%= triggerVals.toString() %>).trigger("change");
    	</script>
    </div>
  </div>
  <br>
  <br>
  <div class="form-group">
    <label for="inputQuestion" class="col-sm-2 control-label"><bean:message key='custom.script'/></label>
    <div class="col-sm-10">
	<textarea class="form-control" rows="5" name="customScript" id="customScript"><%= StringEscapeUtils.escapeHtml4(customScript) %></textarea>
    </div>
  </div>
</div> <%-- Tab Others END --%>
  
  
  
  </div> <%-- qaDataEditorTabContent END --%>
  
</form>
</div>

<div id="set-kid-panel" style="display: none;">
	<form class="pf-form pform_custom" name="set-kid-form" action="#" method="post">
		<div class="pf-element pf-heading">
			<h4><bean:message key='knowledge.num.assign'/></h4>
			<p class="pf-msg"></p>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label text-primary"><bean:message key='knowledge.num'/></span>
				<input class="pf-field" type="text" name="kid" />
				<input class="pf-button btn btn-primary"  data-loading-text="<bean:message key='global.check'/>" type="submit" name="submit" value="<bean:message key='global.recommend'/>" />
				<input class="pf-button btn btn-primary btn-cancel"  type="button" name="cancel" value="<bean:message key='global.cancel'/>" />
			</label>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label text-primary"></span>
			</label>
			<div name="message-body" class="set-kid-message"></div>
		</div>
	</form>
</div>
<div id="check-dialog" title="<bean:message key='similar.problems'/>" style="display:none;">
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="check-list" class="table table-striped table-hover">
  	</table>
  </div>
</div>
<div id="suggest-template-dialog" title="<bean:message key='apply.template'/>" style="display:none;">
  <div class="pull-right" style="margin: 10px;">
  <bean:message key='knowledge.point'/>：<input type="text" id="keywordForTemplate" name="keywordForTemplate">&nbsp;&nbsp;
  <button id='btnApplyToggledTemplate' data-loading-text="<bean:message key='global.applying'/>" class='btn btn-default'><bean:message key='apply.selected.template'/><br><div class='testScore'></div></button>
  </div>
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="template-list" class="table table-striped table-hover">
	<tr>
		<th><bean:message key='template.name'/></th>
		<th><bean:message key='template.description'/></th>
		<th><bean:message key='operation'/></th>
	</tr>
  	</table>
  </div>
</div>
<div id="test-dialog" title="<bean:message key='global.test.record'/>" style="display:none;">
  <div class="pull-right" style="margin: 10px;">
  <input type="checkbox" id="checkIgnoreFlag0" name="checkIgnoreFlag0" checked><bean:message key='ignore.not.select'/>&nbsp;
  <button id='btnAutoTestAll' data-loading-text="<bean:message key='global.testing'/>" class='btn btn-default'><bean:message key='test.all'/><br><div class='testScore'></div></button>
  </div>
  <div style="background-color: white;">
  	<span class="ui-helper-hidden-accessible"><input type="text"/></span>
  	<table id="test-list" class="table table-striped table-hover">
	<tr>
		<th><bean:message key='global.evaluation'/></th>
		<th class='col-md-2'><bean:message key='test.question'/></th>
		<th><bean:message key='test.expected.answer.number'/></th>
		<th><bean:message key='robot.answer.number'/></th>
		<th class='col-md-4'><bean:message key='robot.answer'/></th>
		<th><bean:message key='global.confidence.level'/></th>
		<th><bean:message key='test.time'/></th>
		<th class='col-md-2'><bean:message key='operation'/></th>
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
<div id="magic-dialog" title="<bean:message key='robot.magicBtn.title'/>" style="display:none; z-index:999; ">
</div>
<div id="batch_nlp_panel" style="display: none;">
	<div class="pf-element pf-heading">
		<h4><bean:message key='execute.batchNlp'/></h4>
		<p class="pf-msg"></p>
	</div>
	<div class="pf-element">
		<label>
			<textarea class="pf-field" rows="10" cols="50" id="batchNlpTextArea"></textarea>
			<button class="pf-button btn btn-success" id="btnBatchNlpAndSave1"><bean:message key='execute.batchNlpAndSave'/>(斷)</button>
            <button class="pf-button btn btn-success" id="btnBatchNlpAndSave2"><bean:message key='execute.batchNlpAndSave'/>(斷2)</button>
		</label>
	</div>
</div>
<div id="autoGenerateAlt-dialog" title="預覽套用結果" style="display:none; z-index:999; background-color: white;">
</div>

<%
{
Set<String> dupType = new HashSet<String>();
for (QAChannel ch: channels) {
	if (!ch.getSupportMultiRichMessages()) continue;
	
	String type = StringUtils.lowerCase(ch.getType().name());
	String typeSuffix = "_" + type + "type";
	
	if (dupType.contains(type)) continue;
	else dupType.add(type);
	
	String rmItemNamePrefix = type + "MessageConfig";
%>
<div id="<%= rmItemNamePrefix %>-dialog" title="Richmessage Config" style="display:none; z-index:999;" >
  <input type="radio" name="<%= rmItemNamePrefix %>-itemtype" value=""> Disable
  &nbsp;&nbsp;&nbsp;&nbsp;<input type="radio" name="<%= rmItemNamePrefix %>-itemtype" value="<%= type %>_answer_text"> <bean:message key="line.answer.type.plaintext"/>
  &nbsp;&nbsp;&nbsp;&nbsp;<input type="radio" name="<%= rmItemNamePrefix %>-itemtype" value="<%= type %>_answer_rich_message"> <bean:message key="line.answer.type.richmessage"/>
  <br>
  <br>
  <input type="hidden" name="<%= rmItemNamePrefix %>-concatCode">
  <input type="hidden" name="<%= rmItemNamePrefix %>-idx">
  
  <textarea class="col-md-12" name="<%= rmItemNamePrefix %>-text"></textarea>
  <div class="col-md-12">
	  <div class="col-md-10">
		  <select name="<%= rmItemNamePrefix %>-mkey" class="form-control">
		    <option value="">-- <bean:message key='global.please'/><bean:message key='global.select'/> --</option>
		    <% 
		    /* 
		     * 用 Reflection 找到對應的 RichMessage (前提是 class name 都是 RichMessage，只有 package 不同)
		     * PLAIN_TEXT / RICH_TEXT 都是對應到 web.RichMessage
		     * 每個 RichMessage instance 需要 id / mkey / msgName 才能列表
		     */
		    try {
		    	String packageName = "com.intumit.solr.robot.connector." + type;
		    	if (ch.getType() == QAChannelType.PLAIN_TEXT || ch.getType() == QAChannelType.RICH_TEXT && ch.getCode().equals("webline")) {
		    		packageName = "com.intumit.solr.robot.connector.webline";
		    	}else{
		    		packageName = "com.intumit.solr.robot.connector.web";
		    	}
			    Class clazz = Class.forName(packageName + ".RichMessage");
			    java.lang.reflect.Method method = clazz.getMethod("list", int.class, Boolean.class);
			    List allRM = (List)method.invoke(null, t.getId(), false);
			    for (Object rm : allRM) {
			    %>
			       <option data-id="<%= PropertyUtils.getProperty(rm, "id") %>" value="<%=PropertyUtils.getProperty(rm, "mkey")%>"><%=StringEscapeUtils.escapeHtml3((String)PropertyUtils.getProperty(rm, "msgName"))%></option>
			    <% 
			    } 
		    }
		    catch (Exception ex) {
		    	System.out.println("RichMessage of channel[" + type + "] got error:" + ex.getMessage());
		    }
		    %>
		  </select>
	  </div>
	  <div class="col-md-2">
	  	<a target="_blank" rel="noopener noreferrer" class="btn btn-md btn-success btn<%= rmItemNamePrefix %>OpenEditor" title="開啟視覺化編輯"><span class='glyphicon glyphicon-wrench'></span></a>
	  </div>
  </div>
</div>
<%
}}
%>

<div class='loading-panel'></div>
<script>
var QABuildStatusChecker;
var skpShown = false;
var bnpShown = false;
var skp, bnp, tplData, systemNlp = '', $userNlp = '';
var evaluationLogEntries = {};

var altDataJson = <%= altDataTableArray.toString() %>;

// 移除特定 array item
var redrawDT = function() {
	$dt = $('.altTable').DataTable();
	$dt.data().clear();
	$dt.rows.add(altDataJson).draw();
}

var altJsonToTextArea = function() {
	newText = "";

	for (var i=0; i < altDataJson.length; i++) {
		if (typeof(altDataJson[i]) != 'undefined') {
			json = JSON.parse(JSON.stringify(altDataJson[i]));
			if (json.hasOwnProperty('alt')) {
				newText += json.alt + "\t // ";
				delete json.alt;
				delete json.offset;
				newText += JSON.stringify(json) + "\n";
			}
		}
	}
	return newText;
}

var qaaltList = [], qatestcaseList = [];
$(document).ready(function() {
    <%-- 每個 Channel 多圖文的設定統一處理 --%>
    <%
    Set<String> dupType = new HashSet<String>();
    
    for (QAChannel ch: channels) {
    	if (!ch.getSupportMultiRichMessages()) continue;
    	
    	String type = StringUtils.lowerCase(ch.getType().name());
//     	if (dupType.contains(type)) continue;
//     	else dupType.add(type);
    	
    	String typeSuffix = "_" + type + "type";
    	String rmItemNamePrefix = type + "MessageConfig";
    	String editorDir = (ch.getType() == QAChannelType.PLAIN_TEXT || ch.getType() == QAChannelType.RICH_TEXT && ch.getCode().equals("web")) ? "webRM" : "webLine";
    	%>
	    $('input[type=radio][name$=<%= typeSuffix %>]').change(function() {
		  	var radioName = this.name;
	   		var clickValue = $('[name=' + radioName + ']:checked').val();
	   		if (clickValue === '<%= type %>_answer_text') {
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_original').show();
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_richmessage').hide();
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_multiple_richmessages').hide();
	   		}
	   		else if (clickValue === '<%= type %>_answer_rich_message') {
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_original').hide();
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_richmessage').show();
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_multiple_richmessages').hide();
	   		}
	   		else {
	   			// LINE_ANSWER_MULTIPLE_RICH_MESSAGES
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_original').hide();
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_richmessage').hide();
	   			$('#' + radioName.replace('<%= typeSuffix %>', '') + '_multiple_richmessages').show();
	   		}
	    });
	    
		    $('select[name$=_mkey]').change(function() {
		    	var dir = $(this).parent("div").find("input[name=chCode]").val();
		   		var op = $("option:selected", this);
		   		var rmId = $(op).attr('data-id');
		    	$mkeyLink = $(this).parent().parent().find('.btn<%= rmItemNamePrefix %>OpenEditor');

		       	if (typeof(rmId) != 'undefined') {
		       		$mkeyLink.attr("href", "<%= request.getContextPath() %>/wiseadm/"+dir+"/qaRichMessageList.jsp#" + rmId);
		       		$mkeyLink.show();
		       	}
		       	else {
		       		$mkeyLink.hide();
		       	}
		    });	
	    
	    
	    <%-- 多圖文 dialog 當中的 radio button --%>
	    $('input[type=radio][name=<%= rmItemNamePrefix %>-itemtype]').change(function() {
	    	$this = $(this);
	    	$div = $this.closest('div');
	    	$div.find('[name=<%= rmItemNamePrefix %>-text]').attr('disabled', false);
	    	$div.find('[name=<%= rmItemNamePrefix %>-mkey]').attr('disabled', false);
	    	
	    	var val = $('input[type=radio][name=<%= rmItemNamePrefix %>-itemtype]:checked').val();
	    	if (val == '<%= type %>_answer_text') {
	        	$div.find('[name=<%= rmItemNamePrefix %>-text]').show();
	        	$div.find('[name=<%= rmItemNamePrefix %>-mkey]').hide();
	        	$div.find('.btn<%= rmItemNamePrefix %>OpenEditor').hide();
	    	}
			else if (val === '<%= type %>_answer_rich_message') {
	        	$div.find('[name=<%= rmItemNamePrefix %>-text]').hide();
	        	$div.find('[name=<%= rmItemNamePrefix %>-mkey]').show();
	        	$div.find('.btn<%= rmItemNamePrefix %>OpenEditor').hide();
			}
			else {
	        	$div.find('[name=<%= rmItemNamePrefix %>-text]').attr('disabled', true);
	        	$div.find('[name=<%= rmItemNamePrefix %>-mkey]').attr('disabled', true);
	        	$div.find('[name=<%= rmItemNamePrefix %>-text]').show();
	        	$div.find('[name=<%= rmItemNamePrefix %>-mkey]').show();
	        	$div.find('.btn<%= rmItemNamePrefix %>OpenEditor').show();
			}
	    });
	    
	    $('textarea[name$=_multiple_richmessages]').change(function() {
	    	$this = $(this);
	    	concateCode = $this.attr('data-code');
	    	msgsCfg = JSON.parse($this.val());
	    	
	    	if (msgsCfg.hasOwnProperty('messages')) {
	    		msgsArr = msgsCfg.messages;
	    		
	    		for (var i=0; i < msgsArr.length; i++) {
	    			msgCfg = msgsArr[i];
	    			li = $('.line-message-li[data-code=' + concateCode + '][data-message-idx=' + i + ']');
	    			
	    			if (msgCfg.type == '') {
	    				li.find('.text').text('');
	    				li.css('background-color', 'lightgray');
	    			}
	    			else if (msgCfg.type == '<%= type %>_answer_text') {
	    				li.find('.text').text(msgCfg.text.substr(0, 20) + (msgCfg.text.length > 20 ? "..." : ""));
	    				li.css('background-color', 'lightblue');
	    			}
	    			else if (msgCfg.type == '<%= type %>_answer_rich_message') {
	    				li.find('.text').text(msgCfg.mkey);
	    				li.css('background-color', 'lightyellow');
	    			}
	    		}
	    	}
	    });
	    
	    $('[name$=<%= typeSuffix %>]').change();
	    $('textarea[name$=_multiple_richmessages]').change();
	    
	    $('.btnRMConfig<%= typeSuffix %>').click(function() {
	    	$this = $(this);
	    	$li = $this.closest('li');
	    	concatCode = $li.attr('data-code');
	    	idx = parseInt($li.attr('data-message-idx'));
	    	
	    	msgCfg = null;
	    	msgsCfg = JSON.parse($('textarea[name=' + concatCode + '_multiple_richmessages]').val());
	    	if (msgsCfg.hasOwnProperty('messages')) {
	    		arr = msgsCfg.messages;
	    		if (arr.length > idx) {
	    			msgCfg = arr[idx];
	    		}
	    	}
	    	
	    	if (!msgCfg) {
	    		msgCfg = { type: '', text: '', mkey: ''};
	    	}
	    	
	    	$dialog = $('#<%= rmItemNamePrefix %>-dialog');
	    	$concatCode = $dialog.find('[name=<%= rmItemNamePrefix %>-concatCode]');
	    	$idx = $dialog.find('[name=<%= rmItemNamePrefix %>-idx]');
	    	$text = $dialog.find('[name=<%= rmItemNamePrefix %>-text]');
	    	$mkey = $dialog.find('[name=<%= rmItemNamePrefix %>-mkey]');
	    	$mkeyLink = $dialog.find('.btn<%= rmItemNamePrefix %>OpenEditor');
	    	$mkey.off('change');
	    	$mkey.on('change', function(e) {
	    		var op = $("option:selected", this);
	    		var rmId = $(op).attr('data-id');

	        	if (typeof(rmId) != 'undefined') {
	        		$mkeyLink.attr("href", "<%= request.getContextPath() %>/wiseadm/<%= editorDir %>/qaRichMessageList.jsp#" + rmId);
	        		$mkeyLink.show();
	        	}
	        	else {
	        		$mkeyLink.hide();
	        	}
	    	});
	    	
	    	$dialog.find('input[name=<%= rmItemNamePrefix %>-itemtype][value="' + msgCfg.type + '"]').prop('checked', true);
	    	$concatCode.val(concatCode);
	    	$idx.val(idx);
	    	$text.val(msgCfg.text);
	    	$mkey.val(msgCfg.mkey);
	    	
	    	$dialog.find('input[name=<%= rmItemNamePrefix %>-itemtype]').change();
	    	$mkey.change();
	    	
	    	$dialog.dialog('open');
	    });
	    
		<%-- Initialize JQuery Dialog --%>
	    $( "#<%= rmItemNamePrefix %>-dialog").dialog({
	    	minWidth: 500,
	    	minHeight: 300,
	    	height: 300,
	    	autoOpen: false,
	    	buttons: {
	            "OK": function() {
	            	msgCfg = {};
	            	
	            	$dialog = $('#<%= rmItemNamePrefix %>-dialog');
	            	concatCode = $dialog.find('[name=<%= rmItemNamePrefix %>-concatCode]').val();
	            	idx = parseInt($dialog.find('[name=<%= rmItemNamePrefix %>-idx]').val());
	            	
	            	$text = $dialog.find('[name=<%= rmItemNamePrefix %>-text]');
	            	$mkey = $dialog.find('[name=<%= rmItemNamePrefix %>-mkey]');
	            	
	            	msgCfg.type = $dialog.find('input[name=<%= rmItemNamePrefix %>-itemtype]:checked').val();
	            	msgCfg.text = $text.val();
	            	msgCfg.mkey = $mkey.val();
	            	
	            	msgsCfg = JSON.parse($('textarea[name=' + concatCode + '_multiple_richmessages]').val());
	            	msgsArr = [];

	            	if (!msgsCfg.hasOwnProperty('messages')) {
	            		msgsCfg.messages = [];
	            	}
	            	if (msgsCfg.hasOwnProperty('messages')) {
	            		msgsArr = msgsCfg.messages;
	                	for (var i=0; i < 5; i++) {
	                		if (msgsArr.length <= i) {
	                			msgsArr[i] = { type: '', text: '', mkey: ''};
	                		}
	                	}
	            	}
	            	
	            	msgsArr[idx] = msgCfg;
	            	
	            	$('textarea[name=' + concatCode + '_multiple_richmessages]').val(JSON.stringify(msgsCfg));
	            	$('textarea[name=' + concatCode + '_multiple_richmessages]').change();
	            	
	            	$dialog.dialog('close');
	            }
	    	}
	    });
    <%
    }
    %>
    
    $('input[type=radio][name^=answer_]').change(function() {
	  	var radioName = this.name;
   		var radioNames = radioName.split("_");
    	if (radioName.startsWith("answer_") && radioName.endsWith("_type") && radioNames.length == 4 && this.value == 'advance') {
    		document.getElementById('div_answer_'+radioNames[1]+'_'+radioNames[2]+'_voice').style.display = 'block';
    		document.getElementById('div_answer_'+radioNames[1]+'_'+radioNames[2]+'_mood').style.display = 'block';
    	} 
    	else if (radioName.startsWith("answer_") && radioName.endsWith("_type") && radioNames.length == 4 && this.value == 'general') {
    		document.getElementById('div_answer_'+radioNames[1]+'_'+radioNames[2]+'_voice').style.display = 'none';
    		document.getElementById('div_answer_'+radioNames[1]+'_'+radioNames[2]+'_mood').style.display = 'none';
    	}
    });
	
    $('.altTable').on( 'draw.dt', function() {
	    	$this = $(this);
	    	$this.find('.qa-alt').editable({
	        	tpl: '<input type="text" style="width: 500px">',
	        	emptytext: '{{Empty}}',
	        	display: function(value) {
	        		$(this).html("");
	        		var myCodeMirror = CodeMirror($(this)[0], {
	        			  value: value,
	        			  mode:  "qaalt",
	        			  readonly: true
	        			});
	        		qaaltList.push(myCodeMirror);
	        	},
	        	success: function(response, newValue) {
	        		$this = $(this);
	        		offset = parseInt($this.attr('data-offset'));
	
	        		for (var i=0; i < altDataJson.length; i++) {
	        			json = altDataJson[i];
	        			if (json.offset == offset) {
	        				json["alt"] = newValue;
	        				json["alt.editor"] = <%= currentAdmin.getId() %>;
	        			}
	        		}
            }
        });
	    	
		$this.find('.qa-alt-testCase').editable({
        	tpl: '<input type="text" style="width: 500px">',
        	emptytext: '{{Empty}}',
        	display: function(value) {
        		if (value != "") {
	        		$(this).html("");
	        		var myCodeMirror = CodeMirror($(this)[0], {
	        			  value: value,
	        			  mode:  "qatestcase",
	        			  readonly: true
	        			});
	        		qaaltList.push(myCodeMirror);
        		}
        	},
        	success: function(response, newValue) {
        		$this = $(this);
        		offset = parseInt($this.attr('data-offset'));

        		for (var i=0; i < altDataJson.length; i++) {
        			json = altDataJson[i];
        			if (json.offset == offset) {
        				json["testCase"] = newValue;
        				json["alt.editor"] = <%= currentAdmin.getId() %>;
        			}
        		}
            }
        });
    });

    $('.altTable').DataTable({
    	data: altDataJson,
    	columns: [
    		{
    			data: 'alt',
    			render: function ( data, type, row ) {
    				return '<a href="#" class="qa-alt" data-offset="' + row.offset + '" data-pk="alt' + row.offset + '" id="alt' + row.offset + '">' + data + '</a>';
    			}
    		},
    		{
    			data: 'testCase',
    			render: function ( data, type, row ) {
    				return '<a href="#" class="qa-alt-testCase" data-offset="' + row.offset + '" data-pk="tc' + row.offset + '" id="tc' + row.offset + '">' + data + '</a>';
    			}
    		},
    		{
    		    data: null,
    		    render: function ( data, type, row ) {
    		        return '<a href="#" data-offset="' + row.offset + '" class="btnDelAltRow"><span class="glyphicon glyphicon-remove"></span></a>'
    		        		+ '&nbsp;<a href="#" data-offset="' + row.offset + '" class="btnSimilarAltRow easterEgg"><span class="glyphicon glyphicon-filter"></span></a>';
    		    }
    		}
    	]
    });
    
 	// Javascript to enable link to tab
    var url = document.location.toString();
    if (url.match('#')) {
    	targetTab = url.split('#')[1];
        $('.nav-tabs a[href="#' + targetTab + '-tab"]').tab('show');
    } //add a suffix

    // Change hash for page-reload
    $('#myTabs li a').on('shown.bs.tab', function (e) {
    	noSuffix = e.target.hash.split('-tab')[0];
        window.location.hash = noSuffix;
		refreshExcludeSentenceCodeMirror();
    });
});

$('#toggle-set-kid-panel').click(toggleSetKidPNotify);
$('#toggleBatchNlpPanel').click(toggleBatchNlpPanel);

$('#btnAdvAltEditor').click(function() {
	$('.altTable').DataTable().destroy();
	$('.altTable').toggle();
	$(this).hide();
	$('textarea[name=question_alt]').val(altJsonToTextArea());
	$('textarea[name=question_alt]').toggle();
	
	var altEditor = CodeMirror.fromTextArea(document.getElementById("question_alt"), {
		   lineNumbers: true,
		   matchBrackets: true,
		   mode: "qaalt",
		 });
	$('#question_alt').data('CodeMirrorInstance', altEditor);
});

$( "#magic-dialog").dialog({
	minWidth: 600,
	minHeight: 600,
	height: 600,
	autoOpen: false
});

$( "#autoGenerateAlt-dialog").dialog({
	minWidth: 600,
	minHeight: 600,
	height: 600,
	autoOpen: false
});


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

function toggleSetKidPNotify() {
	console.log(skpShown);
	if (skpShown) {
		skpShown = false;
		skp.remove();
	}
	else {
		skpShown = true;
		skp = new PNotify({
			    text: $('#set-kid-panel').html(),
			    icon: false,
			    width: '400px',
			    hide: false,
			    confirm: {
			        confirm: true,
			        buttons: [{
			            text: '<bean:message key="global.cancel"/>',
			            addClass: 'hide btn-primary',
			            click: function(notice) {
			            	skpShown = false;
			                notice.remove();
			            }
			        },{
			            text: '<bean:message key="not.need.exist.close"/>',
			            addClass: 'hide btn-primary',
			            click: function(notice) {
			            	skpShown = false;
			                notice.remove();
			            }
			        }
			        ]
			    },
			    before_open: function(PNotify) {
		            // Position this notice in the center of the screen.
		            PNotify.get().css({
		                "top": ($(window).height() / 3) - (PNotify.get().height() / 2),
		                "left": ($(window).width() / 2) - (PNotify.get().width() / 2)
		            });
			    },
			    buttons: {
			        closer: false,
			        sticker: false
			    },
			    insert_brs: false
			});
	}
}

function toggleBatchNlpPanel() {
	if (bnpShown) {
		bnpShown = false;
		bnp.remove();
		$('#batchNlpTextArea').text("");
	}
	else {
		bnpShown = true;
		bnp = new PNotify({
			    text: $('#batch_nlp_panel').html(),
			    icon: false,
			    width: '600px',
			    hide: false,
			    confirm: {
			        confirm: true,
			        buttons: [{
			            text: '<bean:message key="global.close"/>',
			            addClass: 'btn-primary',
			            click: function(notice) {
			            	bnpShown = false;
			                notice.remove();
			            }
			        },{
			            text: '<bean:message key="not.need.exist.close"/>',
			            addClass: 'hide btn-primary',
			            click: function(notice) {
			            	bnpShown = false;
			                notice.remove();
			            }
			        }
			        ]
			    },
			    before_open: function(PNotify) {
		            // Position this notice in the center of the screen.
		            PNotify.get().css({
		                "top": ($(window).height() / 3) - (PNotify.get().height() / 2),
		                "left": ($(window).width() / 2) - (PNotify.get().width() / 2)
		            });
			    },
			    buttons: {
			        closer: false,
			        sticker: false
			    },
			    insert_brs: false
			});
	}
}

function getQuestionLink(kid, textClass) {
	if (kid != null && typeof(kid) != 'undefined') {
		var a = $('<a href="qaDataEditor.jsp?id=<%= QAUtil.DATATYPE_COMMON_SENSE %>-' + kid + '" target="_blank"/>');
		if (typeof(textClass) != 'undefined') {
			a.append($('<span/>').addClass(textClass).text(kid));
		}
		else {
			a.text(kid);
		}
		a.append("<br>");
		return a;
	}

	if (typeof(textClass) != 'undefined') {
		return $('<span/>').addClass(textClass);
	}
	return $('<span/>');
}

statusText = {};
<%
for (QAAltBuildQueue.Status buildStatus: QAAltBuildQueue.Status.values()) {
	String buildStatusText = MessageUtil.getMessage(locale, buildStatus.text);

	if (buildStatus == QAAltBuildQueue.Status.PENDING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PENDING) {
		buildStatusText = "<span class='text-danger'><span class='text-danger glyphicon glyphicon-time'></span>" + StringEscapeUtils.escapeHtml4(MessageUtil.getMessage(locale, buildStatus.text)) + "</span>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.PROCESSING || buildStatus == QAAltBuildQueue.Status.SLOWJOB_PROCESSING) {
		buildStatusText = "<span class='text-warning'><span class='text-warning glyphicon glyphicon-refresh'></span>" + StringEscapeUtils.escapeHtml4(MessageUtil.getMessage(locale, buildStatus.text)) + "</span>";
	}
	else if (buildStatus == QAAltBuildQueue.Status.DONE) {
		buildStatusText = "<span class='text-success'><span class='text-successs glyphicon glyphicon-ok-sign'></span>" + StringEscapeUtils.escapeHtml4(MessageUtil.getMessage(locale, buildStatus.text)) + "</span>";
	}
	else {
		buildStatusText = "<span class='text-danger'><span class='text-danger glyphicon glyphicon-exclamation-sign'></span>" + StringEscapeUtils.escapeHtml4(MessageUtil.getMessage(locale, buildStatus.text)) + "</span>";
	}

	out.println("statusText['" + buildStatus.name() + "'] = \"" + buildStatusText + "\";");
}
%>

$(document).ready(function() {
	$('#qaStatus').html(statusText[$('#qaStatus').text()]);
	
	QABuildStatusChecker = createQABuildStatusChecker(
		{
			docId: '<%= id %>', 
			callback: { 
				statusUpdated: function(result) {
					$('#qaStatus').html(statusText[result.status]);
					$('#qaStatusOnTab').html(statusText[result.status]);
  					if (result.status == "DONE") {
  						QABuildStatusChecker.stop();
  					}
				}
			}
		}
	);
	QABuildStatusChecker.start();
});

var mailContent = '';
var qaCategoryRadio = '';
var reason = '';

$(function() {
	var now = new Date();
	$.datepicker.setDefaults({
		dateFormat: 'yy/mm/dd',
		minDate: now
	});
    $( 'input[name="expire"]' ).datepicker();
    $( 'input[name="enableDate"]' ).datepicker();
    $('[name=choosedQAPlugIn]').change(function(){
    	$('[id^="qa-plugin-panel-"]').hide();
    	var idToShow = $(this).val();
    	if(idToShow){
    		$('[id="qa-plugin-panel-' + idToShow + '"]').show();
    	}
    });
    $('[id="qa-plugin-panel-' + $('[name=choosedQAPlugIn]').val() + '"]').show();

    var doSave = function(event, wait, callback) {
	    	wait = typeof wait !== 'undefined' ? wait : false;
	    <%if (!((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O7) == 0 ||
	  			(AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0)){%>
	    	alert("Can not save");
	    	return;
	    <%}%>
		$('.loading-panel').show();

		<%-- 送出前最後處理資料 --%>
		theForm = $('#form');
		inheritanceDT = $('#inheritance-template-list').DataTable();
		//console.log(inheritanceDT);
		var inheritanceDTparams = inheritanceDT.$('input,select,textarea').serializeArray();
		
		// Iterate over all form elements
	    $.each(inheritanceDTparams, function() {
	      // If element doesn't exist in DOM
	      if(!$.contains(document, theForm[this.name])){
	         // Create a hidden element
	         $(theForm).append(
	            $('<input>')
	               .attr('type', 'hidden')
	               .attr('name', this.name)
	               .val(this.value)
	         );
	      }
	    });
		
		$('#customScript').text(customScriptEditor.getValue());
		$('#question').val(questionEditor.getValue());
		$('#exAltTpls').text(exAltTplsEditor.getValue());
		var altEditor = $('#question_alt').data('CodeMirrorInstance');
		if(altEditor) {
			$('textarea[name=question_alt]').val(altEditor.getValue());
		}
		else {
			$('textarea[name=question_alt]').val(altJsonToTextArea());
		}

		if (saveHook.length > 0) {
			for (var i=0; i < saveHook.length; i++) {
				hook = saveHook[i];
				
				if (typeof(hook) == 'function') {
					hook();
				}
			}
		}
		
    		<%
		if(t.getEnableQaAudit()){
		%>
			var auditRadios = document.getElementsByName('qaAudit');
			var auditRadio = '';
			var val = $("input[name=qaAudit]").filter('[value=auditReject]').prop('checked')
			console.log("----"+val+"")
			for(var i = 0; i < auditRadios.length; i++) {
			    if(auditRadios[i].checked == true){
			    	auditRadio = auditRadios[i].value;
			    }
			}
			if(auditRadio == ''){
			    <%
			    for (QAChannel c: channels) {
			    	for (QAUserType ut: userTypes) {
			    		String concatCode = "answer_" + c.getCode() + "_" + ut.getCode();
				    	
			    %>
			    		if(form.<%= concatCode %>.value.replace(/[\r\n]/g, "") != form.<%= concatCode %>_hide.value.replace(/[\r\n]/g, "")){
			    			for(var i = 0; i < auditRadios.length; i++) {
			    			    if(auditRadios[i].value == 'auditProcess'){
			    			    	auditRadios[i].checked = true;
			    			    }
			    			}
			    			auditRadio = "auditProcess";
			    			
			    			if(mailContent == ''){
			    				mailContent = 'QA Audit, due to <br><br>';
			    			}
			    			mailContent += '<%= concatCode %> from [' + form.<%= concatCode %>_hide.value + '] to [' + form.<%= concatCode %>.value + ']<br><br>';
			    		}
			    <%
			    	}
			    }
			    %>
			    /*if((form.notBanned.checked != form.notBanned_hide.checked) || (form.question.value != form.question_hide.value)){
					for(var i = 0; i < auditRadios.length; i++) {
	    			    if(auditRadios[i].value == 'auditProcess'){
	    			    	auditRadios[i].checked = true;
	    			    }
	    			}
					auditRadio = "auditProcess";
					
					if(mailContent == ''){
	    				mailContent = 'QA Audit, due to <br><br>';
	    			}
					
					if(form.question.value != form.question_hide.value)
	    				mailContent += 'QA Question from [' + form.question_hide.value + '] to [' + form.question.value + ']<br><br>';
					
					if(form.notBanned.checked != form.notBanned_hide.checked)
	    				mailContent += 'QA notBanned from [' + form.notBanned_hide.checked + '] to [' + form.notBanned.checked + ']<br><br>';
				}*/
			}
		<%
		}	
		%>
    	
    	<%
    		if(t.getEnableQaAudit() && !"admin".equals(currentAdmin.getLoginName())){
    	%>
		    	var qaCategoryRadios = document.getElementsByName('qaCategoryRadio');
				for(var i = 0; i < qaCategoryRadios.length; i++) {
				    if(qaCategoryRadios[i].checked == true){
				    	qaCategoryRadio = qaCategoryRadios[i].value;
				    }
				}
				
			<%	if (decideAudit.equals("auditEdit")){	%>
					alert("<bean:message key='qa.edit.lock'/>");
					$('.loading-panel').hide();
					return;
			<%	} else if (decideAudit.equals("auditProcess")) { %>
					alert("<bean:message key='qa.audit.lock'/>");
					$('.loading-panel').hide();
					return;				 
			<%	} %>

				if (auditRadio != '') {
					//do nothing
				}else{
		    		alert("<bean:message key='global.audit.status.select'/>");
		    		$('.loading-panel').hide();
		        	return;
				}
    	<%
    		}	
		%>
		
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
	      					var action=""; //判斷是新增或修改
	      					if("<%= id%>"=="null")
	      						action = "save";
	      					else
	      						action = "update";
	      					
	      					if (result.estimate > <%= QAUtil.ALT_LIMIT_PER_QUESTION %>) {
	      						$.alert("<bean:message key='forecast.problem.tooManyAlts'/>");
		      					$('.loading-panel').hide();
	      					}
	      					else {
	      						<% if(t.getEnableQaAudit()){%>
	      						if (auditRadio == 'auditReject') {
	      							reason = prompt("<bean:message key='global.audit.reject.reason'/>");
	      						}else if (auditRadio == 'auditEdit') {
	      							reason = prompt("<bean:message key='global.description'/>");
	      						}
	      						
	      						if (auditRadio == "auditProcess") {
	      							reason = prompt("<bean:message key='global.description'/>");
	      							if(confirm("<bean:message key='qa.audit.process.confirm'/>") == true){
	      								//do nothing
	      							} else {
	      								$('.loading-panel').hide();
	      						   		return;
	      							}
	      						}
	      						<%}%>
	      						if(confirm('<bean:message key="forecast.problem.arry.number"/>：[' + result.estimate.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",") + '] \n <bean:message key="sure.continue"/>')) {
	      							$.ajax({
			      				    		type: 'POST',
			      				    		url: 'qaDataSave.jsp?qaStatus=' + reason + '&action=' + action,
			      				    		data: $('#form').serialize(),
			      				    		success: function(result) {
			      				    			if (result.status == "success") {
			      				    				if(mailContent != ''){
														sendMail(mailContent, qaCategoryRadio);
													}
			      				    				QABuildStatusChecker.start();
			      				    				if(!<%=hideNavBar%>){
				      				    	    		if(typeof(form.qaAudit) != 'undefined' && form.qaAudit.value == ''){
				      				    	    			window.location.replace('qaDataEditor.jsp?id=COMMON_SENSE-' + result.kid);
				      				    	    		} else if (typeof(form.qaAudit) == 'undefined') {
				      				    	    			window.location.replace('qaDataEditor.jsp?id=COMMON_SENSE-' + result.kid);
				      				    	    		} else {
				      				    	    			window.location.replace('qaAdmin.jsp');
				      				    	    		}
				      				    	    	} else {
				      				    	    		window.location.replace('qaAdmin.jsp?hideNavBar=true');
				      				    	    	}
				      				    		}
			      				    			else {
			      				    				alert('<bean:message key="qa.save.error"/>，errorMsg [' + result.errorMsg + ']');
			      				    			}
			      				    		},
			      				    		complete: function() {
			    	      						if (!wait) $('.loading-panel').hide();
			      				    		},
		      		      				error: function(jqXHR, textStatus, errorThrown) {
		      		      				  console.log(textStatus, errorThrown);
		      		      				  window.location.replace('qaAdmin.jsp');
		      		      				}
			      				    	});
	      						}
	      						else {
	      							$('.loading-panel').hide();

	      							if (typeof(callback) != 'undefined')
		      							callback(false);
	      						}
	      					}
	      				},
				    		complete: function() {
			    			},
	      				error: function(jqXHR, textStatus, errorThrown) {
	      				  console.log(textStatus, errorThrown);
	      				}
	      			});
	      		  }
			  }, true);
	};

    $('#save_copy').click(doSave);
    $('#save').click(doSave);
    $('.btn-qa-save').click(doSave);  
    $(document).on('click', '#btnBatchNlpAndSave2', function() {
        batchNlp(true)
    });
    
    $(document).on('click', '#btnBatchNlpAndSave1', function() {
        batchNlp(false)
    });
    function batchNlp(ReplaceSyn){
		$('textarea[name=for_batch_nlp]').val($(bnp.get()).find('textarea').val());
        $('input[name=batchNlpReplaceSyn]').val(ReplaceSyn);
		bnp.remove();
		doSave($(this), true, function() {
			location.reload();
			$('.loading-panel').hide();
		});
    }
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
    $( "#suggest-template-dialog").dialog({
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
	    	$("#check-dialog").dialog('close');
	    	$question = questionEditor.getValue();
		$('#question').val($question);
       	checkSimAndShowPanel($question);
    });

    $(document).on('submit', "form[name='set-kid-form']", function(e) {
	    	$this = $(this);
	    	var message = $this.find("div[name='message-body']");
	    	var kid = $this.find("input[name='kid']").val();
		if(!kid || /\s/.test(kid)) {
			message.html("<bean:message key='kid.label.set.space.alert'/>");
		}
		else {
			var $btn = $this.find('input[type="submit"]').button('loading');
	    		var $input = $this.find('input[type="text"]').attr('disabled', 'disabled');
			$.ajax({
		    		url:'qa-dup-check-ajax.jsp',
		    		data: {kid: kid},
		    		dataType: 'json',
		    		success: function(result) {
		    				if (result.exists) {
		    					if (confirm("<bean:message key='kid.label.set.attention'/>")) {
		        					message.html("<bean:message key='kid.label.set'/>：" + kid);
		        					$('input[type="hidden"][name="id"]').val('<%= QAUtil.DATATYPE_COMMON_SENSE %>-' + kid);
		        					$('[name="kid-label"]').text(kid);
		    					}
		    				}
		    				else {
		    					message.html("<bean:message key='kid.label.set'/>：" + kid);
		    					$('input[type="hidden"][name="id"]').val('<%= QAUtil.DATATYPE_COMMON_SENSE %>-' + kid);
		    					$('[name="kid-label"]').text(kid);
		    					
		    					setTimeout(function() { // 可以成功指定知識編號的話，panel 自動消失
				    			    	skpShown = false;
				    			    	skp.remove();
		    					}, 500);
		    				}
		    			},
		    	    complete: function() {
		    	    	$btn.button('reset');
		    	    	$input.removeAttr("disabled");
		    	    }
		    	});
		}
    		return false;
    });
    $(document).on('click', "form[name='set-kid-form'] .btn-cancel", function(e) {
    	skpShown = false;
    	skp.remove();
    });
    $(document).on('click', ".btnDelAltRow", function(e) {
		$this = $(this);
		offset = parseInt($this.attr('data-offset'));
		var newArray = $.map(altDataJson, function(v, i) {
			return v['offset'] == offset ? null : v;
		});

		altDataJson = newArray;
		redrawDT();
    });
    $(document).on('click', ".btnAddAltRow", function(e) {
        $dt = $('.altTable').DataTable();
        newRow = {offset: altDataJson.length, alt:"", "alt.editor": <%= currentAdmin.getId() %>, testCase: "", "testCase.editor": <%= currentAdmin.getId() %>};
        altDataJson.push(newRow);
        $newRow = $dt.row.add( newRow ).draw(false);
    });
    $(document).on('click', ".btnSimilarAltRow", function(e) {
		$this = $(this);
		offset = parseInt($this.attr('data-offset'));
		var found;
		$.map(altDataJson, function(v, i) {
			if (v['offset'] == offset) {
				found = v;
			}
		});
		
		if (typeof(found) != 'undefined' && found.hasOwnProperty('testCase') && found.testCase != '') {

		    	$.getJSON('qaAltFindSimilar.jsp', {
		    		q: found.testCase
		    	},
		    	function(data) {
			    	$dialog = $("#autoGenerateAlt-dialog");
			    	$dialog.dialog('option', 'title', '從紀錄推薦相似問句');
			    	content = "";
			    	
	    			for (var i=0; i < data.length; i++) {
	    				content += data[i].question + "\n";
	    			}
		    		$dialog.html($('<pre/>').text(content));	
				$dialog.dialog('open');
		    	}
		    	);
		}

		return false;
    });

    $('#suggestTemplateBtn').click(function(){
	    	$( "#suggest-template-dialog").dialog('close');
	    	$.getJSON('qaAltTemplateSuggest.jsp', {
	    		format: 'json',
	    		question: $('#question').val(),
	    	},
	    	function(data) {
	    		var list = $('#template-list');
	    		$('#keywordForTemplate').val(data.keyword);
	    		list.find('tr:has(td)').remove();
	    		if(data.tpls.length == 0){
	    			list.append('<tr class="text-center"><td><bean:message key="have.no.data"/></td></tr>');
	    		}else{
	    			for (var i=0; i < data.tpls.length; i++) {
	    				var r = data.tpls[i];
	    				var tr = $('<tr/>');
	
	    				$('<td name="tplName" data-id="' + r.id + '"><h4>' + r.name + '</h4></td>').appendTo(tr);
	    				$('<td name="description">' + r.description + '</td>').appendTo(tr);
	
	    				var $tdOpers = $('<td/>');
	    				$('<button type="button" class="btn btn-default btnPreviewAutoAltSuggest" data-id="' + r.id + '">預覽</button>').appendTo($tdOpers);
	    				$('<input type="checkbox" name="toggleTpl" data-id="' + r.id + '" data-toggle="toggle" data-on="<bean:message key="global.apply"/>" data-off="<bean:message key="global.disable"/>" data-onstyle="success">').appendTo($tdOpers);
	    				$tdOpers.appendTo(tr);
	    				list.append(tr);
	
	    			}
	    			tplData = data.tpls;
	    			$('input[name="toggleTpl"]').bootstrapToggle();
	    		}
	    		$( "#suggest-template-dialog").dialog('open');
	    	});
	
	    	$("#btnApplyToggledTemplate").click(function() {
	    		var kw = $('#keywordForTemplate').val();
	    		var toggles = $('input[name="toggleTpl"]:checked');
				//kw = "(" + kw + ")";
	    		for (var i=0; i < toggles.length; i++) {
	    			var toggle = toggles[i];
	
	    			var tplId = $(toggle).attr('data-id');
	    			for (var j=0; j < tplData.length; j++) {
	    				if (tplData[j].id == tplId && tplData[j].suggestPatterns != null) {
	    					applied = tplData[j].suggestPatterns.replace(/{{KEYWORD}}/g, kw);
	    					$('#batchNlpTextArea').text(applied);
	    					toggleBatchNlpPanel();
		    		    		$( "#suggest-template-dialog").dialog('close');
	    				}
	    			}
	    		}
	    	});
	    	

	    $(document).on('click', '.btnPreviewAutoAltSuggest', function() {
	    	    	$dialog = $("#autoGenerateAlt-dialog");
			$dialog.dialog('option', 'title', '預覽套用斷句結果');
	    		tplId = $(this).attr('data-id');
	    		var kw = $('#keywordForTemplate').val();
	    		
	    		for (var j=0; j < tplData.length; j++) {
	    			if (tplData[j].id == tplId && tplData[j].suggestPatterns != null) {
	    				kwArr = kw.split(',');
	    				var applied = tplData[j].suggestPatterns;
	    				
	    				for (var k=0; k < kwArr.length; k++) {
	    					if (k == 0) {
	    						applied = applied.replace(new RegExp("{{KEYWORD}}", "g"), kwArr[k]);
	    					}
	    					applied = applied.replace(new RegExp("{{KEYWORD" + (k+1) + "}}", "g"), kwArr[k]);
	    				}
	    				
	    			    	tbl = $('<table class="table table-striped table-bordered" ></table>');
	    			    	
	    			    	lines = applied.trim().split(/\r?\n/g);
	    			    	for (var i=0; i < lines.length; i++) {
	    	    				dataForRender = {
	    	    					no: (i+1),
	    	    					sentence: lines[i]
	    	    				};
	    	    				var template = $('#applied-sentence').html();
	    	    				Mustache.parse(template);
	    	    				var panelBody = Mustache.render(template, dataForRender);
	    	    				
	    	    				tbl.append($(panelBody));
	    			    	}
	    			    	
	    		    		$dialog.html(tbl);	
	    		    		
	    		    		$('.toggleExcludeSentence').bootstrapToggle();
	    					$dialog.dialog('open');
	    		    		
	    		    		$('.excludeSentence').each(function() {
	    		    			$this = $(this);
	    		    			$code = $this.text();
	    		    			
	    			    		CodeMirror($this.parent().find('.showCode')[0], {
	    				        value: $code.trim(),
	    				        mode: 'qaalt',
	    				        lineNumbers: false, lineWrapping: true,
	    				        readOnly: true
	    				    });
	    		    		});
	    			}
	    		}
	    		
	    		return false;
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
	    		if(typeof(data) == 'undefined' || data.length == 0){
	    			list.append('<tr class="text-center"><td><bean:message key="have.no.data"/></td></tr>');
	    		}else{
				evaluationLogEntries = {};
	    			for (var i=0; i < data.length; i++) {
	    				var r = data[i];
	    				evaluationLogEntries[r.id] = r;
	    				var tr = $('<tr/>');
	    				var cv = {output:''};
	    				try {
	    				  cv = JSON.parse(r.conversations);
	    				}
	    				catch(e){}
	    				var t = moment( r.timestamp );
	    				var formatted = t.format("YYYY/MM/DD HH:mm:ss");
					$('<input type="hidden" name="qaCategory" value="' + r.qaCategory + '" />').appendTo(tr);
	    				$('<td name="logId" data-id="' + r.id + '" data-flag="' + r.flag + '"><h3>' + flagGlyphs[r.flag] + '</h3></td>').appendTo(tr);
	    				$('<td name="question">' + r.question + '</td>').appendTo(tr);
	    				$('<td name="correctAnswerId"></td>').append(getQuestionLink(r.correctAnswerId)).appendTo(tr);
	    				$robotAnswerIdBlock = $('<td name="robotAnswerId"></td>')
	    				$robotAnswerIdBlock.append(getQuestionLink(r.robotAnswerId)).appendTo(tr);
	
	    				$answerBlock = $('<td>' + cv.output + '<span>　</span></td>');
	    				$answerBlock.addClass('answer-text').appendTo(tr);
	
		          		if (r.robotAnswerId) {
		          			var btnWhy = $('<button/>');
			          		btnWhy.attr('data-kid', r.robotAnswerId);
			          		btnWhy.attr('data-question', r.question);
			          		btnWhy.attr('data-loading-text', "<bean:message key='in.query'/>");
			          		btnWhy.attr('class', 'btn btn-xs btn-danger btnWhy');
			          		btnWhy.html("WHY?");
			          		btnWhy.appendTo($robotAnswerIdBlock);
		          		}
	
	    				$('<td>' + (cv.hasOwnProperty('confidence') ? cv.confidence : '') + '</td>').appendTo(tr);
	    				$('<td>' + formatted + '</td>').appendTo(tr);
	
	    				var $tdOpers = $('<td/>');
	    				var $btnGroup = $('<div/>').addClass('btn-group');
	    				$('<button name="btnAutoTest" class="btn btn-xs btn-success"><bean:message key="test.btn"/></button>').appendTo($btnGroup);
	    				<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
	    				$('<button name="btnReviewHide" class="btn btn-xs btn-danger"><bean:message key="global.hidden"/></button>').appendTo($btnGroup);
	    				$('<button name="btnMagic" class="btn btn-xs btn-primary"><bean:message key="robot.magicButton"/></button>').appendTo($btnGroup);
	    				<% } %>
	
	    				<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) > 0) { %>
	    				$('<button name="btnReviewChange" class="btn btn-xs btn-warning"><bean:message key="global.change"/></button>').appendTo($btnGroup);
	    				<% } %>
	
	    				$btnGroup.appendTo($tdOpers);
	    				$tdOpers.appendTo(tr);
	    				list.append(tr);
	    			}
	    		}
	    		$( "#test-dialog").dialog('open');
	    	});
    });
    $(document).on('click', 'button[name="btnReviewHide"]', function() {
    	$thisTr = $(this).closest('tr');
    	$logId = parseInt($thisTr.find('td[name="logId"]').attr("data-id"));
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
    				$.alert('<bean:message key="detection.network"/>');
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
    		$thisTr = $(this).closest('tr');
    		$logId = parseInt($(this).closest('td').siblings('td[name="logId"]').attr("data-id"));
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
  			  $.alert("[" + correctAns + "] <bean:message key='is.not.number'/>");
      		  $this.removeClass("text-success");
  			  return;
  		  }

  		  thisData["correctAnswer"] = correctAns;
  		  thisData["robotAnswer"] = correctAns;
  		  break;
  	  case 3:
  		  $this.addClass("text-success");
  		  if (!$.confirm("<bean:message key='sure.select.this.option'/>")) {

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
  			  $.alert("[" + correctAns + "] <bean:message key='is.not.number'/>");
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
      			  $.alert("<bean:message key='top.logout.ex2'/>");
      			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
      		  }
      	  }
        });
    });
    function testCallback(event, btn, stat, callbackWhenFinished) {
	    var $thisBtn = btn;
	    	var $stat = stat;
	    	if (typeof(btn) == 'undefined') {
		    	$thisBtn = $(this);
	    	}
	    	if (typeof($stat) == 'undefined') {
	    		$stat = {good:0, bad:0, unknown:0};
	    	}
	
	    	if (event != null) $('.loading-panel').show();

        return (function($insertAfterThisTr, $question, $correctAnswerId, $robotAnswerId, $qaCategory) {return $.ajax({
       		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
       		dataType: 'json',
       		async: true,
       		data: {
         		  q: $question,
         		  testMode: true,
	    		  tid: <%= t.getId() %>,
          		  html: true,
          		  qaCategorySelect: $qaCategory
       		},
       		error: function() {
       			$.alert('<bean:message key="detection.network"/>');
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
       	    	$(".timeago").timeago('locale', '<%= locale.getLanguage() + locale.getCountry()%>')
   				$(".timeago").timeago();
				if (typeof(callbackWhenFinished) == 'function') {
					callbackWhenFinished($stat);
				}
       		}
        });})
        ($thisBtn.closest('tr'),
        $thisBtn.closest('td').siblings('td[name="question"]').text(),
        parseInt($thisBtn.closest('td').siblings('td[name="correctAnswerId"]').find('a').text()),
        parseInt($thisBtn.closest('td').siblings('td[name="robotAnswerId"]').find('a').text()),
        $thisBtn.closest('td').siblings('input[type="hidden"][name="qaCategory"]').val()
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
		newText = "";

		if (!$('textarea[name=question_alt]').is(":visible")) {
    		altTemplates = altDataJson;

    		for (var i=0; i < altTemplates.length; i++) {
	    		json = altTemplates[i];
    			if (typeof(json) != 'undefined' && json.hasOwnProperty('alt')) {
	    			newText += json.alt + "\n";
    			}
    		}
		}
		else {
			newText = $('textarea[name=question_alt]').val();
		}

	    	$.post('qaDataCheckSynonym.jsp', {
	    		qalt: newText
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
	        				row2.append('<button class="btn btn-warning btnDoReplace" data-user-keyword="' + r.userKeyword + '">' + r2.keyword + '</button>');
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
	    	},
	    	"json"
	    	);
    });
    $(document).on("click", ".btnDoReplace", function() {
	    	var $this = $(this);
	    	var userKw = $this.attr("data-user-keyword");
	    	var newKw = $this.text();
	
	    	if (confirm('確認要將所有[' + userKw + ']替換成[' + newKw + ']?')) {

			if (!$('textarea[name=question_alt]').is(":visible")) {
				$.each(altDataJson, function(i, v) {
	    			t = v.alt;
					t = t.replace(new RegExp('\\(' + userKw + '\\)', 'gi'), '(' + newKw + ')');
					t = t.replace(new RegExp('\\(' + userKw + '\\|', 'gi'), '(' + newKw + '|');
					t = t.replace(new RegExp('\\|' + userKw + '\\|', 'gi'), '|' + newKw + '|');
					t = t.replace(new RegExp('\\|' + userKw + '\\)', 'gi'), '|' + newKw + ')');
					altDataJson[i].alt = t;
				});

				redrawDT();
			}
			else {
				t = $('textarea[name="question_alt"]').val();
				t = t.replace(new RegExp('\\(' + userKw + '\\)', 'gi'), '(' + newKw + ')');
				t = t.replace(new RegExp('\\(' + userKw + '\\|', 'gi'), '(' + newKw + '|');
				t = t.replace(new RegExp('\\|' + userKw + '\\|', 'gi'), '|' + newKw + '|');
				t = t.replace(new RegExp('\\|' + userKw + '\\)', 'gi'), '|' + newKw + ')');
				$('textarea[name="question_alt"]').val(t);
			}
    		}
    });
    $(document).on('click', 'button[name="btnMagic"]', function() {
	    	$thisBtn = $(this);
	    	$thisTr = $(this).closest('tr');
	    	//console.log($('td[data-entry-key="' + $(this).attr('data-target-entry-key') + '"]')[0]);
	    	$evaluationLogId = $thisTr.find('td[name="logId"]').attr('data-id');
	    	$entryData = evaluationLogEntries[$evaluationLogId];
	    	$question = $thisTr.find('td[name="question"]').text();
	    	$correctAnswerId = $thisTr.find('td[name="correctAnswerId"]').text();
	    //console.log($entryData);
	
	    	$data = {
	    		q: $question,
	    		correctKid: $correctAnswerId
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
    
    var checkSimAndShowPanel = function(question) {
       	$.getJSON('qaDataFindSimilar.jsp', {
       		q: question
       	},
       	function(data) {
       		var list = $('#check-list');
       		list.find('tr').remove();
       		list.append(
       				$('<tr class="text-warning"></tr>')
	       				.append('<th>編號</th><th>標準問： <span id="sim-dialog-question">' + question + '</span></th>')
	       				.append($('<th></th>')//.append('<form name="newQuestionForm" action="qaDataEditor.jsp" method="post" target="_new"><textarea name="fromQ" class="hide">' + $question + '</textarea><input name="firstAlt" type="hidden"><button type="submit" class="btnCreateNewQ btn btn-danger"><span class="glyphicon glyphicon-plus-sign"></span></button></form>')
       					)
       				);
       		if (data.length == 0) {
       			list.append('<tr class="text-danger"><td></td><td><bean:message key="have.no.data"/></td><td></td></tr>');
       		}
       		else{

   				list.append('<tr><td colspan=2><input type="text" placeholder="輸入問句或者知識編號" name="question" class="form-control"></td>'
   						+ '<td><button name="btnSearchAndSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>'
   						+ '</td>');
   				
       			for (var i=0; i < data.length; i++) {
       				var r = data[i];
       				list.append('<tr><td name="sim-kid">' + r.kid + '</td><td name="question" data-question="' + r.question + '"><a href="qaDataEditor.jsp?id=' + r.id + '" target="_blank">' 
       						+ r.question 
       						+ '</a></td>'
       						+ '<td><button name="btnSim" class="btn btn-warning" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.similarQuestionButton.tooltip"/>' + '"><span class="glyphicon glyphicon-screenshot"></span></button>'
       						<% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) > 0) { %>
       						+ '<button name="btnMerge" class="btn btn-danger" data-target-entry-key="' + '" data-toggle="tooltip" data-placement="left" title="' + '<bean:message key="robot.mergeQA.tooltip"/>' + '"><span class="glyphicon glyphicon-transfer"></span></button>'
       						<% } %>
       						+ '</td>');
       			}
    		    		$('[data-toggle="tooltip"]').tooltip();
       		}
       		$("#check-dialog").dialog('open');
       	});
    }

    $(document).on('click', ".btnCloseMagicDialog", function(e) {
    		$("#magic-dialog").dialog('close');
    });
    
    $(document).on('click', 'button[name="btnSim"]', function() {
       	$question = $(this).parent().siblings('td[name="question"]').text();
       	checkSimAndShowPanel($question);
    });
    
    $(document).on('click', 'button[name="btnSearchAndSim"]', function() {
       	$question = $(this).closest('tr').find('input[name="question"]').val();
       	checkSimAndShowPanel($question);
    });
    
    $(document).on('click', 'button[name="btnMerge"]', function() {
	    	$util = createQAUtility({tenantId: <%= t.getId() %>});
	    	toKid = $("input[name=id]").val().substring($("input[name=id]").val().indexOf("-") + 1);
	    	fromKid = $(this).closest('tr').find('td[name="sim-kid"]').text();
	
	    	$util.merge(
	    		{fromKid: fromKid, toKid: toKid}, 
	    		function(data, btn) { 

	    			if (!$('input[name=mergeId]').length) {
	    				$("<input type='hidden' name='mergeId' value='" + fromKid + "'>").appendTo($('form'));
	    			}
	    			else {
	    				$('input[name=mergeId]').val(fromKid);
	    			}
	    			doSave();
	    		}
	    	);
    });

    $("#preview-dialog").dialog({
		autoOpen: false,
		resizable: false
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

$(document).on('click', '#autoGenerateAltBtn', function() {
	$thisBtn = $(this);
	$thisTr = $(this).parent().parent();
   	$question = $("#question").val();
	$dialog = $("#autoGenerateAlt-dialog");
	$dialog.html("");

	$msg1 = $("<h3></h3>");
	$msg2 = $("<h3></h3>");
	$msg3 = $("<h3></h3>");

	$title = "自然語言分析"
	$buf = "<h4>本功能將會將問句「<span class='text-danger'>" + $question + "</span>」進行自然語言分析<br>";
	$buf += "透過機器學習挖掘相同類型的句型<br>";
	$buf += "並且自動產生可能的問句<br>";
	$buf += "作業時間長短視該題句子長度而有不同<br>";
	$buf += "<span class='text-danger'>請務必先確認過斷詞結果正確且設定好知識主題</span><br>";
	$buf += "按下Ok開始進行...<br></h4>";

	myConfirm($title, $buf, function() {
		$msg1.appendTo($dialog);
		$msg2.appendTo($dialog);
		$msg3.appendTo($dialog);
		$dialog.dialog('open');

		$msg1.typed({
	        strings: ["現在開始進行分析... ^500"],
	        typeSpeed: 1,
	        onStringTyped: function() {
	        	$.ajax({
	    			url: 'qaAutoGenerateAlt.jsp',
	    			dataType: 'json',
	    			data: {
	        		  q: $question,
	        		  type: 'strict',
	        		  ts: Math.random()
	    			},
	    	    }).then(
		    	    	function(result) {
		    	    		var curId = '<%= StringEscapeUtils.escapeEcmaScript(id)%>';
		    	    		var simQuestions = '';
		    	    		var data = result.sims;
	
		    	    		for (var i=0; i<data.length; i++){
		    	    			if (data[i].id != curId){
		    	    				simQuestions += data[i].question + "<br> ^500";
		    	    			}
		    	    		}
	
	    				$msg2.typed({
	    					strings: ["分析發現下列相似句型:<br>" + simQuestions + " 開始學習各種問法... ^1500"],
	    					typeSpeed: 0,
					        onStringTyped: function() {
					        	if (result.newAltTpls.length > 0) {
				    				$msg3.typed({
				    					strings: ["學習到 [" + result.newAltTpls.length + "] 種問法句型... <br> ^500 已加入各種問法當中<br> ^500 可以開始進行測試... ^100"],
				    					typeSpeed: 0,
								        onStringTyped: function() {
								        	for (var i=0; i < result.newAltTpls.length; i++) {
								        		
								        		if(document.getElementById("btnAdvAltEditor") != null && document.getElementById("btnAdvAltEditor").style.display == "none"){
									        		$('textarea[name="question_alt"]').val(
									        				$.trim($('textarea[name="question_alt"]').val())
									        				+ "\n" + result.newAltTpls[i] + "	 // {\"alt.editor\":\""+ <%= currentAdmin.getId() %> +"\",\"testCase.editor\":\""+ <%= currentAdmin.getId() %> +"\",\"testCase\":\"\"}"
									        				);
								        		} else {
									        		$dt = $('.altTable').DataTable();
									                newRow = {offset: altDataJson.length, alt: result.newAltTpls[i], "alt.editor": <%= currentAdmin.getId() %>, testCase: "", "testCase.editor": <%= currentAdmin.getId() %>};
									                altDataJson.push(newRow);
									                $newRow = $dt.row.add( newRow ).draw(false);
								        		}
								        	}
								        },
				    				});
					        	}
					        	else {
					        		$msg3.typed({
				    					strings: ["無發現可用的問句<br> ^500 也許系統內尚無足夠的知識，我還在繼續努力學習當中喔！^100"],
				    					typeSpeed: 0
					        		});
					        	}
					    },
	    				});
				});
			}
		});
	});
});

CodeMirror.defineSimpleMode("qaalt", {
	  // The start state contains the rules that are intially used
	  start: [
	     {regex: /\/\/.*/, token: "comment"},
	   	 {regex: /<%= kpRegex %>/, token: "string-2" },
	     {regex: /'(?:[^\\]|\\.)*?'/, token: "string" },
	     {regex: /\{\{\[a-zA-Z$]+\}\}/, token: "variable-2" },
	     {regex: /\(/, push: "synonym", token: "bracket" },
	  ],
	  comment: [
	    {regex: /.*?\*\//, token: "comment", next: "start"},
	    {regex: /.*/, token: "comment"}
	  ],
	  // The meta property contains global information about the mode. It
	  // can contain properties like lineComment, which are supported by
	  // all modes, and also directives like dontIndentStates, which are
	  // specific to simple modes.
	  variable: [
	     {regex: /\}\}/, pop: true, token: "bracket", next: "start" },
	     {regex: /\$[a-zA-Z]+/, token: "variable-2" },
	     {regex: /"(?:[^\\]|\\.)*?"/, token: "string" },
	     {regex: /'(?:[^\\]|\\.)*?'/, token: "string" }
	  ],
	  synonym: [
	   	 {regex: /<%= kpRegex %>/, token: "string-2", next: "synonym" },
	     {regex: /\)/, pop: true, token: "bracket", next: "start" },
	     {regex: /\|/, token: "comment" },
	     {regex: /\{\{[a-zA-Z$]+\}\}/, token: "variable-2", next: "synonym" },
	     {regex: /\#[a-zA-Z]+/, token: "variable-2" },
	     {regex: /[^)|]*/, token: "variable-3" },
	  ],
	  knowledgepoint: [
	  ]
	});
	
CodeMirror.defineSimpleMode("qatestcase", {
	  // The start state contains the rules that are intially used
	  start: [
	   	 {regex: /<%= kpRegex %>/, token: "string-2" },
	  ]
	});
	
var customScriptEditor = CodeMirror.fromTextArea(document.getElementById("customScript"), {
	   lineNumbers: true,
	   matchBrackets: true,
	   mode: "text/x-groovy",
	 });

var questionEditor = CodeMirror.fromTextArea(document.getElementById("question"), {
	   lineNumbers: false,
	   matchBrackets: true,
	   mode: "qatestcase",
	 });
	 
questionEditor.on("change", function(qe) {
	checkQuestion(qe.getValue());
});	 
var $msgDiv = $("<div id='checkDiv' style='display:none;color:red'><bean:message key='standard.problem.msg'/><div>");
$("#question").parent("div").append($msgDiv);

checkQuestion(questionEditor.getValue());

function checkQuestion(value){
	var reg =  new RegExp("[`~!#$%^&*()+=\"|{}':;'\\[\\]<>~！#￥%……&*（）——+|{}【】‘；：”“’、]");
	var test = reg.test(value);
	var lastString = value.slice(-1)
	if(test || lastString ==" "){
		$("#checkDiv").css("display","block");
		$("button.btn-qa-save").attr("disabled", true);
		$("#save").attr("disabled", true);
		$("#save_copy").attr("disabled", true);
	}
	else{
		$("#checkDiv").css("display","none");
		$("button.btn-qa-save").attr("disabled", false);
		$("#save").attr("disabled", false);
		$("#save_copy").attr("disabled", false);
	}
}

var exAltTplsEditor = CodeMirror.fromTextArea(document.getElementById("exAltTpls"), {
	   lineNumbers: true,
	   matchBrackets: true,
	   mode: "qaalt",
	 });
	 
<%-- 這個要專屬的 class，不然沒有外框很醜 --%>
$(questionEditor.display.wrapper).addClass('CodeMirror-question');

<%-- 切換 tab 的時候 refresh CodeMirror 們，不然格式會跑掉 --%>
$(document).on('shown.bs.tab', 'a[data-toggle="tab"]', function (e) {
	if (customScriptEditor instanceof CodeMirror){
		customScriptEditor.refresh();
    }
	/*if (altEditor instanceof CodeMirror) {
		altEditor.refresh();
	}*/
	for (var i=0; i < qaaltList.length; i++) {
		qaaltList[i].refresh();	
	}
	for (var i=0; i < qatestcaseList.length; i++) {
		qatestcaseList[i].refresh();	
	}
	exAltTplsEditor.refresh();
});

var sendMailRequest;
function sendMail(mailContent, qaCategory) {
	var knowledgeId = "<%=kid %>";
	var question='<%=StringEscapeUtils.escapeEcmaScript(ques)%>';
	$.ajax({
		url: 'sendMail.jsp',
		dataType: 'json',
		data: {
		  tid: '<%= t.getId()%>',
		  qaCategory: qaCategory,
		  subject: '智能客服系統知識問答異動(' + qaCategory +')',
		  text: '<h1>提醒知識問答異動</h1><table><tr><td>知識編號：' + knowledgeId + '</td><br><br><td>標準問題：' + question + '</td></tr>'+mailContent+'</table>'
		},
    }).then(
    	function(resp) {
	}); 
}

function sendToMeLINE(channel) {
	$.ajax({
		url: '<%=request.getContextPath() %>/qa-ajax.jsp',
		method: 'GET',
		data: {
			id: '<%= uc != null ? uc.getLineUserId() : ""%>',
			q: '<%= StringEscapeUtils.escapeEcmaScript(StringEscapeUtils.escapeHtml4(ques)) %>',
			apikey: '<%=Apikey.list(t.getId()).get(0).getApikey() %>',
			ch: channel,
			testMode: true
		},
		success: function(result) {
			$.ajax({
				url: '<%=request.getContextPath() %>/wiseadm/line/qaRichMessage-ajax.jsp',
				method: 'POST',
				data: {
					action: 'sendToMe',
					lineMessages: JSON.stringify(result.line.messages)
				},
				success: function(result) {
					if (result.StatusCode && result.StatusCode == 200) {
						new PNotify({
						    title: 'Success!',
						    text: '請在手機上查看訊息內容',
						    type: 'success'
						});
					}
					else {
						var msg = result.message;
						
						if (result.details) {
							for (var i=0; i < result.details.length; i++) {
								detail = result.details[i];
								msg += "\n" + detail.property + " => " + detail.message;
							}
						}
						new PNotify({
						    title: 'Error!',
						    text: msg,
						    type: 'error'
						});
					}
				}
			})
		}
	});
}


</script>
</body>
</html>
