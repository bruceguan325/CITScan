<%@page import="org.apache.struts.Globals"%>
<%@page import="com.intumit.message.MessageUtil"%>
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
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.util.*"
import="java.util.Locale"
import="java.util.Base64"
%><%
String ctxPath = request.getContextPath();
//QAUtil.MLT_COMMON_SENSE_FACTOR = 10;
String uuid = UUID.randomUUID().toString();
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);

SolrServer server = t.getCoreServer();
SolrQuery q = new SolrQuery();
q.setQuery("*:*");
q.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
q.addFilterQuery("-isPart_i:[2 TO *]");
q.setRows(0);

boolean allowBatchTest = (admGrp.getSystemAdminCURD() & AdminGroup.A2) > 0;
boolean allowBatchNlp = (admGrp.getSystemAdminCURD() & AdminGroup.A3) > 0;

long total = server.query(q).getResults().getNumFound();
boolean hideUI = Boolean.parseBoolean(request.getParameter("hideUI"));
Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);

List<QAChannel> channels = QAChannel.list(t.getId());
boolean hasLineChannelType = false;
if (channels != null) {
	for (QAChannel ch: channels) {
		if (ch.getType() == QAChannelType.LINE) {
			hasLineChannelType = true;
			break;
		}
	}
}
%>
<style>
.num {
margin: 0 5 0 5;
}
.answer-text-qtp {
  display: block;
  max-width: 580px;
  height: 300px; /* Fallback for non-webkit */
  margin: 0 auto;
  font-size: 18px;
  overflow: hidden;
  overflow-y: auto;
}
.btnNavBtnGroup {
  margin-bottom: 5px;
}
.easterEgg {
  display: none;
}
.mosaic {
    color: transparent;
    text-shadow: 0 0 10px rgba(255,255,255,0.8);
    filter: DXImageTransform.Microsoft.Blur(pixelradius=2);
    zoom: 1;
}
</style>

<div id="site-title-content" <% if(hideUI){ %> style="display: none;" <% } %>>
	<h3 class="site-header"><font color="FFFFFF"> SmartRobot </font></h3>
	<h4 class="site-description">
 	 	<bean:message key='robot.management.interface'/>
 	 	<% if (allowBatchTest) { %>
 	 	<span id="toggle-batch-test-panel" class="text-success glyphicon glyphicon-pushpin"></span>
 	 	<% } %>
    </h4>
</div>
<div id="site-status-content" <% if(hideUI){ %> style="display: none;" <% } %>>
<center>
	<h4 style="margin-top: 20px;"> <%= t.getNotes() %><bean:message key='knowledge.base.state'/> <span class="toggle-test-panel text-success glyphicon glyphicon-pushpin"></span></h4>
	<h5>
	<span><bean:message key='knowledge.total'/>:</span>
	<span class='num text-primary' title="<bean:message key='knowledge.total'/>" data-toggle="tooltip" data-placement="top"><%= total %></span>
	</h5>
</center>
	<script>
	var nsWaiting = false;

    $(function(){
    	$('.num').tooltip();

    	function getNotification() {
    		if (!nsWaiting) {
    			nsWaiting = true;

    			$.ajax({
    				url: "<%= ctxPath %>/nss?t=" + new Date().getTime(),
    				dataType: "json",
    				success: function(notification) {
    					nsWaiting = false;
    					opts = {title: notification.title,
        			            text: notification.content,
        			            type: notification.type,
        			            //desktop: { desktop: true }
        			            };
    					
    					if (notification.hasOwnProperty("hide")) {
    						opts["hide"] = notification.hide;
    					}
    			        new PNotify(opts);
    				}
    			});
    		}
    	}

    	setInterval(getNotification, 1000);
    });
	</script>
</div>
<div id="header" <% if(hideUI){ %> style="display: none;" <% } %>>
</div>
<div class="social-button icon" <% if(hideUI){ %> style="display: none;" <% } %>>
	<span name="toggle-quick-test-panel" class="text-success glyphicon glyphicon-pushpin"></span>
	<% if ((admGrp.getSystemAdminCURD() & AdminGroup.O1) > 0 || 
			(admGrp.getSystemAdminCURD() & AdminGroup.A4) > 0 || 
			(admGrp.getStatisticsAdminCURD() & AdminGroup.R) > 0) { %>
	<div class="btn-group btnNavBtnGroup" role="group" aria-lable="bg2">
		<a href='<%= ctxPath %>/wiseadm/qaDashboard.jsp' class='btn btn-default'><bean:message key='global.dashboard'/></a>
	    <% if ((admGrp.getSystemAdminCURD() & AdminGroup.O1) > 0 || 
	    		    (admGrp.getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
			<button class="btn btn-primary dropdown-toggle" type="button" id="dropdownMenu4" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
			    <bean:message key='qa.admin'/>
			    <span class="caret"></span>
			</button>
			<ul class="dropdown-menu" aria-labelledby="dropdownMenu4" style="text-align: left;">
			<% if ((admGrp.getSystemAdminCURD() & AdminGroup.O1) > 0) { %>		
				<li><a href='<%= ctxPath %>/wiseadm/qaAdmin.jsp'><bean:message key='top.qa'/></a></li>
			<% } %>
			<% if((admGrp.getSystemAdminCURD() & AdminGroup.E5) > 0) {%>
				<li><a href='exportQA.jsp'><bean:message key='knowledge.export'/></a></li>
			<% } %>
			<% if((admGrp.getSystemAdminCURD() & AdminGroup.O8) > 0 && t.getCasualCoreUrl() != null) { %>
				<li><a href='<%= ctxPath %>/wiseadm/qaAdmin2.jsp'><bean:message key='top.life.langue'/></a></li>
			<% } %>
			
			<%-- 熱門 / 制式文案 / LINE圖文 --%>
			<% if ((admGrp.getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
				<!-- <li><a href='<%= ctxPath %>/wiseadm/qaHotProblems.jsp' ><bean:message key='global.hot.problems'/></a></li> -->
				<li><a href='<%= ctxPath %>/wiseadm/qaFormalAnswer.jsp' ><bean:message key='qa.formal.answer'/></a></li>
				<li><a href='<%= ctxPath %>/wiseadm/qaFormalAnswerSticker.jsp' ><bean:message key='qa.formal.answer.sticker'/></a></li>
				<!-- <li><a href='<%= ctxPath %>/wiseadm/webRM/qaRichMessageList.jsp' ><bean:message key='qa.richmessage.navbar.web'/></a></li> -->
				<li><a href='<%= ctxPath %>/wiseadm/webLine/qaRichMessageList.jsp' ><bean:message key='qa.richmessage.navbar.webline'/></a></li>
				<!-- <li><a href='<%= ctxPath %>/wiseadm/citi/qaRichMessageList.jsp' ><bean:message key='qa.citi.card.right'/></a></li> -->
				<% if (hasLineChannelType) { %>
					<li><a href='<%= ctxPath %>/wiseadm/line/qaRichMessageList.jsp' ><bean:message key='qa.richmessage.navbar.line'/></a></li>
				<% } %>
			<% } %>
			</ul>
	    <% } %>
	</div>
	<% } %>
	<% if ((admGrp.getSystemAdminCURD() & AdminGroup.O2) > 0) { %>
	<div class="btn-group btnNavBtnGroup" role="group">
		<button class="btn btn-primary dropdown-toggle" type="button" id="dropdownMenu0" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
		    <bean:message key='dictionary.management'/>
		    <span class="caret"></span>
		</button>
		<ul class="dropdown-menu" aria-labelledby="dropdownMenu0" style="text-align: left;">
			<li><a href='<%= ctxPath %>/wiseadm/qaSynonym.jsp'><bean:message key='global.synonyms'/></a></li>
			<li><a href='<%= ctxPath %>/wiseadm/qaDictionary.jsp'><bean:message key='global.dictionary'/></a></li>
		   <!-- <li><a href='<%= ctxPath %>/wiseadm/qaIntent.jsp'><bean:message key='intent.management'/></a></li> -->
		    <li><a href='<%= ctxPath %>/wiseadm/qaEntity.jsp'><bean:message key='entity.management'/></a></li>
			<% if ((admGrp.getAdphraseAdminCURD() & AdminGroup.O1) > 0) { %>
		   <!-- <li><a href='<%= ctxPath %>/wiseadm/qaWiVoEntry.jsp'><bean:message key='wivoEntry.management'/></a></li> -->
			<% } %>
		</ul>
	</div>
	<% } %>
	<%if (admGrp.getSystemAdminCURD() > 0) {  %>
	<div class="btn-group btnNavBtnGroup" role="group">
		<button class="btn btn-primary dropdown-toggle" type="button" id="dropdownMenu5" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
			<bean:message key='global.audit.management'/>
		    <span class="caret"></span>
		</button>
		<ul class="dropdown-menu" aria-labelledby="dropdownMenu5" style="text-align: left;">
			<% if(t.getEnableQaAudit()){ %>
			<li><a href='<%= ctxPath %>/wiseadm/qaAdminEdit.jsp'><bean:message key='qa.edit2'/></a></li>
				<% if ((admGrp.getSystemAdminCURD() & AdminGroup.O5) > 0) { %>
					<li><a href='<%= ctxPath %>/wiseadm/qaAdminAudit.jsp'><bean:message key='qa.audit'/></a></li>
					<hr>
				<% } %>
			<% } %>
			<li><a href='<%= ctxPath %>/wiseadm/qaSynonymAudit.jsp'><bean:message key='synonym.audit'/></a></li>
			<li><a href='<%= ctxPath %>/wiseadm/qaDictionaryAudit.jsp'><bean:message key='dictionary.audit'/></a></li>
			<li><a href='<%= ctxPath %>/wiseadm/qaEntityAudit.jsp'><bean:message key='entity.audit'/></a></li>
			<li><a href='<%= ctxPath %>/wiseadm/qaFormalAnswerAudit.jsp'><bean:message key='formal.audit'/></a></li>
		</ul>
	</div>
	<%} %>
	<% if ((admGrp.getStatisticsAdminCURD() & AdminGroup.R) > 0) { %>
	<div class="btn-group btnNavBtnGroup" role="group">
		<a href='<%= ctxPath %>/wiseadm/qa-analytics/s.jsp?v=bar' class='btn btn-default'><bean:message key='global.statistics'/></a>
	</div>
	<% } %>
	<% if (false) {// (admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0 || (admGrp.getSystemAdminCURD() & AdminGroup.A4) > 0) { %>
	<div class="btn-group btnNavBtnGroup" role="group" aria-lable="bg2">
        <% if((admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0) { %>	
		<a href='<%= ctxPath %>/wiseadm/qaElevator.jsp' class='btn btn-default'><bean:message key='qa.elevator'/></a>
		<% } %>
	</div>
	<% } %>
	
	<% if((admGrp.getSystemAdminCURD() & AdminGroup.A4) > 0) { %>
	<div class="btn-group btnNavBtnGroup" role="group" aria-lable="bgServiceLog">
		<button class="btn btn-default dropdown-toggle" type="button" id="dropdownMenu3" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
		    <bean:message key='global.log'/>
		    <span class="caret"></span>
		</button>
		<ul class="dropdown-menu" aria-labelledby="dropdownMenu3" style="text-align: left;">
		    <li><a href='<%= ctxPath %>/wiseadm/qaServiceLog.jsp'><bean:message key='qa.service.log'/></a></li>
		    <li><a href='<%= ctxPath %>/wiseadm/qaEvaluationLogView.jsp'><bean:message key='global.test.record'/></a></li>
		    <li><a href='<%= ctxPath %>/wiseadm/qaEvaluationLogStatistics.jsp'><bean:message key='global.test.record'/><bean:message key='global.statistics'/></a></li>
		</ul>
	</div>
	<% } %>
	
	<% if (t.getEnableScenario()) { %>
		<% if ((admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0) { %>
		<div class="btn-group btnNavBtnGroup" role="group" aria-lable="bgScenario">
			<button class="btn btn-warning dropdown-toggle" type="button" id="dropdownMenu1" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
			    <bean:message key='global.scenario'/>
			    <span class="caret"></span>
			</button>
			<ul class="dropdown-menu" aria-labelledby="dropdownMenu1" style="text-align: left;">
		<% if ((admGrp.getSystemAdminCURD() & AdminGroup.E2) > 0) { %>
			    <li><a href='<%= ctxPath %>/wiseadm/qaDialogList.jsp'><bean:message key='scenario.management'/></a></li>
			    <li role="separator" class="divider easterEgg"></li>
			    <li class='easterEgg'><a href='<%= ctxPath %>/wiseadm/qaScenarioList.jsp'>舊<bean:message key='scenario.management'/></a></li>
		<% } %>
			</ul>
		</div>
		<% } %>
	<% } %>
	<% if ((admGrp.getSystemAdminCURD() & AdminGroup.O4) > 0) { 
		int badge = SegmentBatchTask.count(t.getId(), null, SegmentBatchTask.Status.WAIT).intValue();
		request.setAttribute("sbtBadge", badge);
	%>
		<div class="btn-group btnNavBtnGroup" role="group" aria-lable="bgSystemConfig">
			<a href='<%= ctxPath %>/wiseadm/qaPatternList.jsp' class='btn btn-warning'><bean:message key='global.special.answer'/></a>
			<a href='<%= ctxPath %>/wiseadm/qaAmbiguity.jsp' class='btn btn-warning'><bean:message key='global.compulsory.break.word'/></a>
			<button class="btn btn-danger dropdown-toggle" type="button" id="dropdownMenu3" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
			    <bean:message key='global.batch.management'/> 
			    <c:if test='${ sbtBadge > 0 }'> <span class='badge'>${ sbtBadge }</span> </c:if>
			    <span class="caret"></span>
			</button>
			<ul class="dropdown-menu" aria-labelledby="dropdownMenu3" style="text-align: left;">
			<li><a href='<%= ctxPath %>/wiseadm/fileUpload.jsp'><span class='glyphicon glyphicon-import'></span> <bean:message key='global.batch.import'/></a></li>
			<li><a href='<%= ctxPath %>/wiseadm/qaSegBatchList.jsp'><span class='glyphicon glyphicon-edit'></span> <bean:message key='global.batch.segment.change'/> <span class='badge'>${ sbtBadge }</span></a></li>
			<li><a HREF="<%= ctxPath %>/wiseadm/qaAltTemplateList.jsp"><span class='glyphicon glyphicon-book'></span> <bean:message key="alt.template.management"/></a></li>
			<li><a HREF="<%= ctxPath %>/wiseadm/qaEvaImportList.jsp"><span class='glyphicon glyphicon glyphicon-import'></span> <bean:message key="top.eva.import.qa"/></a></li>
			</ul>
		</div>
	    <% if((admGrp.getSystemAdminCURD() & AdminGroup.E3) > 0) { %>
		<div class="btn-group btnNavBtnGroup" role="group" aria-lable="bgSystemConfig">
			<button class="btn btn-danger dropdown-toggle" type="button" id="dropdownMenu2" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true">
			    <bean:message key='top.system.management'/>
			    <span class="caret"></span>
			</button>
			<ul class="dropdown-menu" aria-labelledby="dropdownMenu2" style="text-align: left;">
				<li><a href='<%= ctxPath %>/wiseadm/qaOpLogList.jsp'><bean:message key='top.operation.log'/></a></li>
				<%
				for (QAChannel channel : channels) { 
					if (channel.getType().equals(QAChannelType.LINE)) {
				%>
						<li><a href='<%= ctxPath %>/wiseadm/line/qaRichMenuList.jsp?channel=<%= channel.getCode() %>'>LINE RichMenu管理</a></li>
						<li><a href='<%= ctxPath %>/wiseadm/line/qaLiffAppList.jsp'>LINE LiffApp管理</a></li>
				<% 
						break;
					}
				}
				%>
			    <!-- <li><a href='<%= ctxPath %>/wiseadm/qaEventType.jsp'><bean:message key='eventType.management'/></a></li> -->
			    <li><a href='<%= ctxPath %>/wiseadm/qaSchedule.jsp'><bean:message key='global.scheduling'/></a></li>
			    <li><a href='<%= ctxPath %>/wiseadm/qaTenantAdmin.jsp'><bean:message key='wisesystemconfig.set'/></a></li>
			    <% if (admGrp.getAdminAdminCURD() > 0) {%>
					<li><a href="qaEmbeddedDictionary.jsp"><bean:message key='robot.embedded.dictionary'/></a></li>
				<% } %>
			</ul>
		</div>
		<% } %>
	<% } %>
	<!-- <a href='<%= ctxPath %>/wiseadm/qaDatasetAdmin.jsp' class='btn btn-default'>DataSets</a>
	<a href='<%= ctxPath %>/wiseadm/qaDebug.jsp' class='btn btn-default'>Debug</a>
	-->
	<div class="btn-group btnNavBtnGroup" role="group" aria-lable="bg2">
		<% if ((admGrp.getSystemAdminCURD() & AdminGroup.R) > 0) { %>
		<a href='<%= ctxPath %>/wiseadm/test.jsp' target='_blank' class='btn btn-default'><bean:message key='global.test.interface'/></a>
		<% } %>
		<button class='btn btn-default btnDoLogout'><bean:message key='top.logout'/></button>
	</div>
</div>
<div id="form_login" style="display: none;">
	<form class="pf-form pform_custom" action="<%= ctxPath %>/wiseadm/login.jsp" method="post">
		<div class="pf-element pf-heading">
			<h3><bean:message key='top.logout.ex'/>
			<p class="pf-msg"></p>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label"><bean:message key='admin.login.submit'/><bean:message key='global.account'/></span>
				<input class="pf-field" type="text" name="loginName" />
				<input type="hidden" name="responseType" value="json" />
			</label>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label"><bean:message key='global.password'/></span>
				<input class="pf-field" type="password" name="password" />
			</label>
		</div>
		<div class="pf-element pf-buttons pf-centered">
			<input class="pf-button btn btn-primary" type="submit" name="submit" value="Submit" />
			<input class="pf-button btn btn-default" type="button" name="cancel" value="Cancel" />
		</div>
	</form>
</div>
<div id="quick_test_panel" style="display: none;">
	<form class="pf-form pform_custom" name="quick_test_form" action="#" method="post">
		<div class="pf-element pf-heading">
			<h4><bean:message key='fast.test.area'/> <small><%= uuid %></small></h4>
			<p class="pf-msg"></p>
		</div>
		<div class="pf-element">
			<div class="row">
				<div class="col-md-6">
				<select name="qaCategorySelect" class=" form-control">
					<option value=""><bean:message key="qacategory.not.select"/></option>
			<%
				String[] qaCategorys = null;
				if(t.getQaCategory() != null){
					qaCategorys = t.getQaCategory().split(",");
				}
				for (int i = 0; qaCategorys != null && i < qaCategorys.length; i++) {
				%>
				    <option value="<%=qaCategorys[i]%>"><%=qaCategorys[i]%></option>
				<%
				}
			%>
			</select>
				</div>
				<div class="col-md-6">
			<bean:message key="forceToggleContext.enable"/><input type="radio" name="forceToggleContext" value="true" >&nbsp;
			<bean:message key="forceToggleContext.disable"/><input type="radio" name="forceToggleContext" value="false">
				</div>
			</div>
			<div class="row">
				<div class="col-md-1">
			Channel Type
				</div>
				<div class="col-md-4">
				<select name="qaChannelType" class="form-control">
			<% 
				for(QAChannel channel : channels) { 
			%> 
					<option value="<%=channel.getCode()%>"><%=channel.getName()%></option>
			<%
				}
			%>
			</select>
				</div>
				<div class="col-md-1">
			User Type
				</div>
				<div class="col-md-4">
				<select name="qaUserType" class="form-control">
			<% 
				List<QAUserType> userTypes = QAUserType.list(t.getId());
				for(QAUserType userType : userTypes) { 
			%> 
					<option value="<%=userType.getCode()%>"><%= MessageUtil.returnKeyIfNotExist(locale, userType.getName()) %></option>
			<%
				}
			%>
			</select>
        </div>
			</div>
        </div>
		<div class="pf-element">
			<div class="row">
			<div class=" col-md-12">
			  <div class="input-group">
				<input class="pf-field form-control" type="text" name="question" value="<%= StringEscapeUtils.escapeHtml(new String(Base64.getDecoder().decode(StringUtils.trimToEmpty(WiSeUtils.getFromCookie("lastQuestion", request))), "UTF-8")) %>" placeholder="<bean:message key='input.problem'/>" />
				<div class="input-group-addon btn btn-xs btn-primary" data-loading-text="<bean:message key='in.query'/>" id="btnQuickTestSubmit">
				<span style="color: white;"><bean:message key='global.ask'/></span>
				</div>
				<div class="input-group-addon btn btn-success" id="btnNlp" >
				<span style="color: white;"><bean:message key='global.broken'/></span>
				</div>
				<div class="input-group-addon btn btn-success" id="btnNlpReplaceSyn" >
                <span style="color: white;"><bean:message key='global.broken'/>2</span>
                </div>
				<input type="submit" style="display: none;"><%-- 隱藏版的 submit 可以讓按 enter 直接送出，否則會送出「WHY」 --%>
		</div>
			</div>
			</div>
		</div>
		<div class="pf-element">
			<h5>
				<span class="pf-label text-primary"><bean:message key='global.answer'/></span>
			</h5>
			<div name="message-body" class="answer-text-qtp"></div>
		</div>
	</form>
</div>
<nav class="cbp-spmenu cbp-spmenu-vertical-big cbp-spmenu-right-big dlg-debug-panel-bg" id="side-test-panel">
	<h3 class='side-test-panel-header toggle-side-test-panel' style='background: #1e85e6;'><bean:message key='fast.test.area'/> <span class='glyphicon glyphicon-chevron-right'></span></h3>
	<div class='col-md-12' style='margin-top: 20px;'>
	<div class='col-md-12'>
		<select id="qaCategorySelect">
		<%
			for (int i = 0; qaCategorys != null && i < qaCategorys.length; i++) {
			%>
			    <option value="<%=qaCategorys[i]%>"><%=qaCategorys[i]%></option>
			<%
			}
		%>
		</select>
		<bean:message key="qacategory.select"/><input id="qaCategoryEnableA" type="radio" name="qaCategoryEnable" value="true" >&nbsp;&nbsp;<bean:message key="qacategory.not.select"/><input id="qaCategoryEnableB" type="radio" name="qaCategoryEnable" value="false" checked >
		<br>
		<bean:message key="forceToggleContext.enable"/><input type="radio" name="forceToggleContext" value="true" >&nbsp;
		<bean:message key="forceToggleContext.disable"/><input type="radio" name="forceToggleContext" value="false">
		<br>
		Channel Type
		<select id="qaChannelType">
		<% 
			for(QAChannel channel : channels) { 
		%> 
				<option value="<%=channel.getCode()%>"><%=channel.getName()%></option>
		<%
			}
		%>
		</select>
		User Type
		<select id="qaUserType">
		<% 
			for(QAUserType userType : userTypes) { 
		%> 
				<option value="<%=userType.getCode()%>"><%= MessageUtil.returnKeyIfNotExist(locale, userType.getName()) %></option>
		<%
			}
		%>
		</select>
	</div>
	<div class="col-md-12">
      <button class='btn btn-xs pull-right btnClearSidePanelQuestion' style='margin-bottom: -30px;'>清除重來</button>
      <textarea class="form-control" id='side-panel-question' rows=5 placeholder="SHIFT+ENTER to Ask..."></textarea>
    </div>
    <div id='side-panel-content'>
    </div>
	</div>
</nav>
<div id="batch_test_panel" style="display: none;">
	<form class="pf-form pform_custom" name="batch_test_form" action="qaBatchTest.jsp" method="post">
		<div class="pf-element pf-heading">
			<h4><bean:message key='batch.test.area'/> <small><%= uuid %></small></h4>
			<p class="pf-msg"></p>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label text-primary"><bean:message key='input.problem'/></span>
				<textarea class="pf-field" rows="10" cols="40" name="questions"></textarea>
				<input class="pf-button btn btn-primary"  data-loading-text="<bean:message key='in.query'/>" type="submit" name="submit" value="<bean:message key='global.ask'/>" />
				<% if((admGrp.getSystemAdminCURD() & AdminGroup.A3) > 0) { %>
				<button class="pf-button btn btn-success" id="btnBatchNlp" data-loading-text="<bean:message key='broken.words'/>"><bean:message key='global.broken'/></button>
				<button class="pf-button btn btn-success" id="btnBatchNlp2" data-loading-text="<bean:message key='broken.words'/>2"><bean:message key='global.broken'/>2</button>
				<% } %>			
			</label>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label text-primary"><bean:message key='global.answer'/></span>
			</label>
			<div name="message-body" class="answer-text-qtp"></div>
		</div>
	</form>
</div>
<script>
var qtpShown = false;
var qtp;
var storage = $.sessionStorage;
var uuid = '<%= uuid %>';

$('span[name=toggle-quick-test-panel]').click(toggleQuickTestPNotify);
$('.toggle-test-panel').click(function(e) {
	if (e.shiftKey) {
		// 按著 shift 出現覆蓋的 side panel，如果同時按著 shift+alt 則會變成 push 形式（原內容會被往左移動）
		toggleSideTestPanel(e.altKey);
	}
	else {
		toggleQuickTestPNotify();
	}
});

$('.toggle-side-test-panel').click(function(e) {
	toggleSideTestPanel($('body').hasClass('cbp-spmenu-push-toleft-big'));
});

var btpShown = false;
var btp;
$('#toggle-batch-test-panel').click(toggleBatchTestPNotify);

function toggleQuickTestPNotify() {
	if (qtpShown) {
		qtpShown = false;
		qtp.remove();
	}
	else {
		
		qtpShown = true;
		qtp = new PNotify({
			    text: $('#quick_test_panel').html(),
			    icon: false,
			    width: '600px',
			    addclass: 'quick-test-panel',
			    hide: false,
			    confirm: {
			        confirm: true,
			        buttons: [{
			            text: '<bean:message key="global.close"/>',
			            addClass: 'btn-primary',
			            click: function(notice) {
			            	qtpShown = false;
			                notice.remove();
			            }
			        },{
			            text: '<bean:message key="not.need.exist.close"/>',
			            addClass: 'hide btn-primary',
			            click: function(notice) {
			            	qtpShown = false;
			                notice.remove();
			            }
			        }
			        ]
			    },
			    buttons: {
			        closer: false,
			        sticker: false
			    },
			    insert_brs: false
		});
		
		if (storage.isSet('qtopts')) {
			var lastOptions = storage.get('qtopts');
			$("form[name='quick_test_form']").find("input[name='question']").val(lastOptions.q);
		}
		
		$("form[name='quick_test_form']").find("input[name='question']").focus().select();
	}
};


function toggleSideTestPanel(doPush) {
	if (doPush) {
		classie.toggle( document.body, 'cbp-spmenu-push' );
		classie.toggle( document.body, 'cbp-spmenu-push-toleft-big' );
	}
	classie.toggle( $('#side-test-panel')[0], 'cbp-spmenu-open' );
	$('#side-panel-question').focus();

	if (storage.isSet('stopts')) {
		var lastOptions = storage.get('stopts');
		$('#side-panel-question').val(lastOptions.q);
	}
 	return false;
}

function toggleBatchTestPNotify() {
	if (btpShown) {
		btpShown = false;
		btp.remove();
	}
	else {
		btpShown = true;
		btp = new PNotify({
			    text: $('#batch_test_panel').html(),
			    icon: false,
			    width: '600px',
			    hide: false,
			    confirm: {
			        confirm: true,
			        buttons: [{
			            text: '<bean:message key="global.close"/>',
			            addClass: 'btn-primary',
			            click: function(notice) {
			            	btpShown = false;
			                notice.remove();
			            }
			        },{
			            text: '<bean:message key="not.need.exist.close"/>',
			            addClass: 'hide btn-primary',
			            click: function(notice) {
			            	btpShown = false;
			                notice.remove();
			            }
			        }
			        ]
			    },
			    buttons: {
			        closer: false,
			        sticker: false
			    },
			    insert_brs: false
			});
		
		if (storage.isSet('btopts')) {
			var lastOptions = storage.get('btopts');
			$("form[name='batch_test_form']").find("textarea[name='questions']").text(lastOptions.questions);
		}
	}
}

$(document).on('submit', "form[name='batch_test_form']", function(e) {
	$this = $(this);
	lastOptions = {};
	lastOptions.questions = $this.find("textarea[name='questions']").val();
	storage.set('btopts', lastOptions);
	
	return true;
});

function submitQuickTestQuestion(theForm) {
	var message = $(theForm).find("div[name='message-body']");
	var question = $(theForm).find("input[name='question']").val();
	var $btn = $(theForm).find('#btnQuickTestSubmit');
	var $input = $(theForm).find('input[type="text"]');
    var ftc = $(theForm).find('input[name=forceToggleContext]:checked').val();
    var qaCategorySelect = $(theForm).find('select[name=qaCategorySelect] option:selected').val();
    var selectQaChannelType = $(theForm).find('select[name=qaChannelType] option:selected').val();
    var selectQaUserType = $(theForm).find('select[name=qaUserType] option:selected').val();
    
    if(!selectQaChannelType) {
		selectQaChannelType = 'web'; 
    }
    
	if(!selectQaUserType) {
		selectQaUserType = 'unknown';
	}
    
    options = {
  		  q: question,
		  id: '<%= uuid %>',
		  category: qaCategorySelect,
		  ch: selectQaChannelType,
		  userType: selectQaUserType,
		  testMode: true,
		  tid: <%= t.getId() %>,
  		  html: true
		};
    
    if (typeof(ftc) != 'undefined') {
    	options["ftc"] = ftc;
    }

    beginTS = Date.now();

    $.ajax({
			url: '<%= ctxPath %>/qa-ajax.jsp',
			dataType: 'json',
			data: options,
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				storage.set('qtopts', options);
				
    			resp["ts"] = (new Date()).getTime();
    			message.html("<bean:message key='sentence.be.changed'/>：" + resp.originalQuestion + "<br>");

		        if (typeof(resp.webRM) != 'undefined') {
		        	for (var i=0; i < resp.webRM.messages.length; i++) {
			        	newWebRM = $('<div class="message-row"><div class="msg-content"></div></div>');
			        	newWebRM.find('.body').html("");
		                newWebRM.appendRichMessage(resp.webRM.messages[i]);
		                newWebRM.appendTo(message);
					}
		        }
		        else if (resp.hasOwnProperty('output')) {
					message.html(message.html() + resp.output);
				}
				else {
					message.html("<bean:message key='this.topic.has.not.yet.been.edited'/>");
				}

                if (resp.hasOwnProperty('qaCategory')) {
					$("<br><small class='text-info' style='margin-right: 10px;'>當前<bean:message key='category'/>：" + resp.qaCategory + "</small>").appendTo(message);
      	  		}

				if (resp.hasOwnProperty('confidence')) {
					$("<br><small class='text-primary' style='margin-right: 10px;'><bean:message key='global.confidence.level'/>：" + resp.confidence + " (" + resp.confidenceFactor + ")</small>").appendTo(message);
				}

				if (resp.hasOwnProperty('kid')) {
	          		var anchor = $('<a/>');
	          		anchor.attr('href', "<%= ctxPath %>/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-" + resp.kid);
	          		anchor.attr('target', "_blank");
	          		anchor.html("<small class='text-success'><bean:message key='num'/>" + resp.kid + "</small>");
	          		anchor.appendTo(message);
	          		
	          		$('<span>　</span>').appendTo(message);
	          		
	          		var btnWhy = $('<button/>');
	          		btnWhy.attr('data-kid', resp.kid);
	          		btnWhy.attr('data-question', options.q);
	          		btnWhy.attr('data-loading-text', "<bean:message key='in.query'/>");
	          		btnWhy.attr('class', 'btn btn-xs btn-danger btnWhy');
	          		btnWhy.html("WHY?");
	          		btnWhy.appendTo(message);

	          		function why2() {
	          		    $.ajax({
	          		        url: 'qa-nlp-test-ajax2.jsp',
	          		        data: {
	          		            kid: resp.kid,
	          		            q: options.q
	          		        }
	          		    }).then(function(result) {
	          		        var msg = '最接近的例句：';
	          		        for (var [index, alt] of result.alts.slice(0, 5).entries()) {
	          		            msg += '\n' + (index+1) + ". " + alt.text;
	          		        }
	          		        if (result.alts.length > 5) {
	          		        	msg += '\n（結果超過五筆）'   
	          		        }
	          		        alert(msg);
	          		    });
	          		}
	          		
	          		var btnWhy2 = $('<button class="btn btn-xs btn-danger easterEgg" style="margin-left: 4px;">WHY? 2</button>');
	          		btnWhy2.click(why2);
	          		btnWhy2.appendTo(message);
				}

				if (resp.hasOwnProperty('otherPossibleQuestions')) {
					var oqSpan = $("<span><br><br><bean:message key='oq.span'/></span>");
					var otherQuestions = resp.otherPossibleQuestions;

					for (var i=0; i < otherQuestions.length; i++) {
						var thisLi = $("<span style='border-bottom: 0px; padding: 0; padding-left: 10px;'><br></span>");

						var clickToAsk = $('<span/>');
						clickToAsk.addClass("question-clickable");
						clickToAsk.text("" + (i+1) + ". " + otherQuestions[i].question);
						clickToAsk.appendTo(thisLi);

						var anchor = $('<a/>');
						anchor.attr('href', "<%= ctxPath %>/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-" + otherQuestions[i].kid);
						anchor.attr('target', "_blank");
						anchor.text("(<bean:message key='num'/>" + otherQuestions[i].kid + ")");
						anchor.appendTo(thisLi);

						thisLi.appendTo(oqSpan);
					}
					oqSpan.appendTo(message);
				}

			TC = Date.now() - beginTS;
    		    $("<br><small class='badge' style='margin-right: 10px;'>TS: " + beginTS + "+" + TC + "</small>").appendTo(message);
    	  },
    	  complete: function() {
    		  $btn.button('reset');
    		  $input.removeAttr("disabled");
    		  $input.focus().select();
    	  }
	});
}

$(document).on('click', "#btnQuickTestSubmit", function(e) {
	$this = $(this).closest('form');
	var $input = $this.find('input[type="text"]').attr('disabled', 'disabled');
	setTimeout(function() {submitQuickTestQuestion($this);}, 500);
	return false;
});

$(document).on('submit', "form[name='quick_test_form']", function(e) {
	$this = $(this);
	var $input = $this.find('input[type="text"]').attr('disabled', 'disabled');
	setTimeout(function() {submitQuickTestQuestion($this);}, 500);
	return false;
});

$(document).on('click', ".btnWhy", function(e) {
	$thisBtn = $(this);
	
	(function ($btn) {
		$btn.button('loading');
		var $theForm = $btn.parents("form[name='quick_test_form']");
		var kid = $btn.attr('data-kid');
		var question = $btn.attr('data-question');
		var message = $theForm.find("div[name='message-body']");
		var $input = $theForm.find('input[type="text"]');
	
		return $.ajax({
			url: 'qa-alt-test-ajax.jsp',
			dataType: 'json',
			data: {
			  q: question,
			  kid: kid,
			  ts: Math.random()
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				if (resp.matchedCases.length > 0) {
					alert("<%=MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "according.to.this.gramma.to.compare")%>「" + resp.matchedCases[0].altTpl + "」<%=MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "to.get.the.result")%>，<%=MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "refactor.sentence.be.compared.is")%>「" + resp.matchedCases[0].matchedSentence + "」");
				}
				else {
					alert('<%=MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "no.direct.answer.should.from.fuzzy")%>');
				}
			},
	    		complete: function() {
	    			$btn.button('reset');
	    			$input.removeAttr("disabled");
		    	}
		});
	}) ($thisBtn);
	return false;
});

$(document).on('click', "#btnNlp", function(e) {
    var showColor = false;
    if (e.shiftKey) {
        showColor = true;
    }
    nlpAjax($(this),showColor,false)
    return false;
});


$(document).on('click', "#btnNlpReplaceSyn", function(e) {
	var showColor = false;
	if (e.shiftKey) {
		showColor = true;
	}
    nlpAjax($(this),showColor,true)
    return false;
});

function nlpAjax($btn,showColor,replaceSyn){
    var $theForm = $btn.parents("form[name='quick_test_form']");
    var $btn = $theForm.find('#btnNlp').button('toggle');
    var question = $theForm.find("input[name='question']").val();
    var selectQaChannelType = $theForm.find('select[name=qaChannelType] option:selected').val();
    var $input = $theForm.find('input[type="text"]');
    var message = $theForm.find("div[name='message-body']");
    $.ajax({
			url: 'qa-nlp-test-ajax.jsp',
			dataType: 'json',
			data: {
    		  q: question,
    		  ch: selectQaChannelType,
              replaceSyn: replaceSyn,
    		  ts: Math.random(),
    		  showColor: showColor
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				if (resp.hasOwnProperty('nlp')) {
					var result = resp.nlp;
					if (resp.nlpColor)
						result += "<br>" + resp.nlpColor.replace(/\n/g, "<br>");

					message.html("<bean:message key='broken.words.result'/>：" + result);
				}
				else {
					message.html("<bean:message key='broken.words.result'/>：" + "????");
				}
				if (resp.hasOwnProperty('wivo')) {
					var li = $('<li/>').html("<span class='text-danger'>&#9733;WiVO：" + resp.wivo + "</span>") ;
					li.appendTo(message);
				}
				if (resp.hasOwnProperty('kps')) {
					var kps = resp.kps;
					
					for (var i=0; i < kps.length; i++) {
						var li = $('<li/>').html("<span class='text-primary'>&#9733;<bean:message key='knowledge.compare'/>：" + kps[i].keyword + " (" + kps[i].category + ")</span>") ;
						li.appendTo(message);
					}
				}
				if (resp.hasOwnProperty('intents')) {
					var kps = resp.intents;
					
					for (var i=0; i < kps.length; i++) {
						var li = $('<li/>').html("<span class='text-success'>&#9733;意圖比對：" + kps[i].keywords + " (" + kps[i].tag + ")</span>") ;
						li.appendTo(message);
					}
				}
				if (resp.hasOwnProperty('entities')) {
					var kps = resp.entities;
					
					for (var i=0; i < kps.length; i++) {
						var li = $('<li/>').html("<span class='text-success'>&#9733;實體比對：" + kps[i].name + " (" + kps[i].code + ")</span>") ;
						li.appendTo(message);
					}
				}
				if (resp.hasOwnProperty('reconstructedQuestion')) {
					var reconstructedQuestion = resp.reconstructedQuestion;
					for (var i=0; i < reconstructedQuestion.length; i++) {
						var li = $('<li/>').text(reconstructedQuestion[i]);
						li.appendTo(message);
					}
				}

			},
	    	complete: function() {
	    		  $btn.button('reset');
	    		  $input.removeAttr("disabled");
	        }});
}

$(document).on('click', "#btnBatchNlp", function(e) {
	var $theForm = $(this).parents("form[name='batch_test_form']");
	var $btn = $theForm.find('#btnBatchNlp').button('toggle');
	var message = $theForm.find("div[name='message-body']");
	var question = $theForm.find("textarea[name='questions']").val();
    var selectQaChannelType = $theForm.find('select[name=qaChannelType] option:selected').val();
	var $input = $theForm.find('input[type="text"]');
	console.log(message)
    $.ajax({
			url: 'qa-nlp-test-ajax.jsp',
			type: 'POST',
			dataType: 'json',
			data: {
    		  q: question,
    		  ch: selectQaChannelType,
    		  batch: true,
    		  ts: Math.random()
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				console.log("12345")
				console.log(resp.nlp[0])
				if (resp.hasOwnProperty('nlp')) {
					message.html("<bean:message key='broken.words.result'/>：");
					for (var i=0; i < resp.nlp.length; i++) {
						var li = $('<li/>').html(resp.nlp[i]);
						li.appendTo(message);
					}
				}
				else {
					message.html("<bean:message key='broken.words.result'/>：" + "????");
				}
				if (resp.hasOwnProperty('reconstructedQuestion')) {
					var reconstructedQuestion = resp.reconstructedQuestion;
					for (var i=0; i < reconstructedQuestion.length; i++) {
						var li = $('<li/>').text(reconstructedQuestion[i]);
						li.appendTo(message);
					}
				}
			},
	    	complete: function() {
	    		  $btn.button('reset');
	    		  $input.removeAttr("disabled");
	    	}});
	return false;
});

$(document).on('click', "#btnBatchNlp2", function(e) {
	var $theForm = $(this).parents("form[name='batch_test_form']");
	var $btn = $theForm.find('#btnBatchNlp2').button('toggle');
	var message = $theForm.find("div[name='message-body']");
	var question = $theForm.find("textarea[name='questions']").val();
    var selectQaChannelType = $theForm.find('select[name=qaChannelType] option:selected').val();
	var $input = $theForm.find('input[type="text"]');

    $.ajax({
			url: 'qa-nlp-test-ajax.jsp',
			type: 'POST',
			dataType: 'json',
			data: {
    		  q: question,
    		  ch: selectQaChannelType,
    		  batch: true,
    		  replaceSyn: true,
    		  ts: Math.random()
			},
			error: function() {
				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
			},
			success: function(resp) {
				console.log("54321")
				console.log(resp)
				if (resp.hasOwnProperty('nlp')) {
					message.html("<bean:message key='broken.words.result'/>：");
					for (var i=0; i < resp.nlp.length; i++) {
						var li = $('<li/>').html(resp.nlp[i]);
						li.appendTo(message);
					}
				}
				else {
					message.html("<bean:message key='broken.words.result'/>：" + "????");
				}
				if (resp.hasOwnProperty('reconstructedQuestion')) {
					var reconstructedQuestion = resp.reconstructedQuestion;
					for (var i=0; i < reconstructedQuestion.length; i++) {
						var li = $('<li/>').text(reconstructedQuestion[i]);
						li.appendTo(message);
					}
				}
			},
	    	complete: function() {
	    		  $btn.button('reset');
	    		  $input.removeAttr("disabled");
	    	}});
	return false;
});

function showLoginPNotify(callback) {
	var notice = new PNotify({
  			    text: $('#form_login').html(),
  			    icon: false,
  			    width: 'auto',
  			    hide: false,
  			    buttons: {
  			        closer: false,
  			        sticker: false
  			    },
  			    insert_brs: false
  			});
  			notice.get().find('form.pf-form').on('click', '[name=cancel]', function() {
  			    notice.remove();
  				callback(false);
  			}).submit(function() {
  				var thisForm = $(this);
  			    var username = thisForm.find('input[name=loginName]').val();
  			    if (!username) {
  			        alert('<bean:message key="input.login.user"/>');
  			        return false;
  			    }

  			    $.post(
  			    		thisForm.attr("action"),
  			    		thisForm.serialize(),
  			    		function(result) {
  			    			if ("ok" == result.status) {
  			    				setTimeout(function() {callback(true)}, 1000);
  			    				notice.remove();
  			    			}
  			    			else {
  			    				if (result.hasOwnProperty("msg")) {
	  			    				thisForm.find('.pf-msg').text(result.msg);
	  			    			}
  			    				else {
	  			    				thisForm.find('.pf-msg').text("<bean:message key='user.login.failed'/>");
  			    				}
  			    				return false;
  			    			}
  			    		});
  			    return false;
  			});
}

// callback(loginedOrNot)
function checkIfStillLogin(callback, showLoginForm) {
    $.ajax({
  	  type: 'POST',
  	  url: 'login-check-ajax.jsp',
  	  dataType: 'json',
  	  success: function(resp) {
  		  if (resp.needLogin) {
  			  if (showLoginForm) {
				  showLoginPNotify(callback);
  		  	  }
  			  else {
  				  callback(false);
  			  }
  		  }
  		  else {
  			callback(true);
  		  }
  	  }
    });
}

$(document).ready(function() {
	$( window ).konami();

	$( window ).on('konami', function() {
	    // bj4
		$('.easterEgg').show();
	});
});

<%-- Side test panel 區 --%>
$("#side-panel-question").on("keypress", function(e){
	$this = $(this);
	shifted = e.shiftKey;
	
    if (shifted && e.which == 13) {
        var textareaVal = $this.val();
        if (textareaVal !== ""){
	        	lastOptions = {};
	        	lastOptions.q = textareaVal;
	        	storage.set('stopts', lastOptions);
        	
        		texts = textareaVal.split("\n");
        		
        		function loopDoTest(texts, offset, question) {
        			return function() {
	        			if (offset < texts.length-1) {
	        				doSidePanelTest(texts[offset+1], loopDoTest(texts, offset+1, texts[offset+1]));
	        			}
        			};
        		};
        		
	        	doSidePanelTest(texts[0], loopDoTest(texts, 0, texts[0]));
        		
        		if (texts.length <= 1) {
        			$(this).val("");
        		}
        		
        		$('.timeago').timeago();
        }
        
        return false;
    }
    
    return true;
});

$(document).on('click', '.btnClearSidePanelQuestion', function() {
	function guid() {
	  function s4() {
	    return Math.floor((1 + Math.random()) * 0x10000)
	      .toString(16)
	      .substring(1);
	  }
	  return s4() + s4() + '-' + s4() + '-' + s4() + '-' + s4() + '-' + s4() + s4() + s4();
	}	
	
	$('#side-panel-content').html("");
	uuid = guid();
	$('#side-panel-question').val("");
	$('#side-panel-question').focus();
	$('#dialog-log-panel').html(""); <%-- 這個只有在情境管理介面才會有用 --%>
	
	return false;
});

<%-- Side Panel 上送出問題並顯示答案 --%>
doSidePanelTest = function(question, callback) {
	$panel = $('#side-panel-')
	if (question == "") {
	  	if (typeof(callback) != 'undefined') {
	  		callback();
	  	}
	  	
	  	return;
	}
	
	var qaCategoryEnable = $panel.find('input[name=qaCategoryEnable]:checked').val();
	var ftc = $panel.find('input[name=forceToggleContext]:checked').val();
	var qaCategorySelect;
	if(qaCategoryEnable == 'true'){
		$panel.find('input[type="radio"]').prop("disabled", true);
		$panel.find('option').prop("disabled", true);
		if($panel.find('option:selected').val() != ''){
			qaCategorySelect = $panel.find('#qaCategorySelect option:selected').val();
		}
	}
	var selectQaChannelType = $panel.find('#qaChannelType option:selected').val();
	var selectQaUserType = $panel.find('#qaUserType option:selected').val();

	if(!selectQaChannelType) {
		selectQaChannelType = 'web'; 
	}

	if(!selectQaUserType) {
		selectQaUserType = 'unknown';
	}
	
	var t = moment((new Date()).getTime());
	var formatted = t.format("YYYY/MM/DD HH:mm:ss");
	dataForRender = {text: question, ts:formatted};
  	var template = $('#chat-text-left').html();
  	Mustache.parse(template);
  	var rendered = Mustache.render(template, dataForRender);
  	$(rendered).appendTo($('#side-panel-content')); // 提問先 append 到畫面上
  	
  	var template2 = $('#chat-text-right').html();
  	Mustache.parse(template2);
	
    options = {
  		  q: question,
		  id: uuid,
		  category: qaCategorySelect,
		  ch: selectQaChannelType,
		  userType: selectQaUserType,
		  testMode: true,
		  tid: <%= t.getId() %>,
  		  html: true
	};

	if (typeof(ftc) != 'undefined') {
		options["ftc"] = ftc;
	}
	
	return $.ajax({
		url: '<%= ctxPath %>/qa-ajax.jsp',
		dataType: 'json',
		data: options,
		error: function() {
			//setTimeout(function() {$('.loading-panel').hide();}, 1000);
		},
		success: function(resp) {
			if (resp.hasOwnProperty('output')) {
       			var t = moment((new Date()).getTime());
   				var formatted = t.format("YYYY/MM/DD HH:mm:ss");
				dataForRender2 = {text: resp.output, ts:formatted};
			  	var rendered2 = Mustache.render(template2, dataForRender2);
			  	
   				<%-- 這個 showDialogLog 變數只有在情境管理介面才會有 --%>
   				if (typeof(showDialogLog) != 'undefined' && showDialogLog) { 
	   				$.ajax({
	   					url: '<%= ctxPath %>/wiseadm/qaDialog-ajax.jsp',
	   					dataType: 'json',
	   					data: {
	   						action: 'peekLog',
	   						qaId: uuid
	   					},
	   					error: function() {
	   						//setTimeout(function() {$('.loading-panel').hide();}, 1000);
	   					},
	   					success: function(resp2) {
	   						h4 = $('<h4/>').text(question);
	   						log = resp2.dialogLog.trim();
	   						log = log.replace(/^Dialog/mig, '');
	   						log = log.replace(/^(\[.*?\])/mig, '🔵&nbsp;<small>$1</small>');
	   						log = log.replace(/.(\[.*?\])/mig, '<strong>$1</strong>&nbsp;');
	   						log = log.replace(/->/mig, '&nbsp;➪&nbsp;&nbsp;');
	   						log = log.replace(/(WAIT_INPUT|GOT_INPUT)/mig, '<span class="text text-danger">$1</span>');
	   						log = log.replace(/(null|JUST_ACTIVATED|LEAVING_NODE|DEACTIVE)/mig, '<span class="text text-warning">$1</span>');
	   						log = log.replace(/(initalized|triggered GPN|triggered|changeNodeState|switching|do Reaction|switched)/mig, '<span class="text text-primary">$1</span>&nbsp;');
	   						log = log.replace(/\n/g, '<br>');
	   						pre = $('<div class="alert alert-success"/>').html(log);
	   						$('#dialog-log-panel').append(h4);
	   						$('#dialog-log-panel').append(pre);
	
	   						$(rendered2).appendTo($('#side-panel-content'));		// 答案 append 上去
	   					  	
	   					  	if (typeof(callback) != 'undefined') {
	   					  		callback();
	   					  	}
	   					}
	   				});
   				}
   				else {
				  	$(rendered2).appendTo($('#side-panel-content'));		// 答案 append 上去
				  	
				  	if (typeof(callback) != 'undefined') {
				  		callback();
				  	}
   				}
			}
		},
		complete: function() {
		}
	});
};

$(document).on('click', '.btnDoLogout', function(e) {
	if (e.shiftKey) {
		document.location = "<%= ctxPath %>/wiseadm/chooseTenant.jsp?r=<%= URLEncoder.encode(request.getRequestURI(), "UTF-8") %>";
	}
	else {
		document.location = "<%= ctxPath %>/wiseadm/logout.jsp";
	}
});

$(document).on('click', '.btnSidePanelTest', doSidePanelTest);
</script>
<script id="chat-text-left" type="x-tmpl-mustache">
	<div>
    	<div class="msj macro">
    <div class="chat-text chat-text-l"><p>{{text}}</p><p><small title='{{ts}}' class='timeago'>{{ts}}</small></p></div>
    	</div>
	</div>
</script>
<script id="chat-text-right" type="x-tmpl-mustache">
	<div>
    	<div class="msj-rta macro">
    <div class="chat-text chat-text-r"><p>{{{text}}}</p><p><small title='{{ts}}' class='timeago'>{{ts}}</small></p></div>
    	</div>
	</div>
</script>