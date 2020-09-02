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
		 import="org.apache.wink.json4j.*"
		 import="com.intumit.solr.SearchManager"
		 import="com.intumit.solr.util.*"
		 import="jofc2.*"
		 import="jofc2.model.*"
		 import="jofc2.model.axis.*"
		 import="jofc2.model.elements.*"
		 import="jofc2.model.metadata.*"
		 import="jofc2.util.*" %><%!
long getStat(String from, String to, String func) {
	SolrServer server = SearchManager.getServer("core-keyword");
	SolrQuery query = new SolrQuery();
	query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
	if (StringUtils.isNotEmpty(func))
		query.addFilterQuery("Func_s:" + func);
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
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/json2.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/flotr2.min.js"></script>
<script type="text/javascript">
swfobject.embedSWF("<%= request.getContextPath() %>/open-flash-chart.swf", "report", "100%", "100%", "9.0.0", "expressInstall.swf"
);
</script>
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
<TABLE border="1" class="table table-striped">
<tr><th width="180px"><bean:message key='statistics.space'/></th><th colspan="10"><bean:message key='statistics.kw.menu'/></th></tr>
<%

JSONArray dataSet = new JSONArray();
JSONArray d1 = new JSONArray();
dataSet.put(d1);
int numOfLabels=0;
for (int i=0; i < fromArr.length; i++) {
	long docNum = getStat(fromArr[i], toArr[i], request.getParameter("func"));
	numOfLabels++;
	JSONArray jDot = new JSONArray();
	jDot.put(WiSeUtils.fromSolrDateStr(fromArr[i]).toDate().getTime());
	jDot.put(docNum);
	d1.put(jDot);
	%>
	<TR>
	<td><%= WiSeUtils.fromSolrDateStr(fromArr[i]).toString(ISODateTimeFormat.basicDate()) %>-<%= WiSeUtils.fromSolrDateStr(toArr[i]).toString(ISODateTimeFormat.basicDate()) %></td>
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
<div id="chart-div" style="height:300px; padding:10px;">

<script type="text/javascript">
(function basic_time(container) {
	  var options, o;

	  options = {
	    xaxis : {
	      mode : 'time',
	      labelsAngle : 45
	    },
	    selection : {
	      mode : 'x'
	    },
	    HtmlText : false,
	    title : '<bean:message key="statistics.kw.menu"/>'
	  };

	  // Draw graph with default options, overwriting with passed options
	  function drawGraph (opts) {

	    // Clone the options, so the 'options' variable always keeps intact.
	    o = Flotr._.extend(Flotr._.clone(options), opts || {});

	    // Return a new graph.
	    return Flotr.draw(
	      container,
	      <%= dataSet.toString() %>,
	      o
	    );
	  }

	  graph = drawGraph();

	  Flotr.EventAdapter.observe(container, 'flotr:select', function(area){
	    // Draw selected area
	    graph = drawGraph({
	      xaxis : { min : area.x1, max : area.x2, mode : 'time', labelsAngle : 45 },
	      yaxis : { min : area.y1, max : area.y2 }
	    });
	  });

	  // When graph is clicked, draw the graph with default area.
	  Flotr.EventAdapter.observe(container, 'flotr:click', function () { graph = drawGraph(); });
	})(document.getElementById("chart-div"));
</script>
</div>
</BODY>
</HTML>
