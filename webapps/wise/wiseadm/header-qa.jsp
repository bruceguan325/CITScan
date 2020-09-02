
<%
String ctxPath = request.getContextPath();
%>
<!-- header style -->
<link href="<%= ctxPath %>/styles/bs3/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= ctxPath %>/styles/bs3/bootstrap-theme.min.css" rel="stylesheet">
<link href="<%= ctxPath %>/styles/pnotify.custom.css" media="all" rel="stylesheet" type="text/css" />
<link href="<%= ctxPath %>/styles/pnotify.smartrobot.themes.css" media="all" rel="stylesheet" type="text/css" />
<link href="<%= ctxPath %>/styles/bootstrap-toggle.min.css" rel="stylesheet">
<link href="<%= ctxPath %>/styles/slide-and-push.css" rel="stylesheet">

<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
<!--[if lt IE 9]>
  <script type='text/javascript' src="assets/javascript/ie/html5shiv.js"></script>
  <script type='text/javascript' src="//cdnjs.cloudflare.com/ajax/libs/respond.js/1.4.2/respond.js"></script>
<![endif]-->

<!-- Javascript -->
<!--[if (!IE)|(gt IE 8)]><!-->
<script src="<%= ctxPath %>/script/jquery-1.12.4.min.js"></script>
<!--<![endif]-->

<!--[if lte IE 8]>
<script src="<%= ctxPath %>/script/jquery-1.11.0.js"></script>
<![endif]-->

<script src="<%= ctxPath %>/wiseadm/js/vendor/jquery.ui.widget.js"></script>
<script src="<%= ctxPath %>/wiseadm/js/jquery.iframe-transport.js"></script>

<script type='text/javascript' src="<%= ctxPath %>/assets/javascripts/plugins/modernizr/modernizr.min.js"></script> 
<script type='text/javascript' src="<%= ctxPath %>/script/css3-mediaqueries.js"></script> 

<!-- / jquery mobile (for touch events) -->
<script src="<%= ctxPath %>/assets/javascripts/jquery/jquery.mobile.custom.min.js" type="text/javascript"></script>
<!-- / jquery migrate (for compatibility with new jquery) [required] -->
<script src="<%= ctxPath %>/assets/javascripts/jquery/jquery-migrate.min.js" type="text/javascript"></script>
<!-- / jquery ui -->
<script src="<%= ctxPath %>/assets/javascripts/jquery/jquery-ui.min.js" type="text/javascript"></script>
<!-- / jQuery UI Touch Punch -->
<script src="<%= ctxPath %>/assets/javascripts/plugins/jquery_ui_touch_punch/jquery.ui.touch-punch.min.js" type="text/javascript"></script>
<!-- / retina -->
<script src="<%= ctxPath %>/assets/javascripts/plugins/retina/retina.js" type="text/javascript"></script>
<!-- / theme file [required] -->
<script src="<%= ctxPath %>/assets/javascripts/theme.js" type="text/javascript"></script>
<script src="<%= ctxPath %>/assets/javascripts/plugins/common/moment.min.js"></script>
<script src="<%= ctxPath %>/script/mustache.min.js"></script>

<!-- / START - page related files and scripts [optional] -->
<!-- bootstrap just to have good looking page -->
<script src="<%= ctxPath %>/script/bs3/bootstrap.min.js"></script>
<script src="<%= ctxPath %>/script/pnotify.custom.js" type="text/javascript"></script>
<script src='<%= ctxPath %>/script/js.cookie.js' type='text/javascript'></script>
<script src='<%= ctxPath %>/script/jquery.storageapi.min.js' type='text/javascript'></script>
<script src="<%= ctxPath %>/script/slide-and-push-classie.js"></script>

<script src="<%= ctxPath %>/script/bootstrap-toggle.min.js"></script>
<script src="<%= ctxPath %>/assets/javascripts/plugins/bootstrap_daterangepicker/bootstrap-daterangepicker.js" type="text/javascript"></script>
<script src="<%= ctxPath %>/assets/javascripts/plugins/slimscroll/jquery.slimscroll.min.js" type="text/javascript"></script>
<script src="<%= ctxPath %>/assets/javascripts/plugins/timeago/jquery.timeago.js" type="text/javascript"></script>
<script src="<%= ctxPath %>/wiseadm/js/i18n.js"></script>
<script src="<%= ctxPath %>/wiseadm/js/jquery.konami.min.js"></script>
<script src="<%= ctxPath %>/wiseadm/webRM/js/webRM-preview.js"></script>

<link href="<%= ctxPath %>/wiseadm/webRM/css/webRM-preview.css" rel="stylesheet">
<link href="<%= ctxPath %>/wiseadm/css/mystyle.css" rel="stylesheet">
<link href="<%= ctxPath %>/wiseadm/css/style.css" rel="stylesheet">
