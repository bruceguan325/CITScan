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
<%@ page import="org.apache.lucene.index.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="org.apache.solr.request.*" %>
<%@ page import="org.apache.solr.search.*" %>
<%@ page import="org.joda.time.*" %>
<%@ page import="org.joda.time.format.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@ page import="jofc2.*" %>
<%@ page import="jofc2.model.*" %>
<%@ page import="jofc2.model.axis.*" %>
<%@ page import="jofc2.model.elements.*" %>
<%@ page import="jofc2.model.metadata.*" %>
<%@ page import="jofc2.util.*" %>
<%!
DateTimeFormatter lblSdfDay = DateTimeFormat.forPattern("MM/dd");
DateTimeFormatter lblSdfWeek = DateTimeFormat.forPattern("MM/dd");
DateTimeFormatter lblSdfMonth = DateTimeFormat.forPattern("yyyy/MM");

Label toLabel(DateTime date, String gap) {
	String lbl;
	if ("day".equals(gap)) {
		lbl= date.toString(lblSdfDay);
	}
	else if ("week".equals(gap)) {
		lbl= date.toString(lblSdfWeek);
	}
	else if ("month".equals(gap)) {
		lbl= date.toString(lblSdfMonth);
	}
	lbl= date.toString(lblSdfDay);
	
	Label l =  new Label(lbl);
	return l;
}

long getStat(String from, String to, String queryStr, String func) {
	SolrServer server = SearchManager.getServer("core0");
	SolrQuery query = new SolrQuery();
	query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
	if (func != null)
		query.addFilterQuery("Func:" + func);
	query.setQueryType("standard");
	query.setQuery(queryStr == null ? "*:*" : queryStr);
	try {
		QueryResponse rsp = server.query(query);
		
		return rsp.getResults().getNumFound();
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
	
	return 0;
}

String toUrlRequest(HttpServletRequest req, String paramName) {
	StringBuffer sb = new StringBuffer();
	
	String[] values = req.getParameterValues(paramName);
	
	for (int i=0; i < values.length; i++) {
		if (i > 0)
			sb.append("&");
		try {
			sb.append(paramName + "=" + URLEncoder.encode(values[i], "UTF-8"));
		}
		catch (Exception ex) {}
	}
	
	return sb.toString();
}

static Collection<String> POSSIBLE_FIELDNAMES = Arrays.asList(new String[] {"id", "Name_t", "Title_t", "Date_dt", "TopCategoryName_s", "FirstCategoryName_s", "TopCategory_ms", "Name", "LogTime"});
public String makeUrl(String coreName, HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + WiSeEnv.getAdminContextPath() + "/" + jsp + "?coreName=" + coreName + (query == null ? "" : ("&" + (query)));
	return url;
}

%>
<HTML>
<HEAD>
<TITLE>WiSe - Keyword Statistics</TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/json/json2.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/swfobject.js"></script>
<script type="text/javascript">
swfobject.embedSWF("<%= request.getContextPath() %>/open-flash-chart.swf", "report", "100%", "100%", "9.0.0", "expressInstall.swf"
);
</script>
<link href="./admin.css" type="text/css" rel="stylesheet"/>
</HEAD>
<BODY>
<%
Chart chart = new Chart();
chart.setTitle(new Text("統計"));
chart.setBackgroundColour("#FFFFFF");
//chart.setTooltip(Tooltip.)
YAxis ya = new YAxis();
//ya.setRange(10000, 110000, 10000);
ya.setGridColour("#DDDEE1");
ya.setColour("#96A9C5");
XAxis xa = new XAxis();
xa.setGridColour("#DDDEE1");
xa.setColour("#96A9C5");
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

DateTime now = DateTime.now();

String sevenDaysUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q") 
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusWeeks(1).toString(ISODateTimeFormat.basicDate()) + "&gap=day";

String oneMonthUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q") 
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusMonths(1).toString(ISODateTimeFormat.basicDate()) + "&gap=day";

String threeMonthUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q") 
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusMonths(3).toString(ISODateTimeFormat.basicDate()) + "&gap=week";

String sixMonthUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q") 
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusMonths(6).toString(ISODateTimeFormat.basicDate()) + "&gap=week";

String relatedKwUrl = "statistics-doc-wiki-result.jsp?q=" 
				+ URLEncoder.encode(request.getParameter("q").replaceAll("Keyword_ms:", ""), "UTF-8") 
				+ "&from=" + fromBase.toString(ISODateTimeFormat.basicDate()) + "&to=" + toBase.toString(ISODateTimeFormat.basicDate()) + "&gap=" + gap;
%>
&nbsp;<a href="<%=sevenDaysUrl%>">過去七天</a></a>
&nbsp;<a href="<%=oneMonthUrl%>">過去一個月</a></a>
&nbsp;<a href="<%=threeMonthUrl%>">過去三個月</a></a>
&nbsp;&nbsp;&nbsp;<a href="<%=relatedKwUrl%>">相關連字分析</a></a>
<div id="resize" style="width:100%; height:300px; padding:10px;">
<div id="report"></div>
</div>

<TABLE border="1">
<TR>
<% 

String[] qqqs = request.getParameterValues("q");

for (int qc=0; qc < qqqs.length; qc++) {
	String qtitle = qqqs[qc].replaceAll("Keyword_ms","關鍵字").replaceAll("BODY_mt","全文");
	String randomColor = "#" + Integer.toHexString((new Random(System.currentTimeMillis()).nextInt(16777216)));
	LineChart lcc = new LineChart();
	lcc.setColour(randomColor);
	lcc.setText(qtitle);
	%>
	<TD>
	<TABLE border="1">
	<tr><th colspan="11"><%= qtitle %></th></tr>
	<tr><th>統計區間</th><th colspan="10">曝光統計</th></tr>
	<%
	for (int i=0; i < fromArr.length; i++) {
		long docNum = getStat(fromArr[i], toArr[i], qqqs[qc], request.getParameter("func"));
		//lcc.setText();
		%>
		<TR>
		<td><%= WiSeUtils.fromSolrDateStr(fromArr[i]).toString(ISODateTimeFormat.basicDate()) %>-<%= WiSeUtils.fromSolrDateStr(toArr[i]).toString(ISODateTimeFormat.basicDate()) %></td>
		<%
		LineChart.Dot dot = new LineChart.Dot(docNum, randomColor, 5, 1);
		lcc.addDots(dot);
		%>
		<TD align="center" nowrap>
			<font color="BLUE"><%= docNum %></font>
		</TD>
		<%
		%>
		</TR>
		<%
	}
	%>
	</TABLE>
	</TD>
	<%
	chart.addElements(lcc.setTooltip("#val#"));
}
%>
</TR>
</TABLE>
<%
int numOfLabels = 0;
for (int i=0; i < fromArr.length; i++) { 
	xa.addLabels(toLabel(WiSeUtils.fromSolrDateStr(fromArr[i]), gap));
	numOfLabels++;
}
xa.setSteps(1);
xa.getLabels().setSteps((int)Math.round((double)numOfLabels / 15));
xa.getLabels().setRotation(Label.Rotation.VERTICAL);
chart.setYAxis(ya);
chart.setXAxis(xa);
chart.setFixedNumDecimalsForced(true);
chart.setDecimalSeparatorIsComma(false);
chart.setNumDecimals(0);
chart.computeYAxisRange(10);
//System.err.println(chart.toDebugString());
%>
<script type="text/javascript">
var d = <%=chart%>;
function findSWF(movieName) {
if (navigator.appName.indexOf("Microsoft")!= -1) {
return window[movieName];
} else {
return document[movieName];
}
}
function open_flash_chart_data(){
return JSON.stringify(d);
}

$(document).ready(function(){
$("#resize").resizable();
});
</script>

<BR><BR><BR><BR>
<%
	// 印出查詢結果，方便比對用
String coreName = "core0";

CoreContainer cc = SearchManager.getLocalCores();
SolrCore core = cc.getCore(coreName);
SolrServer server = SearchManager.getServer(coreName);
SolrIndexSearcher search = core.getSearcher().get();
Collection<String> fieldNames = search.getFieldNames();
SolrQuery sqry = new SolrQuery();
int start = 0;

if (request.getParameter("SELECT") != null) {
	String select = request.getParameter("SELECT").trim();
	String[] clauses = select.split("\n");
	String query = "";
	for (int ccc = 0; ccc < clauses.length; ccc++) {
		String c = clauses[ccc].trim();
		if (c.length() == 0)
	continue;
		String key = c.substring(0, c.indexOf("="));
		String val = c.substring(c.indexOf("=") + 1);
		
		sqry.add(key, val);
		
		if (ccc > 0)
	query += "&";
		query += key + "=" + URLEncoder.encode(val, "UTF-8");
	}
}
else if (request.getParameter("q") != null) {
	String qqq = request.getParameter("q");
	start = request.getParameter("start") == null ? 0 : Integer
	.parseInt(request.getParameter("start"));
	String qt = request.getParameter("qt") == null ? "standard"
	: request.getParameter("qt");
	String sort = request.getParameter("sort") == null ? "Date_dt desc"
	: request.getParameter("sort");
	String[] filterQuery = request.getParameterValues("fq");
	SolrQuery.ORDER sortOrder = SolrQuery.ORDER.desc;
	
	if (sort.indexOf(" ") != -1) {
		String sortDir = sort.substring(sort.lastIndexOf(" "))
		.trim();
		sort = sort.substring(0, sort.lastIndexOf(" "));
		sortOrder = SolrQuery.ORDER.valueOf(sortDir);
	}
	for (int jjj = 0; filterQuery != null
	&& jjj < filterQuery.length; jjj++) {
		sqry.addFilterQuery(filterQuery[jjj].trim());
	}
	
	sqry.setQuery(qqq);
	sqry.addSortField(sort, sortOrder);
}
else {
	sqry.setQuery("*:*");
}
	
sqry.setRows(10);
sqry.setStart(start);
if (sqry.getFields() == null) {
	sqry.setFields(POSSIBLE_FIELDNAMES.toArray(new String[0]));
}
sqry.addFilterQuery("Date_dt:[" + WiSeUtils.toSolrDateStr(fromBase) + " TO " +  WiSeUtils.toSolrDateStr(toBase) + "]");

QueryResponse resp = server.query(sqry);
SolrDocumentList docList = resp.getResults();
         //For simplification, we are not using the JSTL
%>
<TABLE border="1" width="100%">
<%
int docOffset = 1;
List<String> fields = new ArrayList<String>(Arrays.asList(sqry.getFields().split(",")));
fields.retainAll(fieldNames);

for (Iterator<SolrDocument> docItr = docList.iterator(); docItr.hasNext(); docOffset++) {
	SolrDocument doc = docItr.next();
	String contentUrl = request.getContextPath() + WiSeEnv.getAdminContextPath() + "/" + coreName + "/select?q=id%3A%22" +  doc.getFieldValue("id") + "%22&sort=Date_dt+desc&wt=xslt&tr=content.xsl";
	%>
	<% if (docOffset==1) { // 印出欄位名稱 %>
	<TR>
		<TH>　</TH><TH>No.</TH><TH>ID</TH>
		<%
		for (String fn: fields) {
			if ("id".equalsIgnoreCase(fn))
				continue;
		%>
			<TH><%= fn %></TH>
		<%
		}
		%>
	</TR>
	<% } %>
	<TR>
		<TD></TD>
		<TD><%= docOffset %></TD>
		<TD>
		<A TARGET="_blank" HREF="<%= contentUrl %>"><%= doc.getFieldValue("id") %></A>
		</TD>
		<%
		// 印出每一個 document 的各欄位值
		for (String fn: fields) {
			
			if ("id".equalsIgnoreCase(fn))
				continue;
			
			Collection values = doc.getFieldValues(fn);
			String valStr = (values != null) ? StringUtils.left(values.toString(), 30) : "";
			%>
			<td>
			<%= valStr %>
			</td>
			<%
		}
		%>
	</TR>
<%
}
%>
</TABLE>


</BODY>
</HTML>
