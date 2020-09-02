<%@page 
import="com.intumit.solr.tenant.Tenant"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.robot.dictionary.DictionaryDatabase"
%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"	language="java"%>
<%@ page import="com.intumit.solr.robot.entity.QAEntityType" %>
<%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
		request.getRequestDispatcher("login.jsp").forward(request, response);
		return;
	}
	Tenant t = Tenant.getFromSession(session);
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	boolean isAuditor = true;
	String[] categories = t.getQaCategory().split(",");
	request.setAttribute("userId", user.getId());
	request.setAttribute("isAuditor", isAuditor);
	request.setAttribute("categories", categories);
	request.setAttribute("enableRestrictToKnowledgePoint", t.getEnableRestrictToKnowledgePoint());
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='entity.audit' /></title>
<jsp:include page="header-qa.jsp"></jsp:include>

<script type="text/javascript" src="<%=request.getContextPath()%>/wiseadm/js/datatables/jquery.dataTables.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/wiseadm/js/datatables/dataTables.bootstrap.min.js"></script>

<script type="text/javascript">
$(document).ready(function(){
	var $messageModal= $("#messageModal").hide()
	var $editModal= $("#editModal").hide()
	var $diffModal= $("#diffModal").hide()
	var $logModal= $("#logModal").hide()
	
	var $table = $("#auditTable").DataTable({
		 ajax: {
		      "url": "EntityVersionServlet",
		      "type": "GET",
		      "error": function (e) {
		      },
		      "dataSrc": ""
		 },
		 order: [[ 9, "desc" ]],
		 columns: [
			 	  { data: 'id'},
		          { data: 'category'},
		          { data: 'code'},
		          { data: 'name'},
		          { data: 'entityValues'},
		          { data: 'entityType'},
		          { data: 'enabled',render: function(data) {
		        	  var enabled;
		        	  switch (data) {
		        	  case true:
		        		  enabled = '<bean:message key="global.shi"/>'
		        			  break;
		        	  case false:
		        		  enabled = '<bean:message key="global.no"/>'
		        			  break;
		        	  default:
		        		  break;
		        	  }
		        	  return enabled;
		          }},
		          { data: 'editorName' },
		          { data: 'action',render: function(data) {
		        	var action;
		        	switch (data) {
					case "ADD":
						action = '<font color="#00BB00"><bean:message key="global.add"/>'
						break;
					case "UPDATE":
						action = '<font color="blue"><bean:message key="modify"/>'
							break;
					case "DELETE":
						action = '<font color="red"><bean:message key="delete"/>'
							break;
					default:
						break;
					}
		        	return action;
		          }},
		          { data: 'updateTime'},
		          { data: 'auditorName',render:function(data){
			        	 var auditorName = data;
			        	 if (auditorName) {
			        		 return auditorName;
			        	 }
				        	return '';
			      }},
			      { data: 'passTime',render:function(data){
			        	 var passTime = '';
				        	if (data) {	
				        		passTime = data;
							}
				        	return passTime;
				  }},
		          {data:"status",render:function(data){
		        	 var status;
			        	switch (data) {
						case "AUDIT":
							status = '<bean:message key="global.audit.status.audit"/>'
							break;
						case "REJECT":
							status = '<bean:message key="global.audit.status.reject"/>'
								break;
						case "HISTORY":
							status = '<bean:message key="global.audit.ratify"/>'
								break;
						default:
							status = '';
							break;
						}
			        	return status;
		          	}
		          },
		      ],
		      "aoColumnDefs": [
		    	   {"aTargets": [13],"mData": null,
		    	        "mRender": function (data) {
		    	        	var isNotEditor = true;
		    	        	if (data.updateLog) {
		    	    			var updateLog = JSON.parse(data.updateLog);
		    	    			for (var i = 0; i < updateLog.length; i ++) {
		    	    				if (updateLog[i].editorId == ${userId}) {
		    	    					isNotEditor = false;
		    	    					break;
		    	    				}
		    	    			}
		    	        	} else {
		    	        		isNotEditor = data.editorId != ${userId};
		    	        	}
		    	        	var html ='<div class="row">' 
		    	        	// 審核者
		    	        	if(${isAuditor} && isNotEditor && data.status == "AUDIT"){
		    	        		// 刪除
		    	        		if (data.action == "DELETE") {
		    	        			html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="passBtn" class="btn btn-success"><bean:message key="global.audit.pass"/></button></div>';
						    	    html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="rejectBtn" class="btn btn-danger"><bean:message key="global.audit.reject"/></button></div>';
						    	 // 新增
		    	        		} else if (data.action == "ADD") {		    	        			
					    	        html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="editBtn" class="btn btn-primary"><bean:message key="global.audit.edit"/></button></div>';
		                            html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="passBtn" class="btn btn-success"><bean:message key="global.audit.pass"/></button></div>';
						    	    html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="rejectBtn" class="btn btn-danger"><bean:message key="global.audit.reject"/></button></div>';
						    	    html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="logBtn" class="btn btn-warning"><bean:message key="global.log"/></button></div>';
		    	        		// 修改
		    	        		} else if (data.action == "UPDATE") {			    	        		
					    	        html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="viewBtn" class="btn btn-info"><bean:message key="global.audit.view"/></button></div>';
					    	        html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="editBtn" class="btn btn-primary"><bean:message key="global.audit.edit"/></button></div>';
					    	        html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="passBtn" class="btn btn-success"><bean:message key="global.audit.pass"/></button></div>';
								    html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="rejectBtn" class="btn btn-danger"><bean:message key="global.audit.reject"/></button></div>';
								    html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="logBtn" class="btn btn-warning" ><bean:message key="global.log"/></button></div>';
		    	        		}
		    	        		return html+="</div>"
		    	        	// 申請者
		    	        	}else if (data.status == "AUDIT"){
		    	        		i// 審核中
	    	        			// 新增
	    	        			if (data.action == "ADD") {			    	        			
	    	        				html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="editBtn" class="btn btn-primary"><bean:message key="global.audit.edit"/></button></div>';
	    	        				html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="logBtn" class="btn btn-warning"><bean:message key="global.log"/></button></div>';
	    	        			// 修改
	    	        			} else if (data.action == "UPDATE") {	    	        			
					    	        html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="viewBtn" class="btn btn-info"><bean:message key="global.audit.view"/></button></div>';	
				    	        	html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="editBtn" class="btn btn-primary"><bean:message key="global.audit.edit"/></button></div>';
				    	        	html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button name="logBtn" class="btn btn-warning"><bean:message key="global.log"/></button></div>';
	    	        			// 刪除
	    	        			} else if (data.action == "DELETE") {
	    	        				return "";
	    	        			}
		    	        		return html+="</div>"
		    	        			// 被駁回    
		    	        	} else if (data.status == "REJECT") {
		    	        		html += '<div style="margin-bottom: 2px;margin-left: 10px;"><button id="rejectViewBtn" name="rejectViewBtn" class="btn btn-default"><bean:message key="global.audit.reject.reason"/></button></div>'
		    	        		return html+="</div>";
		    	        	// 核可
		    	        	} else if (data.status == "HISTORY") {
		    	        		return "";
		    	        	}
		    	        	return "";
		    	        }
		    	    }
		     ], createdRow: function (row, data, index) {
		    	 var $row = $(row);
		    	 if(data.status == "REJECT"){
		    		 $row.addClass("danger")
		    	 }
		    	 $row.attr("data-id",data.id)
		     }
	});
	
	$('#auditTable tbody').on('click', 'button[name=logBtn]', function (){
		var data = $table.row($(this.closest("tr"))).data();
		console.log(data);
		var $tbody = $logModal.find("tbody");
		$tbody.html("");
		if (data.updateLog) {
			var updateLog = JSON.parse(data.updateLog);
			for (var i = 0; i < updateLog.length; i ++) {
				$tbody.append('<tr><td style="border:2px #FFFFFF solid;" class="col-md-2">' + updateLog[i].editorName + '</td>'
					+ '<td style="border:2px #FFFFFF solid;" class="col-md-2">' + updateLog[i].updateTime + '</td>');
			}
		} else {
			$tbody.append('<tr><td style="border:2px #FFFFFF solid;" class="col-md-2">' + data.editorName + '</td>'
				+ '<td style="border:2px #FFFFFF solid;" class="col-md-2">' + data.updateTime + '</td>');
		}
		$logModal.modal('show');
	});
		
	$('#auditTable tbody').on('click', 'button[name=passBtn]', function (){
		var id =  $(this).closest("tr").data("id")
		var updateTime = $table.row($(this.closest("tr"))).data().updateTime;
		$.ajax({
		url:"EntityVersionServlet",
		type:"post",
		data:{id:id,action:"pass",updateTime:updateTime},
		timeout: 30000,
		async:true,
		success:function(data){
			alert(data.msg)
			$table.ajax.reload();
		},
		error:function(){
			alert("error!");
		},
		})
	})
	
	$('#auditTable tbody').on('click', 'button[name=rejectBtn]', function (){
		var id =  $table.row($(this.closest("tr"))).data().id
		var updateTime = $table.row($(this.closest("tr"))).data().updateTime;
		
		$.ajax({
			url:"EntityVersionServlet/check",
			type:"post",
			data:{id:id,updateTime:updateTime},
			timeout: 30000,
			success:function(data){
				if(data.msg != ""){
					alert(data.msg)
					$table.ajax.reload();
				} else {
					$messageModal.find("textarea[name=message]")
					$messageModal.find("h2.modal-title").html('<font color="#FF0000">*</font><bean:message key="global.audit.enter.reject.reason"/>')
					$messageModal.find("#recjectSubmit").css("display","block")
					$messageModal.find("textarea[name=message]").attr("disabled",false).val("")
					$messageModal.modal('show')
					$messageModal.find("input[name=rejectId]").val(id)
					$messageModal.find("input[name=rejectUpdateTime]").val(updateTime);
				}
			},
			error:function(){
				alert("error!");
			}
		})
	})
	
	$('#auditTable tbody').on('click', 'button[name=rejectViewBtn]', function (){
		var data = $table.row($(this.closest("tr"))).data();
		$messageModal.find("h2.modal-title").text('<bean:message key="global.audit.status.reject"/>'
				+'<bean:message key="global.audit.reject.reason"/>')
		$messageModal.find("textarea[name=message]").attr("disabled",true).val(data.message)
		if(data.message != null && data.message.length >0){
	         $("#recjectSubmit").hide(); 
	    }
		$messageModal.modal('show')
	});	

	
	$('#messageModal').on('click', 'button[name=recjectSubmit] ', function (){
		var id =  $messageModal.find("input[name=rejectId]").val()
		var updateTime = $messageModal.find("input[name=rejectUpdateTime]").val()
		console.log(id)
		var message = $messageModal.find("textarea[name=message]").val()
		message = message.replace(/\s/g,'');
		if(message != null && message.length >0 && message !="") {
			$.ajax({
			url:"EntityVersionServlet",
			type:"post",
			data:{id:id,action:"reject",updateTime:updateTime,message:message},
			timeout: 30000,
			success:function(data){
				alert(data.msg)
				$messageModal.modal('hide')
				$messageModal.find("input[name=rejectId]").val("")
				$messageModal.find("textarea[name=message]").val("")
				$table.ajax.reload();
			},
			error:function(){
				alert("error!");
			}
			})
		}else{
			alert("請輸入駁回原因!");
		}
	})
	
	$('#auditTable tbody').on('click', 'button[name=cancelBtn]', function (){
		var id =  $(this).closest("tr").data("id")
		$.ajax({
		url:"EntityVersionServlet/delete?action=cancel&id="+id,
		type:"post",
		timeout: 30000,
		success:function(data){
			alert(data.msg)
			$table.ajax.reload();
		},
		error:function(){
			alert("error!");
		}
		})
	});
	
	$('#auditTable tbody').on('click', 'button[name=viewBtn]', function (){
		var newData = $table.row($(this.closest("tr"))).data();
		var id = newData.publicId
		$.ajax({
		url:"entity?id="+id,
		type:"get",
		dataType:"json",
		timeout: 30000,
		success:function(oldData){
			// 分類
			var oCategory = oldData.category;
			var nCategory = newData.category;
			if (nCategory != oCategory) {
				nCategory = '<font color="#FF0000">'+nCategory+'</font>';
			}
			$("#oldCategory").text(oCategory)
			$("#newCategory").html(nCategory)
			
			// 代號
			var oCode = oldData.code;
			var nCode = newData.code;
			if (nCode != oCode) {
				nCode = '<font color="#FF0000">'+nCode+'</font>';
			}
			$("#oldCode").text(oCode)
			$("#newCode").html(nCode)
			
			// 名稱
			var oName = oldData.name;
			var nName = newData.name;
			if (nName != oName) {
				nName = '<font color="#FF0000">'+nName+'</font>';
			}
			$("#oldName").text(oName)
			$("#newName").html(nName)
			
			// 子實體
			var oSubEntities = oldData.subEntities;
			var nSubEntities = newData.subEntities;
			if (nSubEntities != oSubEntities) {
				nSubEntities = '<font color="#FF0000">'+nSubEntities+'</font>';
			}
			$("#oldSubEntities").text(oSubEntities)
			$("#newSubEntities").html(nSubEntities)
			
			// 值
			var oEntitySplit = oldData.entityValues.split("|");
			var nEntitySplit = newData.entityValues.split("|");
			var obufferEntity = new StringBuffer();
			var nbufferEntity = new StringBuffer();
			
			for (i = 0; i < oEntitySplit.length; i++) {
				if(nEntitySplit.indexOf(oEntitySplit[i]) != -1){
					obufferEntity.append(oEntitySplit[i] + ",");
				}else{
					obufferEntity.append('<font color="#FF0000">' + oEntitySplit[i] + '</font>' + ",");
				}
			}
			
			for (i = 0; i < nEntitySplit.length; i++) {
				if(oEntitySplit.indexOf(nEntitySplit[i]) != -1){
					nbufferEntity.append(nEntitySplit[i] + ",");
				}else{
					nbufferEntity.append('<font color="#FF0000">' + nEntitySplit[i] + '</font>' + ",");
				}
			}
			
			var oldBufferEntity = obufferEntity.toString().substring(0,obufferEntity.toString().length-1)
			var newBufferEntity = nbufferEntity.toString().substring(0,nbufferEntity.toString().length-1)
			$("#oldEntityValues").html(oldBufferEntity)
			$("#newEntityValues").html(newBufferEntity)
			
			// 實體類型
			var oEntityType = oldData.entityType;
			var nEntityType = newData.entityType;
			if (nEntityType != oEntityType) {
				nEntityType = '<font color="#FF0000">'+nEntityType+'</font>';
			}
			$("#oldEntityType").text(oEntityType)
			$("#newEntityType").html(nEntityType)
			
			// 實體值來自索引
			var oFromIndex = oldData.fromIndex == true ? '<bean:message key="global.yes"/>' : '<bean:message key="global.no"/>';
			var nFromIndex = newData.fromIndex == true ? '<bean:message key="global.yes"/>' : '<bean:message key="global.no"/>';
			if (nFromIndex != oFromIndex) {
				nFromIndex = '<font color="#FF0000">'+nFromIndex+'</font>';
			}
			$("#oldFromIndex").text(oFromIndex)
			$("#newFromIndex").html(nFromIndex)
			
			// 啟用
			var oEnabled = oldData.enabled == true ? '<bean:message key="global.yes"/>' : '<bean:message key="global.no"/>';
			var nEnabled = newData.enabled == true ? '<bean:message key="global.yes"/>' : '<bean:message key="global.no"/>';
			if (nEnabled != oEnabled) {
				nEnabled = '<font color="#FF0000">'+nEnabled+'</font>';
			}
			$("#oldEnabled").text(oEnabled)
			$("#newEnabled").html(nEnabled)
			$diffModal.modal("show") 
		},
		error:function(){
			alert("error!");
		}
		})
	});
	
	$('#auditTable tbody').on('click', 'button[name=editBtn]', function (){
		var data = $table.row($(this.closest("tr"))).data();
		var id =  data.id
		var updateTime = data.updateTime;
		
		$("#editModal").find("input[name=id]").val(data.id)
		$("#editModal").find("input[name=category]").val(data.category)
		$("#editModal").find("input[name=code]").val(data.code)
		$("#editModal").find("input[name=name]").val(data.name)
		$("#editModal").find("input[name=subEntities]").val(data.subEntities)
		$("#editModal").find("textarea[name=entityValues]").val(data.entityValues)
		if(data.entityType){
			$("#editModal").find("input[name=entityType]").filter('[value=1]').prop('checked', true)
		}else{
			$("#editModal").find("input[name=entityType]").filter('[value=0]').prop('checked', true)
		}
		if(data.fromIndex){
			$("#editModal").find("input[name=fromIndex]").filter('[value=1]').prop('checked', true)
		}else{
			$("#editModal").find("input[name=fromIndex]").filter('[value=0]').prop('checked', true)
		}
		if(data.enabled){
			$("#editModal").find("input[name=enabled]").filter('[value=1]').prop('checked', true)
		}else{
			$("#editModal").find("input[name=enabled]").filter('[value=0]').prop('checked', true)
		}
		
		$.ajax({
			url:"EntityVersionServlet/check",
			type:"post",
			data:{id:id,updateTime:updateTime},
			timeout: 30000,
			success:function(data){
				if(data.msg != ""){
					alert(data.msg)
					$table.ajax.reload();
				} else {
					$editModal.modal("show"); 
				}
			},
			error:function(){
				alert("error!");
			}
		})
	});
	
	$("#processBtn").click(function(){
		var id = $("input[name=hiddenId]").val();
		$("input[name=action]").val("update")
		var form  = $("#entityForm");
		if (form.find("[name=category]").val().trim() != "" && form.find("[name=code]").val().trim() != ""
			&& form.find("[name=name]").val().trim() != "" && form.find("[name=entityValues]").val().trim() != "") {
			$.ajax({
				url:"EntityVersionServlet",
				type: "post",
				data:form.serialize(),
				success:function (data){
					alert(data.msg)
					if(data.reset){
						$editModal.modal('hide')
						$table.ajax.reload();
					}
				},error:function(){
					alert("error!");
				}
			})
		} else {
			alert("<bean:message key='global.explanation.essential.ent.ex'/>");
		}
	});
	
	/*類 StringBuffer */
    function StringBuffer() {
        this.__strings__ = new Array;
    }
     
    StringBuffer.prototype.append = function (str) {
        this.__strings__.push(str);
    };
     
    StringBuffer.prototype.toString = function () {
        return this.__strings__.join("");
    };
})
</script>
</head>
<body>
	<jsp:include page="navbar-qa.jsp"></jsp:include>
	<div class="container" style="margin-top:2%;width:1500px">
			<div class="row">
				<table id="auditTable" class="table">
					<thead>
						<tr>
						    <th>#<bean:message key="global.id" /></th> 
							<th><bean:message key="global.category" /></th>
							<th><bean:message key="global.code" /></th>
							<th><bean:message key="global.name" /></th>
							<th><bean:message key="global.values" /></th>
							<th><bean:message key="global.entityType" /></th>
							<th><bean:message key="is.enabled" /></th>
							<th><bean:message key="global.audit.proceer" /></th>
							<th><bean:message key="global.audit.reason" /></th>
							<th><bean:message key="global.audit.process.time" /></th>
							<th><bean:message key="global.audit.personnel" /></th>	
							<th><bean:message key="global.audit.time" /></th>
							<th><bean:message key="global.audit.status" /></th>
							<th><bean:message key="operation" /></th>
						</tr>
					</thead>
				</table>
			</div>
	</div>
	
	<div class="modal fade" id="logModal" tabindex="-1" role="dialog" aria-hidden="true">
	  <div class="modal-dialog" role="document">
	    <div class="modal-content">
	      <div class="modal-body">
	        <table class="table table-bordered">
	          <colgroup>
	            <col style="color: #FFFFFF; background-color:#D2E9FF;">
	            <col style="color: #FFFFFF; background-color:#D2E9FF;">
	          </colgroup>
	          <thead>
	            <tr>
	              <th style="color: #FFFFFF; border:2px #FFFFFF solid; background-color:#2828FF;" align='center' valign="middle" class="col-md-4">
	                <bean:message key="global.audit.proceer" />
	              </th>
	              <th style="color: #FFFFFF; border:2px #FFFFFF solid; background-color:#2828FF;" align='center' valign="middle" class="col-md-4">
	                <bean:message key="global.audit.process.time" />
	              </th>
	            </tr>
	          </thead>
	          <tbody>
	          </tbody>
	        </table>
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-secondary" data-dismiss="modal"><bean:message key="global.close"/></button>
	      </div>
	    </div>
	  </div>
	</div>
	
	<!-- 駁回原因填寫視窗 -->
	<div class="modal fade" id="messageModal" tabindex="-1" role="dialog" aria-hidden="true">
	  <div class="modal-dialog" role="document">
	    <div class="modal-content">
	      <div class="modal-header">
	        <h2 class="modal-title"><font color="#FF0000">*</font><bean:message key="global.audit.enter.reject.reason"/></h2>
	      </div>
	      <div class="modal-body">
	      	<input name="rejectId" type="hidden">
	      	<input name="rejectUpdateTime" type="hidden">
	      	<textarea name="message" class="form-control" style="width: 100%"></textarea>
	      </div>
	      <div class="modal-footer">
	        <button id="recjectSubmit" name="recjectSubmit" type="button" class="btn btn-primary" style="display:block; float:left" ><bean:message key="global.submit"/></button>
	        <button type="button" class="btn btn-secondary" data-dismiss="modal"  style="float:right" ><bean:message key="global.close"/></button>
	      </div>
	    </div>
	  </div>
	</div>
	
	<!-- 差異視窗 -->
	<div class="modal fade" id="diffModal" tabindex="-1" role="dialog" aria-hidden="true">
	  <div class="modal-dialog" role="document">
	    <div class="modal-content">
	      <div class="modal-body">
      			<table class="table table-bordered">
      				<colgroup>
                    	<col style="background-color:#2828FF;">
						<col style="background-color:#D2E9FF;">
						<col style="background-color:#D2E9FF;">
                   	</colgroup>
                   	<tr>
						<th style="border:2px #FFFFFF solid;" class="col-md-3"></th>
						<th style="border:2px #FFFFFF solid; background-color:#2828FF;" align='center' valign="middle" class="col-md-4">
							<font color="#FFFFFF"><bean:message key='global.view.before'/></font>
						</th>
						<th style="border:2px #FFFFFF solid; background-color:#2828FF;" align='center' valign="middle" class="col-md-4">
							<font color="#FFFFFF"><bean:message key='global.view.after'/></font>
						</th>
					</tr>
					<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='global.category'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldCategory"></td>
                        <td style="border:2px #FFFFFF solid;" id="newCategory"></td>	      				
	      			</tr>
	      			<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='global.code'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldCode"></td>
                        <td style="border:2px #FFFFFF solid;" id="newCode"></td>	      				
	      			</tr>
					<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='global.name'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldName"></td>
                        <td style="border:2px #FFFFFF solid;" id="newName"></td>	      				
	      			</tr>
	      			<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='global.subEntities'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldSubEntities"></td>
                        <td style="border:2px #FFFFFF solid;" id="newSubEntities"></td>	      				
	      			</tr>
	      			<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='global.values'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldEntityValues"></td>
                        <td style="border:2px #FFFFFF solid;" id="newEntityValues"></td>	      				
	      			</tr>
	      			<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='global.entityType'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldEntityType"></td>
                        <td style="border:2px #FFFFFF solid;" id="newEntityType"></td>	      				
	      			</tr>
	      			<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='entity.fromIndex'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldFromIndex"></td>
                        <td style="border:2px #FFFFFF solid;" id="newFromIndex"></td>	      				
	      			</tr>
	      			<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF"><bean:message key='is.enabled'/></font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldEnabled"></td>
                        <td style="border:2px #FFFFFF solid;" id="newEnabled"></td>	      				
	      			</tr>
      			</table>
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-secondary" data-dismiss="modal"><bean:message key="global.close"/></button>
	      </div>
	    </div>
	  </div>
	</div>
	
	<!-- 編輯視窗 -->
	<div class="modal fade" id="editModal" tabindex="-1" role="dialog" aria-hidden="true">
	  <div class="modal-dialog" role="document">
	    <div class="modal-content">
	      <div class="modal-header">
	        <h2 class="modal-title"><bean:message key="global.edit"/></h2>
	      </div>
	      <div class="modal-body">
	      	<form id="entityForm" class="form">
					<input type="hidden" name="action" >
					<input type="hidden" name="id">
					<div class="form-group row">
						<div class="col-sm-2">
							<bean:message key='global.category'/>：
						</div>
						<div class="col-sm-6">
							<input type="text" class="form-control" id="category" name="category" size="30">
						</div>
					</div>
					<div class="form-group row">
						<div class="col-sm-2">
							<bean:message key='global.code'/>：
						</div>
						<div class="col-sm-6">
							<input type="text" class="form-control" id="code" name="code" size="30">
						</div>
					</div>
					<div class="form-group row">
						<div class="col-sm-2">
							<bean:message key='global.name'/>：
						</div>
						<div class="col-sm-6">
							<input type="text" class="form-control" id="name" name="name" size="30">
						</div>
					</div>
					<div class="form-group row">
						<div class="col-sm-2">
							<bean:message key='global.subEntities'/>：
						</div>
						<div class="col-sm-6">
							<input type="text" class="form-control" id="subEntities" name="subEntities" size="30">
						</div>
					</div>												
					<hr>
					<div class="form-group row">
						<div class="col-sm-1">
							<bean:message key='global.values'/>：
						</div>
						<div class="col-sm-6">
							<textarea  class="form-control" id="entityValues" id="entityValues" name="entityValues" cols="50" rows="5"></textarea> 
							<br>(<bean:message key='intent.keywords.ex'/>)
						</div>
					</div>
					<hr>
					<div class="form-group row">
						<div class="col-sm-2">
							<bean:message key="global.entityType"/>:
						</div>
						<div class="col-sm-7">
						<% for (QAEntityType et: QAEntityType.values()) { %>
						<%= et.name() %><input type="radio" name="entityType" value="<%= et.name() %>" <%= (QAEntityType.STRING == et) ? "checked" : "" %>> &nbsp;
						<% } %>
						</div>
					</div>
					<div class="form-group row">
						<div class="col-sm-2">
							<bean:message key="entity.fromIndex"/>:
						</div>
						<div class="col-sm-7">
							<bean:message key='global.yes'/><input type="radio" name="fromIndex" value="1">
							<bean:message key='global.no'/><input type="radio" name="fromIndex" value="0" checked>
							&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href="qaCustomDataInspect.jsp" target="_new">Inspect Index</a>
						</div>
					</div>
					<hr>
					<div class="form-group row">
						<div class="col-sm-2">
							<bean:message key='is.enabled'/>
						</div>
						<div class="col-sm-2">
							<bean:message key='global.yes'/><input type="radio" class="form-check-input" name="enabled" value="1" checked>
							<bean:message key='global.no'/><input type="radio" class="form-check-input" name="enabled" value="0">	
						</div>
					</div>
				</form>
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-secondary" data-dismiss="modal"><bean:message key="global.close"/></button>
   			<button id="processBtn" class="btn btn-success" type="button">
				<bean:message key='global.audit.process' />
			</button>
	      </div>
	    </div>
	  </div>
	</div>
	
</body>
</html>
