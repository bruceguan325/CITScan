<script>
    if (top.frames.length != 0) {
        alert('error');
        location.assign("http://www.intumit.com/");
    }
</script>
<%@page import="java.util.Locale" %>
<%@page import="com.intumit.message.MessageUtil" %>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="java.util.List"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.hithot.HitHotLocale"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.solr.util.WiSeUtils"
	import="com.intumit.systemconfig.*"
	import="org.apache.struts.Globals"
	import="com.intumit.security.owasp.ESAPI"
%><%
try {
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String nowStr = sdf.format(java.util.Calendar.getInstance().getTime());
String nickname = "Guest";
Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);

Tenant t = null;
String qaId = ESAPI.encoder().encodeForHTML(request.getParameter("qaId"));

String selectedHitHotLocaleStr = ESAPI.encoder().encodeForHTML(request.getParameter("locale"));
Locale browserLocale = request.getLocale();

if (qaId == null) {
	request.getSession(false).invalidate();
	session = request.getSession(true);
	String tidEncoded = request.getParameter("t");
	Integer tid = Integer.parseInt(WiSeUtils.aesDecrypt(Tenant.SIMPLE_AES_SECRET_KEY, Tenant.SIMPLE_AES_INIT_VECTOR, tidEncoded));
	t = Tenant.get(tid);

	Locale sessionLocale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");
	
	if (selectedHitHotLocaleStr != null) {
		HitHotLocale hithotLocale = HitHotLocale.isLocaleString(selectedHitHotLocaleStr) ? HitHotLocale.valueOf(selectedHitHotLocaleStr) : null;
	
		if (hithotLocale != null) {
		    request.getSession().setAttribute("org.apache.struts.action.LOCALE", hithotLocale.getLocale());
		    Locale.setDefault(hithotLocale.getLocale());
		}
	}
	else if (sessionLocale != null) {
	    Locale.setDefault(sessionLocale);
	}
	else if (browserLocale != null) {
	    Locale.setDefault(browserLocale);
	    request.getSession().setAttribute("org.apache.struts.action.LOCALE", browserLocale);
	}
	
	if (t.getEnableUserFrontEnd()) {
		qaId = java.util.UUID.randomUUID().toString();
		QAContext ctx = QAContextManager.create(qaId);
		ctx.setTenant(t);
		QAContextManager.put(qaId, ctx);

		session.setAttribute("tenant", t);
		session.setAttribute("qaId", qaId);
		response.sendRedirect(request.getContextPath() + "/chats.jsp?qaId=" + qaId);
	}
	else {
		if(HitHotLocale.ja_JP.getLocale().equals(request.getSession().getAttribute(Globals.LOCALE_KEY))) {
			out.println("<h1>ただいまメンテナンス中です。 ご不便をおかけいたしますが、今しばらくお待ちください。</h1>");
		}
		else {
			out.println("<h1>User front-end not enabled</h1>");
		}
		return ;
	}
}
else {
	t = (Tenant)session.getAttribute("tenant");
}

QAContext qaCtx = QAContextManager.lookup(qaId);
if (qaCtx == null) {
	out.println("<h2>Session timeout, please restart chat again.</h2>");
	return ;
}
ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);

if (log == null) {
	log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", "" + System.currentTimeMillis(), ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
	ServiceLogEntity.setIntoSession(qaId, log);
}

// 若有 APIKEYS，取第一個
QAUtil qu = QAUtil.getInstance(qaCtx.getTenant());
%>
<!DOCTYPE html>
<html>
<head>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    <jsp:include page="./header-opinion.jsp" />
	<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
	<link href="<%=request.getContextPath()%>/styles/pnotify.custom.css" media="all" rel="stylesheet" type="text/css" />
	<link href="<%=request.getContextPath()%>/styles/pnotify.smartrobot.themes.css" media="all" rel="stylesheet" type="text/css" />
	<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
	<script src="<%=request.getContextPath()%>/script/pnotify.custom.js" type="text/javascript"></script>
	<script src="<%=request.getContextPath()%>/script/bootstrap-toggle.min.js"></script>
	<script src="<%= request.getContextPath() %>/script/mustache.min.js"></script>
    <style>
    .page-header {
    	margin-top: -8px;
    	margin-bottom: 18px;
    	min-height: 70px;
    }
    .message {min-height:70px;}
    .message>div.body>img{
    	max-width: 100%;
    }
    .other-questions {
    	font-size: 1em;
    	padding: 0;
    	padding-left: 20px;
    	border-bottom: 0px;
    }
    .other-questions-li {
    	font-size: 1em;
    	padding: 0;
    	border-bottom: 0px;
    	cursor: pointer;
    }
    .al {
    	color: blue;
    	font-weight: bold;
    	text-decoration: underline;
    }
    .tool-col {
    		border-right: 1px dashed #333;
    		postition: relative;
    		float: left;
    		padding-right: 3px;
    		margin: 0 3px 0 3px;
    }
    .faq-clickable {
    	font-size: 1.2em;
    	padding: 0;
    	border-bottom: 0px;
    	cursor: pointer;
    }
    .test-panel span {
    	font-size: 2em;
    	margin-right: 10px;
    }
	.loading-panel {
		width:100%; height:100%; display: none;
		position: fixed; top:0; left:0; z-index:999;
		background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
	}
    .table-custom-theme1 {
  		border: 1px solid #000000;
  	}
    .table-custom-theme1 > thead > tr > th,
    .table-custom-theme1 > thead > tr > td,
    .table-custom-theme1 > tbody > tr > th,
    .table-custom-theme1 > tbody > tr > td,
    .table-custom-theme1 > tfoot > tr > th,
    .table-custom-theme1 > tfoot > tr > td {
    	border: 1px solid #000000;
    	text-align: center;
    }
	.avatar {
	    position: absolute;
	    margin-right: 10px;
	    top: 10px;
	    left: 10px;
	    width: 46px;
	    height: 46px;
	}
    </style>
</head>
<body class='contrast-red main-nav-closed'>
<header>
    <jsp:include page="./header2-opinion.jsp" />
</header>
<div id='wrapper'>
<div id='main-nav-bg'></div>
<nav class='' id='main-nav'>
<div class='navigation'>
    <jsp:include page="./navbar-opinion.jsp" />
</div>
</nav>
<section id='content'>
    <div class='container'>
      <div class='row' id='content-wrapper'>
        <div class='col-xs-12'>
          <div class='row'>
            <div class='col-sm-12'>
              <div class='page-header'>
                <h1 class='pull-left hidden-xs'>
                  <i class='icon-comments'></i>
                  <span><bean:message key='chat.title.test'/></span>
                </h1>
                <h2 class='pull-left hidden-sm hidden-md hidden-lg'>
                  <i class='icon-comments'></i>
                  <span><bean:message key='chat.title.test'/></span>
                </h2>
                <div class='pull-right hidden-xs'>
                    &nbsp;&nbsp;
				    <a class='btn btnToggleFaq' href='#'>
				      <i class='icon-reorder'></i>
				    </a>
                </div>
                <div class='pull-right hidden-xs hidden-sm'>
                  <ul class='breadcrumb'>
                    <li>
                      <a href='index.jsp'>
                        <i class='icon-bar-chart'></i><bean:message key='global.homepage'/>
                      </a>
                    </li>
                    <li class='separator'>
                      <i class='icon-angle-right'></i>
                    </li>
                    <li><bean:message key='global.customerService'/></li>
                    <li class='separator'>
                      <i class='icon-angle-right'></i>
                    </li>
                    <li class='active'><bean:message key='global.online.service'/></li>
                  </ul>
                </div>
            </div>
          </div>
          </div>

          <div class='row'>
            <div class='col-sm-8 divMainPart'>
              <div class='box'>
                <div class='row'>
                  <div class='chat'>
                    <div class='col-sm-12'>
                      <div class='box'>
                        <div class='box-content box-no-padding'>
                          <div class='scrollable' data-scrollable-height='500' data-scrollable-start='bottom'>
                            <ul class='list-unstyled list-hover list-striped main-stream'>
                              <li class='message'>
                                <div class='avatar'>
                                  <img alt='Avatar' src='<%= request.getContextPath() %>/img/smartrobot_avatar1_46x46.png' >
                                </div>
                                <div class='name-and-time'>
                                  <div class='name pull-left'>
                                    <small>
                                      <a class="text-contrast" href="#"><%= ESAPI.encoder().encodeForHTML(RobotFormalAnswers.giveMeRandomAnswer(qaCtx.getTenant().getId(), "ROBOT_NAME", MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "robot.name"))) %></a>
                                    </small>
                                  </div>
                                  <div class='time pull-right'>
                                    <small class='date pull-right text-muted'>
                                      <span class='timeago fade has-tooltip' data-placement='top' title='<%= nowStr %>'><%= nowStr %></span>
                                      <i class='icon-time'></i>
                                    </small>
                                  </div>
                                </div>
                                <div class='body'>
                                <%String [] args = { nickname }; %>
                                <%= MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "what.can.i.ddo.for.you", args)%>
                                </div>
                              </li>
                            </ul>
                          </div>
                          <form class="new-message" method="post" action="#" accept-charset="UTF-8">
                          <input name="authenticity_token" type="hidden" />
                          <input class='form-control' id='message_body' name='message[body]' inputType='string' placeholder='Type your message here...' type='text'>
                          <button class='btn btn-success btn-lg pull-right' type='submit' style='margin-right: 46px;' >
                            <span class='glyphicon glyphicon-play'></span>
                          </button>
                          <button class='btn btn-success btn-lg pull-right' type='button' id="btnMic">
                            <i class='icon-microphone'></i>
                          </button>
	                          <div style="display: inline;">
<%
if (qaCtx.getTenant().getEnableMultiLocale()) {
	List<HitHotLocale> allowedLocales = qaCtx.getTenant().getAllowedLocalesList();
%>
    <select id="qaLocaleSelect" name="locale">
	<% for (HitHotLocale hhl: allowedLocales) { %>
	    	<option value="<%=ESAPI.encoder().encodeForHTMLAttribute(hhl.name())%>"><%=ESAPI.encoder().encodeForHTML(hhl.getText())%></option>
	<% } %>
	</select>
<% 
} 
%>
<%
String[] qaCategorys = null;
if (qaCtx.getTenant().getQaCategory() != null) {
	qaCategorys = qaCtx.getTenant().getQaCategory().split(",");
}
%>
<div class='tool-col hide'>
<bean:message key="qacategory.select"/>
<select id="qaCategorySelect">
	<option value=''>--NONE--</option>
<% for (int i = 0; qaCategorys != null && i < qaCategorys.length; i++) { %>
	<option value="<%=ESAPI.encoder().encodeForHTMLAttribute(qaCategorys[i])%>"><%=ESAPI.encoder().encodeForHTML(qaCategorys[i])%></option>
<% } %>
</select>
</div>
<div class='tool-col hide'>
Channel 
<select id="qaChannelType">
<% for (QAChannel ch: QAChannel.list(qaCtx.getTenant().getId())) { %>
	<option value="<%= ESAPI.encoder().encodeForHTMLAttribute(ch.getCode()) %>"><%= ESAPI.encoder().encodeForHTML(ch.getName()) %></option>
<% } %>
</select>
</div>
<div class='tool-col hide'>
User Type
<select id="qaUserType">
<% for(QAUserType userType : QAUserType.list(qaCtx.getTenant().getId())) { %> 
	<option value="<%= ESAPI.encoder().encodeForHTMLAttribute(userType.getCode()) %>"><%= ESAPI.encoder().encodeForJavaScript(MessageUtil.returnKeyIfNotExist(locale, userType.getName())) %></option>
<% } %>
</select>
</div>
<div class='tool-col'>
<bean:message key="chat.autoTTS"/> <input id='btnToggleAutoTTS' data-on="<bean:message key="global.enable"/>" data-off="<bean:message key="global.disable"/>" type='checkbox' data-size="mini" data-toggle="toggle">
</div>
							  </div>
						 </form>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>

<%
JSONArray faqsArr = new JSONArray(StringUtils.isNotEmpty(qaCtx.getTenant().getFaqJson()) ? qaCtx.getTenant().getFaqJson() : "[]");
%>
            <div class='col-sm-4 box divFaqPart' style='margin-bottom: 0'>
                  <div class='box-header muted-background'>
                    <div class='title'><bean:message key="chat.faq"/></div>
                    <div class='actions'>
                      <a class="btn box-remove btn-xs btn-link" href="#"><i class='icon-remove'></i>
                      </a>

                      <a class="btn box-collapse btn-xs btn-link" href="#"><i></i>
                      </a>
                    </div>
                  </div>
                  <div class='box-content'>
                    <div class='tabbable'>
                      <ul class='nav nav-tabs nav-tabs-simple'>
                      <%
                      for (int i=0; i < faqsArr.length(); i++) {
                    	  JSONObject faqsObj = faqsArr.getJSONObject(i);
                    	  JSONArray faqs = faqsObj.getJSONArray("questions");
                      %>
                        <li class='<%= i==0? "active": "" %>'>
                          <a class='red-border text-primary' data-toggle='tab' href='#tabsimple-tab<%= i %>'>
                            <i class='icon-money'></i>
                            <%= ESAPI.encoder().encodeForHTML(StringUtils.upperCase(faqsObj.optString("channel", faqsObj.optString("category", "未知")))) %>
                          </a>
                        </li>
                      <%
                      }
                      %>
                      </ul>
                      <div class='tab-content'>

                      <%
                      for (int i=0; i < faqsArr.length(); i++) {
                    	  JSONObject faqsObj = faqsArr.getJSONObject(i);
                    	  JSONArray faqs = faqsObj.getJSONArray("questions");
                      %>
                        <div class='tab-pane <%= i==0? "active": "" %>' id='tabsimple-tab<%= i %>'>
                          <ul class='list-unstyled list-hover list-striped'>
                          	<%
                          	for (int j=0; j < faqs.length(); j++) {
                          		String testQ = faqs.getString(j);
                          	%>
                            <li class='message'>
                              <div class='row'>
                              <div class='col-sm-1 hidden-sm hidden-xs' style="margin-left: 20px;">
                                <span class="icon-question-sign" style="font-size: 23px;"></span>
                              </div>
                              <div class='col-sm-10 body faq-clickable' style='word-break: break-all;'>
                                <%= ESAPI.encoder().encodeForHTML(testQ) %>
                              </div>
                              </div>
                            </li>
                            <% } %>
                          </ul>
                        </div>
                      <%
                      }
                      %>
                      </div>
                    </div>
                  </div>
                </div>
              </div>




          </div>
        </div>
      </div>
    </div>
</section>
</div>
<div style="display: none">
<li class='message message-template-user'>
  <div class='avatar'>
    <img alt='Avatar' src='<%= request.getContextPath() %>/img/smartrobot_avatar2_46x46.png'>
  </div>
  <div class='name-and-time'>
    <div class='name pull-left'>
      <small>
        <a class="text-contrast" href="#"><%=nickname %></a>
      </small>
    </div>
    <div class='time pull-right'>
      <small class='date pull-right text-muted'>
        <span class='timeago fade has-tooltip' data-placement='top' title='<%= nowStr %>'><%= nowStr %></span>
        <i class='icon-time'></i>
      </small>
    </div>
  </div>
  <div class='body'>
     <bean:message key='global.hello'/>
  </div>
</li>
<li class='message message-template-robot'>
  <div class='avatar'>
    <img alt='Avatar' src='<%= request.getContextPath() %>/img/smartrobot_avatar1_46x46.png' >
  </div>
  <div class='name-and-time'>
    <div class='name pull-left'>
      <small>
        <a class="text-contrast" href="#"><%= ESAPI.encoder().encodeForHTML(RobotFormalAnswers.giveMeRandomAnswer(qaCtx.getTenant().getId(), "ROBOT_NAME", MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "robot.name")))%></a>
      </small>
    </div>
    <div class='time pull-right'>
      <small class='date pull-right text-muted'>
        <span class='timeago fade has-tooltip' data-placement='top' title='<%= nowStr %>'><%= nowStr %></span>
        <i class='icon-time'></i>
      </small>
    </div>
  </div>
  <div class='body'>
    <bean:message key='turn.real.customer.service'/>
  </div>
</li>
</div>
<div id="magic-dialog" title="<bean:message key='robot.magicBtn.title'/>" style="display:none; z-index:999; ">
</div>
<jsp:include page="./footer-opinion.jsp" />
	<script type="text/javascript">
	var qaChannel = '';
    if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
    	qaChannel = 'app';
    }
	</script>

    <script>
      var testerMode = false;
      var qaId = '<%= StringEscapeUtils.escapeJavaScript(qaId) %>';
      var histories = {};
      var ulpShown = false;
      var ulp;
      var channel = 'web';
      var userType = 'unknown';

      var currentQuestion;
      var currentTimestamp;
      var faqOpened = true;

      $(".btnToggleFaq").click(function() {
    	  if (faqOpened) {
    		  $('.divMainPart').removeClass('col-md-8');
    		  $('.divMainPart').removeClass('col-sm-8');
    		  $('.divMainPart').addClass('col-sm-12');
    		  $('.divMainPart').addClass('col-md-12');
    		  $('.divFaqPart').hide();
    		  faqOpened = false;
    	  }
    	  else {
    		  $('.divMainPart').removeClass('col-md-12');
    		  $('.divMainPart').removeClass('col-sm-12');
    		  $('.divMainPart').addClass('col-sm-8');
    		  $('.divMainPart').addClass('col-md-8');
    		  $('.divFaqPart').show();
    		  faqOpened = true;
    	  }
      });

      $(".faq-clickable").live('click', function(e) {
    	  var cmd = $(this).text();
		  $("#message_body").val(cmd);
          submitQuestion(cmd, 'string', '<%= QAContext.EVENT_SOURCE_TYPE_BUTTON %>', '_FAQ_GUI');
      });

      var myConfirm = function(title, msg, okCallback) {
      	if (!$('#dataConfirmModal').length) {
      		$('body').append('<div id="dataConfirmModal" class="modal fade" tabindex="-1" role="dialog"> <div class="modal-dialog"> <div class="modal-content"> <div class="modal-header"> <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button> <h4 class="modal-title">Modal title</h4> </div> <div class="modal-body"> <p>One fine body&hellip;</p> </div> <div class="modal-footer"> <button type="button" class="btn btn-default" data-dismiss="modal">Close</button> <button type="button" class="btn btn-primary" id="dataConfirmOK" >Ok</button> </div> </div><!-- /.modal-content --> </div><!-- /.modal-dialog --> </div><!-- /.modal -->');
      	}

      	$('#dataConfirmModal').find('.modal-title').text(title);
      	$('#dataConfirmModal').find('.modal-body').html(msg);
      	$('#dataConfirmModal').modal({show:true});

      	var onClick = function() {
      		$('#dataConfirmModal').modal('toggle');
      		okCallback();
      	};

      	$(document).on("click", '#dataConfirmOK', onClick);

      	$('#dataConfirmModal').on('hidden.bs.modal', function () {
      		$(document).off("click", '#dataConfirmOK', onClick);
      	});

      	return false;
      }

      $(".question-clickable").live('click', function(e) {
    	  var cmd = $(this).attr('data-opnum');
    	  var eventSource = $(this).attr('data-es');
		  $("#message_body").val(cmd);
		  submitQuestion(cmd, 'string', '<%= QAContext.EVENT_SOURCE_TYPE_BUTTON %>', eventSource);
      });

      $(".chat .new-message").live('submit', function(e) {
          submitQuestion($("#message_body").val(), 'string');
		  return e.preventDefault();
      });

      function submitQuestion(q, inputType, eventSourceType, eventSource) {
    	  
    	  var chat, date, li, message, months, reply, scrollable, sender, timeago, qaCategoryEnable, selectQaChannelType, selectQaUserType;
          date = new Date();
          months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
          chat = $("#message_body").parents(".chat");
          message = q;

          $("#message_body").val("");
          $("#message_body").attr('inputType', 'string');

          if (message.length !== 0) {
			var selectQaChannelType = $("#qaChannelType option:selected").val();
			var selectQaUserType = $("#qaUserType option:selected").val();
			var locale = $("#qaLocaleSelect option:selected").val();

			if(!selectQaChannelType) {
				selectQaChannelType = 'web'; 
			}
			
			if(!selectQaUserType) {
				selectQaUserType = 'unknown';
			}
			
            var options = {
              		  id: qaId,
               		  q: message,
               		  testMode: false,
               		  userType: selectQaUserType,
               		  html: true,
               		  locale: locale,
               		  ch:selectQaChannelType
               	  };

            if (typeof(eventSourceType) != 'undefined') {
            	options['est'] = eventSourceType;
            }
            if (typeof(eventSource) != 'undefined') {
            	options['es'] = eventSource;
            }
			if ($("#qaCategorySelect option:selected").text() != '') {
				options.category = $("#qaCategorySelect option:selected").text();
			}

            userli = $("li.message-template-user").clone();
            userli.removeClass("message-template-user");

            if (inputType == 'password' || inputType == 'stringMask') {
            	userli.find(".body").text("********");
            }
            else if (inputType == 'mic') {
            	userli.find(".body").html("<img style='height:50px;' src='<%= request.getContextPath() %>/img/user_speech_icon.png' title='" + q + "'>" + (testerMode ? q : ''));
            }
            else {
            	userli.find(".body").text(message);
            }
            timeago = userli.find(".timeago");
            timeago.removeClass("in");
            var month = (date.getMonth() + 1);
            var date_day = (date.getDate());
            timeago.attr("title", "" + (date.getFullYear()) + "-" + (month<10 ? '0' : '') + month + "-" + (date_day<10 ? '0' : '' ) + date_day + " " + (date.getHours()) + ":" + (date.getMinutes()) + ":" + (date.getSeconds()) + " +0800");
            timeago.text("" + months[date.getMonth()] + " " + (date.getDate()) + ", " + (date.getFullYear()) + " " + (date.getHours()) + ":" + (date.getMinutes()));
            setTimeAgo(timeago);
            sender = $.trim(userli.find(".name").text());

            chat.find("ul.main-stream").append(userli);
            scrollable = userli.parents(".scrollable");
            $(scrollable).slimScroll({
              scrollTo: scrollable.prop('scrollHeight') + "px"
            });
            userli.effect("highlight", {}, 500);
    		$('.loading-panel').show();

            $.ajax({
              type: 'POST',
          	  url: '<%= request.getContextPath() %>/chats-ajax.jsp',
          	  dataType: 'json',
          	  data: options,
          	  error: function() {
					setTimeout(function() {$('.loading-panel').hide();}, 100);
              },
          	  success: function(resp) {
          		    console.log(resp);
          			resp["ts"] = (new Date()).getTime();

                    reply = $("li.message-template-robot").clone();
                    reply.removeClass("message-template-robot");

                    if (resp.hasOwnProperty('output')) {
                    	$(".btnTts").remove();
                    		reply.find(".body").html(resp.output + "&nbsp; <button class='btn btn-default btn-xs btnTts' data-loading-text='<bean:message key="load.and.play..."/>' data-id='<%= qaId %>'><span class='glyphicon glyphicon-bullhorn'></span><bean:message key="speak.out"/></button>");
                    }
                    else {
                    		reply.find(".body").html("<bean:message key='this.topic.has.not.yet.been.edited'/>");
                    }

                    if (resp.hasOwnProperty('qaCategory')) {
						$("<br><small class='text-info' style='margin-right: 10px;'>當前<bean:message key='category'/>：" + resp.qaCategory + "</small>").appendTo(reply.find(".body"));
          	  		}

                    if (resp.hasOwnProperty('confidence')) {
						$("<br><small class='text-primary' style='margin-right: 10px;'><bean:message key='global.confidence.level'/>：" + resp.confidence + " (" + resp.confidenceFactor + ")</small>").appendTo(reply.find(".body"));
					}

                    if (resp.hasOwnProperty('inputType')) {
                    		$("#message_body").attr('inputType', resp.inputType);
                    }
                    inputType = $("#message_body").attr("inputType");

                    if (inputType == 'password') {
                    		$("#message_body").attr('type', 'password');
                    }
                    else {
                    		$("#message_body").attr('type', 'text');
                    }

                    resp["flag"] = 0; // 先儲存一份
                    histories[resp.ts] = resp;
                    currentTimestamp = resp.ts;

					if (resp.hasOwnProperty('otherPossibleQuestions')) {
						var ul = $("<ol class='other-questions'></ol>");
						var otherQuestions = resp.otherPossibleQuestions;
						for (var i=0; i < otherQuestions.length; i++) {
							var thisLi = $("<li class='other-questions-li' style='border-bottom: 0px; padding: 0; padding-left: 10px;'></li>");

							var clickToAsk = $('<span/>');
							clickToAsk.addClass("question-clickable");
							clickToAsk.text(otherQuestions[i].question);
							clickToAsk.appendTo(thisLi);

							thisLi.appendTo(ul);
						}
						reply.find(".body").append("<BR><bean:message key='question.clickable'/>");
						reply.find(".body").append(ul);
					}

					setTimeout(appendReply, 100);
          	  },
          	  complete: function() {
					setTimeout(function() {$('.loading-panel').hide(); $('#message_body').focus();}, 100);
        	  }
            });
          }

          function appendReply() {
            date = new Date();
            timeago = reply.find(".timeago");
            timeago.attr("title", "" + (date.getFullYear()) + "-" + (month<10 ? '0' : '') + month + "-" + (date_day<10 ? '0' : '' ) + date_day + " " + (date.getHours()) + ":" + (date.getMinutes()) + ":" + (date.getSeconds()) + " +0800");
            timeago.text("" + months[date.getMonth()] + " " + (date.getDate()) + ", " + (date.getFullYear()) + " " + (date.getHours()) + ":" + (date.getMinutes()));
            setTimeAgo(timeago);
            scrollable.find("ul.main-stream").append(reply);
            $(scrollable).slimScroll({
              scrollTo: scrollable.prop('scrollHeight') + "px"
            });

            <%--
            // 將現有 magicBtn 隱藏，然後顯示目前問答的 magicBtn
            $("button[name='btnMagic']").hide();
            $("button[name='btnMagic'].notShown").removeClass('notShown').show();
            --%>

            if ($('#btnToggleAutoTTS').prop('checked')) {
            		$(".btnTts").click();
	        }
            return reply.effect("highlight", {}, 300);
          }
      }

      var recognizer = new webkitSpeechRecognition();
      var recognizerRecording = false;

      $(document).on('click', '.btnTts', function() {
	    	  $(this).button('loading');
	    	  var qaId = $(this).attr('data-id');
	    	  var ts = new Date().getTime();
	    	  
	    	  $.get('<%= request.getContextPath() %>/tts?qaId=' + qaId + '&ts=' + ts, function(data){

    		  var audio = new Audio();
              audio.src = '<%= request.getContextPath() %>/commons/' + data + '.wav';
              audio.load();
              audio.play();
              
              audio.addEventListener('ended', function() {
		    	  	$('.btnTts').button('reset');
		      });
	      });
      });

      $('#btnMic').click(function() {
      	recognizer.lang = "zh-TW";
      	if (recognizerRecording) {
      		$('#btnMic').removeClass('btn-danger').addClass('btn-success');
      		recognizerRecording = false;
      		recognizer.stop();
      	}
      	else {
      		$('#btnMic').removeClass('btn-success').addClass('btn-danger');
	      	recognizer.onresult = function(event) {
	      		recognizerRecording = false;
	      		$('#btnMic').removeClass('btn-danger').addClass('btn-success');
	      	    if (event.results.length > 0) {
	      	        var result = event.results[event.results.length-1];
	      	        if(result.isFinal) {
   	 		            submitQuestion(result[0].transcript, 'mic');
	      	        }
	      	    }
	      	};
      		recognizerRecording = true;
	      	recognizer.start();
      	}
      });

      $(document).on('click', '.btnToggle', function() {
    	  $this = $(this);
    	  $divId = $this.attr('data-target-div');
    	  $('#' + $divId).toggle();
    });
      
    var lastDFTS = -1;
    var systemNlp = '';
    
    $('#message_body').focus();
    </script>
<div class='loading-panel'></div>
<div id="divAudio_Player">
    <audio id="audio_player" src=""></audio>
</div>
<%
}
catch (Exception ex) {
	ex.printStackTrace();
}
%>
</body>
</html>



