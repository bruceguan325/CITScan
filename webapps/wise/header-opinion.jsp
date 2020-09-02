<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
    <title><bean:message key='globa.smart.robot.show'/></title>
	<!-- / jquery -->
	<!--[if (!IE)|(gt IE 8)]><!-->
	<script src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
    <!--<![endif]-->

    <!--[if lte IE 8]>
    <script src="<%= request.getContextPath() %>/script/jquery-1.11.0.js"></script>
    <![endif]-->
	<script src="<%= request.getContextPath() %>/script/jquery-migrate-1.4.1.min.js"></script>
    <!--[if lt IE 9]>
      <script src="/wise/assets/javascripts/ie/html5shiv.js" type="text/javascript"></script>
      <script src="/wise/assets/javascripts/ie/respond.min.js" type="text/javascript"></script>
    <![endif]-->

    <!-- / START - page related stylesheets [optional] -->

    <!-- / END - page related stylesheets [optional] -->
    <!-- / bootstrap [required] -->
    <link href="<%= request.getContextPath() %>/assets/stylesheets/bootstrap/bootstrap.css" media="all" rel="stylesheet" type="text/css" />
    <!-- / theme file [required] -->
    <link href="<%= request.getContextPath() %>/assets/stylesheets/light-theme.css" media="all" id="color-settings-body-color" rel="stylesheet" type="text/css" />
    <!-- / coloring file [optional] (if you are going to use custom contrast color) -->
    <link href="<%= request.getContextPath() %>/assets/stylesheets/theme-colors.css" media="all" rel="stylesheet" type="text/css" />
