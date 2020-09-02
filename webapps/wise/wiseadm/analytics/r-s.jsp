<%@ include file="/commons/taglib.jsp"%>
<%@ page import="java.net.URLEncoder"
		 import="java.util.*"
		 import="java.util.regex.*"
		 import="org.apache.solr.core.*"
		 import="org.apache.solr.servlet.*"
		 import="org.apache.solr.client.solrj.*"
		 import="org.apache.solr.client.solrj.embedded.*"
		 import="org.apache.solr.client.solrj.response.*"
		 import="org.apache.solr.common.*"
		 import="org.apache.solr.common.util.*"
         import="org.apache.commons.lang.*"
         import="org.apache.commons.httpclient.methods.GetMethod"
         import="org.apache.commons.httpclient.HttpClient"
         import="org.apache.commons.httpclient.auth.AuthScope"
         import="org.apache.commons.httpclient.UsernamePasswordCredentials"
         import="org.dom4j.Document" 
         import="org.dom4j.DocumentHelper" 
         import="org.dom4j.Element" 
         import="com.intumit.message.*"
         import="com.intumit.solr.*" 
         import="com.intumit.solr.util.*" 
         import="com.intumit.solr.dataset.*"
         import="com.intumit.solr.SearchManager"
         import="com.intumit.solr.searchKeywords.*"
         import="it.exprivia.cnos.opencloud.*" 
         import="com.intumit.solr.config.ColumnNameMappingFacade"
         import="com.intumit.solr.config.ColumnNameMapping"
         import="com.intumit.solr.user.*"
		 contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
%><%!
public static String forwardJSP = "s.jsp";

java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
java.text.SimpleDateFormat sdfToday = new java.text.SimpleDateFormat("HH:mm");
java.text.DecimalFormat df = new java.text.DecimalFormat("$###,###");
String pricePattern = "(.*?):\\[([0-9\\*]+) TO ([0-9\\*]+)\\]";
String datePattern = "(.*?Date.*?):\\[(.*? TO .*?)\\]";
String fgPattern = "(.*?):\\[(.*? TO .*?)\\]";

public String getHighlightString(Map<String,Map<String,List<String>>>highlights, String field, String id, String def) {
	List<String> hlList = highlights.get(id).get(field);
	
	if (hlList != null) {
		for (String hl: hlList) {
			 return hl;
		}
	}

	return def;
}

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/analytics/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}

%>
<%
Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedView = (String) pageContext.findAttribute("selectedView");
User user = User.getFromSession(session);
boolean usingApi = false;
String apikey = request.getParameter("apikey");

if (user == null && apikey != null) {
	user = User.getByApiKey(apikey);
	if (user != null) usingApi = true;
}
String basicMakeUrlParams = (usingApi ? "apikey=" + apikey + "&" : "") + "v=" + selectedView;

try {
	Calendar today = Calendar.getInstance();
	today.set(Calendar.HOUR_OF_DAY, 0);
	today.set(Calendar.MINUTE, 0);
	today.set(Calendar.SECOND, 0);
	
	String qtime = pageContext.findAttribute("QTime").toString();
	DataSet ds = (DataSet)session.getAttribute("logDs");
	
	Map<String,Map<String,List<String>>> highlights = (Map<String,Map<String,List<String>>>) pageContext.findAttribute("highlights");
	SolrDocumentList docs = (SolrDocumentList) pageContext.findAttribute("results");
	HashSet<String> blacks = com.intumit.solr.blackKeywords.BlackKeywordFacade.getInstance().loadBlackList();
    int start = query.getStart();
    
    //For simplification, we are not using the JSTL
    SolrDocumentList docList = (SolrDocumentList) pageContext.findAttribute("results");
	SolrQuery sortByQuery = query.getCopy();
	
	String[] sortables = StringUtils.trimToEmpty(ds != null ? ds.getSortables() : "").split(",");

	StringBuilder paginationInfo = new StringBuilder();
	paginationInfo.append(MessageUtil.getMessage(locale, "search.number.found", new String[] {"" + docs.getNumFound(), "" + (start+1), "" + (start + query.getRows() > docs.getNumFound() ? docs.getNumFound() : start + query.getRows())}));
	StringBuilder pagination = new StringBuilder();
   		
	int currPage = (int)Math.floor(start / query.getRows())+1;
	if (currPage > 1) {
		SolrQuery nextPageQuery = query.getCopy();
		nextPageQuery.setStart(0);
		
		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP,  basicMakeUrlParams + "&" + nextPageQuery.toString()));
		pagination.append("\"><i class=\"icon-backward\"></i>&nbsp;</A>");
	}
	for (int po=-4; po <= 4; po++) {
		int nStart = start + query.getRows() * po;
		if (nStart < 0 || nStart >= docs.getNumFound())
			continue;

		if (po == 0) {
			pagination.append("&nbsp;<A class=\"btn active\" HREF=\"#\">");
			pagination.append((currPage+po));
			pagination.append("</A>");
			continue;
		}
		SolrQuery toPageQuery = query.getCopy();
		toPageQuery.setStart(nStart);
		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + toPageQuery.toString()));
		pagination.append("\">");
		pagination.append((currPage+po));
		pagination.append("</A>");
	}
	int lastPage = (int)Math.floor((docs.getNumFound()-1) / query.getRows()) + 1;
	if (lastPage != currPage) {
		SolrQuery nextPageQuery = query.getCopy();
		nextPageQuery.setStart((lastPage-1)*query.getRows());
		
		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + nextPageQuery.toString()));
		pagination.append("\"><i class=\"icon-forward\"></i>&nbsp;</A>");
	}
    %>
    <div align="left" class="text hidden-phone">
   		<div class="row" style="margin-left: 30px;">
			<div class="col-md-6">
	       		<div class="btn-toolbar">
	       		<div class="btn-group">
				<%= pagination %>
				</div>
				</div>
				
			</div>
			<div class="col-md-6" align="right" >
			<%= MessageUtil.getMessage(locale, "search.spend.second", new String[] {"" + qtime}) %>
			<br/>
         	Sort by
         		<A HREF="<%= makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + sortByQuery.setSortField("Date_dt", SolrQuery.ORDER.desc).toString()) %>">Date <img src="<%= request.getContextPath() %>/img/icon_desc.gif"></A>
         		<A HREF="<%= makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + sortByQuery.setSortField("Date_dt", SolrQuery.ORDER.asc).toString()) %>"> <img src="<%= request.getContextPath() %>/img/icon_asc.gif"></A>
         		&nbsp;&nbsp;<A HREF="<%= makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + sortByQuery.setSortField("score", SolrQuery.ORDER.desc).toString()) %>">Relevance <img src="<%= request.getContextPath() %>/img/icon_desc.gif"></A>
         		<%
         		for (int sss=0; sss < sortables.length; sss++) {
                 	String colName = sortables[sss];

                 	if (StringUtils.isEmpty(colName)) continue;

                 	ColumnNameMapping columnName = ColumnNameMappingFacade.getInstance().get(sortables[sss]);
                 	if(columnName !=null && columnName.getColumnName() != null)
                 		colName = columnName.getMappingName();
                 	else
                 		colName = null;
         		%>
         		&nbsp;&nbsp;<A HREF="<%= makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + sortByQuery.setSortField(sortables[sss], SolrQuery.ORDER.desc).toString()) %>"><%= colName %> <img src="img/icon_desc.gif"></A>
         		<A HREF="<%= makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + sortByQuery.setSortField(sortables[sss], SolrQuery.ORDER.asc).toString()) %>"><img src="img/icon_asc.gif"></A>
         		<%
         		}
         		%>
	         	&nbsp; <%= docs.getNumFound() > 0 ? "<BR/>" + paginationInfo : "" %>
			</div>
		</div>
    </div>
    <ul class="list-unstyled">
     	<% if(docList !=null && docList.size() < 1){ %>
	    	<div id="noresult">
	    	<div class="alert alert-error">
很抱歉，我們無法找到符合“<em><%= query.getQuery() %></em>”的查詢結果。<br />
建議您：<br />
1.	請縮短查詢字組來擴大您的搜尋範圍。<br />
2.	請檢查查詢字有無錯別字。<br />
3.	請以新的關鍵字或相似詞重新查詢。<br />
若您對搜尋查詢結果仍有疑問，歡迎寫信到<a style='font-size: medium;' href='mailto:support@intumit.com'>support@intumit.com</a>，獲得更多建議。
     		</div>
     		</div>
     	<% } %>
      	<%
      	int i=0;
        	
		for (Iterator<SolrDocument> iterator = docList.iterator(); iterator.hasNext(); i++)
		{
           	SolrDocument result = iterator.next();
            String id = result.getFieldValue("id").toString();
			String summary = "";
			String type = result.getFieldValue("SpecialMark_s").toString();
			
			//boolean ellipseBefore = false;
			//boolean ellipseAfter = false;
			String sort = query.getSortField();
			String sortFieldName = null;
			String title = getHighlightString(highlights, "Question_t", id, StringUtils.defaultIfEmpty((String)result.getFirstValue("Name_t"), "無標題"));
			String board = StringUtils.trimToEmpty((String)result.getFirstValue("QuestionCategory_ms"));
			List<String> keywords = (List<String>)result.getFieldValue("KnowledgePoint_ms");
			String author = StringUtils.trimToEmpty((String)result.getFirstValue("Author_t"));
			String snippet = StringUtils.trimToEmpty((String)result.getFirstValue("Snippet_t"));
			Integer hot = (Integer)result.getFirstValue("Hot_i");
			Integer reply = (Integer)result.getFirstValue("Reply_i");
			Integer share = (Integer)result.getFirstValue("Share_i");
			String replyStr = reply != null ? reply.toString() : "";
			boolean isMsg = StringUtils.equals("QAMessage", type);
			String threadPos = null;
			try {
				threadPos = isMsg ? "#" + (Integer.parseInt(StringUtils.substringAfterLast(id, "-")) + 1) : "";
			}
			catch (Exception ignoreIt) {}
			String target = "_new";
			String highlightFieldName = "Answer_mt";
			Date articleDate = (Date)result.getFieldValue("Date_dt");
			String fullDateStr = "";
			String dateStr = "";
			if (articleDate != null) {
				fullDateStr = sdf.format(articleDate);
				
				if (articleDate.before(today.getTime())) {
					dateStr = sdf.format(articleDate);
				}
				else {
					dateStr = sdfToday.format(articleDate);
				}
			}
			
			if (sort != null) {
               	sort = sort.substring(0, sort.indexOf(" "));


                  	if (StringUtils.isNotEmpty(sort)) {
                   	ColumnNameMapping columnName = ColumnNameMappingFacade.getInstance().get(sort);
                   	if(columnName !=null && columnName.getColumnName() != null)
                   		sortFieldName = columnName.getMappingName();
                   	else
                   		sortFieldName = null;
                  	} 
			}
			
            SolrQuery mlt = query.getCopy();
            mlt.setQuery("*:*")
            	.addFilterQuery("Name_t:\"" + title + "\"");

			if (result.getFieldValue(highlightFieldName) != null) {
               	summary = result.getFirstValue(highlightFieldName).toString().trim();
			}
			
			String detailUrl = request.getContextPath() + "/wiseadm/qaServiceLogDetail.jsp?id=" + id.replaceAll("-", "#");
			%>
			<li>
            <table class="table table-striped">
            	<tr valign="top">
                	<td class="op-title col-md-4">
						<a onmousedown="return clk(this.href, this.title)" target="_new" HREF="<%= detailUrl %>" title="<%= StringEscapeUtils.escapeHtml(title) + ",開新視窗" %>" <%= target == null ? "" : ("target=\"" + target + "\"") %>>
						&nbsp;<%= StringUtils.left(title, 60) + (title.length() > 60 ? "..." : "") %>
						</a>
						&nbsp;<a href="<%= makeUrl(request, forwardJSP, basicMakeUrlParams + "&" + mlt.toString()) %>"><%= isMsg ? threadPos : "#對話串" %></a>
						<span class="normal">&nbsp;<%= StringUtils.trimToNull(board) != null ? "[" + board + "]" : "" %></span>
						<br>
						<span>
						<%
						if (keywords != null) {
							int counter = 0;
							for (String keywordName: keywords) {
								if (counter++ > 0) {
									out.println("&nbsp;&nbsp;&nbsp;");
								}
								
		                		%><a href="#" class="text text-warning"><i class="icon-tag"></i><%= keywordName %></a>&nbsp;<%
							}
						}
						%>
						</span>
					</td>
					<td class="col-md-6" >
					<p>
                        <%= summary %>
					</p>
					</td>
					<td class="col-md-2">
					 	<span class='text-danger muted timeago' title='<%= fullDateStr %>'>
					 	<%= dateStr %>
					 	</span>
					</td>
				</tr>                                             
				</table>
				</li>
			<%
			}
			%>

 	<div align="left" class="text">
   		<div class="btn-toolbar">
   		<div class="btn-group">
		<%= pagination %>
		</div>
		</div>
	</div>
<%
}
catch (Exception e) {
	e.printStackTrace();
%>
	<div id="noresult">
	<div class="alert alert-error">
	很抱歉，我們無法找到符合條件的查詢結果。<br />
	建議您：<br />
	1.	請縮短查詢字組來擴大您的搜尋範圍。<br />
	2.	請檢查查詢字有無錯別字。<br />
	3.	請以新的關鍵字或相似詞重新查詢。<br />
	</div>
	</div>
<%
}
%>
         
