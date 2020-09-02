<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
    <title><bean:message key='globa.smart.robot.show'/></title>
	<!-- / jquery -->
	<script src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
	<script src="<%= request.getContextPath() %>/script/jquery-migrate-1.4.1.min.js"></script>
    <!--[if lt IE 9]>
      <script src="<%= request.getContextPath() %>/assets/javascripts/ie/html5shiv.js" type="text/javascript"></script>
      <script src="<%= request.getContextPath() %>/assets/javascripts/ie/respond.min.js" type="text/javascript"></script>
    <![endif]-->

	<script src="<%= request.getContextPath() %>/assets/javascripts/jquery/jquery-ui.min.js" type="text/javascript"></script>
	<script src="<%=request.getContextPath()%>/script/jquery.query-object.js" type="text/javascript"></script>
	<script src="<%=request.getContextPath()%>/script/pnotify.custom.js" type="text/javascript"></script>

    <!-- / START - page related stylesheets [optional] -->
    <!-- / jquery ui -->
    <link href='<%= request.getContextPath() %>/styles/jquery-ui/jquery-ui-1.10.0.custom.css' media='all' rel='stylesheet' type='text/css'>
    <link href='<%= request.getContextPath() %>/styles/jquery-ui/jquery.ui.1.10.0.ie.css' media='all' rel='stylesheet' type='text/css'>

    <!-- / END - page related stylesheets [optional] -->
    <!-- / bootstrap [required] -->
    <link href="<%= request.getContextPath() %>/assets/stylesheets/bootstrap/bootstrap.css" media="all" rel="stylesheet" type="text/css" />
    <!-- / theme file [required] -->
    <link href="<%= request.getContextPath() %>/assets/stylesheets/light-theme.css" media="all" id="color-settings-body-color" rel="stylesheet" type="text/css" />
    <!-- / coloring file [optional] (if you are going to use custom contrast color) -->
    <link href="<%= request.getContextPath() %>/assets/stylesheets/theme-colors.css" media="all" rel="stylesheet" type="text/css" />
    <link href='<%= request.getContextPath() %>/styles/pnotify.custom.css' media='all' rel='stylesheet' type='text/css'>
	<link href="<%=request.getContextPath()%>/styles/pnotify.smartrobot.themes.css" media="all" rel="stylesheet" type="text/css" />
    