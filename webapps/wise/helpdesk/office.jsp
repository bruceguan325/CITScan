<%@ page import="java.util.Locale" %>
<%@ page import="com.intumit.message.MessageUtil" %>
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
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
String nowStr = sdf.format(java.util.Calendar.getInstance().getTime());
String nickname = "Guest";
Locale locale = (Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE");

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
        if(request.getSession().getAttribute(Globals.LOCALE_KEY) != null) {
        	ctx.setLocale(HitHotLocale.guessHitHotLocaleByJavaLocale((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"))); 
        }
        QAContextManager.put(qaId, ctx);
		session.setAttribute("tenant", t);
		session.setAttribute("qaId", qaId);
		response.sendRedirect(request.getContextPath() + "/helpdesk/office.jsp?qaId=" + qaId);
	}
	else {
		if(HitHotLocale.ja_JP.getLocale().equals(request.getSession().getAttribute("org.apache.struts.action.LOCALE"))) {
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
<html lang="utf-8">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>SmartRobot for Office 365 HelpDesk</title>
<link rel="stylesheet" href="<%=request.getContextPath()%>/helpdesk/css/common.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/helpdesk/css/botchat.css">
<link rel="stylesheet" href="<%=request.getContextPath()%>/helpdesk/css/boticon.css">
<script src="<%= request.getContextPath() %>/script/jquery-1.12.4.min.js"></script>
<script src="<%= request.getContextPath() %>/script/mustache.min.js"></script>
<script src="<%= request.getContextPath() %>/assets/javascripts/plugins/slimscroll/jquery.slimscroll.min.js" type="text/javascript"></script>
<script src="<%= request.getContextPath() %>/assets/javascripts/jquery/jquery-ui.min.js" type="text/javascript"></script>
<style type="text/css">
#header #header-title {
background: linear-gradient(to right, #2d73ba 0%, #3ba5ff 100%);
}
#header #header-title p {
    text-shadow: none;
}
.chatbot-area .wc-message-from-bot .wc-message-content .chat-main {
    border: 2px solid #03A9F4;
}
.chatbot-area .wc-message-from-me .chat-statement .chat-statement-inner .chat-main {
    background: #ffefbf;
}
.chatbot-area .wc-message-from-bot .character-icon {
    display: block;
    margin-top: 10px;
    width: 70px;
    height: 70px;
    float: left;
    background: url(./images/icn_intumit_service.png) 0 0 no-repeat;
}
.chatbot-area .wc-console .btn-question a {
    display: inline-block;
    width: 140px;
    height: 43px;
    overflow: hidden;
    box-sizing: border-box;
    vertical-align: middle;
    margin: 0 -4px;
    padding: 0;
    background: #2d73ba;
    background: -webkit-linear-gradient(top, #3ba5ff 0%, #2d73ba 100%);
    background: linear-gradient(to bottom, #3ba5ff 0%, #2d73ba 100%);
    border-radius: 0 10px 10px 0;
    font-size: 18px;
    font-weight: bold;
    line-height: 44px;
    color: #fff;
    text-align: center;
    cursor: pointer;
    text-shadow: 0 0 2px #2d73ba;
}
.chatbot-area .wc-console .btn-question a:hover {
	background: #3ba5ff;
    background: -webkit-linear-gradient(top, #3ba5ff 10%, #2d73ba 90%);
    background: linear-gradient(to bottom, #3ba5ff 10%, #2d73ba 90%);
}
.chatbot-area .wc-console .auto-suggest input {
    margin-top: 1px;
}
.chatbot-area .wc-console .auto-suggest input:focus {
    border: 1px solid #3ba5ff;
}
.chatbot-area .chat-main li:before {
    display: block;
    width: 13px;
    height: 13px;
    content: "";
    border-radius: 5em;
    background: #84d9ff;
    position: absolute;
    top: 6px;
    left: 1px;
    box-shadow: 1px 1px 1px rgba(0, 0, 0, 0.26);
}
.chatbot-area .wc-message-from-bot .wc-message-content .chat-main {
    margin: 5px 10px 10px -12px;
}
.chatbot-area .wc-message-from-me .chat-statement .chat-statement-inner .chat-main {
    margin: 5px -10px 10px 10px;
}
.loading-panel {
    width:100%; height:100%; display: none;
    position: fixed; top:0; left:0; z-index:999;
    background: rgba(255,255,255,0.5) url('<%= request.getContextPath() %>/img/loading3.gif') no-repeat center center;
}
</style>
<script id="bot-message" type="x-tmpl-mustache">
  <div class="wc-message-wrapper list">
    <div class="wc-message wc-message-from-bot">
      <div class="wc-message-content">
        <div class="character-icon"></div>
        <div class="chat-statement">
          <div class="chat-statement-inner">
            <div class="chat-main">
              <span class="format-html"></span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="wc-message-from wc-message-from-bot"><span>Bot</span></div>
  </div>
</script>
<script id="user-message" type="x-tmpl-mustache">
  <div class="wc-message-wrapper list">
    <div class="wc-message wc-message-from-me">
      <div class="wc-message-content">
        <div class="character-icon"></div>
        <div class="chat-statement one-line-only">
          <div class="chat-statement-inner">
            <div class="chat-main">
              <span class="format-plain"></span>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="wc-message-from wc-message-from-me"><span>You</span></div>
  </div>
</script>
</head>
<body>
<div id="wrap">
  <header id="header">
    <p id="header-logo"><a itemprop="url" href="http://www.intumit.com" target="_blank"><img src="<%=request.getContextPath()%>/helpdesk/images/SmartRobot.png" height="60"></a></p>
    <div id="header-utility" class="clearfix">
      <div id="header-menu">
      </div>
    </div>
    <div id="header-title">
      <p>SmartRobot for Office 365 HelpDesk</p>
    </div>
  </header>
  <div id="main-container" class="clearfix">
    <main id="main">
      <div class="chatbot-area">
        <div id="BotChatElement">
          <div data-reactroot="" class="wc-app">
            <div class="wc-chatview-panel">
              <div class="wc-message-pane">
                <div class="wc-message-groups" style="height: 580px;" data-scrollable-height='580' data-scrollable-start='bottom'>
                  <div class="wc-message-group-content">  <!-- 訊息放置區 -->
                    <div class="wc-message-wrapper list">
                      <div class="wc-message wc-message-from-bot">
                        <div class="wc-message-content">
                          <div class="character-icon"></div>
                          <div class="chat-statement">
                            <div class="chat-statement-inner">
                              <div class="chat-main"><span class="format-html">こんにちは！大塚商会のパソコンくんです。<br>
                                お客様マイページへのログインについてお答えします。<br>
                                本日はどのようなご質問でしょうか？<br>
                                下記項目を選択いただくか、ご質問内容を入力してください。
                                <button class="osk-btn" data-command="imBack" value="お客様マイページにログインできない" type="button">お客様マイページにログインできない</button>
                                <button class="osk-btn" data-command="imBack" value="どこからログインして良いのかわからない" type="button">どこからログインして良いのかわからない</button>
                                <button class="osk-btn" data-command="imBack" value="お客様マイページでロックがかかってしまった" type="button">お客様マイページでロックがかかってしまった</button>
                                <br>
                                <button class="osk-btn" data-command="imBack" value="ＩＤについて" type="button">ＩＤについて</button>
                                <button class="osk-btn" data-command="imBack" value="パスワードについて" type="button">パスワードについて</button>
                                </span><!-- react-empty: 56 --><!-- react-empty: 57 --></div>
                            </div>
                          </div>
                        </div>
                      </div>
                      <div class="wc-message-from wc-message-from-bot"><span><!-- react-text: 60 -->Bot<!-- /react-text --></span></div>
                    </div>
                  </div>
                </div>
                <div class="wc-suggested-actions"><!-- react-empty: 26 --></div>
              </div>
              <div class="wc-console">
                <p class="help-popup hide"><!-- react-text: 29 -->こちらにどうぞ<!-- /react-text --><span class="note">（個人が特定できる情報の入力はご遠慮ください）</span></p>
                <div class="auto-suggest">
                  <p class="input-field">
                    <input id="message_body" class="new-message" placeholder="Type your message..." aria-autocomplete="list" aria-owns="candidate-list">
                  </p>
                  <div class="datalist hide">
                    <ul id="candidate-list" role="listbox">
                    </ul>
                  </div>
                </div>
                <p class="btn-question"><a id="sendMessage" role="button">Send</a></p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
  <div class='loading-panel'></div>
</div>
<script language="Javascript">
/* render 
var template = $(templateConfig.showDetail ? '#node-box-template-panel-body-detail' : '#node-box-template-panel-body-normal').html();
Mustache.parse(template);
var panelBody = Mustache.render(template, dataForRender);
*/
var qaChannel = '';
if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
  qaChannel = 'app';
}
var testerMode = false;
var qaId = '<%=StringEscapeUtils.escapeJavaScript(qaId) %>';
var histories = {};
var ulpShown = false;
var ulp;
var channel = 'web';
var userType = 'unknown';

var currentQuestion;
var currentTimestamp;
var faqOpened = true;

$(document).on('click', '.question-clickable', function(e) {
	  var cmd = $(this).attr('data-opnum');
	  $("#message_body").val(cmd);
	  submitQuestion();
});

$(document).on('click', '#sendMessage', function(e) {
  submitQuestion();
  return e.preventDefault();
});

function submitQuestion(q, forceInputType) { 
  var chat, date, li, message, months, reply, scrollable, sender, timeago, qaCategoryEnable, selectQaChannelType, selectQaUserType;
  message = $("#message_body").val();
  inputType = $("#message_body").attr("inputType");

  if(typeof(q) != 'undefined') {
    message = q;
    currentQuestion = q;
  }
  else {
  	currentQuestion = message;
  }
  if(typeof(forceInputType) != 'undefined') {
  	inputType = forceInputType;
  }

  $("#message_body").val("");

  if(message.length !== 0) {
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

	if($("#qaCategorySelect option:selected").text() != '') {
	  options.category = $("#qaCategorySelect option:selected").text();
	}

	var template = $('#user-message').html();
	Mustache.parse(template);
	var renderData = {};
    
    if (inputType == 'password' || inputType == 'stringMask') {
      renderData.question = "********";
    }
    else {
      renderData.question = message;
    }
	var panelBody = $(Mustache.render(template, renderData));
	panelBody.find('.format-plain').text(renderData.question);
	
    $(".wc-message-group-content").append(panelBody);
    scrollable = $(".wc-message-groups");
    $(scrollable).slimScroll({
      scrollTo: scrollable.prop('scrollHeight') + "px"
    });

	$('.loading-panel').show();

    $.ajax({
        type: 'POST',
    	url: '<%=request.getContextPath()%>/chats-ajax.jsp',
    	dataType: 'json',
    	data: options,
    	error: function() {
		  setTimeout(function() {$('.loading-panel').hide();}, 100);
        },
    	success: function(resp) {
    	  console.log(resp);
    	  resp["ts"] = (new Date()).getTime();

    	  template = $('#bot-message').html();
    	  Mustache.parse(template);
          renderData = {};

          if (resp.hasOwnProperty('output')) {
        	renderData.output = resp.output;
          }
          else {
            renderData.output = "<bean:message key='this.topic.has.not.yet.been.edited'/>";
          }
          
          reply = $(Mustache.render(template, renderData));
          reply.find('.format-html').append(renderData.output);

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
			  var thisLi = $("<li class='other-questions-li'></li>");
              var clickToAsk = $('<span/>');
						clickToAsk.addClass("question-clickable");
						clickToAsk.text(otherQuestions[i].question);
						clickToAsk.appendTo(thisLi);

						thisLi.appendTo(ul);
					}
					reply.find(".format-html").append("<BR><bean:message key='question.clickable'/>");
					reply.find(".format-html").append(ul);
				}

				setTimeout(appendReply, 100);
    	  },
    	  complete: function() {
				setTimeout(function() {$('.loading-panel').hide();}, 100);
  	  	  }
      });
    }

    function appendReply() {
      scrollable.find('.wc-message-group-content').append(reply);
      $(scrollable).slimScroll({
        scrollTo: scrollable.prop('scrollHeight') + "px"
      });

      return reply.effect("highlight", {}, 300);
    }
}
$(function(){
  $('.wc-message-groups').slimScroll({
  	height: '580px',
  });
});
</script>
</body>
</html>