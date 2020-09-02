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
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.solr.robot.TemplateUtil.*"
	import="com.intumit.solr.robot.connector.line.RichMessage"
	import="com.intumit.solr.tenant.*"
 %><%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%><%!
public static class MyCustomQAReplacer implements Replacer {
	Integer tenantId;
	List<Object[]> entities = new ArrayList<Object[]>();

	public MyCustomQAReplacer(Integer tenantId) {
		this.tenantId = tenantId;
	}

	@Override
	public String call(String name, String val) {
		if (StringUtils.startsWith(name, "$")) {
			if (StringUtils.equals(name, "$NUM")) {
				return CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.NUMBER);
			}
			else if (StringUtils.equals(name, "$DATETIME") || StringUtils.equals(name, "$DATE")) {
				return CategorizedKeyValuePair.toInlineKey(CategorizedKeyValuePair.Type.TEMPORAL);
			}
			else {
				String suffix = "";
				if (StringUtils.isNotEmpty(val)) {
					suffix = val.replaceAll("[_/\\s\\$\\+]+", "");
				}
				
				
				String key = null;
				QAEntity e = QAEntity.get(tenantId, name.replaceAll("[_/\\s\\$\\+]+", ""), suffix.replaceAll("[_/\\s\\$\\+]+", ""), null);
				//System.out.println("Find " + name + " / " + suffix + " = " + e);

				if (StringUtils.startsWith(name, "$+"))
					key = CustomData.toInlineKey(name, StringUtils.split(suffix, ":")[0]);
				else
					key = CustomData.toPartialInlineKey(name, StringUtils.split(suffix, ":")[0]);
				
				entities.add(new Object[] {key, e});
				
				return key;
			}
		}

		return null;
	}
	
	@Override
	public String call(QAContext ctx, String name, String val) {
		return call(name, val);
	}
	
	public List<Object[]> getMatchedEntities() {
		return entities;
	}
}
%>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>Custom QA Check</TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>

<script type="text/javascript">
</script> 
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>

<div class="container">
<%
com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
String docId = request.getParameter("id");

QAUtil qautil = QAUtil.getInstance(t);
QA qa = new QA(qautil.getMainQASolrDocument(QAUtil.id2Kid(docId)));
String pluginId = (String)qa.getFieldValue(QA.FN_PLUGIN_ID);
if (!StringUtils.equals(CustomQA.ID, pluginId)) {
	// Should not be here
	return ;
}

List<String> altTpls = qa.getQuestionAltTemplates();

int rows = 500;
int start = 0;
int count = 0;
%>
<table class="table table-bordered table-striped">
<tr><th>資料類型</th><th>資料欄位及值列表</th>
<%
for (String rawAltTpl: altTpls) {
	String altTpl = QA.parseAndGetQAAltOnly(rawAltTpl);
	MyCustomQAReplacer mcqar = new MyCustomQAReplacer(t.getId());
	String replaced = TemplateUtil.process(altTpl, mcqar);
	%>
	<tr>
		<td><%= altTpl %></td>
	<%
	try {
		%><td class="col-md-10"><%
		for (Object[] pair: mcqar.getMatchedEntities()) {
			String key = (String)pair[0];
			QAEntity e = (QAEntity)pair[1];
			Set<CustomData> cds = CustomData.getAllTermsFromSpecificDataType(e, t.getCoreServer());

			%><div class="row"><%
			%><div class="col-md-2"><%
				%><strong><%= e.getName() %></strong><%
			%></div><%
			%><div class="col-md-8"><%= replaced %><%
			for (CustomData cd: cds) {
				String str = StringUtils.replaceOnce(replaced, key, cd.getValue());
				%><br><%= str %><%
			}
			%></div><%
			%><div class="col-md-2"><% 
			%></div><%
			%></div><%
			%><div class="row"><hr/></div><%
		}
		%></td><%
		%>
		</tr>
		<%
	}
	catch (Exception e) {
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