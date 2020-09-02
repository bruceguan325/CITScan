<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
	import="java.util.*"
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
String qaId = ESAPI.encoder().encodeForHTML((String)session.getAttribute("qaId"));

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
if (qaCtx == null || t == null) {
	out.println("<h2>Session timeout, please restart chat again.</h2>");
	return ;
}
ServiceLogEntity log = ServiceLogEntity.getFromSession(qaCtx.getTenant(), qaId);

if (log == null) {
	log = ServiceLogEntity.log(qaCtx.getTenant(), request, "robot:cathay:chat", "" + System.currentTimeMillis(), ForwardToCrmRule.Status.ENTERING.name(), new JSONObject().toString(), null);
	ServiceLogEntity.setIntoSession(qaId, log);
}

QAUtil qu = QAUtil.getInstance(qaCtx.getTenant());
%>
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="utf-8" />
    <meta http-equiv="x-ua-compatible" content="IE=edge,chrome=1">
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title><%= t.getNotes() %></title>
    <link href="img/apple-touch-icon.png" rel="apple-touch-icon" />
    <link href="img/webapp-icon.png" rel="icon" sizes="192x192" />
    <link href="img/webapp-icon.png" rel="icon" sizes="128x128" />

    <link href="css/bootstrap.css" rel="stylesheet" />
    <link href="css/custom.css?ut=20180801" rel="stylesheet" />

    <link rel="prefetch" href="img/mobile-menu.svg">
    <link rel="prefetch" href="img/mobile-menu-active.svg">
    <link rel="prefetch" href="img/robot.png">

    <script src="js/jquery-1.10.2.js"></script>
    <script src="js/bootstrap.min.js"></script>
    <script src="js/siriwave.js"></script>
    <script src="js/jquery.dateFormat-1.0.js" type="text/javascript"></script>
    <script src="js/jquery.pressAndHold.js"></script>
    <script src="js/jquery.hotkeys-0.7.9.min.js"></script>
    <script src="js/webRM.js"></script>
</head>
<body>
<div id="tutorial-overlay" class="overlay">
    <span class="skip-tips-btn">跳過教學</span>
</div>
<div id="survey-overlay" class="overlay">
    <div class="survey-wrapper">
    <div class="survey">
        <div class="survey-header">
            <h4>滿意度調查問卷</h4>
            <span class="close-btn"></span>
            <img src="img/survey-icon.svg" />
            <p>感謝您使用本服務，您的滿意是我們的動力，歡迎您填寫服務滿意度調查。</p>
        </div>
        <div class="survey-content">
            <div class="scale-label-container">
                <span>非常滿意</span>
                <span>非常不滿意</span>
            </div>
            <form>
                <div class="survey-question-container">
                    <div class="survey-question">
                        1. 請問您對於「智能客服」這次提供的服務，整體滿意程度為何？
                    </div>
                    <div class="survey-scale">
                        <span class="scale-label">非常滿意</span>
                        <label class="radio-button-wrapper"><input type="radio" name="question1" value="1"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question1" value="2"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question1" value="3"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question1" value="4"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question1" value="5"><span class="radio-button"></span></label>
                        <span class="scale-label">非常不滿意</span>
                    </div>
                    <div class="survey-question">
                        2. 請問您對於「人工客服」這次提供的服務，整體滿意程度為何？
                    </div>
                    <div class="survey-scale">
                        <span class="scale-label">非常滿意</span>
                        <label class="radio-button-wrapper"><input type="radio" name="question2" value="1"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question2" value="2"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question2" value="3"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question2" value="4"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question2" value="5"><span class="radio-button"></span></label>
                        <span class="scale-label">非常不滿意</span>
                    </div>
                    <div class="survey-question">
                        3. 請問您對於ＯＯ銀行這次提供的服務，整體滿意程度為何？
                    </div>
                    <div class="survey-scale">
                        <span class="scale-label">非常滿意</span>
                        <label class="radio-button-wrapper"><input type="radio" name="question3" value="1"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question3" value="2"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question3" value="3"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question3" value="4"><span class="radio-button"></span></label>
                        <label class="radio-button-wrapper"><input type="radio" name="question3" value="5"><span class="radio-button"></span></label>
                        <span class="scale-label">非常不滿意</span>
                    </div>
                </div>
                <input type="submit" value="填完送出" ／>
            </form>
        </div>
    </div>
    </div>
</div>
<div id="voice-input-desktop-overlay" class="overlay"></div>

<div id="lhc_livechat">
    <div class="top">
        <div class="overlay mobile-menu-overlay"></div>
        <div class="top-logo">
            <img src="img/replaceable_company_logo.svg"/><span><%= t.getNotes() %></span>
        </div>
        <button class="btn_help"></button>
        <span class="itri-tooltip">進入互動教學</span>
    </div>
    <div class="top-banner show">
        <img src="img/announce.svg"/><span>為保護您的權益，請詳細閱讀<a>「個人資料運用法定告知事項」</a>，如您已充分瞭解並同意，請繼續進行對談服務。</span>
    </div>
    <div class="main-wrapper">
        <div class="chat-wrapper">
            <div class="overlay mobile-menu-overlay"></div>
            <div class="chat-message-container">
                <div class="chat-messages">

                </div>
            </div>
            <div class="chat-input-container">
                <div class="tip inputbox-tip tutorial-1">
                <div class="tip-content inputbox-tip-content">此為文字輸入框，請描述您的提問，並按下Enter送出即可。(建議以20字簡要描述)</div>
                </div>
                <div class="tip-highlight inputbox-tip-highlight tutorial-1"></div>
                <div class="chat-input-buttons">
                    <button class="mobile-menu-btn"></button>
                    <button class="voice-input-panel-btn"></button>
                </div>
                <div class="chat-input-form">
                    <input id="CSChatMessage" required type="text" name="ChatMessage" class="txt_input" placeholder="請輸入您的問題" id="CSChatMessage" hkid="CSChatMessage">
                    <input type="submit" value="送出" id="btnSendCSChatMessage" class="btn_send" />
                </div>
            </div>
        </div>
        <div class="sidebar-wrapper">
            <div class="sidebar">
                <ul class="tabs">
                    <li class="tab active">快速選單</li>
                    <li class="tab">熱門推薦</li>
                    <li class="tab">生活資訊</li>
                </ul>
                <div class="tab-content-container">
                    <div class="tab-content active">
                        <section>
                            <h4>快速選單</h4>
                            <div class="tip grid-icon-tip tutorial-2">
                                <div class="tip-content grid-icon-tip-content">右方區塊為功能區，您可以在此快速點選熱門常用服務。</div>
                            </div>
                            <div class="tip-highlight grid-icon-tip-highlight tutorial-2"></div>
                            <ul class="icon-block-container">
                                <li class="icon-block">
                                	<a href="#" class="survey-link">
                                        <img src="img/grid_icons/grid_survey.png">
                                        <span>滿意度調查</span>
                                	</a>
                                </li>
                                <li class="icon-block">
                                	<a href="http://www.intumit.com/" target="_new">
                                        <img src="img/grid_icons/grid_04.png">
                                        <span>公司首頁</span>
                                	</a>
                                </li>
                                <li class="icon-block">
                                	<a href="mailto:<%= t.getEmail() %>">
                                        <img src="img/grid_icons/grid_08.png">
                                        <span>聯絡我們</span>
                                	</a>
                                </li>
                            </ul>
                        </section>
                        <%--section>
                            <h4>熱門業務</h4>
                            <ul class="icon-block-container">
                                <a href="#" class="survey-link">
                                    <li class="icon-block">
                                        <img src="img/grid_icons/grid_survey.png">
                                        <span>滿意度調查</span>
                                    </li>
                                </a>
                                <li class="icon-block"></li>
                                <li class="icon-block"></li>
                            </ul>
                        </section --%>
                        <section>
                        	<%
							JSONArray faqsArr = new JSONArray(StringUtils.isNotEmpty(qaCtx.getTenant().getFaqJson()) ? qaCtx.getTenant().getFaqJson() : "[]");
							%>
		                    <%
		                    for (int i=0; i < faqsArr.length(); i++) {
		                    	JSONObject faqsObj = faqsArr.getJSONObject(i);
		                    	JSONArray faqs = faqsObj.getJSONArray("questions");
		                    %>
                            <h4><%= StringUtils.upperCase(faqsObj.optString("channel", faqsObj.optString("category", "未知"))) %></h4>
                            <ul class="text-link-container">
	                          	<%
	                          	for (int j=0; j < faqs.length(); j++) {
	                          		String testQ = faqs.getString(j);
	                          	%>
                                <li class="question-clickable"><%= testQ %></li>
	                          	<%
	                          	}
	                          	%>
                            </ul>
		                    <%
		                    }
		                    %>
                        </section>
                        <div class="slide-indicator-container">
                        </div>
                    </div>
                    <div class="tab-content">
                        <section>
                            <h4>熱門推薦</h4>
                            <ul class="icon-block-container">
                                <li class="icon-block">
                                    <img src="img/default-icon.svg">
                                    <span>優惠商品</span>
                                </li>
                                <li class="icon-block"></li>
                                <li class="icon-block"></li>
                                <li class="icon-block"></li>
                                <li class="icon-block"></li>
                                <li class="icon-block"></li>
                            </ul>
                        </section>
                        <div class="slide-indicator-container"></div>
                    </div>
                    <div class="tab-content">
                        <section>
                            <h4>便民服務</h4>
                            <ul class="icon-block-container">
                                <li class="icon-block">
                                    <img src="img/default-icon.svg">
                                    <span>交通資訊</span>
                                </li>
                                <li class="icon-block"></li>
                                <li class="icon-block"></li>
                            </ul>
                        </section>
                        <div class="slide-indicator-container"></div>
                    </div>
                </div>
            </div>
        </div>
        <div class="voice-input-panel">
            <div class="voice-input-close-btn"></div>
            <div class="voice-input-wrapper">
                <div class="voice-input-txt">按住，並簡短說出您的問題。</div>
                <hr class="waveform-base"></hr>
                <div id="waveform-container"></div>
                <div class="voice-input-btn">
                    <img src="img/voice-input-btn.svg" />
                </div>
            </div>
        </div>
    </div>
</div>
<div style="display: none;">
	<div class="message-template-user message-row by-user">
	    <div class="msg"></div>
	    <div class="message-datetime">NOW</div>
	</div>
	<div class="message-template-robot message-row by-robot">
        <div class="avatar-wrapper"><img class="robot-avatar" src="img/robot.png"></div>
        <div class="msg-content"></div>
        <div class="message-datetime">NOW</div>
    </div>
</div>

<script src="js/custom.js?ut=20180710"></script>
<script type="text/javascript">
	var deviceType = 'web';
	var inSubmitQ = false;
	var qaId = '<%= qaId %>';
	
	if ( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
	    deviceType = 'app';
	}

	function submitQuestion(q, forceInputType, welcome) {
		if (inSubmitQ) return;
		inSubmitQ = true;
		
    	console.log("add msg!")
    	var $inputText = $('#CSChatMessage');
        var surveyLink = $('.chat-survey-link');
        var restartLink = $('.chat-restart-link');
        var redirectLink = $('.chat-redirect-link');
        var cmds = [function(){surveyLink.click();},function(){restartLink.click();}];
        var cmdsOffline = [function(){redirectLink.click();},function(){surveyLink.click();}];
        var enteredCmd;
        var postback;
        
        if (surveyLink.size() == 1 && restartLink.size() == 1){
            enteredCmd = cmds[parseInt($.trim($inputText.val())) - 1];
        }
        if (redirectLink.size() == 1 && surveyLink.size() == 1){
            enteredCmd = cmdsOffline[parseInt($.trim($inputText.val())) - 1];
        }
        message = $inputText.val();
        inputType = $inputText.attr("inputType");
        postback = $inputText.data('postback');
        $inputText.removeData('postback');	// 避免影響下次的問句

        if (typeof(q) != 'undefined') {
          message = q;
          currentQuestion = q;
        }
        else {
        	currentQuestion = message;
        }
        if (typeof(welcome) == 'undefined') {
      	  welcome = false
        }
        
        if (typeof(forceInputType) != 'undefined') {
        	inputType = forceInputType;
        }

        var reply;

        $inputText.val("");
        $inputText.attr('inputType', 'string');

        if (message.length !== 0 || welcome) {
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
		         		ch:selectQaChannelType,
		         		et: welcome ? 'inbound' : 'message'
		         	};

            if (typeof(eventSourceType) != 'undefined') {
          		options['est'] = eventSourceType;
            }
            if (typeof(eventSource) != 'undefined') {
          		options['es'] = eventSource;
            }
            if (typeof(postback) !== 'undefined') {
          		options['postback'] = postback;
          		options['et'] = 'postback';
            }
			if ($("#qaCategorySelect option:selected").text() != '') {
				options.category = $("#qaCategorySelect option:selected").text();
			}
            userli = $(".message-template-user").clone();
            userli.removeClass("message-template-user");

			if (inputType == 'hidden') {
				// Do nothing
			}
			else {
	            if (inputType == 'password' || inputType == 'stringMask') {
	          		userli.find(".msg").text("********");
	            }
	            else if (inputType == 'mic') {
	          		userli.find(".msg").html("<img style='height:50px;' src='<%= request.getContextPath() %>/img/user_speech_icon.png' title='" + q + "'>" + q);
	            }
	            else {
	          		userli.find(".msg").text(message);
	            }
				
				$("div.chat-messages").append(userli);
			}
			userli.find(".message-datetime").text($.format.date(new Date(), 'HH:mm:ss'));
			$('.loading-panel').show();

			$.ajax({
              type: 'POST',
        	  url: '<%= request.getContextPath() %>/chats-ajax.jsp',
        	  dataType: 'json',
        	  data: options,
        	  error: function() {
        		  inSubmitQ = false;
				  setTimeout(function() {$('.loading-panel').hide();}, 100);
              },
        	  success: function(resp) {
				resp["ts"] = (new Date()).getTime();

                reply = $(".message-template-robot").clone();
                reply.removeClass("message-template-robot");
                reply.find(".message-datetime").text($.format.date(new Date(), 'HH:mm:ss'));
                reply.find(".msg-content").addClass("msg");

                if (resp.hasOwnProperty('output')) {
                	$(".btnTts").remove();
					reply.find(".msg").html(resp.output + "&nbsp; <button class='btn btn-default btn-xs btnTts' data-loading-text='<bean:message key="load.and.play..."/>' data-id='<%= qaId %>'><span class='glyphicon glyphicon-bullhorn'></span><bean:message key="speak.out"/></button>");
                }
                else {
					reply.find(".msg").html("<bean:message key='this.topic.has.not.yet.been.edited'/>");
                }

                if (resp.hasOwnProperty('inputType')) {
                	$inputText.attr('inputType', resp.inputType);
                }
                inputType = $("#CSChatMessage").attr("inputType");

                if (inputType == 'password') {
                	$inputText.attr('type', 'password');
                }
                else {
                	$inputText.attr('type', 'text');
                }

                currentTimestamp = resp.ts;

				if (resp.hasOwnProperty('otherPossibleQuestions')) {
					var ul = $("<ol class='other-questions'></ol>");
					var otherQuestions = resp.otherPossibleQuestions;
					for (var i=0; i < otherQuestions.length; i++) {
						var thisLi = $("<li class='other-questions-li' style='border-bottom: 0px; padding: 0; padding-left: 10px;'></li>");

						var clickToAsk = $('<a href="#" />');
						clickToAsk.addClass("question-clickable");
						clickToAsk.text(otherQuestions[i].question);
						clickToAsk.appendTo(thisLi);

						thisLi.appendTo(ul);
					}
					reply.find(".msg").append("<BR><bean:message key='question.clickable'/>");
					reply.find(".msg").append(ul);
				}

				setTimeout( (function(reply, webRM) {
					return function() {
				        date = new Date();
				        
				        if (typeof(webRM) == 'undefined') {
					        $('div.chat-messages').append(reply);
					        $('.chat-message-container').scrollTop($('.chat-message-container').prop("scrollHeight"));
				        }
				        else {
					        for (var i=0; i < webRM.messages.length; i++) {
				                newWebRM = $(".message-template-robot").clone();
				                newWebRM.removeClass("message-template-robot");
				                newWebRM.find(".message-datetime").text($.format.date(new Date(), 'HH:mm:ss'));
				                newWebRM.appendRichMessage(webRM.messages[i]);
					        	
					        	setTimeout( (function(rm) {
					                return function() {
					                	$('div.chat-messages').append(rm);
								        $('.chat-message-container').scrollTop($('.chat-message-container').prop("scrollHeight"));
					                };
					        	})(newWebRM), (i+1)*700);
							}
				        }
					}
				})(reply, resp.webRM), 100);
        	  },
        	  complete: function() {
				  setTimeout(function() {
	        		  	inSubmitQ = false;
						$('.loading-panel').hide(); 
						if (deviceType == 'web') $inputText.focus();
					  }, 
					  100);
      	  	  }
          });
        }
    }

    $('#CSChatMessage').bind('keydown', 'return', function (evt){
        submitQuestion();
        return false;
    });

    $('#CSChatMessage').bind('keyup', 'up', function (evt){
        //lhinst.editPreviousUser();
    });
    
    $('#btnSendCSChatMessage').click(function(evt) {
		submitQuestion();
    });
    
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

    $(document).on("click", ".question-clickable", function() {
    	$this = $(this);
        if (typeof $this.data('opnum') !== 'undefined') {
            var cmd = $this.data('opnum');
            $('#CSChatMessage').val(cmd);
        }
        else if (typeof $this.data('postback') !== 'undefined') {
            $('#CSChatMessage').val($this.text());
        	$('#CSChatMessage').data('postback', $this.data('postback'));
        }
        else {
            $('#CSChatMessage').val($this.text());
        }
        submitQuestion();
    });

    (function() {
        var BREAK_WIDTH = 962;
        var CHAT_BASE_HEIGHT = 500;
        var _doc = $(document);
        var _window = $(window);
        var element;
        var main;
        var messages;
        var notice;
        var scrollbar_inner;
        var input;
        var livechat_button;
        var livechat_button_inner;
        var livechat_button_outer;
        var chat_height = CHAT_BASE_HEIGHT;
        var element_right;

        var setVariables = function(){
            element = $("#lhc_livechat").show();
            main = $(".window", element);
            messages = $('#main-content');
            notice = $(".notice.active", element);
            scrollbar_inner = $(".scrollbar-inner", element);
            input = $(".txt_input", element);
            livechat_button = $(".btn_livechat", element);
            livechat_button_inner = $(".btn_livechat_inner", element);
            livechat_button_outer = $(".btn_livechat_outer", element);
        };

        var initNotice = function(){
            var notice_help = $(".notice.help", element);
            var notice_attention = $(".notice.attention", element);
            $(".btn_attention", element).click( function() {
                if(deviceType == 'app'){
	                var origHeight = messages.height();
	                var newHeight = origHeight;
	                if(notice_attention.hasClass('active')){
	                    newHeight += notice_attention.outerHeight();
	                }
	                if(notice_help.hasClass('active')){
	                    newHeight += notice_help.outerHeight();
	                }
	                notice_help.removeClass("active");
	                notice_attention.toggleClass("active");
	                var delta = 0;
	                if(notice_attention.hasClass('active')){
	                    delta = notice_attention.outerHeight();
	                }
	                newHeight -= delta;
	                fadeInElement(notice_attention, "active");
	                messages.css("height", newHeight);
	                adjustMsgScrollingArea(newHeight - origHeight);
                }
            });

            function adjustMsgScrollingArea(diff){
                $('#messages').height($('#messages').height() + diff);
            }
        };
        var fadeInElement = function(element, className){
            if(className){
                if (element.hasClass(className)){
                    TweenMax.from(element, 1, {css:{ backgroundColor:"#fff" }, ease:Quad.easeOut});
                }
            }
        };
        var initToggleHideButton = function(){
            $(".btn_toggle_hide").click( function() {
                window.close();
            });
        };
        var toggleButton = function(){
            element.toggleClass("hidden");
        };
    })();

    $( document ).ready(function() {
        console.log( "ready!");
        
    	submitQuestion('', 'hidden', true);
    });
    </script>
</body>
</html>
<%
}
catch (Exception e) {
	e.printStackTrace();
}
%>
