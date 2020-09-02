<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="com.intumit.solr.user.*"
	import="java.util.*"
%>
<%
User user = User.getFromSession(session);
%>
  <nav class='navbar navbar-default'>
    <a class='navbar-brand' style="max-width:230px; overflow:hidden;" href='index.jsp'>
         <span><bean:message key='globa.smart.robot.page'/></span>
    </a>
    <a class='toggle-nav btn pull-left' href='#'>
      <i class='icon-reorder'></i>
    </a>
    <ul class='nav'>
      <li class='dropdown medium only-icon widget hidden-xs hidden-sm'>
        <a class='dropdown-toggle' data-toggle='dropdown' href='#'>
          <i class='icon-rss'></i>
          <div class='label'>0</div>
        </a>
        <ul class='dropdown-menu'>
         <%--
          <li>
            <a href='#'>
              <div class='widget-body'>
                <div class='pull-left icon'>
                  <i class='icon-user text-success'></i>
                </div>
                <div class='pull-left text'>
                  John Doe signed up
                  <small class='text-muted'>just now</small>
                </div>
              </div>
            </a>
          </li>
          <li class='divider'></li>
          <li>
            <a href='#'>
              <div class='widget-body'>
                <div class='pull-left icon'>
                  <i class='icon-inbox text-error'></i>
                </div>
                <div class='pull-left text'>
                  New Order #002
                  <small class='text-muted'>3 minutes ago</small>
                </div>
              </div>
            </a>
          </li>
          <li class='divider'></li>
          <li class='widget-footer'>
            <a href='#'>All notifications</a>
          </li>
           --%>
        </ul>
      </li>
      <li class='dropdown dark user-menu hidden-xs hidden-sm'>
        <a class='dropdown-toggle' data-toggle='dropdown' href='#'>
          <span class="icon-user" style="font-size: 25px;"></span>
          <span class='user-name'><%= user == null ? MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "global.test") : user.getName() %></span>
          <b class='caret'></b>
        </a>
        <ul class='dropdown-menu'>
        </ul>
      </li>
    </ul>
  </nav>
