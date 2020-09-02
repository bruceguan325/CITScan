<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.robot.intent.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE><bean:message key='intent.management'/></TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>

<script type="text/javascript">
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
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>

<div class="container">
<iframe src="<%= request.getContextPath() %>/wiseadm/intent"  id="iframe1" marginheight="0" frameborder="0" onLoad="autoResize('iframe1');"></iframe>
</div>
</body> 
</html>