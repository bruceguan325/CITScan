<%@page import="com.intumit.message.MessageUtil"%>
<%@page import="java.util.Locale"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="com.intumit.solr.admin.*" 
	import="com.intumit.quartz.*" 
	import="com.intumit.solr.SearchManager" 
	import="java.util.ArrayList" 
	import="java.util.List"
	import="org.apache.commons.lang.StringUtils"
%>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E3) == 0) { 
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
<title><bean:message key='global.scheduling'/></title>
<jsp:include page="header-qa.jsp"></jsp:include>
    

<link rel="stylesheet" type="text/css" media="screen"
	href="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/themes/redmond/jquery-ui-1.7.1.custom.css" />
<link rel="stylesheet" type="text/css" media="screen"
	href="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/themes/ui.jqgrid.css" />
<link rel="stylesheet" type="text/css" media="screen"
	href="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/themes/ui.datepicker.css" />
<style>
.my-caption {
	color: #ffffff;
	font-weight: bold;
	font-size: 2em;
}
.ui-jqgrid tr.jqgrow td {
        white-space: normal !important;
    }
</style>
<script
	src="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/js/jquery.js"
	type="text/javascript"></script>

<script
	src="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/js/jquery-ui-1.7.2.custom.min.js"
	type="text/javascript"></script>
<script
	src="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/js/timepicker.js"
	type="text/javascript"></script>

<script
	src="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/js/jquery.layout.js"
	type="text/javascript"></script>
<script
	src="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/js/i18n/grid.locale-en.js"
	type="text/javascript"></script>

<script
	src="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/js/jquery.jqGrid.min.js"
	type="text/javascript"></script>
<script
	src="<%=request.getContextPath()%>/script/jquery-ui-1.7.3/js/jquery.tablednd.js"
	type="text/javascript"></script>
<script type="text/javascript">
var gridimgpath = 'themes/redmond/images';
var date_picker_cfg={
	dateFormat:"yy-mm-dd",
	duration: '',
    showTime: true,
    constrainInput: false,
    stepminutess: 1,
    stephourss: 1,
    altTimeField: '',
    time24h: true
};


var lastsel2;
var mydata2 = [{"id":"49830022-6dcf-4639-a0b9-51d288248f92","core":"","daysOfMonth":"?","daysOfWeek":"2,3,4,5,6","endTime":"",
		"executeStatus":"","expression":"1 15 10 ? * MON-FRI","hours":"10","type":"",
		"previousFireTime":"","minutes":"15","months":"*","name":"led","scheduleStatus":"","seconds":"1",
		"startTime":"","years":"*"}, 
                {id:"34243",name:"Laptop",core:1,note:"Long text ",type:"Delta Import",startTime:"2007-12-04 12:00",endTime:"2007-12-04 18:00",previousFireTime:"2007-12-04 16:00"}, 
                {id:"342342543",name:"LCD Monitor",core:1,note:"note3",type:"Delta Import",scheduleStatus:'<bean:message key="scheduling.process"/>',executeStatus:'<bean:message key='global.execution'/>',startTime:"2007-12-05"}
                 ]; 
jQuery(document).ready(function(){
	jQuery("#grid").jqGrid({
		//datatype: "local",
		datatype: "json",
		url:"./schedule/search?gn=<%= com.intumit.solr.servlet.ScheduleServlet.NONINDEX_GROUP_NAME %>",
		height: 250,
		width: 1000,
		autowidth:true,
		colNames:['<bean:message key="global.id"/>','<bean:message key="job.group"/>','<bean:message key="global.name"/>','<bean:message key="scheduling.form"/>','<bean:message key="parameter.one"/>','<bean:message key="parameter.two"/>','<bean:message key="scheduling.grammer"/>', '<bean:message key="global.year"/>','<bean:message key="global.week"/>','<bean:message key="global.month"/>' ,
		          '<bean:message key="global.day"/>','<bean:message key="global.hour"/>','<bean:message key="global.mintues"/>','<bean:message key="global.seconds"/>','<bean:message key="last.time.execution"/>','<bean:message key="next.time.execution"/>',
		  		//'最後一次執行',
		        '<bean:message key="immediate.execution"/>','<bean:message key="execution.state"/>',
		  		//'備註',
		  		'<bean:message key="global.scheduling"/>','<bean:message key="scheduling.state"/>'
		  		//,'排程管理'
		  		], 
		colModel:[ 
				{name:'id',index:'id', editable: false, hidden:true},
				{name:'gn',index:'gn', editable: true, hidden:true, edittype:"select",editoptions: {value:"<%= com.intumit.quartz.NonIndexJob.class.getName() %>:DEFAULT"}},
		   		{name:'description',index:'description', width:120,editable: true,editoptions:{size:"20",maxlength:"30"}},
		   		 //{name:'s_type',index:'s_type', width:90, editable: true,edittype:"checkbox",editoptions: {value:"Yes:No"}},
		   		{name:'type',index:'type', width:90,formatter:'select', editable: true,edittype:"select",editoptions: {value:"<%= CalculateHourlySearchKeywordLogJob.class.getName() %>:Keyword Statistics (Hourly);<%= PurgeSearchKeywordLogJob.class.getName() %>:Purge Search Log;<%= BuildQAAltJob.class.getName() %>:Build QA Alt"}},
		   		{name:'jobDataMap.p1',index:'p1', width:90,editable: true,editoptions:{size:"20",maxlength:"300"}},
		   		{name:'jobDataMap.p2',index:'p2', width:90,editable: true,editoptions:{size:"20",maxlength:"300"}},
		   		{name:'expression',index:'expression', width:90,editable: true,editoptions:{size:"20",maxlength:"30"}},
		   		{name:'years',index:'years', width:60, editable: false,edittype:"select",editoptions:{value:"*:*;SUN:SUN;MON:MON;TUE:TUE;WED:WED;THU:THU;FRI:FRI;SAT:SAT"}},
		   		 {name:'daysOfWeek',index:'daysOfWeek', width:60, editable: false,edittype:"select",editoptions:{value:"*:*;SUN:SUN;MON:MON;TUE:TUE;WED:WED;THU:THU;FRI:FRI;SAT:SAT"}},
		   		{name:'months',index:'months', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		//{name:'months_interval',index:'months_interval', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		{name:'daysOfMonth',index:'daysOfMonth', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		//{name:'daysOfMonth_interval',index:'daysOfMonth_interval', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		{name:'hours',index:'hours', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		//{name:'hours_interval',index:'hours_interval', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		{name:'minutes',index:'minutes', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		//{name:'minutes_interval',index:'minutes_interval', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		{name:'seconds',index:'seconds', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		//{name:'seconds_interval',index:'seconds_interval', width:50, editable: false,edittype:"select",editoptions:{value:"*:*;0:0;1:1;2:2;3:3;4:4;5:5;6:6;7:7;8:8;9:9"}},
		   		{name:'previousFireTime',index:'previousFireTime',width:150, sorttype:"date"},
		   		{name:'nextFireTime',index:'nextFireTime',width:150, sorttype:"date"},
		   		//{name:'finalFireTime',index:'finalFireTime', sorttype:"date"},
		   		{name:'e_act',index:'e_act', width:110 },
		   		{name:'executeStatus',index:'executeStatus', width:60},
		   		 //{name:'note',index:'note', width:150,hidden:true, sortable:false,editable: true,edittype:"textarea", editoptions:{rows:"2",cols:"10"}} ,
		   		 {name:'s_act',index:'s_act', width:110 }, 
		   		{name:'scheduleStatus',index:'scheduleStatus', width:60} 
		   		 
		   		 //,  
		   		 //{name:'act',index:'act', width:200 } 
		   		 ],
		//rowNum:10,
	   	//rowList:[10,20,30],
	   	jsonReader: { repeatitems : false, id: "0" }, 
	   	imgpath: gridimgpath,
	   	pager: jQuery('#pager'),
	   	sortname: 'startTime',
	    viewrecords: true,
	    sortorder: "desc",
	    caption:"<span class='my-caption'><bean:message key='job.schedule.others.manage'/></span>", 
	    
	    //ondblClickRow: editGridRow,
	    gridComplete: function(){
			var ids = jQuery("#grid").getDataIDs();
			
			for(var i=0;i<ids.length;i++){
				var cl = ids[i];

				var e_start = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"schedule_('trigger','"+cl+"');\" title='Start'>"+
				"<span class='ui-icon ui-icon-play'></span>&nbsp</a>";
				var e_stop = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"schedule_('interrupt','"+cl+"');\" title='Stop'>"+
				"<span class='ui-icon ui-icon-stop'></span>&nbsp</a>";

				var s_start = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"schedule_('resume','"+cl+"');\" title='Resume'>"+
				"<span class='ui-icon ui-icon-clock'></span>&nbsp</a>";
				var s_stop = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"schedule_('pause','"+cl+"');\" title='Pause'>"+
				"<span class='ui-icon ui-icon-pause'></span>&nbsp</a>";
				
				//var be = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"editGridRow('"+cl+"');\" title='Edit row'>"+
				//"<span class='ui-icon ui-icon-pencil'></span>&nbsp</a>";
				//var se = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"jQuery('#grid').saveRow('"+cl+"');\" title='Save row'>"+
				//"<span class='ui-icon ui-icon-disk'></span>&nbsp</a>"; 
				//var ce = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"jQuery('#grid').restoreRow('"+cl+"');\" title='Cancel edit'>"+
				//"<span class='ui-icon ui-icon-arrowreturn-1-w'></span>&nbsp</a>";

				//var cname=jQuery('#grid').getCell(cl,'description');
				//var de = "<a class='fm-button ui-state-default ui-corner-all fm-button-icon-left' href='javascript:void(0)' onclick=\"delGridRow('"+cl+"','"+cname+"');\" title='Delete row'>"+
				//"<span class='ui-icon ui-icon-trash'></span>&nbsp</a>";

				jQuery("#grid").setRowData(ids[i],{
					e_act:e_start+e_stop,
					s_act:s_start+s_stop
					//,act:be+se+ce+de
					});
				
				$(".fm-button").hover(
				   function(){$(this).addClass('ui-state-hover');}, 
				   function(){$(this).removeClass('ui-state-hover');}
				);


			}
			
			
			
		},
		editurl: "./schedule?gn=<%= com.intumit.solr.servlet.ScheduleServlet.NONINDEX_GROUP_NAME %>"
	}).navGrid('#pagernav', 
			{
				edit:true,
				del:true,
				search:false
			}, 
			//options 
			{height:250}, 
			// edit options 
			{
				afterShowForm:function(){
					jQuery("#startTime,#endTime").datepicker(date_picker_cfg);
				},
				onclickSubmit:function() {  jQuery("#startTime,#endTime").empty(); }
				,top:100
			},
			 // add options 
			 {top:100
				 }, 
			 // del options 
			 {} 
			 // search options 
			 );
	
	
});
function pickdates(id){ 
	jQuery("#"+id+"_startTime"+",#"+id+"_endTime","#grid").datepicker(date_picker_cfg);
} 

function schedule_(oper_s,id_s){ 
	if(id_s){
		jQuery.ajax({
			url: "./schedule",
			type: "POST",
			data:{oper:oper_s,id:id_s,gn:'<%= com.intumit.solr.servlet.ScheduleServlet.NONINDEX_GROUP_NAME %>'},
			complete:function(data,Status){
				if(Status == "success") {
					jQuery('#grid').trigger("reloadGrid");
				}
			},
			error:function(xhr,st,err){
				
			}
		});
    }
}


function editGridRow(id){ 
	if(id){
    	jQuery('#grid').restoreRow(lastsel2); 
    	jQuery('#grid').editRow(id,true,pickdates); 
    	lastsel2=id; 
    }
}
function delGridRow(id,name){ 
	if(id){
			jQuery("#grid").delGridRow(id,{
				delData:{name:name,gn:'<%= com.intumit.solr.servlet.ScheduleServlet.NONINDEX_GROUP_NAME %>'}});
		
    }
}

 
</script>

</head>
<body>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<div class="container">
<table id="grid" class="scroll" cellpadding="0" cellspacing="0"></table>
<div id="pager" class="scroll" style="text-align: center; white-space: nowrap"></div>
</div>
</body> 
</html>
