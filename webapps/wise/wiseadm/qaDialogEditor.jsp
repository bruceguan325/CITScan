<%@ include file="/commons/taglib.jsp"%>
<%@page import="java.io.Writer"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.collections.*"
	import="org.apache.solr.common.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.client.solrj.embedded.*"
	import="org.apache.solr.client.solrj.response.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.QAContext.*"
	import="com.intumit.solr.robot.qaplugin.*"
	import="com.intumit.solr.robot.qadialog.*"
	import="java.io.UnsupportedEncodingException"
	import="java.net.*"
	import="java.util.*"
	import="com.intumit.message.MessageUtil"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%!
	/**
	 * 正確結構應該如下，只有根節點的 node-row 當中沒有 node-box，只有一個 h1 的 icon 以及一個增加子節點的 button
	 +-- node-row (*)
	 >> +-- node-box#h-box-{id}
	 >>>> +-- ... panel ...
	 >>>>>>> +-- ... buttons ...
	 >>>>>>> +-- ... contents ...
	 >>>>>>> +-- ... input#dlg_qa_{id} [name="dlg_qa_{parentId}"] 
	 // 這個 input 是重點，他的 value 會放這個節點的 json string
	 // submit 出去後 Server 端實際上都是靠這個 input 來判斷整個 dialog 結構
	 >> +-- div.node-level-container [rel="#dlg_qa_{id}"]
	 >>>> +-- div.node-level-container-vertial-line
	 >>>> +-- (repeat next level of node-row (*) )
	
	 */
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String idStr = StringUtils.trimToNull(request.getParameter("id"));
String uuid = UUID.randomUUID().toString();
boolean createMode = false;
boolean copyMode = "copy".equals(request.getParameter("action"));
QADialogConfig dlg = null;
JSONObject data = null;
String targetVer = StringUtils.defaultString(request.getParameter("target"), "head");

if (idStr == null) {
	data = new JSONObject();
	dlg = new QADialogConfig();
	dlg.setDialogName("[Unamed Dialog]");
	dlg.setDialogDesc("[Dialog description]");
	dlg.setMkey("[請務必設置 Mkey]");
	createMode = true;
}
else {
	int id = Integer.parseInt(idStr);
	dlg = QADialogConfig.get(id);
	if (dlg == null || (dlg != null && dlg.getTenantId() != t.getId())) 
		return;
	
	/*
	 * 這裡沒什麼意義，實際上取 config json 是用 ajax 去取的，這裡不需要管 data 才對
	 */
	if (StringUtils.equalsIgnoreCase("head", targetVer)) {
		data = dlg.getDialogConfigObject();
	}
	else if (StringUtils.equalsIgnoreCase("draft", targetVer)) {
		data = dlg.getDraftDialogConfigObject();
	}
	else {
		Integer iTargetVer = Integer.parseInt(targetVer);
		QADialogConfigVersion tv = QADialogConfigVersion.getByKeyAndVersionNumber(t.getId(), dlg.getMkey(), iTargetVer);
		if (tv != null) {
			data = tv.getDialogConfigObject();
		}
	}
}

if (copyMode) {
	dlg.setId(null);
	dlg.setDialogName("[Unamed Dialog]");
	dlg.setDialogDesc("[Dialog description]");
	dlg.setMkey("[請務必設置 Mkey，且 Mkey 無法修改]");
	createMode = true;
}

//System.out.println(new org.json.JSONObject(data.toString()).toString(2));
String entryPoint = StringUtils.trimToNull(data.optString("entryPoint"));
//System.out.println("*****************************");
//System.out.println(new org.json.JSONObject(data.toString()).toString(2));

JSONArray _1stLvChildren = data.optJSONArray("children");
if (_1stLvChildren == null) {
	_1stLvChildren = new JSONArray();
	data.put("children", _1stLvChildren);
}
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<TITLE><bean:message key='dialog.editor'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">

<link rel="stylesheet" href="<%= request.getContextPath() %>/wiseadm/css/codemirror.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/wiseadm/css/dialog-editor.css">

<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/groovy.js"></script>
<script src="<%= request.getContextPath() %>/script/mustache.min.js"></script>

<!--// Loading bootstrap again (after jquery-ui) bcz there is a conflicts using button loading state if bootstrap load before jquery-ui -->
<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jsoneditor.js"></script>

<script>
// Set the default CSS theme and icon library globally
JSONEditor.defaults.theme = 'bootstrap3';
JSONEditor.defaults.iconlib = 'bootstrap3';

function autoResize(id){
    var newheight;
    var newwidth;

    if(document.getElementById){
        newheight=document.getElementById(id).contentWindow.document .body.scrollHeight;
        newwidth=document.getElementById(id).contentWindow.document .body.scrollWidth;
    }

    document.getElementById(id).height= (newheight) + "px";
    document.getElementById(id).width= (newwidth) + "px";
}
</script>
<script id="node-box-template-standard" type="x-tmpl-mustache">
<div class="node-box" id="h-node-{{data.id}}" data-id="{{data.id}}">
	<div class="node-box-connect node-box-connect-first"></div>
	<div class="node-box-collapse-btn" data-id="{{data.id}}">
		<span class="glyphicon glyphicon-minus"></span>
	</div>
	<div class="node-box-expand-btn" style="display: none;" data-id="{{data.id}}">
		<span class="glyphicon glyphicon-plus"></span>
	</div>
	<div class="panel box-shadow--4dp panel-primary">
		<div></div>
		<div class="btn-group btn-xs pull-right" style="margin-top: -18px;">
			<button type="button" data-id="{{data.id}}" class="dlg-qa-delete glyphicon glyphicon-remove btn btn-primary" title=<bean:message key='scenario.deletion'/> ></button>
			<button type="button" data-id="{{data.id}}" class="dlg-qa-edit glyphicon glyphicon-pencil btn btn-primary" title=<bean:message key='scenario.editor.question.answer'/> ></button>
			<button type="button" data-id="{{data.id}}" class="dlg-qa-add-child glyphicon glyphicon-plus btn btn-primary" title=<bean:message key='scenario.added.new.layer'/> ></button>
			<button type="button" class="dlg-qa-dropdown glyphicon glyphicon-play btn dropdown-toggle btn-primary" id="dropdownMenu{{data.id}}" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"></button>

			<ul class="dropdown-menu" aria-labelledby="dropdownMenu{{data.id}}">    
				<li><a class="dlg-qa-cut" data-id="{{data.id}}"><bean:message key='scenario.cut'/></a></li>
				<li><a class="dlg-qa-copy" data-id="{{data.id}}"><bean:message key='scenario.replication'/></a></li>
				<li><a class="dlg-qa-paste" data-id="{{data.id}}"><bean:message key='scenario.posted'/></a></li>
				<li role="separator" class="divider"></li>
				<li><a class="dlg-qa-move-up" data-id="{{data.id}}"><bean:message key='scenario.up'/></a></li>
				<li><a class="dlg-qa-move-down" data-id="{{data.id}}"><bean:message key='scenario.down'/></a></li>
				<li role="separator" class="divider"></li>
				<li><a class="dlg-qa-set-as-entrypoint" data-id="{{data.id}}"><bean:message key='scenario.preset.node'/></a></li>
			</ul>
		</div>
		<div class="panel-heading">
			<h3 class="panel-title">{{data.text}}</h3>
		</div>
		<div class="panel-body">
			<bean:message key='scenario.number'/>：{{data.id}}<br>
			&nbsp;&nbsp;
			<span class="glyphicon glyphicon-import"></span>
			<span class="badge">{{data.perceptions.length}}</span>
			&nbsp;&nbsp;
			<span class="glyphicon glyphicon-screenshot"></span>
			<span class="badge">{{data.reactions.length}}</span>
		</div>
		<div class="panel-footer hide">
			<input type="hidden" id="dlg-qa-{{data.id}}" name="{{parentRefName}}" value="{{stringifyData}}">
		</div>
	</div>
</div>
</script>
<script id="node-box-template-compact" type="x-tmpl-mustache">
<div class="node-box" id="h-node-{{data.id}}" data-id="{{data.id}}">
	<div class="node-box-connect node-box-connect-first"></div>
	<div class="node-box-collapse-btn" data-id="{{data.id}}">
		<span class="glyphicon glyphicon-minus"></span>
	</div>
	<div class="node-box-expand-btn" style="display: none;" data-id="{{data.id}}">
		<span class="glyphicon glyphicon-plus"></span>
	</div>
	<div class="panel box-shadow--4dp panel-primary">
		<div></div>
		<div class="btn-group btn-xs pull-right" style="margin-top: -18px;">
			<button type="button" data-id="{{data.id}}" class="dlg-qa-delete glyphicon glyphicon-remove btn btn-primary" title="<bean:message key='scenario.deletion'/>"></button>
			<button type="button" data-id="{{data.id}}" class="dlg-qa-edit glyphicon glyphicon-pencil btn btn-primary" title=<bean:message key='scenario.editor.question.answer'/> ></button>
			<button type="button" data-id="{{data.id}}" class="dlg-qa-add-child glyphicon glyphicon-plus btn btn-primary" title=<bean:message key='scenario.added.new.layer'/> ></button>
			<button type="button" class="dlg-qa-dropdown glyphicon glyphicon-play btn dropdown-toggle btn-primary" id="dropdownMenu{{data.id}}" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"></button>

			<ul class="dropdown-menu" aria-labelledby="dropdownMenu{{data.id}}">    
				<li><a class="dlg-qa-cut" data-id="{{data.id}}"><bean:message key='scenario.cut'/></a></li>
				<li><a class="dlg-qa-copy" data-id="{{data.id}}"><bean:message key='scenario.replication'/></a></li>
				<li><a class="dlg-qa-paste" data-id="{{data.id}}"><bean:message key='scenario.posted'/></a></li>
				<li role="separator" class="divider"></li>
				<li><a class="dlg-qa-move-up" data-id="{{data.id}}"><bean:message key='scenario.up'/></a></li>
				<li><a class="dlg-qa-move-down" data-id="{{data.id}}"><bean:message key='scenario.down'/></a></li>
				<li role="separator" class="divider"></li>
				<li><a class="dlg-qa-set-as-entrypoint" data-id="{{data.id}}"><bean:message key='scenario.preset.node'/></a></li>
			</ul>
		</div>
		<div class="panel-heading" style='min-height: 60px;'>
			<h3 class="panel-title">{{data.text}}</h3>
			<input type="hidden" id="dlg-qa-{{data.id}}" name="{{parentRefName}}" value="{{stringifyData}}">
		</div>
	</div>
</div>
</script>
<script id="node-box-template-panel-body-normal" type="x-tmpl-mustache">
	編號：{{data.id}}
	{{#data.pipe}}
	 => <span class='hover-hl-node' data-id='{{data.pipe}}'>{{data.pipe}}</span>
	{{/data.pipe}}
	<br>
	
	<span class="glyphicon glyphicon-import"></span>
	<span class="badge">{{data.perceptions.length}}</span>
	&nbsp;&nbsp;
	<span class="glyphicon glyphicon-screenshot"></span>
	<span class="badge">{{data.reactions.length}}</span>
</script>
<script id="node-box-template-panel-body-detail" type="x-tmpl-mustache">
	編號：{{data.id}}
	{{#data.pipe}}
	 => <span class='hover-hl-node' data-id='{{data.pipe}}'>{{data.pipe}}</span>
	{{/data.pipe}}
	<br>
	
	<span class="glyphicon glyphicon-import"></span>
	<span class="badge">{{data.perceptions.length}}</span>
	&nbsp;&nbsp;
	<span class="glyphicon glyphicon-screenshot"></span>
	<span class="badge">{{data.reactions.length}}</span>
	<br>
	<small>
	{{#data.perceptions}}
	<span class="text-primary glyphicon glyphicon-import"></span>&nbsp;<span class="text-primary">{{label}} - {{type}}</span><br>
	{{/data.perceptions}}
	{{#data.reactions}}
	{{#pipe}}
	<span class='hover-hl-node' data-id='{{pipe}}'>
	<span class="text-danger glyphicon glyphicon-screenshot"></span>&nbsp;<span class="text-danger">{{label}} - {{type}}</span><br>
	</span>
	{{/pipe}}
	{{^pipe}}
	<span class="text-danger glyphicon glyphicon-screenshot"></span>&nbsp;<span class="text-danger">{{label}} - {{type}}</span><br>
	{{/pipe}}
	{{/data.reactions}}
	</small>
</script>
<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:9999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}

.dlg-dialog-log-panel-bg {
	background: #D8EBCF;
}
</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"><jsp:param name="hideUI" value="false" /></jsp:include>

<ul class="nav nav-tabs" id="myTabs" role="tablist" style="margin: 10px 0 0 10px;"> 
	<li role="presentation" class="active">
		<a href="#home" id="home-tab" role="tab" data-toggle="tab" aria-controls="home" aria-expanded="true"><bean:message key='scenario.dashboard'/></a>
	</li> 
	<li role="presentation" class="">
		<a href="#dialog" id="dialog-tab" role="tab" data-toggle="tab" aria-controls="dialog" aria-expanded="false"><bean:message key='scenario.dialogue.process'/></a>
	</li> 
	<li role="presentation" class="">
		<a href="#intent" role="tab" id="intent-tab" class='tab-iframe' data-toggle="tab" aria-controls="intent" aria-expanded="false"><bean:message key='scenario.intention'/></a>
	</li> 
	<li role="presentation" class="">
		<a href="#entity" role="tab" id="entity-tab" class='tab-iframe' data-toggle="tab" aria-controls="entity" aria-expanded="false"><bean:message key='scenario.entity'/></a>
	</li> 
	<%-- >li role="presentation" class="dropdown"> 
		<a href="#" class="dropdown-toggle" id="myTabDrop1" data-toggle="dropdown" aria-controls="myTabDrop1-contents">其他 <span class="caret"></span></a> 
		<ul class="dropdown-menu" aria-labelledby="myTabDrop1" id="myTabDrop1-contents"> 
			<li><a href="#dropdown1" role="tab" id="dropdown1-tab" data-toggle="tab" aria-controls="dropdown1">@fat</a></li> 
			<li><a href="#dropdown2" role="tab" id="dropdown2-tab" data-toggle="tab" aria-controls="dropdown2">@mdo</a></li> 
		</ul> 
	</li --%> 
	<li class="pull-right">
	    	<input type="checkbox" id="dlg-qa-show-node-detail" data-toggle="toggle" data-on="Detail" data-off="No detail">
	    	<input type="checkbox" id="dlg-qa-compact-mode" data-toggle="toggle" data-on="Compact" data-off="Normal">
	</li>
</ul>

<form id="form">
<div class="tab-content" id="myTabContent"> 
<div class="tab-pane fade active in" role="tabpanel" id="home" aria-labelledby="home-tab">
	
	<div class="row">
	<div class="col-md-12" style="margin: 15px 0 0 15px; padding-right: 45px;">
	  <div class="panel panel-primary">
		<div class="panel-heading">
		  <h3 class="panel-title"><bean:message key='scenario.basic.setting'/></h3>
		</div>
		<div class="panel-body">
			<table class="table table-bordered">
				<tr>
				  <th class='col-sm-1'>
					<input type='hidden' name='entryPoint' value='<%= StringUtils.trimToEmpty(entryPoint) %>'>
					<input type='hidden' id='curdAction' name='action' value='saveDraft'>
					<input type="hidden" name="id" value="<%= copyMode ? "" : StringUtils.trimToEmpty(idStr) %>">
					<input type="hidden" name="createMode" value="<%= createMode %>">
					<input type="hidden" id="versionComment" name="versionComment" value="">
					<bean:message key='scenario.name'/>
				  </th>
				  <td>
				  <input type="text" name="dialogName" value="<%= dlg.getDialogName() %>" class="col-sm-12" placeholder=<bean:message key='scenario.dialogue.situation.name'/> aria-describedby="basic-addon1">
				  </td>
				</tr>
				
				<tr>
				  <th><bean:message key='scenario.description'/></th>
				  <td>
				  <textarea name="dialogDesc" class="form-control" placeholder=<bean:message key='scenario.dialogue.situation.description'/> rows="10" aria-describedby="basic-addon2"><%= dlg.getDialogDesc() %></textarea>
				  </td>
				</tr>
				
				<tr>
				  <th><bean:message key='scenario.MKey'/></th>
				  <td>
				  <input type="text" name="mkey" value="<%= dlg.getMkey() %>" class="form-control" placeholder=<bean:message key='scenario.MKey'/> aria-describedby="basic-addon3">
				  </td>
				</tr>
				
				<tr>
				  <th><bean:message key='scenario.version.number'/></th>
				  <td><%= dlg.getPublishedVersionNumber() != 0 ? dlg.getPublishedVersionNumber() : "" %><bean:message key='scenario.temporary'/></td>
				</tr>
				
				<tr>
				  <th><bean:message key='scenario.storage.time'/></th>
				  <td><%= dlg.getDialogTimestamp() %></td>
				</tr>
				
				<tr>
				  <th><bean:message key='scenario.editor'/></th>
				  <td><%= dlg.getPublishedVersion() != null ? dlg.getPublishedVersion().getContributor() : "" %><bean:message key='scenario.na'/></td>
				</tr>
				
			    <tr style="margin-top: 30px; margin-bottom: 30px;">
					<td colspan=2>
						<a href="javascript:void(0)" class="btn btn-danger btnSave col-sm-2" role="button" style="margin-right: 30px;"><bean:message key='scenario.storage.and.release'/></a> 
						<a href="javascript:void(0)" class="btn btn-warning btnSaveDraft col-sm-2" role="button" style="margin-right: 30px;"><bean:message key='scenario.temporary'/></a> 
						<a href="javascript:void(0)" class="btn btn-warning col-sm-2 btnDropDraft" role="button"><bean:message key='scenario.abandonment'/></a> 
					</td>
			  	</tr>
			</table>
		</div>
		<div class="panel-footer hide">Panel footer</div>
	  </div>
	</div>
	</div>
</div>

<div class="tab-pane fade in" role="tabpanel" id="dialog" aria-labelledby="dialog-tab">
	<%-- 這裡是編輯 Dialog UI 的區域，主要靠 renderQAInputUI 來產生 --%>
	<nav class="cbp-spmenu cbp-spmenu-vertical-big cbp-spmenu-left-big dlg-dialog-log-panel-bg" id="cbp-spmenu-s1">
		<h3 class='dlg-toggle-quick-menu' style='background: #439843;'><span class='glyphicon glyphicon-chevron-left'></span><bean:message key='scenario.debugging'/>&nbsp;<small><span class="glyphicon glyphicon-pushpin dlg-toggle-debug-panel"></span></small></h3>
		<div class='col-md-12' style='margin-top: 20px;' id="dialog-log-panel">
			
	    	</div>
	</nav>
	<div id="dlg-qa-panel" style="margin: 20px 0 0 50px; width: 5000px;">
	<div class='node-row' id="rootNode">
	<h1 style='float: left; position: absolute; left: 20px;'>
		<button type="button" data-id='' style='float: left; position: absolute; left: 7px; top: 35px;' class="dlg-qa-add-child glyphicon glyphicon-plus btn btn-primary"></button>
		<button type="button" data-id='' style='float: left; position: absolute; left: 7px; top: 58px;' class="dlg-qa-paste glyphicon glyphicon-paste btn btn-primary"></button>
		
		<button type="button" data-id='' style='float: left; position: absolute; left: 7px; top: 88px;' class=" dlg-qa-copy-json glyphicon glyphicon-share btn btn-danger"></button>
		<button type="button" data-id='' style='float: left; position: absolute; left: 7px; top: 118px;' class=" dlg-qa-paste-json glyphicon glyphicon-briefcase btn btn-danger"></button>
		
		<span class='glyphicon glyphicon-education dlg-toggle-quick-menu'></span>
	</h1>
	<div rel='dlg_qa'  class='node-level-container' style='margin-left: 20px;'>
	<div class='node-level-container-vertical-line'></div>
	</div>
	<%
		//renderQAInputUI("dlg_qa", _1stLvChildren, out, locale, 0);
	%>
	</div>
	</div>

	  <div class="row">
	    <div class="col-sm-offset-1 col-sm-11" style="margin-top: 30px; margin-bottom: 30px;">
			<a href="javascript:void(0)" class="btn btn-danger btnSave col-sm-2" role="button" style="margin-right: 30px;"><bean:message key='scenario.storage.and.release'/></a> 
			<a href="javascript:void(0)" class="btn btn-warning btnSaveDraft col-sm-2" role="button" style="margin-right: 30px;"><bean:message key='scenario.temporary'/></a> 
			<a href="javascript:void(0)" class="btn btn-warning col-sm-2 btnDropDraft" role="button"><bean:message key='scenario.abandonment'/></a> 
	  	</div>
	  </div>
</div> <!-- tab-pane end -->

<div class="tab-pane fade" role="tabpanel" id="intent" aria-labelledby="intent-tab" data-src='<%= request.getContextPath() %>/wiseadm/intent'> 
   <div class='container'>
    <iframe src="" width="500" height="203" frameborder="0" id="iframe1" onLoad="autoResize('iframe1');" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>
    </div>
</div>


<div class="tab-pane fade" role="tabpanel" id="entity" aria-labelledby="entity-tab" data-src='<%= request.getContextPath() %>/wiseadm/entity'> 
   <div class='container'>
    <iframe src="" width="500" height="203" frameborder="0" id="iframe2" onLoad="autoResize('iframe2');" webkitAllowFullScreen mozallowfullscreen allowFullScreen></iframe>
    </div>
</div>

</div> <!-- tab-content end -->
</form>
<div id="dlg-prompt" title="工程模式" style="display:none; background-color: #F8F8F8;">
<div name="msg"></div>
<textarea id="dlg-prompt-textarea" class='form-control' rows='10'></textarea>
<button id='btnDlgQaPaste' class='btn btn-danger' style="display: none;">送出</button>
</div>

<div id="loading-panel" class="loading-panel"></div>

<div id="dlg-help" title="說明" style="display:none; background-color: #F8F8F8;">

<table class='table table-bordered table-strip'>
<tr><th><bean:message key='scenario.response.type'/></th><th><bean:message key='scenario.parameter.description'/></th></tr>
<tr><td><bean:message key='scenario.coverage.answer'/></td><td><bean:message key='parameter.one'/>：<bean:message key='scenario.wants.cover'/></td></tr>
<tr>
	<td><bean:message key='scenario.additional.reply'/></td>
	<td><bean:message key='scenario.additional.reply.description'/>
	{{<bean:message key='scenario.additional.reply.description1'/>}}
	{{<bean:message key='scenario.additional.reply.description2'/>}}
	{{<bean:message key='scenario.additional.reply.description3'/>}}
	</td>
</tr>
<tr>
	<td><bean:message key='scenario.setting.menu'/></td>
	<td><bean:message key='scenario.setting.menu.description'/>
	</td>
</tr>
<tr>
	<td><bean:message key='scenario.additional.options'/></td>
	<td>
	<bean:message key='scenario.setting.menu.description'/>
	</td>
</tr>
<tr>
	<td><bean:message key='scenario.attached.LINE.graphic'/></td>
	<td>
	<bean:message key='scenario.attached.LINE.graphic.description'/>
	</td>
</tr>
<tr><td><bean:message key='hierarchical.redirect.other.question'/></td><td><bean:message key='hierarchical.redirect.other.question.description'/></td></tr>
<tr>
	<td><bean:message key='scenario.transferred.another.node'/></td>
	<td><bean:message key='scenario.transferred.another.node.description'/>
		<ul>
		<% for (QAConversationalDialog.NodeState rt: QAConversationalDialog.NodeState.values()) { %>
		<li><%= rt.name() %></li>
		<% } %>
		</ul>
	</td>
</tr>
<tr>
	<td><bean:message key='scenario.index.query'/></td>
	<td><bean:message key='scenario.index.query.description'/>
		
	</td>
</tr>
<tr><td><bean:message key='scenario.transferred.back.previous.node'/></td><td><bean:message key='scenario.transferred.back.previous.node.description'/></td></tr>
<tr><td><bean:message key='scenario.setting.context.variable'/></td><td><bean:message key='scenario.setting.context.variable.description'/></td></tr>
<tr><td><bean:message key='scenario.setting.long.term.variables'/></td><td><bean:message key='scenario.setting.context.variable.description'/></td></tr>
<tr><td><bean:message key='scenario.setting.long.term.variables1'/></td><td><bean:message key='scenario.setting.long.term.variables1.description'/></td></tr>
<tr><td><bean:message key='scenario.setting.onetime.variables'/></td><td><bean:message key='scenario.setting.context.variable.description'/></td></tr>
<tr><td><bean:message key='scenario.clearing.situation.variables'/></td><td><bean:message key='scenario.clearing.situation.variables.description'/></td></tr>
<tr><td><bean:message key='scenario.clearing.longterm.variables'/></td><td><bean:message key='scenario.clearing.situation.variables.description'/></td></tr>
<tr><td><bean:message key='scenario.clear.onetime.variables'/></td><td><bean:message key='scenario.clearing.situation.variables.description'/></td></tr>
<tr><td><bean:message key='scenario.END'/></td><td><bean:message key='scenario.END.description'/></td></tr>
<tr><td><bean:message key='scenario.based.longterm.variables.attached.reply'/></td><td><bean:message key='scenario.based.longterm.variables.attached.reply.description'/></td></tr>
<tr><td><bean:message key='scenario.setting.response.variable'/></td><td><bean:message key='scenario.setting.response.variable.description'/></td></tr>
<tr><td><bean:message key='scenario.sticker.user.label'/></td><td><bean:message key='scenario.sticker.user.label.description'/></td></tr>
</table>
</div>
	
<%-- 接下來是點編輯後跳出的 Dialog 介面 --%>
<div id="dlg-qa-dialog" title="<bean:message key='advanced.qa'/>" style="display:none; background-color: #F8F8F8;">
	
	<ul class="nav nav-tabs" id="dlgTabs" role="tablist" style="margin: 0px 0 0 0px;"> 
		<li role="presentation" class="active">
			<a href="#dlg-main" id="dlg-main-tab" role="tab" data-toggle="tab" aria-controls="dlg-main" aria-expanded="true"><bean:message key='scenario.main.screen'/></a>
		</li> 
		<li role="presentation" class="">
			<a href="#dlg-perceptions" id="dlg-perceptions-tab" role="tab" data-toggle="tab" aria-controls="dlg-perceptions" aria-expanded="false"><bean:message key='scenario.awareness.condition'/></a>
		</li> 
		<li role="presentation" class="">
			<a href="#dlg-actions" id="dlg-actions-tab" role="tab" data-toggle="tab" aria-controls="dlg-actions" aria-expanded="false"><bean:message key='scenario.response.setting'/></a>
		</li> 
		<li role="presentation" class="">
			<a href="#dlg-advanced" id="dlg-advanced-tab" role="tab" data-toggle="tab" aria-controls="dlg-advanced" aria-expanded="false"><bean:message key='scenario.advanced.setting'/></a>
		</li> 
		<li class="pull-right">
	    <div class="btn-group">
	  		<button type="button" class="btn btn-primary btn-dlg-qa-save" title='<bean:message key='submit'/>'><span class='glyphicon glyphicon-floppy-disk'></span></button>
	  		<button type="button" class="btn btn-default btn-dlg-qa-cancel" title='<bean:message key='global.cancel'/>'><span class='glyphicon glyphicon-repeat'></span></button>
	  	</div>
		</li>
	</ul>
  
    <div class="tab-content">
	
	<div class="tab-pane fade active in" role="tabpanel" id="dlg-main" aria-labelledby="dlg-main-tab">
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.node.number'/></label>
	   	<div class="col-sm-7">
			<span id="dlg-qa-id"></span>
	   	</div>
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.enabling.node'/></label>
	   	<div class="col-sm-3">
	    		<input type="checkbox" id="dlg-qa-node-enable" name="nodeEnable" data-size="normal" data-toggle="toggle">
			<p class="help-block"><bean:message key='scenario.whether.node.enabled'/></p>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.node.title'/></label>
	   	<div class="col-sm-11">
			<input id="dlg-qa-t" type="text" class="form-control"></input>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.node.type'/></label>
	   	<div class="col-sm-11">
			<select id="dlg-qa-c" class="form-control">
				<option value="NORMAL"><bean:message key='scenario.general.node'/></option>
				<option value="SYMBOLIC_LINK_TO_NODE"><bean:message key='scenario.reference.another.node'/></option>
				<option value="TRANSPARENT"><bean:message key='scenario.transparent.node'/></option>
			</select>
			<input id="dlg-qa-p" type="text" class="form-control" style="display:none;" placeholder="連結到..."></input>
			<p class="help-block"><bean:message key='scenario.node.type'/></p>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.global.node'/></label>
	   	<div class="col-sm-2">
	    		<input type="checkbox" id="dlg-qa-global-prepend" name="globalPrepend" data-size="normal" data-toggle="toggle">
			<p class="help-block"><bean:message key='scenario.ubiquitous.node'/></p>
	   	</div>
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.protected.node'/></label>
	   	<div class="col-sm-2">
	    		<input type="checkbox" id="dlg-qa-protectMode" name="protectMode" data-size="normal" data-toggle="toggle">
			<p class="help-block"><bean:message key='situation.protected.node.not.snatched.global.node'/></p>
	   	</div>
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.priori.sensing.opportunity'/></label>
	   	<div class="col-sm-5">
	   		<div style="display: none;">
	    	<input type="checkbox" id="dlg-qa-cpsp" name="specialCheckMode" data-size="normal" data-toggle="toggle" data-on="剛進情境模式" data-off="一般模式" data-width="120px">
	    	<br><br>
	    	一般模式比對時機
	    	</div>
			<select id="dlg-qa-cp" class="form-control">
				<option value="JUST_ACTIVATED"><bean:message key='scenario.just.entered.node'/></option>
				<option value="WAIT_INPUT"><bean:message key='scenario.waiting.for.input'/></option>
				<option value="GOT_INPUT"><bean:message key='scenario.receives.user.input'/></option>
				<option value="LEAVING_NODE"><bean:message key='scenario.ready.leave.node'/></option>
				<option value="DEACTIVE"><bean:message key='scenario.node.goes.sleep'/></option>
			</select>
			<p class="help-block"><bean:message key='scenario.prior.type.global.node.sensing.timing'/><%-- ，若「剛進情境模式」為ON，則不管一般模式比對時機。--%></p>
	   	</div>
	  </div>
	  <div class="row" style="display: none;">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">答案</label>
	   	<div class="col-sm-11">
			<textarea id="dlg-qa-a" class="form-control" rows="5" disabled="true"></textarea>
			<p class="help-block">當使用者選擇了此項目後，系統會給予此回答</p>
	   	</div>
	  </div>
	  <div class="row" style="display: none;">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='time.limit'/></label>
	   	<div class="col-sm-11">
	   		<div class="col-sm-10" style="padding: 0;">
				<input id="dlg-qa-e" type="text" class="form-control" placeholder="<bean:message key='keep.time.limit'/>"
		        	readonly="readonly"  disabled="true" style="background-color: white;"></input>
		    </div>
	        <div class="col-sm-1">
	   			<button id="dlg-qa-clear-expiry" type="button" disabled="true" class="btn btn-default"><bean:message key="keep.time.limit"/></button>
	   		</div>
	   	</div>
	  </div>
	  <div class="row" style="display: none;">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">下層顯示模式</label>
	   	<div class="col-sm-5">
			<select id="dlg-qa-v" class="form-control" disabled="true">
				<% for (MenuView msb: MenuView.values()) { %>
				<option value="<%= msb.name() %>"><%= msb.getTitle() %></option>
				<% } %>
			</select>
			<p class="help-block">注意！僅有在有子選單的情況下此選項才有功能</p>
	   	</div>
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">下層輸入模式</label>
	   	<div class="col-sm-5">
			<select id="dlg-qa-i" class="form-control" disabled="true">
				<% for (MenuSelectionBehavior msb: MenuSelectionBehavior.values()) { %>
				<option value="<%= msb.name() %>"><%= msb.getTitle() %></option>
				<% } %>
			</select>
			<p class="help-block">注意！僅有在有子選單的情況下此選項才有功能</p>
	   	</div>
	  </div>
	  <div class="row">	
	    <div class="col-sm-offset-1 col-sm-11">
	    	<hr/>
	  		<button type="button" class="btn btn-primary btn-dlg-qa-save"><bean:message key='submit'/></button>
	  		<button type="button" class="btn btn-default btn-dlg-qa-cancel"><bean:message key='global.cancel'/></button>
	  	</div>
	  </div>
	</div>
	  
	<div class="tab-pane fade" role="tabpanel" id="dlg-perceptions" aria-labelledby="dlg-perceptions-tab">
	  <div class="row">
		<div class="col-sm-11" style="margin-left: 20px;">
		    	<textarea class="form-control hide" rows="5" id="dlg-qa-perceptions-config" name="dlg-qa-perceptions-config"></textarea>
		
		    	<div id='dlg-qa-perceptions-config_editor_holder' class='row'></div>
			<p class="help-block"><bean:message key='scenario.awareness.condition.set.area'/></p>
		</div>
	  </div>
	</div>
	
	<div class="tab-pane fade" role="tabpanel" id="dlg-actions" aria-labelledby="dlg-actions-tab">
	  <div class="row">
		<div class="col-sm-11" style="margin-left: 20px;">
		    	<textarea class="form-control hide" rows="5" id="dlg-qa-reactions-config" name="dlg-qa-reactions-config"></textarea>
		
		    	<div id='dlg-qa-reactions-config_editor_holder' class='row'></div>
			<p class="help-block"><bean:message key='scenario.response.action.setting.area'/></p>
		</div>
	  </div>
	</div>
	  
	<div class="tab-pane fade" role="tabpanel" id="dlg-advanced" aria-labelledby="dlg-advanced-tab">
	  <div class="row">
		<div class="col-sm-11" style="margin-left: 20px;">
		    	<textarea class="form-control hide" rows="5" id="dlg-qa-x" name="dlg-qa-x"></textarea>
		
		    	<div id='dlg-qa-x_editor_holder' class='row'></div>
			<p class="help-block"><bean:message key='qa.extra.params.ex'/></p>
		</div>
	  </div>
	
	  <% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E1) > 0) { %>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='scenario.script'/></label>
	   	<div class="col-sm-11">
			<textarea id="dlg-qa-s" class="form-control" rows="5"></textarea>
			<p class="help-block"><bean:message key='scenario.script.description'/>當執行反應之前，先跑過此腳本（Groovy），腳本回傳 null 會繼續檢測所有反應設置，若不是 null 則會跳過所有反應設置。若回傳的是 PostRuleCheckResult 會根據該 Result 做後續作動</p>
	   	</div>
	  </div>
	  <% } else { // 這裡不應該是用 hidden 方式作，否則是資安漏洞 %>
			<textarea id="dlg-qa-s" class="hidden" rows="5"></textarea>
	  <% } %>
	</div>
	</div>
	  
</div> <!-- dlg-qa-panel end -->

<script>
var templateConfig = {
	name: '#node-box-template-standard',
	showDetail: false,
};

var showDialogLog = false;
var copyArea = null;
var qaExtraParamEditor;
var qaPerceptionsEditor;
var qaReactionsEditor;

$(function() {
  var curDocId = '<%= StringEscapeUtils.escapeJavaScript(StringUtils.defaultString((String)request.getAttribute("docId"))) %>';
  
  $.widget( "ui.autocomplete", $.ui.autocomplete, {
	    options: {
	        delay: 500,
	        prefix: ""
	    },

	    _renderItem: function( ul, item ) {
	        var label = item.label;
	        //console.log(item);
	        if ( this.options.prefix ) {
	            label = this.options.prefix + " " + label;
	        }
	        return $( "<li>")
	        		.append( $("<a>")
		            .append( $( "<strong>" ).text( label ) )
		            .append( $( "<span>" ).text( " (" ) )
		            .append( $( "<small>" ).text( item.id ) )
		            .append( $( "<span>" ).text( ") " ) )
		         )
	            .appendTo( ul );
	    },
	});
  
  $(document).ready(function() {
	  // 解決先顯示 對話流程 Tab 時，某些 style 無法正常呈現的奇怪問題
	  setTimeout(function() {
		  $('#dialog-tab').tab('show');	// 先顯示 對話流程 Tab
		  updateViewOfWholeTree($('#rootNode'), null, false);
		  redrawConnectLines();
	  }, 100);

	  <%-- 某些 tab 是直接嵌入其他網頁（用 iframe） --%>
	  $('.tab-iframe').bind('shown.bs.tab', function(e) {  
		    paneID = $(e.target).attr('href');
		    //console.log(paneID);
		    src = $(paneID).attr('data-src');
		    // if the iframe hasn't already been loaded once
		    if ($(paneID+" iframe").attr("src")=="") {
		        $(paneID+" iframe").attr("src",src);
		    }
	        $(paneID+" iframe").attr("src",src);
		});
	  
	  $('#dlg-qa-p').autocomplete({
		    source: function(req, callback){
			    	if($.trim(req.term)){
				    	$.getJSON('<%= request.getContextPath() %>/wiseadm/qa-plugin/qa-search.jsp',
						{
				    			id: curDocId,
				    			term: req.term
						},
				    		callback
				    	);
			    	} else {
			    		callback([]);
			    	}
		    },
		    minLength: 2,
		    select: function( event, ui ) {
			    	var item = ui.item;
			    	//console.log(item);
			    	var pipe2id = item.id;
			    	$('#dlg-qa-p').val(pipe2id);
			    	return false;
		    }
		  });
	  
	  <% if (idStr != null) { %>
	  <%-- 有 id 的話就取 id 然後 render 畫面 --%>
	  $.getJSON("qaDialog-ajax.jsp", 
			  { action: "get", target: "<%= targetVer %>", id: "<%= StringUtils.trimToEmpty(idStr) %>" },
			  function(result) {
				  //console.log(result);
				  cfg = result.dlgConfig;
				  
				  renderNode(null, $('#rootNode'), cfg.children);
				  redrawConnectLines();
			  }
	  );
	  <% } %>
  });

  <%-- render 流程 --%>
  renderNode = function(parentId, parentElm, childrenJson) {
	
	for (var i=0; i < childrenJson.length; i++) {
		data = childrenJson[i];
		var relToParent = parentId!=null?'dlg_qa_'+parentId:'dlg_qa';
		node = createNodeBox(relToParent, data);
		newLvContainer = $('<div rel="dlg_qa_' + data.id + '" class="node-level-container" style="margin-left: 250px;"><div class="node-level-container-vertical-line"></div></div>');
		
		newRow = $('<div class="node-row"></div>');
		node.appendTo(newRow);
		newLvContainer.appendTo(newRow);
		
		newRow.appendTo(parentElm.children('.node-level-container'));
		
		if (data.hasOwnProperty("children")) {
			renderNode(data.id, newRow, data.children);
		}
	}
  };
  
  redrawConnectLines = function() {
	  $(":animated").promise().done(function() {
		  $('.node-level-container:visible').each(function() {
				$lvContainer = $(this); 
				$lvContainer.children('.node-row').find('.node-box:first > .node-box-connect-first').removeClass('node-box-connect-first');
				$lastRow = $lvContainer.children(".node-row:last");
				
				$firstRow = $lvContainer.children(".node-row:first");
				
				if ($lastRow.length > 0) {
					//$nodeRow.attr('style', 'border: 1px solid red');
					//$nodeRow.prepend('<span>' + $nodeRow.position().top + ' -> box.top:' + box.position().top + '</span>');
					newH = $lastRow.offset().top - $lvContainer.offset().top;
					$lvContainer.find(".node-level-container-vertical-line:first").height(newH);
				}
				
				if ($firstRow.length > 0) {
					$firstRow.find('.node-box:first > .node-box-connect').addClass('node-box-connect-first');
				}
			 });
		  
		});
  };

  <%-- 
  遞迴檢查每個 reaction，如果是 redirect_to_node，就在 reaction 多加一個 pipe property = redirectToNodeId
  多一個 property 的原因是 mustache.js 無法直接用「值」的內容來判斷顯示邏輯，只能用「值」存不存在來判斷
  --%>
  findAndSetPipe = function(data) {
	if (data.reactions) {
		for (var i=0; i< data.reactions.length; i++) {
			var reaction = data.reactions[i];
			if (reaction.type == 'redirect_to_node') {
				reaction.pipe = reaction.contents[0];
				console.log(data);
			}
		}
	}
	
	if (data.hasOwnProperty("children")) {
		for (var i=0; i< data.children.length; i++) {
			findAndSetPipe(data.children[i]);
		}
	}
  };

  <%-- 
  更新 nodeBox 的內容
  如果 recreateNodeBox == true，則會連 nodeBox 自己本身都會 remove 重新再呼叫 createNodeBox 產生一次 
  （recreateNodeBox 主要用在更改 nodeBox 風格時）
  --%>
  updateNodeView = function(nodeId, nodeData, parentId, recreateNodeBox) {
	if (recreateNodeBox) {
		parentRefName = (typeof(parentId) == 'undefined' || parentId == null) ? 'dlg_qa' : 'dlg_qa_' + parentId;
		//console.log("Recreate nodebox [" + nodeId + " of " + parentRefName + "]");
		nodeRow = $('#h-node-' + nodeId).parent('.node-row');
		$('#h-node-' + nodeId).remove();
		n = createNodeBox(parentRefName, nodeData);
		n.prependTo(nodeRow);
	}
	nodeData = $.extend({nodeEnable:true, perceptions:[], reactions:[]}, nodeData); // 確保不會連 perceptions / reactions property 都不存在
	$('#h-node-' + nodeId + ' .panel-title').text(nodeData.text);
	$('#h-node-' + nodeId + ' .panel-body').text('編號：#' + nodeData.id);
	
	$panel = $('#h-node-' + nodeId + ' .panel');
	$btnGrp = $('#h-node-' + nodeId + ' .btn-group .btn');
	$panel.removeClass('panel-primary').removeClass('panel-success').removeClass('panel-default').removeClass('panel-info').removeClass('panel-danger').removeClass('panel-warning');
	$btnGrp.removeClass('btn-primary').removeClass('btn-success').removeClass('btn-default').removeClass('btn-info').removeClass('btn-danger').removeClass('btn-warning');
	$pbody = $('#h-node-' + nodeId + ' .panel-body');
	
	appendIcons = "<br>";
	
	if (!nodeData.nodeEnable) {
		appendIcons += "<span class='glyphicon glyphicon-ban-circle'></span>";
	}
	
	if (nodeData.pipe) {
		$panel.addClass('panel-info');
		$btnGrp.addClass('btn-info');
	}
	else {
		if (nodeId == $('input[name=entryPoint]').val()) {
			$panel.addClass('panel-success');
			$btnGrp.addClass('btn-success');
		}
		else if (nodeData.nodeType == 'TRANSPARENT') {
			$panel.addClass('panel-default');
			$btnGrp.addClass('btn-default');
		}
		else if (nodeData.globalPrepend || nodeData.globalAppend) {
			$panel.addClass('panel-warning');
			$btnGrp.addClass('btn-warning');
		}
		else {
			$panel.addClass('panel-primary');
			$btnGrp.addClass('btn-primary');
		}
	}
	
	dataForRender = {
		data: nodeData,
	};
	
	if (templateConfig.showDetail) {
		findAndSetPipe(nodeData);
	}
	
	var template = $(templateConfig.showDetail ? '#node-box-template-panel-body-detail' : '#node-box-template-panel-body-normal').html();
	Mustache.parse(template);
	var panelBody = Mustache.render(template, dataForRender);
	
	$pbody.html(panelBody);
  };

  <%-- 
  更新每個 nodeBox 的內容
  如果 recreateNodeBox == true，則會連 nodeBox 自己本身都會 remove 重新再呼叫 createNodeBox 產生一次 
  （recreateNodeBox 主要用在更改 nodeBox 風格時）
  --%>
  updateViewOfWholeTree = function(startNodeRow, parentId, recreateNodeBox) {
	 var dataId = startNodeRow.children('.node-box').attr('data-id');
	 
	 if (typeof(dataId) != 'undefined') {
		 theInput = startNodeRow.find('#dlg-qa-' + dataId);
		 var data = JSON.parse(theInput.val());
		 
		 updateNodeView(dataId, data, parentId, recreateNodeBox);
	 }
	 
	 (function(startNodeRow, parentId){startNodeRow.children('.node-level-container').children('.node-row').each(function() { $this = $(this); updateViewOfWholeTree($this, dataId, recreateNodeBox); });})(startNodeRow, dataId);
  };
  
  <%--
  	在 client 端畫 node box，產生一個新的 div.node-box
  --%>
  createNodeBox = function(inputNameToRefParent, data) {
	  dataForRender = {data: data, parentRefName: inputNameToRefParent, stringifyData: JSON.stringify(data)};
	  var template = $(templateConfig.name).html();
	  Mustache.parse(template);
	  var rendered = Mustache.render(template, dataForRender);
	  n = $(rendered);
	  return n;
  };

  flushWholeTreeWithNewId = function(startNodeRow) {
	 var oldId = startNodeRow.children('.node-box').attr('data-id');
	 
	 var newId = new Date().getTime();
	 while ($('#h-node-' + newId).length > 0) {  // 檢測重複
		newId = new Date().getTime(); // 現場就 collision 就一直 loop 到沒有為止
	 }

	 if (copyArea) { // copyArea 如果是 clone 的就不在整個 DOM tree 上，前面的 loop 是找不到重複的
		 while (copyArea.find('#h-node-' + newId).length > 0) {  // 檢測重複
			newId = new Date().getTime(); // 現場就 collision 就一直 loop 到沒有為止
		 }
	 }
	 //console.log("oldId = " + oldId + " / newId = " + newId);
	 
	 startNodeRow.find('#h-node-' + oldId).attr('id', 'h-node-' + newId);
	 theInput = startNodeRow.find('#dlg-qa-' + oldId);
	 theInput.attr('id', 'dlg-qa-' + newId);
	 startNodeRow.find('input[name=dlg_qa_' + oldId + ']').attr('name', 'dlg_qa_' + newId);
	 startNodeRow.find('*[data-id=' + oldId + ']').attr('data-id', '' + newId);
	 startNodeRow.find('div[rel=dlg_qa_' + oldId + ']').attr('rel', 'dlg_qa_' + newId);
	 startNodeRow.children('.node-level-container').find(".node-level-container-vertical-line:first").height(0);

	 var data = JSON.parse(theInput.val());
	 data.id = newId;
	 theInput.val(JSON.stringify(data));
	 
	 startNodeRow.children('.node-level-container').find('.node-row').each(function() { $this = $(this); flushWholeTreeWithNewId($this); });
  };
  
  $(document).on('shown.bs.tab', 'a[data-toggle="tab"]', function (e) {
	  updateViewOfWholeTree($('#rootNode'), null, false);
	  redrawConnectLines();
	});

  $(document).on('click', '.node-box-collapse-btn', function(){
	  dataId = $(this).attr('data-id');
	  $('div[rel=dlg_qa_' + dataId + ']').hide(100);
	  $(this).parent().find('.node-box-expand-btn').show();
	  $(this).hide();
	  redrawConnectLines();
  });
  
  $(document).on('click', '.node-box-expand-btn', function(){
	  dataId = $(this).attr('data-id');
	  $('div[rel=dlg_qa_' + dataId + ']').show(100);
	  $(this).parent().find('.node-box-collapse-btn').show();
	  $(this).hide();
	  redrawConnectLines();
  });

  $(document).on('mouseover', '.hover-hl-node', function() {
	  nodeId = $(this).attr('data-id');
	  $panel = $('#h-node-' + nodeId + ' .panel');
	  $panel.addClass('hl-box-shadow--10dp');
  });
  
  $(document).on('mouseleave', '.hover-hl-node', function() {
	  nodeId = $(this).attr('data-id');
	  $panel = $('#h-node-' + nodeId + ' .panel');
	  $panel.removeClass('hl-box-shadow--10dp');
  });

  $(document).on('click', '.helpIcon', function() {
	  $('#dlg-help').dialog('open');
  });

  <%--
   刪除節點，就是連 node-row 一起刪除，整個節點包含子節點的 DOM 就會刪掉
  --%>
  $(document).on('click', '.dlg-qa-delete', function(){
	 if (confirm("確認刪除節點（包含其下所有子節點）？")) {
		 dataId = $(this).attr('data-id');
		 nodeBox = $(this).parents('.node-box');
		 /*nodeBox.find(".panel").removeClass('panel-primary').removeClass('panel-default').addClass('panel-danger');
		 nodeBox.find("button").removeClass('btn-primary').addClass('btn-danger').attr('disabled', 'true');
		 panelHeading = nodeBox.find('.panel-heading');
		 panelHeading.html($('<del/>').text(panelHeading.text()));
		 
		 nodeBox.parent().children('div.node-level-container').html(""); // 下面節點全部幹掉，還是應該 hide 就好？*/
		 nodeBox.parent().remove();
		 redrawConnectLines();
	 }
  });
  
  $(document).on('click', '.dlg-qa-move-up', function(){
	 nodeRow = $(this).closest('.node-row');
	 nodeRowPrev = $(this).closest('.node-row').prev();
	 
	 if (nodeRow != nodeRowPrev) {
		 nodeRow.insertBefore(nodeRowPrev);
		 redrawConnectLines();
	  }
  });
  
  $(document).on('click', '.dlg-qa-move-down', function(){
	 nodeRow = $(this).closest('.node-row');
	 nodeRowNext = $(this).closest('.node-row').next();
	 
	 if (nodeRow != nodeRowNext) {
		 nodeRow.insertAfter(nodeRowNext);
		 redrawConnectLines();
	 }
  });
  
  $(document).on('click', '.dlg-qa-set-as-entrypoint', function(){
	 var dataId = $(this).attr('data-id');
	 $('.panel-success').removeClass('panel-success');
	 var nodebox = $(this).closest('.node-box');
	 nodebox.find('.panel-primary').addClass('panel-success');
	 $('input[name=entryPoint]').val(dataId);
  });
  
  $(document).on('click', '.dlg-qa-cut', function(){
	 copyArea = $(this).closest('.node-row');
	 $(this).closest('.node-row').hide(500); // 剪下來點動畫
	 $(":animated").promise().done(function() {
		 $(this).closest('.node-row').remove();
		 $(this).closest('.node-row').children('.node-level-container').find(".node-level-container-vertical-line:first").height(0);
		 redrawConnectLines();
	 });
  });

  <%--
	複製貼上的概念很好，但一樣要處理 id 的問題，跟 paste 一樣，copy 應該要處理父節點的部分，也得處理 id 本身（必需要重新生出一個）
  --%>
  $(document).on('click', '.dlg-qa-copy', function(){
	 oldId = $(this).attr('data-id');
	 copyArea = $(this).closest('.node-row').clone();
	 
	 flushWholeTreeWithNewId(copyArea);
	 $(this).closest('.node-row').fadeIn(150).fadeOut(150).fadeIn(150).fadeOut(150).fadeIn(150); // 來點動畫
  });
  
  <%--
   貼上節點需要進一步處理各種節點內 id 的問題（主要是部分 element 上會標有上層節點的資訊，沒有一併更新會有問題）
  --%>
  $(document).on('click', '.dlg-qa-paste', function(){
	 if (copyArea) {
		 targetRow = $(this).closest('.node-row');
		 lvContainer = targetRow.children('.node-level-container');
		 targetNodeBoxId = targetRow.children('.node-box').attr('data-id');
		 copiedNodeBoxId = copyArea.children('.node-box').attr('data-id');
		 
		 // 把被複製的存 json 的那個 input 的 name 設定為新的父節點
		 copyArea.find('input#dlg-qa-' + copiedNodeBoxId).attr('name', typeof(targetNodeBoxId) != 'undefined' ? 'dlg_qa_' + targetNodeBoxId : 'dlg_qa');
		 
		 copyArea.appendTo(lvContainer);
		 copyArea.show(500);

		 $(":animated").promise().done(function() {
			 updateViewOfWholeTree(copyArea, null, false);
			 copyArea = null;
			 redrawConnectLines();
		 });
	 }
  });
  
  $(document).on('click', '.dlg-qa-copy-json', function() {
	  $('#btnDlgQaPaste').hide();
	  $.getJSON("qaDialog-ajax.jsp", 
			  { action: "get", target: "<%= targetVer %>", id: "<%= StringUtils.trimToEmpty(idStr) %>" },
			  function(result) {
				  cfg = result.dlgConfig;
				  $('#dlg-prompt-textarea').val(JSON.stringify(cfg));
				  $('#dlg-prompt div[name=msg]').text("CTRL+C 複製（會抓取已儲存的資料，修改到一半的不會被抓出）");
				  $('#dlg-prompt').dialog('open');
			  }
	  );
  });
  
  $(document).on('click', '#btnDlgQaPaste', function() {
	  cfg = JSON.parse($('#dlg-prompt-textarea').val());
	  $('#btnDlgQaPaste').hide();
	  
	  console.log(cfg);
	  $('#rootNode').children('.node-level-container').children('.node-row').remove();
	  renderNode(null, $('#rootNode'), cfg.children);

	  updateViewOfWholeTree($('#rootNode'), null, false);
	  redrawConnectLines();
  });

  $(document).on('click', '.dlg-qa-paste-json', function() {
	  $('#dlg-prompt-textarea').val("");
	  $('#dlg-prompt div[name=msg]').text("把設定 paste 上來（預設節點需自行設定）");
	  $('#btnDlgQaPaste').show();
	  $('#dlg-prompt').dialog('open');
  });
  
  $(document).on('click', '.dlg-toggle-quick-menu', function() {
	  $this = $(this);
	  classie.toggle( document.body, 'cbp-spmenu-push' );
	  classie.toggle( document.body, 'cbp-spmenu-push-toright-big' );
	  classie.toggle( $('#cbp-spmenu-s1')[0], 'cbp-spmenu-open' );
	  
	  // 有則顯示 dialog log
	  showDialogLog = $('body').hasClass('cbp-spmenu-push-toright-big');
	  
	  if (!$('#side-test-panel').hasClass('cbp-spmenu-open')) {
		  classie.toggle( $('#side-test-panel')[0], 'cbp-spmenu-open' );
		  $('#side-panel-question').focus();
	  }
  });
  
  $(document).on('click', '.dlg-toggle-debug-panel', function() {
	  $this = $(this);
	  classie.toggle( $('#side-test-panel')[0], 'cbp-spmenu-open' );
	  $('#side-panel-question').focus();
	  
	  return false;
  });
  
  $('#dlg-qa-show-node-detail').change(function() {
	  isEnable = $(this).prop('checked');
	  
	  if (isEnable) {
		  templateConfig.showDetail = true;
		  console.log("Toggle detail mode");
	  }
	  else {
		  templateConfig.showDetail = false;
		  console.log("Toggle no detail mode");
	  }
	  
	  updateViewOfWholeTree($('#rootNode'), null, true);
	  redrawConnectLines();
  });
  
  $('#dlg-qa-compact-mode').change(function() {
	  isEnable = $(this).prop('checked');
	  
	  if (isEnable) {
		  templateConfig.name = '#node-box-template-compact';
		  console.log("Toggle compact mode");
	  }
	  else {
		  templateConfig.name = '#node-box-template-standard';
		  console.log("Toggle standard mode");
	  }
	  
	  updateViewOfWholeTree($('#rootNode'), null, true);
	  redrawConnectLines();
  });

  <%--
   新建節點主要就是根據 timestamp 生成 unique id（這個 id 似乎隨著 SmartRobot 功能演進，必需要跨不同的問答之間也必需 unique，
   但目前用 timestamp 的方式其實並沒有辦法保證是 unique。因此這裡會有潛在造成問題的可能
  --%>
  $(document).on('click', '.dlg-qa-add-child', function(){
	parentId = $(this).attr('data-id');
	var id = new Date().getTime();
	while ($('#dlg_qa_' + id).length > 0) {
		id = new Date().getTime(); // 現場就 collision 就一直 loop 到沒有為止
	}
	 
	data = {
		id: id,
		text:  "<bean:message key='scenario.new.node'/>" + id,
	};

	// 這裡要一個所謂 input name for relative to parent，正常編碼是 dlg_qa_XXXXXX 。不過如果是根節點的話，這裡要 dlg_qa
	var relToParent = parentId?'dlg_qa_'+parentId:'dlg_qa';
	node = createNodeBox(relToParent, data);
	newLvContainer = $('<div rel="dlg_qa_' + id + '" class="node-level-container" style="margin-left: 250px;"><div class="node-level-container-vertical-line"></div></div>');
	
	newRow = $('<div class="node-row"></div>');
	node.appendTo(newRow);
	newLvContainer.appendTo(newRow);
	
	$(this).closest('.node-row').children('.node-level-container').append(newRow);
	redrawConnectLines();
	
	//var qa = JSON.parse($("#dlg-qa-" + dataId).val());
	 
	/*
	var inputName = 'dlg_qa_' + parentId;
	$('<ul>').append(createQAInput(inputName))
	 	.appendTo($(this).closest('li'))
	 	.find('.dlg-qa-search').focus();
	$(this).hide();
	*/
  });

  $("#dlg-qa-dialog").dialog({
	minWidth: 860,
	minHeight: 600,
	stack: true,
	autoOpen: false
  });

  $("#dlg-prompt").dialog({
	minWidth: 800,
	minHeight: 500,
	stack: true,
	autoOpen: false
  });

  $("#dlg-help").dialog({
	minWidth: 800,
	minHeight: 500,
	stack: true,
	autoOpen: false
  });

  <%-- 
  t = 節點標題
  c = 節點類型
  s = Groovy script（run just before do OptionAction)
  x = 額外參數 (for JSONEditor)
  --%>
	$(document).on('click', '.dlg-qa-edit', function(){
		dataId = $(this).attr('data-id');
		$('#dlg-qa-id').text('# ' + dataId);
		var qa = JSON.parse($('#dlg-qa-' + dataId).val());
		//console.log("Show Edit Dialog of " + dataId);
		//console.log(qa);
		$('.btn-dlg-qa-save').data('editId', qa.id);
		var t = $.trim(qa.text);
		$('#dlg-qa-t').val(t?t:'');
		
		var c = qa.nodeType;
		$('#dlg-qa-c').val(c?c:'');
		
		if (!qa.hasOwnProperty("nodeEnable") || qa.nodeEnable)
			$('#dlg-qa-node-enable').bootstrapToggle('on');
		else
			$('#dlg-qa-node-enable').bootstrapToggle('off');

		if (qa.hasOwnProperty("globalPrepend") && qa.globalPrepend)
			$('#dlg-qa-global-prepend').bootstrapToggle('on');
		else
			$('#dlg-qa-global-prepend').bootstrapToggle('off');
		
		if (qa.hasOwnProperty("globalAppend") && qa.globalAppend)
			$('#dlg-qa-global-append').bootstrapToggle('on');
		else
			$('#dlg-qa-global-append').bootstrapToggle('off');
		
		if (qa.hasOwnProperty("protectMode") && qa.protectMode)
			$('#dlg-qa-protectMode').bootstrapToggle('on');
		else
			$('#dlg-qa-protectMode').bootstrapToggle('off');
		
		if (qa.hasOwnProperty("specialCheckMode") && qa.specialCheckMode)
			$('#dlg-qa-cpsp').bootstrapToggle('on');
		else
			$('#dlg-qa-cpsp').bootstrapToggle('off');
		
		if (qa.hasOwnProperty("globalNodePerceptionCheckPoint"))
			$('#dlg-qa-cp').val(qa.globalNodePerceptionCheckPoint);
		else 
			$('#dlg-qa-cp').val("GOT_INPUT");
		
		var q = $.trim(qa.question);
		$('#dlg-qa-q').val(q?q:'');
		var a = $.trim(qa.answer);
		$('#dlg-qa-a').val(a?a:'');
		var s = $.trim(qa.script);
		$('#dlg-qa-s').val(s?s:'');
		var e = $.trim(qa.expiry);
		$('#dlg-qa-e').val(e?e:'');
		var v = $.trim(qa.menuView);
		$('#dlg-qa-v').val(v?v:'');
		var x = qa.hasOwnProperty('extraParams') ? qa.extraParams : {};
		$('#dlg-qa-x').val(JSON.stringify(x));
		if (typeof(qaExtraParamEditor) == 'undefined') {
			qaExtraParamEditor = new JSONEditor(document.getElementById('dlg-qa-x_editor_holder'),{
			        // Enable fetching schemas via ajax
			        ajax: true,
			        theme: 'bootstrap3',
			        // The schema for the editor
			        schema: { $ref: "get-jsonschema-ajax.jsp?type=qa-h-extra", },
	
			        // Seed the form with a starting value
			        startval: x
			      });
		    qaExtraParamEditor.on('ready', function() {
		    	  	qaExtraParamEditor.on('change',function() {
			    		$('#dlg-qa-x').val(JSON.stringify(qaExtraParamEditor.getValue()));
			    });
			});
		}
		else {
			qaExtraParamEditor.setValue(x);
		}
		var tc = qa.hasOwnProperty('perceptions') ? qa.perceptions : [];
		$('#dlg-qa-perceptions-config').val(JSON.stringify(tc));
		if (typeof(qaPerceptionsEditor) == 'undefined') {
			qaPerceptionsEditor = new JSONEditor(document.getElementById('dlg-qa-perceptions-config_editor_holder'),{
			        // Enable fetching schemas via ajax
			        ajax: true,
			        theme: 'bootstrap3',
			        // The schema for the editor
			        schema: { $ref: "get-jsonschema-ajax.jsp?type=qa-dlg-perceptions", },
			        // Seed the form with a starting value
			        startval: tc
			      });
			qaPerceptionsEditor.on('ready', function() {
				qaPerceptionsEditor.on('change',function() {
			    		$('#dlg-qa-perceptions-config').val(JSON.stringify(qaPerceptionsEditor.getValue()));
			    });
			});
		}
		else {
			qaPerceptionsEditor.setValue(tc);
		}
		console.log(qaPerceptionsEditor);
		var reactionsArr = qa.hasOwnProperty('reactions') ? qa.reactions : [];
		$('#dlg-qa-reactions-config').val(JSON.stringify(reactionsArr));
		if (typeof(qaReactionsEditor) == 'undefined') {
			qaReactionsEditor = new JSONEditor(document.getElementById('dlg-qa-reactions-config_editor_holder'),{
			        // Enable feactionsArrhing schemas via ajax
			        ajax: true,
			        theme: 'bootstrap3',
			        // The schema for the editor
			        schema: { $ref: "get-jsonschema-ajax.jsp?type=qa-dlg-reactions", },
	
			        // Seed the form with a starting value
			        startval: reactionsArr
			      });
			qaReactionsEditor.on('ready', function() {
				qaReactionsEditor.on('change',function() {
			    		$('#dlg-qa-reactions-config').val(JSON.stringify(qaReactionsEditor.getValue()));
			    });
			});
		}
		else {
			qaReactionsEditor.setValue(reactionsArr);
		}
		var i = $.trim(qa.inputType);
		$('#dlg-qa-i').val(i?i:'');
		var p = $.trim(qa.pipe);
		$('#dlg-qa-p').val(p?p:'');
		$("#dlg-qa-dialog").dialog('open');
	});

	$('#dlg-qa-e').datepicker();

	$('#dlg-qa-clear-expiry').click(function(){
		$('#dlg-qa-e').val('');
	});

	$('.btn-dlg-qa-save').click(function(){
		var id = $(this).data('editId');
		var input = $('#dlg-qa-' + id);
		var text = $.trim($("#dlg-qa-t").val());
		var question = $.trim($("#dlg-qa-q").val());
		var answer = $.trim($("#dlg-qa-a").val());
		var script = $.trim($("#dlg-qa-s").val());
		var nodeType = $.trim($("#dlg-qa-c").val());
		var globalNodePerceptionCheckPoint = $.trim($("#dlg-qa-cp").val());
		var menuView = $.trim($("#dlg-qa-v").val());
		var inputType = $.trim($("#dlg-qa-i").val());
		var expiry = $.trim($("#dlg-qa-e").val());
		var pipe = $.trim($("#dlg-qa-p").val());
		var extraParamsStr = $.trim($("#dlg-qa-x").val());
 		var perceptionsStr = $.trim($("#dlg-qa-perceptions-config").val());
		var reactionsStr = $.trim($("#dlg-qa-reactions-config").val());
		extraParams = extraParamsStr ? JSON.parse(extraParamsStr) : {};
		perceptions = perceptionsStr ? JSON.parse(perceptionsStr) : [];
		reactions = reactionsStr ? JSON.parse(reactionsStr) : [];
		
		
		var data = {
			  id: id,
			  text: text,
			  nodeType: nodeType,
			  nodeEnable: $('#dlg-qa-node-enable').prop("checked"),
			  globalPrepend: $('#dlg-qa-global-prepend').prop("checked"),
			  globalAppend: $('#dlg-qa-global-append').prop("checked"),
			  protectMode: $('#dlg-qa-protectMode').prop("checked"),
			  specialCheckMode: $('#dlg-qa-cpsp').prop("checked"),
			  globalNodePerceptionCheckPoint: globalNodePerceptionCheckPoint,
			  question: question,
			  answer: answer,
			  expiry: expiry,
			  inputType: inputType,
			  menuView: menuView,
			  extraParams: extraParams,
			  perceptions: perceptions,
			  reactions: reactions,
			  script: script,
		};

		if (pipe != '') {
		  data.pipe = pipe;
		}
		
		updateNodeView(id, data, null, false);

		input.attr('value', JSON.stringify(data));
		var expired = false;
		if(expiry){
		  	try{
				var expiryMillis = new Date(expiry).getTime();
			  	expired = new Date().getTime() >= expiryMillis;
			}catch(e){}
		}
		var textElem = input.prev().text(text);
		if(expired){
		  textElem.append('<span class="text-warning">（<bean:message key="global.expired"/>）</span>');
		}
		
	  
	  <%--
	  不再需要因為有「答案欄」就刪除子節點
	  
	  if(answer){ 
		  input.next()
		  	.remove();
	  }
	  //input.siblings('.dlg-qa-add-child')[answer?'hide':'show']();
	  --%>
		$("#dlg-qa-dialog").dialog('close');
	});

	$('.btn-dlg-qa-cancel').click(function() {
		$("#dlg-qa-dialog").dialog('close');
	});
	
	$('.btnSave').click(function() {
		$('#loading-panel').show();
		var mkey = $('input[name=mkey]').val();
		
		if (mkey == '') {
			alert('請務必輸入Mkey');
			$('#loading-panel').hide();
		}
		else {
			if ($('input[name="createMode"]').val() != 'false') {
				$('#curdAction').val('checkDupMkey');
				$.ajax({
					type: 'POST',
					dataType: 'json',
					url: 'qaDialog-ajax.jsp',
					data: $('#form').serialize(),
					success: function(result) {
						if (result.status == "Done") {
							comment = prompt('請輸入版本說明');
							
							if (comment == '') {
								$('.loading-panel').hide();
								return;
							}
							
							$('#versionComment').val(comment);
							$('#curdAction').val('saveAndPublish');
							$.ajax({
									type: 'POST',
									dataType: 'json',
									url: 'qaDialog-ajax.jsp',
									data: $('#form').serialize(),
									success: function(result) {
										if (result.status == "Done") {
											$('input[name="createMode"]').val("false");
											$('input[name="id"]').val(result.id);
										}

					    				setTimeout(function() {$('.loading-panel').hide();window.location = 'qaDialogList.jsp';}, 3000);
									}
							});
						}
						else if (result.status == 'DuplicateMkey') {
							alert(result.errorMsg);
						}
						else if (result.status == 'Error') {
							alert(result.errorMsg);
						}
					}
				});
			}
			else {
				comment = prompt('請輸入版本說明');
				
				if (comment == '') {
					$('.loading-panel').hide();
					return;
				}
				$('#versionComment').val(comment);
				$('#curdAction').val('saveAndPublish');
				$.ajax({
						type: 'POST',
						dataType: 'json',
						url: 'qaDialog-ajax.jsp',
						data: $('#form').serialize(),
						success: function(result) {
							if (result.status == "Done") {
								$('input[name="createMode"]').val("false");
								$('input[name="id"]').val(result.id);
			    				setTimeout(function() {$('.loading-panel').hide();window.location = 'qaDialogList.jsp';}, 3000);
							}
						}
				});
				//window.location = 'qaDialogList.jsp';
			}
		}
	});
	
	$('.btnSaveDraft').click(function() {
		$('#loading-panel').show();
		var mkey = $('input[name=mkey]').val();
		
		if (mkey == '') {
			alert('請務必輸入Mkey');
		}
		else {
			if ($('input[name="createMode"]').val() != 'false') {
				$('#curdAction').val('checkDupMkey');
				
				$.ajax({
					type: 'POST',
					dataType: 'json',
					url: 'qaDialog-ajax.jsp',
					data: $('#form').serialize(),
					success: function(result) {
						if (result.status == "Done") {
							$('#curdAction').val('saveDraft');
							
							$.ajax({
									type: 'POST',
									dataType: 'json',
									url: 'qaDialog-ajax.jsp',
									data: $('#form').serialize(),
									success: function(result) {
										if (result.status == "Done") {
											$('input[name="createMode"]').val("false");
											$('input[name="id"]').val(result.id);
											$('#loading-panel').hide();
										}
									}
							});
						}
						else if (result.status == 'DuplicateMkey') {
							alert(result.errorMsg);
						}
						else if (result.status == 'Error') {
							alert(result.errorMsg);
						}
					}
				});
			}
			else {
				$('#curdAction').val('saveDraft');
				
				$.ajax({
						type: 'POST',
						dataType: 'json',
						url: 'qaDialog-ajax.jsp',
						data: $('#form').serialize(),
						success: function(result) {
							if (result.status == "Done") {
								$('input[name="createMode"]').val("false");
								$('input[name="id"]').val(result.id);
								$('#loading-panel').hide();
							}
						}
				});
			}
		}
	});

	function createQAInput(name){
		var input = $('<input type="text" class="form-control dlg-qa-search">');
		input.keypress(function (e) {
		 if (e.which == 13) {
			var val = $.trim(input.val());
			if(val){
				var id = new Date().getTime();
				var elem = $(
						'<li>'
		  				+ '<button type="button" class="dlg-qa-delete glyphicon glyphicon-remove btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="delete"/>"></button>'
		  				+ '<button type="button" class="dlg-qa-edit glyphicon glyphicon-pencil btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="edit.qa"/>"></button>'
		  				+ '<button type="button" class="dlg-qa-add-child glyphicon glyphicon-plus btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="add.lower.layer"/>"></button>'
		  				+ '<span>' + $('<div>').text(val).html() + '</span>'
		  			+ '</li>'
				);
				elem.append(
					$('<input type="hidden" id="dlg-qa-' + id + '" name="' + name + '">')
						.attr('value', JSON.stringify({id: id, text: val}))
				);
				input.before(elem);
			}
			input.val('');
			return false;
	     }
		})
		.autocomplete({
		source: function(req, callback){
	    	if($.trim(req.term)){
		    	$.getJSON('<%= request.getContextPath() %>/wiseadm/qa-plugin/qa-search.jsp',
					{
		    			id: curDocId,
		    			term: req.term
					},
		    		callback
		    	);
	    	}else{
	    		callback([]);
	    	}
	    },
	    minLength: 2,
	    select: function( event, ui ) {
	    	var item = ui.item;
	    	var pipe2id = item.id;
	    	if($('[name=' + name + '][value=' + pipe2id + ']:hidden').size() == 0){
				var id = new Date().getTime();
				var elem = $(
						'<li>'
		  				+ '<button type="button" class="dlg-qa-delete glyphicon glyphicon-remove btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="delete"/>"></button>'
		  				+ '<button type="button" class="dlg-qa-edit glyphicon glyphicon-pencil btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="edit.qa"/>"></button>'
		  				+ '<a target="_blank" href="qaDataEditor.jsp?id=' + (pipe2id ? pipe2id : id) + '">'
						+ $('<div>').text(item.value).html()
	  				+ '</a>'
		  			+ '</li>'
				);
				elem.append(
					$('<input type="hidden" id="dlg-qa-' + id + '" name="' + name + '">')
						.attr('value', JSON.stringify({id: id, text: item.value, question:item.value, pipe: pipe2id}))
				);
				input.before(elem);
	    	}
	    	input.val('');
	    	return false;
	    }
	  });
	  return input;
	}
});
</script>
<style type="text/css">
#dlg-qa-panel li {
	margin-bottom: 0.5em;
}
#dlg-qa-panel ul {
	padding-top: 0.5em;
}
.dlg-qa-delete, .dlg-qa-add-child, .dlg-qa-paste, .dlg-qa-edit, .dlg-qa-dropdown, .dlg-qa-paste-json, .dlg-qa-copy-json {
	padding: 1px 5px 1px 5px;
}
#dlg-qa-dialog .row {
	margin-top: 0.5em;
}
</style>

</body>
</html>
