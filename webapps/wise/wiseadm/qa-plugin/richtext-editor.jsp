<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
%>
<%
String richtext = StringUtils.defaultString((String)request.getAttribute("richtext"));
String contextPath = request.getContextPath();
%>
<textarea class="form-control" id="answerTextArea" name="answer_richtext" rows="8"><%= StringEscapeUtils.escapeHtml(richtext) %></textarea>
<script src="<%=request.getContextPath()%>/ckeditor/ckeditor.js"></script>
<script>
CKEDITOR.config.baseHref = '<%= StringEscapeUtils.escapeJavaScript(contextPath) %>/';
CKEDITOR.config.language = 'zh';
CKEDITOR.config.extraPlugins='simpleuploads';
CKEDITOR.replace('answerTextArea', {
	filebrowserUploadUrl : '<%= StringEscapeUtils.escapeJavaScript(contextPath) %>/wiseadm/ckeditorUpload.jsp'
});
$(document).ready(function() {
	// A hack for update textarea when using ajax to save QA
	$('#save').on('click', function() {
		$textarea = $('#answerTextArea');
		$textarea.val(CKEDITOR.instances['answerTextArea'].getData());
	});
	
	$('#save_copy').on('click', function() {
		$textarea = $('#answerTextArea');
		$textarea.val(CKEDITOR.instances['answerTextArea'].getData());
	});
});
</script>