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
	 import="com.intumit.solr.robot.qarule.*"
	 import="com.intumit.solr.robot.QAChannel"
	 import="com.intumit.systemconfig.*"
	 import="org.apache.commons.lang.StringUtils"
	 import="com.intumit.quartz.ScheduleUtils"
	 import="com.intumit.quartz.Job"
     import="org.dom4j.*"
	 import="org.apache.commons.httpclient.methods.GetMethod"
	 import="org.apache.commons.httpclient.HttpClient"
	 import="org.apache.commons.httpclient.auth.AuthScope"
	 import="org.apache.commons.httpclient.UsernamePasswordCredentials"
	 import="org.apache.solr.client.solrj.SolrQuery"
	import="com.intumit.solr.admin.*"
%><%!
SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>QAMatch Flow Config</TITLE>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/wiseadm/css/dropzone.css" type="text/css" rel="stylesheet" />
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-slider.css" rel="stylesheet">
<!--[if lt IE 9]>
  <script src="<%=request.getContextPath()%>/script/html5shiv.js"></script>
  <script src="<%=request.getContextPath()%>/script/respond.min.js"></script>
<![endif]-->
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery-ui-1.9.2.custom.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.aciPlugin.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.aciSortable.min.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.iframe-transport.js"></script>
<script src="<%=request.getContextPath()%>/wiseadm/js/jquery.fileupload.js"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap-toggle.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap-slider.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jsoneditor.js"></script>
<style>
td-form {
	width: 50%;
}

td-form input,textarea {
	width: 100%;
}

ul {
    border:3px dotted Gray;
    padding: 10px;
    height:300px;
    display:inline-block;
    vertical-align:top;
}
li {
    background-color:Azure;
    border:1px solid Black;
    margin: 2px;
    padding: 2px;
}
li.selected {
    background-color:GoldenRod
}

.ui-widget input, .ui-widget select, .ui-widget textarea, .ui-widget button {
	font-size: 0.6em;
}
</style>
<script>
JSONEditor.defaults.theme = 'bootstrap3';
var pairs = ['pair1', 'pair2', 'pair3'];
var pairNames = ['PreQAMatchRule', 'QAMatchingRule', 'PostQAMatchRule'];
var ruleConfigJsonEditor;
var schemaMap = {};
</script>
</HEAD>
<BODY>
<%
WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();
String action = StringUtils.defaultString(request.getParameter("action"), "edit");
String target = StringUtils.defaultString(request.getParameter("target"), "tenant");
String idStr = request.getParameter("id");

String header = null;


if ("tenant".equals(target)) {
	Tenant tenant = idStr == null ? new Tenant() : Tenant.get(Integer.parseInt(idStr));
	header = "Match Flow of Tenant: " + tenant.getName();
}
else if ("channel".equals(target)) {
	QAChannel ch = QAChannel.get(Integer.parseInt(idStr));
	header = "Match Flow of Channel: " + ch.getName();
}

out.println("<h2>" + header + "</h2>");

if ("create".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action) || "duplicate".equalsIgnoreCase(action)) {
	%>
<div class="row">
	<div class="col-md-8 col-sm-10 col-xs-12" >
		<div class="col-xs-12" >
		<H1>PreQAMatchRule</H1>
		</div>
		<div class="col-xs-6" >
		<h4>Available</h4>
		<ul class="list-unstyled pull-right col-xs-12 pair1 pair-unselect">
		</ul>
		</div>
		<div class="col-xs-6">
		<h4>Enabled</h4>
		<ul class="list-unstyled col-xs-12 pair1 pair-selected">
		</ul>
		</div>
	</div>
</div>
<div class="row">
	<div class="col-md-8 col-sm-10 col-xs-12" >
		<div class="col-xs-12" >
		<H1>QAMatchingRule</H1>
		</div>
		<div class="col-xs-6" >
		<h4>Available</h4>
		<ul class="list-unstyled pull-right col-xs-12 pair2 pair-unselect">
		</ul>
		</div>
		<div class="col-xs-6">
		<h4>Enabled</h4>
		<ul class="list-unstyled col-xs-12 pair2 pair-selected">
		</ul>
		</div>
	</div>
</div>
<div class="row">
	<div class="col-md-8 col-sm-10 col-xs-12" >
		<div class="col-xs-12" >
		<H1>PostQAMatchRule</H1>
		</div>
		<div class="col-xs-6" >
		<h4>Available</h4>
		<ul class="list-unstyled pull-right col-xs-12 pair3 pair-unselect">
		</ul>
		</div>
		<div class="col-xs-6">
		<h4>Enabled</h4>
		<ul class="list-unstyled col-xs-12 pair3 pair-selected">
		</ul>
		</div>
	</div>
</div>
<div id="ruleConfigDialog">
	<div id="ruleConfigJsonEditorDiv"></div>
</div>
<%--
<li data-rule-name="<%= r %>"><%= r %><span class="glyphicon glyphicon-cog pull-right" aria-hidden="true"></span></li>
--%>
<script>
$(document).ready(function() {
	$.ajax({
		"url": "matchCtrlFlowAdmin-ajax.jsp",
		"data": { id: "<%= idStr %>", target: "<%= target %>", action: "loadConfig" },
		"success": function(resp) {
			srbtConfig = resp.srbtConfig;
			tenantFlowConfig = resp.tenantQaMatchFlowConfig;

			for (var i in pairs) {
				p = pairs[i];
				pn = pairNames[i];

				arrOfAllRules = srbtConfig[pn];
				tenantFlowRules = tenantFlowConfig[pn];
				usedRules = $.map(tenantFlowRules, function(rule, idx) { return rule.name; });

				for (var j=0; j < arrOfAllRules.length; j++) {
					rule = arrOfAllRules[j];

					if (rule.hasOwnProperty("schema")) {
						schemaMap[rule.name] = rule.schema;
					}

					if ($.inArray(rule.name, usedRules) != -1) continue;

					$li = $('<li/>');
					$li.html(rule.name + '<a href="#" class="btnRuleConfig"><span class="glyphicon glyphicon-cog pull-right" style="display:none;" aria-hidden="true"></span></a>');
					$li.attr('data-rule-name', rule.name);

					if (rule.hasOwnProperty("config")) {
						$.data($li[0], 'config', rule.config);
					}
					else {
						$.data($li[0], 'config', {});
					}

					$('ul.pair-unselect.' + p).append($li);
				}

				for (var j=0; j < tenantFlowRules.length; j++) {
					rule = tenantFlowRules[j];

					$li = $('<li/>');
					$li.html(rule.name + '<a href="#" class="btnRuleConfig"><span class="glyphicon glyphicon-cog pull-right" aria-hidden="true"></span></a>');
					$li.attr('data-rule-name', rule.name);

					if (rule.hasOwnProperty("config")) {
						$.data($li[0], 'config', rule.config);
					}
					else {
						$.data($li[0], 'config', {});
					}

					$('ul.pair-selected.' + p).append($li);
				}
			}
		}
	});
});
</script>
	<%
}
%>
<button class="btn btn-primary btnSave"><bean:message key='save'/></button>
<script>

$('.btnSave').click(function() {
	var config = {};

	for (var i in pairs) {
		p = pairs[i];
		pn = pairNames[i];

		config[pn] = [];
		$('ul.pair-selected.' + p + ' > li').each(function() {
			rule = {};
			rule['name'] = $(this).attr('data-rule-name');
			ruleCfg = $.data($(this)[0], 'config')

			if (typeof(ruleCfg) != 'undefined') {
				rule['config'] = ruleCfg;
			}
			config[pn].push(rule);
		});
	}

	$.ajax({
		url: "matchCtrlFlowAdmin-ajax.jsp",
		method: "POST",
		dataType: "JSON",
		data: {
			id: "<%= idStr %>", 
			target: "<%= target %>",
			action: "save",
			data: JSON.stringify(config)
		},
		success: function(result) {
			alert(result.status);
		}
	});
});

for (var p in pairs) {
	$("ul." + pairs[p]).on('click', 'li', function (e) {
	    if (e.ctrlKey || e.metaKey) {
	        $(this).toggleClass("selected");
	    } else {
	        $(this).addClass("selected").siblings().removeClass('selected');
	    }
	}).sortable({
	    connectWith: "ul." + pairs[p],
	    delay: 150, //Needed to prevent accidental drag when trying to select
	    revert: 0,
	    helper: function (e, item) {
	        var helper = $('<li/>');
	        if (!item.hasClass('selected')) {
	            item.addClass('selected').siblings().removeClass('selected');
	        }
	        var elements = item.parent().children('.selected').clone();
	        item.data('multidrag', elements).siblings('.selected').remove();
	        return helper.append(elements);
	    },
	    stop: function (e, info) {
	        info.item.after(info.item.data('multidrag')).remove();
	        $('.pair-unselect li span').hide();
	        $('.pair-selected li span').show();
	    }

	});
}

$(document).on('click', '.btnRuleConfig', function() {
	$this = $(this);
	$li = $this.parent();
	var $dialog = $('#ruleConfigDialog').dialog({
	                   autoOpen: false,
	                   modal: false,
	                   height: 625,
	                   width: 700,
	                   title: "Rule Config"
	               });

	if (typeof(ruleConfigJsonEditor) != 'undefined') {
		ruleConfigJsonEditor.destroy();
	}

	var ruleName = $li.attr('data-rule-name');
	var starting_value = $.data($li[0], 'config');
	var schema = {};

	if (schemaMap.hasOwnProperty(ruleName)) {
		schema = schemaMap[ruleName];
	}

	if (typeof(starting_value) == 'undefined') {
		starting_value = {};
	}

    // Initialize the editor
    ruleConfigJsonEditor = new JSONEditor(document.getElementById('ruleConfigJsonEditorDiv'),{
      // Enable fetching schemas via ajax
      ajax: true,
      disable_collapse: true,
      // The schema for the editor
      schema: schema,
      // Seed the form with a starting value
      startval: starting_value
    });

    ruleConfigJsonEditor.on('change',function() {
        $.data($li[0], 'config', ruleConfigJsonEditor.getValue());
      });
	$dialog.dialog('open');
});
</script>
</BODY>
</HTML>
