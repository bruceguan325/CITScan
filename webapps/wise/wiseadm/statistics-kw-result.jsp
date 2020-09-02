<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" 
		 import="javax.servlet.ServletException" 
		 import="javax.servlet.http.*" 
		 import="java.io.*" 
		 import="java.net.*" 
		 import="java.text.*" 
		 import="java.util.*" 
		 import="org.apache.commons.lang.*" 
		 import="org.apache.commons.httpclient.*" 
		 import="org.apache.commons.httpclient.methods.*" 
		 import="org.apache.commons.httpclient.params.HttpMethodParams" 
		 import="org.apache.solr.core.*" 
		 import="org.apache.solr.servlet.*" 
		 import="org.apache.solr.client.solrj.*" 
		 import="org.apache.solr.client.solrj.embedded.*" 
		 import="org.apache.solr.client.solrj.response.*" 
		 import="org.apache.solr.common.*" 
         import="org.joda.time.*" 
         import="org.joda.time.format.*"
		 import="com.intumit.solr.SearchManager" 
		 import="com.intumit.solr.util.WiSeUtils" %><%!
FacetField getStat(String from, String to, String func) {
	SolrServer server = SearchManager.getServer("core-keyword");
	SolrQuery query = new SolrQuery();
	query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
	query.setFacet(true);
	query.addFacetField("Name_s");
	query.setFacetLimit(20);
	query.setFacetMinCount(1);
	query.setQueryType("standard");
	if (StringUtils.isNotEmpty(func))
		query.addFilterQuery("Func_s:" + func);
	query.setQuery("*:*");
	try {
		QueryResponse rsp = server.query(query);
		
		return rsp.getFacetField("Name_s");
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
	
	return null;
}
%>
<HTML>
<HEAD>
<TITLE>WiSe - Keyword Statistics</TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/json/json2.js"></script>
<%-- 
<script type="text/javascript" src="<%= request.getContextPath() %>/script/swfobject.js"></script>
<script type="text/javascript">
swfobject.embedSWF("<%= request.getContextPath() %>/open-flash-chart.swf", "report", "100%", "100%", "9.0.0", "expressInstall.swf"
);
</script>
--%>
<link rel="stylesheet" type="text/css" href="../styles/bootstrap.min.css"/>
</HEAD>
<BODY>
<%
String gap = request.getParameter("gap");
Period iGap = new Period();

if ("day".equalsIgnoreCase(gap)) {
	iGap = Period.days(1);
} else if ("week".equalsIgnoreCase(gap)) {
	iGap = Period.weeks(1);
} else if ("month".equalsIgnoreCase(gap)) {
	iGap = Period.months(1);
} else if ("quarter".equalsIgnoreCase(gap)) {
	iGap = Period.months(3);
} else if ("year".equalsIgnoreCase(gap)) {
	iGap = Period.years(1);
}

String from = request.getParameter("from");
String to = request.getParameter("to");

DateTime fromBase = ISODateTimeFormat.basicDate().parseDateTime(from);
DateTime toBase = ISODateTimeFormat.basicDate().parseDateTime(to);
List<String> fromList = new ArrayList<String>();
List<String> toList = new ArrayList<String>();
int offset = 1;
boolean overflow = false;

while (!overflow) {
	DateTime cFrom = fromBase.plus(iGap.multipliedBy(offset));
	DateTime cTo = cFrom.plus(iGap);
	
	if (cTo.isAfter(toBase))
		overflow = true;
	
	fromList.add(WiSeUtils.toSolrDateStr(cFrom));
	toList.add(WiSeUtils.toSolrDateStr(cTo));
	offset++;
}

//	System.out.println(fromList);
String[] fromArr = fromList.toArray(new String[0]);		//new String[] {"NOW-1MONTH", "2009-04-17T15:05:56.061Z/YEAR-1YEAR"};
String[] toArr = toList.toArray(new String[0]);			//new String[] {"NOW+1MONTH", "2009-04-17T15:05:56.061Z/YEAR+1YEAR"};
%>
<script>
<%
String trendUrl = "statistics-kw-trend.jsp?from=" + from + "&to=" + to + "&gap=" + gap;
if (request.getParameter("func") != null)
	trendUrl += "&func=" + request.getParameter("func");
%>
function showTrend(kw) {
	var a=encodeURIComponent||escape;
	window.location=["<%=trendUrl%>&q=", a("Name_s:"), a(kw)].join("");
}

function showTrend4All(all) {
	window.location=["<%=trendUrl%>", all].join("");
}
</script>
<TABLE border="1" class="table table-striped">
<tr><th width="180px"><bean:message key='statistics.space'/></th><th colspan="5"><bean:message key='ten.keyword'/></th><th colspan="5"><bean:message key='input.keyword'/><input type="text" onKeypress="{if (event.keyCode==13) showTrend(this.value)}"></th></tr>
<% 

for (int i=0; i < fromArr.length; i++) { 
	FacetField kwFF = getStat(fromArr[i], toArr[i], request.getParameter("func"));
	
	if (kwFF != null && kwFF.getValues() != null) {
		int counter = 0;
		String mergeAll = "";
		%>
		<TR>
		<td><%= WiSeUtils.fromSolrDateStr(fromArr[i]).toString(ISODateTimeFormat.basicDate()) %>-<%= WiSeUtils.fromSolrDateStr(toArr[i]).toString(ISODateTimeFormat.basicDate()) %></td>
		<%
		for (Iterator<FacetField.Count> iterator2 = kwFF.getValues()
				.iterator(); counter < 10; counter++) {
			
			if (iterator2.hasNext()) {
				FacetField.Count facetCount = iterator2.next();
				String word = facetCount.getName();
				mergeAll += "&q=Name_s:" + URLEncoder.encode(word, "UTF-8");
				int ccc = (int) facetCount.getCount();
	
				if (ccc > 0) {
					%>
					<TD align="center" nowrap>
						<A HREF="#" onclick="showTrend('<%= word %>')"><%= word %></A>
						<BR>
						<font color="BLUE"><%= ccc %></font>
					</TD>
					<%
				}
			}
			else {
				%>
					<TD align="center" nowrap>
						<BR>
					</TD>
				<%
			}
		}
		%>
		</TR>
		<%
	}
}
%>
</TABLE>
</BODY>
</HTML>
