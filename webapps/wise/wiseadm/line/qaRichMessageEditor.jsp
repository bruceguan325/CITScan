<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="java.util.*"
import="org.apache.commons.lang.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.connector.line.*"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E2) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%><%!
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title>LINE Rich Message Editor</title>
<jsp:include page="../header-qa.jsp"></jsp:include>
<link href="../css/coverflow.css" rel="stylesheet">
<script src="../js/coverflow.js"></script>
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
</style>
</head>
<body>
<jsp:include page="../navbar-qa.jsp"></jsp:include>
<div class="container">
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
List<RichMessage> msgs = RichMessage.list(t.getId(), null);
int no = 1;
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
request.setAttribute("msgs", msgs);
%>
<div class="col-md-12" style="padding-top: 10px; padding-bottom: 10px;">
	<div class="col-md-2">
	<input type="radio" name="richMessageType" data-target-div="richMessageType-buttons-div" value="buttons">Buttons
	</div>
	<div class="col-md-2">
	<input type="radio" name="richMessageType" data-target-div="richMessageType-carousel-div" value="carousel">Carousel
	</div>
	<div class="col-md-2">
	<input type="radio" name="richMessageType" data-target-div="richMessageType-imagemap-div" value="imagemap">Imagemap
	</div>
</div>
<div class="col-md-12 richMessageTypeDiv" id="richMessageType-buttons-div" style="display: none;">
	<div class="col-md-12">
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120">訊息標題<img style="width: 120px;" src="img/rich-message-type-buttons.png"></td>
				<td colspan=2><input type="text" class="form-control"><small>Max: 40 characters</small></td>
			</tr>
			<tr>
				<td>訊息內文</td>
				<td colspan=2><input type="text" class="form-control">
					<small>
					Max: 160 characters (no image or title)
					<br>Max: 60 characters (message with an image or title)
					</small>
				</td>
			</tr>
			<tr>
				<td>圖片</td>
				<td colspan=2><div class="msgImg"></div><input type="file" class="form-control">
					<small>Image URL (Max: 1000 characters)
					<br>HTTPS
					<br>JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
			<tr>
				<td rowspan="4">按鈕</td>
				<td width=20>A</td>
				<td>
					<input type="radio" name="buttonAactionType" value="message">文字
					<input type="radio" name="buttonAactionType" value="uri">連結
					<input type="radio" name="buttonAactionType" value="postback">文字(後端發送)
					<br>
					<label>按鈕標題</label><input type="text" name="buttonAtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonAdata" class="form-control">
					<small>文字(後端發送)類型的按鈕，點下後不會出現文字，但會在背景發送問句</small>
				</td>
			</tr>
			<tr>
				<td>B</td>
				<td>
					<input type="radio" name="buttonBactionType" value="message">文字
					<input type="radio" name="buttonBactionType" value="uri">連結
					<input type="radio" name="buttonBactionType" value="postback">文字(後端發送)
					<br>
					<label>按鈕標題</label><input type="text" name="buttonBtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonBdata" class="form-control">
					<small>文字(後端發送)類型的按鈕，點下後不會出現文字，但會在背景發送問句</small>
				</td>
			</tr>
			<tr>
				<td>C</td>
				<td>
					<input type="radio" name="buttonCactionType" value="message">文字
					<input type="radio" name="buttonCactionType" value="uri">連結
					<input type="radio" name="buttonCactionType" value="postback">文字(後端發送)
					<br>
					<label>按鈕標題</label><input type="text" name="buttonCtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonCdata" class="form-control">
					<small>文字(後端發送)類型的按鈕，點下後不會出現文字，但會在背景發送問句</small>
				</td>
			</tr>
			<tr>
				<td>D</td>
				<td>
					<input type="radio" name="buttonDactionType" value="message">文字
					<input type="radio" name="buttonDactionType" value="uri">連結
					<input type="radio" name="buttonDactionType" value="postback">文字(後端發送)
					<br>
					<label>按鈕標題</label><input type="text" name="buttonDtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonDdata" class="form-control">
					<small>文字(後端發送)類型的按鈕，點下後不會出現文字，但會在背景發送問句</small>
				</td>
			</tr>
		</table>	
	</div>
</div>
<div class="col-md-12 richMessageTypeDiv" id="richMessageType-carousel-div" style="display: none;">
	<div class="col-md-8">
	<div id="coverflow">
	<%
	int maxCol = 10;
	for (int i=1; i <= maxCol; i++) {
		String prefix = "carouselCol" + i;
	%>
		<img data-col="<%=i%>" src="img/carousel-col-<%=i%>.png">
	<%
	}
	%>
	</div>
	</div>
	<%
	for (int i=1; i <= maxCol; i++) {
		String prefix = "carouselCol" + i;
	%>
	<div id="carouselCol<%=i%>" class="col-md-12 carouselCol" style="display: none;">
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120">訊息標題<img style="width: 120px;" src="img/carousel-col-<%=i%>.png"></td>
				<td colspan=2><input type="text" name="<%=prefix%>title" class="form-control"><small>Max: 40 characters</small></td>
			</tr>
			<tr>
				<td>訊息內文</td>
				<td colspan=2><input type="text" name="<%=prefix%>text" class="form-control">
					<small>
					Max: 160 characters (no image or title)
					<br>Max: 60 characters (message with an image or title)
					</small>
				</td>
			</tr>
			<tr>
				<td>圖片</td>
				<td colspan=2><div class="msgImg"></div><input type="file" class="form-control">
					<small>Image URL (Max: 1000 characters)
					<br>HTTPS
					<br>JPEG or PNG
					<br>Max width: 1024px
					<br>Max: 1 MB
					</small>
				</td>
			</tr>
			<tr>
				<td rowspan="4">按鈕</td>
				<td width=20>A</td>
				<td>
					<input type="radio" name="<%=prefix%>buttonAactionType" value="message">文字
					<input type="radio" name="<%=prefix%>buttonAactionType" value="uri">連結
					<input type="radio" name="<%=prefix%>buttonAactionType" value="postback">文字(後端發送)
					<br>
					<label>按鈕標題</label><input type="text" name="<%=prefix%>buttonAtitle" class="form-control">
					<label>資料</label><input type="text" name="<%=prefix%>buttonAdata" class="form-control">
					<small>文字(後端發送)類型的按鈕，點下後不會出現文字，但會在背景發送問句</small>
				</td>
			</tr>
			<tr>
				<td>B</td>
				<td>
					<input type="radio" name="<%=prefix%>buttonBactionType" value="message">文字
					<input type="radio" name="<%=prefix%>buttonBactionType" value="uri">連結
					<input type="radio" name="<%=prefix%>buttonBactionType" value="postback">文字(後端發送)
					<br>
					<label>按鈕標題</label><input type="text" name="<%=prefix%>buttonBtitle" class="form-control">
					<label>資料</label><input type="text" name="<%=prefix%>buttonBdata" class="form-control">
					<small>文字(後端發送)類型的按鈕，點下後不會出現文字，但會在背景發送問句</small>
				</td>
			</tr>
			<tr>
				<td>C</td>
				<td>
					<input type="radio" name="<%=prefix%>buttonCactionType" value="message">文字
					<input type="radio" name="<%=prefix%>buttonCactionType" value="uri">連結
					<input type="radio" name="<%=prefix%>buttonCactionType" value="postback">文字(後端發送)
					<br>
					<label>按鈕標題</label><input type="text" name="<%=prefix%>buttonCtitle" class="form-control">
					<label>資料</label><input type="text" name="<%=prefix%>buttonCdata" class="form-control">
					<small>文字(後端發送)類型的按鈕，點下後不會出現文字，但會在背景發送問句</small>
				</td>
			</tr>
		</table>	
	</div>
	<% 
	}
	%>
</div>
<div class="col-md-12 richMessageTypeDiv" id="richMessageType-imagemap-div" style="display: none;">
	<div class="col-md-12">
	<div class="imageMapTypeDiv">
		<img src="img/type_1_thumb.png">
		<input type="radio" name="imageMapType" value="type1">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_2_thumb.png">
		<input type="radio" name="imageMapType" value="type2">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_3_thumb.png">
		<input type="radio" name="imageMapType" value="type3">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_4_thumb.png">
		<input type="radio" name="imageMapType" value="type4">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_5_thumb.png">
		<input type="radio" name="imageMapType" value="type5">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_6_thumb.png">
		<input type="radio" name="imageMapType" value="type6">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_7_thumb.png">
		<input type="radio" name="imageMapType" value="type7">
	</div>
	<div class="imageMapTypeDiv">
		<img src="img/type_8_thumb.png">
		<input type="radio" name="imageMapType" value="type8">
	</div>
	</div>
	<div class="col-md-12">
		<table class="table table-bordered table-striped">
			<tr>
				<td width="120">訊息標題</td>
				<td colspan=2><input type="text" class="form-control"><small>Max: 100 characters</small></td>
			</tr>
			<tr>
				<td>背景圖片</td>
				<td colspan=2><div class="msgImg"></div><input type="file" class="form-control">
					<small>JPEG or PNG
					<br>Must be 1040x1040
					</small>
				</td>
			</tr>
			<tr>
				<td rowspan="4">可點選區</td>
				<td width=20>A</td>
				<td>
					<input type="radio" name="buttonAactionType" value="message">文字
					<input type="radio" name="buttonAactionType" value="uri">連結
					<br>
					<label>標題</label><input type="text" name="buttonAtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonAdata" class="form-control">
				</td>
			</tr>
			<tr>
				<td>B</td>
				<td>
					<input type="radio" name="buttonBactionType" value="message">文字
					<input type="radio" name="buttonBactionType" value="uri">連結
					<br>
					<label>標題</label><input type="text" name="buttonBtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonBdata" class="form-control">
				</td>
			</tr>
			<tr>
				<td>C</td>
				<td>
					<input type="radio" name="buttonCactionType" value="message">文字
					<input type="radio" name="buttonCactionType" value="uri">連結
					<br>
					<label>標題</label><input type="text" name="buttonCtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonCdata" class="form-control">
				</td>
			</tr>
			<tr>
				<td>D</td>
				<td>
					<input type="radio" name="buttonDactionType" value="message">文字
					<input type="radio" name="buttonDactionType" value="uri">連結
					<br>
					<label>標題</label><input type="text" name="buttonDtitle" class="form-control">
					<label>資料</label><input type="text" name="buttonDdata" class="form-control">
				</td>
			</tr>
		</table>	
	</div>
</div>

</div>
<script>
function activateCarouselCol(col) {
    $('.carouselCol').hide();
    $('#carouselCol' + col).show();
}

$('input[name=richMessageType]').on('change', function() {
	targetDiv = $(this).attr('data-target-div');
	$('.richMessageTypeDiv').hide();
	$('#' + targetDiv).show();

	$('#coverflow').coverflow({
		select: function(event, ui){
            col = ui.index + 1;
            activateCarouselCol(col);
        }
	});
});

$('#coverflow img').click(function() {
    if( ! $(this).hasClass('ui-state-active')){
        return;
    }
    
    col = $(this).attr('data-col');
    activateCarouselCol(col);
});


</script>
</body>
</html>