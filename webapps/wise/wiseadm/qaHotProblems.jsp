<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="org.apache.wink.json4j.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="com.intumit.solr.robot.*" %>
<%@ page import="com.intumit.solr.tenant.*" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="com.intumit.solr.dataset.*" %>
<%@ page import="com.intumit.solr.dataset.DataSet.*" %>

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
<%
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

	t = Tenant.get(t.getId());
	JSONArray faqJsonArray = null;
	Map<String, Map<String, Object>> old_faqData = new HashMap<String, Map<String, Object>>();
	
	try {
		faqJsonArray = new JSONArray(t.getFaqJson());
		for (Object tmp : faqJsonArray) {
			Map<String, Object> data = (Map<String, Object>)tmp;
			Map<String, Object> tmpData = new HashMap<String, Object>();
			tmpData.put("questions", data.get("questions"));
			tmpData.put("modifyTime", data.get("modifyTime"));
			tmpData.put("modifyUser", data.get("modifyUser"));
			old_faqData.put((String) data.get("channel") ,tmpData);
		}
		System.out.println(old_faqData.toString());
	}
	catch (Exception ex) {
		System.out.println(ex.toString());
	}
	
	int targetDsId = 2;
	DataSet ds = DataSetFacade.getInstance().get(targetDsId);
	{
		// 為了分析 core-log 又不新增 ds 而做
		ds.setFacets("QuestionType_s,QuestionCategory_ms,AnswerType_s,KnowledgePoint_ms,QuestionKeyword_ms,SpecialMark_s,Identity_s,Eservice_s,QuestionTag_ms,UserType_s");
		ds.setCoreName("core-log");
		ds.setBodyFields("Question_mt,Answer_mt");
		ds.setFieldWeight("Question_mt^5 Answer_mt^0.01");
		ds.setFieldHighlight("Question_mt,Answer_mt");
		//ds.setFilters("TenantId_i:" + t.getId());
	}
	session.setAttribute("d", "" + targetDsId);
	session.setAttribute("logDs", ds);
%>
<!DOCTYPE HTML>
<html>
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<title><bean:message key='global.hot.problems'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
    
<style>
.loading-panel {
	width:100%; height:100%; display: none;
	position: fixed; top:0; left:0; z-index:9999;
	background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
</style>
</head>
<body>
<input type="hidden" id="testChannelHotResult">
<input type="hidden" id="testChannelHotCount">
<div class='loading-panel'></div>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<form id="hotProblemsForm" method="post">
	<div class="form-group">
	<div class="col-md-9">
	<table class="table table-striped table-bordered table-hover">
		<thead>
			<tr>				
				<th class="col-lg-2"><bean:message key="hot.problems.channel"/></th>			
				<th class="col-lg-7"><bean:message key="hot.problems.faq"/></th>
				<!--<th class="col-lg-3">推薦問題</th>-->
				<th class="col-lg-2"><bean:message key="hot.problems.status"/></th>
				<th class="col-lg-1"><bean:message key="hot.problems.operate"/></th>
			</tr>
		</thead>
		<% 
		List<QAChannel> channels = QAChannel.list(t.getId());
		for(QAChannel channel : channels){
			Map<String, Object> oldData = old_faqData.get(channel.getCode());
			if (oldData==null)
				oldData = new HashMap<String, Object>();
			String oldHot = old_faqData.get(channel.getCode()) != null ?StringUtils.join( (List<String>) oldData.get("questions"), "\n"):"";
			String channelID = "channel_" + channel.getId();
			String modifyStatusID = "ModifyStatus_" + channel.getId();
			String modifyUser = oldData.get("modifyUser")!= null ? (String) oldData.get("modifyUser"):"";
			String modifyTime = oldData.get("modifyTime")!= null ? (String) oldData.get("modifyTime"):"";
					
		%>
		<tr>
			<td ><%= channel.getName() %></td>
			<td >
				<textarea class="form-control" rows="5" id="<%= channelID %>" name="<%= channelID %>"><%= oldHot %></textarea>
			</td>
			<!--
			<td >
			</td>
			-->
			<td id="<%= modifyStatusID %>">
				<%=modifyUser %>
				</br>
				<%=modifyTime %>
			</td>
			<td >
				<input id="save" class="btn btn-danger" type="button" value="<bean:message key="hot.problems.save"/>" 
				onclick="saveChannelHot('<%= channel.getCode() %>', '<%= channelID %>', '<%= modifyStatusID %>')">
				<input id="edit" class="btn btn-info" type="button" value="<bean:message key="hot.problems.test"/>" 
				onclick="testChannelHot('<%= channel.getCode() %>', '<%= channelID %>')">
				<input id="import" class="btn btn-success" type="button" value="<bean:message key="hot.problems.import"/>" 
				onclick="importChannelHot('<%= channel.getCode() %>', '<%= channelID %>')">
			</td>
		</tr>
		<%
		}
		%>
	</table>
	</div>
	</div>
	<!-- <div class="form-group">
		<div class="col-sm-5">
			<button id="save" type="button" class="btn btn-danger"><bean:message key='submit'/></button>
		</div>
	</div>
	 -->
</form>
<script>

function saveChannelHot(channel, channelID, modifyStatusID){
	$.ajax({
  		type: 'POST',
  		url: 'qaChannelHotSave.jsp?action=update',
  		data: {'channel':channel,
  			   'hot':$('#'+channelID).val(),
  		},
  		success: function(data) {
  			var result = JSON.parse(data);
  			alert(result.status);
  			if (result.status == "success") {
  				$('#'+modifyStatusID).html(result.modifyUser +'</br>'+result.modifyTime);
  			}
  		}
  	});
}

function testChannelHot(channelCode, channelID){
	var strList = $('#'+channelID).val().split("\n");
	$('.loading-panel').show();
	for (i = 0; i < strList.length; i++) {
		//alert(strList[i]);
		document.getElementById("testChannelHotCount").value = 0;
		document.getElementById("testChannelHotResult").value = "";
		$.ajax({
	  		type: 'GET',
	  		url: '../qa-ajax.jsp',
	  		data: {
	  			'q':strList[i],
	  			'totalSize':strList.length,
	  			testMode: true,
	  		  	tid: <%= t.getId() %>,
	    		html: true,
	    		ftc: false,
	    		ch: channelCode,
	  		},
	  		dataType: "json",
	  		success: function(data) {
	  			var count = document.getElementById("testChannelHotCount").value;
	  			//alert('count : ' + document.getElementById("testChannelHotCount").value);
	  			document.getElementById("testChannelHotCount").value = parseInt(count)+1;
	  			if (data.answerType=="NO_ANSWER"){
	  				document.getElementById("testChannelHotResult").value += data.bundle[0].q+"\n";
	  				//alert("無答案 : " + data.bundle[0].q);
	  			}
	  			if(document.getElementById("testChannelHotCount").value==data.bundle[0].totalSize) {
	  				$('.loading-panel').hide();
	  				alert('<bean:message key="hot.problems.test.finish"/>\n<bean:message key="hot.problems.noAnswerList"/> :\n\n' + document.getElementById("testChannelHotResult").value);
	  			}
	  		}
	  	});
	}
}

function importChannelHot(channel, channelID){
	<%
	String query = "q=*:*";
	query += "&fq=(Date_dt:[NOW/DAY+1DAY-1MONTH TO NOW/DAY+1DAY])";
	query += "&sort=Date_dt desc";
	%>
	var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/qa-analytics/adv-stat-ajax.jsp?";
	baseDataUrl += "<%=URLEncoder.encode(query, "UTF-8")%>";
	baseDataUrl += "&fq=Eservice_s:\"" + encodeURI(channel) +"\"";
	baseDataUrl += "&start=0";
	baseDataUrl += "&rows=25";
	baseDataUrl += "&ts=<%= System.currentTimeMillis() %>";
	
	//alert("baseDataUrl : " + baseDataUrl);
	//baseDataUrl = encodeURI(baseDataUrl);
	//alert("encodeURI baseDataUrl : " + baseDataUrl);
	
	var dataField = "matchedQuestion";
	var dataFieldFilter = "";
	var dataNum = 10;
	var groupBy = null;
	
	$('.loading-panel').show();
	
	$.ajax({
		url: baseDataUrl,
		data: {
			df: dataField,
			dff: dataFieldFilter,
			num: dataNum,
			groupBy: groupBy,
		},
		dataType: 'json',
		success: function(result) {
			if (result.status){
				alert("denied");
			} else {
				var series = result.series;
				//coption.yAxis_fq =  result.yAxis_fq.reverse();
				
				//alert(JSON.stringify(result, null, 4));
				$('#'+channelID).val('');
				if (series.length==0)
					alert('<bean:message key="hot.problems.no.data"/>');
				
				for (var i=0; i < series.length; i++) {
					var s = series[i];
					var tmp = $('#'+channelID).val();
					$('#'+channelID).val(tmp + s.name + '\n');
				}
			}
			$('.loading-panel').hide();
		}
	});
}
</script>
</body> 
</html>
