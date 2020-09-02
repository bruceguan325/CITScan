<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.io.*"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.connector.line.*"
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
%>
<%!
String loginAndGetUser(Tenant t, String code, String state) throws JSONException {
	JSONObject lineWlCfg = new JSONObject(t.getLineBotConfigJson()).optJSONObject("lineWebLogin");
	JSONObject botConfig = new JSONObject(t.getLineBotConfigJson()).getJSONObject("line");
	String channelId = lineWlCfg.getString("channelId");
	String channelSecret = lineWlCfg.getString("channelSecret");
	String redirectUrl = lineWlCfg.getString("redirectUrl");
	
	JSONObject replyJSON = new JSONObject();
	JSONObject respProfile = new JSONObject();

	CloseableHttpClient httpClient = HttpClientBuilder.create().build();
	try {
		HttpPost reqP = new HttpPost("https://api.line.me/oauth2/v2.1/token");
	    StringEntity params = new StringEntity("grant_type=authorization_code&code=" + code +
	    	"&redirect_uri=" + redirectUrl + "/wise/wiseadm/line/qaRichMessageList.jsp" + 
	    	"&client_id=" + channelId + "&client_secret=" + channelSecret);
	    reqP.addHeader("content-type", "application/x-www-form-urlencoded");
	    reqP.setEntity(params);
	    HttpResponse rep = httpClient.execute(reqP);
	    
	    if (rep != null) {
	    	System.out.println( "StatusCode : "+rep.getStatusLine().getStatusCode());
	    	
	        BufferedReader in = new BufferedReader(
	                new InputStreamReader(rep.getEntity().getContent()));
	        String inputLine;
	        StringBuffer resultData = new StringBuffer();

	        while ((inputLine = in.readLine()) != null) {
	        	resultData.append(inputLine);
	        }
	        in.close();
	        replyJSON = new JSONObject(resultData.toString());
			System.out.println("access token : " + replyJSON.toString());
	        
	        HttpGet requG = new HttpGet("https://api.line.me/v2/profile");
	        requG.addHeader("content-type", "application/json");
	        requG.addHeader("Authorization", "Bearer " + replyJSON.getString("access_token"));
	        rep = httpClient.execute(requG);
	         
	        if (rep != null) {
	         	System.out.println( "StatusCode : "+rep.getStatusLine().getStatusCode());
	         	
	             in = new BufferedReader(
	                     new InputStreamReader(rep.getEntity().getContent(), "UTF-8"));
	             resultData = new StringBuffer();

	             while ((inputLine = in.readLine()) != null) {
	             	resultData.append(inputLine);
	             }
	             in.close();
	             
	             respProfile = new JSONObject(resultData.toString());
	             JSONArray message = new JSONArray();
	             message.add(0, LINEBotApi.buildTextMessage("login success"));
	             LINEBotApi.push(botConfig.getString("accessToken"), respProfile.getString("userId"),
	            		 message);
	        }
	    }
	} catch (Exception ex) {
		System.out.println(ex.toString());
	} finally {
	    httpClient.getConnectionManager().shutdown();
	}
	
	return respProfile.getString("userId");
}
%>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
String errorMsg = "";

JSONObject lineConfig = null;
try {
	lineConfig = new JSONObject(t.getLineBotConfigJson());
} catch (Exception e) {
	lineConfig = new JSONObject();
}
boolean hasLineWlCfg = lineConfig.has("lineWebLogin");
boolean isAlreadyBinded = false;

JSONObject lineWlCfg = hasLineWlCfg ? lineConfig.optJSONObject("lineWebLogin") : new JSONObject();

UserClue uc = UserClue.getByAdminUserId(t.getId(), user.getId());
String code = request.getParameter("code");
String state = request.getParameter("state");

if (code != null && state != null) {
	if (uc == null) {
		uc = new UserClue();
		uc.setTenantId(t.getId());
		uc.setAdminUserId(user.getId());
	}
	String userId = loginAndGetUser(t, code, state);
	uc.setLineUserId(userId);
	UserClue.saveOrUpdate(uc);
}

isAlreadyBinded = uc != null && uc.getLineUserId() != null;

if (!hasLineWlCfg) {
	errorMsg = "LINE 相關配置尚未設定，請洽系統管理員配置後相關功能才能正常運作<br>";
}
if (!isAlreadyBinded) {
	errorMsg += "請使用 LINE Web Login 後才能使用傳送到我的LINE<br>";
}

%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='qa.richmessage.navbar.line'/></title>
<jsp:include page="../header-qa.jsp"></jsp:include>
<link href="../css/coverflow.css" rel="stylesheet">
<link rel="stylesheet" href="<%= request.getContextPath() %>/styles/cropper.min.css">
<script src="<%= request.getContextPath() %>/script/mustache.min.js"></script>
<script src="<%= request.getContextPath() %>/script/underscore-min.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone-min.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone.ModelBinder.js"></script>
<script src="<%= request.getContextPath() %>/script/backbone.localStorage.min.js"></script>
<script src="../js/coverflow.js"></script>
<script src="<%= request.getContextPath() %>/script/cropper.min.js"></script>
<script src="<%= request.getContextPath() %>/script/jquery-cropper.min.js"></script>
<script type="x-tmpl-mustache" id="rich-message-list-item-tmpl">
	<td>{{mkey}}</td>
	<td><strong>{{msgType}}</strong></td>
	<td><strong>{{msgName}}</strong></td>
	<td style="word-wrap:break-word;">{{msgDesc}}</td>
	<td>{{formattedUpdateTime}}</td>
	<td>{{expireTime}}</td>
	<td>
		<button class='btn btn-success btnEdit' title="<bean:message key='modify'/>"><span class="glyphicon glyphicon-pencil"></span></button>
		<button class='btn btn-default btnDuplicate' title="<bean:message key='copy'/>"><span class="glyphicon glyphicon-duplicate"></span></button>
		<button class='btn btn-danger btnDelete' title="<bean:message key='delete'/>"><span class="glyphicon glyphicon-trash"></span></button>
	</td>
</script>
<script type="x-tmpl-mustache" id="rich-message-detail-item-tmpl">
<div class="col-md-12" style="padding-top: 10px; padding-bottom: 10px;">
	<div class="col-md-12"> 
		<div class="col-md-2"> 操作 </div>
		<div class="col-md-8" style="margin-bottom: 5px;">
		<button class="btn btn-danger btnSave"><i class="glyphicon glyphicon-floppy-disk"></i> 儲存</button>
		&nbsp;&nbsp;<button class="btn btn-info btnSendToMe"><i class="glyphicon glyphicon-screenshot"></i> 傳送到我的LINE</button>
		&nbsp;&nbsp;<button class="btn btn-success btnLineLogin"><i class="glyphicon glyphicon-user"></i> LINE Web Login</button>
		</div>
	</div>
	<div class="col-md-12"> 
		<div class="col-md-2"> MKEY </div>
		<div class="col-md-8">
		<input type="text" name="mkey" class="form-control RichMessageModel" placeholder="MKey">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> 訊息名稱 </div>
		<div class="col-md-8">
		<input type="text" name="msgName" class="form-control RichMessageModel" placeholder="訊息名稱（管理用）">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> 訊息說明 </div>
		<div class="col-md-8">
		<input type="text" name="msgDesc" class="form-control RichMessageModel" placeholder="訊息名稱（管理用）">
		</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2"> 訊息格式 </div>
		<div class="col-md-8">
			<div class="col-md-12">
			<input type="radio" name="msgType" data-target-div="richMessageType-buttons-div" value="buttons" class="RichMessageModel">Buttons
			&nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-carousel-div" value="carousel" class="RichMessageModel">Carousel
			&nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-imagemap-div" value="imagemap" class="RichMessageModel">Imagemap
			&nbsp;&nbsp;&nbsp;<input type="radio" name="msgType" data-target-div="richMessageType-flex-div" value="flex" class="RichMessageModel">Flex
			</div>
		</div>
	</div>
	<div class="row MessageDetailChildView">
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-buttons-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-buttons-div">
	<div class="col-md-12">
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120">訊息標題<img style="width: 120px;" src="img/rich-message-type-buttons.png"></td>
				<td colspan=2>
					<input type="text" class="form-control" name="title"><small>Max: 40 characters</small>
				</td>
			</tr>
			<tr>
				<td>訊息內文</td>
				<td colspan=2><input type="text" class="form-control" name="text">
					<small>
					Max: 160 characters (no image or title)
					<br>Max: 60 characters (message with an image or title)
					</small>
				</td>
			</tr>
			<tr>
				<td>替代文字</td>
				<td colspan=2><input type="text" name="altText" class="form-control">
					<small>
					Max: 400 characters
					</small>
				</td>
			</tr>
			<tr>
				<td>圖片</td>
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
					<br>HTTPS
					<br>JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
			<tr class='actionA'>
				<td rowspan="4">按鈕 <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
				<td width=20>A</td>
				<td>
					<input type="radio" name="actionAtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionAtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionAtype" value="postback" data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>按鈕標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span>
					<input type="text" name="actionAlabel" class="form-control" placeholder="空白標題將會disable此按鈕">
					<label>資料</label>
					<input type="text" name="actionAtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionAuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionAdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionB'>
				<td>B</td>
				<td>
					<input type="radio" name="actionBtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionBtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionBtype" value="postback" data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>按鈕標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span>
					<input type="text" name="actionBlabel" class="form-control" placeholder="空白標題將會disable此按鈕">
					<label>資料</label>
					<input type="text" name="actionBtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionBuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionBdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionC'>
				<td>C</td>
				<td>
					<input type="radio" name="actionCtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionCtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionCtype" value="postback" data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>按鈕標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span>
					<input type="text" name="actionClabel" class="form-control" placeholder="空白標題將會disable此按鈕">
					<label>資料</label>
					<input type="text" name="actionCtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionCuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionCdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionD'>
				<td>D</td>
				<td>
					<input type="radio" name="actionDtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionDtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionDtype" value="postback" data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>按鈕標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span>
					<input type="text" name="actionDlabel" class="form-control" placeholder="空白標題將會disable此按鈕">
					<label>資料</label>
					<input type="text" name="actionDtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionDuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionDdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-carousel-column-tmpl">
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120">訊息標題<img name="colMarkImg" style="width: 120px;" src="img/carousel-col-default.png"></td>
				<td colspan=2><input type="text" name="title" class="form-control"><small>Max: 40 characters</small></td>
			</tr>
			<tr>
				<td>訊息內文</td>
				<td colspan=2>
					<input type="text" name="text" class="form-control" placeholder="本欄若留空白，儲存後會自動刪除本張圖卡">
					<small>
					Required
					<br>Max: 160 characters (no image or title)
					<br>Max: 60 characters (message with an image or title)
					</small>
				</td>
			</tr>
			<tr>
				<td>圖片</td>
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
					<br>HTTPS
					<br>JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
			<tr class='actionA'>
				<td rowspan="4">按鈕 <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span></td>
				<td width=20>A</td>
				<td>
					<input type="radio" name="actionAtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionAtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionAtype" value="postback" data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>按鈕標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span>
					<input type="text" name="actionAlabel" class="form-control" placeholder="空白標題將會disable此按鈕">
					<label>資料</label>
					<input type="text" name="actionAtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionAuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionAdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionB'>
				<td>B</td>
				<td>
					<input type="radio" name="actionBtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionBtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionBtype" value="postback" data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>按鈕標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span>
					<input type="text" name="actionBlabel" class="form-control" placeholder="空白標題將會disable此按鈕">
					<label>資料</label>
					<input type="text" name="actionBtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionBuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionBdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionC'>
				<td>C</td>
				<td>
					<input type="radio" name="actionCtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionCtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionCtype" value="postback" data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>按鈕標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 按鈕，清空按鈕標題即可"></span>
					<input type="text" name="actionClabel" class="form-control" placeholder="空白標題將會disable此按鈕">
					<label>資料</label>
					<input type="text" name="actionCtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionCuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionCdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
		</table>	
</script>

<script type="x-tmpl-mustache" id="rich-message-carousel-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-carousel-div">
	<div class="col-md-12">
	<div class="coverflow" style="display: none;">
	<% // template 太長且重複，用 jsp loop 產生
	int maxCol = 10;
	for (int i=0; i < maxCol; i++) {
		String prefix = "carouselCol" + 0;
	%>
		<img data-col="<%=i%>" src="img/carousel-col-<%=i+1%>.png">
	<%
	}
	%>
	</div>
	</div>
	<div class="col-md-12">
		<div class="col-md-2">替代文字</div>
		<div class="col-md-8"><input type="text" name="altText" class="form-control">
			<small>
			Max: 400 characters
			</small>
		</div>
	</div>
	<div name="carouselCol" class="col-md-12 carouselCol">
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
	</div>
	<div class="col-md-12">
		<table class="table table-bordered table-striped">
			<tr>
				<td>替代文字</td>
				<td colspan=2><input type="text" name="altText" class="form-control">
					<small>
					Max: 400 characters
					</small>
				</td>
			</tr>
			<tr>
				<td>背景圖片</td>
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
			<tr class='actionA'>
				<td rowspan="9">可點選區域 <span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span></td>
				<td width=20>A</td>
				<td>
					<input type="radio" name="actionAtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionAtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionAtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionAlabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionAtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionAuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionAdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionB'>
				<td>B</td>
				<td>
					<input type="radio" name="actionBtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionBtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionBtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionBlabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionBtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionBuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionBdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionC'>
				<td>C</td>
				<td>
					<input type="radio" name="actionCtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionCtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionCtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionClabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionCtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionCuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionCdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionD'>
				<td>D</td>
				<td>
					<input type="radio" name="actionDtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionDtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionDtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionDlabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionDtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionDuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionDdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionE'>
				<td>E</td>
				<td>
					<input type="radio" name="actionEtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionEtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionEtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionElabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionEtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionEuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionEdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionF'>
				<td>F</td>
				<td>
					<input type="radio" name="actionFtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionFtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionFtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionFlabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionFtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionFuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionFdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionG'>
				<td>G</td>
				<td>
					<input type="radio" name="actionGtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionGtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionGtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionGlabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionGtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionGuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionGdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionH'>
				<td>H</td>
				<td>
					<input type="radio" name="actionHtype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionHtype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionHtype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionHlabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionHtext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionHuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionHdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
			<tr class='actionI'>
				<td>I</td>
				<td>
					<input type="radio" name="actionItype" value="message" checked="true">文字 &nbsp;&nbsp;
					<input type="radio" name="actionItype" value="uri">連結 &nbsp;&nbsp;
					<input type="radio" name="actionItype" value="postback" data-toggle="tooltip" disabled title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">
						<span data-toggle="tooltip" title="此類型的按鈕，點下後不會出現文字，但會在背景發送問句">文字(後端發送)</span>
					<br>
					<label>標題</label><span class="glyphicon glyphicon-info-sign" data-toggle="tooltip" title="若要 disable 區域，清空標題即可"></span>
					<input type="text" name="actionIlabel" class="form-control" placeholder="空白標題將會disable此區域">
					<label>資料</label>
					<input type="text" name="actionItext" class="form-control" placeholder="輸入問句">
					<input type="text" name="actionIuri" class="form-control" placeholder="http://xxx.xxx.xxx">
					<input type="text" name="actionIdata" class="form-control" placeholder="後端發送問句的格式為 action=_message&message=XXXX">
				</td>
			</tr>
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-flex-tmpl">
<div class="col-md-12 richMessageTypeDiv richMessageType-flex-div">
	<div class="col-md-12">
		<table class="table table-bordered table-striped">
			<tr>
				<td class="col-md-2">替代文字</td>
				<td><input type="text" name="altText" class="form-control">
					<small>
					Max: 400 characters
					</small>
				</td>
			</tr>
			<tr>
				<td class="col-md-2">
					Flex 內容 JSON<br>
					<a href="https://developers.line.me/console/fx/" target="_blank">官方模擬器</a>
				</td>
				<td>
					<textarea class="form-control" name="contentsString" rows="30"></textarea>
					<textarea class="form-control hide" name="contents" rows="30"></textarea>
					<small>
					</small>
				</td>
			</tr>
		</table>	
	</div>
</div>
</script>

<script type="x-tmpl-mustache" id="rich-message-list-tmpl">
<table class='table table-strip'>
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
<button class='btn btn-success btnCreate'><i class="glyphicon glyphicon-plus"></i> <bean:message key='global.add'/></button>
<button class='btn btn-success btnExport'><bean:message key='search.export'/></button>
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
#div-thumbnail, #preview {
    display: inline-block;
    max-width: 400px;
}
#preview {
    overflow: hidden;
}
</style>
</head>
<body>
<jsp:include page="../navbar-qa.jsp"></jsp:include>
<div class="container">
	<% if (StringUtils.isNotEmpty(errorMsg)) { %>
		<div id="errorMsg" class="alert alert-danger"><%= errorMsg %></div>
	<% } %>
	
	<h3><bean:message key='line.rich.message.batch.import'/><br></h3><br> 
	<form name="UploadForm" enctype="multipart/form-data" method="post" action="<%= request.getContextPath() %>/wiseadm/line/qaRichMessageUploadFile">
    	<table width="100%">
    		<tr>
    			<th width="10%"><input type="file" name="File1" size="20" maxlength="20"> </th>
    			<th width="30%"><input type="submit"value="<bean:message key='global.import.csv'/>"></th>
    		</tr>
    	</table>
	</form>
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
		//console.log("MessageActionView render() start");
		//console.log(this);
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
		//console.log(this);
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
		//console.log("MessageActionView render() end");
		return this;
	}
});

var ButtonsTemplateParentView = Backbone.View.extend({
	template: $('#rich-message-buttons-tmpl').html(),
	initialize: function() {
		//console.log("ButtonsTemplateParentView.initialize.");
		//console.log(this.model);
		this.modelBinder = new Backbone.ModelBinder();
	},
	render: function() {
		//console.log(this.model);
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    $thisEl = this.$el;
	    
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
		//console.log("ButtonsTemplateChildView.initialize.");
		//console.log(this.model);
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
		this.modelBinder.bind(this.model, this.el, {
			title: '[name=title]',
			text: '[name=text]',
			thumbnailImageUrl: 'input[name=thumbnailImageUrl]',
		});
		
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
		
		var actionNameDomPrefix = ['actionA', 'actionB', 'actionC', 'actionD'];
		
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
		return this;
	}
});

var CarouselTemplateParentView = Backbone.View.extend({
	template: $('#rich-message-carousel-tmpl').html(),
	initialize: function() {
		//console.log("CarouselTemplateParentView.initialize.");
		//console.log(this.model);
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
		//console.log("CarouselTemplateChildView.initialize.");
		//console.log(this.model);
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
			altText: '[name=altText]'
		});
		return this;
	}
});

var CarouselTemplateColumnView = Backbone.View.extend({
	template: $('#rich-message-carousel-column-tmpl').html(),
	colMarkImgUrl: null,
	initialize: function(options) {
		//console.log("CarouselTemplateColumnView.initialize.");
		//console.log(this.model);
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
		
		var actions = this.model.get("actions");
		if (actions == null) {
			actions = [];
			this.model.set("actions", actions);
		}
		
		var actionNameDomPrefix = ['actionA', 'actionB', 'actionC'];
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
	actionNameDomPrefix: ['actionA', 'actionB', 'actionC', 'actionD', 'actionE', 'actionF', 'actionG', 'actionH', 'actionI'],
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

var FlexMessageView = Backbone.View.extend({
	template: $('#rich-message-flex-tmpl').html(),
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
	},
	events: {
		'change textarea[name=contentsString]': 'onChangeFlexContents',
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    $thisEl = this.$el;
		var contentsJson = this.model.get("contents");
		this.$('textarea[name=contentsString]').text(JSON.stringify(contentsJson, null, "\t"));
	    
		this.modelBinder.bind(this.model, this.el, {
			altText: '[name=altText]',
			contents: 'textarea[name=contents]'
		});
		
		return this;
	},
	onChangeFlexContents: function(e) {
		var jsonStr = $(e.target).val();
		console.log("onChangeFlexContents: " + jsonStr);
		this.model.set("contents", JSON.parse(jsonStr));
		this.render();
		return false;
	}
});

var RichMessageDetailItemView = Backbone.View.extend({
	el: '#rich-message-detail',
	template: $('#rich-message-detail-item-tmpl').html(),
	childView: null,
	initialize: function() {
		this.modelBinder = new Backbone.ModelBinder();
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

		if (typeof(this.childView) != 'undefined' && this.childView) {
			this.childView = null;	// 不能 remove，會連 div 一起被 remove
		}
		
		if (msgType == 'buttons') {
			this.childView = new ButtonsTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'carousel') {
			this.childView = new CarouselTemplateParentView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'imagemap') {
			this.childView = new ImagemapView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
		}
		else if (msgType == 'flex') {
			this.childView = new FlexMessageView( { el: this.$('.MessageDetailChildView'), model: msgTmpl } );
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
		<% if (!hasLineWlCfg) { // 還沒設定按鈕就不能按 %>
			this.$('.btnLineLogin').attr("disabled", true);
		<% } %>
		
	    return this;
	},
	onSave: function(e) {
		console.log('onSave');
		var isValid = this.model.isValid();
		//console.log('isValid:' + isValid);
		
		if (isValid) {
			this.model.save(null, {
				wait: true, 
				success: function(model, response) {
				    new PNotify({
				        title: 'Success!',
				        text: 'The rich message has been saved',
				        type: 'success'
				    });
					
				    msgs.fetch({ 
				        success: function() {
				    	    $('#rich-message-detail').hide();
				        }
				    });
			    },
			    error: function(model, response) {
			    	console.log(response);
			    	new PNotify({
					    title: 'Error!',
					    text: 'save fail!',
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
		
		$.ajax({
			url: 'qaRichMessage-ajax.jsp',
			dataType: 'JSON',
			method: 'POST',
			data: {
				action: 'sendToMe',
				msgType: msg.msgType,
				msg: JSON.stringify(msg.msgTemplate)
			},
			success: function(result) {
				if (result.StatusCode && result.StatusCode == 200) {
					new PNotify({
					    title: 'Success!',
					    text: '請在手機上查看訊息內容',
					    type: 'success'
					});
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
	onLINEWebLogin: function(e) {
		var channelId = '<%=lineWlCfg.optString("channelId") %>';
		var redirectUrl = '<%=lineWlCfg.optString("redirectUrl") %>';
		location.href = 'https://access.line.me/oauth2/v2.1/authorize?response_type=code&client_id=' + channelId + '&redirect_uri=' + redirectUrl + '/wise/wiseadm/line/qaRichMessageList.jsp&state=none&scope=openid%20profile';
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
				console.log("new TemplateMessageModel");
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
			else if (newMsgType == 'flex') {
				msgTmpl = new FlexMessageModel({ type: "flex" }, {parse: true});
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
	},
	render: function() {
		var html = Mustache.render(this.template, this.model.toJSON());
	    this.$el.html(html);
	    return this;
	},
	onShowEditForm: function(e) {
		console.log('onShowEditForm');
		// Show detail view
		if (detailView != null) {
			detailView.undelegateEvents();
		}
		detailView = new RichMessageDetailItemView({model: this.model});
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
					
					msgs.fetch({ success: function() {
						$('#rich-message-detail').hide();
					}})
				}, 500);
			}});
		}
	}
});

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
	    return this;
	},
	onShowCreateForm: function() {
		console.log('onShowCreateForm');

		// Show detail view
		if (detailView != null) {
			detailView.undelegateEvents();
		}
		detailView = new RichMessageDetailItemView({model: new RichMessageModel()});
		detailView.render();
		
		$('#rich-message-detail').show();
	},
	onExportRichMessages: function() {
		window.location = '<%=request.getContextPath() %>/wiseadm/line/exportRichMessageAnswers.jsp';
	}
});

var msgs = new RichMessageCollection();
	
$(function() {
	var msgsView = new RichMessageListView({collection: msgs});
	msgs.fetch();
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
</script>
</body>
</html>
