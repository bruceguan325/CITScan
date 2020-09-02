<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
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
<%@ page import="com.intumit.solr.searchKeywords.ClickLogFacade" %>
<%@ page import="com.intumit.solr.searchKeywords.ClickLog" %>
<%@ page import="com.intumit.solr.util.WiSeUtils" %>
<%@ page import="jofc2.*" %>
<%@ page import="jofc2.model.*" %>
<%@ page import="jofc2.model.axis.*" %>
<%@ page import="jofc2.model.elements.*" %>
<%@ page import="jofc2.model.metadata.*" %>
<%@ page import="jofc2.util.*" %>
<%!
long getStat(String from, String to, String func) {
	SolrServer server = SearchManager.getServer("core-click");
	SolrQuery query = new SolrQuery();
	query.addFilterQuery("LogTime:[" + from + " TO " + to + "]");
	//if (func != null)
		//query.addFilterQuery("Func_s:" + func);
	query.setQueryType("standard");
	query.setQuery("*:*");
	try {
		QueryResponse rsp = server.query(query);
		return rsp.getResults().getNumFound();
	}
	catch (Exception ex) {
		ex.printStackTrace();
	}
	
	return 0;
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
<link rel="stylesheet" type="text/css" href="../styles/bootstrap.min.css"/>
</HEAD>
<BODY>
<%
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String statistics = MessageUtil.getMessage(locale, "global.statistics");
Chart chart = new Chart();
chart.setTitle(new Text(statistics));
chart.setBackgroundColour("#FFFFFF");
YAxis ya = new YAxis();
//ya.setRange(10000, 110000, 10000);
ya.setGridColour("#DDDEE1");
ya.setColour("#96A9C5");
XAxis xa = new XAxis();
xa.setGridColour("#DDDEE1");
xa.setColour("#96A9C5");
%>
<%
int count = Integer.parseInt(request.getParameter("count"));
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
<TABLE border="1" class="table table-striped">
<tr>
<th width="180px"><bean:message key='statistics.space'/></th>
<th colspan="1"><bean:message key='article.clicks'/></th>
<th colspan="1"><bean:message key='hot.article.title'/></th>
</tr>
<% 

LineChart lcc = new LineChart();
for (int i=0; i < fromArr.length; i++) { 
	List<ClickLog> hotList = ClickLogFacade.getInstance().getHotDocumentFromSearchBeans(fromArr[i], toArr[i], request.getParameter("func"), count);
	//lcc.setText();
	xa.addLabels(WiSeUtils.fromSolrDateStr(fromArr[i]).toString(ISODateTimeFormat.basicDate()));
	%>
	<TR>
	<td><%= WiSeUtils.fromSolrDateStr(fromArr[i]).toString(ISODateTimeFormat.basicDate()) %>-<%= WiSeUtils.fromSolrDateStr(toArr[i]).toString(ISODateTimeFormat.basicDate()) %></td>
	<%--
	LineChart.Dot dot = new LineChart.Dot(docNum, "#0000FF", 3, 1);
	lcc.addDots(dot);
	--%>
	
	<% 
	int size = hotList.size();
	int index = 0;
	
	if (size == 0) {
	%>
	<TD></TD>
	<% 	
	} else {
	for(ClickLog hot:hotList) {
		index++;
	%>
		<TD align="center" nowrap>
		<font color="BLUE"><%= hot.getFrequency() %></font>
		</TD>
		
		<TD align="center" nowrap>
		<font color="BLUE"><%= hot.getClickTitle() %></font>
		</TD>
		<% if (size > index) {%>
		</TR>
		
		<TR>
		<TD></TD>
	<%
		}}}
	%>
	</TR>
	<%
} 

%>
</TABLE>


</div>
</BODY>
</HTML>
