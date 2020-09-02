<%@ include file="/commons/taglib.jsp"%>
<%@page import="com.intumit.message.MessageUtil"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.io.*"
         import="java.net.*"
         import="java.text.*"
         import="java.util.*" 
         import="java.util.*" 
         import="org.apache.commons.lang.*"
         import="org.apache.solr.client.solrj.*"
         import="org.apache.solr.client.solrj.embedded.*"
         import="org.apache.solr.client.solrj.response.*"
         import="com.intumit.solr.config.*"
         import="com.intumit.solr.SearchManager"
%><%
SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'");
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String dataGroupOne = MessageUtil.getMessage(locale, "data.group.one");
String dataGroupTwo = MessageUtil.getMessage(locale, "data.group.two");
String generalQuery = MessageUtil.getMessage(locale, "general.query");
String notFindData = MessageUtil.getMessage(locale, "not.find.data");
HashMap<String, String> translateMap = new HashMap<String, String>();
{
	translateMap.put("", "(N/A)");
	translateMap.put("1", dataGroupOne);
	translateMap.put("2", dataGroupTwo);
	translateMap.put("default", generalQuery);
	translateMap.put("no_result", notFindData);
	// translateMap.put("", "");
}


%>
<%!
public String translate(String org,HashMap<String, String> translateMap) {
	if (org == null)
		return "";

	if (StringUtils.isNotBlank(ColumnNameMappingFacade.getInstance().getMappingName(org))){
		return ColumnNameMappingFacade.getInstance().getMappingName(org);
	}
	if (translateMap.containsKey(org))
		return translateMap.get(org);
	else
		return org;
}
List<String> getFuncList() {
    SolrQuery query = new SolrQuery();
    query.setFacet(true);
    query.setRows(0);
    query.addFacetField("Func_s");
    query.setQuery("Date_dt:[NOW-6MONTH TO NOW]");
    
    List<String> list = new ArrayList<String>();
    try {
        QueryResponse resp = SearchManager.getServer("core-keyword").query(query);
        FacetField ff = resp.getFacetField("Func_s");
        for (FacetField.Count facetCount: ff.getValues()) {
        	list.add(facetCount.getName());
        }
    }
    catch (Exception e) {
        e.printStackTrace();
    }

	return list;
}
%>
<HTML>
<HEAD>
<TITLE>WiSe - Keyword Statistics</TITLE>
<script>
var POP_CALENDER_IMG_DIR = "<%= request.getContextPath() %>/images/calendar/";
</script>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/PopCalendar.js"></script>
</HEAD>
<BODY>
<%

List<String> funcs = getFuncList();
Calendar now = Calendar.getInstance();
Calendar sevenDaysAgo = Calendar.getInstance();
sevenDaysAgo.add(Calendar.HOUR, -1 * 7 * 24);
%>
<TABLE WIDTH="100%">
<TR>
<TD WIDTH="200" valign="TOP">
	<FORM action="statistics-kw-result.jsp" target="mainFrame">
	<b><bean:message key='hot.keyword.statistics'/></b>
	<TABLE>
	<TR><TD><bean:message key='global.start'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="from" VALUE="<%= sdf.format(sevenDaysAgo.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR><TD><bean:message key='global.end'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="to" VALUE="<%= sdf.format(now.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR>
		<TD><bean:message key='global.space'/></TD>
		<TD>
			<SELECT NAME="gap">
				<OPTION VALUE="quarter">QUARTER</OPTION>
				<OPTION VALUE="month">MONTH</OPTION>
				<OPTION VALUE="week">WEEK</OPTION>
				<OPTION VALUE="day" selected>DAY</OPTION>
				<OPTION VALUE="year">YEAR</OPTION>
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD><bean:message key='global.range'/></TD>
		<TD>
			<SELECT NAME="func">
				<% for (String func: funcs) { %>
					<OPTION VALUE="<%= func %>"><%= translate(func,translateMap) %></OPTION>
				<% } %>
				<!-- 
				<OPTION VALUE="RelatedTopic">相關文章</OPTION>
				-->
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD></TD>
		<TD>
			<INPUT TYPE="SUBMIT">
		</TD>
	</TR>
	</TABLE>
	</FORM>
	<BR>
	<FORM action="statistics-kw-result2.jsp" target="mainFrame">
	<b><bean:message key='query.total.statistics'/></b>
	<TABLE>
	<TR><TD><bean:message key='global.start'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="from" VALUE="<%= sdf.format(sevenDaysAgo.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR><TD><bean:message key='global.end'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="to" VALUE="<%= sdf.format(now.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR>
		<TD><bean:message key='global.space'/></TD>
		<TD>
			<SELECT NAME="gap">
				<OPTION VALUE="quarter">QUARTER</OPTION>
				<OPTION VALUE="month">MONTH</OPTION>
				<OPTION VALUE="week">WEEK</OPTION>
				<OPTION VALUE="day" selected>DAY</OPTION>
				<OPTION VALUE="year">YEAR</OPTION>
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD><bean:message key='global.range'/></TD>
		<TD>
			<SELECT NAME="func">
				<% for (String func: funcs) { %>
					<OPTION VALUE="<%= func %>"><%= translate(func,translateMap) %></OPTION>
				<% } %>
				<!-- 
				<OPTION VALUE="RelatedTopic">相關文章</OPTION>
				-->
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD></TD>
		<TD>
			<INPUT TYPE="SUBMIT">
		</TD>
	</TR>
	</TABLE>
	</FORM>
	<BR>
	<FORM action="statistics-kw-result3.jsp" target="mainFrame">
	<b><bean:message key='click.statistics'/></b>
	<TABLE>
	<TR><TD><bean:message key='global.start'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="from" VALUE="<%= sdf.format(sevenDaysAgo.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR><TD><bean:message key='global.end'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="to" VALUE="<%= sdf.format(now.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR>
		<TD><bean:message key='global.space'/></TD>
		<TD>
			<SELECT NAME="gap">
				<OPTION VALUE="quarter">QUARTER</OPTION>
				<OPTION VALUE="month">MONTH</OPTION>
				<OPTION VALUE="week">WEEK</OPTION>
				<OPTION VALUE="day" selected>DAY</OPTION>
				<OPTION VALUE="year">YEAR</OPTION>
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD><bean:message key='global.range'/></TD>
		<TD>
			<SELECT NAME="func">
				<% for (String func: funcs) { %>
					<OPTION VALUE="<%= func %>"><%= translate(func,translateMap) %></OPTION>
				<% } %>
				<!-- 
				<OPTION VALUE="RelatedTopic">相關文章</OPTION>
				-->
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD></TD>
		<TD>
			<INPUT TYPE="SUBMIT">
		</TD>
	</TR>
	</TABLE>
	</FORM>
	<BR>
	<FORM action="statistics-kw-result4.jsp" target="mainFrame">
	<b><bean:message key='hot.article.statistics'/></b>
	<TABLE>
	<TR><TD><bean:message key='global.start'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="from" VALUE="<%= sdf.format(sevenDaysAgo.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR><TD><bean:message key='global.end'/></TD><TD><INPUT onclick="popUpCalendar(this, this, 'yyyymmdd')" NAME="to" VALUE="<%= sdf.format(now.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR>
		<TD><bean:message key='global.space'/></TD>
		<TD>
			<SELECT NAME="gap">
				<OPTION VALUE="quarter">QUARTER</OPTION>
				<OPTION VALUE="month">MONTH</OPTION>
				<OPTION VALUE="week">WEEK</OPTION>
				<OPTION VALUE="day" selected>DAY</OPTION>
				<OPTION VALUE="year">YEAR</OPTION>
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD><bean:message key='global.range'/></TD>
		<TD>
			<SELECT NAME="func">
				<OPTION VALUE="default"><bean:message key='general.query'/></OPTION>
				<OPTION VALUE="hotlink"><bean:message key='recommend.word'/></OPTION>
				<OPTION VALUE="RelatedTopic"><bean:message key='related.theme'/></OPTION>
				<!-- 
				<OPTION VALUE="RelatedTopic">相關文章</OPTION>
				-->
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD><bean:message key='show.number.statistics'/></TD>
		<TD>
			<input type="text" name="count" value="10" size="10"></input>
		</TD>
	</TR>
	<TR>
		<TD></TD>
		<TD>
			<INPUT TYPE="SUBMIT">
		</TD>
	</TR>
	</TABLE>
	</FORM>
	
</TD>
</TR>
</TABLE>
</BODY>
</HTML>
