<%@ include file="/commons/taglib.jsp"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    import="java.util.Locale"
    import="org.apache.commons.lang.StringUtils"
    import="com.intumit.message.MessageUtil"
    import="com.intumit.solr.robot.WSUtil"
    import="com.intumit.systemconfig.*"
    pageEncoding="UTF-8"%>
<%
// Basic security checking
com.intumit.solr.admin.AdminUser user = com.intumit.solr.admin.AdminUserFacade.getInstance().getFromSession(session);

if (user == null) {
	return;
}

WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();
String msg = request.getParameter("msg");
%>
<!DOCTYPE>
<html>
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><bean:message key='wisesystemconfig.set'/></title>
<script type="text/javascript" src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script type="text/javascript">
<%
	Locale local = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	String mxaCore = MessageUtil.getMessage(local, "max.core.num");
	String abstractNum = MessageUtil.getMessage(local, "abstract.num");
%>
function Save(){
	var message="";
	var form = document.configForm;
	if(form.coreMax=""){
		message=message+<%=mxaCore%>
	}
	if(form.descriptionMax=""){
		message=message+<%=abstractNum%>
	}

	if(message==""){
		form.submit();
	}

}
</script>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
</head>
<body>
<% if (msg != null) { %>
<div class="alert">
  <button type="button" class="close" data-dismiss="alert alert-success">&times;</button>
  <strong> <%= msg %> </strong>
</div>
<% } %>
<form action="<%= request.getContextPath() %>/wiseadm/UpdateSystemConfigServlet" method="POST" name="configForm">
<table>
<tr>
<td>Hostname</td>
<td>
<input type="text" name="hostname" value="<%= StringUtils.trimToEmpty(cfg.getHostname()) %>">(<bean:message key='system.hostname.ex'/>)
<input type="hidden" name="descriptionMax" value="<%=cfg.getDescriptionMax() %>">
<input type="hidden" name="coreMax" value="<%=cfg.getCoreMax() %>">
<input type="hidden" name="hotKeywordFrom" value="<%=cfg.getHotKeywordFrom() %>">
</td>
</tr>
<tr>
<td>localPort</td>
<td>
<input type="text" name="localPort" value="<%= StringUtils.trimToEmpty(cfg.getLocalPort()) %>">(ex:8080)
</td>
</tr>
<tr>
<td>Context Path</td>
<td>
<input type="text" name="contextPath" value="<%=cfg.getContextPath() %>">(<bean:message key='system.preset'/>/wise)
</td>
</tr>
<tr>
<td><bean:message key='allowedReverseProxies'/></td>
<td><input type="text" name="allowedReverseProxies" value="<%=cfg.getAllowedReverseProxies() %>"><bean:message key='allowedReverseProxies.ex'/></td>
</tr>
<tr>
<td><bean:message key='proxyenable'/></td>
<td><input type="radio" name="proxyEnable" value="true" <%=cfg.getProxyEnable() ? "checked" : "" %>><bean:message key='global.able'/>&nbsp;<input type="radio" name="proxyEnable" value="false" <%=cfg.getProxyEnable() ? "" : "checked" %>><bean:message key='global.close'/>&nbsp;
<br>
<div id="proxyDetail" <%=cfg.getProxyEnable() ? "" : "style='display:none'" %>>
	<input type="text" name="proxyHost" value="<%=StringUtils.trimToEmpty(cfg.getProxyHost()) %>">(ex. xxx.xxx.xxx)
	<input type="text" name="proxyPort" value="<%=cfg.getProxyPort() == null ? "" : cfg.getProxyPort() %>">(ex. 3128)
</div>
<script>
$('input:radio[name="proxyEnable"]').change(function() {
	if ($(this).is(':checked') && $(this).val() == 'true') {
        $("#proxyDetail").show();
    }
	else {
        $("#proxyDetail").hide();
	}
});
</script>
</td>
</tr>
<tr>
<td><bean:message key="mail.server.host"/></td>
<td><input type="text" name="mailServerHost" value="<%=cfg.getMailServerHost() %>"></td>
</tr>
<tr>
<td><bean:message key="mail.server.port"/></td>
<td><input type="text" name="mailServerPort" value="<%=cfg.getMailServerPort() %>"></td>
</tr>
<tr>
<td><bean:message key="mail.server.enable.ssl"/></td>
<td><input type="text" name="enableSsl" value="<%=cfg.isEnableSsl() %>"><bean:message key="mail.server.enable.ssl.desc"/></td>
</tr>
<tr>
<td><bean:message key="mail.user.name"/></td>
<td><input type="text" name="mailUsername" value="<%=cfg.getMailUsername() %>">(<bean:message key="mail.user.name.complete"/>)</td>
</tr>
<tr>
<td><bean:message key="mail.password"/></td>
<td><input type="password" name="mailPassword" value="">(<bean:message key="mail.password.empty.not.change"/>)</td>
</tr>
<%--
<tr>
<td><bean:message key='enableFrontEndUser'/></td>
<td><input type="text" name="enableFrontEndUser" value="<%=cfg.getEnableFrontEndUser() %>"><bean:message key='enableFrontEndUser.ex'/></td>
</tr>
--%>
<tr>
<td><bean:message key='lbModeEnable'/> (IsLeaderNow(): <%= com.intumit.solr.ClusterMembershipListener.getInstance().isLeaderNow() %>)</td>
<td><input type="text" name="lbModeEnable" value="<%=cfg.getLbModeEnable() %>"><bean:message key='lbModeEnable.ex'/></td>
</tr>
<tr>
<td><bean:message key='defaultTenantId'/></td>
<td><input type="text" name="defaultTenantId" value="<%=cfg.getDefaultTenantId() %>"><bean:message key='defaultTenantId.ex'/></td>
</tr>

<tr>
<td><bean:message key='vlog'/></td>
<td><input type="text" name="vLog" value="<%=cfg.getvLog() %>"></td>
</tr>

<tr>
<td><bean:message key='sso.permission.check.url'/></td>
<td><input type="text" name="ssoPermissionCheckUrl" value="<%=StringUtils.trimToEmpty(cfg.getSsoPermissionCheckUrl()) %>"></td>
</tr>

<%-- 
<tr>
<td><bean:message key='bancsWsUrl'/></td>
<td><input type="text" name="bancsWsUrl" value="<%=cfg.getBancsWsUrl() %>">(<bean:message key='system.preset'/> <%= WSUtil.DEFAULT_BANCS_ENDPOINT %>)</td>
</tr>
<tr>
<td><bean:message key='cardWsUrl'/></td>
<td><input type="text" name="cardWsUrl" value="<%=cfg.getCardWsUrl() %>">(<bean:message key='system.preset'/> <%= WSUtil.DEFAULT_CARD_ENDPOINT %>)</td>
</tr>
<tr>
<td><bean:message key='fepWsUrl'/></td>
<td><input type="text" name="fepWsUrl" value="<%=cfg.getFepWsUrl() %>">(<bean:message key='system.preset'/> <%= WSUtil.DEFAULT_FEP_ENDPOINT %>)</td>
</tr>
<tr>
<td><bean:message key='iBankLoginUrl'/></td>
<td><input type="text" name="iBankLoginUrl" value="<%= StringUtils.defaultString(cfg.getiBankLoginUrl()) %>">(<bean:message key='system.preset'/> <%= WiseSystemConfig.DEFAULT_IBANK_LOGIN_URL %>)</td>
</tr>
<tr>
<td><bean:message key='iBankFuncBaseUrl'/></td>
<td><input type="text" name="iBankFuncBaseUrl" value="<%= StringUtils.defaultString(cfg.getiBankFuncBaseUrl()) %>">(<bean:message key='system.preset'/> <%= WiseSystemConfig.DEFAULT_IBANK_FUNC_BASE_URL %>)</td>
</tr>
<tr>
<td><bean:message key='appUrlTransformPrefix'/></td>
<td><input type="text" name="appUrlTransformPrefix" value="<%= StringUtils.defaultString(cfg.getAppUrlTransformPrefix()) %>">(<bean:message key='system.preset'/> <%= WiseSystemConfig.DEFAULT_APP_URL_TRANSFORM_PREFIX %>)</td>
</tr>
<tr>
<td><bean:message key='appIbankUrlTransformPrefix'/></td>
<td><input type="text" name="appIbankUrlTransformPrefix" value="<%= StringUtils.defaultString(cfg.getAppIbankUrlTransformPrefix()) %>">(<bean:message key='system.preset'/> <%= WiseSystemConfig.DEFAULT_APP_IBANK_URL_TRANSFORM_PREFIX %>)</td>
</tr>
--%>
<tr><td colspan="2">
<input type="submit" name="save" value="<bean:message key='submit.set'/>"></td></tr>
</table>
</form>
</body>
</html>
<%
// 進系統設定畫面會強制刷新一下目前的 master / slave 狀態
com.intumit.solr.ClusterMembershipListener.getInstance().checkAndSwitchMode();
%>
