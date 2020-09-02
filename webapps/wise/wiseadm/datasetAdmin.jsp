<%@ include file="/commons/taglib.jsp"%>
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
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
if (admGrp == null || admGrp.getDataSetAdminCURD() == 0) {
	return;
}
%>
<!DOCTYPE html>
<HTML>
<HEAD>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<TITLE>WiSe - Helper Links</TITLE>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%=request.getContextPath()%>/styles/bs3/bootstrap-theme.min.css" type="text/css" rel="stylesheet">
<link href="<%=request.getContextPath()%>/assets/stylesheets/jquery_ui/jquery-ui-1.10.0.custom.css" rel="stylesheet">
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js"></script>
<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
<script src="<%=request.getContextPath()%>/script/jquery-ui/jquery-ui.min.js"></script>
<!--[if lt IE 9]>s
  <script src="<%=request.getContextPath()%>/script/html5shiv.js"></script>
  <script src="<%=request.getContextPath()%>/script/respond.min.js"></script>
<![endif]-->
<script src="<%=request.getContextPath()%><%= WiSeEnv.getAdminContextPath() %>/js/jquery.aciPlugin.min.js"></script>
<script src="<%=request.getContextPath()%><%= WiSeEnv.getAdminContextPath() %>/js/jquery.aciSortable.min.js"></script>
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
	if(confirm('<bean:message key='sure.del.data.group'/>')){
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
.dlgBtns{
margin-left:45%
}
.dlgBtns button{
margin-left:2%;
}
</style>
</HEAD>
<BODY>
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
    	      height:600,
    	      dialogClass:"fieldsDlg",
    	      create:function(){
    	       var buttons = "<hr><div class='dlgBtns'><button id='dlgSubmit' class='btn btn-primary'>確定</button><button id='dlgClose' class='btn btn-primary'>關閉</button><div>"
    	       $(".fieldsDlg").append(buttons);
    	       $("#dlgSubmit").click(dialogSubmit);
    	       $("#dlgClose").click(dialogClose);
    	       $("span.ui-button-text").remove();
    	      }
    	  });
    	})
</script>
	<bean:message key='edit.data.group'/>
	<form action="datasetAdmin.jsp" name="myForm" id="myForm">
	<input type="hidden" name="action" value="save">
	<table class="table table-bordered">
    <div id="fieldsDialog"></div>
	<%
	if ("edit".equalsIgnoreCase(action)) {
	%>
	<tr>
		<td><bean:message key='global.id'/></td>
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
		<td><bean:message key='global.order'/></td>
		<td class="td-form">
			<input type="text" size="40" name="dsOrder" value="<%= WiSeUtils.output(ds.getDsOrder(), "", "", "") %>">
		</td>
		<td>&nbsp;</td>
	</tr>
	<tr>
		<td><bean:message key='is.enabled'/></td>
		<td class="td-form">
			TRUE：<input type="radio" name="dsEnable" value="true" <%= Boolean.TRUE.equals(ds.getEnable()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="dsEnable" value="false" <%= Boolean.FALSE.equals(ds.getEnable()) ? "checked" : "" %>>
		</td>
		<td><bean:message key='is.enabled.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='is.show'/></td>
		<td class="td-form">
			TRUE：<input type="radio" name="dsVisible" value="true" <%= Boolean.TRUE.equals(ds.getVisible()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="dsVisible" value="false" <%= Boolean.FALSE.equals(ds.getVisible()) ? "checked" : "" %>>
		</td>
		<td><bean:message key='is.show.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='global.name'/></td>
		<td class="td-form">
			<input type="text" size="40" name="name" value="<%= ds.getName() %>">
		</td>
		<td><bean:message key='global.name.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='select.resource'/></td>
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
		<td><bean:message key='select.resource.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='filtration.condition'/></td>
		<td>
			<input type="text" size="40" name="filters" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFilters(), "", "", "")) %>">

			<!--把所有文章的facet組成樹，可以再搜尋-->
			<!--begin-->
			<div id="facet_tree1" >
			<ul style="width: 400">
				<%--
					List orgFilterList = Arrays.asList(WiSeUtils.output(ds.getFilters(), "", "", "").split(" OR "));
					HttpClient httpclient = new HttpClient();
					//指定第一層節點欄位
					String field1 = "TopCategoryName_s";
					//指定第二層節點欄位
					String field2 = "SecondCategoryName_s";
					String fieldNames = field1+","+field2;

					//認證
					//httpclient.getState().setCredentials(AuthScope.ANY,
					//		new UsernamePasswordCredentials("root", "intumitdemo"));

					//取出xml
					GetMethod get = new GetMethod(
							"http://"+request.getServerName()+":"+request.getServerPort()+"/wise<%= WiSeEnv.getAdminContextPath() %>/core0/select?"
									+ "q=*:*&rows=0&facet=on&facet.tree=" + fieldNames);
					httpclient.executeMethod(get);
					String xml = get.getResponseBodyAsString();

					//組成ul>a li>a，讓jsTree轉成樹
					try {
						org.dom4j.Document doc = DocumentHelper.parseText(xml);
						// 這個xpath的意思是,獲取text='系統管理'的一個Item下的所有Item的節點
						String xpath = "//lst[@name='" + fieldNames + "']/child::*";

						SolrQuery new_query2=new SolrQuery();//query.getCopy();
						//用dismax會有錯
						new_query2.setQueryType("standard");
						new_query2.setFilterQueries(null);

						for (Iterator i = doc.selectNodes(xpath).iterator(); i
								.hasNext();) {
							org.dom4j.Element elt = (org.dom4j.Element) i.next();

							Attribute attr = elt.attribute("name");
							String parent_name=attr.getValue();

							StringBuilder sb = new StringBuilder();
							int parentcount = 0;
							for (Iterator i2 = elt.elementIterator(); i2.hasNext();) {
								org.dom4j.Element child = (org.dom4j.Element) i2.next();
								String ctext = child.getText();
								String name = child.attributeValue("name");

								//過濾0的facet
								if (!"0".equals(ctext)) {
									//算出父節點的數量
									parentcount += Integer.valueOf(ctext);
									String cond = field1 + ":\"" + parent_name + "\" AND " + field2 + ":\"" + name + "\"";

									//放id，再查詢連結，id的編碼要避開jquery的運算子
									sb.append("<li style=\"width:200\"><input type=\"checkbox\" name=\"filterChecks\" value=\""
											+ URLEncoder.encode(cond, "UTF-8") + "\"" + (orgFilterList.contains("(" + cond + ")") ? "checked" : "") + ">");
									sb.append(name + "(" + ctext + ")");
									sb.append("</li>");
								}
							}
							//放id，再查詢連結

							String condParent = field1 + ":\"" + parent_name + "\"";
							out.println("<li style=\"width:200\"><input type=\"checkbox\" name=\"filterChecks\" value=\""
									+ URLEncoder.encode(condParent, "UTF-8") + "\"" + (orgFilterList.contains("(" + condParent + ")") ? "checked" : "") + ">");
							out.println(parent_name + "(" + parentcount + ")");
							out.println("<ul>");
							out.println(sb.toString());
							out.println("</ul>");
							out.println("</li>");
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				--%>
				</ul>
			</div>
			<!--end-->
			&nbsp;
			<!--把搜尋結果facet組成樹，可以再搜尋-->
			<!--div必須套 style="clear: both;" 以免被上面那顆數的float屬性給蓋住-->
	        <div style="clear: both;width: 180;padding: 0">&nbsp;</div>
			&nbsp;
		</td>
		<td><bean:message key='filtration.condition.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='show.data.number'/></td>
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
		<bean:message key='show.data.number.ex'/>
		</td>
	</tr>
	<tr>
		<td><bean:message key='multi.dimensional.field'/></td>
		<td>
			<input type="text" size="40" name="facets" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFacets(), "", "", "")) %>">
			<input class="btn btn-primary" type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>
		(Ex. TopCategoryName_s,SecondCategoryName_s)
		</td>
	</tr>
	<tr>
		<td><bean:message key='facet.queries'/></td>
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
		&lt;<bean:message key='less.than'/>$999&gt;Price_i:[* TO 999]<br>
		&lt;<bean:message key='more.than'/>$1,000&gt;Price_i:[1000 TO *]<br>
		}<br>
		{&lt;Date_dt:2&gt;<br>
		&lt;<bean:message key='global.today'/>&gt;Date_dt:[NOW/DAY TO NOW/DAY+1DAY]<BR>
		&lt;<bean:message key='nearly.three.days'/>&gt;Date_dt:[NOW/DAY+1DAY-3DAY TO NOW/DAY+1DAY]<BR>
		&lt;<bean:message key='nearly.one.months'/>&gt;Date_dt:[NOW/DAY+1DAY-1MONTH TO NOW/DAY+1DAY]<BR>
		&lt;<bean:message key='nearly.three.months'/>&gt;Date_dt:[NOW/DAY+1DAY-3MONTH TO NOW/DAY+1DAY]<BR>
		&lt;<bean:message key='nearly.half.a.year'/>&gt;Date_dt:[NOW/DAY+1DAY-6MONTH TO NOW/DAY+1DAY]<BR>
		&lt;<bean:message key='over.half.a.year'/>&gt;Date_dt:[* TO NOW/DAY+1DAY-6MONTH]<BR>
		}<br>
        {&lt;-CategorySkill_s:3&gt;<br>
        &lt;-testRobot&gt;CategorySkill_s:* AND -CategorySkill_s:testRobot<br>
        &lt;-testWeb&gt;CategorySkill_s:* AND -CategorySkill_s:testWeb<br>
        &lt;-testKm&gt;CategorySkill_s:* AND -CategorySkill_s:testKm<br>
        }<br>
		</td>
	</tr>
	<tr>
		<td><bean:message key='name.filed'/></td>
		<td>
			<input type="text" size="40" name="nameField" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getNameField(), "", "", "")) %>">
		</td>
		<td>
		<bean:message key='name.filed.ex'/>
		</td>
	</tr>
	<tr>
		<td><bean:message key='body.fields'/></td>
		<td>
			<input type="text" size="40" name="bodyFields" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getBodyFields(), "", "", "")) %>">
		</td>
		<td>
		<bean:message key='body.fields.ex'/>
		</td>
	</tr>
	<tr>
		<td><bean:message key='sort'/><bean:message key='column'/></td>
		<td>
			<input type="text" size="40" name="sortables" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getSortables(), "", "", "")) %>">
			<input class="btn btn-primary" type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>
		<bean:message key='sort.column.ex'/>
		</td>
	</tr>
	<tr>
		<td><bean:message key='default.sort'/></td>
		<td>
			<input type="text" size="40" name="defaultSort" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getDefaultSort(), "", "", "")) %>">
		</td>
		<td>
		<bean:message key='default.sort.ex'/>
		</td>
	</tr>
	<tr>
		<td><bean:message key='field.weight'/></td>
		<td>
		<%--
		<input type="text" size="27" name="fieldMltName1" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltName1(), "", "", "")) %>">
		<input type="text" size="10" name="fieldMltValue1" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltValue1(), "", "", "")) %>">
		<input type="button" onclick="window.showModalDialog('_choose-fields.jsp?coreName=<%= ds.getCoreName() %>&form=myForm&field=fieldMltName1', window);" value="Choose">
		<br>
		<input type="text" size="27" name="fieldMltName2" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltName2(), "", "", "")) %>">
		<input type="text" size="10" name="fieldMltValue2" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltValue2(), "", "", "")) %>">
		<input type="button" onclick="window.showModalDialog('_choose-fields.jsp?coreName=<%= ds.getCoreName() %>&form=myForm&field=fieldMltName2', window);" value="Choose">
		<br>
		<input type="text" size="27" name="fieldMltName3" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltName3(), "", "", "")) %>">
		<input type="text" size="10" name="fieldMltValue3" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltValue3(), "", "", "")) %>">
		<input type="button" onclick="window.showModalDialog('_choose-fields.jsp?coreName=<%= ds.getCoreName() %>&form=myForm&field=fieldMltName3', window);" value="Choose">
		<br/>
		--%>
		<textarea cols="80" rows="10" name="fieldWeight"><%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldWeight(), "", "", "")) %></textarea>
		</td>
		<td>(Ex. Name_t^3.0 Description_mt^0.2)</td>
	</tr>
	<tr>
		<td><bean:message key='query.boost'/></td>
		<td>
		<textarea cols="80" rows="10" name="queryBoost"><%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getQueryBoost(), "", "", "")) %></textarea>
		</td>
		<td>(Ex. product(ord(pub_date_dt),map(map(Amount,0,1,0.1),2,50000,1))^0.012)</td>
	</tr>
	<tr>
		<td><bean:message key='query.boost.multiply'/></td>
		<td>
		<textarea cols="80" rows="10" name="queryBoostMultiply"><%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getQueryBoostMultiply(), "", "", "")) %></textarea>
		</td>
		<td>(Ex. product(ord(pub_date_dt),map(map(Amount,0,1,0.1),2,50000,1))^0.012)</td>
	</tr>
	<tr>
		<td><bean:message key='field.highlight'/></td>
		<td>
			<input type="text" size="40" name="fieldHighlight" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldHighlight(), "", "", "")) %>">
			<input class="btn btn-primary" type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>(Ex. Name_t,Description_mt)</td>
	</tr>
	<tr>
		<td><bean:message key='field.wiki'/></td>
		<td>
			<input type="text" size="40" name="fieldWiki" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldWiki(), "", "", "")) %>">
			<input class="btn btn-primary" type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>(Ex. Description_mt)</td>
	</tr>
	<tr>
		<td><bean:message key='field.wiki.count'/></td>
		<td>
			<input type="text" size="40" name="fieldWikiCount" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldWikiCount(), "", "", "")) %>">
		</td>
		<td>(Ex. 5)</td>
	</tr>
	<tr>
		<td><bean:message key='field.mlt'/></td>
		<td>
			<input type="text" size="40" name="fieldMlt" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMlt(), "", "", "")) %>">
			<input class="btn btn-primary" type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>(Ex. Name_t,Description_mt)</td>
	</tr>
	<tr>
		<td><bean:message key='field.mlt.count'/></td>
		<td>
			<input type="text" size="40" name="fieldMltCount" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFieldMltCount(), "", "", "")) %>">
		</td>
		<td>(Ex. 5)</td>
	</tr>
	<tr>
		<td><bean:message key='field.rss'/></td>
		<td class="td-form">
			TRUE：<input type="radio" name="rss" value="true" <%= Boolean.TRUE.equals(ds.getRss()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="rss" value="false" <%= Boolean.FALSE.equals(ds.getRss()) ? "checked" : "" %>>
		</td>
		<td><bean:message key='field.rss.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='need.fuzzy.search'/></td>
		<td class="td-form">
			TRUE：<input type="radio" name="needFuzzySearch" value="true" <%= Boolean.TRUE.equals(ds.getFuzzySearch()) ? "checked" : "" %>><BR>
			FALSE：<input type="radio" name="needFuzzySearch" value="false" <%= Boolean.FALSE.equals(ds.getFuzzySearch()) ? "checked" : "" %>>
		</td>
		<td><bean:message key='need.fuzzy.search.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='fuzzy.field'/></td>
		<td>
			<input type="text" size="40" name="fuzzyField" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFuzzyField(), "", "", "")) %>">
			<input class="btn btn-primary" type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td><bean:message key='fuzzy.field.ex'/></td>
	</tr>
	<tr>
		<td><bean:message key='fuzzy.field.weight'/></td>
		<td>
			<input type="text" size="40" name="fuzzyfieldWeight" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getFuzzyFieldWeight(), "", "", "")) %>">
		</td>
		<td>(Ex. Name_t^3.0 Description_mt^0.2)</td>
	</tr>
	<tr>
		<td><bean:message key='advanced.date.field'/></td>
		<td>
			<input type="text" size="40" name="advancedDateField" value="<%= StringEscapeUtils.escapeHtml(WiSeUtils.output(ds.getAdvancedDateField(), "", "", "")) %>">
			<input class="btn btn-primary" type="button" onclick="openDialog(this)" value="Choose">
		</td>
		<td>(Ex. Date_dt)</td>
	</tr>
	<tr>
		<td></td>
		<td>
			<input class="btn btn-primary" type="submit" value="<bean:message key='submit'/>">
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
		ds.setNameField(request.getParameter("nameField"));
		ds.setBodyFields(request.getParameter("bodyFields"));
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
		<bean:message key='data.group'/>(<%= idStr %>)<bean:message key='already.remove'/>
		<%
	}
}
%>
<form action="" method="post" name="dataSetListForm">
<TABLE width="100%" class="table table-striped">
	<THEAD>
	<TR>
		<TH valign="top"><bean:message key='num'/></TH>
		<TH valign="top"><bean:message key='global.order'/></TH>
		<TH valign="top"><bean:message key='global.able'/></TH>
		<TH valign="top"><bean:message key='global.show'/></TH>
		<TH valign="top"><bean:message key='data.group'/><bean:message key='global.name'/></TH>
		<TH valign="top"><bean:message key='select.resource'/></TH>　
		<TH valign="top">RSS</TH>
		<TH valign="top"><bean:message key='operation'/></TH>
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
			<a class="btn btn-primary" href="datasetAdmin.jsp?action=EDIT&id=<%= id %>"><span class="glyphicon glyphicon-edit"></span>&nbsp;<bean:message key='modify'/></a>
			<% } %>
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.D) != 0) {
			%>
			<a class="btn btn-danger" href="#" onclick="deleteDataSet('datasetAdmin.jsp?action=DELETE&id=<%= id %>')"><span class="glyphicon glyphicon-remove"></span>&nbsp;<bean:message key='delete'/></a>
			<% } %>
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.C) != 0) {
			%>
			<a class="btn btn-danger" onclick="return confirm('<bean:message key='sure.copy.data.group'/>');" href="datasetAdmin.jsp?action=DUPLICATE&id=<%= id %>"><span class="glyphicon glyphicon-plus"></span>&nbsp;<bean:message key='copy'/></a>
			<% } %>
			<%
			if ((AdminGroupFacade.getInstance().getFromSession(session).getDataSetAdminCURD() & AdminGroup.R) != 0) {
			%>
			<a class="btn btn-success" href="elevator-list.jsp?d=<%= id %>"><span class="glyphicon glyphicon-sort"></span>&nbsp;<bean:message key='adjust.search.weight'/></a>
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
<A class="btn btn-default" href="<%=request.getContextPath() %><%= WiSeEnv.getAdminContextPath() %>/datasetAdmin.jsp?action=create"><bean:message key='create.new.data.group'/></A>
<% } %>
</BODY>
</HTML>
