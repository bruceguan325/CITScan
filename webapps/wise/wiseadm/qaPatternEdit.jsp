<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang3.*"
import="com.intumit.solr.robot.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='global.special.answer'/> - <bean:message key='global.edit'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<form id="thisForm" action="qaPatternSave.jsp" method="post">
	<div class="col-md-8">
	<table class='table table-bordered table-striped' style="width: 100%;">
	<thead>
		<tr>
		<th>Field</th>
		<th>Value</th>
		</tr>
	</thead>
	<%
	String id = request.getParameter("id");
	QAPattern p = id == null ? null : QAPattern.get(new Integer(id));
	%>
	<tr>
		<input type="hidden" value="<%= (p == null) ? "save" : "update" %>" name="action" />
		<th><bean:message key='global.mkey'/></th>
		<td>
		<% if (id != null) { %>
		<input type='hidden' name="id" value='<%= p.getId() %>' >
		<% } %>
		<input type='text' class='form-control' name="mkey"
			value='<%= p == null ? "" : p.getMkey() %>'
			>
			<ul>
				<li><span class="text-danger"><bean:message key="mkey.desc1"/></span></li>
				<li><span class="text-danger"><bean:message key="mkey.desc2"/></span></li>
			</ul>
		</td>
	</tr>
	<tr>
		<th><bean:message key='global.name'/></th>
		<td>
		<input type='text' class='form-control' name="questionTemplate"
			value='<%= p == null ? "" : p.getQuestionTemplate() %>'
			>
		</td>
	</tr>
	<tr>
		<th><bean:message key="data.resource"/></th>
		<td>
		<select type='text' class='form-control' name="dataSource">
		<% for (QAPattern.DataSource ds: QAPattern.DataSource.values()) {
			if (t.getOpendataCoreUrl() == null && ds == QAPattern.DataSource.OPENDATA) continue;
		%>
			<option value='<%= ds.name() %>' <%= p != null && ds == p.getDataSource() ? "selected" : "" %>
			><%= ds.name() %></option>
		<% } %>
		</select>
		</td>
	</tr>
	<tr>
		<th><bean:message key='data.alignment.model'/></th>
		<td>
		<select type='text' class='form-control' name="dataAggregator">
		<% for (String ao: QADataAggregator.listTypes()) { %>
			<option value='<%= ao %>' <%= ao.equalsIgnoreCase(p == null ? null : p.getDataAggregator()) ? "selected" : "" %>
			><%= ao %></option>
		<% } %>
		</select>
			<ul>
			<bean:message key='data.alignment.model.ex'/>
			</ul>
		</td>
	</tr>
	<tr class="period">
		<th><bean:message key='qa.pattern.period'/></th>
		<td>
		<input type='text' class='form-control' name="answerPeriod"
			value='<%= p == null ? "" : p.getAnswerPeriod() %>'
			>
		</td>
	</tr>
	<tr>
		<th><bean:message key='max.number.pens'/></th>
		<td>
		<input type='text' class='form-control' name="maxMatched"
			value='<%= p == null ? "" : p.getMaxMatched() %>'
			>
		</td>
	</tr>
	<tr>
		<th><bean:message key='output.voice'/></th>
		<td>
		<textarea class='form-control' name="answerVoice" rows="1"
			><%= p == null ? "" : StringEscapeUtils.escapeHtml4(StringUtils.trimToEmpty(p.getAnswerVoice())) %></textarea>
		</td>
	</tr>
	<tr>
		<th><bean:message key='output.template'/></th>
		<td>
		<textarea class='form-control' name="answerTemplate" rows="20"
			><%= p == null ? "" : StringEscapeUtils.escapeHtml4(StringUtils.trimToEmpty(p.getAnswerTemplate())) %></textarea>
			<ul>
			<bean:message key='output.template.ex'/>
			</ul>
		</td>
	</tr>
	<tr>
		<th><bean:message key='special.restrictions'/></th>
		<td>
		<textarea class='form-control'  name="specialRestriction" rows="10"
			><%= p == null ? "" : StringUtils.defaultString(p.getSpecialRestriction(), "") %></textarea>
			<bean:message key='special.restrictions.ex'/>
		<!-- <p>
			搜尋可能的答案時套上這些搜尋參數，可以分成多行，一行一個，可套用範本參數
			<ol>
				<li>fq= 開頭為限制條件
				<li>sort= 為排序條件 (ex. sort=Value_d desc)（可用 asc / desc）
			</ol>
			可能的搜尋條件有
			<ol>
				<li>dataType_s 資料類型，為當初上傳 tsv 的檔案名稱（Ex. Board.tsv 則 dataType_s 的資料為 Board）</li>
				<li>XXXXX_s 或 XXXXX_t 每一個 tsv 的標頭都會自動產生 XXXXX_s 及 XXXXX_t 兩個索引欄位，前者為完全 match，後者可為部分 match</li>
				<li>若選擇 DataAggregator 為 URL 時，這裡請填入外部 URL 網址</li>
			</ol>
		</p> -->
		</td>
	</tr>
	<tr>
		<th><bean:message key='global.preview'/><bean:message key='query'/></th>
		<td>
		<textarea class='form-control'  name="previewParameters" rows="10"
			><%=p == null ? "" : StringUtils.defaultString(p.getPreviewParameters(), "")%></textarea>
		<bean:message key='global.preview.query.ex'/>
		<!-- <p>
			用來預覽結果的搜尋，這裡直接放上預計搜尋到的資料所需的範例參數，當點選預覽時將直接用這些參數進行搜尋並套用相關輸出
			<ol>
			<li><span class="text-danger">用 oq= 來代表輸入的問題，會嘗試從問句當中拆出相關變數（這是必要參數）</span></li>
			<li>其餘參數參考特殊限制</li>
			</ol>
		</p> -->
		</td>
	</tr>
	<tr>
		<th><bean:message key='global.standard.answer'/></th>
		<td style="word-wrap:break-word;">
		<input type='text'  class='form-control' name="staticAnswer"
			value='<%= p == null ? "" : StringUtils.defaultString(p.getStaticAnswer(), "") %>'
			>
		<bean:message key='global.standard.answer.ex'/>
		<!-- <p>若選擇 DataAggregator 為 URL 時，制式回答將會以 addtionalData 參數傳遞
		</p> -->
		</td>
	</tr>
	<tr>
		<th><bean:message key='operation'/></th>
		<td>
		<input type='hidden' id='keepEdit' name='keepEdit' value='false'>
		<button id="saveAndExit" type="submit" class='btn btn-success'><bean:message key='submit.and.leave'/></button>
		&nbsp;
		<button id="save" type="submit" class='btn btn-success'><bean:message key='submit'/></button>
		&nbsp;
		<button id="preview" type="button" class='btn btn-primary'><bean:message key='global.preview'/></button>
		&nbsp;
		&nbsp;
		&nbsp;
		<button id="saveAs" class='btn btn-warning'><bean:message key='save.a.new.file'/></button>
		&nbsp;
		<button type="reset" class='btn btn-danger'><bean:message key='global.give.up'/></button>
		</td>
	</tr>
	</table>
	</div>

	<div id="preview-dialog" title="<bean:message key='global.preview'/>" style="display:none; padding:0;">
	  <div class="container" style="background-color: white;">
	    <div style="padding: 15px 0 0;">
		  	<label style="font-weight: normal;">
		      <input type="radio" name="previewType" value="1024,600" checked="checked">&nbsp;PC
		    </label>&nbsp;&nbsp;
		    <label style="font-weight: normal;">
		      <input type="radio" name="previewType" value="480,600">&nbsp;Mobile
		    </label>
	    </div>
	  </div>
	</div>
</form>
<script>
$("#save").click(function() {
	$('#keepEdit').val(true);
	$('#thisForm').attr({
		action: 'qaPatternSave.jsp',
		target: null
	}).submit();
});

$("#saveAndExit").click(function() {
	$('#keepEdit').val(false);
	$('#thisForm').attr({
		action: 'qaPatternSave.jsp',
		target: null
	}).submit();
});

$("#saveAs").click(function() {
	$('input[name="id"]').remove();
	$('#thisForm').attr({
		action: 'qaPatternSave.jsp',
		target: null
	}).submit();
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
	$('#thisForm').attr({
			action: 'qaPatternPreview.jsp',
			target: 'preview'
		}).submit();
	$("#preview-dialog").dialog('option', 'width', width + 40);
});
$('#preview').click(function(){
	if ($('textarea[name=previewParameters]').val() == '') {
		alert("<bean:message key='preview.parameters'/>");
	}
	else {
		$('input[name=previewType]:checked').click();
		$( "#preview-dialog").dialog('open');
	}
});

$('select[name=dataAggregator]').on('change', function() {
	dataAggregatorChanged(this.value);
});

function dataAggregatorChanged(value) {
	if(value == 'PeriodDataAggregator') {
		$('.period').show();
	}
	else {
		$('.period').hide();
	}
}

$(document).ready(function() {
	dataAggregatorChanged($('select[name=dataAggregator]').val());
});
</script>
</div>
</body>
</html>