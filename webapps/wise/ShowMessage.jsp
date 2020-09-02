<%@ include file="/commons/taglib.jsp"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="com.intumit.message.MessageUtil" %>
<%@ page import="java.util.Locale" %>

<%
    String msg = "";

    Object o = request.getAttribute("message-key");
    if (o != null && (o instanceof String)) { 
        String msgKey = (String)o;
        msg = MessageUtil.getMessage(Locale.TAIWAN, msgKey);
        if (msg == null) {
            msg = MessageUtil.getMessage(Locale.TAIWAN, "global.errorMsg");
        }
        msg += "<br />";
    }

    Exception e = (Exception)request.getAttribute("exception");
    if (e != null && e.getMessage() != null) {
        msg += " : " + StringEscapeUtils.escapeHtml(e.getMessage()) + "\n";
    }

    String customMsg = (String)request.getAttribute("message");
    if (customMsg != null) {
        msg += StringEscapeUtils.escapeHtml(customMsg) + "<br />";
    }
%>

<html>
<head>
<title><bean:message key="global.errorMsg" /></title>
</head>
<body leftmargin="0" topmargin="0" marginwidth="0" marginheight="0" class="mainbody">
	<br>
	<table valign="top" width="100%" border="0" cellspacing="0" cellpadding="0">
		<tr>
			<td align="left" class="topic" height="100%"><font
				color="#CC3300"><%=msg%></font></td>
		</tr>
	</table>
</body>
</html>
