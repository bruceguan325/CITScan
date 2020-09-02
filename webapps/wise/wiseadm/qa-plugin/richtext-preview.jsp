<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.commons.lang.*"
	import="com.intumit.solr.robot.*"
%>
<% 
String richtext = (String)request.getAttribute("richtext");
if(StringUtils.isNotBlank(richtext)){
	String answer = (String) request.getAttribute("answer");
	if(StringUtils.isNotBlank(answer)){
%>
<br/><br/>
<%
	}
%>
<i><bean:message key='richtext.preview'/>
	<form action="<%= request.getContextPath() + "/qaAnswerRichtextPreview.jsp" %>"
			target="blank" method="post" style="display: none;">
		<input type="hidden" name="text" value="<%= StringEscapeUtils.escapeHtml(richtext) %>" />
	</form>
</i>
<script type="text/javascript">
function next(elem) {
    do {
        elem = elem.nextSibling;
    } while (elem && elem.nodeType !== 1);
    return elem;        
}
</script>
<% 
}
%>