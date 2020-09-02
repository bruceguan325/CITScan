<%@ include file="/commons/taglib.jsp"%>
<%@page import="java.io.Writer"%>
<%@page import="java.util.Map.Entry"%>
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.QAContext.*"
	import="com.intumit.solr.robot.qaplugin.*"
	import="java.util.Locale"
	import="com.intumit.message.MessageUtil"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%!
void renderQAInputUI(String name, JSONArray data, Map<String, String> hIdQaMap, Writer out, Locale locale)
		throws Exception {
	if(data.length() > 0){
		out.write("<ul id=sortable2 rel=\"" + name + "\" class=\"hierarchical-list\">");
		for(int i=0; i<data.length(); i++){
			Object obj = data.opt(i);
			if(obj instanceof String){
				String id = (String) obj;
				out.write(
				"<li>"
  					+ "<button type=\"button\" class=\"h-qa-delete glyphicon glyphicon-remove btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "delete") + "\"></button>"
  					+ "<a target=\"_blank\" href=\"qaDataEditor.jsp?id=" + id + "\">"
						+ StringEscapeUtils.escapeHtml(hIdQaMap.get(id))
  					+ "</a>"
  					+ "<input type=\"hidden\" name=\"" + name + "\" value=\"" + id + "\" />"
  				+ "</li>"
  				);
			}
			else if(obj instanceof JSONObject){
				JSONObject json = (JSONObject)obj;

				String oaName = json.optString("optionAction");
				OptionAction oa = null;
				if (StringUtils.trimToNull(oaName) != null) {
					oa = OptionAction.valueOf(oaName);
				}
				
				if (OptionAction.REDIRECT_TO_QUESTION == oa || OptionAction.REDIRECT_TO_OPTION == oa) {
					String id = json.optString("id");
					String text = json.optString("text");
					String expiry = json.optString("expiry");
					String pipe = json.optString("pipe");
					
					String typeIcon = null;
					if (OptionAction.REDIRECT_TO_QUESTION == oa) {
						typeIcon = "<span class='glyphicon glyphicon-share-alt'></span>";
					}
					else if (OptionAction.REDIRECT_TO_OPTION == oa) {
						typeIcon = "";//<span class='glyphicon glyphicon-list'></span>";
					}
					
					JSONObject val = new JSONObject(json, HierarchicalQA.JSON_PROPS);
					String html = "<button type=\"button\" class=\"h-qa-delete glyphicon glyphicon-remove btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "delete") + "\"></button>";
					html += "<button type=\"button\" class=\"h-qa-edit glyphicon glyphicon-pencil btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "edit.qa") + "\"></button>";
					
					if (OptionAction.REDIRECT_TO_QUESTION == oa) {
		  				html += "<a target=\"_blank\" href=\"qaDataEditor.jsp?id=" + pipe + "\">" + StringEscapeUtils.escapeHtml(text)
		  					 + "&nbsp;<span class='glyphicon glyphicon-share-alt'></span></a>";
					}
					else if (OptionAction.REDIRECT_TO_OPTION == oa) {
		  				html += "<a target=\"_blank\" class='hover-highlight-option' data-id='" + pipe + "'>" + StringEscapeUtils.escapeHtml(text)
			  					 + "</span></a>";
					}
					
	  				if(QAUtil.isDateExpired(expiry)){
	  					html += "<span class='text-warning'>（" + MessageUtil.getMessage(locale, "global.expired") + "）</span>";
	  				}
	  				
	  				html += "<input type=\"hidden\" id=\"h-qa-" + id + "\" name=\"" + name + "\" value=\"" + StringEscapeUtils.escapeHtml(val.toString()) + "\" />";
	  				out.write("<li>" + html);
					out.write("</li>");
				}
				else {
					String id = json.optString("id");
					String text = json.optString("text");
					String expiry = json.optString("expiry");
					JSONArray children = json.optJSONArray("children");
					boolean hasAnswer = StringUtils.isNotBlank(json.optString("answer"));
					boolean showAddChildBtn = true;//children.length() == 0 && !hasAnswer;
					JSONObject val = new JSONObject(json, HierarchicalQA.JSON_PROPS);
					String html = "<button type=\"button\" class=\"h-qa-delete glyphicon glyphicon-remove btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "delete") + "\"></button>";
					html += "<button type=\"button\" class=\"h-qa-edit glyphicon glyphicon-pencil btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "edit.qa") + "\"></button>";
					html += "<button type=\"button\" class=\"h-qa-add-child glyphicon glyphicon-plus btn btn-xs\"";
					html += " style=\"margin-right: 0.5em;" + (showAddChildBtn?"":"display:none;") + "\" title=\"" + MessageUtil.getMessage(locale, "add.lower.layer") + "\"></button>";
					//html += "<button type=\"button\" class=\"h-qa-copy glyphicon glyphicon-copy btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "copy") + "\"></button>";
					//html += "<button type=\"button\" class=\"h-qa-cut glyphicon glyphicon-scissors btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "cut") + "\"></button>";
					//html += "<button type=\"button\" class=\"h-qa-paste glyphicon glyphicon-paste btn btn-xs\" style=\"margin-right: 0.5em;\" title=\"" + MessageUtil.getMessage(locale, "paste") + "\"></button>";
	  				html += "<span class=mouse2>" + StringEscapeUtils.escapeHtml(text);
	  				if(QAUtil.isDateExpired(expiry)){
	  					html += "<span class='text-warning'>（" + MessageUtil.getMessage(locale, "global.expired") + "）</span>";
	  				}
	  				html += "</span>";
	  				html += "<input type=\"hidden\" id=\"h-qa-" + id + "\" name=\"" + name + "\" value=\"" + StringEscapeUtils.escapeHtml(val.toString()) + "\" />";
	  				out.write("<li>" + html);
	  				//if(!hasAnswer){
						renderQAInputUI("h_qa_" + id, children, hIdQaMap, out, locale);
	  				//}
					out.write("</li>");
				}
			}
		}
		out.write("</ul>");
	}
}
%>
<%

Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
JSONObject data = (JSONObject) request.getAttribute(HierarchicalQA.REQ_ATTR_QA_DATA);
Map<String, String> hIdQaMap = (Map<String, String>) request.getAttribute(HierarchicalQA.REQ_ATTR_ID_QA_MAP);
String entryPoint = null;
JSONArray _1stLvChildren = null;
boolean lockFirstLvChildren = false;  // 若為新的階層，第一層直接產生一個「預設進入點」，且鎖定不要提供可生成同樣在第一層的節點的輸入框

if (data != null) {
	 entryPoint = StringUtils.trimToNull(data.optString("entryPoint"));
	_1stLvChildren = data.optJSONArray("children");
}

if (_1stLvChildren == null || (_1stLvChildren != null && _1stLvChildren.length() == 0)) {
	JSONObject newEP = new JSONObject();
	Long newEpId = System.currentTimeMillis();
	entryPoint = "" + newEpId;
	newEP.put("id", newEpId);
	newEP.put("text", "[" + MessageUtil.getMessage(locale, "hierarchical.entry.point") + "]");
	newEP.put("children", new JSONArray());
	_1stLvChildren = new JSONArray();
	_1stLvChildren.add(newEP);
	data.put("children", _1stLvChildren);
	data.put("entryPoint", entryPoint);
}

if (entryPoint != null)
	lockFirstLvChildren = true;

if (_1stLvChildren == null)
	_1stLvChildren = new JSONArray();
%>
<a href="qaHierarchicalEditor.jsp?id=<%= request.getParameter("id") %>" target="_hdialog" class="btn btn-md btn-default easterEgg">開啟視覺化編輯</a>

<input type='hidden' name='entryPoint' value='<%= StringUtils.trimToEmpty(entryPoint) %>'>
<div id="h-qa-panel">
<%
if(_1stLvChildren.length() == 0){
%>
	<ul rel="h_qa" class="hierarchical-list"></ul>
<%
}else{
	renderQAInputUI("h_qa", _1stLvChildren, hIdQaMap, out, locale);
}
%>
</div>
<div id="h-qa-dialog" title="<bean:message key='advanced.qa'/>" style="display:none;">
  <div class="row">
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.option.num'/></label>
   	<div class="col-sm-11">
		<span id="h-qa-id"></span>
   	</div>
  </div>
  <div class="row">
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.option.title'/></label>
   	<div class="col-sm-11">
		<input id="h-qa-t" type="text" class="form-control"></input>
   	</div>
  </div>
  <div class="row">
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.behavior.after.select'/></label>
   	<div class="col-sm-11">
		<select id="h-qa-c" class="form-control">
			<% for (OptionAction oa: OptionAction.values()) { %>
			<option value="<%= oa.name() %>"><%= oa.getMessage(locale) %></option>
			<% } %>
		</select>
		<input id="h-qa-p" type="text" class="form-control" style="display:none;" placeholder="轉送到..."></input>
		<p class="help-block"><bean:message key='hierarchical.system.reaction'/></p>
   	</div>
  </div>
  <div class="row">
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.question'/></label>
   	<div class="col-sm-11">
		<input id="h-qa-q" type="text" class="form-control"></input>
		<p class="help-block"><bean:message key='hierarchical.user.ask.selected.question'/></p>
   	</div>
  </div>
  <div class="row">
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.option.answer'/></label>
   	<div class="col-sm-11">
		<textarea id="h-qa-a" class="form-control" rows="5"></textarea>
		<p class="help-block"><bean:message key='hierarchical.answer.of.option'/></p>
   	</div>
  </div>
  <div class="row">
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.option.example'/></label>
   	<div class="col-sm-11">
		<textarea id="h-qa-ms" class="form-control" rows="5"></textarea>
		<p class="help-block"><bean:message key='hierarchical.option.example.desc'/></p>
   	</div>
  </div>
  <% if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E1) > 0) { %>
  <div class="row">
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.option.script'/></label>
   	<div class="col-sm-11">
		<textarea id="h-qa-s" class="form-control" rows="5"></textarea>
		<p class="help-block"><bean:message key='hierarchical.run.script.before.reaction'/></p>
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
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.second.level.display.mode'/></label>
   	<div class="col-sm-5">
		<select id="h-qa-v" class="form-control">
			<% for (MenuView msb: MenuView.values()) { %>
			<option value="<%= msb.name() %>"><%= msb.getMessage(locale) %></option>
			<% } %>
		</select>
		<p class="help-block"><bean:message key='hierarchical.second.level.notification'/></p>
   	</div>
    <label class="col-sm-1 control-label text-right" style="padding: 0;"><bean:message key='hierarchical.second.level.input.mode'/></label>
   	<div class="col-sm-5">
		<select id="h-qa-i" class="form-control">
			<% for (MenuSelectionBehavior msb: MenuSelectionBehavior.values()) { %>
			<option value="<%= msb.name() %>"><%= msb.getMessage(locale) %></option>
			<% } %>
		</select>
		<p class="help-block"><bean:message key='hierarchical.second.level.notification'/></p>
   	</div>
  </div>
  <div class="row">
    <div class="col-sm-offset-1 col-sm-11">
  		<button id="h-qa-save" type="button" class="btn btn-primary"><bean:message key='submit'/></button>
  		<button id="h-qa-cancel" type="button" class="btn btn-default"><bean:message key='global.cancel'/></button>
  	</div>
  </div>
</div>
<script>

$(function(){
	//$( "#sortable2" ).sortable({connectWith: "#sortable1",cancel: '.no-drag',items: 'li:not(:first)'});
	//$( "#sortable2" ).disableSelection();
	});

var copyArea = null;
var qaExtraParamEditor;

$(function() {
  var curDocId = '<%= StringEscapeUtils.escapeJavaScript(StringUtils.defaultString((String)request.getAttribute("docId"))) %>';

  <% if (lockFirstLvChildren) { %>
	$('#h-qa-panel ul[rel!="h_qa"]').each(function(){
	  var ul = $(this);
	  ul.append(createQAInput(ul.attr('rel')));
	});
  <% } else { %>
	$('#h-qa-panel ul').each(function(){
	  var ul = $(this);
	  
	  ul.append(createQAInput(ul.attr('rel')));
	});
  <% } %>
  
  function checkPipeInputVisibility() {
	 v = $('#h-qa-c').val(); 
	 if (v == '<%= OptionAction.REDIRECT_TO_QUESTION.name() %>'
	 		|| v == '<%= OptionAction.REDIRECT_TO_OPTION.name() %>') {
		$('#h-qa-p').show();
	 }
	 else {
		$('#h-qa-p').hide();
	 }
  }
  
  $(document).on('mouseover', '.hover-highlight-option', function() {
	  id = $(this).attr('data-id');
	  $('#qn-qa-' + id).parent('li').addClass('alert').addClass('alert-danger');
  });
  
  $(document).on('mouseleave', '.hover-highlight-option', function() {
	  $('li').removeClass('alert').removeClass('alert-danger');
  });

  $(document).on('change', '#h-qa-c', checkPipeInputVisibility);

  $(document).on('click', '.h-qa-delete', function(){
	 $(this).closest('li').remove();
  });

  $(document).on('click', '.h-qa-add-child', function(){
	 var qa = JSON.parse($(this).siblings('input:hidden').val());
	 var inputName = 'h_qa_' + qa.id;
	 $('<ul id="sortable1">').append(createQAInput(inputName))
	 	.appendTo($(this).closest('li'))
	 	.find('.h-qa-search').focus();
	 $(this).hide();
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
  p = PIPE（接到別題）
  s = Groovy script（run just before do OptionAction)
  x = 額外參數 (for JSONEditor)
  v = 下層節點顯示方式
  i = 下層節點輸入方式
  --%>
  $(document).on('click', '.h-qa-edit', function(){
	  var qa = JSON.parse($(this).siblings('input:hidden').val());
	  $('#h-qa-id').text('# ' + qa.id);
	  $('#h-qa-save').data('editId', qa.id);
	  var t = $.trim(qa.text);
	  $('#h-qa-t').val(t?t:'');
	  var c = $.trim(qa.optionAction);
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
	  var matchSentences = matchSentencesStr ? matchSentencesStr.split("\n") : [];
	  
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

	  input.attr('value', JSON.stringify(data));
	  var expired = false;
	  if(expiry){
	  	try{
			var expiryMillis = new Date(expiry).getTime();
		  	expired = new Date().getTime() >= expiryMillis;
		}catch(e){}
	  }
	  var textElem = input.prev()
	  	.text(text);
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

  $('#h-qa-cancel').click(function(){
	  $("#h-qa-dialog").dialog('close');
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
		  				+ '<span class=mouse1>' + $('<div>').text(val).html() + '</span>'
		  			+ '</li>'
				);
				elem.append(
					$('<input type="hidden" id="h-qa-' + id + '" name="' + name + '">')
						.attr('value', JSON.stringify({id: id, text: val}))
				);
				input.before(elem);
				$(function(){
				    $( "#sortable1" ).sortable();
				    $( "#sortable1" ).disableSelection();});
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
#h-qa-dialog .row {
	margin-top: 0.5em;
}
</style>
