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
	import="java.io.UnsupportedEncodingException"
	import="java.net.*"
	import="java.util.*"
	import="com.intumit.message.MessageUtil"
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
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp?r=<%= StringEscapeUtils.escapeJavaScript(URLEncoder.encode(url, "UTf-8")) %>';
	</script>
	<%
	return;
}
%>
<%!

/*
 * 也許之後可以全部交給 client 端的 javascript，後面有個 javascript 的 createNode 做的是一樣的事情
 */
String renderNodeBox(String name, JSONObject json, Locale locale, boolean isFirst) {
	StringBuffer buf = new StringBuffer();
	try {
		String id = json.optString("id");
		String text = json.optString("text");
		String expiry = json.optString("expiry");
		String pipe = json.optString("pipe");
		JSONArray children = json.optJSONArray("children");
		boolean hasAnswer = StringUtils.isNotBlank(json.optString("answer"));
		boolean showAddChildBtn = true;//children.length() == 0 && !hasAnswer;
		JSONObject val = new JSONObject(json, HierarchicalQA.JSON_PROPS);
		
		buf.append("<div class='node-box' id='h-node-"+id+"' data-id='"+id+"'>\n");
		buf.append("<div class='node-box-connect'></div>\n");
		
		buf.append("<div class='node-box-collapse-btn' data-id='" + id + "'><span class='glyphicon glyphicon-minus'></span></div>\n");
		buf.append("<div class='node-box-expand-btn' style='display: none;' data-id='" + id + "'><span class='glyphicon glyphicon-plus'></span></div>\n");
		
		buf.append("<div class='panel box-shadow--4dp'>\n"); 
		buf.append("	<div class='btn-group btn-xs pull-right' style='margin-top: -15px;'>\n");
		buf.append("  <button type=\"button\" data-id='" + id + "' class=\"h-qa-delete glyphicon glyphicon-remove btn btn-primary \" title=\"" + MessageUtil.getMessage(locale, "delete") + "\"></button>");
		buf.append("  <button type=\"button\" data-id='" + id + "' class=\"h-qa-edit glyphicon glyphicon-pencil btn btn-primary \" title=\"" + MessageUtil.getMessage(locale, "edit.qa") + "\"></button>");
		buf.append("  <button type=\"button\" data-id='" + id + "' class=\"h-qa-add-child glyphicon glyphicon-plus btn btn-primary \""); buf.append(" " + (showAddChildBtn?"":"display:none;") + "\" title=\"" + MessageUtil.getMessage(locale, "add.lower.layer") + "\"></button>");
		buf.append("  <button type='button' class='h-qa-dropdown glyphicon glyphicon-play btn btn-primary dropdown-toggle' id='dropdownMenu" + id + "' data-toggle='dropdown' aria-haspopup='true' aria-expanded='true'></button>");
		buf.append("  <ul class='dropdown-menu' aria-labelledby='dropdownMenu" + id + "'>");
		buf.append("    <li><a class='h-qa-cut' data-id='" + id + "'>剪下</a></li>");
		buf.append("    <li><a class='h-qa-copy' data-id='" + id + "'>複製</a></li>");
		buf.append("    <li><a class='h-qa-paste' data-id='" + id + "'>貼上</a></li>");
		buf.append("    <li role='separator' class='divider'></li>");
		buf.append("    <li><a class='h-qa-move-up' data-id='" + id + "'>上移</a></li>");
		buf.append("    <li><a class='h-qa-move-down' data-id='" + id + "'>下移</a></li>");
		buf.append("    <li role='separator' class='divider'></li>");
		buf.append("    <li><a class='h-qa-set-as-entrypoint' data-id='" + id + "'>預設節點</a></li>");
		buf.append("  </ul>");

		buf.append("	</div>\n");
		
		buf.append("	<div class='panel-heading'>\n");
		buf.append("	  <h3 class='panel-title'>&nbsp;");
		buf.append("  </h3>\n");
		buf.append("	</div>\n");
		buf.append("	<div class='panel-body'>&nbsp;\n");
		buf.append("	</div>\n");
		buf.append("	<div class='panel-footer hide'>");
		buf.append("		<input type=\"hidden\" id=\"h-qa-" + id + "\" name=\"" + name + "\" value=\"" + StringEscapeUtils.escapeHtml(val.toString()) + "\" />");
		buf.append("</div>\n");
		buf.append("  </div>\n");
		buf.append("</div>\n");
	}
	catch (Exception ignoreIt) {}
	
	return buf.toString();
}


/**
 * 正確結構應該如下，只有根節點的 node-row 當中沒有 node-box，只有一個 h1 的 icon 以及一個增加子節點的 button
 +-- node-row (*)
 >> +-- node-box#h-box-{id}
 >>>> +-- ... panel ...
 >>>>>>> +-- ... buttons ...
 >>>>>>> +-- ... contents ...
 >>>>>>> +-- ... input#h_qa_{id} [name="h_qa_{parentId}"] 
		         // 這個 input 是重點，他的 value 會放這個節點的 json string
		         // submit 出去後 Server 端實際上都是靠這個 input 來判斷整個 dialog 結構
 >> +-- div.node-level-container [rel="#h_qa_{id}"]
 >>>> +-- div.node-level-container-vertial-line
 >>>> +-- (repeat next level of node-row (*) )
 
 */
void renderQAInputUI(String relName, JSONArray data, Map<String, String> hIdQaMap, Writer out, Locale locale, int lv)
		throws Exception {
	out.write("<div rel=\"" + relName + "\"  class='node-level-container' " + (lv == 0 ? "style='margin-left: 20px;'" : "style='margin-left: 250px;'") + ">");
	out.write("<div class='node-level-container-vertical-line'></div>\n");
	
	if (data != null && data.length() > 0) {
		for(int i=0; i<data.length(); i++){
			out.write("<div class='node-row'>");
			
			Object obj = data.opt(i);
			JSONObject json = (JSONObject)obj;
			String id = json.optString("id");
			String text = json.optString("text");
			String expiry = json.optString("expiry");
			JSONArray children = json.optJSONArray("children");
			boolean hasAnswer = StringUtils.isNotBlank(json.optString("answer"));
			boolean showAddChildBtn = true;//children.length() == 0 && !hasAnswer;
			JSONObject val = new JSONObject(json, HierarchicalQA.JSON_PROPS);

			String html = "";
			html += renderNodeBox(relName, json, locale, i==0);
			out.write(html);
			renderQAInputUI("h_qa_" + id, children, hIdQaMap, out, locale, lv+1);
			out.write("</div>");
		}
	}
	
	out.write("</div>");
}

static Map<String, String> getIdQaMap(com.intumit.solr.tenant.Tenant tenant, Set<String> hQaIds) {
	Map<String, String> rs = new HashMap<String, String>();
	if(CollectionUtils.isNotEmpty(hQaIds)){
		String query = "";
		for(String id : hQaIds){
			try {
				query += (StringUtils.isEmpty(query)?"":" OR ") + "id:\"" 
					+ URLEncoder.encode(id, "UTF-8") + "\"";
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		SolrQuery q = new SolrQuery();
		q.setQuery(query);
		q.addFilterQuery("-isPart_i:[2 TO *]");
		q.addFilterQuery("dataType_s:" + QAUtil.DATATYPE_COMMON_SENSE);
		q.setRows(hQaIds.size());
		
		SolrServer server = tenant.getCoreServer();
		try {
			SolrDocumentList docs = server.query(q).getResults();
			for(SolrDocument doc: docs){
				String id = (String) doc.getFieldValue("id");
				String ques = (String) doc.getFieldValue("QUESTION_s");
				rs.put(id, ques);
			}
		} catch (SolrServerException e) {
			e.printStackTrace();
		}
	}
	return rs;
}

static Set<String> getQaIds(JSONArray data){
	Set<String> ids = new HashSet<String>();
	try {
		if(data != null){
			for(int i=0; i<data.length(); i++){
				Object obj = data.opt(i);
				if(obj instanceof String){
					ids.add((String)obj);
				}else if(obj instanceof JSONObject){
					JSONObject json = (JSONObject)obj;
					if (json.has("pipe")) {
						ids.add(json.optString("pipe"));
					}
					ids.addAll(getQaIds((json).optJSONArray("children")));
				}
			}
		}
	}
	catch (JSONException e) {
		e.printStackTrace();
	}
	return ids;
}

static JSONArray removeOrphanQa(JSONArray data, Map<String, String> hIdQaMap) 
		throws JSONException {
	JSONArray newData = new JSONArray();
	if(data != null){
		for(int i=0; i<data.length(); i++){
			Object obj = data.opt(i);
			if(obj instanceof String){
				String id = (String)obj;
				if(hIdQaMap.containsKey(id)){
					newData.put(id);
				}
			}else if(obj instanceof JSONObject){
				JSONObject json = (JSONObject) obj;
				JSONObject newJson = new JSONObject(json, HierarchicalQA.JSON_PROPS);
				newJson.put("children", removeOrphanQa(
					json.optJSONArray("children"), hIdQaMap));
				newData.put(newJson);
			}
		}
	}
	return newData;
}

%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String id = StringUtils.trimToNull(request.getParameter("id"));

QAUtil qu = QAUtil.getInstance(t);

SolrDocument doc = qu.getMainQASolrDocument(qu.id2Kid(id));

JSONObject data = HierarchicalQA.getDataFromDocument(doc);
JSONArray _1stLvChildren = data.getJSONArray("children");
String entryPoint = StringUtils.trimToNull(data.optString("entryPoint"));
boolean lockFirstLvChildren = false;

if (_1stLvChildren == null || (_1stLvChildren != null && _1stLvChildren.length() == 0)) {
	JSONObject newEP = new JSONObject();
	String newEpId = "" + System.currentTimeMillis();
	entryPoint = newEpId;
	newEP.put("id", newEpId);
	newEP.put("text", "進入點 (預設進入節點)");
	newEP.put("children", new JSONArray());
	_1stLvChildren = new JSONArray();
	_1stLvChildren.add(newEP);
	data.put("children", _1stLvChildren);
	data.put("entryPoint", entryPoint);
	lockFirstLvChildren = true;
}

Map<String, String> hIdQaMap = getIdQaMap(t, getQaIds(_1stLvChildren));
try {
	_1stLvChildren = removeOrphanQa(_1stLvChildren, hIdQaMap);
} catch (JSONException e) {
	e.printStackTrace();
}
System.out.println(hIdQaMap);
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<TITLE><bean:message key='qa.data.manger'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>

<link rel="stylesheet" href="<%= request.getContextPath() %>/wiseadm/css/codemirror.css">
<script src="<%= request.getContextPath() %>/wiseadm/js/codemirror.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/groovy.js"></script>

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

<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:9999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}

.box-shadow--2dp {
    box-shadow: 0 2px 2px 0 rgba(0, 0, 0, .14), 0 3px 1px -2px rgba(0, 0, 0, .2), 0 1px 5px 0 rgba(0, 0, 0, .12)
}
.box-shadow--3dp {
    box-shadow: 0 3px 4px 0 rgba(0, 0, 0, .14), 0 3px 3px -2px rgba(0, 0, 0, .2), 0 1px 8px 0 rgba(0, 0, 0, .12)
}
.box-shadow--4dp {
    box-shadow: 0 4px 5px 0 rgba(0, 0, 0, .14), 0 1px 10px 0 rgba(0, 0, 0, .12), 0 2px 4px -1px rgba(0, 0, 0, .2)
}
.box-shadow--6dp {
    box-shadow: 0 6px 10px 0 rgba(0, 0, 0, .14), 0 1px 18px 0 rgba(0, 0, 0, .12), 0 3px 5px -1px rgba(0, 0, 0, .2)
}

.node-row {
	display: table;
}

.node-box {
	width: 250px;
	min-height: 100px;
	padding-left: 15px;
	float: left;
    position:relative;
}

.node-level-container {
    position:relative;
}

.node-box-connect {
    width: 36px;
    height: 47px;
    border-top: 2px solid black;
    position: absolute;
    top: 40px;
    left: 7px;
    z-index: -999;
}

.node-box-connect-first {
    width: 66px;
    height: 47px;
    border-top: 2px solid black;
    position: absolute;
    top: 40px;
    left: 0px;
    z-index: -999;
}

.node-level-container-vertical-line {
    width: 36px;
    height: 100%;
    border-left: 2px solid black;
    position: absolute;
    top: 40px;
    left: 7px;
    z-index: -999;
}

/*.node-level-container:after {
  content: '';
  width: 0;
  height: 90%;
  position: absolute;
  border: 1px solid rgba(200,200,200,0.6);
  top: 40px;
  left: -8px;
}*/

.node-box-collapse-btn {
    width: 160px;
    height: 30px;
    position: absolute;
    top: 0px;
    left: 18px;
    color: white;
}

.node-box-expand-btn {
    width: 160px;
    height: 30px;
    position: absolute;
    top: 0px;
    left: 18px;
    color: white;
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

.ui-dialog { z-index: 1000 !important ;}

</style>
</head>
<body>
<jsp:include page="navbar-qa.jsp"><jsp:param name="hideUI" value="false" /></jsp:include>

<ul class="nav nav-tabs" id="myTabs" role="tablist" style="margin: 10px 0 0 10px;"> 
	<li role="presentation" class="active">
		<a href="#home" id="home-tab" role="tab" data-toggle="tab" aria-controls="home" aria-expanded="true">儀表板</a>
	</li> 
	<li role="presentation" class="">
		<a href="#dialog" id="dialog-tab" role="tab" data-toggle="tab" aria-controls="dialog" aria-expanded="false">對話流程</a>
	</li> 
	<%-- >li role="presentation" class="dropdown"> 
		<a href="#" class="dropdown-toggle" id="myTabDrop1" data-toggle="dropdown" aria-controls="myTabDrop1-contents">其他 <span class="caret"></span></a> 
		<ul class="dropdown-menu" aria-labelledby="myTabDrop1" id="myTabDrop1-contents"> 
			<li><a href="#dropdown1" role="tab" id="dropdown1-tab" data-toggle="tab" aria-controls="dropdown1">@fat</a></li> 
			<li><a href="#dropdown2" role="tab" id="dropdown2-tab" data-toggle="tab" aria-controls="dropdown2">@mdo</a></li> 
		</ul> 
	</li --%> 
</ul>


<form id="form">
<div class="tab-content" id="myTabContent"> 
<div class="tab-pane fade active in" role="tabpanel" id="home" aria-labelledby="home-tab">
	
	<div class="row">
	<div class="col-md-12" style="margin: 15px 0 0 15px; padding-right: 45px;">
	  <div class="panel panel-primary">
		<div class="panel-heading">
		  <h3 class="panel-title">基本設定</h3>
		</div>
		<div class="panel-body">
				<div class="input-group">
				<input type='hidden' name='entryPoint' value='<%= StringUtils.trimToEmpty(entryPoint) %>'>
	
				  <span class="input-group-addon" id="basic-addon1">名稱</span>
				  <input type="hidden" name="id" value="<%= id %>">
				  <input type="text" class="form-control" placeholder="對話情境名稱" aria-describedby="basic-addon1">
				</div>
				
				<div class="input-group">
				  <span class="input-group-addon" id="basic-addon1">說明</span>
				  <textarea class="form-control" placeholder="對話情境說明" rows="10" aria-describedby="basic-addon2"></textarea>
				</div>
				
				<br>
				<a href="#" class="btn btn-success" role="button">儲存</a> 
				<a href="#" class="btn btn-danger" role="button">取消</a> 
				<a href="#" class="btn btn-warning" role="button">複製</a> 
			
		</div>
		<div class="panel-footer hide">Panel footer</div>
	  </div>
	</div>
	</div>
</div>

<div class="tab-pane fade in" role="tabpanel" id="dialog" aria-labelledby="dialog-tab">
	<%-- 這裡是編輯 Dialog UI 的區域，主要靠 renderQAInputUI 來產生 --%>
	<div id="h-qa-panel" style="margin: 20px 0 0 50px; width: 5000px;">
	<div class='node-row'>
	<h1 style='float: left; position: absolute; left: 20px;'>
		<button type="button" data-id='' style='float: left; position: absolute; left: 7px; top: 35px;' class="h-qa-add-child glyphicon glyphicon-plus btn btn-primary"></button>
		<button type="button" data-id='' style='float: left; position: absolute; left: 7px; top: 58px;' class="h-qa-paste glyphicon glyphicon-paste btn btn-primary"></button>
		<span class='glyphicon glyphicon-education'></span>
	</h1>
	<%
	if(_1stLvChildren.length() == 0){
	%>
		<div rel="h_qa"></div>
	<%
	}else{
		renderQAInputUI("h_qa", _1stLvChildren, hIdQaMap, out, locale, 0);
	}
	%>
	</div>
	</div>
	
	<%-- 接下來是點編輯後跳出的 Dialog 介面 --%>
	<div id="h-qa-dialog" title="<bean:message key='advanced.qa'/>" style="display:none;">
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">節點編號</label>
	   	<div class="col-sm-11">
			<span id="h-qa-id"></span>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">節點標題</label>
	   	<div class="col-sm-11">
			<input id="h-qa-t" type="text" class="form-control"></input>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">觸發後行為</label>
	   	<div class="col-sm-11">
			<select id="h-qa-c" class="form-control">
				<% for (OptionAction oa: OptionAction.values()) { %>
				<option value="<%= oa.name() %>"><%= oa.getTitle() %></option>
				<% } %>
				<%--
				<option value="DIRECT_ANSWER">輸出回應</option>
				<option value="REDIRECT_NODE">轉送到別的節點 (觸發該節點)</option>
				<option value="REDIRECT_TO_QUESTION">轉送指定問答 (視為客戶問了該題)</option>
				<option value="SET_VARIABLE">設定變數</option>
				<option value="RESTART_CHECK">重新檢測輸入</option>
				 --%>
			</select>
			<input id="h-qa-p" type="text" class="form-control" style="display:none;" placeholder="轉送到..."></input>
			<p class="help-block">當使用者觸發了此節點後，系統應該如何反應</p>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">問題</label>
	   	<div class="col-sm-11">
			<input id="h-qa-q" type="text" class="form-control"></input>
			<p class="help-block">當使用者觸發了此節點後，系統會視為使用者問了此欄的「問題」</p>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">答案</label>
	   	<div class="col-sm-11">
			<textarea id="h-qa-a" class="form-control" rows="5"></textarea>
			<p class="help-block">當使用者選擇了此項目後，系統會給予此回答</p>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">例句</label>
	   	<div class="col-sm-11">
			<textarea id="h-qa-ms" class="form-control" rows="5"></textarea>
			<p class="help-block">若允許使用者輸入文字來比對選項，這裡是可以放相關例句。若要用正規表示法，請於最前面加入 (?regexp)，例如「(?regexp)^對.*」</p>
	   	</div>
	  </div>
	  <% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E1) > 0) { %>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">腳本</label>
	   	<div class="col-sm-11">
			<textarea id="h-qa-s" class="form-control" rows="5"></textarea>
			<p class="help-block">當執行反應之前，先跑過此腳本（Groovy）</p>
	   	</div>
	  </div>
	  <% } else { // 這裡不應該是用 hidden 方式作，否則是資安漏洞 %>
			<textarea id="h-qa-s" class="hidden" rows="5"></textarea>
	  <% } %>
	  <div class="row">
		<div class="col-sm-11" style="margin-left: 20px;">
		    	<textarea class="form-control hide" rows="5" id="h-qa-x" name="h-qa-x"></textarea>
		
		    	<div id='h-qa-x_editor_holder' class='row'></div>
			<p class="help-block"><bean:message key='qa.extra.params.ex'/></p>
		</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='time.limit'/></label>
	   	<div class="col-sm-11">
	   		<div class="col-sm-10" style="padding: 0;">
				<input id="h-qa-e" type="text" class="form-control" placeholder="<bean:message key='keep.time.limit'/>"
		        	readonly="readonly" style="background-color: white;"></input>
		    </div>
	        <div class="col-sm-1">
	   			<button id="h-qa-clear-expiry" type="button" class="btn btn-default"><bean:message key="keep.time.limit"/></button>
	   		</div>
	   	</div>
	  </div>
	  <div class="row">
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">下層顯示模式</label>
	   	<div class="col-sm-5">
			<select id="h-qa-v" class="form-control">
				<% for (MenuView msb: MenuView.values()) { %>
				<option value="<%= msb.name() %>"><%= msb.getTitle() %></option>
				<% } %>
			</select>
			<p class="help-block">注意！僅有在有子選單的情況下此選項才有功能</p>
	   	</div>
	    <label class="col-sm-1 control-label text-right" style="padding: 0;">下層輸入模式</label>
	   	<div class="col-sm-5">
			<select id="h-qa-i" class="form-control">
				<% for (MenuSelectionBehavior msb: MenuSelectionBehavior.values()) { %>
				<option value="<%= msb.name() %>"><%= msb.getTitle() %></option>
				<% } %>
			</select>
			<p class="help-block">注意！僅有在有子選單的情況下此選項才有功能</p>
	   	</div>
	  </div>
	  <div class="row">
	    <div class="col-sm-offset-1 col-sm-11">
	  		<button id="h-qa-save" type="button" class="btn btn-primary"><bean:message key='submit'/></button>
	  		<button id="h-qa-cancel" type="button" class="btn btn-default"><bean:message key='global.cancel'/></button>
	  	</div>
	  </div>
	</div> <!-- h-qa-panel end -->

	  <div class="row">
	    <div class="col-sm-offset-1 col-sm-11">
				<a href="#" class="btn btn-success btnSave col-sm-2" role="button">儲存</a> 
				<a href="#" class="btn btn-danger col-sm-2 col-sm-offset-1" role="button">取消</a> 
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
<script>

var copyArea = null;
var qaExtraParamEditor;

$(function() {
  var curDocId = '<%= StringEscapeUtils.escapeJavaScript(StringUtils.defaultString((String)request.getAttribute("docId"))) %>';
  
  $.widget( "ui.autocomplete", $.ui.autocomplete, {
	    options: {
	        delay: 500,
	        prefix: ""
	    },

	    _renderItem: function( ul, item ) {
	        var label = item.label;
	        console.log(item);
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
	  $('#dialog-tab').tab('show');
	  
	  $('.tab-iframe').bind('shown.bs.tab', function(e) {  
		    paneID = $(e.target).attr('href');
		    console.log(paneID);
		    src = $(paneID).attr('data-src');
		    // if the iframe hasn't already been loaded once
		    if ($(paneID+" iframe").attr("src")=="") {
		        $(paneID+" iframe").attr("src",src);
		    }
	        $(paneID+" iframe").attr("src",src);
		});
	  
	  $('#h-qa-p')
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
			    	} else {
			    		callback([]);
			    	}
		    },
		    minLength: 2,
		    select: function( event, ui ) {
			    	var item = ui.item;
			    	console.log(item);
			    	var pipe2id = item.id;
			    	$('#h-qa-p').val(pipe2id);
			    	return false;
		    }
		  });
  });
  
  checkPipeInputVisibility = function() {
	  v = $('#h-qa-c').val(); 
	 if (v == '<%= OptionAction.REDIRECT_TO_QUESTION.name() %>'
	 		|| v == '<%= OptionAction.REDIRECT_TO_OPTION.name() %>') {
		$('#h-qa-p').show();
		 if (v == '<%= OptionAction.REDIRECT_TO_QUESTION.name() %>') {
			 $('#h-qa-p').autocomplete( "enable" );
		 }
		 else {
			 $('#h-qa-p').autocomplete( "disable" );
		 }
	 }
	 else {
		$('#h-qa-p').hide();
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
  
  updateNodeView = function(nodeId, nodeData) {
	$('#h-node-' + nodeId + ' .panel-title').text(nodeData.text);
	$('#h-node-' + nodeId + ' .panel-body').text('編號：#' + nodeData.id);
	
	if (nodeData.pipe) {
		$('#h-node-' + nodeId + ' .panel').removeClass('panel-primary').removeClass('panel-success').addClass('panel-info');
		$('#h-node-' + nodeId + ' .panel-body').text('編號：#' + nodeData.id + " => " + nodeData.pipe);
	}
	else {
		if (nodeId == $('input[name=entryPoint]').val()) {
			$('#h-node-' + nodeId + ' .panel').removeClass('panel-info').addClass('panel-success');
		}
		else {
			$('#h-node-' + nodeId + ' .panel').removeClass('panel-info').addClass('panel-primary');
		}
		$('#h-node-' + nodeId + ' .panel-body').text('編號：#' + nodeData.id);
	}
  };
  
  updateViewOfWholeTree = function(startNodeRow) {
	 var dataId = startNodeRow.children('.node-box').attr('data-id');
	 theInput = startNodeRow.find('#h-qa-' + dataId);
	 var data = JSON.parse(theInput.val());
	 
	 updateNodeView(dataId, data);
	 
	 startNodeRow.children('.node-level-container').find('.node-row').each(function() { $this = $(this); updateViewOfWholeTree($this); });
  };
  
  <%--
  	這個跟本 JSP 最上方有個 renderNodeBox 功能重複，但一個是 js 在 client 端畫 node box
    一個在 server 端，所以改動 node box 兩邊都要改，之後應該要把 server side 移掉 
  --%>
  createNode = function(inputNameToRefParent, data) {
	  n = $('<div class="node-box" id="h-node-' + data.id + '" data-id="'+data.id+'"></div>');
	  $('<div class="node-box-connect"></div>').appendTo(n);
	  $('<div class="node-box-collapse-btn" data-id="' + data.id + '"><span class="glyphicon glyphicon-minus"></span></div>').appendTo(n);
	  $('<div class="node-box-expand-btn" style="display: none;" data-id="' + data.id + '"><span class="glyphicon glyphicon-plus"></span></div>').appendTo(n);
	  
	  p = $('<div class="panel ' + (data.pipe?'panel-info':'panel-primary') + ' box-shadow--4dp"><div>');
	  $('<div class="btn-group btn-xs pull-right" style="margin-top: -15px;">'
	  	+ '<button type="button" data-id="'+data.id+'" class="h-qa-delete glyphicon glyphicon-remove btn btn-primary " title="刪除"></button>'
	  	+ '<button type="button" data-id="'+data.id+'" class="h-qa-edit glyphicon glyphicon-pencil btn btn-primary " title="編輯問答"></button>'
	  	+ '<button type="button" data-id="'+data.id+'" class="h-qa-add-child glyphicon glyphicon-plus btn btn-primary " title="新增下層"></button>'
		+ '  <button type="button" class="h-qa-dropdown glyphicon glyphicon-play btn btn-primary dropdown-toggle" id="dropdownMenu'+data.id+'" data-toggle="dropdown" aria-haspopup="true" aria-expanded="true"></button>'
		+ '  <ul class="dropdown-menu" aria-labelledby="dropdownMenu'+data.id+'">'
		+ '    <li><a class="h-qa-cut" data-id="'+data.id+'">剪下</a></li>'
		+ '    <li><a class="h-qa-copy" data-id="'+data.id+'">複製</a></li>'
		+ '    <li><a class="h-qa-paste" data-id="'+data.id+'">貼上</a></li>'
		+ '    <li role="separator" class="divider"></li>'
		+ '    <li><a class="h-qa-move-up" data-id="'+data.id+'">上移</a></li>'
		+ '    <li><a class="h-qa-move-down" data-id="'+data.id+'">下移</a></li>'
		+ '    <li role="separator" class="divider"></li>'
		+ '    <li><a class="h-qa-set-as-entrypoint" data-id="'+data.id+'">預設節點</a></li>'
		+ '  </ul>'
	  	+ '</div>')
	  	.appendTo(p);
	  
	  
	  $('<div class="panel-heading"><h3 class="panel-title">' + data.text + '</h3></div>').appendTo(p);
	  $('<div class="panel-body">編號：#' + data.id + '</div>').appendTo(p);
	  f = $('<div class="panel-footer hide"></div>')
	  
	  i = $('<input type="hidden" id="h-qa-' + data.id + '" name="' + inputNameToRefParent + '" value="">');
	  i.val(JSON.stringify(data));
	  
	  i.appendTo(f);
	  f.appendTo(p);
	  p.appendTo(n);
	  
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
	 theInput = startNodeRow.find('#h-qa-' + oldId);
	 theInput.attr('id', 'h-qa-' + newId);
	 startNodeRow.find('input[name=h_qa_' + oldId + ']').attr('name', 'h_qa_' + newId);
	 startNodeRow.find('*[data-id=' + oldId + ']').attr('data-id', '' + newId);
	 startNodeRow.find('div[rel=h_qa_' + oldId + ']').attr('rel', 'h_qa_' + newId);
	 startNodeRow.children('.node-level-container').find(".node-level-container-vertical-line:first").height(0);

	 var data = JSON.parse(theInput.val());
	 data.id = newId;
	 theInput.val(JSON.stringify(data));
	 
	 startNodeRow.children('.node-level-container').find('.node-row').each(function() { $this = $(this); flushWholeTreeWithNewId($this); });
  };
  
  $(document).on('shown.bs.tab', 'a[data-toggle="tab"]', function (e) {
	  $('.node-box').each(function() {
	  	dataId = $(this).attr('data-id');
		var data = JSON.parse($('#h-qa-' + dataId).val());  
		updateNodeView(dataId, data);
	  });

	  redrawConnectLines();
	});

  $(document).on('click', '.node-box-collapse-btn', function(){
	  dataId = $(this).attr('data-id');
	  $('div[rel=h_qa_' + dataId + ']').hide(100);
	  $(this).parent().find('.node-box-expand-btn').show();
	  $(this).hide();
	  redrawConnectLines();
  });
  
  $(document).on('click', '.node-box-expand-btn', function(){
	  dataId = $(this).attr('data-id');
	  $('div[rel=h_qa_' + dataId + ']').show(100);
	  $(this).parent().find('.node-box-collapse-btn').show();
	  $(this).hide();
	  redrawConnectLines();
  });
  
  <%--
	檢查「編輯」介面當中「觸發後動作」選項會有連動 input 框要不要顯示的機制
  --%>
  $(document).on('change', '#h-qa-c', checkPipeInputVisibility);

  <%--
   刪除節點，就是連 node-row 一起刪除，整個節點包含子節點的 DOM 就會刪掉
  --%>
  $(document).on('click', '.h-qa-delete', function(){
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
  
  $(document).on('click', '.h-qa-move-up', function(){
	 nodeRow = $(this).closest('.node-row');
	 nodeRowPrev = $(this).closest('.node-row').prev();
	 
	 if (nodeRow != nodeRowPrev) {
		 nodeRow.insertBefore(nodeRowPrev);
		 redrawConnectLines();
	  }
  });
  
  $(document).on('click', '.h-qa-move-down', function(){
	 nodeRow = $(this).closest('.node-row');
	 nodeRowNext = $(this).closest('.node-row').next();
	 
	 if (nodeRow != nodeRowNext) {
		 nodeRow.insertAfter(nodeRowNext);
		 redrawConnectLines();
	 }
  });
  
  $(document).on('click', '.h-qa-set-as-entrypoint', function(){
	 var dataId = $(this).attr('data-id');
	 $('.panel-success').removeClass('panel-success');
	 var nodebox = $(this).closest('.node-box');
	 nodebox.find('.panel-primary').addClass('panel-success');
	 $('input[name=entryPoint]').val(dataId);
  });
  
  $(document).on('click', '.h-qa-cut', function(){
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
  $(document).on('click', '.h-qa-copy', function(){
	 oldId = $(this).attr('data-id');
	 copyArea = $(this).closest('.node-row').clone();
	 
	 flushWholeTreeWithNewId(copyArea);
	 $(this).closest('.node-row').fadeIn(150).fadeOut(150).fadeIn(150).fadeOut(150).fadeIn(150); // 來點動畫
  });
  
  <%--
   貼上節點需要進一步處理各種節點內 id 的問題（主要是部分 element 上會標有上層節點的資訊，沒有一併更新會有問題）
  --%>
  $(document).on('click', '.h-qa-paste', function(){
	 if (copyArea) {
		 targetRow = $(this).closest('.node-row');
		 lvContainer = targetRow.children('.node-level-container');
		 targetNodeBoxId = targetRow.children('.node-box').attr('data-id');
		 copiedNodeBoxId = copyArea.children('.node-box').attr('data-id');
		 
		 // 把被複製的存 json 的那個 input 的 name 設定為新的父節點
		 copyArea.find('input#h-qa-' + copiedNodeBoxId).attr('name', typeof(targetNodeBoxId) != 'undefined' ? 'h_qa_' + targetNodeBoxId : 'h_qa');
		 
		 copyArea.appendTo(lvContainer);
		 copyArea.show(500);

		 $(":animated").promise().done(function() {
			 updateViewOfWholeTree(copyArea);
			 copyArea = null;
			 redrawConnectLines();
		 });
	 }
  });

  <%--
   新建節點主要就是根據 timestamp 生成 unique id（這個 id 似乎隨著 SmartRobot 功能演進，必需要跨不同的問答之間也必需 unique，
   但目前用 timestamp 的方式其實並沒有辦法保證是 unique。因此這裡會有潛在造成問題的可能
  --%>
  $(document).on('click', '.h-qa-add-child', function(){
	parentId = $(this).attr('data-id');
	var id = new Date().getTime();
	while ($('#h_qa_' + id).length > 0) {
		id = new Date().getTime(); // 現場就 collision 就一直 loop 到沒有為止
	}
	 
	data = {
		id: id,
		text: '新建節點 #' + id,
	};

	// 這裡要一個所謂 input name for relative to parent，正常編碼是 h_qa_XXXXXX 。不過如果是根節點的話，這裡要 h_qa
	var relToParent = parentId?'h_qa_'+parentId:'h_qa';
	node = createNode(relToParent, data);
	newLvContainer = $('<div rel="h_qa_' + id + '" class="node-level-container" style="margin-left: 250px;"><div class="node-level-container-vertical-line"></div></div>');
	
	newRow = $('<div class="node-row"></div>');
	node.appendTo(newRow);
	newLvContainer.appendTo(newRow);
	
	$(this).closest('.node-row').children('.node-level-container').append(newRow);
	redrawConnectLines();
	
	//var qa = JSON.parse($("#h-qa-" + dataId).val());
	 
	/*
	var inputName = 'h_qa_' + parentId;
	$('<ul>').append(createQAInput(inputName))
	 	.appendTo($(this).closest('li'))
	 	.find('.h-qa-search').focus();
	$(this).hide();
	*/
  });

  $("#h-qa-dialog").dialog({
	minWidth: 800,
	autoOpen: false
  });

  <%-- 
  t = 選項標題
  c = 選擇此項的反應方式
  q = 問題
  a = 回答
  e = 保存期限
  p = PIPE（接到別題，或別的選項）
  s = Groovy script（run just before do OptionAction)
  x = 額外參數 (for JSONEditor)
  v = 下層節點顯示方式
  i = 下層節點輸入方式
  --%>
	$(document).on('click', '.h-qa-edit', function(){
		dataId = $(this).attr('data-id');
		console.log("Show Edit Dialog of " + dataId);
		$('#h-qa-id').text('# ' + dataId);
		var qa = JSON.parse($('#h-qa-' + dataId).val());
		console.log(qa);
		
		$('#h-qa-save').data('editId', qa.id);
		var t = $.trim(qa.text);
		$('#h-qa-t').val(t?t:'');
		
		var c = qa.optionAction;//qa.hasOwnProperty('optionAction') ? qa.optionAction : [];
		$('#h-qa-c').val(c?c:'');
		var q = $.trim(qa.question);
		$('#h-qa-q').val(q?q:'');
		var a = $.trim(qa.answer);
		$('#h-qa-a').val(a?a:'');
		var ms = $.trim(qa.hasOwnProperty('matchSentences') ? qa.matchSentences.join("\n") : "");
		$('#h-qa-ms').val(ms?ms:'');
		var s = $.trim(qa.script);
		$('#h-qa-s').val(s?s:'');
		var e = $.trim(qa.expiry);
		$('#h-qa-e').val(e?e:'');
		var v = $.trim(qa.menuView);
		$('#h-qa-v').val(v?v:'');
		var x = qa.hasOwnProperty('extraParams') ? qa.extraParams : {};
		$('#h-qa-x').val(JSON.stringify(x));
		if (typeof(qaExtraParamEditor) == 'undefined') {
			qaExtraParamEditor = new JSONEditor(document.getElementById('h-qa-x_editor_holder'),{
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
			    		$('#h-qa-x').val(JSON.stringify(qaExtraParamEditor.getValue()));
			    });
			});
		}
		else {
			qaExtraParamEditor.setValue(x);
		}
		var i = $.trim(qa.inputType);
		$('#h-qa-i').val(i?i:'');
		var p = $.trim(qa.pipe);
		$('#h-qa-p').val(p?p:'');
		$("#h-qa-dialog").dialog('open');
		checkPipeInputVisibility();
	});

	$('#h-qa-e').datepicker();

	$('#h-qa-clear-expiry').click(function(){
		$('#h-qa-e').val('');
	});

	$('#h-qa-save').click(function(){
		var id = $(this).data('editId');
		var input = $('#h-qa-' + id);
		var text = $.trim($("#h-qa-t").val());
		var question = $.trim($("#h-qa-q").val());
		var answer = $.trim($("#h-qa-a").val());
		var script = $.trim($("#h-qa-s").val());
		var optionAction = $.trim($("#h-qa-c").val());
		var menuView = $.trim($("#h-qa-v").val());
		var inputType = $.trim($("#h-qa-i").val());
		var expiry = $.trim($("#h-qa-e").val());
		var pipe = $.trim($("#h-qa-p").val());
		var extraParamsStr = $.trim($("#h-qa-x").val());
		extraParams = extraParamsStr ? JSON.parse(extraParamsStr) : {};
		var matchSentencesStr = $.trim($("#h-qa-ms").val());
		var matchSentences = matchSentencesStr.split("\n");
		
		
		var data = {
			  id: id,
			  text: text,
			  optionAction: optionAction,
			  question: question,
			  answer: answer,
			  matchSentences: matchSentences,
			  expiry: expiry,
			  inputType: inputType,
			  menuView: menuView,
			  extraParams: extraParams,
			  script: script,
		};

		if (pipe != '') {
		  data.pipe = pipe;
		}
		
		updateNodeView(id, data);

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
	  //input.siblings('.h-qa-add-child')[answer?'hide':'show']();
	  --%>
		$("#h-qa-dialog").dialog('close');
	});

	$('#h-qa-cancel').click(function() {
		$("#h-qa-dialog").dialog('close');
	});
	
	$('.btnSave').click(function() {
		$.ajax({
				type: 'POST',
				dataType: 'json',
				url: 'qaHierarchicalEditor-ajax.jsp',
				data: $('#form').serialize(),
				success: function(result) {
					
				}
		});
	});

	function createQAInput(name){
		var input = $('<input type="text" class="form-control h-qa-search">');
		input.keypress(function (e) {
		 if (e.which == 13) {
			var val = $.trim(input.val());
			if(val){
				var id = new Date().getTime();
				var elem = $(
						'<li>'
		  				+ '<button type="button" class="h-qa-delete glyphicon glyphicon-remove btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="delete"/>"></button>'
		  				+ '<button type="button" class="h-qa-edit glyphicon glyphicon-pencil btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="edit.qa"/>"></button>'
		  				+ '<button type="button" class="h-qa-add-child glyphicon glyphicon-plus btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="add.lower.layer"/>"></button>'
		  				+ '<span>' + $('<div>').text(val).html() + '</span>'
		  			+ '</li>'
				);
				elem.append(
					$('<input type="hidden" id="h-qa-' + id + '" name="' + name + '">')
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
		    	$.getJSON('<%= request.getContextPath() %>/wiseadm/qa-plugin/hierarchical-qa-search.jsp',
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
		  				+ '<button type="button" class="h-qa-delete glyphicon glyphicon-remove btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="delete"/>"></button>'
		  				+ '<button type="button" class="h-qa-edit glyphicon glyphicon-pencil btn btn-xs" style="margin-right: 0.5em;" title="<bean:message key="edit.qa"/>"></button>'
		  				+ '<a target="_blank" href="qaDataEditor.jsp?id=' + (pipe2id ? pipe2id : id) + '">'
						+ $('<div>').text(item.value).html()
	  				+ '</a>'
		  			+ '</li>'
				);
				elem.append(
					$('<input type="hidden" id="h-qa-' + id + '" name="' + name + '">')
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
#h-qa-panel li {
	margin-bottom: 0.5em;
}
#h-qa-panel ul {
	padding-top: 0.5em;
}
.h-qa-delete, .h-qa-add-child, .h-qa-paste, .h-qa-edit, .h-qa-dropdown {
	padding: 1px 5px 1px 5px;
}
#h-qa-dialog .row {
	margin-top: 0.5em;
}
</style>

</body>
</html>
