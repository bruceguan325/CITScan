<%@page import="org.apache.commons.io.IOUtils"%>
<%@page import="org.apache.commons.io.FileUtils"%>
<%@page import="org.apache.velocity.runtime.parser.node.GetExecutor"%>
<%@page import="org.apache.commons.io.FilenameUtils"%>
<%@page import="org.apache.commons.fileupload.FileUploadException"%>
<%@page import="org.apache.commons.fileupload.FileItem"%>
<%@page import="org.apache.commons.fileupload.disk.DiskFileItemFactory"%>
<%@page import="org.apache.commons.fileupload.servlet.ServletFileUpload"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="javax.servlet.ServletConfig"
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
import="org.apache.lucene.index.*"
import="org.apache.solr.core.*"
import="org.apache.solr.servlet.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.embedded.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="org.apache.solr.common.cloud.*"
import="org.apache.solr.request.*"
import="org.apache.solr.search.*"
import="com.intumit.solr.SearchManager"
import="com.intumit.solr.config.ColumnNameMappingFacade"
import="com.intumit.systemconfig.WiseSystemConfig"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O1) == 0) {
    return;
}
%><%!
String getPath(String filename){
    Date now = new Date();
    //SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd");// For Jetty
    //return "ckeditor-upload/" + dFormat.format(now) + "/" + now.getTime()
    SimpleDateFormat dFormat = new SimpleDateFormat("yyyyMMdd");// For Websphere
    return "./ckeditor-upload/" + dFormat.format(now) + now.getTime()
        + "." + FilenameUtils.getExtension(filename);
}
%><%
String data = null;
String path = null;
String url = null;
boolean isMultipart = ServletFileUpload.isMultipartContent(request);
if(isMultipart){
    DiskFileItemFactory factory = new DiskFileItemFactory();
    ServletFileUpload upload = new ServletFileUpload(factory);
    InputStream is = null;
    OutputStream os = null;
    try {
        @SuppressWarnings("unchecked")
        List<FileItem> items = upload.parseRequest(request); //每次重啟Websphere的時候會重展開war檔，而且Websphere禁止User去變動
        for(FileItem item:items){                            //展開war檔後的wise資料夾，導致上傳 ckedior-upload 的時候
            if (!item.isFormField()) {               //會報 Internal Server 500 error, 所以把 ckedior-upload 的資料夾移出 war 檔
                path = getPath(item.getName());      //並在 Websphere 的 server.sh 內每開完機以後，立即建一個 Symblick Link 指向  ckeditor-upload
                is = item.getInputStream();
                //os = FileUtils.openOutputStream(new File(application.getRealPath(path)));  // For Jetty 
                os = FileUtils.openOutputStream(new File(path));                         // For Websphere
                IOUtils.copy(is, os);
                WiseSystemConfig syscfg = WiseSystemConfig.get();
                url =  StringUtils.defaultString(syscfg.getHostname()) + syscfg.getContextPath() + "/" + path;
                break;
            }
        }
    } catch (FileUploadException e) {
        data = e.getMessage();
    } finally {
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
    }
}
String funcNum = request.getParameter("CKEditorFuncNum");
%>
<script type="text/javascript">
window.parent.CKEDITOR.tools.callFunction(<%=funcNum%>,
    '<%= StringEscapeUtils.escapeJavaScript(url) %>'
    <% if(data != null){ %>
    , '<%= StringEscapeUtils.escapeJavaScript(data) %>'
    <% } %>
);
</script>