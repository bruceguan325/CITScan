<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="java.util.*"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.admin.*"
%>
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

List<QAChannel> qaChannels = QAChannel.list(t.getId());
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='qa.richmenu.navbar.web'/></title>
<jsp:include page="../header-qa.jsp"></jsp:include>
<link href="../css/coverflow.css" rel="stylesheet">
<link rel="stylesheet" href="<%= request.getContextPath() %>/styles/cropper.min.css">
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-base/jquery-ui-1.10.4.custom.min.css" rel="stylesheet">
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/stylesheets/plugins/select2/select2.css">
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/select2/select2.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/dataTables.bootstrap.min.js"></script>
<script src="<%= request.getContextPath() %>/script/mustache.min.js"></script>
<script src="<%= request.getContextPath() %>/script/underscore-min.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone-min.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone.ModelBinder.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone.localStorage.min.js"></script>
<script src="../js/coverflow.js"></script>
<script src="<%= request.getContextPath() %>/script/cropper.min.js"></script>
<script src="<%= request.getContextPath() %>/script/jquery-cropper.min.js"></script>
<style>
.important {
	color: red;
}
</style>
<script type="x-tmpl-mustache" id="rich-message-list-item-tmpl">
	<td>{{mkey}}</td>
	<td><strong>{{channelCode}}</strong></td>
	<td>{{menuSeq}}</td>
	<td><strong>{{msgName}}</strong></td>
	<td style="word-wrap:break-word;">{{msgDesc}}</td>
	<td>{{formattedUpdateTime}}</td>
	<td>{{basicRichMenu}}</td>
	<td>
		<button class='btn btn-success btnEdit' title="<bean:message key='modify'/>"><span class="glyphicon glyphicon-pencil"></span></button>
		<% if (AdminGroup.U > 0) { %>
		{{#basicRichMenu}}
			<button class='btn btn-warning btnCancelMenu' title="<bean:message key='global.able.stop'/>"><span class="glyphicon glyphicon-star"></span></button>
		{{/basicRichMenu}}
		{{^basicRichMenu}}
			<button class='btn btn-primary btnDefaultMenu' title="<bean:message key='global.able'/>"><span class="glyphicon glyphicon-star"></span></button>
		{{/basicRichMenu}}
		<% } %>
		<% if (AdminGroup.D > 0) { %>
		<button class='btn btn-danger btnDelete' title="<bean:message key='delete'/>"><span class="glyphicon glyphicon-trash"></span></button>
		<% } %>
	</td>
</script>
<script type="x-tmpl-mustache" id="rich-message-detail-item-tmpl">
<div class="col-md-12" style="padding-top: 10px; padding-bottom: 10px;">
	<div class="col-md-12"> 
		<div class="col-md-2"> <bean:message key='operation'/> </div>
		<div class="col-md-8" style="margin-bottom: 5px;">
		<button class="btn btn-danger btnSave"><i class="glyphicon glyphicon-floppy-disk"></i> <bean:message key='submit'/></button>
		</div>
	</div>
	<div class="col-md-12"> 
		<div class="col-md-2"> MKEY </div>
		<div class="col-md-8">
		<input type="text" name="mkey" class="form-control RichMessageModel" placeholder="MKey">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> <bean:message key='qa.richmessage.message.name'/> </div>
		<div class="col-md-8">
		<input type="text" name="msgName" class="form-control RichMessageModel" placeholder="<bean:message key='qa.richmessage.message.name'/>（<bean:message key='qa.richmessage.for.manage.display'/>）">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> <bean:message key='qa.richmessage.message.desc'/> </div>
		<div class="col-md-8">
		<input type="text" name="msgDesc" class="form-control RichMessageModel" placeholder="<bean:message key='qa.richmessage.message.desc'/>（<bean:message key='qa.richmessage.for.management'/>）">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> <bean:message key='qa.richmenu.sequence'/> </div>
		<div class="col-md-8">
		<input type="number" min="1" max="5" name="menuSeq" class="form-control RichMessageModel" placeholder="<bean:message key='qa.richmenu.sequence'/>（<bean:message key='qa.richmenu.sequence.desc'/>）">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> Channel </div>
		<div class="col-md-8">
		<select name="channelCode" id="channelCode" class="form-control RichMessageModel" placeholder="Channel">
			<option value=""><bean:message key="global.pleaseSelect"/></option>
			<%
			for(QAChannel qaChannel : qaChannels) { 
				if (qaChannel.getType().equals(QAChannelType.PLAIN_TEXT) ||
					qaChannel.getType().equals(QAChannelType.PLAIN_TEXT_WITH_VOICE) ||
					qaChannel.getType().equals(QAChannelType.RICH_TEXT)) {
				%>
					<option value="<%= qaChannel.getCode() %>"><%= qaChannel.getName() %>(<%= qaChannel.getCode() %>)</option>
				<%
				}
			}
			%>
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> <bean:message key='qa.richmessage.message.format'/> </div>
		<div class="col-md-8">
			<div class="col-md-12">
			<input type="radio" name="msgType" data-target-div="richMessageType-imagemap-div" value="richmenu" class="RichMessageModel">Rich Menu
			</div>
		</div>
	</div>
	<div class="row MessageDetailChildView">
	</div>
</div>
</script>

<script>
$(document).ready(function() {
	$("#channelCode").select2({
		width: '100%' 
	});
});
</script>

<script type="x-tmpl-mustache" id="rich-message-imagemap-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-imagemap-div">
	<div class="col-md-12">
	<div class="imageMapTypeDiv">
		<img src="img/type_1_guide_s.png">
		<input type="radio" name="imageMapType" value="type1">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_2_guide_s.png">
		<input type="radio" name="imageMapType" value="type2">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_3_guide_s.png">
		<input type="radio" name="imageMapType" value="type3">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_4_guide_s.png">
		<input type="radio" name="imageMapType" value="type4">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_5_guide_s.png">
		<input type="radio" name="imageMapType" value="type5">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_6_guide_s.png">
		<input type="radio" name="imageMapType" value="type6">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_7_guide_s.png">
		<input type="radio" name="imageMapType" value="type7">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_8_guide_s.png">
		<input type="radio" name="imageMapType" value="type8">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_9_guide_s.png">
		<input type="radio" name="imageMapType" value="type9">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_10_guide_s.png">
		<input type="radio" name="imageMapType" value="type10">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_11_guide_s.png">
		<input type="radio" name="imageMapType" value="type11">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_12_guide_s.png">
		<input type="radio" name="imageMapType" value="type12">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_13_guide_s.png">
		<input type="radio" name="imageMapType" value="type13">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_14_guide_s.png">
		<input type="radio" name="imageMapType" value="type14">
	</div>
	<div style="display:none">
		<input type="radio" name="imageMapType" value="type99">
	</div>
	</div>
	<div class="col-md-12">
		<table class="table table-bordered table-striped">
			<tr>
				<td><bean:message key='qa.richmenu.image'/>(<span class="important">*</span>)</td>
				<td colspan=2>
                    <div>
                        <div id="div-thumbnail">
                            <img name="baseUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="baseUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>JPEG or PNG
					<br><span style="color:red;">The resolution must be 2500x1686/2500x843 pixels.</span>
					</small>
				</td>
			</tr>
			<tr class='actionA'>
				<td rowspan="15"><bean:message key='qa.richmessage.clickable.area'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span></td>
				<td width=20>A</td>
				<td>
					<input type="radio" name="actionAtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionAtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionAlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionAtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionAuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionAdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionB'>
				<td>B</td>
				<td>
					<input type="radio" name="actionBtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionBtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionBlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionBtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionBuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionBdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionC'>
				<td>C</td>
				<td>
					<input type="radio" name="actionCtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionCtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionClabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionCtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionCuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionCdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionD'>
				<td>D</td>
				<td>
					<input type="radio" name="actionDtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionDtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionDlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionDtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionDuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionDdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionE'>
				<td>E</td>
				<td>
					<input type="radio" name="actionEtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionEtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionElabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionEtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionEuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionEdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionF'>
				<td>F</td>
				<td>
					<input type="radio" name="actionFtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionFtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionFlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionFtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionFuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionFdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionG'>
				<td>G</td>
				<td>
					<input type="radio" name="actionGtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionGtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionGlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionGtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionGuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionGdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionH'>
				<td>H</td>
				<td>
					<input type="radio" name="actionHtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionHtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionHlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionHtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionHuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionHdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionI'>
				<td>I</td>
				<td>
					<input type="radio" name="actionItype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionItype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionIlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionItext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionIuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionIdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionJ'>
				<td>J</td>
				<td>
					<input type="radio" name="actionJtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionJtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionJlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionJtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionJuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionJdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionK'>
				<td>K</td>
				<td>
					<input type="radio" name="actionKtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionKtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionKlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionKtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionKuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionKdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionL'>
				<td>L</td>
				<td>
					<input type="radio" name="actionLtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionLtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionLlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionLtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionLuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionLdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionM'>
				<td>M</td>
				<td>
					<input type="radio" name="actionMtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionMtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionMlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionMtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionMuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionMdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionN'>
				<td>N</td>
				<td>
					<input type="radio" name="actionNtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionNtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionNlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionNtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionNuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionNdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
			<tr class='actionO'>
				<td>O</td>
				<td>
					<input type="radio" name="actionOtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
					<input type="radio" name="actionOtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
					<br>
					<label><bean:message key='qa.richmessage.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
					<input type="text" name="actionOlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
					<label><bean:message key='qa.richmessage.data'/></label>
					<input type="text" name="actionOtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
					<input type="text" name="actionOuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionOdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
				</td>
			</tr>
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-list-tmpl">
<table class="table table-striped table-bordered table-hover" id="mainTable">
<thead>
	<tr>
	<th><bean:message key='global.mkey'/></th>
	<th>Channel</th>
	<th><bean:message key='qa.richmenu.sequence'/></th>
	<th><bean:message key='global.name'/></th>
	<th><bean:message key='global.description'/></th>
	<th>Update time</th>
	<th><bean:message key='global.able'/></th>
	<th class='col-sm-3'><bean:message key='operation'/></th>
	</tr>
</thead>
<tbody>
</tbody>
</table>
<% if (AdminGroup.C > 0) { %>
<button class='btn btn-success btnCreate'><i class="glyphicon glyphicon-plus"></i> <bean:message key='global.add'/></button>
<% } %>
</script>
<style>
.msgImg {
    overflow: hidden;
    position: relative;
    width: 90px;
    height: 90px;
    -moz-box-sizing: border-box;
    box-sizing: border-box;
    border: 1px solid #e4e4e4;
    border-radius: 3px;
    box-shadow: inset 1px 1px 1px rgba(0, 0, 0, 0.05);
    text-align: center;
    margin: 0 10px 10px 0;
    background: white url(<%= request.getContextPath() %>/wiseadm/line/img/noImg_image_90x90.png) no-repeat center center;
}
.richMessageTypeDiv {
	padding: 25px;
}
.imageMapTypeDiv {
	display:inline-block;
	width: 100px;
}
.imageMapTypeDiv img {
	width: 100px;
}
.imageMapTypeDiv input {
	margin-left: 47px;
}
.ui-coverflow-wrapper{
    height:280px;
}
#div-thumbnail {
    display: inline-block;
    max-width: 400px;
}
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:9999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
</style>
<link rel="stylesheet" href="<%= request.getContextPath() %>/assets/stylesheets/plugins/select2/select2.css">
<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/select2/select2.js"></script>
<script>
$(document).ready(function() {
	$(".btnExport").click(function() {
		window.location = '<%=request.getContextPath() %>/wiseadm/webRM/exportRichMenu.jsp?action=export';
	});
});
</script>
</head>
<body>
<jsp:include page="../navbar-qa.jsp"></jsp:include>
<div id="top" class="container">
	<% if (AdminGroup.C > 0 && AdminGroup.U > 0) { %>
  	<br>
	<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/webRM/qaRichMenuUploadFile?action=import">
    	<table width="100%">
    		<tr>
    			<th width="10%"><input type="file" name="File1" size="20" maxlength="20"> </th>
    			<th width="30%">
    				<input class='btn btn-default' type="submit"value="<bean:message key='global.import.csv'/>">
    				<a href='#' class='btn btn-success btnExport'><bean:message key='search.export'/></a>
    			</th>
    		</tr>
    	</table>
	</form>
	<% } %>
	<br>
	<div id="rich-message-detail"></div>

	<div id="rich-message-list"></div>
	
	<div class='loading-panel'></div>
</div>
<script src="qaRichMenu.model.js"></script>
<script>
var detailView = null;

<%-- itemSaveCallback & target 變數僅給使用 iframe 帶出圖文編輯介面用，作用是當按下儲存時，會呼叫 itemSaveCallback 並將 target 及 response 傳遞過去 --%>
var itemSaveCallback = null;
<%= request.getParameter("target") != null ? "var target='" + request.getParameter("target") + "'" : "" %>

var ImagemapActionView = Backbone.View.extend({
	domNamePrefix: "",
	initialize: function(options) {
		this.modelBinder = new Backbone.ModelBinder();
		
		if (options != null) {
			this.domNamePrefix = options.domNamePrefix;
		}
	    this.listenTo(this.model, 'change', this.render);
	},
	render: function() {
		//console.log("MessageActionView render() start");
		textInputSelector = '[name=' + this.domNamePrefix + 'text]';
		dataInputSelector = '[name=' + this.domNamePrefix + 'data]';
		uriInputSelector = '[name=' + this.domNamePrefix + 'uri]';
		
		this.modelBinder.bind(this.model, this.el, {
			type: '[name=' + this.domNamePrefix + 'type]',
			label: '[name=' + this.domNamePrefix + 'label]',
			text: textInputSelector,
			data: dataInputSelector,
			uri: uriInputSelector,
		});
				
		var type = this.model.get("type");
		
		if (type == 'message') {
			$(textInputSelector).show();
			$(dataInputSelector).hide();
			$(uriInputSelector).hide();
		}
		else if (type == 'postback') {
			$(textInputSelector).hide();
			$(dataInputSelector).show();
			$(uriInputSelector).hide();
		}
		else if (type == 'uri') {
			$(textInputSelector).hide();
			$(dataInputSelector).hide();
			$(uriInputSelector).show();
		}
		//console.log("MessageActionView render() end");
		return this;
	}
});

var ImagemapView = Backbone.View.extend({
	template: $('#rich-message-imagemap-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	events: {
		'change input[name=imageMapType][type=radio]': 'onChangeImageMapType',
		'change input[name=baseUrl]': 'onChangeBaseImageUrl',
	},
	onChangeBaseImageUrl: function(e) {
		var imgSrc = $(e.target).val();
		this.$('img[name=baseUrl]').attr('src', imgSrc);
	},
	actionNameDomPrefix: ['actionA', 'actionB', 'actionC', 'actionD', 'actionE',
		'actionF', 'actionG', 'actionH', 'actionI', 'actionJ',
		'actionK', 'actionL', 'actionM', 'actionN', 'actionO'],
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    if(this.model.get("baseUrl")) {
			this.$('input[name=baseUrl]').val(this.model.get("baseUrl"));
			this.$('input[name=baseUrl]').trigger('change');
	    }
	    $thisEl = this.$el;
	    
		var areas = this.model.get("areas");
		if (areas == null) {
			areas = [];
			this.model.set("areas", areas);
		}
		
		for (var i=0; i < this.actionNameDomPrefix.length; i++) {
			if (i >= areas.length) {
				areas[i] = new ImagemapActionModel({}, {parse: true});
			}

			var actionModel = areas[i];
			var actionView = new ImagemapActionView({
				model: actionModel, 
				el: $("." + this.actionNameDomPrefix[i], this.$el),
				domNamePrefix: this.actionNameDomPrefix[i],
			});
			actionView.render();
		}
		
		var imgMapType = this.model.get("imageMapType");

		// 決定哪些 areas 設定區塊要隱藏，沒有就全部隱藏
		var bounds = this.model.theDefaultAreaSettings[imgMapType];

		if (typeof(bounds) != 'undefined') {
			for (var i=0; i < this.actionNameDomPrefix.length; i++) {
				if (i < bounds.length) {
					this.$('.' + this.actionNameDomPrefix[i]).show();
				}
				else {
					this.$('.' + this.actionNameDomPrefix[i]).hide();
				}
			}
		} else {
			for (var i=0; i < this.actionNameDomPrefix.length; i++) {
				this.$('.' + this.actionNameDomPrefix[i]).hide();
			}
		}

		this.modelBinder.bind(this.model, this.el, {
			imageMapType: '[name=imageMapType]',
			baseUrl: 'input[name=baseUrl]',
		});
				
		return this;
	},
	onChangeImageMapType: function(e) {
		var newImgMapType = $(e.target).val();
		console.log("onChangeImageMapType " + this.model.get("imageMapType") + " -> " + newImgMapType);
		this.model.set("imageMapType", newImgMapType);
		this.render();
		return false;
	}
});

var defaultImgHeight = 1686;
var fixImgHeight = 0;

var RichMessageDetailItemView = Backbone.View.extend({
	el: '#rich-message-detail',
	template: $('#rich-message-detail-item-tmpl').html(),
	childView: null,
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	events: {
		'click .btnSave': 'onSave',
		'change input[name=msgType][type=radio]': 'onChangeMsgType',
		'change input[type=file]': 'onChangeFile',
	},
	render: function() {
		//console.log(this.model.toJSON());
		var html = Mustache.render(this.template, this.model.toJSON());
		$this = this;
	    this.$el.html(html);
		
		var msgTmpl = this.model.get("msgTemplate");
		var msgType = this.model.get("msgType");

		if (typeof(this.childView) != 'undefined' && this.childView) {
			this.childView = null;	// 不能 remove，會連 div 一起被 remove
		}

		if (msgType == 'richmenu') {
			this.childView = new ImagemapView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		 
		if (this.childView != null) {
			this.childView.render();
		}

		this.modelBinder.bind(this.model, this.el, {
			mkey: '[name=mkey]',
			msgName: '[name=msgName]',
			msgDesc: '[name=msgDesc]',
			menuSeq: '[name=menuSeq]',
			channelCode: '[name=channelCode]',
			msgType: '[name=msgType]',
		});
		
	    return this;
	},
	onSave: function(e) {
		console.log('onSave');
		$('.loading-panel').show();
		var isValid = this.model.isValid();
		//console.log('isValid:' + isValid);
		if (isValid) {
			this.model.save(null, {
				wait: true, 
				success: function(model, response) {
				    new PNotify({
				        title: 'Success!',
				        text: response.name + '<bean:message key='global.save.success'/>',
				        type: 'success'
				    });
				    
				    if (itemSaveCallback != null) {
				    	itemSaveCallback(target, response);
				    }
					
				    msgs.fetch({ 
				        success: function() {
				    	    $('#rich-message-detail').hide();
				        }
				    });
				    
				    $('.loading-panel').hide();
				    
				    setTimeout( function() {
						location.reload();
					}, 1500);
			    },
			    error: function(model, response) {
			    	console.log(response);
			    	new PNotify({
					    title: 'Error!',
					    text: '<bean:message key='global.save.failed'/>' + (response.responseJSON.result ? ':' + response.responseJSON.result : ''),
					    type: 'error'
				    });
			    	$('.loading-panel').hide();
			    }
			});
		}
		else {
			new PNotify({
			    title: 'Error!',
			    text: this.model.validationError,
			    type: 'error'
			});
			$('.loading-panel').hide();
		}
	},
	onChangeMsgType: function(e) {
		var msgTmpl = this.model.get("msgTemplate");
		var newMsgType = $(e.target).val();
		console.log("onChangeMsgType " + this.model.get("msgType") + " -> " + newMsgType);
		this.model.set("msgType", newMsgType);
		
		if (typeof(msgTmpl) != 'undefined' && msgTmpl) {
			oldMsgType = null;
			
			if ("template" == msgTmpl.get("type")) {
				oldMsgType = msgTmpl.get("template").get("type");
			}
			else {
				oldMsgType = msgTmpl.get("type");
			}
			
			if (oldMsgType == newMsgType && this.childView != null) {
				// keep msgTmpl
				console.log("msgType not changed, keep it.");
			}
			else {
				msgTmpl.destroy();
				msgTmpl = null;
			}
		}

		if (msgTmpl == null) {
			if (newMsgType == 'richmenu') {
				msgTmpl = new ImagemapMessageModel({ type: "richmenu" }, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
		}
		else {
			console.log("msgTmpl reuse");
			console.log(msgTmpl);
		}
		
		this.render();
	}, 
	onChangeFile: function(e) {
		var f = e.target.files[0];
		if(f && f.type.match('image.*')) {
			if(f.size > (1024 * 1024)) {
				alert('<bean:message key="qa.richmenu.image.file.size"/>');
				$(e.target).val('');
				return;
			}
			var reader = new FileReader();
			reader.readAsDataURL(f);
			reader = new FileReader();
		    reader.onload = (function(theFile) {
		    	return function(env) {
		    		
		    		var data = this.result;
				    var img = new Image();
				    img.src = data;
				    img.onload = function() {
				    	if (fixImgHeight != 0 && this.height != fixImgHeight) {
				    		alert('<bean:message key="qa.richmenu.image.limit.size"/> 2500x' + fixImgHeight + ' pixels!');
				    		return;
				    	}
				    	if (this.width != 2500) {
				    		alert('<bean:message key="qa.richmenu.image.limit.size"/> 2500x1686/2500x843 pixels!');
					    	return;
				    	} else if (this.height != 843 && this.height != 1686) {
				    		alert('<bean:message key="qa.richmenu.image.limit.size"/> 2500x1686/2500x843 pixels!');
					    	return;
				    	} else {
				    		if($(e.target).siblings('input[name=baseUrl]').size() > 0){
				        		$(e.target).siblings('input[name=baseUrl]').val(env.target.result);
				        		$(e.target).siblings('input[name=baseUrl]').trigger('change');
				        	}
				        	
				        	if (defaultImgHeight == 843 && this.height == 1686) {
				        		defaultImgHeight = 1686;
				        		var target = $("input[name=imageMapType]:checked").val();
				        		$("input[name=imageMapType][value=type99]").attr('checked', 'checked').trigger("change");
			        			if(typeof target !== "undefined") {
				        			$("input[name=imageMapType][value="+target+"]").attr('checked', 'checked').trigger("change");
				        		}
				        	} else if (defaultImgHeight == 1686 && this.height == 843) {
				        		defaultImgHeight = 843;
				        		var target = $("input[name=imageMapType]:checked").val();
				        		$("input[name=imageMapType][value=type99]").attr('checked', 'checked').trigger("change");
			        			if(typeof target !== "undefined") {
				        			$("input[name=imageMapType][value="+target+"]").attr('checked', 'checked').trigger("change");
				        		}
				        	}
				    	}
				    };
		    	};
		    })(f);
		    reader.readAsDataURL(f);
		}
		else {
			alert('<bean:message key="qa.richmenu.image.only"/>');
			$(e.target).val('');
		}
	}
});

var RichMessageListItemView = Backbone.View.extend({
	tagName: 'tr',
	className: 'rich-message-list-item',
	template: $('#rich-message-list-item-tmpl').html(),
	initialize: function() {
	    this.listenTo(this.collection, 'sync change', this.render);
	    this.on('showEditForm', this.onShowEditForm, this);
	},
	events: {
		'click .btnEdit': 'onShowEditForm',
		'click .btnDelete': 'onDelete',
		'click .btnDefaultMenu': 'onDefaultMenu',
		'click .btnCancelMenu': 'onCancelMenu',
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    return this;
	},
	onShowEditForm: function(e) {
		console.log('onShowEditForm');
		var currentModel = this.model;
		
		setTimeout( function() {
		<% if (AdminGroup.U > 0) { %>
			$('.btnSave').attr("disabled", false);
		<% } else { %>
			$('.btnSave').attr("disabled", true);
		<% } %>
		}, 100);
		
		fixImgHeight = currentModel.get('fixHeight');
		setTimeout( function() {
			$('html,body').animate({scrollTop:$('#top').offset().top}, 0);
		}, 100);
		
		// Show detail view
		if (detailView != null) {
			detailView.undelegateEvents();
		}
		detailView = new RichMessageDetailItemView({model: currentModel});
		detailView.render();
		$('#rich-message-detail').show();
	},
	onDelete: function(e) {
		console.log('onDelete');
		var currentModel = this.model;
		swal({
			title: "Are you sure?",
			text: "Delete id : " + this.model.get('id') ,
			type: "warning",
			showCancelButton: true,
			confirmButtonClass: "btn-danger",
			confirmButtonText: "YES",
			cancelButtonText: "NO",
			closeOnConfirm: true
		},
		function(){
			console.log('onDelete');
			// Show detail view
			if (detailView != null) {
				detailView.undelegateEvents();
			}
	
			currentModel.destroy({wait: true, success: function() {
				setTimeout( function() {
					new PNotify({
					    title: 'Success!',
					    text: 'The rich menu has been deleted',
					    type: 'success'
					});
					
					msgs.fetch({ success: function() {
						$('#rich-message-detail').hide();
					}})
					
					setTimeout( function() {
						location.reload();
					}, 1500);
				}, 500);
			}});
		});
	},
	onDefaultMenu: function(e) {
		msg = this.model.recursiveToJSON();
		console.log(msg);
		
		$.ajax({
			url: 'qaRichMenu-ajax.jsp',
			dataType: 'JSON',
			method: 'POST',
			data: {
				action: 'update',
				type: 'setDefaultMenu',
				msg: JSON.stringify(msg)
			},
			success: function(result) {
				if (result.StatusCode && result.StatusCode == 200) {
					new PNotify({
					    title: 'Success!',
					    text: '<bean:message key="global.success"/>',
					    type: 'success'
					});
					
					setTimeout( function() {
						location.reload();
					}, 1500);
				}
				else {
					var msg = result.message;
					
					if (result.details) {
						for (var i=0; i < result.details.length; i++) {
							detail = result.details[i];
							msg += "\n" + detail.property + " => " + detail.message;
						}
					}
					new PNotify({
					    title: 'Error!',
					    text: msg,
					    type: 'error'
					});
				}
			}
		});
	},
	onCancelMenu: function(e) {
		msg = this.model.recursiveToJSON();
		console.log(msg);
		
		$.ajax({
			url: 'qaRichMenu-ajax.jsp',
			dataType: 'JSON',
			method: 'POST',
			data: {
				action: '"update"',
				type: 'cancelDefaultMenu',
				msg: JSON.stringify(msg)
			},
			success: function(result) {
				if (result.StatusCode && result.StatusCode == 200) {
					new PNotify({
					    title: 'Success!',
					    text: '<bean:message key="global.success"/>',
					    type: 'success'
					});
					
					setTimeout( function() {
						location.reload();
					}, 1500);
				}
				else {
					var msg = result.message;
					
					if (result.details) {
						for (var i=0; i < result.details.length; i++) {
							detail = result.details[i];
							msg += "\n" + detail.property + " => " + detail.message;
						}
					}
					new PNotify({
					    title: 'Error!',
					    text: msg,
					    type: 'error'
					});
				}
			}
		});
	}
});

var RichMessageListView = Backbone.View.extend({
	el: '#rich-message-list',
	template: $('#rich-message-list-tmpl').html(),
	initialize: function() {
	    this.listenTo(this.collection, 'sync change', this.render);
	},
	events: {
		'click .btnCreate': 'onShowCreateForm'
	},
	render: function() {
		var html = $(Mustache.render(this.template, this.collection.toJSON()));
		var list = $('tbody', html);
		var targetId;
		var targetItem;
		
	    var url = document.location.toString();
	    if (url.match('#')) {
	    	targetId = parseInt(url.split('#')[1]);
	    }
		
		this.collection.each(function(model) {
			var item = new RichMessageListItemView({model: model});
			if (typeof(targetId) != 'undefined' && model.get('id') === targetId) {
				targetItem = item;
			}
			list.append(item.render().$el);
		});
		
		if (typeof(targetItem) != 'undefined') {
			targetItem.trigger('showEditForm');
		}
		
	    this.$el.html(html);
	    $('#mainTable').DataTable({ "paging": false, "dom": 'fiplrtif' });
	    return this;
	},
	onShowCreateForm: function() {
		console.log('onShowCreateForm');
		
		setTimeout( function() {
		<% if (AdminGroup.C > 0) { %>
			$('.btnSave').attr("disabled", false);
		<% } else { %>
			$('.btnSave').attr("disabled", true);
		<% } %>
		}, 100);
		
		defaultImgHeight = 1686;
		fixImgHeight = 0;
		$('.btnSave').attr("disabled", true);
		$('html,body').animate({scrollTop:$('#top').offset().top}, 0);
		
		// Show detail view
		if (detailView != null) {
			detailView.undelegateEvents();
		}
		detailView = new RichMessageDetailItemView({model: new RichMessageModel()});
		detailView.render();
		
		$('#rich-message-detail').show();
	}
});

var msgs = new RichMessageCollection();
	
$(function() {
	var msgsView = new RichMessageListView({collection: msgs});
	msgs.fetch();

	// Hack for using #0 to show create form
	// Need itemSaveCallback declare as null at begining, and request parameter must have "target"
    var url = document.location.toString();
    if (url.match('#')) {
    	targetId = parseInt(url.split('#')[1]);
    	if (typeof(targetId) != 'undefined' && targetId == 0) {
    		msgsView.onShowCreateForm();	// 顯示新增的 form
    		$(msgsView.el).hide();  		// Dialog 新增模式隱藏圖文清單
    		itemSaveCallback = window.parent.setMkeyAndCloseDialog;
    	}
    }
});
</script>
</body>
</html>
