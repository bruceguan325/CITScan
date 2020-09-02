<%@ include file="/commons/taglib.jsp"%>
<%@ page
		contentType="text/html;charset=UTF-8"
        pageEncoding="UTF-8"
        language="java"

		import="java.util.HashMap"
		import="java.io.*"
		import="java.sql.*"
		import="java.util.*"
		import="com.intumit.solr.*"
		import="com.intumit.solr.dataimport.*"
		import="com.intumit.solr.util.VelocityManager"
		import="org.apache.velocity.app.*"
		import="org.apache.velocity.VelocityContext"
		import="org.apache.commons.lang.StringUtils"
		import="org.apache.commons.io.FileUtils"
		import="org.apache.commons.lang.StringEscapeUtils"
		import="java.util.regex.Pattern"
		import="java.util.regex.Matcher" %><%!

boolean transToBoolean(String value) {
    if (value!=null) {
        return Boolean.valueOf(value);
    }
    else {
        return false;
    }
}
boolean isTrim(String type) {
    String lType = type.toLowerCase();
	if (lType.indexOf("char") != -1 || lType.indexOf("text") != -1 || lType.indexOf("clob") != -1)
		return true;
	else
	    return false;
}
boolean isClob(String type) {
    String lType = type.toLowerCase();
	if (lType.indexOf("text") != -1 || lType.indexOf("clob") != -1)
		return true;
	else
	    return false;
}
String guessSuffix(String type) {
	String lType = type.toLowerCase();
	if (lType.indexOf("char") != -1 || lType.indexOf("text") != -1 || lType.indexOf("clob") != -1)
		return "_t";
	if (lType.indexOf("int") != -1 || lType.indexOf("decimal") != -1)
		return "_i";
	if (lType.indexOf("float") != -1)
		return "_f";
	if (lType.indexOf("double") != -1)
		return "_d";
	if (lType.indexOf("date") != -1 || lType.indexOf("time") != -1)
		return "_dt";

	return "_ig";
}

String getWithDefault(HttpServletRequest req, String pName, String def) {
	String step = null;
	if (req.getParameter(pName) != null) {
		step = req.getParameter(pName);
	}
	else {
		step = def;
	}

	return step;
}
String getAndSet(HttpServletRequest req, HttpSession sess, String pName, String def) {
	String step = null;
	if (req.getParameter(pName) != null) {
		step = req.getParameter(pName);
	}
	else if (sess.getAttribute(pName) != null) {
		step = (String)sess.getAttribute(pName);
	}
	else {
		step = def;
	}

	sess.setAttribute(pName, step);
	return step;
}
String[] getAndSetValues(HttpServletRequest req, HttpSession sess, String pName, String[] defaultArr) {

	String[] step = null;
	if (req.getParameterValues(pName) != null) {
		step = req.getParameterValues(pName);
	}
	else if (sess.getAttribute(pName) != null) {
		step = (String[])sess.getAttribute(pName);
	}

	sess.setAttribute(pName, step);

	if (step == null)
		return defaultArr;
	return step;
}
Connection getConnection(String driver, String url, String account, String password) throws Exception {
    Class.forName(driver);
    return DriverManager.getConnection(url, account, password);
}

String generateXml(HttpServletRequest request, DataConfig dataCfg) {
    StringBuffer fieldsBuf = new StringBuffer();

    DataField pkf = null;

    if (dataCfg.getFields()!=null) {
	    for (DataField f: dataCfg.getFields()) {
	    	String suffix = guessSuffix(f.getDbColumnType());
	    	if (f.isIgnored()) continue;

	    	if (f.isPrimaryKey()) {
	    		pkf = f;
		    	fieldsBuf.append("\t");
		    	fieldsBuf.append(f.toConfigElement("id"));
		    	fieldsBuf.append("\n");
	    	}
	    	if (f.isMainTitle() && !f.getDbColumnName().equals("Name_t")) {
		    	fieldsBuf.append("\t");
		    	fieldsBuf.append(f.toConfigElement("Name_t"));
		    	fieldsBuf.append("\n");
	    	}
	    	if (f.isMainText() && !f.getDbColumnName().equals("Description_mt")) {
		    	fieldsBuf.append("\t");
		    	fieldsBuf.append(f.toConfigElement("Description_mt"));
		    	fieldsBuf.append("\n");
	    	}
	    	if (f.isMainDate() && !f.getDbColumnName().equals("Date_dt")) {
		    	fieldsBuf.append("\t");
		    	fieldsBuf.append(f.toConfigElement("Date_dt"));
		    	fieldsBuf.append("\n");
	    	}

	    	fieldsBuf.append("\t");
	    	fieldsBuf.append(f.toConfigElement(f.getDbColumnName().endsWith(suffix) ? f.getDbColumnName() : (f.getDbColumnName() + suffix)));
	    	fieldsBuf.append("\n");

	    	if (f.isFacetable() && suffix.equals("_t")) {
		    	fieldsBuf.append("\t");
		    	fieldsBuf.append(f.toConfigElement(f.getDbColumnName() + "_s"));
		    	fieldsBuf.append("\n");
	    	}
	    }
    }


    String deltaQuery = "SELECT " + pkf.getDbColumnName() + " AS id FROM " + dataCfg.getDeltaQueryFrom() + " WHERE " + dataCfg.getDeltaQueryWhere() + " >= ${dih.last_index_time}";
    String deleteQuery = "SELECT " + pkf.getDbColumnName() + " AS id FROM " + dataCfg.getDeleteQueryFrom() + " WHERE " + dataCfg.getDeleteQueryWhere() + " >= ${dih.last_index_time}";

    String deltaImportSql = dataCfg.getSqlQuery();
    if (StringUtils.indexOfIgnoreCase(deltaImportSql, "where") != -1) {
    	int pos = StringUtils.indexOfIgnoreCase(deltaImportSql, "where");
    	String injectSql = " " + pkf.getDbColumnName() + "='${dih.delta.id}' AND ";

    	deltaImportSql = deltaImportSql.substring(0, pos) + injectSql + deltaImportSql.substring(pos);
    }
    else {
    	String injectSql = " WHERE " + pkf.getDbColumnName() + "='${dih.delta.id}' ";
    	deltaImportSql += injectSql;
    }


    StringWriter w = new StringWriter();
    VelocityEngine engine = com.intumit.solr.util.WiSeUtils.getVelocityEngine(request);

    VelocityContext entitymap = new VelocityContext();
    entitymap.put("name", dataCfg.getEntityName());
    entitymap.put("transformer", dataCfg.getTransformers());
    entitymap.put("sql", dataCfg.getSqlQuery());
    entitymap.put("body", fieldsBuf.toString());
    entitymap.put("deltaSql", deltaQuery);
    entitymap.put("deleteSql", deleteQuery);
    entitymap.put("deltaImportSql", deltaImportSql);

    engine.mergeTemplate("entity.template", "UTF-8", entitymap, w);
    String entity = w.toString();

   // String entity = VelocityManager.getInstance().getTemplateString("/entity.template", entitymap);

    w = new StringWriter();
    VelocityContext datamap = new VelocityContext();
    datamap.put("driver", dataCfg.getJdbcDriver());
    datamap.put("url", dataCfg.getJdbcUri());
    datamap.put("account", dataCfg.getJdbcUser());
    datamap.put("password", dataCfg.getJdbcPass());
    datamap.put("body", entity);
    //String body = VelocityManager.getInstance().getTemplateString("/data-config.template", datamap);
    engine.mergeTemplate("data-config.template", "UTF-8", datamap, w);
    String body = w.toString();

    return body;
}
%><%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}
%>
<%
String coreName = request.getParameter("coreName");

DataConfig dataCfg = DataConfigFacade.get(coreName);

if (dataCfg == null) {
	dataCfg = (DataConfig)session.getAttribute("dataConfig");

	if (dataCfg == null) {
		dataCfg = new DataConfig("core0");
		dataCfg.setEntityName("product");
	    session.setAttribute("dataConfig", dataCfg);
	}
}
else {
    session.setAttribute("dataConfig", dataCfg);
}

dataCfg.setJdbcDriver(StringUtils.defaultString(request.getParameter("driver"), dataCfg.getJdbcDriver()));
dataCfg.setJdbcUri(StringUtils.defaultString(request.getParameter("jdbcStr"), dataCfg.getJdbcUri()));
dataCfg.setJdbcUser(StringUtils.defaultString(request.getParameter("jdbcUser"), dataCfg.getJdbcUser()));
dataCfg.setJdbcPass(StringUtils.defaultString(request.getParameter("jdbcPass"), dataCfg.getJdbcPass()));
dataCfg.setSqlQuery(getWithDefault(request, "rootSql", dataCfg.getSqlQuery()));

String step = getAndSet(request, session, "step", "1");

dataCfg.setTargetCoreName(StringUtils.defaultString(request.getParameter("coreName"), dataCfg.getTargetCoreName()));
%>
<html>
<head>
<title><%= dataCfg.getTargetCoreName() %> - Data Configuration</title>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/styles/bootstrap-responsive.min.css" type="text/css" rel="stylesheet"/>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap.min.js" type="text/javascript"></script>
<script>
function toPrevStep() {
	document.myform.step.value = document.myform.step.value-2;
	document.myform.submit();
}

function changeJdbcDriverAndStr(dbType) {
	if (dbType == 'mysql') {
		document.myform.driver.value = 'com.mysql.jdbc.Driver';
		document.myform.jdbcStr.value = 'jdbc:mysql://localhost:3306/forum?useUnicode=true&zeroDateTimeBehavior=convertToNull&characterEncoding=UTF8&characterSetResults=UTF8';
	}
	else if (dbType == 'sqlserver') {
		document.myform.driver.value = 'com.inet.tds.TdsDriver';
		document.myform.jdbcStr.value = 'jdbc:inetdae7:localhost:1433?database=dbname';
	}
	else if (dbType == 'oracle') {
		document.myform.driver.value = 'com.inet.ora.OraDriver';
		document.myform.jdbcStr.value = 'jdbc:inetora:localhost:1521:ORCL';
	}
}
function addChild() {
    $('#myform').attr('action','child.jsp');
    $('#myform').submit();
}
function backToOne() {
    document.myform.step.value = 1;
    document.myform.submit();
}
</script>

</head>

<body>
<H2><bean:message key="step"/><%= step %> (<%= dataCfg.getTargetCoreName() %>)</H2>
<div class="progress progress-striped active" style="width: 80%;">
	<div class="bar" style="width: <%= Math.round((Double.parseDouble(step) / 7)*100) %>%"></div>
</div>
<font color='blue'><bean:message key="database.set.attention"/></font>
<form name="myform" id="myform" action="gen-dataconfig-db.jsp" METHOD="POST">
<%
if ("1".equals(step)) {
    session.setAttribute("childBody", "");
%>
	<table class="table table-striped">
	<tr>
		<td>Choose DB Type</td>
		<td>
			<a href="#" class="btn btn-default" onclick="changeJdbcDriverAndStr('mysql');">MySQL</a>&nbsp;
			<a href="#" class="btn btn-default" onclick="changeJdbcDriverAndStr('sqlserver');">SQLServer</a>&nbsp;
			<a href="#" class="btn btn-default" onclick="changeJdbcDriverAndStr('oracle');">Oracle</a>&nbsp;
		</td>
	</tr>
	<tr>
		<td>Driver</td>
		<td><input type="text" size="30" name="driver" value="<%= StringUtils.trimToEmpty(dataCfg.getJdbcDriver()) %>"></td>
	</tr>
	<tr>
		<td>JDBC String</td>
		<td><input type="text" size="100"  style="width: 600px;" name="jdbcStr" value="<%= StringUtils.trimToEmpty(dataCfg.getJdbcUri()) %>"></td>
	</tr>
	<tr>
		<td>Username</td>
		<td><input type="text" name="jdbcUser" value="<%= StringUtils.trimToEmpty(dataCfg.getJdbcUser()) %>"></td>
	</tr>
	<tr>
		<td>Password</td>
		<td><input type="text" name="jdbcPass" value="<%= StringUtils.trimToEmpty(dataCfg.getJdbcPass()) %>"></td>
	</tr>
	<tr>
		<td>
		<input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>"><BR>
		<input class="btn btn-success" type="submit" value="<bean:message key='next.step'/>">
		</td>
		<td>
		</td>
	</tr>
	</table>

<%
}
else if ("2".equals(step)) {
    Connection conn = null;
    try {
        conn = getConnection(dataCfg.getJdbcDriver(), dataCfg.getJdbcUri(), dataCfg.getJdbcUser(), dataCfg.getJdbcPass());
        out.println("<font color='blue'><bean:message:key='dbset.test.success'/></font><br>");
%>
        <p><bean:message key="please.enter.sql.syntax"/></p>
		<textarea cols="100" rows="30" style="width:80%;" name="rootSql"><%= StringUtils.trimToEmpty(dataCfg.getSqlQuery()) %></textarea>
		<input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>"><BR>
		<input class="btn btn-danger" type="button" value="<bean:message key='last.step'/>" onclick="toPrevStep();">
		<input class="btn btn-success" type="submit" value="<bean:message key='next.step'/>">
<%
    }
    catch(Throwable t) {
        out.println("<font color='red'><bean:message key='dbset.test.failed'/></font><br>");
        t.printStackTrace();
%>
    <input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>"><BR>
    <input class="btn btn-danger" type="button" value="<bean:message key='last.step'/>" onclick="toPrevStep();">
<%
    }
    finally {
        if (conn!=null) {
            conn.close();
        }
    }
}
else if ("3".equals(step)) {
	Connection conn = null;
	try {
        conn = getConnection(dataCfg.getJdbcDriver(), dataCfg.getJdbcUri(), dataCfg.getJdbcUser(), dataCfg.getJdbcPass());
		Statement stat = conn.createStatement();

		// 只要抓一筆資料就好
		stat.setFetchSize(1);
		stat.setMaxRows(1);
		ResultSet rs = stat.executeQuery(dataCfg.getSqlQuery());
	    out.println("<font color='blue'><bean:message key='sql.syntax'/></font><br>");

		ResultSetMetaData meta = rs.getMetaData();
%>
    <bean:message key="dbset.attention1"/><BR>
    <bean:message key="dbset.attention2"/><BR>
    <bean:message key="dbset.attention3"/><BR>
    <bean:message key="dbset.attention4"/><BR>
	Entity Name: <input type="text" name="entityName" value="<%= dataCfg.getEntityName() %>"/><br>

	Primary Key: <select name="pk">
		<%
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			String fName = meta.getColumnLabel(i);
			DataField f = dataCfg.findField(fName);
			out.println("<option value='" + fName + "' " + (f!=null&&f.isPrimaryKey()?"selected":"") + ">" + fName + "</option>");
		}
		%>
	</select>
	Main Title: <select name="mainTitle">
		<%
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			String fName = meta.getColumnLabel(i);
			String fType = meta.getColumnTypeName(i);
			String fSuffix = fName.equalsIgnoreCase("id") ? "" : guessSuffix(fType);
			DataField f = dataCfg.findField(fName);
			out.println("<option value='" + fName + "' " + (f!=null&&f.isMainTitle()?"selected":"") + ">" + fName + "</option>");
		}
		%>
	</select>
	Main Text: <select name="mainText">
		<%
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			String fName = meta.getColumnLabel(i);
			String fType = meta.getColumnTypeName(i);
			String fSuffix = fName.equalsIgnoreCase("id") ? "" : guessSuffix(fType);
			DataField f = dataCfg.findField(fName);
			out.println("<option value='" + fName + "' " + (f!=null&&f.isMainText()?"selected":"") + ">" + fName + "</option>");
		}
		%>
	</select>
	Main Date: <select name="mainDate">
		<%
		for (int i = 1; i <= meta.getColumnCount(); i++) {
			String fName = meta.getColumnLabel(i);
			String fType = meta.getColumnTypeName(i);
			String fSuffix = fName.equalsIgnoreCase("id") ? "" : guessSuffix(fType);

			if (fSuffix.equals("_dt")) {
				DataField f = dataCfg.findField(fName);
				out.println("<option value='" + fName + "' " + (f!=null&&f.isMainDate()?"selected":"") + ">" + fName + "</option>");
			}
		}
		%>
	</select>

	<table border="1" class="table table-striped">
	    <tr>
	        <th>Column</th>
	        <th>Ignored</th>
	        <th>Facetable</th>
	        <th>Indexable</th>
	        <th>Trim</th>
	        <th>Html</th>
	        <th>Clob</th>
	        <th>Comments</th>
	    </tr>

<%
	if (dataCfg.getFields() == null) {
		for (int i = 1; i <= meta.getColumnCount(); i++) {
		    out.println("<tr>");
			String fName = meta.getColumnLabel(i);
			String fType = meta.getColumnTypeName(i);
			String fSuffix = fName.equalsIgnoreCase("id") ? "" : guessSuffix(fType);

			out.println("<td><input type='hidden' name='column' value='" + fName + "'/>"
						+ "<input type='hidden' name='columnType' value='" + fType + "'/>"
						+ fName + " <span class='badge'>" + fType + "</span>"
						+ "</td>");
			out.println("<td><input type='checkbox' name='ignored' value='"+ fName+"' " + (fSuffix.equals("_ig")?"checked":"") + " ></td>");
			out.println("<td><input type='checkbox' name='indexable' value='"+ fName+"' "+ ((isTrim(fType))?"checked":"") +" ></td>");
			out.println("<td><input type='checkbox' name='facetable' value='"+ fName+"' "+ ((isTrim(fType))?"checked":"") +" ></td>");
			out.println("<td><input type='checkbox' name='trim' value='"+ fName+"' "+ ((isTrim(fType))?"checked":"") +" ></td>");
			out.println("<td><input type='checkbox' name='html' value='"+fName+"' "+ ((isClob(fType))?"checked":"") +" ></td>");
			out.println("<td><input type='checkbox' name='clob' value='"+fName+"' "+ ((isClob(fType))?"checked":"") +" ></td>");
			out.println("<td><input type='text' name='comment' ></td>");
			out.println("</tr>");
		}
	}
	else {
		Set<String> configuredColumnNames = new HashSet<String>();
		Set<String> metaColumnNames = new HashSet<String>();

		for (int i = 1; i <= meta.getColumnCount(); i++) {
			String fName = meta.getColumnLabel(i);
			metaColumnNames.add(fName);
		}

		for (DataField f: dataCfg.getFields()) {
		    out.println("<tr>");
		    String fName = f.getDbColumnName();
		    String fType = f.getDbColumnType();
		    configuredColumnNames.add(fName);

		    if (!metaColumnNames.contains(fName))
		    	continue;

			out.println("<td><input type='hidden' name='column' value='" + fName + "'/>"
						+ "<input type='hidden' name='columnType' value='" + fType + "'/>"
						+ fName + " <span class='badge'>" + fType + "</span>"
						+ "</td>");
			out.println("<td><input type='checkbox' name='ignored' value='"+ fName+"' "+ (f.isIgnored()?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='indexable' value='"+ fName+"' "+ (f.isSearchable()?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='facetable' value='"+ fName+"' "+ (f.isFacetable()?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='trim' value='"+ fName+"' "+ (f.hasTransformer(Transformer.Trim)?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='html' value='"+fName+"' "+ (f.hasTransformer(Transformer.Html)?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='clob' value='"+fName+"' "+ (f.hasTransformer(Transformer.Clob)?"checked":"") +" /></td>");
			out.println("<td><input type='text' name='comment' ></td>");

			out.println("</tr>");
		}
		for (int i = 1; i <= meta.getColumnCount(); i++) {
		    out.println("<tr>");
			String fName = meta.getColumnLabel(i);
			if (configuredColumnNames.contains(fName)) {
				continue;
			}

			String fType = meta.getColumnTypeName(i);
			String fSuffix = fName.equalsIgnoreCase("id") ? "" : guessSuffix(fType);

			out.println("<td><input type='hidden' name='column' value='" + fName + "'/>"
						+ "<input type='hidden' name='columnType' value='" + fType + "'/>"
						+ fName + " <span class='badge'>" + fType + "</span>"
						+ "</td>");
			out.println("<td><input type='checkbox' name='ignored' value='"+ fName+"' " + (fSuffix.equals("_ig")?"checked":"") + " ></td>");
			out.println("<td><input type='checkbox' name='indexable' value='"+ fName+"' "+ ((isTrim(fType))?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='facetable' value='"+ fName+"' "+ ((isTrim(fType))?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='trim' value='"+ fName+"' "+ ((isTrim(fType))?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='html' value='"+fName+"' "+ ((isClob(fType))?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='clob' value='"+fName+"' "+ ((isClob(fType))?"checked":"") +" /></td>");
			out.println("<td><input type='text' name='comment' /></td>");
			out.println("</tr>");
		}
	}
%>

	</table>

		<input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>"><BR>
		<input type="button" value="<bean:message key='last.step'/>" onclick="toPrevStep();">
		<input type="submit" value="<bean:message key='next.step'/>"/>
<%
	}
	catch(Throwable e) {
	    out.println("<font color='red'><bean:message key='sql.syntax.exception'/></font><br>");
	    out.println(e.getMessage());
%>
    <input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>">
    <input class="btn btn-danger" type="button" value="<bean:message key='last.step'/>" onclick="toPrevStep();">
<%
	}
	finally {
	    if (conn!=null) {
		    conn.close();
	    }
	}
}
else if ("4".equals(step)) {
    String entityName = request.getParameter("entityName");
	DataField pkf = null;

    if (entityName != null) {
	    String pk = getWithDefault(request, "pk", "id");
	    String mainTitle = getWithDefault(request, "mainTitle", "id");
	    String mainText = getWithDefault(request, "mainText", "Name");
	    String mainDate = getWithDefault(request, "mainDate", "Date");

	    String[] emptyArr = new String[0];
	    String[] column = getAndSetValues(request, session, "column", emptyArr);
	    String[] columnType = getAndSetValues(request, session, "columnType", emptyArr);
	    String[] searchable = getAndSetValues(request, session, "indexable", emptyArr);
	    String[] facetable = getAndSetValues(request, session, "facetable", emptyArr);
	    String[] ignored = getAndSetValues(request, session, "ignored", emptyArr);
	    String[] trim = getAndSetValues(request, session, "trim", emptyArr);
	    String[] html = getAndSetValues(request, session, "html", emptyArr);
	    String[] clob = getAndSetValues(request, session, "clob", emptyArr);

	    dataCfg.setEntityName(entityName);

	    //default
	    List<Class> transformerList = new ArrayList<Class>();
	    transformerList.add(TrimTransformer.class);
	    List<DataField> dataFields = new ArrayList<DataField>();

	    if (column!=null && column.length>0) {
		    for (int i=0;i<column.length;i++) {
		    	String colName = column[i];
		    	DataField f = new DataField();

		    	f.setDbColumnName( colName );
		    	f.setDbColumnType( columnType[i] );
		    	f.setPrimaryKey( colName.equals(pk) );
		    	f.setMainTitle( colName.equals(mainTitle) );
		    	f.setMainText( colName.equals(mainText) );
		    	f.setMainDate( colName.equals(mainDate) );

		    	f.setIgnored( Arrays.asList(ignored).contains( colName ) );
		    	f.setSearchable( Arrays.asList(searchable).contains( colName ) );
		    	f.setFacetable( Arrays.asList(facetable).contains( colName ) );

		    	String transformers = "";

		    	if (Arrays.asList(trim).contains( colName )) {
		    		if (transformers.length() > 0)
		    			transformers += ",";
		    		transformers += Transformer.Trim.name();
		    	}
		    	if (Arrays.asList(html).contains( colName )) {
		    		if (transformers.length() > 0)
		    			transformers += ",";
		    		transformers += Transformer.Html.name();
		    	}
		    	if (Arrays.asList(clob).contains( colName )) {
		    		if (transformers.length() > 0)
		    			transformers += ",";
		    		transformers += Transformer.Clob.name();
		    	}

		    	f.setAppliedTransformers(transformers);

		    	if (f.isPrimaryKey())
		    		pkf = f;
		    	dataFields.add(f);
		    }

		    dataCfg.setFields(dataFields);
	    }

	    String transformer = "";
	    if (clob.length > 0) {
	    	if (transformer.length() > 0)
	    		transformer += ",";
	    	transformer += Transformer.Clob.getClazzName();
	    }
	    if (html.length > 0) {
	    	if (transformer.length() > 0)
	    		transformer += ",";
	    	transformer += Transformer.Html.getClazzName();
	    }
	    if (trim.length > 0) {
	    	if (transformer.length() > 0)
	    		transformer += ",";
	    	transformer += Transformer.Trim.getClazzName();
	    }
	    dataCfg.setTransformers(transformer);
    }
    else {

    	for (DataField f: dataCfg.getFields()) {
    		if (f.isPrimaryKey()) {
    			pkf = f;
    			break;
    		}
    	}
    }
%>
   <h3><bean:message key="incremental.index.query"/></h3>
   <strong>SELECT <%= pkf.getDbColumnName() %> FROM <input type="text" name="deltaQueryFrom" value="<%= StringUtils.trimToEmpty(dataCfg.getDeltaQueryFrom()) %>">
   WHERE <input type="text" name="deltaQueryWhere" value="<%= StringUtils.trimToEmpty(dataCfg.getDeltaQueryWhere()) %>"> >= {{LAST INDEX TIME}}</strong><BR>
   <div class="alert" style="width:80%;">
   <p><bean:message key="incremental.index.query.attention1"/>{{LAST INDEX TIME}} <bean:message key="incremental.index.query.attention2"/></p>
   </div>
   <h3><bean:message key="data.delete.query"/></h3>
   <strong>SELECT <%= pkf.getDbColumnName() %> FROM <input type="text" name="deleteQueryFrom" value="<%= StringUtils.trimToEmpty(dataCfg.getDeleteQueryFrom()) %>">
   WHERE <input type="text" name="deleteQueryWhere" value="<%= StringUtils.trimToEmpty(dataCfg.getDeleteQueryWhere()) %>"> >= {{LAST INDEX TIME}}</strong><BR>
   <div class="alert alert-danger" style="width:80%;">
   <p><bean:message key="data.delete.query.attention1"/>{{LAST INDEX TIME}} <bean:message key="data.delete.query.attention2"/></p>
   </div>

<%--
   if (childList.size() > 0) {
      out.println("請勾選要用到的child entity<br>");
   }

   for(int i=0;i<childList.size();i++) {
       String eachChild = childList.get(i);
%>
   <input name="childs" type="checkbox" value="<%=i%>"/>
   <textarea name="childBodys" rows="5" cols="100" style="overflow: scroll;" ><%= eachChild %></textarea><br>
<%
   }
   <input type="button" value="加入childEntity" onclick="addChild();">
--%>
   <input type="hidden" name="childStep" value="1"><BR>
   <input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>"><BR>
   <input type="button" value="<bean:message key='last.step'/>" onclick="toPrevStep();">
   <input type="submit" value="<bean:message key='next.step'/>">
<%
}
else if("5".equals(step)) {
	String deltaQueryFrom = StringUtils.defaultString(request.getParameter("deltaQueryFrom"), dataCfg.getDeltaQueryFrom());
	String deltaQueryWhere = StringUtils.defaultString(request.getParameter("deltaQueryWhere"), dataCfg.getDeltaQueryWhere());
	String deleteQueryFrom = StringUtils.defaultString(request.getParameter("deleteQueryFrom"), dataCfg.getDeleteQueryFrom());
	String deleteQueryWhere = StringUtils.defaultString(request.getParameter("deleteQueryWhere"), dataCfg.getDeleteQueryWhere());

	dataCfg.setDeltaQueryFrom(deltaQueryFrom);
	dataCfg.setDeltaQueryWhere(deltaQueryWhere);
	dataCfg.setDeleteQueryFrom(deleteQueryFrom);
	dataCfg.setDeleteQueryWhere(deleteQueryWhere);

    String body = generateXml(request, dataCfg);
    session.setAttribute("dataConfigXml", body);
%>
   <textarea name="main" class="main" rows="20" cols="100" rows="30" style="width:80%; overflow: scroll;" readOnly="true"><%= body %></textarea><br>


   <input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>"><BR>
   <input type="button" value="<bean:message key='last.step'/>" onclick="toPrevStep();">
   <input type="submit" value="<bean:message key='next.step'/>">
<%
}
else if("6".equals(step)) {
    DataConfigFacade.save(dataCfg);
%>
   <bean:message key='storage'/><br>
    <input type="hidden" name="step" value="<%= Integer.parseInt(step)+1 %>"><BR>
    <input type="button" value="<bean:message key='last.step'/>" onclick="toPrevStep();">
    <input type="submit" value="<bean:message key='output.to.file'/>">
<%
	}
else if("7".equals(step)) {
    String filePath = SearchManager.getLocalCores().getCore(dataCfg.getTargetCoreName()).getDataDir() + "../conf/data-config.xml";
    String total = (String)session.getAttribute("dataConfigXml");

    Pattern p = Pattern.compile("url=\"(.*)\" user");
    Matcher m = p.matcher(total);
    while(m.find()) {
    	String g = m.group();
    	g = g.replaceAll("url=\"","");
    	g = g.replaceAll("\" user","");
    	total = StringUtils.replace(total, g, StringEscapeUtils.escapeHtml(g));
    }

    p = Pattern.compile("query=\"(.*)\" batchSize");
    m = p.matcher(total);
    while(m.find()) {
    	String g = m.group();
    	g = g.replaceAll("query=\"","");
    	g = g.replaceAll("\" batchSize","");
    	total = StringUtils.replace(total, g, StringEscapeUtils.escapeHtml(g));
    }

    File write = new File(filePath);
    FileUtils.writeStringToFile(write, total);
%>
    <bean:message key='complete'/>
    <input type="hidden" name="step" value="1"><BR>
    <input type="button" value="<bean:message key='step.back.one'/>" onclick="backToOne();"/>
<%
}
%>
</form>
<script>
$(document).ready(function() {
	$('input[name=ignored]:checked').parents('tr').addClass('error');
});

$('input[name=ignored]').live('change', function() {
	if ($(this).is(':checked')) {
		$(this).parents('tr').addClass('error');
	}
	else {
		$(this).parents('tr').removeClass('error');
	}
});
</script>
</body>
</html>
