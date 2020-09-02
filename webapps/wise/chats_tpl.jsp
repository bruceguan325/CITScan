<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="org.apache.commons.lang.*"
%><%
String nickname = StringUtils.defaultString(request.getParameter("nickname"), "Guest");
%>
<!DOCTYPE html>
<html>
<head>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    
    <jsp:include page="header-opinion.jsp" />
    <style>
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
    </style>
</head>
<body class='contrast-grass-green main-nav-closed'>
<header>
    <jsp:include page="header2-opinion.jsp" />
</header>
<div id='wrapper'>
<div id='main-nav-bg'></div>
<nav class='' id='main-nav'>
<div class='navigation'>
    <jsp:include page="navbar-opinion.jsp" />
</div>
</nav>
<section id='content'>
    <div class='container'>
      <div class='row' id='content-wrapper'>
        <div class='col-xs-12'>
          <div class='row'>
            <div class='col-sm-12'>
              <div class='page-header'>
                <h1 class='pull-left'>
                  <i class='icon-comments'></i>
                  <span>智能機器人展示介面 - 線上客服</span>
                </h1>
                <div class='pull-right'>
                  <ul class='breadcrumb'>
                    <li>
                      <a href='index.jsp'>
                        <i class='icon-bar-chart'></i>首頁
                      </a>
                    </li>
                    <li class='separator'>
                      <i class='icon-angle-right'></i>
                    </li>
                    <li>客戶服務</li>
                    <li class='separator'>
                      <i class='icon-angle-right'></i>
                    </li>
                    <li class='active'>線上客服</li>
                  </ul>
                </div>
              </div>
            </div>
          </div>
          <%-- 這個區段是 fix 在右下角的對話框
          <div class='row'>
            <div class='chat chat-fixed'>
              <div class='col-sm-12'>
                <div class='box box-collapsed'>
                  <div class='box-header box-header-small contrast-background'>
                    <div class='title'>
                      <i class='icon-comments-alt'></i>
                      Chat
                    </div>
                    <div class='actions'>
                      <a class="btn box-remove btn-xs btn-link" href="#"><i class='icon-remove'></i>
                      </a>
                          
                      <a class="btn box-collapse btn-xs btn-link" href="#"><i></i>
                      </a>
                    </div>
                  </div>
                  <div class='box-content box-no-padding'>
                    <div class='scrollable' data-scrollable-height='300' data-scrollable-start='bottom'>
                      <ul class='list-unstyled list-hover list-striped'>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:42:35 +0800'>September 15, 2013 - 17:42</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                          	這啥鬼？ 
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:41:35 +0800'>September 15, 2013 - 17:41</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Natus tempora est et aut expedita voluptatem voluptas
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:40:35 +0800'>September 15, 2013 - 17:40</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Dolores sunt distinctio qui corporis
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:39:35 +0800'>September 15, 2013 - 17:39</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Occaecati voluptas adipisci itaque minima est labore explicabo natus aut consectetur eaque voluptatem laboriosam
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:38:35 +0800'>September 15, 2013 - 17:38</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Aliquam quibusdam et quia
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:37:35 +0800'>September 15, 2013 - 17:37</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Et aut quia et magnam iusto consequatur enim non numquam eaque
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Samuel</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:36:35 +0800'>September 15, 2013 - 17:36</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Vel saepe dolores in totam
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:35:35 +0800'>September 15, 2013 - 17:35</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Eos exercitationem aut est enim eum consequatur iste odio tenetur ipsum minima et
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Samuel</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:34:35 +0800'>September 15, 2013 - 17:34</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Enim non expedita veritatis ut quam deserunt et odit eius corporis quos quae
                          </div>
                        </li>
                        <li class='message'>
                          <div class='avatar'>
                            <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                          </div>
                          <div class='name-and-time'>
                            <div class='name pull-left'>
                              <small>
                                <a class="text-contrast" href="#">Dwayne</a>
                              </small>
                            </div>
                            <div class='time pull-right'>
                              <small class='date pull-right text-muted'>
                                <span class='timeago fade has-tooltip' data-placement='top' title='2013-09-15 17:33:35 +0800'>September 15, 2013 - 17:33</span>
                                <i class='icon-time'></i>
                              </small>
                            </div>
                          </div>
                          <div class='body'>
                            Aut ducimus et quis eius
                          </div>
                        </li>
                      </ul>
                    </div>
                              <form class="new-message" method="post" action="#" accept-charset="UTF-8"><input name="authenticity_token" type="hidden" /><input class='form-control' id='message_body' name='message[body]' placeholder='Type your message here...' type='text'>
                    <button class='btn btn-success' type='submit'>
                      <i class='icon-plus'></i>
                    </button>
                    </form>
              
                  </div>
                </div>
              </div>
            </div>
          </div>
           --%>
           
          <div class='row'>
            <div class='col-sm-7'>
              <div class='box'>
                <div class='row'>
                  <div class='chat'>
                    <div class='col-sm-12'>
                      <div class='box'>
                        <div class='box-content box-no-padding'>
                          <div class='scrollable' data-scrollable-height='400' data-scrollable-start='bottom'>
                            <ul class='list-unstyled list-hover list-striped main-stream'>
                              <li class='message'>
                                <div class='avatar'>
                                  <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
                                </div>
                                <div class='name-and-time'>
                                  <div class='name pull-left'>
                                    <small>
                                      <a class="text-contrast" href="#"><%= nickname %></a>
                                    </small>
                                  </div>
                                  <div class='time pull-right'>
                                    <small class='date pull-right text-muted'>
                                      <span class='timeago fade has-tooltip' data-placement='top' title='2014-07-15 17:42:35 +0800'>September 15, 2013 - 17:42</span>
                                      <i class='icon-time'></i>
                                    </small>
                                  </div>
                                </div>
                                <div class='body'>
                                    <%= request.getParameter("q") %>
                                </div>
                              </li>
                              <li class='message'>
                                <div class='avatar'>
                                  <img alt='Avatar' height='23' src='img/portrait_robot.png' width='23'>
                                </div>
                                <div class='name-and-time'>
                                  <div class='name pull-left'>
                                    <small>
                                      <a class="text-contrast" href="#">小智</a>
                                    </small>
                                  </div>
                                  <div class='time pull-right'>
                                    <small class='date pull-right text-muted'>
                                      <span class='timeago fade has-tooltip' data-placement='top' title='2014-07-15 17:41:35 +0800'>September 15, 2013 - 17:41</span>
                                      <i class='icon-time'></i>
                                    </small>
                                  </div>
                                </div>
                                <div class='body'>
                                  <%= request.getParameter("a") %>
                                </div>
                              </li>
                            </ul>
                          </div>
                          <form class="new-message" method="post" action="#" accept-charset="UTF-8"><input name="authenticity_token" type="hidden" /><input class='form-control' id='message_body' name='message[body]' placeholder='Type your message here...' type='text'>
                          <button class='btn btn-success' type='submit'>
                            <i class='icon-plus'></i>
                          </button>
                          </form>
                    
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            
<%
	String[][] tests = {
			// ATM
			{
				"民生東路哪裡有提款機?",
				"外幣提款機哪裡有?",
				"中山北路哪裡有補摺機?",
				"哪裡的ATM可以提領外幣?",
			},
			// BRANCH
			{
				"請問館前分行電話?",
				"南京東路哪裡有分行?",
				"南京東路分行在哪?",
				"請問南京東路分行有換外幣嗎?",
				"請問台東分行代號?",
			},
			// FUND
			{
				"如何申購／贖回／轉換基金？",
				"基金申購優惠?",
				"施羅德環球精選價值 A1基金語音代碼為何",
				"華南永昌前瞻科技基金代碼為何",
			},
			// OTHERS
			{
				"晶片讀卡機哪裡買?",
				"交易執行過程中出現元件錯誤",
				"於MyATM元件執行過程中，出現之安全性設定警示的訊息怎麼辦?",
				"用IE8使用MyATM的常見問題",
				"MyBank登入問題",
				"MyBank外匯轉帳匯款問題",
			},
			// 信用卡
			{
				"海外刷國泰世華卡有什麼好康嗎？",
				"國內訂房刷國泰世華卡有什麼好康嗎？",
				"卡片毀損了，怎麼辦？",
				"信用卡將到期是否須重新申請換卡？若到期即不再續用，怎麼辦？",
				"卡片遺失（被竊）了，怎麼辦？",
				"額度不夠用，怎麼辦？",
				"您們的信用卡年費如何收取？",
				"如何預借現金?",
				"預借現金手續費如何計算？",
				"在國外消費的簽帳款，如何換算成新台幣？",
			},
		};
%>
            <div class='col-sm-5 box' style='margin-bottom: 0'>
                  <div class='box-header red-background'>
                    <div class='title'>常見問題</div>
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
                        <li class='active'>
                          <a class='green-border' data-toggle='tab' href='#tabsimple-credit'>
                            <i class='icon-money'></i>
                            信用卡 
                          </a>
                        </li>
                        <li>
                          <a class='green-border' data-toggle='tab' href='#tabsimple-atm'>
                            <i class='icon-usd'></i>
                            ATM
                          </a>
                        </li>
                        <li>
                          <a data-toggle='tab' href='#tabsimple-branch'>
                            <i class='icon-briefcase'></i>
                            分行
                          </a>
                        </li>
                        <li>
                          <a data-toggle='tab' href='#tabsimple-fund'>
                            <i class='icon-money'></i>
                            基金
                          </a>
                        </li>
                        <li>
                          <a data-toggle='tab' href='#tabsimple-others'>
                            <i class='icon-group'></i>
                            其他
                          </a>
                        </li>
                      </ul>
                      <div class='tab-content'>
                        <div class='tab-pane active' id='tabsimple-credit'>
                          <ul class='list-unstyled list-hover list-striped'>
                          	<% for (String testQ: tests[4]) { %>
                            <li class='message'>
                              <div class='row'>
                              <div class='col-sm-1' style="margin-left: 20px;">
                                <span class="icon-question-sign" style="font-size: 23px;"></span>
                              </div>
                              <div class='col-sm-10 body question-clickable'>
                                <%= testQ %>
                              </div>
                              </div>
                            </li>
                            <% } %>
                          </ul>
                        </div>
                        
                        <div class='tab-pane' id='tabsimple-atm'>
                          <ul class='list-unstyled list-hover list-striped'>
                          	<% for (String testQ: tests[0]) { %>
                            <li class='message'>
                              <div class='row'>
                              <div class='col-sm-1' style="margin-left: 20px;">
                                <span class="icon-question-sign" style="font-size: 23px;"></span>
                              </div>
                              <div class='col-sm-10 body question-clickable'>
                                <%= testQ %>
                              </div>
                              </div>
                            </li>
                            <% } %>
                          </ul>
                        </div>
                        
                        <div class='tab-pane' id='tabsimple-branch'>
                          <ul class='list-unstyled list-hover list-striped'>
                          	<% for (String testQ: tests[1]) { %>
                            <li class='message'>
                              <div class='row'>
                              <div class='col-sm-1' style="margin-left: 20px;">
                                <span class="icon-question-sign" style="font-size: 23px;"></span>
                              </div>
                              <div class='col-sm-10 body question-clickable'>
                                <%= testQ %>
                              </div>
                              </div>
                            </li>
                            <% } %>
                          </ul>
                        </div>
                        
                        <div class='tab-pane' id='tabsimple-fund'>
                          <ul class='list-unstyled list-hover list-striped'>
                          	<% for (String testQ: tests[2]) { %>
                            <li class='message'>
                              <div class='row'>
                              <div class='col-sm-1' style="margin-left: 20px;">
                                <span class="icon-question-sign" style="font-size: 23px;"></span>
                              </div>
                              <div class='col-sm-10 body question-clickable'>
                                <%= testQ %>
                              </div>
                              </div>
                            </li>
                            <% } %>
                          </ul>
                        </div>
                        
                        <div class='tab-pane' id='tabsimple-others'>
                          <ul class='list-unstyled list-hover list-striped'>
                          	<% for (String testQ: tests[3]) { %>
                            <li class='message'>
                              <div class='row'>
                              <div class='col-sm-1' style="margin-left: 20px;">
                                <span class="icon-question-sign" style="font-size: 23px;"></span>
                              </div>
                              <div class='col-sm-10 body question-clickable'>
                                <%= testQ %>
                              </div>
                              </div>
                            </li>
                            <% } %>
                          </ul>
                        </div>
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
    <img alt='Avatar' height='23' src='assets/images/avatar.jpg' width='23'>
  </div>
  <div class='name-and-time'>
    <div class='name pull-left'>
      <small>
        <a class="text-contrast" href="#"><%= nickname %></a>
      </small>
    </div>
    <div class='time pull-right'>
      <small class='date pull-right text-muted'>
        <span class='timeago fade has-tooltip' data-placement='top' title='2014-07-15 17:42:35 +0800'>September 15, 2013 - 17:42</span>
        <i class='icon-time'></i>
      </small>
    </div>
  </div>
  <div class='body'>
     你好
  </div>
</li>
<li class='message message-template-robot'>
  <div class='avatar'>
    <img alt='Avatar' height='23' src='img/portrait_robot.png' width='23'>
  </div>
  <div class='name-and-time'>
    <div class='name pull-left'>
      <small>
        <a class="text-contrast" href="#">小智</a>
      </small>
    </div>
    <div class='time pull-right'>
      <small class='date pull-right text-muted'>
        <span class='timeago fade has-tooltip' data-placement='top' title='2014-07-15 17:41:35 +0800'>September 15, 2013 - 17:41</span>
        <i class='icon-time'></i>
      </small>
    </div>
  </div>
  <div class='body'>
    這個問題我沒有合適的答案，需要幫您轉真人客服嗎？
  </div>
</li>
</div>

<jsp:include page="footer-opinion.jsp" />

    <script>
      var askUserRedirectToCRM = false;
      var confirmStringArray = ['好', '好的', '好啊', '好吧', '行', 'Y', 'y', 'YES', 'yes', 'Yes', '可以', '沒問題', 'OK', 'ok'];
      
      $(".question-clickable").live('click', function(e) {
    	$("#message_body").val($(this).text());
        submitQuestion();
      });
      
      $(".chat .new-message").live('submit', function(e) {
        submitQuestion();
        return e.preventDefault();
      });
      
      function submitQuestion() {
    	  var chat, date, li, message, months, reply, scrollable, sender, timeago;
          date = new Date();
          months = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
          chat = $("#message_body").parents(".chat");
          message = $("#message_body").val();
          $("#message_body").val("");
          
          if (message.length !== 0) {
            userli = $("li.message-template-user").clone();
            userli.removeClass("message-template-user");
            
            userli.find(".body").text(message);
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
            
            if (askUserRedirectToCRM && $.inArray(message, confirmStringArray) != -1) {
            	reply = $("li.message-template-robot").clone();
                reply.removeClass("message-template-robot");
            	reply.find(".body").html("好的，請稍待，系統正在轉接客服當中<em>（備註：展示系統可能沒有銜接客服單位）</em>");
                
                setTimeout((function() {
                    date = new Date();
                    timeago = reply.find(".timeago");
                    timeago.attr("title", "" + (date.getFullYear()) + "-" + (month<10 ? '0' : '') + month + "-" + (date_day<10 ? '0' : '' ) + date_day + " " + (date.getHours()) + ":" + (date.getMinutes()) + ":" + (date.getSeconds()) + " +0800");
                    timeago.text("" + months[date.getMonth()] + " " + (date.getDate()) + ", " + (date.getFullYear()) + " " + (date.getHours()) + ":" + (date.getMinutes()));
                    setTimeAgo(timeago);
                    scrollable.find("ul.main-stream").append(reply);
                    $(scrollable).slimScroll({
                      scrollTo: scrollable.prop('scrollHeight') + "px"
                    });
                    return reply.effect("highlight", {}, 500);
                  }), 1000);
            }
            else {
	            $.ajax({
	          	  url: 'qa-ajax.jsp',
	          	  dataType: 'json',
	          	  data: {q: message, html: true},
	          	  success: function(resp) {
	                    reply = $("li.message-template-robot").clone();
	                    reply.removeClass("message-template-robot");
	                    
	                    if (resp.hasOwnProperty('output')) {
	                    	reply.find(".body").html(resp.output);
	                    	askUserRedirectToCRM = false;
	                    }
	                    else {
	                    	askUserRedirectToCRM = true;
	                    }
	                    
	                    if (resp.hasOwnProperty('otherPossibleQuestions')) {
	                    	var ul = $("<ol class='other-questions'></ol>");
	                    	var otherQuestions = resp.otherPossibleQuestions;
	                    	for (var i=0; i < otherQuestions.length; i++) {
	                    		var thisLi = $("<li class='question-clickable other-questions-li' style='border-bottom: 0px; padding: 0; padding-left: 10px;'></li>");
	                    		thisLi.text(otherQuestions[i].question);
	                    		
	                    		thisLi.appendTo(ul);
	                    	}
	                    	reply.find(".body").append("<BR>或是您可以從下面選擇同類型的其他常見問題");
	                    	reply.find(".body").append(ul);
	                    }
	                    
	                    setTimeout((function() {
	                      date = new Date();
	                      timeago = reply.find(".timeago");
	                      timeago.attr("title", "" + (date.getFullYear()) + "-" + (month<10 ? '0' : '') + month + "-" + (date_day<10 ? '0' : '' ) + date_day + " " + (date.getHours()) + ":" + (date.getMinutes()) + ":" + (date.getSeconds()) + " +0800");
	                      timeago.text("" + months[date.getMonth()] + " " + (date.getDate()) + ", " + (date.getFullYear()) + " " + (date.getHours()) + ":" + (date.getMinutes()));
	                      setTimeAgo(timeago);
	                      scrollable.find("ul.main-stream").append(reply);
	                      $(scrollable).slimScroll({
	                        scrollTo: scrollable.prop('scrollHeight') + "px"
	                      });
	                      return reply.effect("highlight", {}, 500);
	                    }), 1000);
	          	  }
	            });
            }
          }
      }
    </script>
</body>
</html>

