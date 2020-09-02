<%@page import="java.util.Locale"%>
<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="java.util.List"
	import="org.apache.wink.json4j.*"
	import="org.apache.commons.lang.*"
	import="com.intumit.hithot.HitHotLocale"
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.qarule.*"
	import="com.intumit.solr.tenant.*"
	import="com.intumit.systemconfig.*"
	import="org.apache.struts.Globals"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.R) == 0) {
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp';
	</script>
	<%
	return;
}
%>
<%
AdminUser admUser = AdminUserFacade.getInstance().getFromSession(session);
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String nowStr = sdf.format(java.util.Calendar.getInstance().getTime());
String nickname = StringUtils.defaultString(admUser != null ? admUser.getName() : null, "Guest");
Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);

String qaId = null;
QAContext qaCtx = null;
do {
	qaId = java.util.UUID.randomUUID().toString();
	qaCtx = QAContextManager.lookup(qaId);
} while (qaCtx != null);

qaCtx = QAContextManager.create(qaId);

if (qaCtx.getTenant() == null) {
	com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

	if (t == null) {
		response.sendRedirect(request.getContextPath() + "/wiseadm/chooseTenant.jsp?r=" + java.net.URLEncoder.encode(request.getContextPath() + "/wiseadm/test.jsp", "UTF-8"));
	}
	else {
		qaCtx.setTenant(t);
		QAContextManager.put(qaId, qaCtx);
	}
}
else {
	// 為了展示系統設定功能，設定完要能馬上看到效果，所以在 test.jsp load 時候就重新去讀取一次 Tenant
	qaCtx.setTenant(Tenant.get(qaCtx.getTenant().getId()));
}
ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);

if (log == null) {
	log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", "" + System.currentTimeMillis(), ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
	ServiceLogEntity.setIntoSession(qaId, log);
}

// 若有 APIKEYS，取第一個
List<Apikey> apikeys = Apikey.list(qaCtx.getTenant().getId());
String apikey = apikeys.size() > 0 ? apikeys.get(0).getApikey() : "";
QAUtil qu = QAUtil.getInstance(qaCtx.getTenant());
%>
<!DOCTYPE html>
<html>
<head>
	<meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    <jsp:include page="../header-opinion.jsp" />
	<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
	<link href="<%=request.getContextPath()%>/wiseadm/webRM/css/webRM-testView.css" rel="stylesheet">
	<link href="<%=request.getContextPath()%>/styles/pnotify.custom.css" media="all" rel="stylesheet" type="text/css" />
	<link href="<%=request.getContextPath()%>/styles/pnotify.smartrobot.themes.css" media="all" rel="stylesheet" type="text/css" />
	<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
	<script src="<%=request.getContextPath()%>/script/pnotify.custom.js" type="text/javascript"></script>
	<script src="<%=request.getContextPath()%>/script/bootstrap-toggle.min.js"></script>
	<script src="<%=request.getContextPath()%>/script/mustache.min.js"></script>
	<script src="<%=request.getContextPath()%>/wiseadm/webRM/js/webRM-testView.js"></script>
    <script src="<%=request.getContextPath()%>/wiseadm/js/srbt-common.js"></script>
    <script src="<%=request.getContextPath()%>/script/swiper.min.js"></script>
    <style>
    .page-header {
    	margin-top: -8px;
    	margin-bottom: 18px;
    	min-height: 70px;
    }
    .main-stream>.message {min-height: 70px;}
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
    		padding-right: 5px;
    		margin: 5px 3px 5px 3px;
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
	a.answer-link {
        color: blue;
    }
    
    .textarea {
      border-left-style: none;
      height: 45px !important;
    } 
    </style>
</head>
<body class='contrast-red main-nav-closed'>
<header>
    <jsp:include page="../header2-opinion.jsp" />
</header>
<div id='wrapper'>
<div id='main-nav-bg'></div>
<nav class='' id='main-nav'>
<div class='navigation'>
    <jsp:include page="../navbar-opinion.jsp" />
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
                            <ul class='chat list-unstyled list-hover list-striped main-stream'>
                              <li class='message'>
                                <div class='avatar'>
                                  <img alt='Avatar' src='<%= request.getContextPath() %>/img/smartrobot_avatar1_46x46.png' >
                                </div>
                                <div class='name-and-time'>
                                  <div class='name pull-left'>
                                    <small>
                                      <a class="text-contrast" href="#"><%= RobotFormalAnswers.giveMeRandomAnswer(qaCtx.getTenant().getId(), "ROBOT_NAME", MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "robot.name")) %></a>
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
                          <div>
	                          <div id="inputDiv" style="margin-right: 100px;">
		                          <input name="authenticity_token" type="hidden" />
		                          <textarea class='form-control textarea' id='message_body' name='message[body]' inputType='string' placeholder='Type your message here...'></textarea>
	                          </div>
	                          <div>
		                          <button class='btn btn-success btn-lg pull-right' type='submit' style='margin-right: 46px;' >
		                            <span class='glyphicon glyphicon-play'></span>
		                          </button>
		                          <button class='btn btn-success btn-lg pull-right' type='button' id="btnMic">
		                            <i class='icon-microphone'></i>
		                          </button>
	                          </div>
                          </div>
	                          <div style="display: inline;">
                          <%
                          if (qaCtx.getTenant().getEnableMultiLocale()) {
                        	  List<HitHotLocale> allowedLocales = qaCtx.getTenant().getAllowedLocalesList();
%>
	                          <select id="qaLocaleSelect" name="locale">
	<% for (HitHotLocale hhl: allowedLocales) { %>
					       	    	<option value="<%=hhl.name()%>"><%=hhl.getText()%></option>
	<% } %>
							  </select>
<% 
} 
%>
<%
String[] qaCategorys = null;
if(qaCtx.getTenant().getQaCategory() != null) {
				       		qaCategorys = qaCtx.getTenant().getQaCategory().split(",");
}
%>
<div class='tool-col'>
<bean:message key="qacategory.select"/>
<select id="qaCategorySelect">
	<option value=''>--NONE--</option>
<% for (int i = 0; qaCategorys != null && i < qaCategorys.length; i++) { %>
	<option value="<%=qaCategorys[i]%>"><%=qaCategorys[i]%></option>
<% } %>
</select>
</div>
<div class='tool-col'>
Channel 
<select id="qaChannelType">
<% for (QAChannel ch: QAChannel.list(qaCtx.getTenant().getId())) { %>
	<option value="<%= ch.getCode() %>"><%= ch.getName() %></option>
<% } %>
</select>
</div>
<div class='tool-col'>
User Type
<select id="qaUserType">
<% for(QAUserType userType : QAUserType.list(qaCtx.getTenant().getId())) { %> 
	<option value="<%=userType.getCode()%>"><%= MessageUtil.returnKeyIfNotExist(locale, userType.getName()) %></option>
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
                            <%= StringUtils.upperCase(faqsObj.optString("channel", faqsObj.optString("category", "未知"))) %>
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
                              <div class='col-sm-1' style="margin-left: 20px;">
                                <span class="icon-question-sign" style="font-size: 23px;"></span>
                              </div>
                              <div class='col-sm-10 body faq-clickable' style='word-break: break-all;'>
                                <%= testQ %>
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
        <a class="text-contrast" href="#"><%= RobotFormalAnswers.giveMeRandomAnswer(qaCtx.getTenant().getId(), "ROBOT_NAME", MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "robot.name"))%></a>
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
<div id="user-login-panel" style="display: none;">
	<form class="pf-form pform_custom" name="user-login-form" action="#" method="post">
		<div class="pf-element pf-heading">
			<h4>登入官網帳號</h4>
			<p class="pf-msg"></p>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label text-primary">帳號</span>
				<input class="pf-field" type="text" name="username" />
				<br>
				<span class="pf-label text-primary">密碼</span>
				<input class="pf-field" type="password" name="password" />
				<br>
				<br>
				<input class="pf-button btn btn-primary btnUserLoginSubmit" data-loading-text="登入中..." type="button" name="submit" value="登入" />
				<input class="pf-button btn btn-primary btn-cancel" type="button" name="cancel" value="取消" />
			</label>
		</div>
		<div class="pf-element">
			<label>
				<span class="pf-label text-primary"></span>
			</label>
			<div name="message-body" class="set-kid-message"></div>
		</div>
	</form>
</div>
<div id="magic-dialog" title="<bean:message key='robot.magicBtn.title'/>" style="display:none; z-index:999; ">
</div>
<jsp:include page="../footer-opinion.jsp" />
	<script type="text/javascript">
	 var qaChannel = '';
     if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
    	 qaChannel = 'app';
    }
    (function (factory) {
        if (typeof define === 'function' && define.amd) {
            // AMD. Register as an anonymous module.
            define(['jquery'], factory);
        } else {
            // Browser globals
            factory(jQuery);
        }
    }(function ($) {
      $.timeago = function(timestamp) {
        if (timestamp instanceof Date) {
          return inWords(timestamp);
        } else if (typeof timestamp === "string") {
          return inWords($.timeago.parse(timestamp));
            } else if (typeof timestamp === "number") {
                return inWords(new Date(timestamp));
        } else {
          return inWords($.timeago.datetime(timestamp));
        }
      };
      var $t = $.timeago;

      $.extend($.timeago, {
        settings: {
          refreshMillis: 60000,
          allowFuture: false,
                localeTitle: false,
                cutoff: 0,
          strings: {
            prefixAgo: null,
            prefixFromNow: null,
                    suffixAgo: "<bean:message key='global.front'/>",
                    suffixFromNow: "<bean:message key='global.ago'/>",
                    seconds: "<bean:message key='less.one.mintues'/>",
                    minute: "<bean:message key='about.one.minutes'/>",
                    minutes: "%d<bean:message key='global.mintues.one'/>",
                    hour: "<bean:message key='global.about'/>1<bean:message key='global.hour'/>",
                    hours: "<bean:message key='global.about'/>%d<bean:message key='global.hour'/>",
                    day: "1<bean:message key='global.sky'/>",
                    days: "%d<bean:message key='global.sky'/>",
                    month: "1<bean:message key='global.one.day'/>",
                    months: "%d<bean:message key='global.one.day'/>",
                    year: "1<bean:message key='global.year'/>",
                    years: "%d<bean:message key='global.year'/>",
                    wordSeparator: "",
            numbers: []
          }
        },
        inWords: function(distanceMillis) {
          var $l = this.settings.strings;
          var prefix = $l.prefixAgo;
          var suffix = $l.suffixAgo;
          if (this.settings.allowFuture) {
            if (distanceMillis < 0) {
              prefix = $l.prefixFromNow;
              suffix = $l.suffixFromNow;
            }
          }

                var seconds = Math.abs(distanceMillis) / 1000;
          var minutes = seconds / 60;
          var hours = minutes / 60;
          var days = hours / 24;
          var years = days / 365;

          function substitute(stringOrFunction, number) {
            var string = $.isFunction(stringOrFunction) ? stringOrFunction(number, distanceMillis) : stringOrFunction;
            var value = ($l.numbers && $l.numbers[number]) || number;
            return string.replace(/%d/i, value);
          }

          var words = seconds < 45 && substitute($l.seconds, Math.round(seconds)) ||
            seconds < 90 && substitute($l.minute, 1) ||
            minutes < 45 && substitute($l.minutes, Math.round(minutes)) ||
            minutes < 90 && substitute($l.hour, 1) ||
            hours < 24 && substitute($l.hours, Math.round(hours)) ||
                    hours < 42 && substitute($l.day, 1) ||
                    days < 30 && substitute($l.days, Math.round(days)) ||
                    days < 45 && substitute($l.month, 1) ||
                    days < 365 && substitute($l.months, Math.round(days / 30)) ||
                    years < 1.5 && substitute($l.year, 1) ||
                    substitute($l.years, Math.round(years));

                var separator = $l.wordSeparator || "";
                if ($l.wordSeparator === undefined) { separator = " "; }
                return $.trim([prefix, words, suffix].join(separator));
        },
        parse: function(iso8601) {
          var s = $.trim(iso8601);
                s = s.replace(/\.\d+/,""); // remove milliseconds
                s = s.replace(/-/,"/").replace(/-/,"/");
                s = s.replace(/T/," ").replace(/Z/," UTC");
          s = s.replace(/([\+\-]\d\d)\:?(\d\d)/," $1$2"); // -04:00 -> -0400
          return new Date(s);
        },
        datetime: function(elem) {
                var iso8601 = $t.isTime(elem) ? $(elem).attr("datetime") : $(elem).attr("title");
                return $t.parse(iso8601);
            },
            isTime: function(elem) {
          // jQuery's `is()` doesn't play well with HTML5 in IE
                return $(elem).get(0).tagName.toLowerCase() === "time"; // $(elem).is("time");
          }
        });

        // functions that can be called via $(el).timeago('action')
        // init is default when no action is given
        // functions are called with context of a single element
        var functions = {
            init: function(){
                var refresh_el = $.proxy(refresh, this);
                refresh_el();
                var $s = $t.settings;
                if ($s.refreshMillis > 0) {
                    setInterval(refresh_el, $s.refreshMillis);
          }
            },
            update: function(time){
                $(this).data('timeago', { datetime: $t.parse(time) });
                refresh.apply(this);
          }
        };

        $.fn.timeago = function(action, options) {
            var fn = action ? functions[action] : functions.init;
            if(!fn){
                throw new Error("Unknown function name '"+ action +"' for timeago");
        }
            // each over objects here and call the requested function
            this.each(function(){
                fn.call(this, options);
      });
            return this;
      };

      function refresh() {
        var data = prepareData(this);
            var $s = $t.settings;

        if (!isNaN(data.datetime)) {
                if ( $s.cutoff == 0 || distance(data.datetime) < $s.cutoff) {
          $(this).text(inWords(data.datetime));
        }
            }
        return this;
      }

      function prepareData(element) {
        element = $(element);
        if (!element.data("timeago")) {
          element.data("timeago", { datetime: $t.datetime(element) });
          var text = $.trim(element.text());
                if ($t.settings.localeTitle) {
                    element.attr("title", element.data('timeago').datetime.toLocaleString());
                } else if (text.length > 0 && !($t.isTime(element) && element.attr("title"))) {
            element.attr("title", text);
          }
        }
        return element.data("timeago");
      }

      function inWords(date) {
        return $t.inWords(distance(date));
      }

      function distance(date) {
        return (new Date().getTime() - date.getTime());
      }

      // fix for IE6 suckage
      document.createElement("abbr");
      document.createElement("time");
    }));
	</script>
	<%
	if (qaCtx.getTenant().getEnableAutoLearning()) {
	%>
	<script id="testResponsePanel" type="x-tmpl-mustache">
		<div class="test-panel" data-ts="{{ts}}">
		<!--span data-flag="1" class="glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span-->
		<span data-flag="2" class="glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span>
		<span data-flag="3" class="glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span>
		<!--span data-flag="4" class="glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span-->
		<span data-flag="5" class="glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span>
		<!--span data-flag="6" class="glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span-->
	    </div>
	</script>
	<%
	}
	else {
	%>
	<script id="testResponsePanel" type="x-tmpl-mustache">
		<div class="test-panel" data-ts="{{ts}}">
		<span data-flag="1" class="glyphicon glyphicon-star" title="<bean:message key='test.record.well'/>" aria-hidden="true" data-toggle="tooltip" data-placement="top"></span>
		<span data-flag="2" class="glyphicon glyphicon-thumbs-up" title="<bean:message key='test.record.good'/>" aria-hidden="true"></span>
		<span data-flag="3" class="glyphicon glyphicon glyphicon-pencil" title="<bean:message key='test.record.no.answer'/>" aria-hidden="true"></span>
		<span data-flag="4" class="glyphicon glyphicon-ok" title="<bean:message key='test.record.no.expect'/>" aria-hidden="true"></span>
		<span data-flag="5" class="glyphicon glyphicon-thumbs-down" title="<bean:message key='test.record.bad'/>" aria-hidden="true"></span>
		<span data-flag="6" class="glyphicon glyphicon-exclamation-sign" title="<bean:message key='test.record.worst'/>" aria-hidden="true"></span>
	    </div>
	</script>
	<%
	}
	%>

    <script>
      var testerMode = <%= qaCtx.getTenant().getEnableDebug() %>;
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
    	  if (typeof(cmd) == 'undefined') {
    		  cmd = $(this).text();
    	  }
		  $("#message_body").val(cmd);
		  submitQuestion(cmd, 'string', '<%= QAContext.EVENT_SOURCE_TYPE_BUTTON %>', eventSource);
      });

      $(".chat .new-message").live('submit', function(e) {
        submitQuestion($("#message_body").val(), 'string');
        return e.preventDefault();
      });

      $('.kms').live('click', function(e) {
        	var that = this;
        	e.preventDefault();
        	$.post('<%=qaCtx.getTenant().getKmsUrl() + "/api/getTicket.jsp"%>', {account: '<%=admUser.getLoginName()%>', format: 'json'}, function(data) {
        		window.open($(that).attr('data-url') + '&ticket=' + data.ticket, "_blank");
        	}, 'jsonp');
      });

      $(".test-panel span").live('click', function(e) {
    	  var $this = $(this);
    	  var choosed = $this.parent().attr("data-choosed");
    	  if (typeof choosed !== typeof undefined) {
    		  if (!confirm("<bean:message key='have.chosen.determine.modify'/>")) {
    		  	return;
    		  }
    	  }
    	  var ts = $this.parent().attr("data-ts");
    	  var flag = parseInt($this.attr("data-flag"));

    	  var thisData = histories[ts];

    	  $this.siblings().removeClass("text-success");
    	  $this.siblings().removeClass("text-danger");

    	  switch (flag) {
    	  case 1:
    	  case 2:
    		  $this.addClass("text-success");
    		  /*if (!confirm("<bean:message key='confirmation.sent'/>")) {

        		  $this.removeClass("text-success");
    			  return;
    		  }*/

    		  if (thisData.hasOwnProperty('kid')) {
        		  thisData["correctAnswer"] = thisData.kid;
    		  }

    		  break;
    	  case 3:
    		  $this.addClass("text-success");
    		  var des = prompt("<bean:message key='sure.input.mes'/>", "");
    		  var myReg =/^(?!.*[^\u4e00-\u9fa5a-zA-Z0-9-,.，。])/;
    		 
//     		  if (!confirm("<bean:message key='sure.select.this.option'/>")) {
//         		  $this.removeClass("text-success");
//     			  return;
//     		  }

    		  if (thisData.hasOwnProperty('kid')) {
        		  thisData["correctAnswer"] = thisData.kid;
    		  }
				
    		  if(!(myReg.test(des))){
    			  alert("<bean:message key='sure.notspecial.sign'/>"); 
    			  $this.removeClass("text-success");
    		  }else{
    			  thisData["des"] = des;
    		  }
    		  break;
    	  case 4:
    	  case 5:
    	  case 6:
    		  $this.addClass("text-danger");
    		  var correctAns = prompt("<bean:message key='input.correct.asnwer'/>", "0");
    		  var correctAnsDes = '';
    		  if (!correctAns) {
        		  $this.removeClass("text-danger");
    			  return;
    		  }
    		  else if (isNaN(parseInt(correctAns))) {
    			  alert("[" + correctAns + "] <bean:message key='is.not.number'/>");
        		  $this.removeClass("text-danger");
    			  return;
    		  }
    		  
    		  thisData["correctAnswer"] = correctAns;
    		  if(thisData["correctAnswer"] == '0'){
    			  $buf = "<select name='qaCategory'>";
    			  <% for (int i = 0; qaCategorys != null && i < qaCategorys.length; i++) { %>
    			  $buf += "<option value='<%=qaCategorys[i]%>'><%=qaCategorys[i]%></option>";
    			  <% } %>
    			  $buf += "</select>";
    			  correctAnsDes = myConfirm('請挑選建議的QA分類', $buf, function(data) {
    				  thisData["correctAnswerCategory"] = $('[name=qaCategory]').val();
    				  thisData["flag"] = flag;
    				 
    				  
    				  $.ajax({
    		        	  type: 'POST',
    		        	  url: 'qa-eval-log-ajax.jsp',
    		        	  dataType: 'json',
    		        	  data: {
    		        		  data: JSON.stringify(thisData)
    		        	  },
    		        	  success: function(resp) {
    		        		  if (resp.needLogin) {
    		        			  alert("<bean:message key='top.logout.ex2'/>");
    		        			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
    		        		  }
    		        	  }
    		          });
    			  });
    			
    		  }
    		  break;
    	  }
    	  $this.parent().attr("data-choosed", "true");
		  thisData["flag"] = flag;

	      $.ajax({
	      	  type: 'POST',
	       	  url: 'qa-eval-log-ajax.jsp',
	       	  dataType: 'json',
	       	  data: {
	       		  data: JSON.stringify(thisData)
	       	  },
	       	  success: function(resp) {
	       		  if (resp.needLogin) {
	       			  alert("<bean:message key='top.logout.ex2'/>");
	       			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
	       		  }
	       	  }
	      });

      });
      
      function checkIfStillLogin() {
    	  $.ajax({
    		  type: 'POST',
    	      url: 'login-check-ajax.jsp',
    	      success: function(resp) {
    	    	  if (resp.needLogin) {
    	    		  alert("<bean:message key='top.logout.ex2'/>");
    	    		  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
    	    	  }
    	      }
    	  });
      }

      function submitQuestion(q, inputType, eventSourceType, eventSource) {
    	  
    	  checkIfStillLogin();
    	  
    	  var chat, date, li, message, months, reply, scrollable, sender, timeago, qaCategoryEnable, selectQaChannelType, selectQaUserType;
          date = new Date();
          months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
          chat = $("#message_body").parents(".chat");
          message = q;
          currentQuestion = q;

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
               		  apikey: '<%=apikey%>',
               		  locale: locale,
               		  ch:selectQaChannelType
               	  };

            if (typeof(eventSourceType) != 'undefined') {
            	options['est'] = eventSourceType;
            }
            if (typeof(eventSource) != 'undefined') {
            	options['es'] = eventSource;
            }

			if($("#qaCategorySelect option:selected").text() != '') {
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
          	  url: '<%= request.getContextPath() %>/qa-ajax.jsp',
          	  dataType: 'json',
          	  data: options,
          	  error: function() {
					setTimeout(function() {$('.loading-panel').hide();}, 100);
              },
          	  success: function(resp) {
          			resp["ts"] = (new Date()).getTime();

                    reply = $("li.message-template-robot").clone();
                    reply.removeClass("message-template-robot");

                    if (resp.hasOwnProperty('output')) {
                    	$(".btnTts").remove();
                    	
                    	if (resp.hasOwnProperty('webRM')) {
                    		reply.find(".body").html("&nbsp; <button class='btn btn-default btn-xs btnTts' data-loading-text='<bean:message key="load.and.play..."/>' data-id='<%= qaId %>'><span class='glyphicon glyphicon-bullhorn'></span><bean:message key="speak.out"/></button>");
                    	}
                    	else {
                    		reply.find(".body").html(resp.output + "&nbsp; <button class='btn btn-default btn-xs btnTts' data-loading-text='<bean:message key="load.and.play..."/>' data-id='<%= qaId %>'><span class='glyphicon glyphicon-bullhorn'></span><bean:message key="speak.out"/></button>");
                    	}
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

                    if (resp.hasOwnProperty('kid')) {
                		var anchor = $('<a/>');
                		anchor.attr('href', "<%= request.getContextPath() %>/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-" + resp.kid);
                		anchor.attr('target', "_new");
                		anchor.html("<small class='text-success'><bean:message key='num'/>" + resp.kid + "</small>");
                		anchor.appendTo(reply.find(".body"));
                    }

                    if (resp.hasOwnProperty('inputType')) {
                    	$("#message_body").attr('inputType', resp.inputType);
                    }
                    inputType = $("#message_body").attr("inputType");

                    if (inputType == 'password' || inputType == 'stringMask') {
                    	$('#message_body').remove();
                    	$('<input>').attr('id', 'message_body').attr('name', 'message[body]').attr('placeholder', 'Type your message here...').attr('inputType', inputType).attr('type', 'password').addClass('form-control textarea').appendTo($('#inputDiv'));
                    }
                    else {
                    	$('#message_body').remove();
                    	$('<textarea>').attr('id', 'message_body').attr('name', 'message[body]').attr('placeholder', 'Type your message here...').attr('inputType', inputType).addClass('form-control textarea').appendTo($('#inputDiv'));
                    }

                    resp["flag"] = 0; // 先儲存一份
                    histories[resp.ts] = resp;
                    currentTimestamp = resp.ts;

                    //console.log(resp);

                    $.ajax({
                  	  type: 'POST',
                  	  url: 'qa-eval-log-ajax.jsp',
                  	  dataType: 'json',
                  	  data: {
                  		  data: JSON.stringify(resp)
                  	  },
                  	  success: function(initLog) {
                  		    if (initLog.hasOwnProperty("needLogin") && initLog.needLogin) {
                  			  alert("<bean:message key='top.logout.ex2'/>");
                  			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
                  		    }

                  		    resp["logId"] = initLog.logId;

                  		    if (testerMode) {
                  		    	var template = $('#testResponsePanel').html();
                  			  	Mustache.parse(template);
                  			  	var panel = Mustache.render(template, resp);
								reply.find(".body").append(panel);
								$(panel).children('span').tooltip();
                  		    }

							if (resp.hasOwnProperty('otherPossibleQuestions')) {
								var ul = $("<ol class='other-questions'></ol>");
								var otherQuestions = resp.otherPossibleQuestions;
								for (var i=0; i < otherQuestions.length; i++) {
									var thisLi = $("<li class='other-questions-li' style='border-bottom: 0px; padding: 0; padding-left: 10px;'></li>");

									var clickToAsk = $('<span/>');
									clickToAsk.addClass("question-clickable");
									clickToAsk.text(otherQuestions[i].question);
									clickToAsk.appendTo(thisLi);

									var anchor = $('<a/>');
									anchor.attr('href', "<%= request.getContextPath() %>/wiseadm/qaDataEditor.jsp?id=COMMON_SENSE-" + otherQuestions[i].kid);
									anchor.attr('target', "_new");
									anchor.text("(<bean:message key='num'/>" + otherQuestions[i].kid + ")");
									anchor.appendTo(thisLi);

									thisLi.appendTo(ul);
								}
								reply.find(".body").append("<BR><bean:message key='question.clickable'/>");
								reply.find(".body").append(ul);
							}

							if(resp.hasOwnProperty('kmsDocs')) {
		                       reply.find(".body").append('<br><span style="color:blue;"><bean:message key="kms.suggest.docs"/><br>' + resp.kmsDocs + '</span>');
		                    }

							setTimeout(
								(function(reply, webRM) {
									return function() {
							            date = new Date();
							            timeago = reply.find(".timeago");
							            timeago.attr("title", "" + (date.getFullYear()) + "-" + (month<10 ? '0' : '') + month + "-" + (date_day<10 ? '0' : '' ) + date_day + " " + (date.getHours()) + ":" + (date.getMinutes()) + ":" + (date.getSeconds()) + " +0800");
							            timeago.text("" + months[date.getMonth()] + " " + (date.getDate()) + ", " + (date.getFullYear()) + " " + (date.getHours()) + ":" + (date.getMinutes()));
							            setTimeAgo(timeago);

							            appendFunc = function(newRM, idx) {
                                            setTimeout( (function(rm) {
                                                return function() {
                                                    $(scrollable.find("ul.main-stream")).append(rm);
                                                    $(scrollable).slimScroll({
                                                        scrollTo: scrollable.prop('scrollHeight') + "px"
                                                    });
                                                    
                                                    if (idx == 0 && testerMode) {
                                                        var template = $('#testResponsePanel').html();
                                                        Mustache.parse(template);
                                                        var panel = Mustache.render(template, resp);
                                                        rm.find('.body').append(panel);
                                                        $(panel).children('span').tooltip();
                                                    }
                                                    if (typeof(ImageSwiper) != 'undefined' && Object.entries(ImageSwiper.eventsListeners).length > 0){
                                                        ImageSwiper.destroy();
                                                    }
                                                    ImageSwiper = new Swiper('.card-B1-swiper',
                                                            {
                                                                slidesPerView: 1,
                                                                loop: true,
                                                                spaceBetween: 60,
                                                                pagination: {
                                                                    el: '.swiper-pagination',
                                                                    type: 'bullets',
                                                                },
                                                                navigation: {
                                                                    nextEl: '.swiper-button-next',
                                                                    prevEl: '.swiper-button-prev',
                                                                }
                                                            });
                                                };
                                            })(newRM), (idx+1)*300);
                                        }
                                        
                                        genEmptyRM = function() {
                                            emptyRM = $("li.message-template-robot").clone();
                                            emptyRM.removeClass("message-template-robot");
                                            emptyRM.find('.body').html("");
                                            return emptyRM;
                                        }
                                         
                                        var newRM = genEmptyRM();
                                        
                                        if (genRichMessages(appendFunc, newRM, resp)) {
                                            // Good to go
                                            
                                            if (resp.hasOwnProperty('confidence')) {
                                                $("<br><small class='text-primary' style='margin-right: 10px;'><bean:message key='global.confidence.level'/>：" + confidenceMap[resp.confidence] + " (" + resp.confidenceFactor + ")</small>").appendTo(newRM.find(".body"));
                                            }

                                            if (resp.hasOwnProperty('kid')) {
                                                var anchor = $('<a/>');
                                                anchor.attr('href', "<%= request.getContextPath() %>/wiseadm/qaDataEditor.jsp?action=edit&id=COMMON_SENSE-" + resp.kid);
                                                anchor.attr('target', "_new");
                                                anchor.html("<small class='text-success'><bean:message key='num'/>" + resp.kid + "</small>");
                                                anchor.appendTo(newRM.find(".body"));
                                            }                                           
                                        }
                                        else {
                                            scrollable.find("ul.main-stream").append(reply);
                                            $(scrollable).slimScroll({
                                                scrollTo: scrollable.prop('scrollHeight') + "px"
                                            });
                                                
                                            if ($('#btnToggleAutoTTS').prop('checked')) {
                                                $(".btnTts").click();
                                            }
                                            reply.effect("highlight", {}, 300);
                                        }
                                    };
                                })(reply, resp), 100);
                  	  },
                  	  complete: function() {
							setTimeout(function() {$('.loading-panel').hide(); $('#message_body').focus();}, 100);
                  	  }
                    });

          	  }
            });
          }
      }
      
      if(typeof(webkitSpeechRecognition) === 'function') {
    	  var recognizer = new webkitSpeechRecognition();
    	  recognizer.continuous = true;
    	  recognizer.interimResults = true;
          var recognizerRecording = false;
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
    		      	    	$("#message_body").val(result[0].transcript);
    		      	        if(result.isFinal) {
    	   	 		            submitQuestion(result[0].transcript, 'mic');
    		      	        }
    		      	    }
    		      	};
    	      		recognizerRecording = true;
    		      	recognizer.start();
    	      	}
    	    });
      }
      else {
    	  $('#btnMic').hide();
      }
      
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

      <%--
      --%>

      $(document).on('click', '.btnToggle', function() {
    	 $this = $(this);
    	 $divId = $this.attr('data-target-div');
    	 $('#' + $divId).toggle();
      });

      var lastDFTS = -1;

      function demoFlowController() {
    	 $.ajax({
    		 url: 'demoFlow-status-ajax.jsp',
    		 data: { action: "status" },
    	 	 success: function(dfObj) {
    	 		 if (dfObj.enable && dfObj.clientAutoSendOutMode) {
    	 			 if (dfObj.currentOffset != -1 && dfObj.currentTimestamp > lastDFTS) {
    	 				 lastDFTS = dfObj.currentTimestamp;
    	 				 $("#message_body").val(dfObj.questions[dfObj.currentOffset]);
    	 		         submitQuestion($("#message_body").val(), 'string');
    	 			 }
    	 		 }
    	 	 }
    	 });
      }

      var dfTimer;

      $(document).ready(function() {
    	  <% if (qaCtx.getTenant().getEnableDebug()) { %>
    	  demoFlowController();
    	  dfTimer = setInterval(demoFlowController, 1000);
    	  <% } %>

    	  $( "#magic-dialog").dialog({
    	  	minWidth: 600,
    	  	minHeight: 600,
    	  	height: 600,
    	  	autoOpen: false
    	  });
    	  
    	  $(document).on('keydown', '#message_body', function(e) { 
              if (e.which === 13) { 
                e.preventDefault(); 
                submitQuestion($("#message_body").val(), 'string'); 
              } 
          }); 
    	  
    	  $('#message_body').focus();
      });

      var systemNlp = '';

      function testCallback(question, correctAnwerId, stat, callbackWhenFinished) {
      	var $stat = stat;
      	if (typeof($stat) == 'undefined') {
      		$stat = {good:0, bad:0, unknown:0, total:1};
      	}

    	return (function($correctAnwerId, $question) {
         	$.ajax({
     	   		url: '<%= request.getContextPath() %>/qa-ajax.jsp',
     	   		dataType: 'json',
     	   		data: {
     	     		  q: $question,
     	     		  testMode: true,
     	     		  ch: channel,
               		  userType: userType,
     	     		  ftc: false,
     	    		  tid: <%= qaCtx.getTenant().getId() %>,
     	      		  html: true
     	   		},
     	   		error: function() {
     	   			alert('<bean:message key="detection.network"/>');
     	   		},
     	   		success: function(resp) {
     	   			var t = moment((new Date()).getTime());
     					var kid;
     					//var tmp = "";
     					if (resp.hasOwnProperty("kid")) {
     						kid = resp.kid.toString();
     					}
     					if ($correctAnwerId == kid) {
     						addClass = "text-success";
     						$stat.good++;
     					}
     					else if (isNaN($correctAnwerId)){
     						addClass = "text-warning";
     						$stat.unknown++;
     					}
     					else {
     						addClass = "text-danger";
     						$stat.bad++;
     						//tmp += "<br>(" + $correctAnwerId + "/" + kid + ")";
     					}
     	     		},
      	   		complete: function() {
      				$(".timeago").timeago();

      				if (typeof(callbackWhenFinished) == 'function') {
      					callbackWhenFinished($stat);
      				}
      	   		}
      	    });
          })(correctAnwerId, question);
      }

      <%--
      $(document).on('click', 'button[name="btnMagic"]', function() {
      	$thisBtn = $(this);
      	$thisTr = $(this).parent().parent();
        $question = currentQuestion;
      	$correctAnswerId = $(this).attr("data-kid");
      	$dialog = $("#magic-dialog");
      	$dialog.html("");

      	$msg1 = $("<h3></h3>");
      	$msg2 = $("<h3></h3>");
      	$msg3 = $("<h3></h3>");
      	$msgFinalStatus = $("<h3></h3>");

      	$title = "機器學習"
      	$buf = "<h4>本功能將會將自動學習「<span class='text-danger'>" + $question + "</span>」的句型<br>";
      	$buf += "並且將其與問題編號「<span class='text-danger'>" + $correctAnswerId + "</span>」產生關連<br>";
      	$buf += "當整個機器學習完成後會自動進行一次測試<br>";
      	$buf += "作業時間長短視該題的複雜程度而有不同<br>";
      	$buf += "<span class='text-danger'>請務必先確認過上述題號為正確答案</span><br>";
      	$buf += "正確的問答編號：<input type='text' name='correctAnswerId' value='"+$correctAnswerId+"' /><br>";
      	$buf += "即將開始學習<input type='hidden' name='userNlp' value='"+systemNlp+"' /><br>";
      	$buf += "按下Ok開始進行...<br></h4>";

      	myConfirm($title, $buf, function() {
      		$msg1.appendTo($dialog);
      		$msg2.appendTo($dialog);
      		$msg3.appendTo($dialog);
      		$msgFinalStatus.appendTo($dialog);

      		$dialog.dialog('open');
      		if($('[name=correctAnswerId]').val()) {
      			$correctAnswerId = $('[name=correctAnswerId]').val();
      			$userNlp = $('[name=userNlp]').val();
      		}

      		$msg1.typed({
      	        strings: ["現在開始進行自然語言分析... ^500"],
      	        typeSpeed: 1,
      	        onStringTyped: function() {
      	        	$.ajax({

      	    	    }).then(
      	    	    	function(resp) {
      	    				$msg2.typed({
      	    					strings: ["開始檢測問題「" + $correctAnswerId + "」... ^500"],
      	    					typeSpeed: 0,
      					        onStringTyped: function() {
      					        	$.ajax({
      					    			url: 'qa-nlp-test-ajax.jsp',
      					    			dataType: 'json',
      					    			data: {
      					        		  q: $question,
      					        		  replaceSyn: true,
      					        		  checkAndAddToKid: $correctAnswerId,
      					        		  userNlp: $userNlp,
      					        		  ts: Math.random()
      					    			},
      					    	    }).then(
      					    	    	function(resp) {
      					    	    		if (resp.checkAndAddResult.status == "success") {
      						    				$msg3.typed({
      						    					strings: ["確認無重複問法... <br> ^500 開始學習此種句型<br> ^500 正在等待句型模型建置完畢後進行測試... ^100"],
      						    					typeSpeed: 0,
      										        onStringTyped: function() {
      										        	var checkStatusTID;

      				      				    	    	function checkStatus() {
      			      				    	    			$.ajax({
      			      				    	    				url: "qa-build-status-ajax.jsp?t=" + new Date().getTime(),
      			      				    	    				dataType: "json",
      			      				    	    				data: {
      			      				    	    					docId: '<%= QAUtil.DATATYPE_COMMON_SENSE %>-' + $correctAnswerId
      			      				    	    				},
      			      				    	    				success: function(result) {
      			      				    	    					if (result.status == "DONE") {
      			      				    	    						stat = {good: 0, bad: 0, unknown: 0};
      														        	testCallback($question, $correctAnswerId, stat, function() {
      														        		if (stat.good) {
      														        			evalData = histories[currentTimestamp];
      														        			evalData["flag"] = 4;
      														        			evalData["correctAnswer"] = $correctAnswerId;

      														                    $.ajax({
      														                	  type: 'POST',
      														                	  url: 'qa-eval-log-ajax.jsp',
      														                	  dataType: 'json',
      														                	  data: {
      														                		  data: JSON.stringify(evalData)
      														                	  },
      														                	  success: function(resp) {
      														                		  if (resp.needLogin) {
      														                			  alert("<bean:message key='top.logout.ex2'/>");
      														                			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
      														                		  }
      														                	  }
      														                    });

      														        			$msgFinalStatus.typed({
      							      						    					strings: ["複測結果成功! ^500"],
      							      						    					typeSpeed: 0,
      							      										        onStringTyped: function() {

      							      										        },
      							      						    				});
      														        		}
      														        		if (stat.bad) {
      														        			evalData = histories[currentTimestamp];
      														        			evalData["flag"] = 6;
      														        			evalData["correctAnswer"] = $correctAnswerId;

      														                    $.ajax({
      														                	  type: 'POST',
      														                	  url: 'qa-eval-log-ajax.jsp',
      														                	  dataType: 'json',
      														                	  data: {
      														                		  data: JSON.stringify(evalData)
      														                	  },
      														                	  success: function(resp) {
      														                		  if (resp.needLogin) {
      														                			  alert("<bean:message key='top.logout.ex2'/>");
      														                			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
      														                		  }
      														                	  }
      														                    });

      														        			$msgFinalStatus.typed({
      							      						    					strings: ["複測結果失敗! 回報後端進一步查核問題！^500"],
      							      						    					typeSpeed: 0,
      							      										        onStringTyped: function() {

      							      										        },
      							      						    				});
      														        		}
      														        		else {

      														        		}
      														        	});
      			      				    	    						clearInterval(checkStatusTID);
      			      				    	    					}
      			      				    	    				}
      			      				    	    			});
      				      				    	    	}

      				      				    	    	checkStatusTID = setInterval(checkStatus, 1000);
      										        },
      						    				});
      						    	    	}
      					    	    		else if (resp.checkAndAddResult.status == "error") {
							        			evalData = histories[currentTimestamp];
							        			evalData["flag"] = 6;
							        			evalData["correctAnswer"] = $correctAnswerId;

							                    $.ajax({
							                	  type: 'POST',
							                	  url: 'qa-eval-log-ajax.jsp',
							                	  dataType: 'json',
							                	  data: {
							                		  data: JSON.stringify(evalData)
							                	  },
							                	  success: function(resp) {
							                		  if (resp.needLogin) {
							                			  alert("<bean:message key='top.logout.ex2'/>");
							                			  document.location = "<%= request.getContextPath() %>/wiseadm/login.jsp";
							                		  }
							                	  }
							                    });

      						    				$msg3.typed({
      						    					strings: ["儲存失敗... <br> ^500 錯誤訊息：" + resp.checkAndAddResult.errorMsg + " ^500"],
      						    					typeSpeed: 0,
      										        onStringTyped: function() {

      										        },
      						    				});
      					    	    		}
      					    	    		else {
      					    	    			alert(JSON.stringify(resp));
      					    	    		}
      					    			});
      					        },
      	    				});
      	    			});
      				}
            });
      	});
      });

      $(document).on('click', "button[name=btnNlp]", function(e) {
      	var $thisBtn = $(this);
         	$question = $thisBtn.parent().siblings('td[name="question"]').text();

          $.ajax({
      			url: 'qa-nlp-test-ajax.jsp',
      			dataType: 'json',
      			data: {
          		  q: $question,
      			  replaceSyn: true,
          		  ts: Math.random()
      			},
      			error: function() {
      				//setTimeout(function() {$('.loading-panel').hide();}, 1000);
      			},
      			success: function(resp) {
      				alert(resp.nlp);
      				systemNlp = resp.nlp;
      			},
      	    	complete: function() {
      	    	}});
      	return false;
      });
      --%>

      $(document).on('click', '.btnUserLoginSubmit', function() {
    	  userType = 'logined';
    	  alert("登入完成");
    	  ulp.remove();
      });

      $('#btnUserLogin').click(function() {
    	  if (ulpShown) {
    			ulpShown = false;
    			ulp.remove();
    		}
    		else {
    			ulpShown = true;
    			ulp = new PNotify({
    				    text: $('#user-login-panel').html(),
    				    icon: false,
    				    width: '400px',
    				    hide: false,
    				    confirm: {
    				        confirm: true,
    				        buttons: [{
    				            text: '<bean:message key="global.cancel"/>',
    				            addClass: 'hide btn-primary',
    				            click: function(notice) {
    				            	ulpShown = false;
    				                notice.remove();
    				            }
    				        },{
    				            text: '<bean:message key="not.need.exist.close"/>',
    				            addClass: 'hide btn-primary',
    				            click: function(notice) {
    				            	ulpShown = false;
    				                notice.remove();
    				            }
    				        }
    				        ]
    				    },
    				    before_open: function(PNotify) {
    			            // Position this notice in the center of the screen.
    			            PNotify.get().css({
    			                "top": ($(window).height() / 3) - (PNotify.get().height() / 2),
    			                "left": ($(window).width() / 2) - (PNotify.get().width() / 2)
    			            });
    				    },
    				    buttons: {
    				        closer: false,
    				        sticker: false
    				    },
    				    insert_brs: false
    				});
    		}
      });
    </script>
<div class='loading-panel'></div>
<div id="divAudio_Player">
    <audio id="audio_player" src=""></audio>
</div>
</body>
</html>

