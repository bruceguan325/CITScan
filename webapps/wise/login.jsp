<%@page import="org.hibernate.criterion.Restrictions"%>
<%@page import="org.hibernate.Criteria"%>
<%@page import="org.hibernate.Query"%>
<%@page import="com.intumit.hibernate.HibernateUtil"%>
<%@page import="org.hibernate.Transaction"%>
<%@page import="org.hibernate.Session"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" 
language="java" 
	import="java.util.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.message.MessageUtil"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.user.*"
	import="com.intumit.systemconfig.WiseSystemConfig"
%>
<%@ include file="/commons/taglib.jsp"%>
<%
String email = request.getParameter("email");
String password = request.getParameter("password");
String redirectTo = request.getParameter("r");
Locale browserLocale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
String msg = null;

if (StringUtils.isNotEmpty(email)) {
	//如果 enable 前台帳號，則呼叫任何 qa 都需要有 apikey 對應的 Tenant 才行
	if (WiseSystemConfig.get().getEnableFrontEndUser()) {
		User u = User.login(email, password);
		
		if (u != null) {
			User.setToSession(session, u);
			
			if (StringUtils.isNotBlank(redirectTo)){
				response.sendRedirect(redirectTo);
			}
		}
		else {
			msg = MessageUtil.getMessage(browserLocale, "user.login.failed");
		}
	}
	else {
		
		if (User.isLogin(email, password)) {
			User u = new User.GuestUser();
			u.setName(StringEscapeUtils.escapeHtml(email));
			User.setToSession(session, u);
			
			if (StringUtils.isNotBlank(redirectTo)){
				response.sendRedirect(redirectTo);
			}
		}
		else {
			msg = MessageUtil.getMessage(browserLocale, "user.login.failed");
		}
	}
}

%>
<!DOCTYPE html>
<html>
<head>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>

    <jsp:include page="header-opinion.jsp" />
</head>
<body class='contrast-banana login contrast-background '>
<div class="hero-unit">
<% if (msg != null) { %>
<div class="alert alert-danger" role="alert"><%= msg %></div>
<% } %>
</div>

    <div class='middle-container'>
      <div class='middle-row'>
        <div class='middle-wrapper'>
          <div class='login-container-header'>
            <div class='container'>
              <div class='row'>
                <div class='col-sm-12'>
                  <h1 class='text-center title'>
                   	<bean:message key="global.product.name"/>
                  </h1>
                </div>
              </div>
            </div>
          </div>
          <div class='login-container'>
            <div class='container'>
              <div class='row'>
                <div class='col-sm-4 col-sm-offset-4'>
                  <h2 class='text-center title'><bean:message key="global.signin.pleaseEnterYourNickname"/></h2>
                  <form action='/wise/wiseadm/login.jsp' class='validate-form' method='post'>
                    <input type="hidden" name="r" value="chooseTenant.jsp">
                    <div class='form-group'>
                      <div class='controls with-icon-over-input'>
                        <input value="" placeholder="Email / Nickname" class="form-control" data-rule-required="true" name="email" type="text" />
                        <i class='icon-user text-muted'><bean:message key="global.signin.email"/></i>
                      </div>
                    </div>
                    <div class='form-group'>
                      <div class='controls with-icon-over-input'>
                        <input value="" placeholder="Password" class="form-control" data-rule-required="true" name="password" type="password" autocomplete="off" />
                        <i class='icon-user text-muted'><bean:message key="global.password"/></i>
                      </div>
                    </div>
                    <%--
                    <div class='form-group'>
                      <div class='controls with-icon-over-input'>
							<select name="tid" class="form-control">
							<%
							List<Tenant> tenants = Tenant.list();
							for (Tenant t: tenants) {
							%>
							<option value="<%= t.getId() %>"><%= t.getNotes() %>(<%= t.getName() %>)</option>
							<%
							}
							%>
							</select>
                      </div>
                    </div>
                    <div class='checkbox'>
                      <label for='remember_me'>
                        <input id='remember_me' name='remember_me' type='checkbox' value='1'>
                        Remember me
                      </label>
                    </div>
                  <div class='text-center'>
                    <hr class='hr-normal'>
                    <a href='forgot_password.html'>Forgot your password?</a>
                  </div>
                   --%>
                    <button class='btn btn-block'><bean:message key="global.signin.submit"/></button>
                  </form>
                </div>
              </div>
            </div>
          </div>
          <div class='login-container-footer'>
            <div class='container'>
              <div class='row'>
                <div class='col-sm-12'>
                  <div class='text-center'>
                    <a href='login.jsp'>
                      <i class='icon-user'></i>
                      	powered by Intumit Inc.
                    </a>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

<jsp:include page="footer-opinion.jsp" />
</body>
</html>
