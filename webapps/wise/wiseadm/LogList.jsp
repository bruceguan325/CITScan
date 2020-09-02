<%@page import="java.util.List"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ include file="/commons/taglib.jsp"%>

<%
Boolean b = (Boolean)session.getAttribute("isTestAdmin");
if( b == null || !b){
	request.getRequestDispatcher("../ErrorPage404.jsp").forward(request, response);
}
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>jetty logs</title>
<jsp:include page="header-qa.jsp"></jsp:include>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<script type="text/javascript"
	src="<%=request.getContextPath()%>/wiseadm/js/jquery.dataTables.min.js"></script>
<script type="text/javascript"
	src="<%=request.getContextPath()%>/wiseadm/js/dataTables.bootstrap.min.js"></script>
<script
	src="<%=request.getContextPath()%>/wiseadm/js/codemirror/lib/codemirror.js"></script>
<script
	src="<%=request.getContextPath()%>/wiseadm/js/codemirror/mode/groovy/groovy.js"></script>

<link rel="stylesheet"
	href="<%=request.getContextPath()%>/wiseadm/js/codemirror/theme/dracula.css" />
<link rel="stylesheet"
	href="<%=request.getContextPath()%>/wiseadm/js/codemirror/lib/codemirror.css" />

<script type="text/javascript">
	$(document)
			.ready(
					function() {
						var logModal = $('#logModal').modal("hide");

						var editor = CodeMirror.fromTextArea(document
								.getElementById("code"), {
							lineNumbers : true,
							mode : "text/groovy",
							theme : "dracula",
							foldGutter : true,
							readOnly : true,
							matchBrackets : true,
						});
						editor.setSize(1250, 450);

						var $table = $("#logTable")
								.DataTable(
										{
											ajax : {
												"url" : "logViewer",
												"type" : "GET",
											},
											columns : [ {
												data : 'fileName'
											}, {
												data : 'size'
											}, {
												data : 'lastModified'
											}, ],
											"aoColumnDefs" : [ {
												"aTargets" : [ 3 ],
												"mData" : null,
												"mRender" : function(data) {
													return '<button class="btn btn-primary viewBtn">檢視</button>'
												}
											} ]
										});

						$('#logTable tbody').on(
								'click',
								'button.viewBtn',
								function() {
									var fileName = $(this).parents("tr").find(
											"td:first-child").text();
									$(logModal).find("input[name=fileName]")
											.val(fileName)
									if (fileName) {
										$.ajax({
											url : "logViewer",
											type : "POST",
											data : {
												"fileName" : fileName
											},
											success : function(data) {
												$("#size").text(data.size)
												editor.setValue(data.log)
												logModal.show()
											},
											error : function() {
											}

										})
									}
								})

						$("button.submit").on(
								"click",
								function() {
									var fileName = $(logModal).find(
											"input[name=fileName]").val()
									var start = $("input[name=start]").val()
									var end = $("input[name=end]").val()
									if (start && end) {
										$.ajax({
											url : "logViewer",
											type : "POST",
											dataType : "json",
											data : {
												"fileName" : fileName,
												"start" : start,
												"end" : end
											},
											success : function(data) {
												$("#size").text(data.size)
												editor.setValue(data.log)
											},
											error : function() {
											}
										})
									}
								})

						$("#btn-close").on("click", function() {
							logModal.hide()
						})
					})
</script>
<style type="text/css">
textarea.log {
	width: 100%;
}
</style>
<body>
	<div class="container">
		<div class="logs">
			<table id="logTable" class="table">
				<thead>
					<tr>
						<th>name</th>
						<th>size</th>
						<th>lastModified</th>
						<th>action</th>
					</tr>
				</thead>
			</table>
		</div>

		<div class="logContent">
			<div id="logModal" class="modal" tabindex="-1" role="dialog">
				<div class="modal-dialog" style="width: 1300px; height: 480px"
					role="document">
					<div class="modal-content">
						<div class="modal-header">
							<div class="row">
								<input type="hidden" name="fileName">
								<div class="col-sm-3">
									<span>起始行數 <input type="text" name="start"></span>
								</div>
								<div class="col-sm-3">
									<span>結束行數 <input type="text" name="end"></span>
								</div>
								<div class="col-sm-3">
									<span>總共行數 </span>
									<span id="size"></span>
								</div>
								<div class="col-sm-3">
									<span><button class="btn btn-primary submit">確定</button></span>
								</div>
							</div>

						</div>
						<div class="modal-body">
							<textarea id="code" class="log"></textarea>
						</div>
						<div class="modal-footer">
							<button type="button" id="btn-close" class="btn btn-secondary">Close</button>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>

</body>
</html>