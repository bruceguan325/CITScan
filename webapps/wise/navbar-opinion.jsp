<%@page import="com.intumit.message.MessageUtil"%>
<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" 
	pageEncoding="UTF-8" language="java" 
	import="java.util.*"
	import="com.intumit.solr.user.*"
	%>
<%
//User user = User.getFromSession(session);
String[] navLinks = {};
String[] navIcons = {};
String[] navTitles = {};
Map<String, String[]> submenu = new HashMap<String, String[]>();

Locale locale = Locale.getDefault();

navLinks = new String[] {"chats.jsp", "#", "#"};
navIcons = new String[] {"icon-dashboard", "icon-star", "icon-wrench"};
navTitles = new String[] {MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "global.homepage"), 
		MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "my.favorite"), 
		MessageUtil.getMessage((Locale)request.getSession().getAttribute("org.apache.struts.action.LOCALE"), "global.config")};

/*
if (user.getFacebookAnalysis()) {
	navLinks = new String[] {"index.jsp", "keywords.jsp", "report.jsp", null, "alert.jsp", null};
	navIcons = new String[] {"icon-dashboard", "icon-check", "icon-bar-chart", "icon-facebook", "icon-bell", "icon-wrench"};
	navTitles = new String[] {"儀表板", "已訂閱關鍵字", "統計及圖表", "臉書數據分析", "狀況分級顯示", "配置"};

	if (user.getKeywordAnalysis()) {
		submenu.put("臉書數據分析L", new String[] {"fbcompare.jsp", "fbanalysis.jsp", "fbhitft.jsp"});
		submenu.put("臉書數據分析I", new String[] {"icon-check", "icon-microphone", "icon-book"});
		submenu.put("臉書數據分析T", new String[] {"粉絲團比較", "粉文分布", "內容分析"});
	}
	else {
		submenu.put("臉書數據分析L", new String[] {"fbcompare.jsp", "fbanalysis.jsp"});
		submenu.put("臉書數據分析I", new String[] {"icon-check", "icon-microphone"});
		submenu.put("臉書數據分析T", new String[] {"粉絲團比較", "粉文分布"});
	}
	
	submenu.put("配置L", new String[] {"keyword-pref.jsp", "alert-pref.jsp", "user-source-pref.jsp?source=fb_fanpage", "account-pref.jsp"});
	submenu.put("配置I", new String[] {"icon-check", "icon-exclamation", "icon-microphone", "icon-book"});
	submenu.put("配置T", new String[] {"關鍵字設定", "警示設定", "來源設定", "帳戶設定"});
}
else {
	
	if (user.getKeywordAnalysis()) {
		navLinks = new String[] {"index.jsp", "keywords.jsp", "report.jsp", null, "alert.jsp", null};
		navIcons = new String[] {"icon-dashboard", "icon-check", "icon-bar-chart", "icon-comments", "icon-bell", "icon-wrench"};
		navTitles = new String[] {"儀表板", "已訂閱關鍵字", "統計及圖表", "進階分析", "狀況分級顯示", "配置"};
		submenu.put("進階分析L", new String[] {"s-opinion.jsp?d=1", "s-hitft.jsp"});
		submenu.put("進階分析I", new String[] {"icon-check", "icon-book"});
		submenu.put("進階分析T", new String[] {"多維度搜尋", "內容分析"});
	}
	else {
		navLinks = new String[] {"index.jsp", "keywords.jsp", "report.jsp","alert.jsp", null};
		navIcons = new String[] {"icon-dashboard", "icon-check", "icon-bar-chart", "icon-bell", "icon-wrench"};
		navTitles = new String[] {"儀表板", "已訂閱關鍵字", "統計及圖表", "狀況分級顯示", "配置"};
	}
	
	submenu.put("配置L", new String[] {"keyword-pref.jsp", "alert-pref.jsp", "user-source-pref.jsp?source=fb_fanpage", "account-pref.jsp"});
	submenu.put("配置I", new String[] {"icon-check", "icon-exclamation", "icon-microphone", "icon-book"});
	submenu.put("配置T", new String[] {"關鍵字設定", "警示設定", "來源設定", "帳戶設定"});
}
*/
//submenu.
//submenu.put("配置LINKS", new String[] {"index.jsp", "keywords.jsp", "report.jsp", "alert.jsp", "preference.jsp"});
//String[] navIcons = {"icon-dashboard", "icon-check", "icon-bar-chart", "icon-bell", "icon-wrench"};
//String[] navTitles = {"儀表板", "已訂閱關鍵字", "統計及圖表", "狀況分級顯示", "配置"};
%><%
String currentPage = request.getRequestURI();
%>
<ul class='nav nav-stacked'>
	<%
	for (int i=0; i < navLinks.length; i++) {
		String link = navLinks[i];
		
		if (link != null) {
			%>
			<li class='<%= (request.getContextPath() + "/" + navLinks[i]).equals(currentPage) ? "active" : "" %>'>
			    <a href='<%= navLinks[i] %>'>
			        <i class='<%= navIcons[i] %>'></i>
			        <span><%= navTitles[i] %></span>
			    </a>
			</li>
		    <%
		}
		else {
			String[] subLinks = submenu.get(navTitles[i] + "L");
			String[] subIcons = submenu.get(navTitles[i] + "I");
			String[] subTitles = submenu.get(navTitles[i] + "T");
			%>
			<li>
		    <a class='dropdown-collapse' href='#'>
			    <i class='<%= navIcons[i] %>'></i>
			    <span><%= navTitles[i] %></span>
		        <i class='icon-angle-down angle-down'></i>
		    </a>
		    
		    <ul class='nav nav-stacked' id='nav-stack-<%= i %>'>
		    <%
		    for (int j=0; j < subLinks.length; j++) {
				%>
				<li class='<%= (request.getContextPath() + "/" + subLinks[j]).equals(currentPage) ? "active" : "" %>'>
				    <a href='<%= subLinks[j] %>'>
				        <i class='<%= subIcons[j] %>'></i>
				        <span><%= subTitles[j] %></span>
				    </a>
				    <%= (request.getContextPath() + "/" + subLinks[j]).equals(currentPage) ? "<script>$('#nav-stack-" + i + "').addClass('in');</script>" : "" %>
				</li>
			    <%
		    }
		    %>
		    </ul>
		    </li>
		    <%
		}
	}
    %>
</ul>
