<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.joda.time.*" %>
<%@ page import="org.joda.time.format.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.util.WiSeUtils" %>
<%@ page import="jofc2.*" %>
<%@ page import="jofc2.model.*" %>
<%@ page import="jofc2.model.axis.*" %>
<%@ page import="jofc2.model.elements.*" %>
<%@ page import="jofc2.model.metadata.*" %>
<%@ page import="jofc2.util.*" %>
<%!
FacetField getStat(String from, String to, String qqq, String blog, String category) {
	SolrServer server = SearchManager.getServer("core0");
	SolrQuery query = new SolrQuery();
	query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
	query.setFacet(true);
	query.addFacetField("Keyword_ms");
	query.setFacetLimit(70);
	query.setFacetMinCount(1);
	query.setQueryType("standard");
	if (StringUtils.isNotEmpty(blog))
		query.addFilterQuery("BlogId_i:" + blog);
	if (StringUtils.isNotEmpty(category))
		query.addFilterQuery("SiteCategoryId_i:" + category);
	query.setQuery(StringUtils.isNotEmpty(qqq) ? "BODY_mt:"+qqq : "*:*");
	try {
		QueryResponse rsp = server.query(query);
		
		return rsp.getFacetField("Keyword_ms");
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
	
	return null;
}

HashSet<String> stopwords = new HashSet<String>();
{
	stopwords.add("xd");
	stopwords.add("XD");
	stopwords.add("明明");
	stopwords.add("Times_New_Roman");
	stopwords.add("FON");
	stopwords.add("天一");
	stopwords.add("眼睛");
	stopwords.add("天天");
	stopwords.add("新細明體");
	stopwords.add("TT");
	stopwords.add("哈哈");
	stopwords.add("Orz");
	stopwords.add("QQ");
	//stopwords.add("");
};

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
<link href="./admin.css" type="text/css" rel="stylesheet"/>
</HEAD>
<BODY>
<%
Chart chart = new Chart();
chart.setTitle(new Text("統計"));
chart.setBackgroundColour("#FFFFFF");
%>
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

if (iGap != null) {
	while (!overflow) {
		DateTime cFrom = fromBase.plus(iGap.multipliedBy(offset));
		DateTime cTo = cFrom.plus(iGap);
		
		if (cTo.isAfter(toBase))
			overflow = true;
		
		fromList.add(WiSeUtils.toSolrDateStr(cFrom));
		toList.add(WiSeUtils.toSolrDateStr(cTo));
		offset++;
	}
}
else {
	fromList.add(WiSeUtils.toSolrDateStr(fromBase));
	toList.add(WiSeUtils.toSolrDateStr(toBase));
}

//	System.out.println(fromList);
String[] fromArr = fromList.toArray(new String[0]);		//new String[] {"NOW-1MONTH", "2009-04-17T15:05:56.061Z/YEAR-1YEAR"};
String[] toArr = toList.toArray(new String[0]);			//new String[] {"NOW+1MONTH", "2009-04-17T15:05:56.061Z/YEAR+1YEAR"};
%>
<script>
<%
String trendUrl = "statistics-wiki-trend.jsp?from=" + from + "&to=" + to + "&gap=" + gap;
if (request.getParameter("category") != null)
	trendUrl += "&category=" + request.getParameter("category");
if (request.getParameter("blog") != null)
	trendUrl += "&blog=" + request.getParameter("blog");
%>
function showTrend(kw) {
	var a=encodeURIComponent||escape;
	window.location=["<%=trendUrl%>", "&q=", a("Keyword_ms:"), a(kw), "&q=", a("BODY_mt:"), a(kw)].join("");
}

function showTrend4All(all) {
	window.location=["<%=trendUrl%>", all].join("");
}
</script>
<TABLE border="1">
<tr><th>統計區間</th><th colspan="5">十大關鍵字</th><th colspan="5">輸入關鍵字：<input type="text" onKeypress="{if (event.keyCode==13) showTrend(this.value)}"></th></tr>
<% 

for (int i=0; i < fromArr.length; i++) { 
	FacetField kwFF = getStat(fromArr[i], toArr[i], request.getParameter("q"), request.getParameter("blog"), request.getParameter("category"));
	if (kwFF != null && kwFF.getValues() != null) {
		int counter = 0;
		LineChart lcc = new LineChart();
		lcc.setColour("#13" + "" + (i * 23 % 100) + "" + (i * 17 % 100));
		lcc.setText(WiSeUtils.fromSolrDateStr(fromArr[i]).toString(ISODateTimeFormat.basicDate()) + "-" + WiSeUtils.fromSolrDateStr(toArr[i]).toString(ISODateTimeFormat.basicDate()));
		
		String mergeAll = "";
		%>
		<TR>
		<td><%= WiSeUtils.fromSolrDateStr(fromArr[i]).toString(ISODateTimeFormat.basicDate()) %>-<%= WiSeUtils.fromSolrDateStr(toArr[i]).toString(ISODateTimeFormat.basicDate()) %></td>
		<%
		for (Iterator<FacetField.Count> iterator2 = kwFF.getValues()
				.iterator(); iterator2.hasNext() && counter < 50; ) {
			FacetField.Count facetCount = iterator2.next();
			String word = facetCount.getName();
			
			if (stopwords.contains(word) || word.equalsIgnoreCase(request.getParameter("q")))
				continue;
			mergeAll += "&q=Keyword_ms:" + URLEncoder.encode(word, "UTF-8");
			
			int ccc = (int) facetCount.getCount();

			if (ccc > 0) {
				counter++;
				LineChart.Dot dot = new LineChart.Dot(ccc, "#0000FF", 3, 1);
				lcc.addDots(dot);
				
				%>
				<TD align="center" nowrap>
					<A HREF="#" onclick="showTrend('<%= word %>')"><%= word %></A>
					<BR>
					<font color="BLUE"><%= ccc %></font>
				</TD>
				<%
			}
		}
		chart.addElements(lcc);
		%>
		<TD><A onclick="showTrend4All('<%= mergeAll %>');" HREF="#">ALL</TD>
		</TR>
		<%
	}
}

YAxis ya = new YAxis();
// ya.setRange(10000, 110000, 10000);
ya.setGridColour("#DDDEE1");
ya.setColour("#96A9C5");
chart.setYAxis(ya);
XAxis xa = new XAxis();
xa.setGridColour("#DDDEE1");
xa.setColour("#96A9C5");
chart.setXAxis(xa);
chart.setFixedNumDecimalsForced(true);
chart.setDecimalSeparatorIsComma(true);
chart.computeYAxisRange(100);
//chart.computeYAxisRange(1);
%>
</TABLE>
</BODY>
</HTML>
