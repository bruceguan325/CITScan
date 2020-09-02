<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig"
		 import="javax.servlet.ServletException"
		 import="javax.servlet.http.*"
		 import="java.io.*"
		 import="java.net.*"
		 import="java.text.*"
		 import="java.util.*"
		 import="org.apache.wink.json4j.*"
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
		 import="com.intumit.solr.util.*"
		 import="jofc2.*"
		 import="jofc2.model.*"
		 import="jofc2.model.axis.*"
		 import="jofc2.model.elements.*"
		 import="jofc2.model.metadata.*"
		 import="jofc2.util.*" %><%!

long getStat(String from, String to, String queryStr, String func) {
	SolrServer server = SearchManager.getServer("core-keyword");
	SolrQuery query = new SolrQuery();
	query.addFilterQuery("Date_dt:[" + from + " TO " + to + "]");
	if (StringUtils.isNotEmpty(func))
		query.addFilterQuery("Func_s:" + func);
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
%>
<HTML>
<HEAD>
<TITLE>WiSe - Keyword Statistics</TITLE>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/json2.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/js/flotr2.min.js"></script>
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

DateTime now = DateTime.now();

String sevenDaysUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q")
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusWeeks(1).toString(ISODateTimeFormat.basicDate()) + "&gap=day";

String oneMonthUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q")
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusMonths(1).toString(ISODateTimeFormat.basicDate()) + "&gap=day";

String threeMonthUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q")
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusMonths(3).toString(ISODateTimeFormat.basicDate()) + "&gap=week";

String sixMonthUrl = "statistics-kw-trend.jsp?" + toUrlRequest(request, "q")
		+ "&from=" + now.toString(ISODateTimeFormat.basicDate()) + "&to=" + now.minusMonths(6).toString(ISODateTimeFormat.basicDate()) + "&gap=week";
%>
&nbsp;<a href="<%=sevenDaysUrl%>">過去七天</a></a>
&nbsp;<a href="<%=oneMonthUrl%>">過去一個月</a></a>
&nbsp;<a href="<%=threeMonthUrl%>">過去三個月</a></a>
<div id="chart-div" style="height:300px; padding:10px;">
<div id="report"></div>
</div>
<%
String[] qqqs = request.getParameterValues("q");
JSONArray dataSet = new JSONArray();

for (int qc=0; qc < qqqs.length; qc++) {
	JSONArray d1 = new JSONArray();
	dataSet.put(d1);
	%>
	<TABLE border="1" class="table table-striped">
	<tr><th width="180px">統計區間</th><th colspan="10">查詢統計</th></tr>
	<%
	for (int i=0; i < fromArr.length; i++) {
		long docNum = getStat(fromArr[i], toArr[i], qqqs[qc], request.getParameter("func"));

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
	<%
}

%>
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
	    title : '查詢統計'
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
</BODY>
</HTML>
