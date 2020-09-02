<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	 import="javax.servlet.ServletConfig"
	 import="javax.servlet.ServletException"
	 import="javax.servlet.http.*"
	 import="java.io.*"
	 import="java.net.*"
	 import="java.text.*"
	 import="java.util.*"
	 import="org.json.*"
	 import="org.apache.commons.io.*"
	 import="org.apache.commons.httpclient.*"
	 import="org.apache.commons.httpclient.methods.*"
	 import="org.apache.commons.httpclient.params.HttpMethodParams"
	 import="org.apache.commons.lang.*"
	 import="org.apache.solr.core.*"
	 import="org.apache.solr.servlet.*"
	 import="org.apache.solr.client.solrj.*"
	 import="org.apache.solr.client.solrj.embedded.*"
	 import="org.apache.solr.client.solrj.response.*"
	 import="org.apache.solr.common.*"
	 import="com.intumit.solr.SearchManager"
	 import="com.intumit.solr.tenant.*"
	 import="com.intumit.solr.util.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.entity.*"
	import="com.intumit.solr.robot.intent.*"
	import="com.intumit.solr.synonymKeywords.*"
	 import="com.intumit.systemconfig.*"
	 import="org.apache.commons.lang.StringUtils"
	 import="com.intumit.quartz.ScheduleUtils"
	 import="com.intumit.quartz.Job"
     import="org.dom4j.*"
	 import="org.apache.solr.client.solrj.SolrQuery"
	import="com.intumit.solr.admin.*"
	import="com.intumit.license.LicenseChecker"
	import="com.intumit.message.MessageUtil"
%><%!
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
String wrap(String text, String tag) { return "<" + tag + ">" + text + "</" + tag + ">"; }
%>
<%
AdminUser admUser = AdminUserFacade.getInstance().getFromSession(session);
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(admUser.getLoginName()) && 1==admUser.getId())) {
	return;
}

Locale sessionLocale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
if(sessionLocale == null) sessionLocale = Locale.TAIWAN;
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/wiseadm/css/dropzone.css" type="text/css" rel="stylesheet" />
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-slider.css" rel="stylesheet">
<link href="<%= request.getContextPath()%>/wiseadm/css/select2.min.css" type="text/css" rel="stylesheet" />
<link href="<%= request.getContextPath() %>/wiseadm/css/jquery.dataTables.min.css" type="text/css" rel="stylesheet"/>
<link rel="stylesheet" href="<%= request.getContextPath() %>/wiseadm/css/codemirror.css">
<!--[if lt IE 9]>
  <script src="<%=request.getContextPath()%>/script/html5shiv.js"></script>
  <script src="<%=request.getContextPath()%>/script/respond.min.js"></script>
<![endif]-->
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery-ui-1.9.2.custom.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/assets/javascripts/plugins/datatables/jquery.dataTables.numeric-comma.js"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.aciPlugin.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.aciSortable.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.iframe-transport.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.fileupload.js"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap-toggle.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap-slider.js"></script>
<script src="<%= request.getContextPath()%>/wiseadm/js/select2.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.mode.javascript.js"></script>
<script>
function doSubmit() {
	document.myForm.cmd.value="add";
	document.myForm.submit();
}
function doDelete(theForm) {
	theForm.cmd.value = "delete";
	theForm.submit();
}
function doUpdate(theForm) {
	theForm.cmd.value = "update";
	theForm.submit();
}

function deleteTenant(url){
	if(confirm('<bean:message key="sure.del.tenant"/>')){
	document.dataSetListForm.action=url;
	document.dataSetListForm.method='POST';
	document.dataSetListForm.submit();
	top.leftFrame.location.reload();
	}else{
	return;
	}
}
</script>
<style>
.dt-mt {
	margin-top: 10px;
}

td-form {
	width: 50%;
}

td-form input {
	width: 100%;
}

.btnEditChannel {
	margin-bottom:4px;
	white-space: normal;
}

.CodeMirror {
  border: 1px solid #bbb;
  width: 600px;
}
</style>
</HEAD>
<BODY>
<%
WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();
String action = request.getParameter("action");
String idStr = request.getParameter("id");
boolean detailForm = "create".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action) || "duplicate".equalsIgnoreCase(action);

if (detailForm) {
	Tenant tenant = idStr == null ? new Tenant() : Tenant.get(Integer.parseInt(idStr));
	%>
	<bean:message key="tenant.base.data"/>
	<form action="tenantAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<table class="table table-bordered">
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td><bean:message key="tenant.id"/></td>
		<td class="td-form">
			<input type="hidden" name="id" value="<%= tenant.getId() %>">
			<%= tenant.getId() %>
		</td>
		<td>&nbsp;</td>
	</tr>
	<%
	}
	%>
	<tr>
		<td><bean:message key="tenant.name"/></td>
		<td class="td-form">
			<input type="text" size="40" name="name" value="<%= StringUtils.trimToEmpty(tenant.getName()) %>">
		</td>
		<td><bean:message key="tenant.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="tenant.core.name"/></td>
		<td class="td-form">
			<input type="text" size="40" name="coreName" value="<%= StringUtils.trimToEmpty(tenant.getCoreName()) %>">
		</td>
		<td><bean:message key="tenant.core.name.ex"/></td>
	</tr>
	<tr>
		<td>Default Locale</td>
		<td class="td-form">
			<input type="text" size="40" name="locale" value="<%= tenant.getLocale().name() %>">
		</td>
		<td>Supported locale (zh_TW / zh_CN / ja_JP)</td>
	</tr>
	<tr>
		<td>QaCategory</td>
		<td class="td-form">
		<%
		if(tenant.getQaCategory() == null){
		%>
			<input type="text" size="40" name="qaCategory" value="通用">
		<%
		}else{
		%>
			<input type="text" size="40" name="qaCategory" value="<%= StringUtils.trimToEmpty(tenant.getQaCategory()) %>">
		<%
		}
		%>
		</td>
		<td><bean:message key="qacategory.desc"/></td>
	</tr>
	<tr>
		<td>Specific Eservice</td>
		<td class="td-form">
			<input type="text" size="40" name="specificEservice" value="<%= StringUtils.trimToEmpty(tenant.getSpecificEservice()) %>">
		</td>
		<td><bean:message key="specific.eservice.desc"/></td>
	</tr>
	<tr>
		<td><bean:message key="tenant.email"/></td>
		<td class="td-form">
			<input type="text" size="40" name="email" value="<%= StringUtils.trimToEmpty(tenant.getEmail()) %>">
		</td>
		<td><bean:message key="tenant.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="tenant.address"/></td>
		<td class="td-form">
			<input type="text" size="40" name="address" value="<%= StringUtils.trimToEmpty(tenant.getAddress()) %>">
		</td>
		<td><bean:message key="tenant.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="tenant.tel"/></td>
		<td class="td-form">
			<input type="text" size="40" name="tel" value="<%= StringUtils.trimToEmpty(tenant.getTel()) %>">
		</td>
		<td><bean:message key="tenant.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="tenant.note"/></td>
		<td class="td-form">
			<input type="text" size="40" name="notes" value="<%= StringUtils.trimToEmpty(tenant.getNotes()) %>">
		</td>
		<td><bean:message key="tenant.note.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="text.customer.service.website"/></td>
		<td class="td-form">
			<input type="text" size="80" name="crmApiBaseUrl" value="<%= StringUtils.trimToEmpty(tenant.getCrmApiBaseUrl()) %>">
		</td>
		<td><bean:message key="text.customer.service.website.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="life.langue.index.url"/></td>
		<td class="td-form">
			<input type="text" size="80" name="casualCoreUrl" value="<%= StringUtils.trimToEmpty(tenant.getCasualCoreUrl()) %>">
		</td>
		<td><bean:message key="life.langue.index.desc"/></td>
	</tr>
	<tr>
		<td>Statistics index url</td>
		<td class="td-form">
			<input type="text" size="80" name="statisticsCoreUrl" value="<%= StringUtils.trimToEmpty(tenant.getStatisticsCoreUrl()) %>">
		</td>
		<td>(Ex. http://localhost/wise/wiseadm/core-log )</td>
	</tr>
	<tr>
		<td>Open Data index url</td>
		<td class="td-form">
			<input type="text" size="80" name="opendataCoreUrl" value="<%= StringUtils.trimToEmpty(tenant.getOpendataCoreUrl()) %>">
		</td>
		<td>(Ex. http://localhost/wise/wiseadm/core1 )</td>
	</tr>
	<tr>
		<td>Outer source url</td>
		<td class="td-form">
			<input type="text" size="80" name="outerSourceUrl" value="<%= StringUtils.trimToEmpty(tenant.getOuterSourceUrl()) %>">
		</td>
		<td>(You must have apikey)</td>
	</tr>
	<tr>
		<td><bean:message key="tenant.allowedLocales"/></td>
		<td class="td-form">
			<input type="text" size="80" name="allowedLocales" value="<%= StringUtils.trimToEmpty(tenant.getAllowedLocales()) %>">
		</td>
		<td><bean:message key="tenant.allowedLocales.desc"/></td>
	</tr>
	<tr>
		<td>KMS url</td>
		<td class="td-form">
			<input type="text" size="80" name="kmsUrl" value="<%= StringUtils.trimToEmpty(tenant.getKmsUrl()) %>">
		</td>
		<td>(Ex. http://localhost/SmartKMS )</td>
	</tr>
	<tr>
		<td><bean:message key="customData.allowedFieldNames"/></td>
		<td class="td-form">
			<input type="text" size="80" name="allowedCustomDataFieldNames" value="<%= StringUtils.trimToEmpty(tenant.getAllowedCustomDataFieldNames()) %>">
		</td>
		<td><bean:message key="customData.allowedFieldNames.desc"/></td>
	</tr>
	<tr>
		<td><bean:message key="tenant.forceIgnoreCharacters"/></td>
		<td class="td-form">
			<input type="text" size="80" name="forceIgnoreCharacters" value="<%= StringUtils.trimToEmpty(tenant.getForceIgnoreCharacters()) %>">
		</td>
		<td><bean:message key="tenant.forceIgnoreCharacters.ex"/></td>
	</tr>
	<tr>
		<td><bean:message key="function.setting"/></td>
		<td class="col-md-8">
		<bean:message key="recommended.number"/>
		<input type="text" id="otherMltQANumSlider"
							name="otherMltQANum" class="col-md-8 slider" value="<%= tenant.getOtherMltQANum() %>"
							data-slider-min="1" data-slider-max="20" data-slider-step="1" data-slider-value="<%= tenant.getOtherMltQANum() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showOtherMltQANumVal"><%= tenant.getOtherMltQANum() %></span><br>
		<BR>
		<bean:message key="accuracy.threshold"/>1
		<input type="text" id="threshold1"
							name="threshold1" class="col-md-8 slider" value="<%= tenant.getThreshold1() %>"
							data-slider-min="5" data-slider-max="500" data-slider-step="5" data-slider-value="<%= tenant.getThreshold1() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showThreshold1Val"><%= tenant.getThreshold1() %></span> <bean:message key="complete.comparison"/><br>
		<BR>
		<bean:message key="accuracy.threshold"/>2
		<input type="text" id="threshold2"
							name="threshold2" class="col-md-8 slider" value="<%= tenant.getThreshold2() %>"
							data-slider-min="5" data-slider-max="500" data-slider-step="5" data-slider-value="<%= tenant.getThreshold2() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showThreshold2Val"><%= tenant.getThreshold2() %></span> <bean:message key="fuzzy.comparison"/><br>
		<BR>
		<bean:message key="accuracy.threshold"/>3
		<input type="text" id="threshold3"
							name="threshold3" class="col-md-8 slider" value="<%= tenant.getThreshold3() %>"
							data-slider-min="5" data-slider-max="500" data-slider-step="5" data-slider-value="<%= tenant.getThreshold3() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showThreshold3Val"><%= tenant.getThreshold3() %></span> <bean:message key="related.recommendation"/><br>
		<BR>
		<bean:message key="knowledge.point.weighted"/>
		<input type="text" id="boost1"
							name="boost1" class="col-md-8 slider" value="<%= tenant.getBoost1() %>"
							data-slider-min="0" data-slider-max="50" data-slider-step="1" data-slider-value="<%= tenant.getBoost1() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showBoost1Val"><%= tenant.getBoost1() %></span> （<bean:message key="knowledge.point.weighted"/>）<br>
		<BR>
		<bean:message key="context.weighted"/>
		<input type="text" id="boost2"
							name="boost2" class="col-md-8 slider" value="<%= tenant.getBoost2() %>"
							data-slider-min="1" data-slider-max="50" data-slider-step="1" data-slider-value="<%= tenant.getBoost2() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showBoost2Val"><%= tenant.getBoost2() %></span> （<bean:message key="context.weighted"/>）<br>
		<BR>
		<bean:message key="smart.robot.tooManyRepeat.threshold"/>
		<input type="text" id="tooManyRepeatThreshold"
							name="tooManyRepeatThreshold" class="col-md-8 slider" value="<%= tenant.getTooManyRepeatThreshold() %>"
							data-slider-min="2" data-slider-max="50" data-slider-step="1" data-slider-value="<%= tenant.getTooManyRepeatThreshold() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showTooManyRepeatThresholdVal"><%= tenant.getTooManyRepeatThreshold() %></span> <br>
		<BR>
		<bean:message key="smart.robot.maxBatchUpload.threshold"/>
		<input type="text" id="maxBatchUploadSize"
							name="maxBatchUploadSize" class="col-md-8 slider" value="<%= tenant.getMaxBatchUploadSize() %>"
							data-slider-min="50" data-slider-max="1000" data-slider-step="50" data-slider-value="<%= tenant.getMaxBatchUploadSize() %>" data-slider-handle="square" data-slider-selection="after" data-slider-tooltip="show"><span id="showMaxBatchUploadSizeVal"><%= tenant.getMaxBatchUploadSize() %></span> <br>
		<BR>
		<%
	    String checked = null;
	    checked = tenant.getEnableMultiLocale() ? "checked" : "";
		%>
	    	<input type="checkbox" name="enableMultiLocale" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.multiLocale"/> <br/>
		<%
	    checked = tenant.getEnableScenario() ? "checked" : "";
		%>
	    	<input type="checkbox" name="enableScenario" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.scenario"/> <br/>
		<%
	    checked = tenant.getEnableAutoLearning() ? "checked" : "";
		%>
	    	<input type="checkbox" name="enableAutoLearning" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.autoLearning"/> <br/>
		<%
	    checked = tenant.getEnableEssentialKeywordMode() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableEssentialKeywordMode" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.essentialKeywordMode"/> <br/>
		<%
	    checked = tenant.getEnableContextMatch() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableContextMatch" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.context"/> <br/>
		<%
	    checked = tenant.getEnableRestrictToQaCategory() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableRestrictToQaCategory" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="qacategory.enable"/> <br/>
    	<%
	    checked = tenant.getEnableRestrictToKnowledgePoint() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableRestrictToKnowledgePoint" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.restrictToKnowledgePoint.enable"/> <br/>
		<%
	    checked = tenant.getEnablePhoneticHomonym() ? "checked" : "";
		%>
    	<input type="checkbox" name="enablePhoneticHomonym" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.comparison"/> <br/>
	   	<%-- 新壽新增欄位 (BEGIN) 
		<%
	    checked = tenant.getEnableUserLogin() ? "checked" : "";
		%>
	   	<input type="checkbox" name="enableUserLogin" <%= checked %> data-size="mini" data-toggle="toggle"> 啟用使用者驗身機制 <br/>
		<%
	    checked = tenant.getEnableUploadFileInRichText() ? "checked" : "";
		%>
	   	<input type="checkbox" name="enableUploadFileInRichText" <%= checked %> data-size="mini" data-toggle="toggle"> 啟用檔案上傳功能 <br/>
	   	新壽新增欄位 (END) --%>
		<%
	    checked = tenant.getEnableTextCrm() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableTextCrm" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.written.customer.service"/>
		 (<bean:message key="department.number"/> <input type="text" name="defaultTextCrmDepartmentId" size="4" value="<%= tenant.getDefaultTextCrmDepartmentId() %>">，<bean:message key="is.enabled.written.customer.service.ex"/>)
    	<br>
		<%
	    checked = tenant.getEnableQaAudit() ? "checked" : "";
		%>
	    <input type="checkbox" name="enableQaAudit" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.qaAudit"/> <br/>
	    	<%--
		<%
	    checked = tenant.getApikey() != null ? "checked" : "";
		%>
    	<input type="checkbox" name="enableApikey" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.apikey"/> <br/>
		</td>
		<td></td>
	</tr>
	--%>
		<%
		checked = tenant.getEnableScenarioCrm() ? "checked" : "";
		%>
		<input type="checkbox" name="enableScenarioCrm" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.scenario.satisfaction"/> <br/>
	<tr>
		<td><bean:message key="wisesystemconfig.set"/></td>
		<td class="col-md-8">
		<%
		checked = tenant.getEnableTenant() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableTenant" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled"/> <br/>
		<%
		checked = tenant.getEnableUserFrontEnd() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableUserFrontEnd" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key='is.enabled.uesrFrontEnd'/> 
    		<%
    		if (tenant.getEnableUserFrontEnd()) {
    			String te = WiSeUtils.aesEncrypt(Tenant.SIMPLE_AES_SECRET_KEY, Tenant.SIMPLE_AES_INIT_VECTOR, "" + tenant.getId());
				%>
				(
				URL: <span class='text-danger'><%= request.getContextPath() %>/webchat/default/?t=<%= URLEncoder.encode(te, "UTF-8") %></span> 
				&nbsp;<a href='<%= request.getContextPath() %>/webchat/default/?t=<%= URLEncoder.encode(te, "UTF-8") %>' target='_new'><span class='glyphicon glyphicon-new-window'></span></a>
				)
				<%
    		}
    		%>
    	<br/>
    	<%
	    checked = tenant.getEnableDebug() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableDebug" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.debug"/> <br/>
		<%
	    checked = tenant.getEnableQAExplain() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableQAExplain" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.detail.record"/> <br/>
    	
    	<%-- 限流設定 --%>
		<%
	    checked = tenant.getEnableRateLimitByIP() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableRateLimitByIP" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.rateLimitByIP"/> 
    	(<bean:message key="request.per.second"/> <input type="text" name="rateLimitByIpPerSec" size="4" value="<%= tenant.getRateLimitByIpPerSec() %>">)<br/>
		<%
	    checked = tenant.getEnableRateLimitByQAContext() ? "checked" : "";
		%>
    	<input type="checkbox" name="enableRateLimitByQAContext" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.rateLimitByQAContext"/> 
    	(<bean:message key="request.per.second"/> <input type="text" name="rateLimitByQAContextPerSec" size="4" value="<%= tenant.getRateLimitByQAContextPerSec() %>">)<br/>
    	
    		<input type="text" name="sessionExpirationSecond" size="8" value="<%= tenant.getSessionExpirationSecond() %>"> <bean:message key="session.timeout.second"/> <br/>
    
		</td>
		<td></td>
	</tr>
	<tr>
		<td><bean:message key="common.problems"/></td>
		<td class="td-form">
			<textarea rows="20" cols="100" id="faqJson" name="faqJson"><%= StringUtils.trimToEmpty(tenant.getFaqJson()) %></textarea>
		</td>
		<td><bean:message key="common.problems.ex1"/><A HREF="http://www.convertcsv.com/csv-to-json.htm"><bean:message key="here"/></A><bean:message key="common.problems.ex2"/>
<pre>
[
	{ "modifyUser":"USER1",
	  "modifyTime":"2017-01-01 00:00",
	  "channel": "<bean:message key="common.problems.category1"/>",
	  "questions": [
			<bean:message key="common.problems.question1"/>
			]
	},
	{ "modifyUser":"USER1",
	  "modifyTime":"2017-01-01 00:00",
	  "channel": "<bean:message key="common.problems.category2"/>",
	  "questions": [
			<bean:message key="common.problems.question2"/>
			]
	}
]
</pre>
		</td>
	</tr>
	<tr>
		<td>BOT Connector Config</td>
		<td class="td-form col-md-4">
			<textarea rows="20" cols="100" id="botConfigJson" name="botConfigJson"><%= StringUtils.trimToEmpty(tenant.getLineBotConfigJson()) %></textarea>
		</td>
		<td>Connector configuration for *ALL* external BOT services
<pre>
{
	"line": {
		"enableUserBinding": false,
		"enableAudioSTT": false, "enableAudioSTT_comment": "是否啟用 STT", 
		"enableAnswerTTS": false, "enableAnswerTTS_comment": "是否啟用 TTS，目前僅限定語音留言來的才會作動", 
		"usePostbackForTemplateAction": false, "usePostbackForTemplateAction_comment": "Use postback action instead of text action",
		"addDisplayTextWhenUsingPostback": false, "addDisplayTextWhenUsingPostback_comment": "Add displayText when using postback",
		"enableMenuToQuickReply": "當階層選項超過4個且低於13個是否啟用quickReplay",
		"menuToImage": {
			"comment": "用來設定把選單自動轉換為影像選單的邏輯",
			"enable": false,
			"bgColor": "#FFFFFF",
			"fontColor": "#3B5998",
			"boxBgColor": "#FFFFFF",
			"boxBorderColor": "#3B5998",
			"borderStroke": 5
		},
		"accessToken": "XXXXXX",
		"channelId": "XXXXXX",
		"channelSecret": "XXXXXX",
		"domainUrl": "Robot對外網址，需為 https，網址不可含 context path（系統會自動加入系統設定內的 contextPath），若未設定預設使用系統參數的 hostname"
	},
	"lineWebLogin": {
		"channelId" : "XXXXX",
		"channelSecret": "XXXXX",
		"redirectUrl": "https://xxxxx.xxxx.xxx （不含 context path），LINE 後台的設定一般設定為 redirectUrl + /wise/wiseadm/line/qaRichMessageList.jsp"
	},
	"botframework": {
		"clientId": "XXXX",
		"clientSecret": "XXXX"
	},
	"messenger": {
		"pageToken": "XXXXX",
		"verifyToken": "XXXXX(自訂的驗證權杖)",
		"answerToQuickReply": "是否啟用快速回覆(這邊是答案中轉變的選項，若啟用這個推薦記得關掉，不然兩邊都有答案的選項會被吃掉)",
		"menuToQuickReply": "是否啟用快速回覆(這邊是階層或是推薦的選項，若啟用推薦記得關掉，不然會覆蓋階層的選項)"
	}
}
</pre>
		</td>
	</tr>
	<tr>
		<td><bean:message key="user.admin"/></td>
		<td class="td-form">
		<select name="managers" class="col-md-12" multiple="multiple">
		<%
		List<AdminUser> users = AdminUserFacade.getInstance().listAll();
		
		for (AdminUser user: users) {
			boolean selected = false;
			if (user.getTenantIdSet().contains(tenant.getId())) {
				selected = true;
			}
			
			AdminGroup defAG = null;
			TenantAdminGroup tag = TenantAdminGroup.getByTenantAndAdmin(tenant.getId(), user.getId());
			if (tag != null) {
				defAG = tag.getAdminGroup();
			}
			if (defAG == null) {
				defAG = user.getDefaultGroup();
			}
			%>
			<option value="<%= user.getId() %>" <%= selected ? " selected" : "" %>>(<%= user.getId() %>). <%= user.getName() %><%= defAG != null ? " << " + defAG.getName() + " >>" : "" %></option>
			<%
		}
		%>
		</select>
		</td>
		<td>
		</td>
	</tr>
	<tr>
		<td colspan=3>
			<input class="btn btn-primary" type="submit" value="<bean:message key='save'/>">
			<input class="btn btn-warning" id='btnOpenChannelManagement' type="button" value="Channel Management">
			<input class="btn btn-warning" id='btnOpenUserTypeManagement' type="button" value="UserType Management">
			<a href="apikeyAdmin.jsp?tid=<%= tenant.getId() %>" class="btn btn-default">Apikey Management</a>
		</td>
	</tr>
	</table>
	</form>
	
	<div id="channel-management-dialog" title="<bean:message key='multiChannelAnswer.manage'/>" style="display:none; z-index:999; ">
		<div class="col-sm-12">
		<div class="row">
		<h3><bean:message key="common.warning.operationWillModifyImmediately"/></h3>
	<%
		AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
		String msgTitle = request.getParameter("msgTitle");
		String content = request.getParameter("content");
		//System.out.println( "request tenantId : " + tenantId );
		%>
		</div>
		<div class="row">
		<div id="channelContent" class="col-sm-6 scrollList">
		
		</div>
		<div class="col-sm-6">
		<div class="form-group">
		<bean:message key="global.add"/>channel
		</div>
		<div class="form-group">
		<div><bean:message key="global.format"/>：
			<select id="channelType" name="channelType">
			<% for (QAChannelType type: QAChannelType.values()) { %>
			<option value="<%= type.name() %>"><%= type.name() %></option>
			<% } %>
			</select>
		</div>
		</div>
		<div class="form-group">
		<input type="hidden" id="channelId" name="channelId" value="-1">
		<div><bean:message key="global.name"/>：<input type="text" id="channelName" name="channelName"></div>
		</div>
		<div class="form-group">
		<div><bean:message key="global.code"/>：<input type="text" id="channelCode" name="channelCode"> </div>
		</div>
		<div class="form-group">
			<div>
				預設答案的取用頻道：
				<select id="channelDefaultChannelCode" name="channelDefaultChannelCode"> 
					<% for (QAChannel ch: QAChannel.list(tenant.getId())) { %>
					<option value="<%= ch.getCode() %>"><%= ch.getName() %> (<%= ch.getType() %>)</option>
					<% } %>
				</select>
			</div>
		</div>
		<div class="form-group">
		<div>
		<input type="checkbox" id="appendOptionToOutput" data-toggle="toggle" name="channelCode"> &nbsp;附加選項到答案<br>
		<input type="checkbox" id="returnOptionInJson" data-toggle="toggle" name="channelCode"> &nbsp;附加選項到API<br>
		<input type="checkbox" id="useHtmlNewline" data-toggle="toggle" name="channelCode"> &nbsp;用HTML的換行<br>
		<input type="checkbox" id="supportMultiRichMessages" data-toggle="toggle" name="channelCode"> &nbsp;支援多圖文設定 <font id="supportMultiRichMessagesFont"></font><br>
		<input type="checkbox" id="useCustomMatchCtrlFlow" data-toggle="toggle" name="channelCode"> &nbsp;使用自訂流程 &nbsp; 
		<button type="button" class="btn btn-primary" id="btnChannelMatchCtrlFlow" disabled="disabled"><bean:message key='qa.matchCtrlFlow.modify'/></button>
		<br>
		</div>
		</div>
		<div class="form-group">
		<button type="button" class="btn btn-success btnChannelSubmit" onclick="saveOrUpdateChannelUserType('channel')"><bean:message key="global.add"/></button>
		</div>
		</div>
		</div>
		</div>
	
	</div>
	<div id="userType-management-dialog" title="<bean:message key='multiChannelAnswer.manage'/>" style="display:none; z-index:999; ">
		<div class="col-sm-12">
		<div class="row">
		<h3><bean:message key="common.warning.operationWillModifyImmediately"/></h3>
	<%
		//System.out.println( "request tenantId : " + tenantId );
		%>
		</div>
		<div class="row">
		<div id="userTypeContent" class="col-sm-6 scrollList">
		
		</div>
		<div class="col-sm-6">
		<div class="form-group">
		<bean:message key="global.add"/> User Type
		</div>
		<div class="form-group">
		<input type="hidden" id="userTypeId" name="userTypeId" value="-1">
		<div><bean:message key="global.name"/>：<input type="text" id="userTypeName" name="userTypeName"></div>
		</div>
		<div class="form-group">
		<div><bean:message key="global.code"/>：<input type="text" id="userTypeCode" name="userTypeCode"> </div>
		</div>
		<div class="form-group">
		<button type="button" class="btn btn-default btnUserTypeSubmit" onclick="saveOrUpdateChannelUserType('userType')"><bean:message key="global.add"/></button>
		</div>
		</div>
		
		</div>
		</div>
	
	</div>
	<%
}
else if ("save".equalsIgnoreCase(action)) {
	if(idStr == null && LicenseChecker.isOverTenantLimit()) {
		out.println(MessageUtil.getMessage(sessionLocale, "license.tenant.exceed.limitation")); 
	}
	else {
	Tenant ds = idStr == null ? new Tenant() : Tenant.get(Integer.parseInt(idStr));

	ds.setName(request.getParameter("name"));
	ds.setEmail(StringUtils.trimToNull(request.getParameter("email")));
	ds.setAddress(StringUtils.trimToNull(request.getParameter("address")));
	ds.setTel(StringUtils.trimToNull(request.getParameter("tel")));
	ds.setCoreName(StringUtils.trimToNull(request.getParameter("coreName")));
	ds.setQaCategory(StringUtils.trimToNull(request.getParameter("qaCategory")));
	ds.setSpecificEservice(StringUtils.trimToNull(request.getParameter("specificEservice")));
	ds.setNotes(StringUtils.trimToNull(request.getParameter("notes")));
	ds.setFaqJson(StringUtils.trimToNull(request.getParameter("faqJson")));
	ds.setLineBotConfigJson(StringUtils.trimToNull(request.getParameter("botConfigJson")));

	if (StringUtils.trimToNull(request.getParameter("locale")) != null) {
		ds.setLocale(com.intumit.hithot.HitHotLocale.valueOf(StringUtils.trimToNull(request.getParameter("locale"))));
	}


	ds.setOtherMltQANum(Integer.parseInt(request.getParameter("otherMltQANum")));
	ds.setEnableTenant(request.getParameter("enableTenant") != null);
	ds.setEnableDebug(request.getParameter("enableDebug") != null);
	ds.setEnableQAExplain(request.getParameter("enableQAExplain") != null);
	ds.setEnableContextMatch(request.getParameter("enableContextMatch") != null);
	ds.setEnableRestrictToQaCategory(request.getParameter("enableRestrictToQaCategory") != null);
	ds.setEnableRestrictToKnowledgePoint(request.getParameter("enableRestrictToKnowledgePoint") != null);
	ds.setEnablePhoneticHomonym(request.getParameter("enablePhoneticHomonym") != null);
	ds.setEnableEssentialKeywordMode(request.getParameter("enableEssentialKeywordMode") != null);
	ds.setEnableTextCrm(request.getParameter("enableTextCrm") != null);
	ds.setEnableScenario(request.getParameter("enableScenario") != null);
	ds.setEnableAutoLearning(request.getParameter("enableAutoLearning") != null);
	ds.setEnableMultiLocale(request.getParameter("enableMultiLocale") != null);
	
	ds.setEnableScenarioCrm(request.getParameter("enableScenarioCrm") != null);
	
	ds.setAllowedLocales(StringUtils.trimToNull(request.getParameter("allowedLocales")));
	ds.setKmsUrl(StringUtils.trimToNull(request.getParameter("kmsUrl")));
	ds.setCrmApiBaseUrl(request.getParameter("crmApiBaseUrl"));
	ds.setForceIgnoreCharacters(StringUtils.trimToNull(request.getParameter("forceIgnoreCharacters")));
	ds.setCasualCoreUrl(StringUtils.trimToNull(request.getParameter("casualCoreUrl")));
	ds.setOpendataCoreUrl(StringUtils.trimToNull(request.getParameter("opendataCoreUrl")));
	ds.setStatisticsCoreUrl(StringUtils.trimToNull(request.getParameter("statisticsCoreUrl")));
	ds.setOuterSourceUrl(StringUtils.trimToNull(request.getParameter("outerSourceUrl")));
	// deprecated // ds.setAllowedCustomDataFieldNames(StringUtils.trimToNull(request.getParameter("allowedCustomDataFieldNames")));
	// deprecated // ds.setAllowedOptionsJson(StringUtils.trimToNull(request.getParameter("allowedOptionsJson")));
	ds.setDefaultTextCrmDepartmentId(Integer.parseInt(request.getParameter("defaultTextCrmDepartmentId")));
	ds.setThreshold1(Integer.parseInt(request.getParameter("threshold1")));
	ds.setThreshold2(Integer.parseInt(request.getParameter("threshold2")));
	ds.setThreshold3(Integer.parseInt(request.getParameter("threshold3")));
	ds.setTooManyRepeatThreshold(Integer.parseInt(request.getParameter("tooManyRepeatThreshold")));
	ds.setBoost1(Integer.parseInt(request.getParameter("boost1")));
	ds.setBoost2(Integer.parseInt(request.getParameter("boost2")));
	ds.setEnableQaAudit(request.getParameter("enableQaAudit") != null);
	ds.setEnableUserFrontEnd(request.getParameter("enableUserFrontEnd") != null);

	// 限流區
	ds.setEnableRateLimitByIP(request.getParameter("enableRateLimitByIP") != null);
	ds.setEnableRateLimitByQAContext(request.getParameter("enableRateLimitByQAContext") != null);
	ds.setRateLimitByIpPerSec(Integer.parseInt(request.getParameter("rateLimitByIpPerSec")));
	ds.setRateLimitByQAContextPerSec(Integer.parseInt(request.getParameter("rateLimitByQAContextPerSec")));
	
	// 其他
	ds.setSessionExpirationSecond(Integer.parseInt(request.getParameter("sessionExpirationSecond")));
	
	/*
	// 新壽增加欄位
	ds.setEnableUserLogin(request.getParameter("enableUserLogin") != null);
	ds.setEnableUploadFileInRichText(request.getParameter("enableUploadFileInRichText") != null);
	*/
	
	ds.setMaxBatchUploadSize(Integer.valueOf(StringUtils.defaultString(request.getParameter("maxBatchUploadSize"), "200")));

	/*if (request.getParameter("enableApikey") != null) {
		if (StringUtils.isEmpty(ds.getApikey())) {
			//ds.setApikey(Tenant.genApikey());
		}
	}
	else {
		ds.setApikey(null);
	}*/

	Tenant.saveOrUpdate(ds);
	
	// 公司別建立後也順便建立預設的EventType
	if (EventType.listAll(ds.getId(), null).size() == 0){
		EventType.initBuiltInTypes(ds.getId(), null);
	}
	 // 公司別建立後設定預設apikey
    if (Apikey.list(ds.getId()).size()==0){
        Apikey key = new Apikey();
        key.setTenantId(ds.getId());
        key.setEnableApikey(true);
        key.setName("default");
        key.setApikey(key.genApikey());
        Apikey.saveOrUpdate(key);
    }
	// 另外處理管理者設定
	Set<String> managers = new HashSet<String>();
	if (request.getParameterValues("managers") != null) {
		managers.addAll(Arrays.asList(request.getParameterValues("managers")));
	}
	for (AdminUser user: AdminUserFacade.getInstance().listAll()) {
		Set<Integer> ids = user.getTenantIdSet();
		if (ids.contains(ds.getId()) && !managers.contains("" + user.getId())) {
			ids.remove(ds.getId());
			user.setTenantIds(StringUtils.join(ids, ","));
			AdminUserFacade.getInstance().saveOrUpdate(user);
		}
		else if (!ids.contains(ds.getId()) && managers.contains("" + user.getId())) {
			ids.add(ds.getId());
			user.setTenantIds(StringUtils.join(ids, ","));
			AdminUserFacade.getInstance().saveOrUpdate(user);
		}
	}
	%>
	Saved!
	<%
	}
}
else if ("sudo".equalsIgnoreCase(action)) {
	Tenant ds = Tenant.get(Integer.parseInt(idStr));
	Tenant.setSession(session, ds);
}
else if ("delete".equalsIgnoreCase(action) || "cleanAll".equalsIgnoreCase(action)) {
	String validation = request.getParameter("captcha");
	String rightCaptcha = com.intumit.solr.servlet.CaptchaServlet.getValidationCode(session, "tenant");
	
	if (StringUtils.isEmpty(validation) || !StringUtils.equalsIgnoreCase(validation, rightCaptcha)) {
		session.setAttribute("TENANT_DEL_ID", idStr);
		Tenant t = idStr == null ? new Tenant() : Tenant.get(Integer.parseInt(idStr));
		%>
		<div class="row alert alert-danger">
			<h3>即將清除整個公司別「<%= t.getName() %> - <%= StringUtils.trimToEmpty(t.getNotes()) %>) (<%= t.getId() %>)」</h3>
			<h3>若要清除部分資料，請點選<button class='btn btn-default' data-id='<%= t.getId() %>' id="btnOpenTenantDataTypeSelection">欲清除的資料</button></h3>
			<h3>若確認要進行此危險操作，請輸入下列影像數字：</h3>
			<div class="col-md-2">
			<img src="<%= request.getContextPath() %>/captcha?attr=tenant&fn=<%= System.currentTimeMillis() %>.jpg">
			</div>
			<div class="col-md-2">
			<form action="tenantAdmin.jsp">
			<input type="hidden" name="id" value="<%= idStr %>">
			<input type="hidden" name="action" value="<%= action %>">
			<input type="text" class='form-control' name="captcha"><br>
			<textarea id="choosedDataType" name="choosedDataType" style="display: none;"></textarea>
			<input type="submit" class="btn btn-primary">
			</form>
			</div>
		</div>
		<%
	}
	else if (idStr != null) {
		Tenant t = idStr == null ? new Tenant() : Tenant.get(Integer.parseInt(idStr));
		if (!StringUtils.equals(idStr, (String)session.getAttribute("TENANT_DEL_ID"))) return;

		if (t != null) {
			JSONObject choosed = new JSONObject(StringUtils.defaultString(StringUtils.trimToEmpty(request.getParameter("choosedDataType")), "{}"));
			
			if (choosed.optBoolean("docs", Boolean.TRUE)) {
				out.println(wrap("刪除所有知識點.....", "h3"));
				SolrServer server = t.getCoreServer4Write();
				server.deleteByQuery("*:*");
				server.commit();
				
				List<com.intumit.solr.robot.connector.line.RichMessage> rmList = com.intumit.solr.robot.connector.line.RichMessage.list(t.getId(), null);
				out.println(wrap("" + rmList.size() + " LINE圖文 to be delete...", "h3"));
				for (com.intumit.solr.robot.connector.line.RichMessage rm: rmList) {
					com.intumit.solr.robot.connector.line.RichMessage.delete(rm);
				}
				
				/*
				List<com.intumit.solr.robot.connector.web.RichMessage> webRMList = com.intumit.solr.robot.connector.web.RichMessage.list(t.getId(), null);
				out.println(wrap("" + webRMList.size() + " WEB圖文 to be delete...", "h3"));
				for (com.intumit.solr.robot.connector.web.RichMessage rm: webRMList) {
					com.intumit.solr.robot.connector.web.RichMessage.delete(rm);
				}
				*/
			}

			if (choosed.optBoolean("mcas", Boolean.TRUE)) {
				List<MultiChannelAnswer> mcaList = MultiChannelAnswer.list(t.getId());
				out.println(wrap("" + mcaList.size() + " MultiChannelAnswers to be delete...", "h3"));
				for (MultiChannelAnswer mca: mcaList) {
					MultiChannelAnswer.delete(mca);
				}
			}

			if (choosed.optBoolean("syns", Boolean.TRUE)) {
				List<SynonymKeyword> syns = SynonymKeywordFacade.getInstance().listAll(t.getId());
				out.println(wrap("" + syns.size() + " 同義詞組 to be delete...", "h3"));
				for (SynonymKeyword obj: syns) {
					SynonymKeywordFacade.getInstance().delete(t.getId(), Long.parseLong("" + obj.getId()));
				}
			}

			if (choosed.optBoolean("dicts", Boolean.TRUE)) {
				List<DictionaryDatabase> dicts = DictionaryDatabase.listByTenantId(t.getId());
				out.println(wrap("" + dicts.size() + " 詞庫詞組 to be delete...", "h3"));
				for (DictionaryDatabase obj: dicts) {
					DictionaryDatabase.delete(t.getId(), Long.parseLong("" + obj.getId()));
				}
			}

			if (choosed.optBoolean("channels", Boolean.TRUE)) {
				List<QAChannel> chList = QAChannel.list(t.getId());
				out.println(wrap("" + chList.size() + " channels to be delete...", "h3"));
				for (QAChannel obj: chList) {
					QAChannel.delete(obj);
				}
			}

			if (choosed.optBoolean("userTypes", Boolean.TRUE)) {
				List<QAUserType> utList = QAUserType.list(t.getId());
				out.println(wrap("" + utList.size() + " usertypes to be delete...", "h3"));
				for (QAUserType obj: utList) {
					QAUserType.delete(obj);
				}
			}

			if (choosed.optBoolean("qapatterns", Boolean.TRUE)) {
				List<QAPattern> qpList = QAPattern.list(t.getId());
				out.println(wrap("" + qpList.size() + " 特殊答案 to be delete...", "h3"));
				for (QAPattern obj: qpList) {
					QAPattern.delete(t.getId(), obj.getId());
				}
			}

			if (choosed.optBoolean("rfa", Boolean.TRUE)) {
				List<RobotFormalAnswers> rfaList = RobotFormalAnswers.list(t.getId());
				out.println(wrap("" + rfaList.size() + " 制式文案 to be delete...", "h3"));
				for (RobotFormalAnswers obj: rfaList) {
					RobotFormalAnswers.delete(t.getId(), obj.getKeyName());
				}
			}

			if (choosed.optBoolean("altTpls", Boolean.TRUE)) {
				List<QAAltTemplate> altTpls = QAAltTemplate.list(t.getId());
				out.println(wrap("" + altTpls.size() + " 例句範本 to be delete...", "h3"));
				for (QAAltTemplate obj: altTpls) {
					QAAltTemplate.delete(t.getId(), obj.getId());
				}
			}

			if (choosed.optBoolean("sbts", Boolean.TRUE)) {
				List<SegmentBatchTask> sbts = SegmentBatchTask.list(t.getId(), null);
				out.println(wrap("" + sbts.size() + " 批次校詞 to be delete...", "h3"));
				for (SegmentBatchTask obj: sbts) {
					SegmentBatchTask.delete(t.getId(), "" + obj.getId());
				}
			}

			if (choosed.optBoolean("intents", Boolean.TRUE)) {
				List<QAIntent> intents = QAIntent.listByTenantId(t.getId());
				out.println(wrap("" + intents.size() + " intents to be delete...", "h3"));
				for (QAIntent obj: intents) {
					QAIntent.delete(t.getId(), "" + obj.getId());
				}
			}

			if (choosed.optBoolean("entities", Boolean.TRUE)) {
				List<QAEntity> entities = QAEntity.listByTenantId(t.getId());
				out.println(wrap("" + entities.size() + " entities to be delete...", "h3"));
				for (QAEntity obj: entities) {
					QAEntity.delete(t.getId(), "" + obj.getId());
				}
			}

			if (choosed.optBoolean("dialogs", Boolean.TRUE)) {
				List<QADialogConfig> dlgConfigs = QADialogConfig.list(t.getId());
				out.println(wrap("" + dlgConfigs.size() + " Dialogs to be delete...", "h3"));
				for (QADialogConfig obj: dlgConfigs) {
					QADialogConfig.delete(t.getId(), obj.getId());
				}
			}

			if (choosed.optBoolean("dialogVers", Boolean.TRUE)) {
				List<QADialogConfigVersion> dlgConfigs = QADialogConfigVersion.list(t.getId());
				out.println(wrap("" + dlgConfigs.size() + " Dialog versions to be delete...", "h3"));

				/* 直接下Query刪除，好像不用特別做成 Method... */
				org.hibernate.Session ses = null;
				try {
					ses = com.intumit.hibernate.HibernateUtil.getSession();
					org.hibernate.Transaction tsc = ses.beginTransaction();
					StringBuffer hql = new StringBuffer("delete from QADialogConfigVersion where tenantId = :tenantId ");
		
					org.hibernate.Query query = ses.createSQLQuery(hql.toString());
					query.setInteger("tenantId", t.getId());
					query.executeUpdate();
					tsc.commit();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ses.close();
				}
			}

			if (choosed.optBoolean("ele", Boolean.TRUE)) {
				int eleCount = EvaluationLogEntity.countBy(t.getId(), QAUtil.CURRENT_EVALUATION_LOG_NS, null, null, null, null, null, null, null, false, null, null, null, null).intValue();
				out.println(wrap("" + eleCount + " Evaluation logs to be delete...", "h3"));
	
		
				/* 直接下Query刪除，好像不用特別做成 Method... */
				org.hibernate.Session ses = null;
				try {
					ses = com.intumit.hibernate.HibernateUtil.getSession();
					org.hibernate.Transaction tsc = ses.beginTransaction();
					StringBuffer hql = new StringBuffer("delete from EvaluationLogEntity where tenantId = :tenantId ");
		
					org.hibernate.Query query = ses.createSQLQuery(hql.toString());
					query.setInteger("tenantId", t.getId());
					query.executeUpdate();
					tsc.commit();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ses.close();
				}
			}

			if (choosed.optBoolean("sle", Boolean.TRUE)) {
				int sleCount = ServiceLogEntity.countBy(t.getId(), null, null, null, null, null, null, null).intValue();
				out.println(wrap("" + sleCount + " Service logs to be delete...", "h3"));
		
				/* 直接下Query刪除，好像不用特別做成 Method... */
				org.hibernate.Session ses = null;
				try {
					ses = com.intumit.hibernate.HibernateUtil.getSession();
					StringBuffer hql = new StringBuffer("delete from ServiceLogEntity where tenantId = :tenantId ");
		
					org.hibernate.Transaction tsc = ses.beginTransaction();
					org.hibernate.Query query = ses.createSQLQuery(hql.toString());
					query.setInteger("tenantId", t.getId());
					query.executeUpdate();
					tsc.commit();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					ses.close();
				}
			}

			if (choosed.optBoolean("statistics", Boolean.TRUE)) {
				/* 刪除統計資料 */
				try {
					SolrServer statServer = t.getStatisticsSolrServer();
					SolrQuery deleteQ = new SolrQuery("+TenantId_i:" + t.getId());
					long statCount = statServer.query(deleteQ).getResults().getNumFound();
					out.println(wrap("" + statCount + "筆統計資料 to be delete...", "h3"));
					statServer.deleteByQuery("+TenantId_i:" + t.getId());
					statServer.commit();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			// 後面這些是 default 必砍
			List<EventType> eventType = EventType.listAll(t.getId(), null);
			out.println(wrap("" + eventType.size() + " eventType to be delete...", "h3"));
			for (EventType obj: eventType) {
				EventType.delete(t.getId(), obj.getId());
			}
			
			try {
				// 對應專屬權限砍一砍，避免有些特殊漏洞產生
				List<TenantAdminGroup> tags = TenantAdminGroup.listByTenant(t.getId());	
				
				if (tags != null && tags.size() > 0) {
					for (TenantAdminGroup tag: tags) {
						TenantAdminGroup.delete(tag.getId());
					}
				}
				
				// Loop 所有用戶，把可管理該公司別的設定清除
				for (AdminUser user: AdminUserFacade.getInstance().listAll()) {
					Set<Integer> idSet = user.getTenantIdSet();
					if (idSet.contains(t.getId())) {
						idSet.remove(new Integer(t.getId()));
						user.setTenantIds(StringUtils.join(idSet, ","));
						AdminUserFacade.getInstance().saveOrUpdate(user);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			if ("delete".equalsIgnoreCase(action)) {
				Tenant.delete(t.getId());
			}
		}
	}
}
%>
<script>
$(function() {
	$.datepicker.setDefaults({
		dateFormat: 'yy/mm/dd'
	});
    $( 'input[name^="date"]' ).datepicker();
});

</script>
<TABLE width="100%" class="table table-striped" id="tenants">
	<THEAD>
	<TR>
		<TH class="text-center" valign="top"><bean:message key='num'/></TH>
		<TH class="text-center" valign="top"><bean:message key='tenant.name'/></TH>
		<TH class="text-center" valign="top"><bean:message key='use.index'/></TH>
		<TH class="text-center" valign="top"><bean:message key='is.enabled'/></TH>
		<TH class="text-center" valign="top"><bean:message key='tenant.note'/></TH>
		<TH class="text-center" valign="top"><bean:message key='operation'/></TH>
	</TR>
	</THEAD>
<%
List<Tenant> list = Tenant.list();
for (int i=0; i < list.size(); i++) {
	Tenant admGrp = list.get(i);
	int id = admGrp.getId();
%>
	<TR>
		<TD class="text-center" valign="top" class="col-md-1"><%= admGrp.getId() %></TD>
		<TD class="text-center" valign="top" class="col-md-1"><%= admGrp.getName() %></TD>
		<TD class="text-center" valign="top" class="col-md-2"><%= admGrp.getCoreName() %></TD>
		<TD class="text-center" valign="top" class="col-md-2">
			<input type="checkbox" disabled <%= admGrp.getEnableTenant() ? " checked" : "" %> data-toggle="toggle" data-on="Enabled" data-off="Disabled" data-onstyle="success" data-offstyle="danger">
		</TD>
		<TD class="text-center" valign="top" class="col-md-2"><%= admGrp.getNotes() %></TD>
		<TD class="text-center" valign="top" class="col-md-4">
			<a class="btn btn-success" target="_chooseTenant" href="chooseTenant.jsp?tid=<%= id %>"><bean:message key='transfiguration'/></a>
			<a class="btn btn-primary" href="tenantAdmin.jsp?action=EDIT&id=<%= id %>"><bean:message key='modify'/></a>
			<% if(!LicenseChecker.isOverTenantLimit()) { %> 
			<a class="btn btn-default" onclick="return confirm('<bean:message key="sure.copy.tenant"/>');" href="tenantAdmin.jsp?action=DUPLICATE&id=<%= id %>"><bean:message key='copy'/></a>
			<% } %>
			&nbsp;
			<a class="btn btn-primary btnFlow" href="matchCtrlFlowAdmin.jsp?action=EDIT&id=<%= id %>"><bean:message key='qa.matchCtrlFlow.modify'/></a>
			
			<div class="btn-group" role="group" aria-lable="bg-others">
				<button class="btn btn-warning dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
				    <bean:message key='other.menu'/>
				    <span class="caret"></span>
				</button>
				<ul class="dropdown-menu" aria-labelledby="dropdownMenu1" style="text-align: left;">
					<li><a href="apikeyAdmin.jsp?tid=<%= id %>">APIKEYs</a></li>
					<li><a href="tenantImportUpload.jsp?id=<%= id %>"><bean:message key='global.import'/></a></li>
					<li><a href="tenantExporter.jsp?id=<%= id %>"><bean:message key='search.export'/></a></li>
					<li role="separator" class="divider"></li>
				    <li role="separator" class="divider"></li>
					<% if(!LicenseChecker.isOverTenantLimit()) { %> 
					<li><a onclick="return confirm('<bean:message key="sure.del.tenant2"/>');" href="tenantAdmin.jsp?action=DELETE&id=<%= id %>"><bean:message key='delete'/></a></li>
				    <li role="separator" class="divider"></li>
				    <li role="separator" class="divider"></li>
					<% } %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() > 0
|| ("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
%>
					<li><a onclick="return confirm('<bean:message key="sure.cleanAll.tenant"/>');" href="tenantAdmin.jsp?action=CLEANALL&id=<%= id %>"><bean:message key='cleanAll'/></a></li>
<%
}
%>
				</ul>
			</div>
		</TD>
	</TR>
<%
}
%>
</TABLE>
<% if(!LicenseChecker.isOverTenantLimit()) { %>
<A class="btn btn-danger" target="mainFrame" HREF="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/tenantAdmin.jsp?action=create"><bean:message key='create.new.tenant'/></A>
<% } %>
<div id="tenantStatusDlg" title="預覽資料狀態" style="display:none; z-index:999; background-color: white;">
</div>
<script>
$('select[name=managers]').select2();
$('.slider').bootstrapSlider();

<% if (detailForm) { %>
$('#otherMltQANumSlider').bootstrapSlider().on('slide', function(_ev) {
    $('#showOtherMltQANumVal').text(_ev.value);
});
$('#threshold1').bootstrapSlider().on('slide', function(_ev) {
    $('#showThreshold1Val').text(_ev.value);
});
$('#threshold2').bootstrapSlider().on('slide', function(_ev) {
    $('#showThreshold2Val').text(_ev.value);
});
$('#threshold3').bootstrapSlider().on('slide', function(_ev) {
    $('#showThreshold3Val').text(_ev.value);
});
$('#tooManyRepeatThreshold').bootstrapSlider().on('slide', function(_ev) {
    $('#showTooManyRepeatThresholdVal').text(_ev.value);
});
$('#maxBatchUploadSize').bootstrapSlider().on('slide', function(_ev) {
    $('#showMaxBatchUploadSizeVal').text(_ev.value);
});
$('#boost1').bootstrapSlider().on('slide', function(_ev) {
    $('#showBoost1Val').text(_ev.value);
});
$('#boost2').bootstrapSlider().on('slide', function(_ev) {
    $('#showBoost2Val').text(_ev.value);
});
<% } %>

var faqJsonEditor, botConfigJsonEditor;

if ($('#faqJson').length > 0) {
	faqJsonEditor = CodeMirror.fromTextArea(document.getElementById("faqJson"), {
	   lineNumbers: true,
	   matchBrackets: true,
	   mode: "javascript",
	 });
}

if ($('#botConfigJson').length > 0) {
	botConfigJsonEditor = CodeMirror.fromTextArea(document.getElementById("botConfigJson"), {
	   lineNumbers: true,
	   lineWrapping: true,
	   matchBrackets: true,
	   mode: "javascript",
	 });
}

$( "#channel-management-dialog").dialog({
	minWidth: 800,
	minHeight: 600,
	height: 600,
	autoOpen: false
});

$( '#btnOpenChannelManagement').click(function() {
	$('#userType-management-dialog').dialog('close');
	$('#channel-management-dialog').dialog('open');
});

$( "#userType-management-dialog").dialog({
	minWidth: 800,
	minHeight: 600,
	height: 600,
	autoOpen: false
});

$( '#btnOpenUserTypeManagement').click(function() {
	$('#channel-management-dialog').dialog('close');
	$('#userType-management-dialog').dialog('open');
});

function loadList(data) {
	loadChannel(data.channel);
	loadUserType(data.userType);
}

function delChannelUserType(type, id) {
	if (confirm('Are you sure?')) {
		$.post('multiChannelUserTypeDel.jsp',
			{
			tenantId: <%= idStr %>,
			type:type,
			id:id
			},
			reLoadSingleSide,
			"json"
		);
	}
}

function saveOrUpdateChannelUserType(type) {
	if (confirm('Are you sure?')) {
		var postData = {
				tenantId: <%= idStr %>,
				type:type,
			};
		if (type=='channel') {
			postData['id'] = $("#channelId").val();
			postData['name'] = $("#channelName").val();
			postData['code'] = $("#channelCode").val();
			postData['defaultChannelCode'] = $("#channelDefaultChannelCode").val();
			postData['channelType'] = $("#channelType").val();
			postData['appendOptionToOutput'] = $("#appendOptionToOutput").prop( "checked" );
			postData['returnOptionInJson'] = $("#returnOptionInJson").prop( "checked" );
			postData['useHtmlNewline'] = $("#useHtmlNewline").prop( "checked" );
			postData['supportMultiRichMessages'] = $("#supportMultiRichMessages").prop( "checked" );
			postData['useCustomMatchCtrlFlow'] = $("#useCustomMatchCtrlFlow").prop( "checked" );
		} else {
			postData['id'] = $("#userTypeId").val();
			postData['name'] = $("#userTypeName").val();
			postData['code'] = $("#userTypeCode").val();
		}
		$.post('multiChannelUserTypeAdd.jsp',
			postData,
			function(data) {
				reLoadSingleSide(data);
				clearForm('channel');
			},
			"json"
		);
	}
}

$(document).on('click', '.btnEditChannel', function() {
	var chData = $(this).parent().data("channel");

	$("#channelId").val(chData.id);
	$("#channelName").val(chData.name);
	$("#channelCode").val(chData.code);
	$("#channelDefaultChannelCode").val(chData.defaultChannelCode);
	
	if (chData.code == '<%= QAChannel.DEFAULT_CHANNEL_CODE %>') {
		$('#channelDefaultChannelCode').attr('disabled', 'disabled');
		$('#supportMultiRichMessagesFont').text('「花旗配合前端版型，此選項不可開啟」');
	}
	else {
		$('#channelDefaultChannelCode').removeAttr('disabled');
		$('#channelDefaultChannelCode option').removeAttr('disabled');
		$('#channelDefaultChannelCode option[value=' + chData.code + ']').attr('disabled', "disabled");
		$('#supportMultiRichMessagesFont').text('');
	}
	$("#channelType").val(chData.type);
	
	$('#appendOptionToOutput').bootstrapToggle(chData.hasOwnProperty("appendOptionToOutput") && chData.appendOptionToOutput ? 'on' : 'off');
	$('#returnOptionInJson').bootstrapToggle(chData.hasOwnProperty("returnOptionInJson") && chData.returnOptionInJson ? 'on' : 'off');
	$('#useHtmlNewline').bootstrapToggle(chData.hasOwnProperty("useHtmlNewline") && chData.useHtmlNewline ? 'on' : 'off');
	$('#supportMultiRichMessages').bootstrapToggle(chData.hasOwnProperty("supportMultiRichMessages") && chData.supportMultiRichMessages ? 'on' : 'off');
	$('#useCustomMatchCtrlFlow').bootstrapToggle(chData.hasOwnProperty("useCustomMatchCtrlFlow") && chData.useCustomMatchCtrlFlow ? 'on' : 'off');
	$('#btnChannelMatchCtrlFlow').data('target-channel-id', chData.id);
	$('#btnChannelMatchCtrlFlow').removeAttr('disabled');

	$('.btnChannelSubmit').text('<bean:message key="global.update"/>');
});

function reLoadSingleSide(data) {
	if (data.fail != null) {
		alert(data.fail);
		return;
	}
	if (data.channel != null) {
		loadChannel(data.channel);
	} else {
		loadUserType(data.userType);
	}
}

function clearForm(type) {
	if (type=='channel') {
		$("#channelId").val("-1");
		$("#channelName").val("");
		$("#channelCode").val("");
		$("#channelType").val("PLAIN_TEXT");
		$("#channelDefaultChannelCode").val("<%= QAChannel.DEFAULT_CHANNEL_CODE %>");
		
		// Default appendOptionToOuput = true
		$('#appendOptionToOutput').bootstrapToggle('on');
		$('#returnOptionInJson').bootstrapToggle('off');
		$('#useHtmlNewline').bootstrapToggle('off');
		$('#useCustomMatchCtrlFlow').bootstrapToggle('off');
		
		$('#btnChannelMatchCtrlFlow').attr('disabled', 'disabled');
		
		
		$('.btnChannelSubmit').text('<bean:message key="global.add"/>');
	}
	else if (type=='userType') {
	}
}

function loadChannel(chData) {
	var len = chData.length;
	$("#channelContent").empty();
	
	for (var i=0; i<len; i++) {
		var $html = $('<div class="btn-group col-sm-12"/>');
		var html = '<button type="button" class="btn btn-default btnEditChannel col-sm-9">';
		html += chData[i].name+' - ' + chData[i].code + ' (' + chData[i].type + ')';
		html += '</button>';
		
		if (chData[i].code != 'web' && chData[i].code != 'app') {
			html += '<button type="button" class="btn btn-default" onclick="delChannelUserType(\'channel\','+chData[i].id+')">';
			html += '<span class="glyphicon glyphicon-remove" aria-hidden="true"></span>';
			html += '</button>';
		}
		
		$html.html(html);
		$html.data("channel", chData[i]);
		
		$("#channelContent").append($html);
	}
}

function loadUserType(utData) {
	var len = utData.length;
	$("#userTypeContent").empty();
	for (var i=0; i<len; i++) {
		//alert("id:"+utData[i].id+" name:"+utData[i].name+" code:"+utData[i].code);

		var html = '<div class="btn-group col-sm-12">';
		html += '<button type="button" class="btn btn-default col-sm-9">';
		html += utData[i].name+' - ' + utData[i].code;
		html += '</button>';
		if (utData[i].code != 'unknown') {
			html += '<button type="button" class="btn btn-default" onclick="delChannelUserType(\'userType\','+utData[i].id+')">';
			html += '<span class="glyphicon glyphicon-remove" aria-hidden="true"></span>';
			html += '</button>';
		}
		html += '</div>';
		$("#userTypeContent").append(html);
	}
}

$("#dialog").dialog({
    autoOpen: false,
    position: 'center' ,
    title: 'EDIT',
    draggable: false,
    width : 300,
    height : 40, 
    resizable : false,
    modal : true,
});

$('#btnChannelMatchCtrlFlow').click(function() {
	window.open('matchCtrlFlowAdmin.jsp?action=EDIT&target=channel&id=' + $(this).data('target-channel-id'));
});

function choosedDataType(data) {
	$('#choosedDataType').text(JSON.stringify(data));
}

$("#btnOpenTenantDataTypeSelection").click( function() {
	tid = $(this).attr('data-id');
    $("#tenantStatusDlg").load('tenantStatus.jsp?id=' + tid + "&cb=choosedDataType", function() {
        $("#tenantStatusDlg").dialog("open");
    });
});

$(document).ready(function() {
	$( "#tenantStatusDlg").dialog({
		minWidth: 600,
		minHeight: 600,
		height: 600,
		autoOpen: false
	});
	
	$.post('multiChannelUserTypeList.jsp',
			{
			tenantId: <%= idStr %>,
			},
			loadList,
			"json"
		);

	$('#tenants').DataTable(
		{ 
			"paging": false, 
			"stateSave": true,
			"dom": '<<"row"<"col-md-3 dt-mt"f><"col-md-3 dt-mt"l>><"clear">t<"clear"><"row"<"col-md-3 dt-mt"f><"col-md-3 dt-mt"l>>>',
			"order": [[ 1, "asc" ]],
			"columnDefs": [
			 { "type": "numeric-comma", targets: 0 }
	        ]
		});
});
</script>
</BODY>
</HTML>
