<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.io.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="org.apache.struts.Globals"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.connector.web.*"
import="com.intumit.solr.admin.*"
import="com.intumit.message.MessageUtil"
import="org.apache.wink.json4j.*"
import="org.apache.http.impl.client.CloseableHttpClient"
import="org.apache.http.impl.client.HttpClientBuilder"
import="org.apache.http.client.methods.HttpGet"
import="org.apache.http.client.methods.HttpPost"
import="org.apache.http.entity.StringEntity"
import="org.apache.http.HttpResponse"
import="com.intumit.solr.tenant.Tenant"
import="com.intumit.solr.util.XssHttpServletRequestWrapper"
import="com.intumit.message.MessageUtil"
%>
<%!
final static char[] alphebet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
%>
<%
XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(request);
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
String errorMsg = "";
Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);
String contextPath = request.getContextPath();

boolean isAlreadyBinded = false;

UserClue uc = UserClue.getByAdminUserId(t.getId(), user.getId());
String code = xssReq.getParameter("code");
String state = xssReq.getParameter("state");

isAlreadyBinded = uc != null && uc.getCookieUserId() != null;
Object templateMkey = request.getAttribute("template_mkey");
pageContext.setAttribute("alphebet", alphebet);
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='qa.richmessage.navbar.web'/></title>
<jsp:include page="../header-qa.jsp"></jsp:include>
<link href="../css/coverflow.css" rel="stylesheet">
<link rel="stylesheet" href="<%= request.getContextPath() %>/styles/cropper.min.css">
<link rel="stylesheet" href="<%= request.getContextPath() %>/styles/swiper.min.css">
<link href="<%=request.getContextPath()%>/wiseadm/css/ui-base/jquery-ui-1.10.4.custom.min.css" rel="stylesheet">
<script src="<%= request.getContextPath() %>/wiseadm/js/jquery.dataTables.min.js"></script>
<script src="<%= request.getContextPath() %>/wiseadm/js/dataTables.bootstrap.min.js"></script>
<script src="<%= request.getContextPath() %>/script/mustache.min.js"></script>
<script src="<%= request.getContextPath() %>/script/underscore-min.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone-min.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone.ModelBinder.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone.localStorage.min.js"></script>
<script src="<%= request.getContextPath() %>/script/swiper.min.js"></script>
<script src="../js/coverflow.js"></script>
<script src="<%= request.getContextPath() %>/script/cropper.min.js"></script>
<script src="<%= request.getContextPath() %>/script/jquery-cropper.min.js"></script>
<script src="<%= request.getContextPath() %>/ckeditor/ckeditor.js"></script>
<script type="x-tmpl-mustache" id="rich-message-list-item-tmpl">
	<td>{{mkey}}</td>
	<td><strong>{{msgTypeName}}</strong></td>
	<td><strong>{{msgName}}</strong></td>
	<td style="word-wrap:break-word;">{{msgDesc}}</td>
	<td>{{formattedUpdateTime}}</td>
	<td>{{expireTime}}</td>
	<td>
		<button class='btn btn-success btnEdit' title="<bean:message key='modify'/>"><span class="glyphicon glyphicon-pencil"></span></button>
		<% if (AdminGroup.C > 0) { %>
		<button class='btn btn-default btnDuplicate' title="<bean:message key='copy'/>"><span class="glyphicon glyphicon-duplicate"></span></button>
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
		&nbsp;&nbsp;<button class="btn btn-info btnSendToMe hide"><i class="glyphicon glyphicon-screenshot "></i> <bean:message key='global.preview'/></button>
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
		<input type="text" name="msgName" class="form-control RichMessageModel" placeholder="<bean:message key='qa.richmessage.message.name'/>（<bean:message key='qa.richmessage.for.management'/>）">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> <bean:message key='qa.richmessage.message.desc'/> </div>
		<div class="col-md-8">
		<input type="text" name="msgDesc" class="form-control RichMessageModel" placeholder="<bean:message key='qa.richmessage.message.desc'/>（<bean:message key='qa.richmessage.for.management'/>）">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> <bean:message key='qa.richmessage.message.format'/> </div>
		<div class="col-md-8">
			<div class="col-md-12">

            <!--
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-imagemap-div" value="imagemap" class="RichMessageModel">Imagemap
			&nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-imageCarousel-div" value="imageCarousel" class="RichMessageModel">圖片輪播
            -->

            				  <input type="radio" name="msgType" data-target-div="richMessageType-buttons-div" value="buttons" class="RichMessageModel">Buttons
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-text-div" value="text" class="RichMessageModel">Text
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-carousel-div" value="carousel" class="RichMessageModel">Carousel
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-stretch-div" value="stretch" class="RichMessageModel">Stretch
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-threegrid-div" value="threegrid" class="RichMessageModel">3 Grid
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-twogrid-div" value="twogrid" class="RichMessageModel">2 Grid
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-textwithoutbutton-div" value="textwithoutbutton" class="RichMessageModel">Text Without Button
            <br>			  <input type="radio" name="msgType" data-target-div="richMessageType-textwithbutton-div" value="textwithbutton" class="RichMessageModel">Text With Button
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-onegrid-div" value="onegrid" class="RichMessageModel">1 Grid
			&nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-quickReplies-div" value="quickReplies" class="RichMessageModel">Quick Replies
            
			<!--
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-optionsWithIconText-div" value="optionsWithIconText" class="RichMessageModel">按鈕
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-optionsWithImageTitle-div" value="optionsWithImageTitle" class="RichMessageModel">圖卡
            &nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-imageModal-div" value="imageModal" class="RichMessageModel">圖片彈跳視窗
			<br>			  <input type="radio" name="msgType" data-target-div="richMessageType-textModal-div" value="textModal" class="RichMessageModel">文字彈跳視窗
			&nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-alert-div" value="alert" class="RichMessageModel">提醒視窗
            -->

			</div>
		</div>
	</div>
	<div class="row MessageDetailChildView">
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-text-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-text-div">
    <div class="col-md-12">
        <div style="text-align:center">
            <img src="img/image-text.png" alt="" style="height:237px">
        </div>
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
        <table class="table table-bordered table-striped">
            <tr>
                <td>訊息內文*</td>
                <td colspan=2>
                    <textarea name="text" style="width:100%;max-width:620px;height:220px"></textarea>
                    <br>
                    <small>Required</small>
                </td>
            </tr>
        </table>    
    </div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-buttons-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-buttons-div">
	<div class="col-md-12">
        <div style="text-align:center">
            <img src="img/image-buttons.png" alt="" style="height:237px">
        </div>
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
		<table class="table table-bordered table-striped">
            <tr>
                <td>訊息內文*</td>
                <td colspan=2>
                    <textarea name="text" style="width:100%;max-width:620px;height:220px"></textarea>
                    <br>
                    <small>Required</small>
                </td>
            </tr>
            <!--
			<tr>
				<td><bean:message key='qa.richmessage.image'/></td>
				<td colspan=2>
					<div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>Image URL (Max: 1000 characters)
					<br>Ratio (WxH) 1.81:1
					<br>GIF or JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
            -->
            <c:forEach var="i" begin="0" end="9">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="10"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-stretch-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-stretch-div">
    <div class="col-md-12">
        <div style="text-align:center">
            <img src="img/image-stretch.png" alt="" style="height:237px">
        </div>
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
        <table class="table table-bordered table-striped">
            <tr>
                <td>主內文 *</td>
                <td colspan=2>
                    <textarea name="text" style="width:100%;max-width:620px;height:220px"></textarea>
                    <br>
                    <small>Required</small>
                </td>
            </tr>
            <tr>
                <td>延伸按鈕標題 </td>
                <td colspan=2>
                    <input type="text" name="stretchTitle" class="form-control">
                </td>
            </tr>
            <tr>
                <td>延伸內文</td>
                <td colspan=2>
                    <textarea name="stretchText" style="width:100%;max-width:620px;height:220px"></textarea>
                </td>
            </tr>
            
            <c:forEach var="i" begin="0" end="9">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="10"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>
        </table>    
    </div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-carousel-column-tmpl">
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120"><bean:message key='qa.richmessage.message.title'/>*<img name="colMarkImg" style="width: 120px;" src="img/carousel-col-default.png"></td>
				<td colspan=2><input type="text" name="title" class="form-control" maxlength="40" placeholder="<bean:message key='qa.richmessage.card.hint'/>">
					<small>
						Required
						<br>Max: 40 characters					
					</small>
				</td>	
			</tr>
			<tr>
				<td><bean:message key='qa.richmessage.message.subtitle'/><br></td>
				<td colspan=2>
					<textarea name="text" style="width:100%;max-width:620px;height:220px"></textarea>
				</td>
			</tr>
            <tr>
                <td><bean:message key='qa.richmessage.image'/></td>
                <td colspan=2>
                    <!--
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
                    <input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
                    <div class="msgImg hide"></div><input type="file" class="form-control">
                    <small>Image URL (Max: 1000 characters)
                    <br>Ratio (WxH) 1.81:1
                    <br>GIF or JPEG or PNG
                    <br>Max width: 1024px
                    <br>Max: 1 MB
                    </small>
                    -->
                    <input type="text" class="form-control" name="thumbnailImageUrl" value="">
                    <small>Image's file name</small>
                </td>
            </tr>
            <c:forEach var="i" begin="0" end="4">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="5"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>
		</table>	
</script>

<script type="x-tmpl-mustache" id="rich-message-carousel-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-carousel-div">
	<div class="col-md-12">
	<div class="coverflow" style="display: none;">
	<% // template 太長且重複，用 jsp loop 產生
	int maxCol = 15;
	for (int i=0; i < maxCol; i++) {
		String prefix = "carouselCol" + 0;
	%>
		<img data-col="<%=i%>" src="img/carousel-col-<%=i+1%>.png" style="height:237px">
	<%
	}
	%>
	</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>
	</div>
    <div class="col-md-12">
        <div class="col-md-2">固定主內文*</div>
        <div class="col-md-8"><input type="text" name="fixedTitle" class="form-control">
            <small>
                Required                 
            </small>
        </div>
    </div>
	<div name="carouselCol" class="col-md-12 carouselCol">
	</div>
</div>
</script>

<!-- one grid 圖文內容維護form view -->
<script type="x-tmpl-mustache" id="rich-message-onegrid-column-tmpl">
<table class="table table-bordered table-striped">
    <tr>
        <td><bean:message key='qa.richmessage.image'/></td>
        <td colspan=2>
            <!--
            <div>
                <div id="div-thumbnail">
                    <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                </div>
                <div id="preview" class="pull-right">
                    <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                </div>
            </div>
            <input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
            <div class="msgImg hide"></div><input type="file" class="form-control">
            <small>Image URL (Max: 1000 characters)
            <br>Ratio (WxH) 1.81:1
            <br>GIF or JPEG or PNG
            <br>Max width: 1024px
            <br>Max: 1 MB
            </small>
            -->
            <input type="text" class="form-control" name="thumbnailImageUrl" value="">
            <small>Image's file name</small>
        </td>
    </tr>
    <tr>
        <td>圖片標題(MLI卡號資訊)</td>
        <td colspan=2>
            <input type="text" name="picTitle" class="form-control" maxlength="40">
            <small>Max: 40 characters</small>
        </td>
    </tr>
    <tr>
        <td width="120">
        內文標題*<img name="colMarkImg" style="width: 120px;" src="img/Type10-1gridcard/Type10-1gridcard-default.png">
        </td>
        <td colspan=2>
            <input type="text" name="title" class="form-control" maxlength="40" placeholder="<bean:message key='qa.richmessage.card.hint'/>">
            <small>
                Required
                <br>Max: 40 characters                  
            </small>
        </td>   
    </tr>
    <tr>
        <td>內文1-1</td>
        <td colspan=2>
            <textarea name="text1" id="text1" style="width:100%;max-width:620px;height:220px"></textarea>
        </td>
    </tr>
    <tr class='actionA'>
        <td><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
        <td>
            <input type="radio" name="actionAtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
            <input type="radio" name="actionAtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
            <input type="radio" name="actionAtype" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
            <input type="radio" name="actionAtype" value="call"><bean:message key='qa.richmessage.call'/>                   
            <br>
            <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
            <input type="text" name="actionAlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
            <label><bean:message key='qa.richmessage.data'/></label>
            <input type="text" name="actionAtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
            <input type="text" name="actionAuri" class="form-control" placeholder="http://xxx.xxx.xxx">
            <input type="text" name="actionAdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
            <input type="text" name="actionAcall" class="form-control" placeholder="02-12345678">                   
        </td>
    </tr>
    <tr>
        <td>內文1-2</td>
        <td colspan=2>
            <input type="text" name="text2" class="form-control" maxlength="40">
            <small>Max: 40 characters</small>
        </td>
    </tr>
</table>
</script>

<!-- OneGridTemplateParentView -one grid 示意圖、固定內文 view -->
<script type="x-tmpl-mustache" id="rich-message-onegrid-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-onegrid-div">
    <div class="col-md-12">
    <div class="coverflow" style="display: none;">
        <c:forEach var="i" begin="1" end="15">
            <img data-col="${i-1}" src="img/Type10-1gridcard/Type10-1gridcard-${i}.png" style="width:150px;height:237px">
        </c:forEach>
    </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2">固定主內文*</div>
        <div class="col-md-8"><input type="text" name="fixedTitle" class="form-control" maxlength="40">
            <small>
                Required
                <br>Max: 40 characters                  
            </small>
        </div>
    </div>
    <div name="onegridCol" class="col-md-12 onegridCol">
    </div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-threegrid-column-tmpl">
        <table class="table table-bordered table-striped">
            <tr>
                <td width="120">內文標題*<img name="colMarkImg" style="width: 120px;" src="img/carousel-col-default.png"></td>
                <td colspan=2><input type="text" name="title" class="form-control" maxlength="40" placeholder="<bean:message key='qa.richmessage.card.hint'/>">
                    <small>
                        Required
                        <br>Max: 40 characters                  
                    </small>
                </td>   
            </tr>
            <tr>
                <td><bean:message key='qa.richmessage.image'/></td>
                <td colspan=2>
                    <!--
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
                    <input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
                    <div class="msgImg hide"></div><input type="file" class="form-control">
                    <small>Image URL (Max: 1000 characters)
                    <br>Ratio (WxH) 1.81:1
                    <br>GIF or JPEG or PNG
                    <br>Max width: 1024px
                    <br>Max: 1 MB
                    </small>
                    -->
                    <input type="text" class="form-control" name="thumbnailImageUrl" value="">
                    <small>Image's file name</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 1</td>
                <td colspan=2>
                    <input type="text" name="text1" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 2</td>
                <td colspan=2>
                    <input type="text" name="text2" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 3</td>
                <td colspan=2>
                    <input type="text" name="text3" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 4</td>
                <td colspan=2>
                    <input type="text" name="text4" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 5</td>
                <td colspan=2>
                    <input type="text" name="text5" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 6</td>
                <td colspan=2>
                    <input type="text" name="text6" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
            <tr class='actionA'>
                <td><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                <td>
                    <input type="radio" name="actionAtype" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                    <input type="radio" name="actionAtype" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                    <input type="radio" name="actionAtype" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                        <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                    <input type="radio" name="actionAtype" value="call"><bean:message key='qa.richmessage.call'/>                   
                    <br>
                    <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                    <input type="text" name="actionAlabel" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                    <label><bean:message key='qa.richmessage.data'/></label>
                    <input type="text" name="actionAtext" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                    <input type="text" name="actionAuri" class="form-control" placeholder="http://xxx.xxx.xxx">
                    <input type="text" name="actionAdata" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                    <input type="text" name="actionAcall" class="form-control" placeholder="02-12345678">                   
                </td>
            </tr>
            <tr>
                <td>內文 - 7</td>
                <td colspan=2>
                    <input type="text" name="text7" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
        </table>    
</script>

<script type="x-tmpl-mustache" id="rich-message-threegrid-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-threegrid-div">
    <div class="col-md-12">
    <div class="coverflow" style="display: none;">
    <% // template 太長且重複，用 jsp loop 產生
    maxCol = 15;
    for (int i=0; i < maxCol; i++) {
        String prefix = "threegridCol" + 0;
    %>
        <img data-col="<%=i%>" src="img/image-3grid-col-<%=i+1%>.png" style="height:237px">
    <%
    }
    %>
    </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2">固定主內文*</div>
        <div class="col-md-8"><input type="text" name="fixedTitle" class="form-control" maxlength="40">
            <small>
                Required
                <br>Max: 40 characters                  
            </small>
        </div>
    </div>
    <div name="threegridCol" class="col-md-12 threegridCol">
    </div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-twogrid-column-tmpl">
        <table class="table table-bordered table-striped">
            <tr>
                <td width="120">內文標題*<img name="colMarkImg" style="width: 120px;" src="img/carousel-col-default.png"></td>
                <td colspan=2><input type="text" name="title" class="form-control" maxlength="40" placeholder="<bean:message key='qa.richmessage.card.hint'/>">
                    <small>
                        Required
                        <br>Max: 40 characters                  
                    </small>
                </td>   
            </tr>
            <tr>
                <td><bean:message key='qa.richmessage.image'/></td>
                <td colspan=2>
                    <!--
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
                    <input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
                    <div class="msgImg hide"></div><input type="file" class="form-control">
                    <small>Image URL (Max: 1000 characters)
                    <br>Ratio (WxH) 1.81:1
                    <br>GIF or JPEG or PNG
                    <br>Max width: 1024px
                    <br>Max: 1 MB
                    </small>
                    -->
                    <input type="text" class="form-control" name="thumbnailImageUrl" value="">
                    <small>Image's file name</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 1</td>
                <td colspan=2>
                    <input type="text" name="text1" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
            <tr>
                <td>內文 - 2</td>
                <td colspan=2>
                    <input type="text" name="text2" class="form-control" maxlength="40">
                    <small>Max: 40 characters</small>
                </td>
            </tr>
        </table>    
</script>

<script type="x-tmpl-mustache" id="rich-message-twogrid-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-twogrid-div">
    <div class="col-md-12">
    <div class="coverflow" style="display: none;">
    <% // template 太長且重複，用 jsp loop 產生
    maxCol = 15;
    for (int i=0; i < maxCol; i++) {
        String prefix = "twogridCol" + 0;
    %>
        <img data-col="<%=i%>" src="img/image-2grid-col-<%=i+1%>.png" style="height:237px">
    <%
    }
    %>
    </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2">固定主內文*</div>
        <div class="col-md-8"><input type="text" name="fixedTitle" class="form-control" maxlength="40">
            <small>
                Required
                <br>Max: 40 characters                  
            </small>
        </div>
    </div>
    <div name="twogridCol" class="col-md-12 twogridCol">
    </div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-textwithoutbutton-column-tmpl">
        <table class="table table-bordered table-striped">
            <tr>
                <td width="120">內文標題*<img name="colMarkImg" style="width: 120px;" src="img/carousel-col-default.png"></td>
                <td colspan=2><input type="text" name="title" class="form-control" maxlength="40" placeholder="<bean:message key='qa.richmessage.card.hint'/>">
                    <small>
                        Required
                        <br>Max: 40 characters                  
                    </small>
                </td>   
            </tr>
            <tr>
                <td><bean:message key='qa.richmessage.image'/></td>
                <td colspan=2>
                    <!--
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
                    <input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
                    <div class="msgImg hide"></div><input type="file" class="form-control">
                    <small>Image URL (Max: 1000 characters)
                    <br>Ratio (WxH) 1.81:1
                    <br>GIF or JPEG or PNG
                    <br>Max width: 1024px
                    <br>Max: 1 MB
                    </small>
                    -->
                    <input type="text" class="form-control" name="thumbnailImageUrl" value="">
                    <small>Image's file name</small>
                </td>
            </tr>
            <tr>
                <td>內文</td>
                <td colspan=2>
                    <textarea name="text" style="width:100%;max-width:620px;height:220px"></textarea>
                </td>
            </tr>
        </table>    
</script>

<script type="x-tmpl-mustache" id="rich-message-textwithoutbutton-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-textwithoutbutton-div">
    <div class="col-md-12">
    <div class="coverflow" style="display: none;">
    <% // template 太長且重複，用 jsp loop 產生
    maxCol = 15;
    for (int i=0; i < maxCol; i++) {
        String prefix = "textwithoutbuttonCol" + 0;
    %>
        <img data-col="<%=i%>" src="img/image-textwithoutbutton-col-<%=i+1%>.png" style="height:237px">
    <%
    }
    %>
    </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2">固定主內文*</div>
        <div class="col-md-8"><input type="text" name="fixedTitle" class="form-control" maxlength="40">
            <small>
                Required
                <br>Max: 40 characters                  
            </small>
        </div>
    </div>
    <div name="textwithoutbuttonCol" class="col-md-12 textwithoutbuttonCol">
    </div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-textwithbutton-column-tmpl">
        <table class="table table-bordered table-striped">
            <tr>
                <td width="120">內文標題*<img name="colMarkImg" style="width: 120px;" src="img/carousel-col-default.png"></td>
                <td colspan=2><input type="text" name="title" class="form-control" maxlength="40" placeholder="<bean:message key='qa.richmessage.card.hint'/>">
                    <small>
                        Required
                        <br>Max: 40 characters                  
                    </small>
                </td>   
            </tr>
            <tr>
                <td><bean:message key='qa.richmessage.image'/></td>
                <td colspan=2>
                    <!--
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
                    <input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
                    <div class="msgImg hide"></div><input type="file" class="form-control">
                    <small>Image URL (Max: 1000 characters)
                    <br>Ratio (WxH) 1.81:1
                    <br>GIF or JPEG or PNG
                    <br>Max width: 1024px
                    <br>Max: 1 MB
                    </small>
                    -->
                    <input type="text" class="form-control" name="thumbnailImageUrl" value="">
                    <small>Image's file name</small>
                </td>
            </tr>
            <tr>
                <td>內文 </td>
                <td colspan=2>
                    <textarea name="text" style="width:100%;max-width:620px;height:220px"></textarea>
                </td>
            </tr>
            <c:forEach var="i" begin="0" end="4">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="5"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>
        </table>    
</script>

<script type="x-tmpl-mustache" id="rich-message-textwithbutton-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-textwithbutton-div">
    <div class="col-md-12">
    <div class="coverflow" style="display: none;">
    <% // template 太長且重複，用 jsp loop 產生
    maxCol = 15;
    for (int i=0; i < maxCol; i++) {
        String prefix = "textwithbuttonCol" + 0;
    %>
        <img data-col="<%=i%>" src="img/image-textwithbutton-col-<%=i+1%>.png" style="height:237px">
    <%
    }
    %>
    </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
    </div>
    <div class="col-md-12">
        <div class="col-md-2">固定主內文*</div>
        <div class="col-md-8"><input type="text" name="fixedTitle" class="form-control" maxlength="40">
            <small>
                Required
                <br>Max: 40 characters                  
            </small>
        </div>
    </div>
    <div name="textwithbuttonCol" class="col-md-12 textwithbuttonCol">
    </div>
</div>
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
	</div>
	<div class="col-md-12">
		<table class="table table-bordered table-striped">
			<tr>
				<td><bean:message key='qa.richmessage.alt.text'/></td>
				<td colspan=2><input type="text" name="altText" class="form-control">
					<small>
					Max: 400 characters
					</small>
				</td>
			</tr>
			<tr>
				<td><bean:message key='qa.richmessage.background.image'/></td>
				<td colspan=2>
                    <div>
                        <div id="div-thumbnail">
                            <img name="baseUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="baseUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>JPEG or PNG
					<br>The resolution must be 1040x1040 pixels.
					</small>
				</td>
			</tr>
            <c:forEach var="i" begin="0" end="14">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="15"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>
		</table>	
	</div>
</div>
</script>
<script type="x-tmpl-mustache" id="rich-message-quickReplies-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-quickReplies-div">
	<div class="col-md-12">
        <div style="text-align:center">
            <img src="img/Type11-quick replies.png" alt="" style="height:168px">
        </div>
        <div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
        <div class="col-md-8"><input type="text" name="altText" class="form-control">
            <small>
            Max: 400 characters
            </small>
        </div>
		<table class="table table-bordered table-striped">
            <tr>
                <td>訊息內文*</td>
                <td colspan=2>
                    <textarea name="text" style="width:100%;max-width:620px;height:220px"></textarea>
                    <br>
                    <small>Required</small>
                </td>
            </tr>
            <!--
			<tr>
				<td><bean:message key='qa.richmessage.image'/></td>
				<td colspan=2>
					<div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>Image URL (Max: 1000 characters)
					<br>Ratio (WxH) 1.81:1
					<br>GIF or JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
            -->
            <c:forEach var="i" begin="0" end="14">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="15"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-imageCarousel-column-tmpl">
		<table class="table table-bordered table-striped">	
			<tr class="actionA">
				<td width="120">圖片替代文字*</td>
				<td colspan=2><input type="text" name="imgAltText" class="form-control" placeholder="<bean:message key='qa.richmessage.card.hint'/>"><small>Required<br>Max: 400 characters</small></td>
			</tr>		
			<tr>
				<td><bean:message key='qa.richmessage.image'/>*</td>
				<td colspan=2>
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>Required
					<br>Image URL (Max: 1000 characters)
					<br>Ratio (WxH) 2:1
					<br>GIF or JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
			<tr>
				<td width="120">點選連結*</td>
				<td colspan=2><input type="text" name="uri" class="form-control"><small>Required<br>e.g. https://www.intumit.com/</small></td>
			</tr>
		</table>	
</script>

<script type="x-tmpl-mustache" id="rich-message-imageCarousel-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-imageCarousel-div">
	<div class="swiper-container card-B1-swiper" id="imageSwiper"> 
        <div class="swiper-wrapper"> 
		<% 
		int imageCarouselMax = 5;
		for (int i = 0 ; i < imageCarouselMax ; i++){ %>
            <div class="swiper-slide" data-col="<%=i%>"> 
                <img data-col="<%=i%>" src="img/image-carousel-col-<%=i+1%>.png" alt="" title="banner 0<%=i+1%>" style="height:237px">
            </div>
        <% } %>
        </div> 
        <div class="swiper-pagination">
        </div> 
        <div class="swiper-button-prev small">
        </div> 
        <div class="swiper-button-next small">
        </div> 
    </div>	
	<div class="col-md-12">
		<div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/>*</div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>
	</div>
	<div name="imageCarouselCol" class="col-md-12 imageCarouselCol">
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-optionsWithIconText-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-optionsWithIconText">
	<div class="col-md-12">		
		<div style="text-align:center">
			<img src="img/image-optionsWithIconText.png" alt="" style="height:237px">
		</div>
		<div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>	
		<table class="table table-bordered table-striped">			
			<tr>
				<td>Icon</td>
				<td colspan=2>
					<div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
						<div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>Image URL (Max: 1000 characters)
					<br>Ratio (WxH) 1:1
					<br>JPEG or PNG
					<br>Max width: 20px
					<br>Max: 1 MB
					</small>
				</td>				
			</tr>
			<tr>
				<td>主內文*</td>
				<td colspan=2>
					<textarea name="iconText" style="width:100%;max-width:620px;height:220px"></textarea>
					<br>
					<small>Required
					<br>Max: 120 characters.
					</small>
				</td>
			</tr>
			<tr>
				<td>延伸按鈕標題</td>
				<td colspan=2>
					<input type="text" name="extendTitle" class="form-control" maxlength="8">					
					<small>
						Max: 8 characters.
					</small>
				</td>
			</tr>
			<tr>
				<td>延伸內文</td>
				<td colspan=2>
					<textarea name="extendContent" style="width:100%;max-width:620px;height:220px"></textarea>
					<br>
					<small>
						Max: 120 characters.
					</small>
				</td>
			</tr>
            <c:forEach var="i" begin="0" end="3">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="4"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-optionsWithImageTitle-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-optionsWithImageTitle">
	<div class="col-md-12">		
		<div style="text-align:center">
			<img src="img/rich-message-type-buttons.png" alt="" style="height:237px">
		</div>
		<div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>	
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120"><bean:message key='qa.richmessage.card.title'/>*</td>
				<td colspan=2><input type="text" name="title" maxlength="40" class="form-control">
					<small>
						Required
						<br>Max: 40 characters					
					</small>
				</td>	
			</tr>
			<tr>
				<td><bean:message key='qa.richmessage.image'/>*</td>
				<td colspan=2>
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>Required
					<br>Image URL (Max: 1000 characters)
					<br>Ratio (WxH) 1.81:1
					<br>GIF or JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
			<tr>
				<td width="120"><bean:message key='qa.richmessage.message.title'/></td>
				<td colspan=2><input type="text" name="msgTitle" maxlength="40" class="form-control">
					<small>
						Max: 40 characters					
					</small>
				</td>	
			</tr>
			<tr>
				<td><bean:message key='qa.richmessage.message.subtitle'/></td>
				<td colspan=2>
					<input type="text" name="msgText" class="form-control">
					<small>
					Max: 160 characters (no image or title)
					<br>Max: 60 characters (message with an image or title)
					</small>
				</td>
			</tr>
            <c:forEach var="i" begin="0" end="3">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="4"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>			
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-imageModal-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-imageModal">
	<div class="col-md-12">		
		<div style="text-align:center">
			<img src="img/rich-message-type-imageModal.png" alt="" style="height:237px">
		</div>
		<div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>	
		<table class="table table-bordered table-striped">
			<tr>
				<td><bean:message key='qa.richmessage.image'/>*</td>
				<td colspan=2>
                    <div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                        <div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>Required
					<br>Image URL (Max: 1000 characters)
					<br>Ratio (WxH) 1:1
					<br>GIF or JPEG or PNG
					<br>Max width: 300px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
            <c:forEach var="i" begin="0" end="3">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="4"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>			
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-textModal-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-textModal">
	<div class="col-md-12">		
		<div style="text-align:center">
			<img src="img/rich-message-type-textModal.png" alt="" style="height:237px">
		</div>
		<div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>	
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120"><bean:message key='qa.richmessage.message.title'/>*</td>
				<td colspan=2><input type="text" name="title" maxlength="12" class="form-control">
					<small>Required
					<br>Max: 12 characters					
					</small>
				</td>	
			</tr>
			<tr>
				<td><bean:message key='qa.richmessage.message.subtitle'/>*</td>
				<td colspan=2>
					<input type="text" name="text" maxlength="30" class="form-control">
					<small>Required
					<br>Max: 30 characters
					</small>
				</td>
			</tr>
			<tr>
				<td>延伸按鈕標題</td>
				<td colspan=2>
					<input type="text" name="extendTitle" class="form-control" maxlength="8">					
					<small>
						Max: 8 characters.
					</small>
				</td>
			</tr>
			<tr>
				<td>延伸內文</td>
				<td colspan=2>
					<textarea name="extendContent" style="width:100%;max-width:620px;height:220px"></textarea>
					<br>
					<small>
						Max: 60 characters.
					</small>
				</td>
			</tr>
            <c:forEach var="i" begin="0" end="3">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="4"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>			
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-alert-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-alert">
	<div class="col-md-12">		
		<div style="text-align:center">
			<img style="width:75%" src="img/image-alert.png" alt="">
		</div>
		<div class="col-md-2"><bean:message key='qa.richmessage.alt.text'/></div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>	
		<table class="table table-bordered table-striped">
			<tr>
				<td>主標題*</td>
				<td colspan=2>
					<input type="text" name="title" class="form-control" maxlength="10">
					<small>Required
					<br>Max: 10 characters.
					</small>
				</td>
			</tr>
			<tr>
				<td>主標題icon</td>
				<td colspan=2>
					<div>
                        <div id="div-thumbnail">
                            <img name="thumbnailImageUrl" src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
						<div id="preview" class="pull-right">
                            <img src="" class="img-thumbnail" onerror="this.src='img/noImg_image_90x90.png';">
                        </div>
                    </div>
					<input type="text" class="form-control hide" name="thumbnailImageUrl" value="">
					<div class="msgImg hide"></div><input type="file" class="form-control">
					<small>Image URL (Max: 1000 characters)
					<br>Ratio (WxH) 1:1
					<br>JPEG or PNG
					<br>Max width: 20px
					<br>Max: 1 MB
					</small>
				</td>				
			</tr>
			<tr>
				<td><bean:message key='qa.richmessage.message.subtitle'/>*</td>
				<td colspan=2>
					<input type="text" name="text" class="form-control" maxlength="50">
					<small>Required
					<br>Max: 50 characters.
					</small>
				</td>
			</tr>		
            <c:forEach var="i" begin="0" end="1">
                 <tr class='action${alphebet[i]}'>
                    <c:if test="${i == 0}">
                        <td rowspan="2"><bean:message key='qa.richmessage.button'/> <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
                    </c:if>
                    <td width=20>${alphebet[i]}</td>
                    <td>
                        <input type="radio" name="action${alphebet[i]}type" value="message" checked="true"><bean:message key='qa.richmessage.text'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="uri"><bean:message key='qa.richmessage.url'/> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="postback" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>">
                            <span data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.postback.desc'/>"><bean:message key='qa.richmessage.text'/>(<bean:message key='qa.richmessage.postback'/>)</span> &nbsp;&nbsp;
                        <input type="radio" name="action${alphebet[i]}type" value="call"><bean:message key='qa.richmessage.call'/>                   
                        <br>
                        <label><bean:message key='qa.richmessage.button.title'/></label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="<bean:message key='qa.richmessage.button.disable.desc'/>"></span>
                        <input type="text" name="action${alphebet[i]}label" class="form-control" placeholder="<bean:message key='qa.richmessage.button.disable.hint'/>">
                        <label><bean:message key='qa.richmessage.data'/></label>
                        <input type="text" name="action${alphebet[i]}text" class="form-control" placeholder="<bean:message key='qa.richmessage.input.question'/>">
                        <input type="text" name="action${alphebet[i]}uri" class="form-control" placeholder="http://xxx.xxx.xxx">
                        <input type="text" name="action${alphebet[i]}data" class="form-control" placeholder="<bean:message key='qa.richmessage.button.postback.hint'/>">
                        <input type="text" name="action${alphebet[i]}call" class="form-control" placeholder="02-12345678">                   
                    </td>
                </tr>
            </c:forEach>			
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-list-tmpl">
<table class="table table-striped table-bordered table-hover" id="mainTable">
<thead>
	<tr>
	<th><bean:message key='global.mkey'/></th>
	<th>Type</th>
	<th><bean:message key='global.name'/></th>
	<th><bean:message key='global.description'/></th>
	<th>Update time</th>
	<th>Expire time</th>
	<th class='col-sm-3'><bean:message key='operation'/></th>
	</tr>
</thead>
<tbody>
</tbody>
</table>
<% if (AdminGroup.C > 0) { %>
<button class='btn btn-success btnCreate'><i class="glyphicon glyphicon-plus"></i> <bean:message key='global.add'/></button>
<% } %>
<% if (AdminGroup.C > 0 && AdminGroup.U > 0 && (admGrp.getSystemAdminCURD() & AdminGroup.O3) > 0 ) { %>
<button class='btn btn-success btnExport'><bean:message key='search.export'/></button>
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
    background: white url(<%= request.getContextPath() %>/wiseadm/webRM/img/noImg_image_90x90.png) no-repeat center center;
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
#div-thumbnail, #preview {
    display: inline-block;
    max-width: 400px;
}
#preview {
    overflow: hidden;
}
.swiper-slide {
	text-align: center;
}
</style>
</head>
<body>
<jsp:include page="../navbar-qa.jsp"></jsp:include>
<div id="top" class="container">
	<% if (StringUtils.isNotEmpty(errorMsg)) { %>
		<div id="errorMsg" class="alert alert-danger"><%= errorMsg %></div>
	<% } %>
	<% if (AdminGroup.C > 0 && AdminGroup.U > 0 && (admGrp.getSystemAdminCURD() & AdminGroup.O4) > 0 ) { %>
	<h3><bean:message key='qa.richmessage.navbar.web'/><bean:message key='global.batch.import'/><br></h3><br> 
	<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/webRM/qaRichMessageUploadFile?action=import" onsubmit="return submitForm(this);">
    	<table style="width:100%">
    		<tr>
    			<th width="10%"><input type="file" name="File1" size="20" maxlength="20"></th>
                <th width="10%">
                    <select name="type">
                        <option value="default">Default</option>
                        <option value="buttons">Buttons</option>
                        <option value="text">Text</option>
                        <option value="carousel">Carousel</option>
                        <option value="stretch">Stretch</option>
                        <option value="threegrid">3 Grid</option>
                        <option value="twogrid">2 Grid</option>
                        <option value="textwithoutbutton">Text Without Button</option>
                        <option value="textwithbutton">Text With Button</option>
						<option value="onegrid">1 Grid</option>
						<option value="quickReplies">Quick Replies</option>
                    </select>
                </th>
    			<th width="30%"><input type="submit" value="<bean:message key='global.import.csv'/>"></th>
    		</tr>
    	</table>
	</form>
	<% } %>
	<div id="rich-message-detail">
	</div>
	<br>
	<br>
	<div id="rich-message-list">
	
	</div>
</div>
<script src="qaRichMessage.model.js"></script>
<script>
var detailView = null;
var ImageSwiper;
<%-- itemSaveCallback & target 變數僅給使用 iframe 帶出圖文編輯介面用，作用是當按下儲存時，會呼叫 itemSaveCallback 並將 target 及 response 傳遞過去 --%>
var itemSaveCallback = null;
<%= xssReq.getParameter("target") != null ? "var target='" + xssReq.getParameter("target") + "'" : "" %>

var MessageActionView = Backbone.View.extend({
	domNamePrefix: "",
	initialize: function(options) {
		this.modelBinder = new Backbone.ModelBinder();
		
		if (options != null) {
			this.domNamePrefix = options.domNamePrefix;
		}
	    this.listenTo(this.model, 'change', this.render);
	},
	render: function() {
		textInputSelector = '[name=' + this.domNamePrefix + 'text]';
		dataInputSelector = '[name=' + this.domNamePrefix + 'data]';
		uriInputSelector = '[name=' + this.domNamePrefix + 'uri]';
		callInputSelector = '[name=' + this.domNamePrefix + 'call]';
		
		this.modelBinder.bind(this.model, this.el, {
			type: '[name=' + this.domNamePrefix + 'type]',
			label: '[name=' + this.domNamePrefix + 'label]',
			text: textInputSelector,
			data: dataInputSelector,
			uri: uriInputSelector,
			phone: callInputSelector,
		});
		
		var type = this.model.get("type");
		
		if (type == 'message') {
			$(textInputSelector).show();
			$(dataInputSelector).hide();
			$(uriInputSelector).hide();
			$(callInputSelector).hide();
		}
		else if (type == 'postback') {
			$(textInputSelector).hide();
			$(dataInputSelector).show();
			$(uriInputSelector).hide();
			$(callInputSelector).hide();
		}
		else if (type == 'uri') {
			$(textInputSelector).hide();
			$(dataInputSelector).hide();
			$(uriInputSelector).show();
			$(callInputSelector).hide();
		}
		else if (type == 'call') {
			$(textInputSelector).hide();
			$(dataInputSelector).hide();
			$(uriInputSelector).hide();
			$(callInputSelector).show();
		}
		return this;
	}
});

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
		textInputSelector = '[name=' + this.domNamePrefix + 'text]';
		dataInputSelector = '[name=' + this.domNamePrefix + 'data]';
		uriInputSelector = '[name=' + this.domNamePrefix + 'uri]';
		
		this.modelBinder.bind(this.model, this.el, {
			type: '[name=' + this.domNamePrefix + 'type]',
			label: '[name=' + this.domNamePrefix + 'label]',
			text: textInputSelector,
			data: dataInputSelector,
			linkUri: uriInputSelector,
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
		return this;
	}
});

var QuickRepliesActionView = Backbone.View.extend({
	domNamePrefix: "",
	initialize: function(options) {
		this.modelBinder = new Backbone.ModelBinder();
		
		if (options != null) {
			this.domNamePrefix = options.domNamePrefix;
		}
		this.listenTo(this.model, 'change', this.render);
	},
	render: function() {
		textInputSelector = '[name=' + this.domNamePrefix + 'text]';
		dataInputSelector = '[name=' + this.domNamePrefix + 'data]';
		this.modelBinder.bind(this.model, this.el, {
			type: '[name=' + this.domNamePrefix + 'type]',
			label: '[name=' + this.domNamePrefix + 'label]',
			text: textInputSelector,
			data: dataInputSelector
		});
		
		var type = this.model.get('type');
		
		if (type == 'message') {
			$(textInputSelector).show();
			$(dataInputSelector).hide();
		} else if (type == 'postback') {
			$(textInputSelector).hide();
			$(dataInputSelector).show();
		}		
		return this;
	}
});

var ModalActionView = Backbone.View.extend({
	domNamePrefix: "",
	initialize: function(options) {
		this.modelBinder = new Backbone.ModelBinder();
		
		if (options != null) {
			this.domNamePrefix = options.domNamePrefix;
		}
		this.listenTo(this.model, 'change', this.render);
	},
	render: function() {
		textInputSelector = '[name=' + this.domNamePrefix + 'text]';
		dataInputSelector = '[name=' + this.domNamePrefix + 'data]';
		uriInputSelector = '[name=' + this.domNamePrefix + 'uri]';
		callInputSelector = '[name=' + this.domNamePrefix + 'call]';
		this.modelBinder.bind(this.model, this.el, {
			type: '[name=' + this.domNamePrefix + 'type]',
			label: '[name=' + this.domNamePrefix + 'label]',
			text: textInputSelector,
			data: dataInputSelector,
			uri: uriInputSelector,
			phone: callInputSelector
		});
		
		var type = this.model.get("type");
		
		if (type == 'message') {
			$(textInputSelector).show();
			$(dataInputSelector).hide();
			$(uriInputSelector).hide();
			$(callInputSelector).hide();
		}
		else if (type == 'postback') {
			$(textInputSelector).hide();
			$(dataInputSelector).show();
			$(uriInputSelector).hide();
			$(callInputSelector).hide();
		}
		else if (type == 'uri') {
			$(textInputSelector).hide();
			$(dataInputSelector).hide();
			$(uriInputSelector).show();
			$(callInputSelector).hide();
		}
		else if (type == 'call') {
			$(textInputSelector).hide();
			$(dataInputSelector).hide();
			$(uriInputSelector).hide();
			$(callInputSelector).show();
		}
		return this;
	}
});

var AlertActionView = Backbone.View.extend({
	domNamePrefix: "",
	initialize: function(options) {
		this.modelBinder = new Backbone.ModelBinder();
		
		if (options != null) {
			this.domNamePrefix = options.domNamePrefix;
		}
		this.listenTo(this.model, 'change', this.render);
	},
	render: function() {		
		callInputSelector = '[name=' + this.domNamePrefix + 'call]';
		uriInputSelector = '[name=' + this.domNamePrefix + 'uri]';
		dataTitleSelector = '[name=' + this.domNamePrefix + 'dataTitle]';
		this.modelBinder.bind(this.model, this.el, {
			type: '[name=' + this.domNamePrefix + 'type]',
			label: '[name=' + this.domNamePrefix + 'label]',
			phone: callInputSelector,
			uri: uriInputSelector
		});
		
		var type = this.model.get("type");
		
		if (type == 'cancel') {			
			$(callInputSelector).hide();
			$(uriInputSelector).hide();
			$(dataTitleSelector).hide();
		}
		else if (type == 'call') {			
			$(callInputSelector).show();
			$(uriInputSelector).hide();
			$(dataTitleSelector).show();
		}
		else if (type == 'uri') {			
			$(callInputSelector).hide();
			$(uriInputSelector).show();
			$(dataTitleSelector).show();
		}
		return this;
	}
});

var TextParentView = Backbone.View.extend({
    template: $('#rich-message-text-tmpl').html(),
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    events: {
    	'update [name=text]' : 'onChangeText'
    },
    onChangeText: function(e) {
    	input = $(e.currentTarget);
        this.model.set('text', input.val());
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        $this = this;
        this.$el.html(html);
        
        this.modelBinder.bind(this.model, this.el, {
            altText: '[name=altText]',
            text: '[name=text]'
        });
        
        $('[data-toggle="tooltip"]').tooltip();
        
        return this;
    }
});

var StretchTemplateParentView = Backbone.View.extend({
    template: $('#rich-message-stretch-tmpl').html(),
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        $this = this;
        this.$el.html(html);
        
        var childView = new StretchTemplateChildView({el: this.$el, model: this.model.get("template")});
        childView.render();

        this.modelBinder.bind(this.model, this.el, {
            altText: '[name=altText]'
        });
        
        $('[data-toggle="tooltip"]').tooltip();
        
        return this;
    }
});

var StretchTemplateChildView = Backbone.View.extend({
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    events: {
        'update [name=text]' : 'onChangeText'
    },
    onChangeText: function(e) {
        input = $(e.currentTarget);
        this.model.set('text', input.val());
    },
    render: function() {
        $this = this;
        var actions = this.model.get("actions");
        if (actions == null) {
            actions = [];
            this.model.set("actions", actions);
        }
        
        var actionNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD', 'actionE', 'actionF', 'actionG', 'actionH', 'actionI', 'actionJ'];
        
        for (var i=0; i < actionNameDomPrefix.length; i++) {
            if (i >= actions.length) {
                actions[i] = new MessageActionModel({}, {parse: true});
            }

            var actionModel = actions[i];
            var actionView = new MessageActionView({
                model: actionModel, 
                el: $("." + actionNameDomPrefix[i], this.$el),
                domNamePrefix: actionNameDomPrefix[i],
            });
            actionView.render();
        }
        
        this.modelBinder.bind(this.model, this.el, {
            stretchTitle: '[name=stretchTitle]',
            stretchText: '[name=stretchText]',
            text: '[name=text]'
        });
            
        return this;
    }
});

var ButtonsTemplateParentView = Backbone.View.extend({
	template: $('#rich-message-buttons-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		$this = this;
	    this.$el.html(html);
	    
	    var childView = new ButtonsTemplateChildView({el: this.$el, model: this.model.get("template")});
	    childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
	    
		return this;
	}
});

var ButtonsTemplateChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	events: {
		'change input[name=thumbnailImageUrl]': 'onChangeThumbnail',
		'update [name=text]' : 'onChangeText'
	},
	onChangeThumbnail: function(e) {
		var imgSrc = $(e.target).val();
	    this.$('img[name=thumbnailImageUrl]').attr('src', imgSrc);
	},
    onChangeText: function(e) {
        input = $(e.currentTarget);
        this.model.set('text', input.val());
    },
	render: function() {
		$this = this;
		var actions = this.model.get("actions");
		if (actions == null) {
			actions = [];
			this.model.set("actions", actions);
		}
		
		var actionNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD', 'actionE', 'actionF', 'actionG', 'actionH', 'actionI', 'actionJ'];
		
		for (var i=0; i < actionNameDomPrefix.length; i++) {
			if (i >= actions.length) {
				actions[i] = new MessageActionModel({}, {parse: true});
			}

			var actionModel = actions[i];
			var actionView = new MessageActionView({
				model: actionModel, 
				el: $("." + actionNameDomPrefix[i], this.$el),
				domNamePrefix: actionNameDomPrefix[i],
			});
			actionView.render();
		}
		
	    this.modelBinder.bind(this.model, this.el, {
	        //title: '[name=title]',
	        text: '[name=text]'
	        //thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
	    });
	        
	    if(this.model.get("thumbnailImageUrl")) {
	        //cropImage(this.model.get("thumbnailImageUrl"));
	        //this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
	        //this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
	        
		return this;
	}
});

var CarouselTemplateParentView = Backbone.View.extend({
	template: $('#rich-message-carousel-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		$this = this;
	    this.$el.html(html);
	    
	    var childView = new CarouselTemplateChildView({el: this.$el, model: this.model.get("template")});
	    childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var CarouselTemplateChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		$this = this;
		
		activateCarouselCol = function(idx) {
			var columns = $this.model.get("columns");
			
			if (columns.length <= idx) {
				for (var i=0; i <= idx; i++) {
					if (columns.length <= i) {
						columns[i] = new CarouselMessageColumnModel({}, {parse: true});
						console.log("Not initialized column, create model..");
					}
				}
			}
			var childView = new CarouselTemplateColumnView({ el: $this.$('.carouselCol'), model: columns[idx], colMarkImgUrl: 'img/carousel-col-' + (parseInt(idx)+1) + '.png' });
			childView.render();
			
			$('[data-toggle="tooltip"]').tooltip();
		};
		
		setTimeout( function() { // 用 timeout 避免 coverflow 壞掉
			$this.$('.coverflow').show({
				duration: 0,
				complete: function() {
					$this.$('.coverflow').coverflow({
						select: function(event, ui) {
				            activateCarouselCol(ui.index);
				        }
					});
				}
			});
		}, 100);

		this.$('.coverflow img').unbind();
		this.$('.coverflow img').click(function() {
		    if( ! $(this).hasClass('ui-state-active')){
		        return;
		    }
		    
		    col = $(this).attr('data-col');
		    activateCarouselCol(col);
		});

		this.modelBinder.bind($this.model, $this.el, {
			altText: '[name=altText]',
            fixedTitle: '[name=fixedTitle]'
		});
		return this;
	}
});

var CarouselTemplateColumnView = Backbone.View.extend({
	template: $('#rich-message-carousel-column-tmpl').html(),
	colMarkImgUrl: null,
	initialize: function(options) {
		this.colMarkImgUrl = options.colMarkImgUrl;
		this.modelBinder = new Backbone.ModelBinder();
	},
	events: {
		//'change input[name=thumbnailImageUrl]': 'onChangeThumbnail',
		'update [name=text]' : 'onChangeText'
	},
	onChangeThumbnail: function(e) {
		//var imgSrc = $(e.target).val();
	    //this.$('img[name=thumbnailImageUrl]').attr('src', imgSrc);
	},
    onChangeText: function(e) {
    	//取出cid
    	var cid = $(e.target).attr("cid");
    	//只修該當前綁定的model
    	if(this.model.cid == cid){
            var input =  $(e.target).val();
            this.model.set('text', input);
    	}
    },
	render: function() {
		var cid = this.model.cid;
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    this.$('img[name=colMarkImg]').attr('src', this.colMarkImgUrl);
	    if(this.model.get("thumbnailImageUrl")) {
	    	cropImage(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
		
	    //先把cid綁定在textarea上，確保在update事件時修改到正確的model
		$("textarea[name=text]").attr("cid",cid);
	    
		var actions = this.model.get("actions");
		if (actions == null) {
			actions = [];
			this.model.set("actions", actions);
		}
		
		var actionNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD', 'actionE'];
		for (var i=0; i < actionNameDomPrefix.length; i++) {
			if (i >= actions.length) {
				actions[i] = new MessageActionModel({}, {parse: true});
			}

			var actionModel = actions[i];
			var actionView = new MessageActionView({
				model: actionModel, 
				el: $("." + actionNameDomPrefix[i], this.$el),
				domNamePrefix: actionNameDomPrefix[i],
			});
			actionView.render();
		}
	    
		this.modelBinder.bind(this.model, this.el, {
			title: '[name=title]',
			text: '[name=text]',
			thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
		});
		
		generateCKEditor('text');
		return this;
	}
});

// one grid 示意圖、固定內文view 
var OneGridTemplateParentView = Backbone.View.extend({
    template : $("#rich-message-onegrid-tmpl").html(),
    initialize: function() {
      this.modelBinder = new Backbone.ModelBinder();
    },
    render: function(){
      var html = Mustache.render(this.template, this.model.toJSON());
      $this = this;
      this.$el.html(html);
      var childView = new OneGridTemplateChildView({
        el: this.$el, 
        model: this.model.get("template")}
        /*
        template:{ 
          type: "onegrid", 
          columns:[], 
          fixedTitle: ""
        }
        */
      );
      childView.render();

      this.modelBinder.bind(this.model, this.el, {
          altText: '[name=altText]'
      });
      
      $('[data-toggle="tooltip"]').tooltip();
      
      return this;
    }
});

var OneGridTemplateChildView = Backbone.View.extend({
    initialize: function() {
      this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
      $this = this;
      // render 維護欄位form view
      activateOneGridCol = function(idx) {
        var columns = $this.model.get("columns");
        if (columns.length <= idx) {
            for (var i=0; i <= idx; i++) {
                if (columns.length <= i) {
                    columns[i] = new OneGridMessageColumnModel({}, {parse: true});
                    console.log("Not initialized column, create model..");
                }
            }
        }
        var childView = new OneGridTemplateColumnView({ 
          el: $this.$('.onegridCol'),
          model: columns[idx], // OneGridMessageColumnModel
          colMarkImgUrl: 'img/Type10-1gridcard/Type10-1gridcard-' + (parseInt(idx)+1) + '.png' 
        });
        childView.render();
        
        $('[data-toggle="tooltip"]').tooltip();
      };
      
      this.$('.coverflow img').unbind();
      this.$('.coverflow img').click(function() {
          if(!$(this).hasClass('ui-state-active')){
              return;
          }
          col = $(this).attr('data-col');
          activateOneGridCol(col);
      });
  

      this.modelBinder.bind($this.model, $this.el, {
          altText: '[name=altText]',
          fixedTitle: '[name=fixedTitle]'
      });
    
      setTimeout( function() { // 用 timeout 避免 coverflow 壞掉
        $this.$('.coverflow').show({
            duration: 0,
            complete: function() {
                $this.$('.coverflow').coverflow({
                    select: function(event, ui) {
                        activateOneGridCol(ui.index);
                    }
                });
            }
          });
      }, 100);
      
    }
})

// one grid 圖文內容維護 form view
var OneGridTemplateColumnView = Backbone.View.extend({
    template: $("#rich-message-onegrid-column-tmpl").html(),
    colMarkImgUrl: null,
    initialize: function(options) {
        this.colMarkImgUrl = options.colMarkImgUrl;
        this.modelBinder = new Backbone.ModelBinder();
    },
    events: {
      'update [name=text1]' : 'onChangeText'
    },
    onChangeText: function(e) {
    	//取出cid
    	var cid = $(e.target).attr("cid");
    	//只修該當前綁定的model
    	if(this.model.cid == cid){
            var input =  $(e.target).val();
            this.model.set('text1', input);
    	}
    },
    render: function() {
      var cid = this.model.cid;
      var html = Mustache.render(this.template, this.model.toJSON());
      this.$el.html(html);
      this.$('img[name=colMarkImg]').attr('src', this.colMarkImgUrl);
      if(this.model.get("thumbnailImageUrl")) {
          cropImage(this.model.get("thumbnailImageUrl"));
          this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
          this.$('input[name=thumbnailImageUrl]').trigger('change');
      }
      
      //先把cid綁定在textarea上，確保在update事件時修改到正確的model
      $("textarea[name=text1]").attr("cid",cid);
      
      var actions = this.model.get("actions");
      if (actions == null) {
          actions = [];
          this.model.set("actions", actions);
      }
      
      var actionNameDomPrefix = ['actionA'];
      for (var i=0; i < actionNameDomPrefix.length; i++) {
          if (i >= actions.length) {
              actions[i] = new MessageActionModel({}, {parse: true});
          }

          var actionModel = actions[i];
          var actionView = new MessageActionView({
              model: actionModel, 
              el: $("." + actionNameDomPrefix[i], this.$el),
              domNamePrefix: actionNameDomPrefix[i],
          });
          actionView.render();
      }
      
      this.modelBinder.bind(this.model, this.el, {
          title: '[name=title]',
          picTitle: '[name=picTitle]',
          text1: '[name=text1]',
          text2: '[name=text2]',
          thumbnailImageUrl: 'input[name=thumbnailImageUrl]'
      });
      generateCKEditor('text1');
      return this;
    }
    
})

var ThreeGridTemplateParentView = Backbone.View.extend({
    template: $('#rich-message-threegrid-tmpl').html(),
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        $this = this;
        this.$el.html(html);
        
        var childView = new ThreeGridTemplateChildView({el: this.$el, model: this.model.get("template")});
        childView.render();

        this.modelBinder.bind(this.model, this.el, {
            altText: '[name=altText]'
        });
        
        $('[data-toggle="tooltip"]').tooltip();
        
        return this;
    }
});

var ThreeGridTemplateChildView = Backbone.View.extend({
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        $this = this;
        
        activateThreeGridCol = function(idx) {
            var columns = $this.model.get("columns");
            
            if (columns.length <= idx) {
                for (var i=0; i <= idx; i++) {
                    if (columns.length <= i) {
                        columns[i] = new ThreeGridMessageColumnModel({}, {parse: true});
                        console.log("Not initialized column, create model..");
                    }
                }
            }
            var childView = new ThreeGridTemplateColumnView({ el: $this.$('.threegridCol'), model: columns[idx], colMarkImgUrl: 'img/image-3grid-col-' + (parseInt(idx)+1) + '.png' });
            childView.render();
            
            $('[data-toggle="tooltip"]').tooltip();
        };
        
        setTimeout( function() { // 用 timeout 避免 coverflow 壞掉
            $this.$('.coverflow').show({
                duration: 0,
                complete: function() {
                    $this.$('.coverflow').coverflow({
                        select: function(event, ui) {
                        	activateThreeGridCol(ui.index);
                        }
                    });
                }
            });
        }, 100);

        this.$('.coverflow img').unbind();
        this.$('.coverflow img').click(function() {
            if( ! $(this).hasClass('ui-state-active')){
                return;
            }
            
            col = $(this).attr('data-col');
            activateThreeGridCol(col);
        });

        this.modelBinder.bind($this.model, $this.el, {
            altText: '[name=altText]',
            fixedTitle: '[name=fixedTitle]'
        });
        return this;
    }
});

var ThreeGridTemplateColumnView = Backbone.View.extend({
    template: $('#rich-message-threegrid-column-tmpl').html(),
    colMarkImgUrl: null,
    initialize: function(options) {
        this.colMarkImgUrl = options.colMarkImgUrl;
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        this.$el.html(html);
        this.$('img[name=colMarkImg]').attr('src', this.colMarkImgUrl);
        if(this.model.get("thumbnailImageUrl")) {
            cropImage(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').trigger('change');
        }
        
        var actions = this.model.get("actions");
        if (actions == null) {
            actions = [];
            this.model.set("actions", actions);
        }
        
        var actionNameDomPrefix = ['actionA'];
        for (var i=0; i < actionNameDomPrefix.length; i++) {
            if (i >= actions.length) {
                actions[i] = new MessageActionModel({}, {parse: true});
            }

            var actionModel = actions[i];
            var actionView = new MessageActionView({
                model: actionModel, 
                el: $("." + actionNameDomPrefix[i], this.$el),
                domNamePrefix: actionNameDomPrefix[i],
            });
            actionView.render();
        }
        
        this.modelBinder.bind(this.model, this.el, {
            title: '[name=title]',
            text1: '[name=text1]',
            text2: '[name=text2]',
            text3: '[name=text3]',
            text4: '[name=text4]',
            text5: '[name=text5]',
            text6: '[name=text6]',
            text7: '[name=text7]',
            thumbnailImageUrl: 'input[name=thumbnailImageUrl]'
        });
        return this;
    }
});

var TwoGridTemplateParentView = Backbone.View.extend({
    template: $('#rich-message-twogrid-tmpl').html(),
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        $this = this;
        this.$el.html(html);
        
        var childView = new TwoGridTemplateChildView({el: this.$el, model: this.model.get("template")});
        childView.render();

        this.modelBinder.bind(this.model, this.el, {
            altText: '[name=altText]'
        });
        
        $('[data-toggle="tooltip"]').tooltip();
        
        return this;
    }
});

var TwoGridTemplateChildView = Backbone.View.extend({
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        $this = this;
        
        activateTwoGridCol = function(idx) {
            var columns = $this.model.get("columns");
            
            if (columns.length <= idx) {
                for (var i=0; i <= idx; i++) {
                    if (columns.length <= i) {
                        columns[i] = new TwoGridMessageColumnModel({}, {parse: true});
                        console.log("Not initialized column, create model..");
                    }
                }
            }
            var childView = new TwoGridTemplateColumnView({ el: $this.$('.twogridCol'), model: columns[idx], colMarkImgUrl: 'img/image-2grid-col-' + (parseInt(idx)+1) + '.png' });
            childView.render();
            
            $('[data-toggle="tooltip"]').tooltip();
        };
        
        setTimeout( function() { // 用 timeout 避免 coverflow 壞掉
            $this.$('.coverflow').show({
                duration: 0,
                complete: function() {
                    $this.$('.coverflow').coverflow({
                        select: function(event, ui) {
                            activateTwoGridCol(ui.index);
                        }
                    });
                }
            });
        }, 100);

        this.$('.coverflow img').unbind();
        this.$('.coverflow img').click(function() {
            if( ! $(this).hasClass('ui-state-active')){
                return;
            }
            
            col = $(this).attr('data-col');
            activateTwoGridCol(col);
        });

        this.modelBinder.bind($this.model, $this.el, {
            altText: '[name=altText]',
            fixedTitle: '[name=fixedTitle]'
        });
        return this;
    }
});

var TwoGridTemplateColumnView = Backbone.View.extend({
    template: $('#rich-message-twogrid-column-tmpl').html(),
    colMarkImgUrl: null,
    initialize: function(options) {
        this.colMarkImgUrl = options.colMarkImgUrl;
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        this.$el.html(html);
        this.$('img[name=colMarkImg]').attr('src', this.colMarkImgUrl);
        if(this.model.get("thumbnailImageUrl")) {
            cropImage(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').trigger('change');
        }
        
        this.modelBinder.bind(this.model, this.el, {
            title: '[name=title]',
            text1: '[name=text1]',
            text2: '[name=text2]',
            thumbnailImageUrl: 'input[name=thumbnailImageUrl]'
        });
        return this;
    }
});

var TextWithoutButtonTemplateParentView = Backbone.View.extend({
    template: $('#rich-message-textwithoutbutton-tmpl').html(),
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        $this = this;
        this.$el.html(html);
        
        var childView = new TextWithoutButtonTemplateChildView({el: this.$el, model: this.model.get("template")});
        childView.render();

        this.modelBinder.bind(this.model, this.el, {
            altText: '[name=altText]'
        });
        
        $('[data-toggle="tooltip"]').tooltip();
        
        return this;
    }
});

var TextWithoutButtonTemplateChildView = Backbone.View.extend({
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        $this = this;
        
        activateTextWithoutButtonCol = function(idx) {
            var columns = $this.model.get("columns");
            
            if (columns.length <= idx) {
                for (var i=0; i <= idx; i++) {
                    if (columns.length <= i) {
                        columns[i] = new TextWithoutButtonMessageColumnModel({}, {parse: true});
                        console.log("Not initialized column, create model..");
                    }
                }
            }
            var childView = new TextWithoutButtonTemplateColumnView({ el: $this.$('.textwithoutbuttonCol'), model: columns[idx], colMarkImgUrl: 'img/image-textwithoutbutton-col-' + (parseInt(idx)+1) + '.png', index: parseInt(idx) });
            childView.render();
            
            $('[data-toggle="tooltip"]').tooltip();
        };
        
        setTimeout( function() { // 用 timeout 避免 coverflow 壞掉
            $this.$('.coverflow').show({
                duration: 0,
                complete: function() {
                    $this.$('.coverflow').coverflow({
                        select: function(event, ui) {
                        	activateTextWithoutButtonCol(ui.index);
                        }
                    });
                }
            });
        }, 100);

        this.$('.coverflow img').unbind();
        this.$('.coverflow img').click(function() {
            if( ! $(this).hasClass('ui-state-active')){
                return;
            }
            
            col = $(this).attr('data-col');
            activateTextWithoutButtonCol(col);
        });

        this.modelBinder.bind($this.model, $this.el, {
            altText: '[name=altText]',
            fixedTitle: '[name=fixedTitle]'
        });
        return this;
    }
});

var TextWithoutButtonTemplateColumnView = Backbone.View.extend({
    template: $('#rich-message-textwithoutbutton-column-tmpl').html(),
    colMarkImgUrl: null,
    index: null,
    initialize: function(options) {
        this.colMarkImgUrl = options.colMarkImgUrl;
        this.index = options.index;
        this.modelBinder = new Backbone.ModelBinder();
    },
    events: {
        'update [name=text]' : 'onChangeText'
    },
    onChangeText: function(e) {
        input = $(e.currentTarget);
        if (input.data('index') == this.index) {
            this.model.set('text', input.val());
        }
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        this.$el.html(html);
        this.$('img[name=colMarkImg]').attr('src', this.colMarkImgUrl);
        this.$('[name=text]').data('index', this.index);
        if(this.model.get("thumbnailImageUrl")) {
            cropImage(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').trigger('change');
        }
        
        this.modelBinder.bind(this.model, this.el, {
            title: '[name=title]',
            text: '[name=text]',
            thumbnailImageUrl: 'input[name=thumbnailImageUrl]'
        });
        
        generateCKEditor('text');
        return this;
    }
});

var TextWithButtonTemplateParentView = Backbone.View.extend({
    template: $('#rich-message-textwithbutton-tmpl').html(),
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        $this = this;
        this.$el.html(html);
        
        var childView = new TextWithButtonTemplateChildView({el: this.$el, model: this.model.get("template")});
        childView.render();

        this.modelBinder.bind(this.model, this.el, {
            altText: '[name=altText]'
        });
        
        $('[data-toggle="tooltip"]').tooltip();
        
        return this;
    }
});

var TextWithButtonTemplateChildView = Backbone.View.extend({
    initialize: function() {
        this.modelBinder = new Backbone.ModelBinder();
    },
    render: function() {
        $this = this;
        
        activateTextWithButtonCol = function(idx) {
            var columns = $this.model.get("columns");
            
            if (columns.length <= idx) {
                for (var i=0; i <= idx; i++) {
                    if (columns.length <= i) {
                        columns[i] = new TextWithButtonMessageColumnModel({}, {parse: true});
                        console.log("Not initialized column, create model..");
                    }
                }
            }

            var childView = new TextWithButtonTemplateColumnView({ el: $this.$('.textwithbuttonCol'), model: columns[idx], colMarkImgUrl: 'img/image-textwithbutton-col-' + (parseInt(idx)+1) + '.png', index: parseInt(idx) });
            childView.render();
            
            $('[data-toggle="tooltip"]').tooltip();
        };
        
        setTimeout( function() { // 用 timeout 避免 coverflow 壞掉
            $this.$('.coverflow').show({
                duration: 0,
                complete: function() {
                    $this.$('.coverflow').coverflow({
                        select: function(event, ui) {
                            activateTextWithButtonCol(ui.index);
                        }
                    });
                }
            });
        }, 100);

        this.$('.coverflow img').unbind();
        this.$('.coverflow img').click(function() {
            if( ! $(this).hasClass('ui-state-active')){
                return;
            }
            
            col = $(this).attr('data-col');
            activateTextWithButtonCol(col);
        });

        this.modelBinder.bind($this.model, $this.el, {
            altText: '[name=altText]',
            fixedTitle: '[name=fixedTitle]'
        });
        return this;
    }
});

var TextWithButtonTemplateColumnView = Backbone.View.extend({
    template: $('#rich-message-textwithbutton-column-tmpl').html(),
    colMarkImgUrl: null,
    index: null,
    initialize: function(options) {
        this.colMarkImgUrl = options.colMarkImgUrl;
        this.index = options.index;
        this.modelBinder = new Backbone.ModelBinder();
    },
    events: {
        'update [name=text]' : 'onChangeText'
    },
    onChangeText: function(e) {
        input = $(e.currentTarget);
        if (input.data('index') == this.index) {
            this.model.set('text', input.val());
        }
    },
    render: function() {
        var html = Mustache.render(this.template, this.model.toJSON());
        this.$el.html(html);
        this.$('img[name=colMarkImg]').attr('src', this.colMarkImgUrl);
        this.$('[name=text]').data('index', this.index);
        if(this.model.get("thumbnailImageUrl")) {
            cropImage(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
            this.$('input[name=thumbnailImageUrl]').trigger('change');
        }
        
        var actions = this.model.get("actions");
        if (actions == null) {
            actions = [];
            this.model.set("actions", actions);
        }
        
        var actionNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD', 'actionE'];
        for (var i=0; i < actionNameDomPrefix.length; i++) {
            if (i >= actions.length) {
                actions[i] = new MessageActionModel({}, {parse: true});
            }

            var actionModel = actions[i];
            var actionView = new MessageActionView({
                model: actionModel, 
                el: $("." + actionNameDomPrefix[i], this.$el),
                domNamePrefix: actionNameDomPrefix[i],
            });
            actionView.render();
        }
        
        this.modelBinder.bind(this.model, this.el, {
            title: '[name=title]',
            text: '[name=text]',
            thumbnailImageUrl: 'input[name=thumbnailImageUrl]'
        });
        
        generateCKEditor('text');
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
		if(/^data/.test(imgSrc)) {
	    	this.$('img[name=baseUrl]').attr('src', imgSrc);
		}
		else {
			this.$('img[name=baseUrl]').attr('src', imgSrc + "/460");
		}
	},
	actionNameDomPrefix: ['actionA', 'actionB', 'actionC', 'actionD', 'actionE',
		'actionF', 'actionG', 'actionH', 'actionI', 'actionJ',
		'actionK', 'actionL', 'actionM', 'actionN', 'actionO'],
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    if(this.model.get("baseUrl")) {
	    	cropImage(this.model.get("baseUrl") + "/460");
			this.$('input[name=baseUrl]').val(this.model.get("baseUrl"));
			this.$('input[name=baseUrl]').trigger('change');
	    }
	    $thisEl = this.$el;
	    
		var actions = this.model.get("actions");
		if (actions == null) {
			actions = [];
			this.model.set("actions", actions);
		}
		
		for (var i=0; i < this.actionNameDomPrefix.length; i++) {
			if (i >= actions.length) {
				actions[i] = new ImagemapActionModel({}, {parse: true});
			}

			var actionModel = actions[i];
			var actionView = new ImagemapActionView({
				model: actionModel, 
				el: $("." + this.actionNameDomPrefix[i], this.$el),
				domNamePrefix: this.actionNameDomPrefix[i],
			});
			actionView.render();
		}
		
		var imgMapType = this.model.get("imageMapType");
		// 決定哪些 actions 設定區塊要隱藏
		var areas = this.model.theDefaultAreaSettings[imgMapType];
		
		if (typeof(areas) != 'undefined') {
			for (var i=0; i < this.actionNameDomPrefix.length; i++) {
				if (i < areas.length) {
					this.$('.' + this.actionNameDomPrefix[i]).show();
				}
				else {
					this.$('.' + this.actionNameDomPrefix[i]).hide();
				}
			}
		}

		this.modelBinder.bind(this.model, this.el, {
			imageMapType: '[name=imageMapType]',
			altText: '[name=altText]',
			baseUrl: 'input[name=baseUrl]'
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

var QuickRepliesParentView = Backbone.View.extend({
	template: $('#rich-message-quickReplies-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		$this = this;
	    this.$el.html(html);
	    
	    var childView = new QuickRepliesChildView({el: this.$el, model: this.model.get("template")});
	    childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
	    
		return this;
	}
});

var QuickRepliesChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	events: {
		'change input[name=thumbnailImageUrl]': 'onChangeThumbnail',
		'update [name=text]' : 'onChangeText'
	},
	onChangeThumbnail: function(e) {
		var imgSrc = $(e.target).val();
	    this.$('img[name=thumbnailImageUrl]').attr('src', imgSrc);
	},
    onChangeText: function(e) {
        input = $(e.currentTarget);
        this.model.set('text', input.val());
    },
	render: function() {
		$this = this;
		var actions = this.model.get("actions");
		if (actions == null) {
			actions = [];
			this.model.set("actions", actions);
		}
		
		var actionNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD', 'actionE', 'actionF', 'actionG', 'actionH', 'actionI', 'actionJ', 'actionK', 'actionL', 'actionM', 'actionN', 'actionO'];
		
		for (var i=0; i < actionNameDomPrefix.length; i++) {
			if (i >= actions.length) {
				actions[i] = new MessageActionModel({}, {parse: true});
			}

			var actionModel = actions[i];
			var actionView = new MessageActionView({
				model: actionModel, 
				el: $("." + actionNameDomPrefix[i], this.$el),
				domNamePrefix: actionNameDomPrefix[i],
			});
			actionView.render();
		}
		
	    this.modelBinder.bind(this.model, this.el, {
	        //title: '[name=title]',
	        text: '[name=text]'
	        //thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
	    });
	        
	    if(this.model.get("thumbnailImageUrl")) {
	        //cropImage(this.model.get("thumbnailImageUrl"));
	        //this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
	        //this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
	        
		return this;
	}
});

var ImageCarouselTemplateParentView = Backbone.View.extend({
	template: $('#rich-message-imageCarousel-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {		
		if (ImageSwiper) {
			ImageSwiper.destroy();
		}
		var html = Mustache.render(this.template, this.model.toJSON());
		$this = this;
	    this.$el.html(html);
	    
	    var childView = new ImageCarouselTemplateChildView({el: this.$el, model: this.model.get("template")});
	    childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();		
		return this;
	}
});

var ImageCarouselTemplateChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		$this = this;
		
		activateCarouselCol = function(idx) {
			var columns = $this.model.get("columns");
			
			if (columns.length <= idx) {
				for (var i=0; i <= idx; i++) {
					if (columns.length <= i) {
						columns[i] = new ImageCarouselMessageColumnModel({}, {parse: true});
						console.log("Not initialized column, create model..");
					}
				}
			}
			var childView = new ImageCarouselTemplateColumnView({ el: $this.$('.imageCarouselCol'), model: columns[idx], colMarkImgUrl: 'img/carousel-col-' + (parseInt(idx)+1) + '.png' });
			childView.render();
			
			$('[data-toggle="tooltip"]').tooltip();
		};
		
		ImageSwiper = new Swiper('#imageSwiper',
		        {
		            slidesPerView: 1,
		            loop: true,
		            spaceBetween: 60, //swiper-button-next width
		            // Navigation arrows
		            pagination: {
		                el: '.swiper-pagination',
		                type: 'bullets',
		            },
		            navigation: {
		                nextEl: '.swiper-button-next',
		                prevEl: '.swiper-button-prev',
		            },
		            on : {
		            	init : function(){
		            		activateCarouselCol(0);
		            	}
		            }
		        });					
				
		ImageSwiper.on('slideChange', function () {
			activateCarouselCol(this.realIndex);				
		});
		return this;
	}
});

var ImageCarouselTemplateColumnView = Backbone.View.extend({
	template: $('#rich-message-imageCarousel-column-tmpl').html(),
	colMarkImgUrl: null,
	initialize: function(options) {
		this.colMarkImgUrl = options.colMarkImgUrl;
		this.modelBinder = new Backbone.ModelBinder();
	},
	events: {
		'change input[name=thumbnailImageUrl]': 'onChangeThumbnail',
	},
	onChangeThumbnail: function(e) {
		var imgSrc = $(e.target).val();
	    this.$('img[name=thumbnailImageUrl]').attr('src', imgSrc);
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    this.$('img[name=colMarkImg]').attr('src', this.colMarkImgUrl);
	    if(this.model.get("thumbnailImageUrl")) {
	    	cropImage(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
    
		this.modelBinder.bind(this.model, this.el, {
			imgAltText: '[name=imgAltText]',
			uri: '[name=uri]',
			thumbnailImageUrl: 'input[name=thumbnailImageUrl]',			
		});
		return this;
	}
});

var OptionsWithIconTextParentView = Backbone.View.extend({
	domNamePrefix: "",
	template: $('#rich-message-optionsWithIconText-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();		
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		this.$el.html(html);
		$thisE1 = this.$el;
		
		var childView = new OptionsWithIconTextChildView({el: this.$el, model: this.model.get("template")});
		childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var OptionsWithIconTextChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		$this = this;
		var columns = this.model.get("columns");
		if (columns == null) {
			columns = [];
			this.model.set("columns", columns);
		}
		
		var columnNameDomPrefix = ['actionA', 'actionB', 'actionC',	'actionD'];
		
		for (var i=0; i < columnNameDomPrefix.length; i++) {
			if (i >= columns.length) {
				columns[i] = new MessageActionModel({}, {parse: true});
			}

			var actionModel = columns[i];
			var actionView = new MessageActionView({
				model: actionModel, 
				el: $("." + columnNameDomPrefix[i], this.$el),
				domNamePrefix: columnNameDomPrefix[i],
			});
			actionView.render();
		}
		
		if(this.model.get("thumbnailImageUrl")) {
	    	cropImage(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
		
		this.modelBinder.bind(this.model, this.el, {			
			thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
			iconText: '[name=iconText]',
			extendTitle : '[name=extendTitle]',
			extendContent : '[name=extendContent]',
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var OptionsWithImageTitleParentView = Backbone.View.extend({
	domNamePrefix: "",
	template: $('#rich-message-optionsWithImageTitle-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();		
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		this.$el.html(html);
		$thisE1 = this.$el;
		
		var childView = new OptionsWithImageTitleChildView({el: this.$el, model: this.model.get("template")});
		childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var OptionsWithImageTitleChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		$this = this;
		var columns = this.model.get("columns");
		if (columns == null) {
			columns = [];
			this.model.set("columns", columns);
		}
		
		var columnNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD'];
		
		for (var i=0; i < columnNameDomPrefix.length; i++) {
			if (i >= columns.length) {
				columns[i] = new MessageActionModel({}, {parse: true});
			}

			var actionModel = columns[i];
			var actionView = new MessageActionView({
				model: actionModel, 
				el: $("." + columnNameDomPrefix[i], this.$el),
				domNamePrefix: columnNameDomPrefix[i],
			});
			actionView.render();
		}
		
		if(this.model.get("thumbnailImageUrl")) {
	    	cropImage(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
		
		this.modelBinder.bind(this.model, this.el, {			
			thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
			title: '[name=title]',
			msgTitle: '[name=msgTitle]',
			msgText: '[name=msgText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var ImageModalParentView = Backbone.View.extend({
	domNamePrefix: "",
	template: $('#rich-message-imageModal-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();		
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		this.$el.html(html);
		$thisE1 = this.$el;
		
		var childView = new ImageModalChildView({el: this.$el, model: this.model.get("template")});
		childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var ImageModalChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		$this = this;
		var columns = this.model.get("columns");
		if (columns == null) {
			columns = [];
			this.model.set("columns", columns);
		}
		
		var columnNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD'];
		
		for (var i=0; i < columnNameDomPrefix.length; i++) {
			if (i >= columns.length) {
				columns[i] = new ModalActionModel({}, {parse: true});
			}

			var actionModel = columns[i];
			var actionView = new ModalActionView({
				model: actionModel, 
				el: $("." + columnNameDomPrefix[i], this.$el),
				domNamePrefix: columnNameDomPrefix[i],
			});
			actionView.render();
		}
		
		if(this.model.get("thumbnailImageUrl")) {
	    	cropImage(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
		
		this.modelBinder.bind(this.model, this.el, {			
			thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var TextModalParentView = Backbone.View.extend({
	domNamePrefix: "",
	template: $('#rich-message-textModal-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();		
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		this.$el.html(html);
		$thisE1 = this.$el;
		
		var childView = new TextModalChildView({el: this.$el, model: this.model.get("template")});
		childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var TextModalChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		$this = this;
		var columns = this.model.get("columns");
		if (columns == null) {
			columns = [];
			this.model.set("columns", columns);
		}
		
		var columnNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD'];
		
		for (var i=0; i < columnNameDomPrefix.length; i++) {
			if (i >= columns.length) {
				columns[i] = new ModalActionModel({}, {parse: true});
			}

			var actionModel = columns[i];
			var actionView = new ModalActionView({
				model: actionModel, 
				el: $("." + columnNameDomPrefix[i], this.$el),
				domNamePrefix: columnNameDomPrefix[i],
			});
			actionView.render();
		}
		
		this.modelBinder.bind(this.model, this.el, {			
			title: 'input[name=title]',
			text: 'input[name=text]',
			extendTitle: 'input[name=extendTitle]',
			extendContent: 'textarea[name=extendContent]',
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var AlertParentView = Backbone.View.extend({
	domNamePrefix: "",
	template: $('#rich-message-alert-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();		
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
		this.$el.html(html);
		$thisE1 = this.$el;
		
		var childView = new AlertChildView({el: this.$el, model: this.model.get("template")});
		childView.render();

		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

var AlertChildView = Backbone.View.extend({
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		$this = this;
		var columns = this.model.get("columns");
		if (columns == null) {
			columns = [];
			this.model.set("columns", columns);
		}
		
		var columnNameDomPrefix = ['actionA', 'actionB'];
		
		for (var i=0; i < columnNameDomPrefix.length; i++) {
			if (i >= columns.length) {
				columns[i] = new AlertActionModel({}, {parse: true});
			}

			var actionModel = columns[i];
			var actionView = new AlertActionView({
				model: actionModel, 
				el: $("." + columnNameDomPrefix[i], this.$el),
				domNamePrefix: columnNameDomPrefix[i],
			});
			actionView.render();
		}
		
		if(this.model.get("thumbnailImageUrl")) {
	    	cropImage(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').val(this.model.get("thumbnailImageUrl"));
			this.$('input[name=thumbnailImageUrl]').trigger('change');
	    }
		
		this.modelBinder.bind(this.model, this.el, {			
			thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
			title: 'input[name=title]',
			text: 'input[name=text]'
		});
		
		$('[data-toggle="tooltip"]').tooltip();
		
		return this;
	}
});

// web圖文維護view
var RichMessageDetailItemView = Backbone.View.extend({
	el: '#rich-message-detail',
	template: $('#rich-message-detail-item-tmpl').html(),
	childView: null,
	initialize: function() {
		//console.log("RichMessageDetailItemView.initialize.");
		//console.log(this.model);
		this.modelBinder = new Backbone.ModelBinder();
	    //this.listenTo(this.model, 'change', this.render);
	},
	events: {
		'click .btnSave': 'onSave',
		'click .btnSendToMe': 'onSendToMe',
		'click .btnLineLogin': 'onLINEWebLogin',
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
		var nameOfCKEditor;

		if (typeof(this.childView) != 'undefined' && this.childView) {
			this.childView = null;	// 不能 remove，會連 div 一起被 remove
		}
		
		if (msgType == 'buttons') {
			this.childView = new ButtonsTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
			nameOfCKEditor = 'text';
		}
		else if (msgType == 'carousel') {
			this.childView = new CarouselTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'imagemap') {
			this.childView = new ImagemapView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'quickReplies') {
			this.childView = new QuickRepliesParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
			nameOfCKEditor = 'text';
		}
		else if (msgType == 'imageCarousel') {
			this.childView = new ImageCarouselTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'optionsWithIconText') {
			this.childView = new OptionsWithIconTextParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'optionsWithImageTitle') {
			this.childView = new OptionsWithImageTitleParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'imageModal') {
			this.childView = new ImageModalParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'textModal') {
			this.childView = new TextModalParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'alert') {
			this.childView = new AlertParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
	    else if (msgType == 'text') {
	        this.childView = new TextParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
	        nameOfCKEditor = 'text';
	    }
	    else if (msgType == 'stretch') {
	        this.childView = new StretchTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
	        nameOfCKEditor = 'text';
	    }
	    else if (msgType == 'threegrid') {
            this.childView = new ThreeGridTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
        }
	    else if (msgType == 'twogrid') {
	        this.childView = new TwoGridTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
        }
  	    else if (msgType == 'onegrid') {
  	        this.childView = new OneGridTemplateParentView({
  	          el: this.$('.MessageDetailChildView'),
  	          model: msgTmpl // TemplateMessageModel template = OneGridMessageModel
  	        });
	    }
	    else if (msgType == 'textwithoutbutton') {
            this.childView = new TextWithoutButtonTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
	    }
	    else if (msgType == 'textwithbutton') {
	        this.childView = new TextWithButtonTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
	    }
		if (this.childView != null) {
			this.childView.render();
		}

		this.modelBinder.bind(this.model, this.el, {
			mkey: '[name=mkey]',
			msgName: '[name=msgName]',
			msgDesc: '[name=msgDesc]',
			msgType: '[name=msgType]',
		});
		// trigger event
		//$('input[name=msgType][value=' + this.model.get("msgType") + ']').trigger('change');
		
		<% if (!isAlreadyBinded) { // 還沒設定按鈕就不能按 %>
			this.$('.btnSendToMe').attr("disabled", true);
		<% } %>
		
		if (nameOfCKEditor) {
		    generateCKEditor(nameOfCKEditor);
		}
	    return this;
	},
	onSave: function(e) {
		console.log('onSave');
		console.log(this.model);
		var isValid = this.model.isValid();
		
		if (isValid) {
			this.model.save(null, {
				wait: true, 
				success: function(model, response) {
					console.log(response);
				    new PNotify({
				        title: 'Success!',
				        text: response.name + '<bean:message key='global.save.success'/>',
				        type: 'success'
				    });
				    
				    if (itemSaveCallback != null) {
				    	itemSaveCallback(target, response);
				    }
					
				    msgs.fetch({ 
						cache: false,  //Hit the server
	 					reset: true,
	 					success: function() {
	 						if (model.attributes.msgType === 'imageCarousel'){
	 							// 圖片輪播若沒有重新整理，swiper套件會怪怪的，destroy也沒有用，因此先reload待修
	 							location.reload();
	 						} else {
	 							$('#rich-message-detail').hide();	
	 						}	 						
				        }
				    });				    
			    },
			    error: function(model, response) {
			    	console.log(response);
			    	new PNotify({
					    title: 'Error!',
					    text: '<bean:message key='global.save.failed'/>',
					    type: 'error'
				    });			    	
			    }
			});
		}
		else {
			new PNotify({
			    title: 'Error!',
			    text: this.model.validationError,
			    type: 'error'
			});
		}
	},
	onSendToMe: function(e) {
		msg = this.model.recursiveToJSON();
		console.log(msg);
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
			if (newMsgType == 'buttons') {
				console.log("new ButtonsMessageModel");
				msgTmpl = new TemplateMessageModel({ type: "template", template: { type: "buttons", actions: [] }}, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'carousel') {
				console.log("new TemplateMessageModel");
				msgTmpl = new TemplateMessageModel({ type: "template", template: { type: "carousel", columns: [] }}, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'imagemap') {
				msgTmpl = new ImagemapMessageModel({ type: "imagemap" }, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'quickReplies') {
				console.log("new TemplateMessageModel");
				msgTmpl = new TemplateMessageModel({ type: "template", template: { type: "quickReplies", actions: [] }}, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'imageCarousel') {
				console.log("new TemplateMessageModel");
				msgTmpl = new TemplateMessageModel({ type: "template", template: { type: "imageCarousel", columns: []}}, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'optionsWithIconText') {
				msgTmpl = new OptionsWithIconTextMessageModel({ type: "template", template:{ type: "optionsWithIconText", columns:[], iconText: "", thumbnailImageUrl: ""} }, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'optionsWithImageTitle') {
				msgTmpl = new OptionsWithImageTitleMessageModel({ type: "template", template:{ type: "optionsWithImageTitle", columns:[], title: "", text: "", thumbnailImageUrl: ""} }, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'imageModal') {
				msgTmpl = new ImageModalMessageModel({ type: "template", template:{ type: "imageModal", columns:[], thumbnailImageUrl: ""} }, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'textModal') {
				msgTmpl = new TextModalMessageModel({ type: "template", template:{ type: "textModal", columns:[], title: "", text: ""} }, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
			else if (newMsgType == 'alert') {
				msgTmpl = new AlertMessageModel({ type: "template", template:{ type: "alert", columns:[], title: "", thumbnailImageUrl: ""} }, {parse: true});
				this.model.set("msgTemplate", msgTmpl);
				console.log(msgTmpl);
			}
	        else if (newMsgType == 'text') {
	            msgTmpl = new TextMessageModel({ type: "text", text: ""}, {parse: true});
	            this.model.set("msgTemplate", msgTmpl);
	            console.log(msgTmpl);
	        }
	        else if (newMsgType == 'stretch') {
	            msgTmpl = new TemplateMessageModel({ type: "template", template:{ type: "stretch", actions:[], text: "", stretchTitle: "", stretchText: ""} }, {parse: true});
	            this.model.set("msgTemplate", msgTmpl);
	            console.log(msgTmpl);
	        }
	        else if (newMsgType== 'onegrid') {
	            // 點選radio切換template類型時 重新把RichMessageModel的msgTemplate設定成對應類型的msgTmpl 在render時作為model傳到對應的view
	            msgTmpl = new TemplateMessageModel({  
    	            type: "template", 
        	        template:{ 
        	              type: "onegrid", 
        	              columns:[], 
        	              fixedTitle: ""
        	        }}, {
        	        // data.template = new OneGridMessageModel
        	        parse: true // call parse fucntion to transfer template to correct backbone model when create TemplateMessageModel 
        	    });
                this.model.set("msgTemplate", msgTmpl); // msgTmpl is subview's model
                console.log(msgTmpl);
            }
	        else if (newMsgType == 'threegrid') {
                msgTmpl = new TemplateMessageModel({ type: "template", template:{ type: "threegrid", columns:[], fixedTitle: ""} }, {parse: true});
                this.model.set("msgTemplate", msgTmpl);
                console.log(msgTmpl);
            }
	        else if (newMsgType == 'twogrid') {
                msgTmpl = new TemplateMessageModel({ type: "template", template:{ type: "twogrid", columns:[], fixedTitle: ""} }, {parse: true});
                this.model.set("msgTemplate", msgTmpl);
                console.log(msgTmpl);
            }
	        else if (newMsgType == 'textwithoutbutton') {
	            msgTmpl = new TemplateMessageModel({ type: "template", template:{ type: "textwithoutbutton", columns:[], fixedTitle: ""} }, {parse: true});
	            this.model.set("msgTemplate", msgTmpl);
                console.log(msgTmpl);
	        }
	        else if (newMsgType == 'textwithbutton') {
	            msgTmpl = new TemplateMessageModel({ type: "template", template:{ type: "textwithbutton", columns:[], fixedTitle: ""} }, {parse: true});
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
				alert('請選擇小於1MB的檔案!');
				$(e.target).val('');
				return;
			}
			var reader = new FileReader();
		    reader.onload = (function(theFile) {
		      return function(env) {
		    	  cropImage(env.target.result);
		    	  
		        if($(e.target).siblings('input[name=thumbnailImageUrl]').size() > 0) {
		        	$(e.target).siblings('input[name=thumbnailImageUrl]').val(env.target.result);
		        	$(e.target).siblings('input[name=thumbnailImageUrl]').trigger('change');
		        }
		        else if($(e.target).siblings('input[name=baseUrl]').size() > 0){
		        	$(e.target).siblings('input[name=baseUrl]').val(env.target.result);
		        	$(e.target).siblings('input[name=baseUrl]').trigger('change');
		        }
		      };
		    })(f);
		    reader.readAsDataURL(f);
		}
		else {
			alert('請選擇圖檔!');
			$(e.target).val('');
		}
	}
});

// 列表圖文資料view
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
		'click .btnDuplicate': 'onShowDuplicateForm',
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    return this;
	},
	onShowEditForm: function(e) {
		console.log('onShowEditForm');
		
		setTimeout( function() {
		<% if (AdminGroup.U > 0) { %>
			$('.btnSave').attr("disabled", false);
		<% } else { %>
			$('.btnSave').attr("disabled", true);
		<% } %>
		}, 100);
		
		$('html,body').animate({scrollTop:$('#top').offset().top}, 0);
		
		// Show detail view
		if (detailView != null) {
			detailView.undelegateEvents();
		}
		detailView = new RichMessageDetailItemView({model: this.model});
		detailView.render();
		$('#rich-message-detail').show();
	},
	onShowDuplicateForm: function(e) {
		console.log('onShowDuplicateForm');
		
		setTimeout( function() {
		<% if (AdminGroup.C > 0) { %>
			$('.btnSave').attr("disabled", false);
		<% } else { %>
			$('.btnSave').attr("disabled", true);
		<% } %>
		}, 100);
		
		$('html,body').animate({scrollTop:$('#top').offset().top}, 0);
		
		// Show detail view
		if (detailView != null) {
			detailView.undelegateEvents();
		}
		this.DupModel = this.model.clone();
		this.DupModel.set("mkey", "");
		this.DupModel.set("id", null);
		detailView = new RichMessageDetailItemView({model: this.DupModel});
		detailView.render();
		$('#rich-message-detail').show();
	},
	onDelete: function(e) {
		if (confirm('Are you sure?')) {
			console.log('onDelete');
			// Show detail view
			if (detailView != null) {
				detailView.undelegateEvents();
			}
	
			this.model.destroy({wait: true, success: function() {
				setTimeout( function() {
					new PNotify({
					    title: 'Success!',
					    text: 'The rich message has been deleted',
					    type: 'success'
					});
					
					msgs.fetch({ 
						cache: false,  //Hit the server
	 					reset: true,
						success: function() {
							$('#rich-message-detail').hide();
						}
					})
				}, 500);
			}});
		}
	}
});

// 列表view
var RichMessageListView = Backbone.View.extend({
	el: '#rich-message-list',
	template: $('#rich-message-list-tmpl').html(),
	initialize: function() {
	    this.listenTo(this.collection, 'sync change', this.render);
	},
	events: {
		'click .btnCreate': 'onShowCreateForm',
		'click .btnExport': 'onExportRichMessages'
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
		
		$('html,body').animate({scrollTop:$('#top').offset().top}, 0);

		// Show detail view
		if (detailView != null) {
			detailView.undelegateEvents();
		}
		detailView = new RichMessageDetailItemView({model: new RichMessageModel()});
		detailView.render();
		
		$('#rich-message-detail').show();
	},
	onExportRichMessages: function() {
		window.location = '<%=request.getContextPath() %>/wiseadm/webRM/exportRichMessageAnswers.jsp?action=export';
	}
});

var msgs = new RichMessageCollection();
	
$(function() {
	var msgsView = new RichMessageListView({collection: msgs});
	msgs.fetch({
		cache: false,  //Hit the server
	});
	
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

function cropImage(src) {
	var $divThumbnail = $('#div-thumbnail');
	var $img = $divThumbnail.children('img');
    var options = {
            autoCropArea: 1,
            preview: '#preview',
            cropend: function() {
            	var $parent = $divThumbnail.parent();
                var canvas = $img.cropper('getCroppedCanvas');
                if ($parent.siblings('input[name=thumbnailImageUrl]').size() > 0) {
                	$parent.siblings('input[name=thumbnailImageUrl]').val(canvas.toDataURL());
                	$parent.siblings('input[name=thumbnailImageUrl]').trigger('change');
                }
                else if ($parent.siblings('input[name=baseUrl]').size() > 0) {
                	$parent.siblings('input[name=baseUrl]').val(canvas.toDataURL());
                	$parent.siblings('input[name=baseUrl]').trigger('change');
                }
            }
    };
    
    $img.cropper('destroy').attr('src', src).cropper(options);
}

//CKEditor global setting
CKEDITOR.config.baseHref = '<%= contextPath %>/';
CKEDITOR.config.language = '<%= locale.getLanguage() %>';

CKEDITOR.on('dialogDefinition', function(ev) {
    var dialogName = ev.data.name;
    var dialogDefinition = ev.data.definition;
    if(dialogName == 'link') {
        var targetTab = dialogDefinition.getContents('target');
        var advancedTab = dialogDefinition.getContents('advanced');
        var linkTargetField = targetTab.get('linkTargetType');
        var advCssField = advancedTab.get('advCSSClasses');
        linkTargetField['default'] = '_blank';
        advCssField['default'] = 'answer-link';
    }
});

function generateCKEditor(target) {
    if (CKEDITOR.instances.target) {
        CKEDITOR.instances.target.destroy();
    }
	
    var editor =  CKEDITOR.replace(target, {
        customConfig: '<%= contextPath %>/ckeditor/richmessage_config.js'
    });
    
    editor.on('change', function( evt ) {
    	$textarea = $(editor.element.$);
    	$textarea.val(editor.getData()).trigger('update');
    });
}

// attach import type
function submitForm(form) {
	form = $(form);
	type = form.find(':selected').val();
    if (type) {
    	form.attr('action', function(i, value) {
    		return value + "&type=" + type;
    	});
    	return true;
    } else {
    	return false;
    }
}
</script>
</body>
</html>
