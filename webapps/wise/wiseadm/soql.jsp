<%@ page language="java" contentType="text/html; charset=UTF-8"  pageEncoding="UTF-8" %>
<%@ page isELIgnored ="false" %>

<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.gibello.zql.*" %>
<%@ page import="org.gibello.zql.data.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.solr.client.solrj.request.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%!
String diveIntoExp(PrintWriter out, ZExpression zexp, int depth) throws Exception {
	out.println("DEPTH[" + depth + "]<BR>");
    out.println("Operands (" + zexp.nbOperands() + "):" + zexp.getOperands());
	out.println("<BR>");
	out.println("Op:" + zexp.getOperator());
	out.println("<BR>");
	out.println("<BR>");

	if (zexp.nbOperands() == 2) {
		Object o1 = zexp.getOperand(0);
		Object o2 = zexp.getOperand(1);

		String syntax = "";
		String prepend = "";
		if ("AND".equalsIgnoreCase(zexp.getOperator())) {
			prepend = "+";
		}

		if (o1 instanceof ZConstant) {
			syntax += ((ZConstant)o1).getValue() + ":";
		}
		else {
			syntax += prepend + diveIntoExp(out, (ZExpression)o1, depth+1);
		}

		if (o2 instanceof ZConstant) {
			syntax += StringUtils.strip(((ZConstant)o2).getValue(), "\"");
		}
		else {
			ZExpression z2 = (ZExpression)o2;
			if (z2.nbOperands() == 1 && z2.getOperator().equals("NOT")) {

				Object z2o1 = z2.getOperand(0);

				if (z2o1 instanceof ZConstant) {
					syntax += " -" + z2o1.toString();//" + "-" + diveIntoExp(out, z2, depth+1);
				}
				else {
					syntax += " -" + diveIntoExp(out, (ZExpression)z2o1, depth+1);
				}
			}
			else {
				syntax += " " + prepend + diveIntoExp(out, z2, depth+1);
			}
		}

		if (o1 instanceof ZExpression && o2 instanceof ZExpression)
			return "(" + syntax + ")";
		return syntax;
	}
	return "";
}%>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() == 0) {
	return;
}
%>
<%@taglib uri="/WEB-INF/tld/jmesa.tld" prefix="jmesa"%>
<HTML>
<HEAD>
<TITLE>WiSe - SoQL 介面</TITLE>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>

<!-- / bootstrap3 -->
<link href="<%= request.getContextPath() %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>

<!-- / jquery ui -->
<link href='<%= request.getContextPath() %>/assets/stylesheets/jquery_ui/jquery-ui-1.10.0.custom.css' media='all' rel='stylesheet' type='text/css'>
<link href='<%= request.getContextPath() %>/assets/stylesheets/jquery_ui/jquery.ui.1.10.0.ie.css' media='all' rel='stylesheet' type='text/css'>
<script src='<%= request.getContextPath() %>/assets/javascripts/jquery_ui/jquery-ui.min.js' type='text/javascript'></script>

<script src='<%= request.getContextPath() %>/script/jquery.fileDownload.js' type='text/javascript'></script>
<!--[if lt IE 9]>
  <script src="<%=request.getContextPath()%>/script/html5shiv.js"></script>
  <script src="<%=request.getContextPath()%>/script/respond.min.js"></script>
<![endif]-->
</HEAD>
<BODY>

<form id="myForm">
<h2>Currently only support following syntax (semicolon is required):</h2>
<ul>
<li>UPDATE coreX SET Name_t="ABC", Date_dt="2015/11/11 11:11:11" WHERE Date_dt="[NOW/DAY-3DAY TO NOW]";</a></li>
<li>coreX 的部分因為 SQL語法不支援 "-"，所以語法中請用底線（ex. core-user-1 請用 core_user_1）</li>
<li>可以欄位互相指定 (ex. SET Name_t=Subject_t, ....)</li>
<li>SET 部分不支援加減乘除的運算式</li>
<li>若想清除欄位 A，就用 SET A=Null_ms（因為當讓他等於一個不存在的欄位時就會清空，除非真的有 Null_ms 這個欄位）</li>
<li>每次 "doIt" 只會抓前 50,000 筆出來處理，超過 50,000 的要多 "doIt" 幾次</li>
<li>記住，請先 "test" 再 "doIt".... 這是沒有後悔藥的</li>
</ul>

<div class="progress progress-striped active">
  <div class="progress-bar" id="updateProgress" role="progressbar" aria-valuenow="60" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
    <span class="sr-only">0% Complete</span>
  </div>
</div>
<textarea name="soql" cols="100" rows="10"><%= request.getParameter("soql") != null ? request.getParameter("soql") : "" %></textarea>
<input type="hidden" name="dryRun" value="true">
<input type="submit" value="test">
<input type="button" id="doIt" value="doIt">
</form>
<script>
$('#doIt').click(function() {
	$('input[name="dryRun"]').val("false");
	$('#myForm').submit();
});
</script>

<%
String soql = request.getParameter("soql");
if (StringUtils.isNotEmpty(soql)) {

    ZqlParser p = new ZqlParser( IOUtils.toInputStream( soql ) );
    ZStatement st;
    try {
	    while((st = p.readStatement()) != null) {

			System.out.println("***" + st.toString()); // Display the statement

			if(st instanceof ZQuery) { // An SQL query: query the DB
				out.println("It's a select");
			} else if(st instanceof ZUpdate) { // An SQL insert
				SolrQuery sq = new SolrQuery("*:*");
				out.println("=======================<BR>");
				out.println("It's a UPDATE to ");
				ZUpdate ust = (ZUpdate)st;
				out.println(ust.getTable());
				out.println("<BR>");

				String coreName = ust.getTable();
				coreName = coreName.replaceAll("_", "-");
				ZExpression where = (ZExpression) ust.getWhere();

				boolean whileMode = true;
				boolean dryRun = !"false".equalsIgnoreCase(request.getParameter("dryRun"));
				int LIMIT = 500000;
				long firstTotal = -1;
				int processed = 0;
				int rows = 10000;
				int start = 0;
				long currPos = 0;
				int lastPercent = 0;

				//out.println("QUERY::::" + diveIntoExp(out, where, 1));
				String fq = diveIntoExp(out, where, 1);
				out.println("=======================<BR>");
				out.println("QUERY::::" + fq);
				out.println("<BR>");
				sq.addFilterQuery(fq);
				sq.setRows(rows);
				SolrServer server = com.intumit.solr.SearchManager.getServer(coreName);
				SolrInputDocument docTpl = new SolrInputDocument();

				while (currPos < LIMIT) {
					sq.setStart(start);
					QueryResponse srsp = server.query(sq);
					SolrDocumentList docs = srsp.getResults();

					if (firstTotal == -1) {
						firstTotal = docs.getNumFound();
						out.println("Found: " + firstTotal);
						out.println("<BR>");

						for (int cuc=1; cuc <= ust.getColumnUpdateCount(); cuc++) {
							String fn = ust.getColumnUpdateName(cuc);
							String val = ust.getColumnUpdate(cuc).toString();

							if (val.startsWith("\"") && val.endsWith("\"")) {
								Map<String,Object> partialUpdate = new HashMap<String,Object>();
								partialUpdate.put("set", StringUtils.strip(val, "\""));
								docTpl.addField(fn, partialUpdate);
							}

							out.println("SET THE ");
							out.println(fn);
							out.println(" TO ");
							out.println(val);
							out.println("<BR>");
						}
					}
					else {
						if (firstTotal == docs.getNumFound() + rows) {
							out.println("The update will decrease total num found, using while mode.");
							whileMode = true;
						}
						else if (firstTotal == docs.getNumFound()) {
							out.println("The update won't decrease total num found, diable while mode.");
							whileMode = false;
							start += rows;
							continue;
						}
					}

					if (!dryRun) {
						if (docs.size() == 0) {
							break;
						}
						for (SolrDocument idDoc: docs) {
							Object id = idDoc.getFieldValue("id");

							SolrInputDocument doc = docTpl.deepCopy();
							doc.addField("id", id);

							for (int cuc=1; cuc <= ust.getColumnUpdateCount(); cuc++) {
								String fn = ust.getColumnUpdateName(cuc);
								String val = ust.getColumnUpdate(cuc).toString();

								if (!(val.startsWith("\"") && val.endsWith("\""))) {
									Map<String,Object> partialUpdate = new HashMap<String,Object>();

									if (idDoc.getFieldValue(val) != null) {
										partialUpdate.put("set", idDoc.getFieldValue(val));
									}
									else {
										partialUpdate.put("set", null);
									}
									doc.addField(fn, partialUpdate);
								}
							}

							int percent = Math.round((float)currPos * 100f / (float)Math.min(firstTotal, LIMIT));

							if (lastPercent != percent) {
								lastPercent = percent;

								if (percent % 5 == 0) {
									out.println("<script>$('#updateProgress').css({'width':'" + percent + "%'});</script>");
									out.flush();
								}
							}
							out.println("updating [" + id + "]:" + doc + ".....<BR>");
							server.add(doc);

							currPos++;
						}
						server.commit();
					}
					else {
						break; // dryRun only loop once.
					}

					if (!whileMode) {
						start += rows;
					}
				}
			}
	    }
    }catch (Exception ex) {
    	ex.printStackTrace();
    }
}
%>

</BODY>
</HTML>
