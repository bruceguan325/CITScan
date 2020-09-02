<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ page import="javax.servlet.ServletConfig" %>
<%@ page import="javax.servlet.ServletException" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.net.*" %>
<%@ page import="java.text.*" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.commons.io.*" %>
<%@ page import="org.apache.commons.httpclient.*" %>
<%@ page import="org.apache.commons.httpclient.methods.*" %>
<%@ page import="org.apache.commons.httpclient.params.HttpMethodParams" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="org.apache.solr.core.*" %>
<%@ page import="org.apache.solr.servlet.*" %>
<%@ page import="org.apache.solr.client.solrj.*" %>
<%@ page import="org.apache.solr.client.solrj.embedded.*" %>
<%@ page import="org.apache.solr.client.solrj.response.*" %>
<%@ page import="org.apache.solr.common.*" %>
<%@ page import="com.intumit.solr.admin.*" %>
<%@ page import="com.intumit.solr.SearchManager" %>
<%@ page import="com.intumit.solr.dataset.*" %>
<%@ page import="com.intumit.solr.util.*" %>
<%@ page import="com.intumit.systemconfig.*" %>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.intumit.quartz.ScheduleUtils"%>
<%@page import="com.intumit.quartz.Job"%>

<%@page import="org.dom4j.*"%>
<%@page import="org.apache.commons.httpclient.methods.GetMethod"%>
<%@page import="org.apache.commons.httpclient.HttpClient"%>
<%@page import="org.apache.commons.httpclient.auth.AuthScope"%>
<%@page import="org.apache.commons.httpclient.UsernamePasswordCredentials"%>
<%@page import="org.apache.solr.client.solrj.SolrQuery"%>

<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
	return;
}
%>
<HTML>
<HEAD>
<TITLE>資料組</TITLE>
<jsp:include page="header-qa.jsp"></jsp:include>
<script src="<%=request.getContextPath()%><%= WiSeEnv.getAdminContextPath() %>/js/jquery.aciPlugin.min.js"></script>
<script src="<%=request.getContextPath()%><%= WiSeEnv.getAdminContextPath() %>/js/jquery.aciSortable.min.js"></script>
<link href="<%=request.getContextPath()%>/assets/stylesheets/jquery_ui/jquery-ui-1.10.0.custom.css" rel="stylesheet">
<script>
function doSubmit() {
	document.myForm.cmd.value="add";
	document.myForm.submit();
}
function doDelete(theForm) {
	theForm.cmd.value = "delete";
	theForm.submit();
}
function doUpdate(theForm) {
	theForm.cmd.value = "update";
	theForm.submit();
}

function deleteDataSet(url){
	if(confirm('確定要刪除此資料組？')){
	document.dataSetListForm.action=url;
	document.dataSetListForm.method='POST';
	document.dataSetListForm.submit();
	top.leftFrame.location.reload();
	}else{
	return;
	}
}
</script>
<style>
td-form {
	width: 50%;
}

td-form input,textarea {
	width: 100%;
}
.ui-dialog-titlebar-close {
    padding:0 !important;
}

.ui-dialog-titlebar-close:after {
    content: '';
    width: 20px;
    height: 20px;
    display: inline-block;
    background-image: url(<%=request.getContextPath()%>/assets/images/jquery_ui/ui-icons_222222_256x240.png);
    background-position: -96px -128px;
    background-repeat: no-repeat;
}
.dlgBtns{
margin-left:45%
}
.dlgBtns button{
width:10%;
margin-left:2%;
}
</style>
</HEAD>
<BODY>
<jsp:include page="navbar-qa.jsp"></jsp:include>
<%
WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();
String action = request.getParameter("action");
String idStr = request.getParameter("id");

if (idStr != null) {
	int acl = AdminGroupFacade.getInstance().getDataSetViewAcl(session, Integer.parseInt(idStr));
	if ((acl & GroupDataSet.O4) == 0) return; // 無權限
}

if ("create".equalsIgnoreCase(action)) {
	int coreMax = cfg.getCoreMax();
	List<DataSet> list = DataSetFacade.getInstance().listAll();
	if(list.size()<=coreMax){
		DataSetFacade.getInstance().save("New DataSet");
	%>
		<%= "New DataSet" %> created!
	<%
	}else{
		System.out.println("DataSet max core is "+coreMax);
	}
}
else if ("edit".equalsIgnoreCase(action) || "duplicate".equalsIgnoreCase(action)) {
	DataSet ds = DataSetFacade.getInstance().get(new Integer(idStr));
	%>
<script type="text/javascript">
    function dialogSubmit(){
      var choosedFns = $('.fields-choosed ul li');
      var inputName = $("#fieldsDialog").data("input");
      var fields =[];
      for(i=0;i<choosedFns.length;i++){
       var data = $(choosedFns[i]).attr('data-fn');
       fields.push(data);
      }
      $("input[name="+inputName+"]").val(fields.join(","));
      $("#fieldsDialog").dialog("close");
     }
     function dialogClose(){
      $("#fieldsDialog").dialog("close");
     }
     function openDialog(btn){
       var inputName = $(btn).parent("td").children("input[type=text]").attr("name");
       var coreName = "<%= ds.getCoreName() %>";
       var $fDialog = $("#fieldsDialog");
       $fDialog.load("_choose-fields.jsp?coreName="+coreName+"&form=myForm&field=facets" ,function(){
        $fDialog.data("input",inputName).dialog('open');
       })
      }
     $(document).ready(function(){
       $("#fieldsDialog").dialog({
        autoOpen: false,
           title: 'Choose',
           width:800,
           height:800,
           dialogClass:"fieldsDlg",
           position: {
        	   my: "center bottom",
        	   at: "center top",
        	   of: $("#submitbutton"),
        	   within: $(".content")
        	  },
           create:function(){
            var buttons = "<hr><div class='dlgBtns'><button id='dlgSubmit' class='btn-primary'>確定</button><button id='dlgClose' class='btn-primary'>關閉</button><div>"
            $(".fieldsDlg").append(buttons);
            $("#dlgSubmit").click(dialogSubmit);
            $("#dlgClose").click(dialogClose);
           }
       });
     })
</script>
	編輯資料組
	<form action="qaDatasetAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<table class="table table-bordered">
    <div id="fieldsDialog"></div>
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td>ID</td>
		<td class="td-form">
			<input type="hidden" name="id" value="<%= ds.getId() %>">
			<%= ds.getId() %>
		</td>
		<td>&nbsp;</td>
	</tr>
	<%
	}
	%>
	<tr>
		<td>順序</td>
		<td class="td-form">
			<input type="text" size="40" name="dsOrder" value="<%= WiSeUtils.output(ds.getDsOrder(), "", "", "") %>">
		</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td>是否啟用</td>
		<td class="td-form">
			TRUE：<input type="radio" name="dsEnable" value="true" <%= Boolean.TRUE.equals(ds.getEnable()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="dsEnable" value="false" <%= Boolean.FALSE.equals(ds.getEnable()) ? "checked" : "" %>>
		</td>
		<td>(Ex.true 則啟用此資料組)</td>
	</tr>
	<tr>
		<td>是否顯示</td>
		<td class="td-form">
			TRUE：<input type="radio" name="dsVisible" value="true" <%= Boolean.TRUE.equals(ds.getVisible()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="dsVisible" value="false" <%= Boolean.FALSE.equals(ds.getVisible()) ? "checked" : "" %>>
		</td>
		<td>(Ex.本選項只有啟用的資料組才有效用，隱藏的資料組將不會出現跨頻道搜尋結果當中)</td>
	</tr>
	<tr>
		<td>名稱</td>
		<td class="td-form">
			<input type="text" size="40" name="name" value="<%= ds.getName() %>">
		</td>
		<td>(Ex. 中文書)</td>
	</tr>
	<tr>
		<td>選定來源 </td>
		<td>
			<SELECT NAME="coreName">
			<%
			for (int i=0; i < SearchManager.getCoreCount(); i++) {
				String cc = "core" + i;
			%>
				<OPTION VALUE="<%= cc %>" <%= cc.equals(ds.getCoreName()) ? "selected" : "" %>>CORE <%= i %></OPTION>
			<%
			}
			%>
			</SELECT>
		</td>
		<td>(Ex. coreN)</td>
	</tr>
	<tr>
		<td>過濾條件 </td>
		<td>
			<input type="text" size="40" name="filters" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFilters(), "", "", "")) %>">
		</td>
		<td>(Ex. TopCategoryName_s:中文書)</td>
	</tr>
	<tr>
		<td>顯示筆數</td>
		<td>
		<select name="displayRows">
		  <option value="5">5</option>
		  <option value="10" selected>10</option>
		  <option value="15">15</option>
		  <option value="20">20</option>
		  <option value="25">25</option>
		  <option value="50">50</option>
		  <option value="100">100</option>
		  <option value="--">== for Image Template ==</option>
		  <option value="9">9</option>
		  <option value="12">12</option>
		  <option value="15">15</option>
		  <option value="30">30</option>
		  <option value="60">60</option>
		</select>
		</td>
		<td>
		(Ex. 10)
		</td>
	</tr>
	<tr>
		<td>多維度欄位</td>
		<td>
			<input type="text" size="40" name="facets" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFacets(), "", "", "")) %>">
			<input type="button" onclick="openDialog(this)" data-choose-fields="facets" value="Choose">
		</td>
		<td>
		(Ex. TopCategoryName_s,SecondCategoryName_s)
		</td>
	</tr>
	<tr>
		<td>範圍型多維度</td>
		<td>
		<%--
		com.intumit.solr.dataset.DataSet.FacetQuery[] fqs = ds.getFQ();
		out.println(fqs[0].getOffset());
		out.println(fqs[0].facetQuries());
		--%>
		<textarea cols="80" rows="10" name="facetQueries"><%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFacetQueries(), "", "", "")) %></textarea>
		</td>
		<td>
		Ex. <br>
		{&lt;Price_i:1&gt;<br>
		&lt;小於 $999&gt;Price_i:[* TO 999]<br>
		&lt;大於$1,000&gt;Price_i:[1000 TO *]<br>
		}<br>
		{&lt;Date_dt:2&gt;<br>
		&lt;今日&gt;Date_dt:[NOW/DAY TO NOW/DAY+1DAY]<BR>
		&lt;近三日&gt;Date_dt:[NOW/DAY+1DAY-3DAY TO NOW/DAY+1DAY]<BR>
		&lt;近一個月&gt;Date_dt:[NOW/DAY+1DAY-1MONTH TO NOW/DAY+1DAY]<BR>
		&lt;近三個月&gt;Date_dt:[NOW/DAY+1DAY-3MONTH TO NOW/DAY+1DAY]<BR>
		&lt;近半年&gt;Date_dt:[NOW/DAY+1DAY-6MONTH TO NOW/DAY+1DAY]<BR>
		&lt;超過半年&gt;Date_dt:[* TO NOW/DAY+1DAY-6MONTH]<BR>
		}<br>
		</td>
	</tr>
	<tr>
		<td>排序欄位</td>
		<td>
			<input type="text" size="40" name="sortables" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getSortables(), "", "", "")) %>">
			<input type="button" onclick="openDialog(this)" data-choose-fields="sortables" value="Choose">
		</td>
		<td>
		(Ex. 預設已有相關性及日期)
		</td>
	</tr>
	<tr>
		<td>預設排序</td>
		<td>
			<input type="text" size="40" name="defaultSort" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getDefaultSort(), "", "", "")) %>">
		</td>
		<td>
		(Ex. 預設以相關度排序, 可填入 Date_dt desc 或者 XXXXX asc 等語法）
		</td>
	</tr>
	<tr>
		<td>欄位權重</td>
		<td>
		<textarea cols="80" rows="10" name="fieldWeight"><%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldWeight(), "", "", "")) %></textarea>
		</td>
		<td>(Ex. Name_t^3.0 Description_mt^0.2)</td>
	</tr>
	<tr>
		<td>查詢特殊欄位加權</td>
		<td>
		<textarea cols="80" rows="10" name="queryBoost"><%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getQueryBoost(), "", "", "")) %></textarea>
		</td>
		<td>(Ex. product(ord(pub_date_dt),map(map(Amount,0,1,0.1),2,50000,1))^0.012)</td>
	</tr>
	<tr>
		<td>查詢特殊欄位加權2</td>
		<td>
		<textarea cols="80" rows="10" name="queryBoostMultiply"><%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getQueryBoostMultiply(), "", "", "")) %></textarea>
		</td>
		<td>(Ex. product(ord(pub_date_dt),map(map(Amount,0,1,0.1),2,50000,1))^0.012)</td>
	</tr>
	<tr>
		<td>高亮顯示欄位</td>
		<td>
			<input type="text" size="40" name="fieldHighlight" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldHighlight(), "", "", "")) %>">
			<input type="button" onclick="openDialog(this)" data-choose-fields="fieldHighlight" value="Choose">
		</td>
		<td>(Ex. Name_t,Description_mt)</td>
	</tr>
	<tr>
		<td>關鍵字欄位</td>
		<td>
			<input type="text" size="40" name="fieldWiki" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldWiki(), "", "", "")) %>">
			<input type="button" onclick="openDialog(this)" data-choose-fields="fieldWiki" value="Choose">
		</td>
		<td>(Ex. Description_mt)</td>
	</tr>
	<tr>
		<td>關鍵字數量</td>
		<td>
			<input type="text" size="40" name="fieldWikiCount" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldWikiCount(), "", "", "")) %>">
		</td>
		<td>(Ex. 5)</td>
	</tr>
	<tr>
		<td>相關文章欄位</td>
		<td>
			<input type="text" size="40" name="fieldMlt" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMlt(), "", "", "")) %>">
			<input type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>(Ex. Name_t,Description_mt)</td>
	</tr>
	<tr>
		<td>相關文章筆數</td>
		<td>
			<input type="text" size="40" name="fieldMltCount" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltCount(), "", "", "")) %>">
		</td>
		<td>(Ex. 5)</td>
	</tr>
	<tr>
		<td>開啟RSS頻道</td>
		<td class="td-form">
			TRUE：<input type="radio" name="rss" value="true" <%= Boolean.TRUE.equals(ds.getRss()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="rss" value="false" <%= Boolean.FALSE.equals(ds.getRss()) ? "checked" : "" %>>
		</td>
		<td>(Ex.true 為開啟RSS功能)</td>
	</tr>
	<tr>
		<td>模糊搜尋</td>
		<td class="td-form">
			TRUE：<input type="radio" name="needFuzzySearch" value="true" <%= Boolean.TRUE.equals(ds.getFuzzySearch()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="needFuzzySearch" value="false" <%= Boolean.FALSE.equals(ds.getFuzzySearch()) ? "checked" : "" %>>
		</td>
		<td>(Ex.true 為開啟模糊搜尋功能)</td>
	</tr>
	<tr>
		<td>模糊搜尋欄位</td>
		<td>
			<input type="text" size="40" name="fuzzyField" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFuzzyField(), "", "", "")) %>">
			<input type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>(如果開啟模糊搜尋功能請一定要設定該資訊)</td>
	</tr>
	<tr>
		<td>模糊欄位權重</td>
		<td>
			<input type="text" size="40" name="fuzzyfieldWeight" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFuzzyFieldWeight(), "", "", "")) %>">
		</td>
		<td>(Ex. Name_t^3.0 Description_mt^0.2)</td>
	</tr>
	<tr>
		<td>進階搜尋時間欄位</td>
		<td>
			<input type="text" size="40" name="advancedDateField" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getAdvancedDateField(), "", "", "")) %>">
			<input type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>(Ex. Date_dt)</td>
	</tr>
	<tr>
		<td></td>
		<td>
			<input class="btn btn-primary" type="submit" value="儲存">
		</td>
	</tr>
	</table>
	</form>
	<%
}
else if ("save".equalsIgnoreCase(action)) {
	if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.U) != 0) {
		DataSet ds = new DataSet();
		if (idStr != null)
			ds.setId(new Integer(idStr));
		if (StringUtils.isNotEmpty(request.getParameter("dsOrder")))
			ds.setDsOrder(new Long(request.getParameter("dsOrder")));

		String[] filterChecks = request.getParameterValues("filterChecks");
		StringBuffer sb = new StringBuffer();

		for (int fff=0; filterChecks != null && fff < filterChecks.length; fff++) {
			String cond = URLDecoder.decode(filterChecks[fff], "UTF-8");
			if (fff > 0)
				sb.append(" OR ");
			sb.append("(" + cond + ")");
		}
		ds.setName(request.getParameter("name"));
		ds.setEnable(!"false".equalsIgnoreCase(request.getParameter("dsEnable")));
		ds.setVisible(!"false".equalsIgnoreCase(request.getParameter("dsVisible")));
		ds.setCoreName(request.getParameter("coreName"));
		ds.setFilters(request.getParameter("filters"));
		if (sb.length() > 0)
			ds.setFilters(sb.toString());
		ds.setFacets(request.getParameter("facets"));
		ds.setSortables(request.getParameter("sortables"));
		ds.setDefaultSort(request.getParameter("defaultSort"));
		ds.setFacetQueries(request.getParameter("facetQueries"));

		//ds.setFieldMltName1(request.getParameter("fieldMltName1"));
		//ds.setFieldMltName2(request.getParameter("fieldMltName2"));
		//ds.setFieldMltName3(request.getParameter("fieldMltName3"));
		//ds.setFieldMltValue1(request.getParameter("fieldMltValue1"));
		//ds.setFieldMltValue2(request.getParameter("fieldMltValue2"));
		//ds.setFieldMltValue3(request.getParameter("fieldMltValue3"));
		//System.out.println("__" + ds.getFieldWeight());
		//ds.setFieldWeight(ds.processMlt());
		ds.setFieldWeight(request.getParameter("fieldWeight"));
		ds.setQueryBoost(request.getParameter("queryBoost"));
		ds.setQueryBoostMultiply(request.getParameter("queryBoostMultiply"));

		ds.setFieldHighlight(request.getParameter("fieldHighlight"));
		ds.setFieldWiki(request.getParameter("fieldWiki"));
		ds.setFieldWikiCount(new Long(request.getParameter("fieldWikiCount")));
		ds.setFieldMlt(request.getParameter("fieldMlt"));
		ds.setFieldMltCount(new Long(request.getParameter("fieldMltCount")));
		ds.setRss("true".equalsIgnoreCase(request.getParameter("rss")));
		ds.setFuzzySearch("true".equalsIgnoreCase(request.getParameter("needFuzzySearch")));
		ds.setFuzzyField(request.getParameter("fuzzyField"));
		ds.setFuzzyFieldWeight(request.getParameter("fuzzyfieldWeight"));
		ds.setAdvancedDateField(request.getParameter("advancedDateField"));
		ds.setDisplayRows(request.getParameter("displayRows"));

		DataSetFacade.getInstance().saveOrUpdate(ds);
		%>
		資料組(<%= ds.getName() %>)已儲存!
		<%
	}
}
else if ("delete".equalsIgnoreCase(action)) {
	if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.D) != 0) {
		DataSetFacade.getInstance().delete(new Integer(idStr));
		%>
		資料組(<%= idStr %>)已移除!
		<%
	}
}
%>
<form action="" method="post" name="dataSetListForm">
<TABLE width="100%" class="table table-striped">
	<THEAD>
	<TR>
		<TH valign="top">編號</TH>
		<TH valign="top">順序</TH>
		<TH valign="top">啟用</TH>
		<TH valign="top">顯示</TH>
		<TH valign="top">資料組名稱</TH>
		<TH valign="top">選定來源</TH>　
		<TH valign="top">RSS</TH>
		<TH valign="top">操作</TH>
	</TR>
	</THEAD>
<%
List<DataSet> list = DataSetFacade.getInstance().listAll();
for (int i=0; i < list.size(); i++) {
	DataSet ds = list.get(i);
	int id = ds.getId();
    int thisAcl = AdminGroupFacade.getInstance().getDataSetViewAcl(session, id);
    if ((thisAcl & GroupDataSet.O4) == 0) continue; // 無權限
%>
	<TR>
		<TD align="center" valign="top" width="30"><%= ds.getId() %></TD>
		<TD align="center" valign="top" width="30"><%= ds.getDsOrder() %></TD>
		<TD align="center" valign="top" width="45"><%= Boolean.FALSE.equals(ds.getEnable()) ? "<span style='color: gray' class='glyphicon glyphicon-ban-circle'></span>&nbsp;" : "<span class='glyphicon glyphicon-check'></span>&nbsp;" %></TD>
		<TD align="center" valign="top" width="45"><%= Boolean.FALSE.equals(ds.getVisible()) ? "<span style='color: gray' class='glyphicon glyphicon-ban-circle'></span>&nbsp;" : "<span class='glyphicon glyphicon-check'></span>&nbsp;" %></TD>
		<TD align="center" valign="top"><%= ds.getName() %></TD>
		<TD align="center" valign="top" width="100"><%= ds.getCoreName() %></TD>
		<TD align="center" valign="top" width="100"><%= ds.getRss() %></TD>
		<TD align="left" valign="top" >
			<div class="btn-group">
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.U) != 0) {
			%>
			<a class="btn btn-primary" href="qaDatasetAdmin.jsp?action=EDIT&id=<%= id %>"><span class="glyphicon glyphicon-edit"></span>&nbsp;修改</a>
			<% } %>
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.D) != 0) {
			%>
			<a class="btn btn-danger" href="#" onclick="deleteDataSet('qaDatasetAdmin.jsp?action=DELETE&id=<%= id %>')"><span class="glyphicon glyphicon-remove"></span>&nbsp;刪除</a>
			<% } %>
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.C) != 0) {
			%>
			<a class="btn btn-danger" onclick="return confirm('確定要複製此資料組？');" href="qaDatasetAdmin.jsp?action=DUPLICATE&id=<%= id %>"><span class="glyphicon glyphicon-plus"></span>&nbsp;複製</a>
			<% } %>
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.R) != 0) {
			%>
			<a class="btn btn-success" href="elevator-list.jsp?d=<%= id %>"><span class="glyphicon glyphicon-sort"></span>&nbsp;調整搜尋權重</a>
			<% } %>
			</div>
		</TD>
	</TR>
<%
}
%>
</TABLE>
</form>
<%

int coreMax=Integer.parseInt(StringUtils.defaultString(System.getProperty("searchresult_coremax"), "10"));
if(list.size()<=coreMax) {
%>
<A class="btn btn-default" href="<%=request.getContextPath() %>/wiseadm/qaDatasetAdmin.jsp?action=create">建立新的資料組</A>
<% } %>
</BODY>
</HTML>