<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);

if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O4) == 0) { 
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
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<title><bean:message key='data.upload.page'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>

<script src="js/jquery.fileupload.js"></script>

<!-- we code these -->
<link href="css/dropzone.css" type="text/css" rel="stylesheet" />
<script src="js/myuploadfunction.js?_=ttt"></script>
</head>

<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
	<h3><bean:message key='knowledge.batch.import'/><br></h3> 

	<div class="row">
		<div class="col-md-6">
		<input id="fileupload" type="file" name="files[]" data-url="<%= request.getContextPath() %>/wiseadm/fileUpload" multiple>
		
		<div id="dropzone" class="fade well">Drop files here</div>
		</div>
		<div class="col-md-6">
		<bean:message key='btn.text.paste'/>
		<!-- <button id="btnTextPaste">文字上傳區</button>點擊此按鈕使用剪貼簿直接貼上文字 -->
		</div>
	</div>
	<%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E5) > 0
		|| ("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	%>
	<div class="row">
        <div class="col-md-8">
            <button id="export"><bean:message key='knowledge.export'/></button>
        </div>
        <div class="col-md-4">
            <button id="export_similar"><bean:message key='similar.export'/></button>
        </div>
    </div>
    <%
	}
    %>
    <br>
	<bean:message key='import.alert.info'/>
	<!-- <div class="alert alert-info">
	<ul>
	<li>請使用 TAB 分隔的文字檔格式匯入 (Excel > 另存新檔 > TAB分隔的文字檔)</li>
	</ul>
	</div>
	<div class="alert alert-info">
	<ul>
	<li>匯入問答知識庫的，檔名請使用「<span class='text-warning'>智能客服<span class='text-danger'>問答知識庫</span>匯入檔</span>」開頭的文字檔（例如：智能客服問答知識庫匯入檔_20150101_v1.txt）</li>
	</ul>
	</div>
	<div class="alert alert-info">
	<ul>
	<li>匯入階層式問答資料的，檔名請使用「<span class='text-warning'>智能客服<span class='text-danger'>階層式問答</span>匯入檔</span>」開頭的文字檔（例如：智能客服階層式問答匯入檔_20150101_v1.txt）</li>
	<li>階層式問答的知識編號不能與前述問答知識庫重複，同一編號則會視為該問答轉換為階層式問答</li>
	</ul>
	</div>
	<div class="alert alert-info">
	<ul>
	<li>若要匯入特殊問題使用的資料（如ATM或者分行資料），檔名請使用全大寫英文（例如 ATM.TXT）</li>
	</ul>
	</div>
	<div class="alert alert-danger">
	<ul>
	<li>記得上傳後，要點選下方的 import 按鈕才會真正做匯入檢測，點選後根據畫面說明完成匯入動作</li>
	</ul>
	</div> -->
	
	<div id="progress" class="progress">
    	<div class="progress-bar" style="width: 0%;"></div>
	</div>
	
	<h5 style="text-align:center"><i style="color:#ccc"><small>Max File Size: 2 Mb - Display last 20 files</small></i></h5>

	<table id="uploaded-files" class="table">
		<tr>
			<th>File Name</th>
			<th>File Size</th>
			<th>File Type</th>
			<th>Uploaded By</th>
			<th>Action</th>
		</tr>
	</table>
</div>
<div id="form_text_paste" style="display: none;">
	<form class="pf-form pform_custom" action="<%= request.getContextPath() %>/wiseadm/fileUpload" method="post">
		<div class="pf-element pf-heading">
		<bean:message key='form.text.paste'/>
			<!-- <h3>使用剪貼文字上傳檔案</h3>
			<p>在電腦上用記事本打開文字檔 -> 全選 -> 複製 -> 在下方文字內容區塊貼上</p> -->
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label"><bean:message key='global.file.name'/></span>
			</label>
			<input type="hidden" name="tm" value="true">
			<input class="pf-field" style="width: 90%" type="text" name="fileName" />
		</div>
		<div class="pf-element">
			<label class="">
				<span class="pf-label"><bean:message key='global.text.content'/></span>
			</label>
			<textarea class="pf-field" rows="8" style="width:90%; margin: 5px;" name="textContent" ></textarea>
		</div>
		<div class="pf-element pf-buttons pf-centered">
			<input class="pf-button btn btn-primary" type="submit" name="submit" value="Submit" />
			<input class="pf-button btn btn-default" type="button" name="cancel" value="Cancel" />
		</div>
	</form>
</div>
<script>

function loadFileList() {
	$.ajax({
		data: {c:"l"},
		dataType: "json",
		url: "<%= request.getContextPath() %>/wiseadm/fileUpload",
		success: function(data) {
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
	                				"<a href='javascript:checkImportState(" + file.qaFile + ", " + index + ");'>Import</a>"
	                			  + "&nbsp;<a href='<%= request.getContextPath() %>/wiseadm/fileUpload?c=d&f="+index+"'>Delete</a>"
	                						))
	
	                		)//end $("#uploaded-files").append()
	            }); 
        	}
		}
	});
}

function checkImportState(qaFile, index) {
	if(qaFile) {
		$.ajax({
			dataType: 'json', 
			url: "<%= request.getContextPath() %>/wiseadm/qaImportCheckAjax.jsp",
			success: function(data) {
				if(data.pass) {
					window.location.href = '<%= request.getContextPath() %>/wiseadm/fileUpload?c=i&f=' + index;
				}
				else {
					alert(data.message);
				}
			}
		});
	}
	else {
		window.location.href = '<%= request.getContextPath() %>/wiseadm/fileUpload?c=i&f=' + index;
	}
}

$(document).ready(function() {
	
	loadFileList();
	
	$('#btnTextPaste').click(function(e) {
		var notice = new PNotify({
		    text: $('#form_text_paste').html(),
		    icon: false,
		    width: '50%',
		    height: '50%',
		    hide: false,
		    buttons: {
		        closer: false,
		        sticker: false
		    },
		    insert_brs: false
		});
		notice.get().find('form.pf-form').on('click', '[name=cancel]', function() {
		    notice.remove();
		}).submit(function() {
		    var filename = $(this).find('input[name=fileName]').val();
		    if (!filename) {
		        alert('<bean:message key="input.file.name"/>');
		        return false;
		    }
		    
		    $.post(
		    		$(this).attr('action'),
		    		$(this).serialize(),
		    		function(result) {
		    		    notice.update({
		    		        title: '<bean:message key="upload.success"/>',
		    		        text: '<bean:message key="global.file"/>[' + filename + ']<bean:message key="upload.success"/>',
		    		        icon: true,
		    		        width: PNotify.prototype.options.width,
		    		        hide: true,
		    		        buttons: {
		    		            closer: true,
		    		            sticker: true
		    		        },
		    		        type: 'success'
		    		    });
		    		    loadFileList();
		    		});
		    return false;
		});
	});
    
    $('#export').click(function() {
        window.open("<%= request.getContextPath() %>/wiseadm/exportQA.jsp?qaAlt=true");
    });
    
    $('#export_similar').click(function() {
        window.open("<%= request.getContextPath() %>/wiseadm/exportQS.jsp");
    });
});
</script>
</body> 
</html>
