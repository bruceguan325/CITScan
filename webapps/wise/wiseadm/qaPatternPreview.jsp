<%@ include file="/commons/taglib.jsp"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugin"%>
<%@page import="com.intumit.solr.robot.qaplugin.QAPlugins"%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Date"%>
<%@page import="java.text.SimpleDateFormat"%>
<%@page import="com.intumit.solr.util.*"%>
<%@page import="com.intumit.solr.admin.*"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
	import="com.intumit.solr.robot.*"
	import="com.intumit.solr.robot.dictionary.*"
	import="com.intumit.smartwiki.util.*"
	import="com.intumit.solr.util.*"
	import="java.io.*"
	import="java.util.*"
	import="org.apache.solr.client.solrj.*"
	import="org.apache.solr.common.*"
	import="org.apache.commons.codec.binary.Base64"
	import="org.apache.commons.lang.*"
	import="org.apache.commons.lang.math.RandomUtils"
	import="org.apache.wink.json4j.*"
	import="com.intumit.solr.admin.*"
%><%
	if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.O3) == 0) { 
	return;
}
String nickname = AdminUserFacade.getInstance().getFromSession(session).getName();

Date now = new Date();
SimpleDateFormat dFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
SimpleDateFormat dFormat2 = new SimpleDateFormat("MMMMM d, yyyy - HH:mm");
String date1 = dFormat.format(now);
String date2 = dFormat2.format(now);

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);
QAPattern p = new QAPattern();

if (request.getParameter("id") != null)
	p.setId(new Integer(request.getParameter("id")));
p.setQuestionTemplate(StringUtils.trimToNull(request.getParameter("questionTemplate")));
p.setAnswerTemplate(StringUtils.trimToNull(request.getParameter("answerTemplate")));
p.setMaxMatched(new Integer(StringUtils.defaultString(request.getParameter("maxMatched"), "1")));
p.setDataAggregator(StringUtils.trimToNull(request.getParameter("dataAggregator")));
p.setSpecialRestriction(StringUtils.trimToNull(request.getParameter("specialRestriction")));
p.setPreviewParameters(StringUtils.trimToNull(request.getParameter("previewParameters")));
p.setStaticAnswer(StringUtils.trimToNull(request.getParameter("staticAnswer")));

QAPattern bestQp = p;
Class aggregatorClazz = QADataAggregator.findRegisteredClass(bestQp.getDataAggregator());
Class outputClazz = QAOutputTemplate.findRegisteredClass("GeneralTextOutput");

QAUtil qu = QAUtil.getInstance(t);
Map<String, List<String>> params = p.getPreviewParameterMap();
%>
<!DOCTYPE html>
<html>
<head>
    <meta content='width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no' name='viewport'>
    <meta content='text/html;charset=utf-8' http-equiv='content-type'>
    
        <title><bean:message key='globa.smart.robot.show'/></title>
	<!-- / jquery -->
	<script src="http://code.jquery.com/jquery-1.12.4.min.js"></script>
	<script src="http://code.jquery.com/jquery-migrate-1.4.1.min.js"></script>

    <!-- / START - page related stylesheets [optional] -->
    
    <!-- / END - page related stylesheets [optional] -->
    <!-- / bootstrap [required] -->
    <link href="../assets/stylesheets/bootstrap/bootstrap.css" media="all" rel="stylesheet" type="text/css" />
    <!-- / theme file [required] -->
    <link href="../assets/stylesheets/light-theme.css" media="all" id="color-settings-body-color" rel="stylesheet" type="text/css" />
    <!-- / coloring file [optional] (if you are going to use custom contrast color) -->
    <link href="../assets/stylesheets/theme-colors.css" media="all" rel="stylesheet" type="text/css" />
    <!--[if lt IE 9]>
      <script src="assets/javascripts/ie/html5shiv.js" type="text/javascript"></script>
      <script src="assets/javascripts/ie/respond.min.js" type="text/javascript"></script>
    <![endif]-->
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
<div id='wrapper'>
<section id='content' style="margin-left: 0; padding-top: 20px;">
    <div class='container'>
      <div class='row' id='content-wrapper' style="padding: 0;">
        <div class='col-xs-12'>
          <div class='row'>
            <div class='col-sm-12'>
              <div class='box' style="margin:0;">
                <div class='row'>
                  <div class='chat'>
                    <div class='col-sm-12'>
                      <div class='box'>
                        <div class='box-content box-no-padding'>
                          <div class='scrollable' data-scrollable-height="<%= Integer.parseInt(StringUtils.defaultString(request.getParameter("height"), "480")) - 95 %>" data-scrollable-start='top'>
                            <ul class='list-unstyled list-hover list-striped main-stream'>
                            <%
                            for (String q: params.get("oq")) {
								boolean noCustomQA = true;
								
								QAContext qaCtx = new QAContext();
								qaCtx.setCurrentQuestion(q);
								qaCtx.setTenant(t);
								qaCtx.setClientSupportHtml(true);
								
								DictionaryDatabase[] currentKPs = new DictionaryDatabase[0];
								currentKPs = KnowledgePointDictionary.search(t.getId(), q.toCharArray(), null);
								if (currentKPs.length > 0){
									qaCtx.setRestrictToQaCategory(currentKPs[0].getCategory());
								}
								
								JSONObject conversation = qaCtx.getConversationsJson();
								qaCtx.setConversations("{}");
								
								QAOutputTemplate outputTpl = (QAOutputTemplate) outputClazz.newInstance();
								SolrDocument customDoc = qu.searchDirectCustomQA(qaCtx.getCurrentQuestion(), outputTpl, qaCtx);
								List<CustomData> nvPairs = (List<CustomData>)qaCtx.getRequestAttribute("nvPairs");
								qaCtx.setQuestionType(QAContext.QUESTION_TYPE.PROFESSIONAL);
								QADataAggregator aggregator = (QADataAggregator) aggregatorClazz.newInstance();
								
								//out.println("Query Debug:" + java.net.URLDecoder.decode(myQuery.toString(), "UTF-8"));
								//out.println("<HR>");
								String output = null;
								
								if (customDoc != null) {
									if (qaCtx.hasAnswerText()) {
										noCustomQA = false;
										output = qaCtx.getAnswerText().toString();
										if (t.getEnableQAExplain()) qaCtx.appendExplain("Got result from this QAPattern", output);
									}
								}
								else {
									if (t.getEnableQAExplain()) qaCtx.appendExplain("No best QAPattern", null);
								}
							%>
                              <li class='message'>
                                <div class='avatar'>
                                  <img alt='Avatar' height='23' src='../assets/images/avatar.jpg' width='23'>
                                </div>
                                <div class='name-and-time'>
                                  <div class='name pull-left'>
                                    <small>
                                      <a class="text-contrast" href="#"><%= nickname %></a>
                                    </small>
                                  </div>
                                  <div class='time pull-right'>
                                    <small class='date pull-right text-muted'>
                                      <span class='timeago fade has-tooltip' data-placement='top' title='<%= StringEscapeUtils.escapeHtml(date1) %>'><%= StringEscapeUtils.escapeHtml(date2) %></span>
                                      <i class='icon-time'></i>
                                    </small>
                                  </div>
                                </div>
                                <div class='body'>
                                   <%= StringEscapeUtils.escapeHtml(qaCtx.getCurrentQuestion()) %>
                                </div>
                              </li>
                              <li class='message'>
                                <div class='avatar'>
                                  <img alt='Avatar' height='23' src='../img/portrait_robot.png' width='23'>
                                </div>
                                <div class='name-and-time'>
                                  <div class='name pull-left'>
                                    <small>
                                      <a class="text-contrast" href="#"><bean:message key='robot.name'/></a>
                                    </small>
                                  </div>
                                  <div class='time pull-right'>
                                    <small class='date pull-right text-muted'>
                                      <span class='timeago fade has-tooltip' data-placement='top' title='<%= StringEscapeUtils.escapeHtml(date1) %>'><%= StringEscapeUtils.escapeHtml(date2) %></span>
                                      <i class='icon-time'></i>
                                    </small>
                                  </div>
                                </div>
                                <div class='body'>
                                  <% if (noCustomQA) { %>
                                  <bean:message key='problem.can.not.be.compared'/>
                                  <% } else { %>
                                   <bean:message key='preview.answer'/>：<%= output %>
                                  <% } %>
                                  <hr/>
                                  <bean:message key='special.restrictions'/>：<br>
                                  <%= WiSeUtils.nl2br(p.applySpecialRestrictionTemplate(null, qaCtx, nvPairs)) %>
                                </div>
                              </li>
                            <%
                            }
                            %>
                            </ul>
                          </div>
                          <form class="new-message" onsubmit="return false;" method="post" action="#" accept-charset="UTF-8"><input name="authenticity_token" type="hidden" /><input class='form-control' id='message_body' name='message[body]' placeholder='Type your message here...' type='text'>
                          <button class='btn btn-success' type='button'>
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
           </div>
            
            
            
            
                
          </div>
        </div>
      </div>
</section>
</div>
    <!-- / jquery [required] -->
    <script src="../assets/javascripts/jquery/jquery.min.js" type="text/javascript"></script>
    <!-- / jquery mobile (for touch events) -->
    <script src="../assets/javascripts/jquery/jquery.mobile.custom.min.js" type="text/javascript"></script>
    <!-- / jquery migrate (for compatibility with new jquery) [required] -->
    <script src="../assets/javascripts/jquery/jquery-migrate.min.js" type="text/javascript"></script>
    <!-- / jquery ui -->
    <script src="../assets/javascripts/jquery/jquery-ui.min.js" type="text/javascript"></script>
    <!-- / jQuery UI Touch Punch -->
    <script src="../assets/javascripts/plugins/jquery_ui_touch_punch/jquery.ui.touch-punch.min.js" type="text/javascript"></script>
    <!-- / bootstrap [required] -->
    <script src="../assets/javascripts/bootstrap/bootstrap.js" type="text/javascript"></script>
    <!-- / modernizr -->
    <script src="../assets/javascripts/plugins/modernizr/modernizr.min.js" type="text/javascript"></script>
    <!-- / retina -->
    <script src="../assets/javascripts/plugins/retina/retina.js" type="text/javascript"></script>
    <!-- / theme file [required] -->
    <script src="../assets/javascripts/theme.js" type="text/javascript"></script>
    <!-- / START - page related files and scripts [optional] -->
    <script src="../assets/javascripts/plugins/bootstrap_daterangepicker/bootstrap-daterangepicker.js" type="text/javascript"></script>
    <script src="../assets/javascripts/plugins/slimscroll/jquery.slimscroll.min.js" type="text/javascript"></script>
    <!-- <script src="../assets/javascripts/plugins/timeago/jquery.timeago.js" type="text/javascript"></script> -->
    <script type="text/javascript">
    /**
     * Timeago is a jQuery plugin that makes it easy to support automatically
     * updating fuzzy timestamps (e.g. "4 minutes ago" or "about 1 day ago").
     *
     * @name timeago
     * @version 1.2.0
     * @requires jQuery v1.2.3+
     * @author Ryan McGeary
     * @license MIT License - http://www.opensource.org/licenses/mit-license.php
     *
     * For usage and examples, visit:
     * http://timeago.yarp.com/
     *
     * Copyright (c) 2008-2013, Ryan McGeary (ryan -[at]- mcgeary [*dot*] org)
     */

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
</body>
</html>

