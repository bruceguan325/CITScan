<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.codec.binary.Base64" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.commons.fileupload.FileItem" %>
<%@ page import="org.apache.commons.fileupload.disk.DiskFileItemFactory" %>
<%@ page import="org.apache.commons.fileupload.servlet.ServletFileUpload" %>
<%@ page import="org.apache.commons.io.FilenameUtils" %>
<%@ page import="org.apache.lucene.index.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.solr.request.*" %>
<%@ page import="org.apache.solr.search.*" %>
<%@ page import="org.apache.wink.json4j.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.robot.RobotFormalAnswersSticker" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.util.XssHttpServletRequestWrapper" %>
<%@ page import="com.intumit.solr.util.XssStringFilter" %>
<%@ page import="com.intumit.syslog.OperationLogEntity" %>
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
<title><bean:message key='formal.answer.maintenance'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
<link rel="stylesheet" href="<%= request.getContextPath() %>/styles/cropper.min.css">
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

var _URL = window.URL || window.webkitURL;

$(document).ready(function() {
	
	var $container = $("#img-container");
	var $img = $("#img");
	var $preview = $("#preview");
	
	var $uploadWidth = $("#uploadWidth");
	var $uploadHeight = $("#uploadHeight");
	
	var options = {
			autoCropArea: 1,
			preview: '#preview',
            crop: function(e) {
                $uploadWidth.val(Math.round(e.detail.width));
                $uploadHeight.val(Math.round(e.detail.height));
            }
	};
	
	$("#file").change(function (e) {
		var file;
		if ((file = this.files[0])) {
			$img.cropper("destroy").attr("src", _URL.createObjectURL(file)).cropper(options);
			
			$img.load(function() {
				$uploadWidth.val(this.width);
				$uploadHeight.val(this.height);
				
				var sw = this.width;
				var sh = this.height;
				
				if (sw >= 300) {
					sh = Math.round(sh * (300 / sw));
					sw = 300;
				}
				
				$container.width(sw).height(sh);
				$preview.width(sw).height(sh);
			});
		}
	});
	
	$img.hide();
});

function addTag() {
	var canvas = $("#img").cropper("getCroppedCanvas");
	$("#img-string").val(canvas.toDataURL());
	$("#add-form").submit();
}
</script> 

</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<script src="<%= request.getContextPath() %>/script/cropper.min.js"></script>
<script src="<%= request.getContextPath() %>/script/jquery-cropper.min.js"></script>
<div class="container">
<%
	XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
	boolean isMultipart = ServletFileUpload.isMultipartContent(request);
	String valValue =  "";
	String imgString = "";
	Integer width = 100;
	Integer height = 100;
	Integer opLogId = (Integer) xssReq.getFakeAttribute("opLogId");
	OperationLogEntity log = opLogId == null ? null : OperationLogEntity.get(opLogId);
	if(log != null && isMultipart){
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		List<FileItem> items = upload.parseRequest(request);
	    for (FileItem item : items) {
	        if (item.isFormField() && "key".equalsIgnoreCase(item.getFieldName())) {
	            valValue = item.getString();
	        }
	        else if(item.isFormField() && "width".equalsIgnoreCase(item.getFieldName())) {
	        	width = Integer.parseInt(StringUtils.defaultString(item.getString(), "100"));
	        }
	        else if(item.isFormField() && "height".equalsIgnoreCase(item.getFieldName())) {
	        	height = Integer.parseInt(StringUtils.defaultString(item.getString(), "100"));
	        }
	        else if (item.isFormField() && "img-string".equalsIgnoreCase(item.getFieldName())) {
	        	imgString = item.getString();
	        }
	    }
	    RobotFormalAnswersSticker.saveFile(t, valValue, imgString, width, height);
	    JSONObject body = new JSONObject();
	    body.put("valValue", valValue);
	    body.put("imgString", imgString);
	    body.put("width", width);
	    body.put("height", height);
	    log.setParameters(body.toString(4));
	    log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
		log.update();
	}

	if (log != null && request.getParameterMap().containsKey("key")) {
		String[] reqKeys = xssReq.getParameterValues("key");
		String[] reqVals = xssReq.getParameterValues("value");
		String[] reqWidths = xssReq.getParameterValues("width");
		String[] reqHeights = xssReq.getParameterValues("height");

		Map<String, RobotFormalAnswersSticker> columns = new HashMap<String, RobotFormalAnswersSticker>();
		for(int i = reqKeys.length - 1 ; i >= 0 ; i--){
			String valStr = StringUtils.isEmpty(reqVals[i]) ? "" : reqVals[i];
			RobotFormalAnswersSticker sticker = new RobotFormalAnswersSticker(t.getId(), reqKeys[i], valStr, Integer.parseInt(StringUtils.defaultString(reqWidths[i], "100")), Integer.parseInt(StringUtils.defaultString(reqHeights[i], "100")));
			columns.put(reqKeys[i], sticker);
		}

		if(columns.size() > 0){
			RobotFormalAnswersSticker.saveMap(t.getId(), columns);
		}

		String[] clear = xssReq.getParameterValues("clear");
		if(clear != null){
			for (String clearField:clear) {
				if(StringUtils.isNotBlank(clearField)){
					RobotFormalAnswersSticker.delete(t.getId(), clearField);
				}
			}
			log.setEvent("delete");
		}
		log.setStatusMessage(OperationLogEntity.Status.SUCCESS);
		log.update();
	%>
	<div class="alert alert-warning" role="alert"><bean:message key='modify'/><bean:message key='global.success'/></div>
	<%
	}
%>
<BR>
<%
Map<String, RobotFormalAnswersSticker> keyVal = RobotFormalAnswersSticker.getKeyValueMap(t.getId());
%>
<bean:message key='manual.increase'/>：<BR>
<form id="add-form" action="<%= request.getContextPath() %>/wiseadm/qaFormalAnswerSticker.jsp?action=save" method="post" enctype="multipart/form-data">
	<TABLE width="100%">
		<tr>
			<th width="20%">TAG：<input type="text" value="" name="key" /></th>
			<th width="10%">Width：<input type="text" style="width:60px" value="" name="width" id="uploadWidth" /></th>
			<th width="10%">Height：<input type="text" style="width:60px" value="" name="height" id="uploadHeight" /></th>
			<th width="40%">STICKER：<input type="file" name="file" id="file" /></th>
			<th width="10%"><input type="button" value="<bean:message key='manual.increase'/>" class="btn btn-success" onclick="addTag()"></th>
		</tr>
        <tr>
            <td colspan="3">
                <div id="img-container">
                    <img id="img" src="" alt="Picture">
                    <input type="hidden" id="img-string" name="img-string" value="" />
                </div>
            </td>
            <td colspan="2">
                <div id="preview" style="overflow: hidden;"></div>
            </td>
        </tr>
	</TABLE>
</form>	
<bean:message key='have.set'/>：<BR>	
<form method="POST" action="<%= request.getContextPath() %>/wiseadm/qaFormalAnswerSticker.jsp">
	<input type="hidden" value="update" name="action" />
	<TABLE width="100%" class="table table-striped table-bordered">
		<tr>
			<th width="30%">TAG</th>
			<th width="20%">width x height</th>
			<th width="50%">STICKER</th>
		</tr>
		<%
			RobotFormalAnswersSticker sticker;
			for(String key:keyVal.keySet()){
				sticker = keyVal.get(key);
				
				int sw = sticker.getWidth() != null ? sticker.getWidth() : 100;
				int sh = sticker.getWidth() != null ? sticker.getHeight() : 100;
				
				if (sw >= 300) {
					sh = (int)(sh * (300f / sw));
					sw = 300;
				}
		%>
			<tr>
				<td><%= key %></td>
				<td>
				  <input type="text" style="width:60px;" value="<%= sticker.getWidth() != null ? sticker.getWidth() : 100 %>" name="width"> X <input type="text" style="width:60px;" value="<%= sticker.getHeight() != null ? sticker.getHeight() : 100 %>" name="height">
                </td>
				<td>
					<input type="hidden" value='<%= key %>' name="key" />
					<input type="hidden" value='<%= sticker.getAnswers() %>' name="value" />
					<img height="<%= sh %>" width="<%= sw %>" src='<%= request.getContextPath() + "/img/sticker/" + sticker.getAnswers()%>'>
					&nbsp;<bean:message key='global.eliminate'/><input type='checkbox' name='clear' value='<%= key %>' >
				</td>
			</tr>
		<%

			}
		%>
		<tr>
			<td width="30%"></td>
			<td width="20%"><input type="submit" class="btn btn-warning" value="<bean:message key='submit'/>"></td>
			<td width="50%"></td>
		</tr>
	</TABLE>
</form>
</div>
</body> 
</html>
