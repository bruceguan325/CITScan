<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page 
	import="javax.servlet.ServletConfig"
	import="javax.servlet.ServletException"
	import="javax.servlet.http.*"
	import="java.io.*"
	import="java.net.*"
	import="java.text.*"
	import="java.util.*"
	import="org.json.*"
	import="org.apache.commons.lang.StringUtils"
	import="org.apache.solr.core.*"
	import="org.apache.solr.servlet.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.client.solrj.embedded.*"
	import="org.apache.solr.client.solrj.response.*"
	import="org.apache.solr.common.*"
	import="org.apache.solr.request.*"
	import="com.intumit.solr.config.ColumnNameMappingFacade"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.entity.*"
	import="com.intumit.solr.robot.qaplugin.*"
	import="com.intumit.solr.robot.MultiChannelAnswer"
	import="com.intumit.solr.robot.dictionary.DictionaryDatabase"
	import="com.intumit.solr.robot.connector.line.RichMessage"
	import="com.intumit.solr.tenant.*"
 %><%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O2) == 0) { 
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>Index Inspector</TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>

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

</script> 
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>

<div class="container">
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
List<String> dataTypes = QAUtil.getAllPossibleFacetTerms(t.getCoreServer(), "dataType");
System.out.println("Going to process all possible terms in [" + dataTypes + "]");

int rows = 500;
int start = 0;
int count = 0;
%>
<table class="table table-bordered table-striped">
<tr><th>資料類型</th><th>資料欄位及值列表</th>
<%
for (String dataType: dataTypes) {
	if (QAUtil.DATATYPE_COMMON_SENSE.equals(dataType) || QAUtil.DATATYPE_CASUAL.equals(dataType)) continue;
	boolean isValidCate = QAEntity.isValidCodeOrCategory(dataType);
	
	%><tr><td><%= isValidCate ? "" : "<br><span class='text text-danger glyphicon glyphicon-remove'></span>" %><%= dataType %></td><%
	Set<String> scannedFNs = new HashSet<String>();
	SolrQuery q = new SolrQuery();
	q.setQuery("*:* -dataType_s:(" + QAUtil.DATATYPE_COMMON_SENSE + " " + QAUtil.DATATYPE_CASUAL + ")");
	q.setFilterQueries("dataType_s:" + dataType);
	q.setRows(rows).setStart(start);
	
	try {
		QueryResponse result = t.getCoreServer().query(q);
		SolrDocumentList docs = result.getResults();
		
		%><td class="col-md-10"><%
		for (SolrDocument doc: docs) {
			Collection<String> fns = doc.getFieldNames();
			String dt = (String)doc.getFirstValue("dataType_s");
			
			if (dt == null) continue;
			
			for (String fn: fns) {
				if ("id".equalsIgnoreCase(fn) || "dataType_s".equalsIgnoreCase(fn))
					continue;
				
				if (StringUtils.endsWithAny(fn, new String[] {"_s", "_ms"})) {
					if (scannedFNs.contains(fn)) continue;
					scannedFNs.add(fn);

					String cleanedFn = fn.replaceAll("_m?[tsifldp]$", "");
					List<String> allPossibleTerms = QAUtil.getAllPossibleFacetTerms(t.getCoreServer(), cleanedFn, "dataType_s:" + dataType);
					
					boolean isValidCode = QAEntity.isValidCodeOrCategory(cleanedFn);
					QAEntity e = QAEntity.get(t.getId(), cleanedFn, dt, null);
					
					%><div class="row"><%
					%><div class="col-md-2"><%
						%><%= isValidCode ? "" : "<br><span class='text text-danger glyphicon glyphicon-remove'></span>" %><%
						%><strong><%= cleanedFn %></strong><%
						%><%= e != null ? "<br>(" + e.getName() + ")" : "" %><%
					%></div><%
					%><div class="col-md-8"><%= StringUtils.join(allPossibleTerms, ", ") %></div><%
					%><div class="col-md-2"><% 
						%><div style="display: none;" name="category"><%= dt %></div><%
						%><div style="display: none;" name="code"><%= cleanedFn %></div><%
						if (e != null) {
							%><button class="btn btn-default" disabled="true">已加入</button><%
						}
						else if (isValidCode && isValidCate) {
							%><button class="btn btn-warning btnAddToEntity">加入</button><%
						}
						else {
							%><button class="btn btn-danger" disabled="true">名稱不合規</button><%
						}
					%></div><%
					%></div><%
					%><div class="row"><hr/></div><%
				}
			}
		}
		%></td><%
		%>
		</tr>
		<%
	}
	catch (SolrServerException e) {
		e.printStackTrace();
	}

}
%>
</table>
<script>
$('.btnAddToEntity').click(function() {
	$this = $(this);
	category = $this.closest('div').find('div[name=category]').text();
	code = $this.closest('div').find('div[name=code]').text();
	
	$.ajax({
		url: 'qaCustomDataInspect-ajax.jsp',
		data: {
			action:"add",
			category: category,
			code: code,
		},
		dataType: 'json',
		success: function(result) {
			if (result && result.status == 'Done') {
				$this.attr('disabled', true);
			}
		},
	});
});
</script>
</div>
</body> 
</html>