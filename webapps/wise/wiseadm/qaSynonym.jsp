<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="com.intumit.solr.admin.*" %>
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
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='synonym.maintenance'/></title>
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

</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<iframe src="<%= request.getContextPath() %>/wiseadm/syn"  id="iframe1" marginheight="0" frameborder="0" onLoad="autoResize('iframe1');"></iframe>
</div>
<script>
$(document).ready(function() {
	$.ajax({
		data: {c:"l"},
		dataType: "json",
		url: "<%= request.getContextPath() %>/wiseadm/fileUpload",
		success: function(data) {
			console.log(data);
		    $.each(data, function (index, file) {
                $("#uploaded-files").append(
                		$('<tr/>')
                		.append($('<td/>').text(file.fileName))
                		.append($('<td/>').text(file.fileSize))
                		.append($('<td/>').text(file.fileType))
                		.append($('<td/>').text("@"+file.twitter))
                		.append($('<td/>').html(
                				"<a href='/wise/wiseadm/processData?f="+index+"'>Import</a>"
                			  + "&nbsp;<a href='/wise/wiseadm/fileUpload?c=d&f="+index+"'>Delete</a>"
                						))

                		)//end $("#uploaded-files").append()
            });
		}
	});
});
</script>
</body>
</html>
