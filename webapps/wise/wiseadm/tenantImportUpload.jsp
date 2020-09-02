<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
%>
<script>
window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
</script>
<%
return;
}

String id = request.getParameter("id");
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<title><bean:message key='data.upload.page'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>

<script src="js/jquery.fileupload.js"></script>

<!-- we code these -->
<link href="css/dropzone.css" type="text/css" rel="stylesheet" />
</head>

<body>
	<h3><bean:message key='knowledge.batch.import'/><br></h3> 

	<div class="row">
		<div class="col-md-6">
		<input id="fileupload" type="file" name="files[]" data-url="<%= request.getContextPath() %>/wiseadm/tenantDumpUpload" multiple>
		
		<div id="dropzone" class="fade well">Drop files here</div>
		</div>
	</div>
	
	<div id="progress" class="progress">
    	<div class="progress-bar" style="width: 0%;"></div>
	</div>
	
	<div class="alert alert-warning">
	除了檔案、測試紀錄、對話紀錄外，應該全部都會匯出，也全部都能匯入。<br>
    匯入的時候最好該公司別是空的，沒有重複資料檢測機制。<br>
    目前沒考慮資料量太大的問題。匯出應該不是問題，匯入如果遇到極大的匯出檔可能會 OOM。
	</div>
	
	<h5 style="text-align:center"><i style="color:#ccc"><small>Max File Size: 2 Mb - Display last 20 files</small></i></h5>

	<table id="uploaded-files" class="table">
		<tr>
			<th>File Name</th>
			<th>File Size</th>
			<th>File Type</th>
			<th>Download</th>
			<th>Uploaded By</th>
		</tr>
	</table>


<script>

$(function () {
	
    $('#fileupload').fileupload({
        dataType: 'json',
        success: function (data) {
	        	$("tr:has(td)").remove();
	        	
	        	if (data != null) {
				    $.each(data, function (index, file) {
		                $("#uploaded-files").append(
		                		$('<tr/>')
		                		.append($('<td/>').text(file.fileName))
		                		.append($('<td/>').text(file.fileSize))
		                		.append($('<td/>').text(file.fileType))
		                		.append($('<td/>').text("@"+file.twitter))
		                		.append($('<td/>').html(
		                				"<a href='/wise/wiseadm/tenantImport.jsp?id=<%= id %>&c=i&f="+index+"'>Import</a>"
		                						))
		
		                		);
		            }); 
	        	}
        },
        progressall: function (e, data) {
	        var progress = parseInt(data.loaded / data.total * 100, 10);
	        $('#progress .progress-bar').css(
	            'width',
	            progress + '%'
	        );
   		},
   		
		dropZone: $('#dropzone')
    }).bind('fileuploadsubmit', function (e, data) {
    });
   
});


</script>
</body> 
</html>
