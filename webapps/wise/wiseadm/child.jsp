<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@page import="java.util.HashMap"%>
<%@page import="java.io.File"%>
<%@page import="java.sql.*"%>
<%@page import="java.util.List"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.intumit.solr.*"%>
<%@page import="com.intumit.solr.util.VelocityManager"%>
<%@page import="org.apache.velocity.VelocityContext"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.regex.Matcher"%>
<%@page import="java.util.regex.Pattern"%>
<%!
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
	if (lType.indexOf("char") != -1)
		return "_s";
	if (lType.indexOf("text") != -1 || lType.indexOf("clob") != -1)
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
String[] getAndSetValues(HttpServletRequest req, HttpSession sess, String pName) {

	String[] step = null;
	if (req.getParameterValues(pName) != null) {
		step = req.getParameterValues(pName);
	}
	else if (sess.getAttribute(pName) != null) {
		step = (String[])sess.getAttribute(pName);
	}

	sess.setAttribute(pName, step);
	return step;
}
Connection getConnection(String driver, String url, String account, String password) throws Exception {
    Class.forName(driver);
    return DriverManager.getConnection(url, account, password);
}
%>
<%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
if (admGrp == null || admGrp.getIndexAdminCURD() == 0) {
	return;
}
%>
<%


String testSql =
    "select content from archive_content WHERE no=${Product.id}";

String step = getAndSet(request, session, "childStep", "1");
String driver = getAndSet(request, session, "driver", "com.mysql.jdbc.Driver");
String jdbcStr = getAndSet(request, session, "jdbcStr", "jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=UTF8&characterSetResults=UTF8");
String jdbcUser = getAndSet(request, session, "jdbcUser", "root");
String jdbcPass = getAndSet(request, session, "jdbcPass", "root");
String rootSql = getAndSet(request, session, "childRootSql", testSql);
String entityName = getAndSet(request, session, "childEntityName", "ChildProduct");
%>

<html>
<head>
<title>Data Configuration</title>
<script language="JavaScript" src="<%= request.getContextPath() %>/script/jquery-latest.js" ></script>
<script>
function toPrevStep() {
	document.myform.childStep.value = document.myform.childStep.value-2;
	if (document.myform.childStep.value == 0) {
	    document.myform.action = "genDataConfig.jsp";
	}
	document.myform.submit();
}

function backToMain() {
    $('#myform').attr('action','genDataConfig.jsp');
    $('#myform').submit();
}
</script>

</head>

<body>
<H2>Child Entity步驟<%= step %></H2>
<form name="myform" id="myform" action="child.jsp" METHOD="POST">

<%
if ("1".equals(step)) {
%>
   Child Entity Name: <input type="text" name="childEntityName" value="<%= entityName %>"/><br>
         請輸入 SQL 語法：<font color='red'>(p.s.所有${XXX.XX}參數在測試連線時會以'0'代替)</font><BR>
		<textarea cols="100" rows="10" name="childRootSql"><%= rootSql %></textarea><br>
		<input type="hidden" name="childStep" value="<%= Integer.parseInt(step)+1 %>"><BR>
		<input type="hidden" name="step" value="4"><BR>
		<input type="button" value="上一步" onclick="toPrevStep();">
		<input type="submit" value="下一步">
<%
}
else if ("2".equals(step)) {
	Connection conn = null;
	try {
        conn = getConnection(driver, jdbcStr, jdbcUser, jdbcPass);
		Statement stat = conn.createStatement();

		String tSql = "";

		Pattern pn = Pattern.compile("\\$\\{.*\\}");
		Matcher m = pn.matcher(rootSql);
		while(m.find()) {
		    String a = m.group();
		    tSql = StringUtils.replace(rootSql, a, "'0'");
		}
		ResultSet rs = stat.executeQuery(tSql);
	    out.println("<font color='blue'>Sql語法正常</font><br>");

		ResultSetMetaData meta = rs.getMetaData();
%>
	<table border="1">
	    <tr>
	        <td>Column</td>
	        <td>Index name</td>
	        <td>Trim</td>
	        <td>Timestamp</td>
	        <td>Html</td>
	        <td>Clob</td>
	    </tr>

<%
		for (int i = 1; i <= meta.getColumnCount(); i++) {
		    out.println("<tr>");
			String fName = meta.getColumnLabel(i);
			String fType = meta.getColumnTypeName(i);
			String fSuffix = fName.equalsIgnoreCase("id") ? "" : guessSuffix(fType);

			out.println("<td><input type='hidden' name='childcolumn' value='" + fName + "'/>" + fName + "</td>");
			out.println("<td><input type='text' name='childindex' value='" + fName + fSuffix + "'/></td>");
			out.println("<td><input type='checkbox' name='childtrim' value='"+ fName+"' "+ ((isTrim(fType))?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='childtimestamp' value='"+fName+"' /></td>");
			out.println("<td><input type='checkbox' name='childhtml' value='"+fName+"' "+ ((isClob(fType))?"checked":"") +" /></td>");
			out.println("<td><input type='checkbox' name='childclob' value='"+fName+"' "+ ((isClob(fType))?"checked":"") +" /></td>");
			out.println("</tr>");
		}
%>

	</table>

		<input type="hidden" name="childStep" value="<%= Integer.parseInt(step)+1 %>"><BR>
		<input type="button" value="上一步" onclick="toPrevStep();">
		<input type="submit" value="下一步">
<%
	}
	catch(Throwable e) {
	    out.println("<font color='red'>Sql語法異常</font><br>");
	    out.println(e.getMessage());
	}
	finally {
	    if (conn!=null) {
		    conn.close();
	    }
	}
}
else if ("3".equals(step)) {
    String[] column = getAndSetValues(request, session, "childcolumn");
    String[] index = getAndSetValues(request, session, "childindex");
    String[] trim = getAndSetValues(request, session, "childtrim");
    String[] timestamp = getAndSetValues(request, session, "childtimestamp");
    String[] html = getAndSetValues(request, session, "childhtml");
    String[] clob = getAndSetValues(request, session, "childclob");

    //default
    List<Class> transformerList = new ArrayList<Class>();
    transformerList.add(TrimTransformer.class);

    String field = "";
    if (column!=null && column.length>0) {
	    for (int i=0;i<column.length;i++) {
	        VelocityContext fieldmap = new VelocityContext();
	        fieldmap.put("column", column[i]);
	        fieldmap.put("index", index[i]);
	        fieldmap.put("trimflag", "false");
	        fieldmap.put("timestampflag", "false");
	        fieldmap.put("htmlflag", "false");
	        fieldmap.put("clobflag", "false");

	        if (trim!=null) {
		        for (String each: trim) {
		            if (each.equals(column[i])) {
		                fieldmap.put("trimflag", "true");
		                break;
		            }
		        }
	        }
	        if (timestamp!=null) {
		        for (String each: timestamp) {
		            if (each.equals(column[i])) {
		                fieldmap.put("timestampflag", "true");
                        if (!transformerList.contains(TimestampTransformer.class)) {
		                    transformerList.add(TimestampTransformer.class);
		                }
		                break;
		            }
		        }
	        }
	        if (html!=null) {
		        for (String each: html) {
		            if (each.equals(column[i])) {
		                fieldmap.put("htmlflag", "true");
		                break;
		            }
		        }
	        }
	        if (clob!=null) {
		        for (String each: clob) {
		            if (each.equals(column[i])) {
		                fieldmap.put("clobflag", "true");
		                if (!transformerList.contains(ClobTransformer.class)) {
		                    transformerList.add(ClobTransformer.class);
		                }
		                break;
		            }
		        }
	        }
	        field += VelocityManager.getInstance().getTemplateString("/field.template", fieldmap);
	    }
    }

    String transformer = "";
    for (int i=0;i<transformerList.size();i++) {
        if(i!=0) {
            transformer += ",";
        }
        transformer += transformerList.get(i).getName();
    }

    VelocityContext entitymap = new VelocityContext();
    entitymap.put("name", entityName);
    entitymap.put("transformer", transformer);
    entitymap.put("sql", rootSql);
    entitymap.put("body", field);
    String entity = VelocityManager.getInstance().getTemplateString("/entity.template", entitymap);

%>
       檢視Child Entity內容：<br>
   <textarea cols="100" rows="20" name="childBody" style="overflow: scroll;" ><%= entity %></textarea>
   <input type="hidden" name="childStep" value="<%= Integer.parseInt(step)+1 %>"><BR>
   <input type="hidden" name="step" value="4"><BR>
   <input type="button" value="上一步" onclick="toPrevStep();">
   <input type="button" value="下一步" onclick="backToMain();">
<%
}
%>
</form>
</body>
</html>
