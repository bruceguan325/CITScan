<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%!
SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'");
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
Calendar now = Calendar.getInstance();
Calendar sevenDaysAgo = Calendar.getInstance();
sevenDaysAgo.add(Calendar.MONTH, -3);
%>
<TABLE WIDTH="100%">
<TR>
<TD WIDTH="200" valign="TOP">
	<FORM action="statistics-doc-result.jsp" target="mainFrame">
	<b><bean:message key='statistics.doc.menu'/></b>
	<TABLE>
	<TR><TD><bean:message key='global.start'/></TD><TD><INPUT NAME="from" onclick="popUpCalendar(this, this, 'yyyymmdd')" VALUE="<%= sdf.format(sevenDaysAgo.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR><TD><bean:message key='global.end'/></TD><TD><INPUT NAME="to" onclick="popUpCalendar(this, this, 'yyyymmdd')" VALUE="<%= sdf.format(now.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
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
		<TD><bean:message key='global.data.resource'/></TD>
		<TD>
			<SELECT NAME="coreName">
			<% for (int i=0; i < SearchManager.getCoreCount(); i++) { %>
				<OPTION VALUE="core<%= i %>">CORE <%= i %></OPTION>
			<% } %>
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
	<!-- 
	關鍵字曝光統計的啟用，要在 core0 data-config.xml 當中，針對要使用 wiki 斷關鍵字的欄位作類似下面的設定
	<field column="keyword" name="Keyword_ms" wiki="true" sourceColName="t1,b2" limit="10" />
	其中 t1,b2 是 column name，可以多個欄位用逗號分開。
	然後當然也要設定 transformer="com.intumit.solr.WikiKeywordTransformer"
	
	
	<FORM action="statistics-doc-wiki-result.jsp" target="mainFrame">
	<b>關鍵字曝光統計</b>
	<TABLE>
	<TR><TD>起始</TD><TD><INPUT NAME="from" VALUE="<%= sdf.format(sevenDaysAgo.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR><TD>結束</TD><TD><INPUT NAME="to" VALUE="<%= sdf.format(now.getTime()) %>" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR><TD>關連</TD><TD><INPUT NAME="q" VALUE="" TYPE="TEXT" SIZE="10"></TD></TR>
	<TR>
		<TD>統計<BR>間距</TD>
		<TD>
			<SELECT NAME="gap">
				<OPTION VALUE="month" selected>MONTH</OPTION>
				<OPTION VALUE="quarter">QUARTER</OPTION>
				<OPTION VALUE="year">YEAR</OPTION>
				<OPTION VALUE="week">WEEK</OPTION>
				<OPTION VALUE="day">DAY</OPTION>
				<OPTION VALUE="all">無間距</OPTION>
			</SELECT>
		</TD>
	</TR>
	<TR>
		<TD>統計<BR>範圍</TD>
		<TD>
			<SELECT NAME="category">
				<OPTION VALUE="">全部</OPTION>
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
	 -->
</TD>
</TR>
</TABLE>
</BODY>
</HTML>
