<%@page 
import="com.intumit.solr.tenant.Tenant"
import="com.intumit.solr.admin.*"
import="com.intumit.solr.robot.RobotFormalAnswers"
%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8"	language="java"%>
<%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) {
		request.getRequestDispatcher("login.jsp").forward(request, response);
		return;
	}
	Tenant t = Tenant.getFromSession(session);
	AdminUser user = AdminUserFacade.getInstance().getFromSession(session);
	boolean isAuditor = true;
	String answersSeparator = RobotFormalAnswers.SEPARATOR;
	String[] categories = t.getQaCategory().split(",");
	request.setAttribute("userId", user.getId());
	request.setAttribute("isAuditor", isAuditor);
	request.setAttribute("answersSeparator", answersSeparator);
%>
<!DOCTYPE HTML>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta charset="utf-8">
<title><bean:message key='formal.audit' /></title>
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
		      "url": "RobotFormalAnswersVersionServlet",
		      "type": "GET",
		      "error": function (e) {
		      },
		      "dataSrc": ""
		 },
		 order: [[ 5, "desc" ]],
		 columns: [
				  { data: 'id'},
		          { data: 'keyName'},
		          { data: 'answers',render: function(data) {
		        	  var answers = data;
		        	  return $("<div/>").text(answers).html().split("${answersSeparator}").join("<br>");
		          }},
		          { data: 'editorName'},
		          { data: 'action',render: function(data) {
		        	var action;
		        	switch (data) {
					case "ADD":
                        action = '<font color="#00BB00"><bean:message key="global.add"/></font>'
							break;
					case "UPDATE":
                        action = '<font color="blue"><bean:message key="modify"/></font>'
							break;
					case "DELETE":
                        action = '<font color="red"><bean:message key="delete"/></font>'
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
			          	}
			          },
			          { data: 'passTime',render:function(data){
				        	 var passTime = '';
					        	if (data) {	
					        		passTime = data;
								}
					        	return passTime;
				          	}
				          },
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
		    	   {"aTargets": [9],"mData": null,
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
		    	        	if (${isAuditor} && isNotEditor && data.status == "AUDIT") {
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
		    	        	} else if (data.status == "AUDIT") {
		    	        		// 審核中
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
	    	        			return html+="</div>";
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
		    	 $row.attr("data-id",data.id);
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
		var id =  $(this).closest("tr").data("id");
		var updateTime = $table.row($(this.closest("tr"))).data().updateTime;
		$.ajax({
		url:"RobotFormalAnswersVersionServlet",
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
			url:"RobotFormalAnswersVersionServlet/check",
			type:"post",
			data:{id:id,updateTime:updateTime},
			timeout: 30000,
			success:function(data){
				if(data.msg != null){
					alert(data.msg)
					$table.ajax.reload();
				} else {
					$messageModal.find("textarea[name=message]")
					$messageModal.find("h2.modal-title").html('<font color="#FF0000">*</font><bean:message key="global.audit.enter.reject.reason"/>')
					$messageModal.find("#recjectSubmit").css("display","block")
					$messageModal.find("textarea[name=message]").prop("readonly",false).val("")
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
		$messageModal.find("textarea[name=message]").prop("readonly",true).val(data.message)
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
				url:"RobotFormalAnswersVersionServlet",
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
		url:"RobotFormalAnswersVersionServlet/delete?action=cancel&id="+id,
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
		var id = newData.publicId;
		$("#oldKeyName").html(newData.keyName);
		$("#newKeyName").html(newData.keyName);
		var nAnswers = $("<div/>").text(newData.answers).html().split("${answersSeparator}").join("<br>");
		if (newData.oldAnswers != newData.answers) {
			nAnswers = '<font color="#FF0000">'+nAnswers+'</font>';
		}
		$("#oldAnswers").html($("<div/>").text(newData.oldAnswers).html().split("${answersSeparator}").join("<br>"));
		$("#newAnswers").html(nAnswers);
		$diffModal.modal("show");
	});
	
	$('#auditTable tbody').on('click', 'button[name=editBtn]', function (){
		var data = $table.row($(this.closest("tr"))).data();
		var id =  data.id
		var updateTime = data.updateTime;
		
		var $keyName = $("#editModal").find("[name=keyName]");
		$("#editModal").find("input[name=id]").val(data.id);
		$("#editModal").find("input[name=keyName]").val(data.keyName).prop("readonly", (data.action == "UPDATE"));
		$("#editModal").find("[name=answers]").val(data.answers.split("${answersSeparator}").join("\n"));
		
		$.ajax({
			url:"RobotFormalAnswersVersionServlet/check",
			type:"post",
			data:{id:id,updateTime:updateTime},
			timeout: 30000,
			success:function(data){
				if(data.msg != null){
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
		var form  = $("#dicForm");
		if (form.find("[name=keyName]").val().trim() != "" && form.find("[name=answers]").val().trim() != "") {
			$.ajax({
				url:"RobotFormalAnswersVersionServlet",
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
			alert("<bean:message key='global.explanation.essential.formal.ex'/>");
		}
	}); 
    
    $('#auditTable tbody').on('click', 'button[name=rejectViewBtn] ', function (){
    var rejectMessage = $messageModal.find("textarea[name=message]").val();
    if(rejectMessage != null && rejectMessage.length >0){
         $("#recjectSubmit").hide(); 
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
	<div class="container" style="margin-top:2%;width:90% ">
			<div class="row">
				<table id="auditTable" class="table">
					<thead>
						<tr>
							<th style="width: 3%;"><bean:message key="global.id" /></th>
						    <th style="width: 16%;">KEY</th>
							<th style="width: 25%;">VAL</th>
							<th style="width: auto;"><bean:message key="global.audit.proceer" /></th>
							<th style="width: auto;"><bean:message key="global.audit.reason" /></th>
							<th style="width: auto;"><bean:message key="global.audit.process.time" /></th>	
							<th style="width: auto;"><bean:message key="global.audit.personnel" /></th>	
							<th style="width: auto;"><bean:message key="global.audit.time" /></th>					
							<th style="width: auto;"><bean:message key="global.audit.status" /></th>
							<th style="width: 5%;"><bean:message key="operation" /></th>
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
 						<font color="#FFFFFF">KEY</font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldKeyName"></td>
                        <td style="border:2px #FFFFFF solid;" id="newKeyName"></td>	      				
	      			</tr>
					<tr>
						<td style="border:2px #FFFFFF solid;" class="col-md-2">
 						<font color="#FFFFFF">VAL</font>
	      				</td>
	      				<td style="border:2px #FFFFFF solid;" id="oldAnswers"></td>
                        <td style="border:2px #FFFFFF solid;" id="newAnswers"></td>	      				
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
				<form id="dicForm" class="form">
					<input type="hidden" name="action">
					<input type="hidden" name="id">
					<div class="form-group row">
						<div class="col-sm-2">
							KEY：
						</div>
						<div class="col-sm-6">
							<input type="text" class="form-control" id="keyName" name="keyName" size="30">
						</div>
					</div>
					<hr>
					<div class="form-group row">
						<div class="col-sm-2">
							VAL：
						</div>
						<div class="col-sm-6">
							<textarea class="form-control" id="answers" name="answers" rows="3"></textarea>
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
