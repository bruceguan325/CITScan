<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	 import="javax.servlet.ServletConfig"
	 import="javax.servlet.ServletException"
	 import="javax.servlet.http.*"
	 import="java.io.*"
	 import="java.net.*"
	 import="java.text.*"
	 import="java.util.*"
	 import="org.apache.commons.io.*"
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
	 import="com.intumit.systemconfig.*"
	 import="org.apache.commons.lang.StringUtils"
	 import="com.intumit.quartz.ScheduleUtils"
	 import="com.intumit.quartz.Job"
     import="org.dom4j.*"
	 import="org.apache.solr.client.solrj.SolrQuery"
	import="com.intumit.solr.admin.*"
%><%!
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
%>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E3) == 0) {
	return;
}
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='wisesystemconfig.set'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/styles/bootstrap-slider.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bootstrap-slider.js"></script>
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<%
WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();
String action = request.getParameter("action");
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
com.intumit.solr.tenant.Tenant tenant = com.intumit.solr.tenant.Tenant.get(t.getId());

if ("save".equalsIgnoreCase(action)) {
	// 這邊只留允許讓客戶自行設定的欄位
	tenant.setName(request.getParameter("name"));
	tenant.setEmail(StringUtils.trimToNull(request.getParameter("email")));
	tenant.setAddress(StringUtils.trimToNull(request.getParameter("address")));
	tenant.setTel(StringUtils.trimToNull(request.getParameter("tel")));
	//tenant.setCoreName(StringUtils.trimToNull(request.getParameter("coreName")));
	tenant.setQaCategory(StringUtils.trimToNull(request.getParameter("qaCategory")));
	tenant.setSpecificEservice(StringUtils.trimToNull(request.getParameter("specificEservice")));
	tenant.setNotes(StringUtils.trimToNull(request.getParameter("notes")));
	//tenant.setFaqJson(StringUtils.trimToNull(request.getParameter("faqJson")));

	tenant.setOtherMltQANum(Integer.parseInt(request.getParameter("otherMltQANum")));
	
	//tenant.setEnableTenant(request.getParameter("enableTenant") != null);
	//tenant.setEnableDebug(request.getParameter("enableDebug") != null);
	//tenant.setEnableQAExplain(request.getParameter("enableQAExplain") != null);
	tenant.setEnableContextMatch(request.getParameter("enableContextMatch") != null);
	tenant.setEnableRestrictToQaCategory(request.getParameter("enableRestrictToQaCategory") != null);
	tenant.setEnablePhoneticHomonym(request.getParameter("enablePhoneticHomonym") != null);
	//tenant.setEnableEssentialKeywordMode(request.getParameter("enableEssentialKeywordMode") != null);
	//tenant.setEnableTextCrm(request.getParameter("enableTextCrm") != null);
	//tenant.setEnableScenario(request.getParameter("enableScenario") != null);
	tenant.setEnableAutoLearning(request.getParameter("enableAutoLearning") != null);
	//tenant.setEnableMultiLocale(request.getParameter("enableMultiLocale") != null);
	//tenant.setAllowedLocales(StringUtils.trimToNull(request.getParameter("allowedLocales")));
	tenant.setCrmApiBaseUrl(request.getParameter("crmApiBaseUrl"));
	//tenant.setForceIgnoreCharacters(StringUtils.trimToNull(request.getParameter("forceIgnoreCharacters")));
	//tenant.setCasualCoreUrl(StringUtils.trimToNull(request.getParameter("casualCoreUrl")));
	//tenant.setOpendataCoreUrl(StringUtils.trimToNull(request.getParameter("opendataCoreUrl")));
	//tenant.setOuterSourceUrl(StringUtils.trimToNull(request.getParameter("outerSourceUrl")));
	//tenant.setAllowedCustomDataFieldNames(StringUtils.trimToNull(request.getParameter("allowedCustomDataFieldNames")));
	tenant.setAllowedOptionsJson(StringUtils.trimToNull(request.getParameter("allowedOptionsJson")));
	tenant.setKmsUrl(StringUtils.trimToNull(request.getParameter("kmsUrl")));
	//tenant.setDefaultTextCrmDepartmentId(Integer.parseInt(request.getParameter("defaultTextCrmDepartmentId")));
	tenant.setThreshold1(Integer.parseInt(request.getParameter("threshold1")));
	tenant.setThreshold2(Integer.parseInt(request.getParameter("threshold2")));
	tenant.setThreshold3(Integer.parseInt(request.getParameter("threshold3")));
	tenant.setTooManyRepeatThreshold(Integer.parseInt(request.getParameter("tooManyRepeatThreshold")));
	tenant.setBoost1(Integer.parseInt(request.getParameter("boost1")));
	tenant.setBoost2(Integer.parseInt(request.getParameter("boost2")));
	/*
	tenant.setEnableUserLogin(request.getParameter("enableUserLogin") != null);
	tenant.setEnableUploadFileInRichText(request.getParameter("enableUploadFileInRichText") != null);
	*/

	/*if (request.getParameter("enableApikey") != null) {
		if (StringUtils.isEmpty(tenant.getApikey())) {
			tenant.setApikey(Tenant.genApikey());
		}
	}
	else {
		tenant.setApikey(null);
	}*/

	Tenant.saveOrUpdate(tenant);
	com.intumit.solr.robot.QAUtil.getInstance(tenant.getId()).forceRebuildUserDefiniedDictionary();
	%>
	<h4 class="text text-danger">系統參數已經更新！</h4>
	<%
}
%>
<h2><bean:message key="wisesystemconfig.set"/></h2>
<form action="qaTenantAdmin.jsp" name="myForm" id="myForm" method="post">
<input type="hidden" name="action" value="save">
<table class="table table-bordered table-striped">
<tr>
	<td><bean:message key="tenant.name"/></td>
	<td class="td-form">
		<input type="text" size="40" name="name" value="<%= StringUtils.trimToEmpty(tenant.getName()) %>">
	</td>
	<td><bean:message key="tenant.ex"/></td>
</tr>
<%--tr>
	<td><bean:message key="tenant.core.name"/></td>
	<td class="td-form">
		<input type="text" size="40" name="coreName" value="<%= StringUtils.trimToEmpty(tenant.getCoreName()) %>">
	</td>
	<td><bean:message key="tenant.core.name.ex"/></td>
</tr--%>
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
<%--
<tr>
	<td><bean:message key="tenant.apikey"/></td>
	<td class="td-form">
		<label><%= StringUtils.trimToEmpty(tenant.getApikey()) %></label>
	</td>
	<td><bean:message key="tenant.apikey.ex"/></td>
</tr>
--%>
<tr>
	<td><bean:message key="text.customer.service.website"/></td>
	<td class="td-form">
		<input type="text" size="80" name="crmApiBaseUrl" value="<%= StringUtils.trimToEmpty(tenant.getCrmApiBaseUrl()) %>">
	</td>
	<td><bean:message key="text.customer.service.website.ex"/></td>
</tr>
<%--tr>
	<td><bean:message key="life.langue.index.url"/></td>
	<td class="td-form">
		<input type="text" size="80" name="casualCoreUrl" value="<%= StringUtils.trimToEmpty(tenant.getCasualCoreUrl()) %>">
	</td>
	<td><bean:message key="life.langue.index.desc"/></td>
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
</tr--%>
<tr>
	<td>KMS URL</td>
	<td class="td-form">
		<input type="text" size="80" name="kmsUrl" value="<%= StringUtils.trimToEmpty(tenant.getKmsUrl()) %>">
	</td>
	<td></td>
</tr>
<tr>
	<td><bean:message key="customData.allowedFieldNames"/></td>
	<td class="td-form">
		<input type="text" size="80" name="allowedCustomDataFieldNames" value="<%= StringUtils.trimToEmpty(tenant.getAllowedCustomDataFieldNames()) %>">
	</td>
	<td><bean:message key="customData.allowedFieldNames.desc"/></td>
</tr>
<tr>
	<td><bean:message key="options.show.json"/></td>
	<td class="td-form">
		<input type="text" size="80" name="allowedOptionsJson" value="<%= StringUtils.trimToEmpty(tenant.getAllowedOptionsJson()) %>">
	</td>
	<td><bean:message key="options.show.json.desc"/></td>
</tr>
<%--tr>
	<td><bean:message key="tenant.forceIgnoreCharacters"/></td>
	<td class="td-form">
		<input type="text" size="80" name="forceIgnoreCharacters" value="<%= StringUtils.trimToEmpty(tenant.getForceIgnoreCharacters()) %>">
	</td>
	<td><bean:message key="tenant.forceIgnoreCharacters.ex"/></td>
</tr--%>
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
	<%
    String checked = null;
	checked = tenant.getEnableTenant() ? "checked" : "";
	%>
<%--
   	<input type="checkbox" name="enableTenant" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled"/> <br/>
--%>
	<%
    checked = tenant.getEnableMultiLocale() ? "checked" : "";
	%>
<%--
   	<input type="checkbox" name="enableMultiLocale" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.multiLocale"/> <br/>
--%>
   	<%
    checked = tenant.getEnableScenario() ? "checked" : "";
	%>
<%--
   	<input type="checkbox" name="enableScenario" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.scenario"/> <br/>
--%>
	<%
    checked = tenant.getEnableDebug() ? "checked" : "";
	%>
<%--
   	<input type="checkbox" name="enableDebug" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.debug"/> <br/>
--%>
	<%
    checked = tenant.getEnableQAExplain() ? "checked" : "";
	%>
<%--
   	<input type="checkbox" name="enableQAExplain" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.detail.record"/> <br/>
--%>
	<%
    checked = tenant.getEnableAutoLearning() ? "checked" : "";
	%>
   	<input type="checkbox" name="enableAutoLearning" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.autoLearning"/> <br/>
	<%
    checked = tenant.getEnableEssentialKeywordMode() ? "checked" : "";
	%>
<%--
   	<input type="checkbox" name="enableEssentialKeywordMode" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.essentialKeywordMode"/> <br/>
--%>
	<%
    checked = tenant.getEnableContextMatch() ? "checked" : "";
	%>
   	<input type="checkbox" name="enableContextMatch" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.context"/> <br/>
	<%
    checked = tenant.getEnableRestrictToQaCategory() ? "checked" : "";
	%>
   	<input type="checkbox" name="enableRestrictToQaCategory" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="qacategory.enable"/> <br/>
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
<%--
   	<input type="checkbox" name="enableTextCrm" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.written.customer.service"/>
	 (<bean:message key="department.number"/> <input type="text" name="defaultTextCrmDepartmentId" size="4" value="<%= tenant.getDefaultTextCrmDepartmentId() %>">，<bean:message key="is.enabled.written.customer.service.ex"/>)
   	<br>
--%>
<%--
	<%
    checked = tenant.getApikey() != null ? "checked" : "";
	%>
   	<input type="checkbox" name="enableApikey" <%= checked %> data-size="mini" data-toggle="toggle"> <bean:message key="is.enabled.apikey"/> <br/>
--%>
	</td>
	<td></td>
</tr>
<%--
<tr>
	<td><bean:message key="common.problems"/></td>
	<td class="td-form">
		<textarea rows="20" cols="100" name="faqJson"><%= StringUtils.trimToEmpty(tenant.getFaqJson()) %></textarea>
	</td>
	<td><bean:message key="common.problems.ex1"/><A HREF="http://www.convertcsv.com/csv-to-json.htm"><bean:message key="here"/></A><bean:message key="common.problems.ex2"/>
<pre>
[
	{ "category": "<bean:message key="common.problems.category1"/>",
	  "questions": [
			<bean:message key="common.problems.question1"/>
			]
	},
	{ "category": "<bean:message key="common.problems.category2"/>",
	  "questions": [
			<bean:message key="common.problems.question2"/>
			]
	}
]
</pre>
	</td>
</tr>
--%>
<tr>
	<td colspan=3>
		<input class="btn btn-primary" type="submit" value="<bean:message key='save'/>">
	</td>
</tr>
</table>
</form>
</div>
<script>
$('.slider').bootstrapSlider();
$('#otherMltQANumSlider').bootstrapSlider().on('slideStop', function(_ev) {
    $('#showOtherMltQANumVal').text(_ev.value);
});
$('#threshold1').bootstrapSlider().on('slideStop', function(_ev) {
    $('#showThreshold1Val').text(_ev.value);
});
$('#threshold2').bootstrapSlider().on('slideStop', function(_ev) {
    $('#showThreshold2Val').text(_ev.value);
});
$('#threshold3').bootstrapSlider().on('slideStop', function(_ev) {
    $('#showThreshold3Val').text(_ev.value);
});
$('#tooManyRepeatThreshold').bootstrapSlider().on('slideStop', function(_ev) {
    $('#showTooManyRepeatThresholdVal').text(_ev.value);
});
$('#boost1').bootstrapSlider().on('slideStop', function(_ev) {
    $('#showBoost1Val').text(_ev.value);
});
$('#boost2').bootstrapSlider().on('slideStop', function(_ev) {
    $('#showBoost2Val').text(_ev.value);
});

</script>
</body>
</html>
