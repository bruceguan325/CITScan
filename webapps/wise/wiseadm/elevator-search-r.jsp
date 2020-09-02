<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
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
         import="com.intumit.solr.*"
         import="com.intumit.solr.util.*"
         import="com.intumit.solr.dataset.*"
         import="it.exprivia.cnos.opencloud.*"
         import="com.intumit.solr.config.ColumnNameMappingFacade"
         import="com.intumit.solr.config.ColumnNameMapping"
         import="com.intumit.solr.admin.*"
		 contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
%><%!
   public void travel_tree(String esId, String dsId, Element et, SolrQuery query, String fieldName, String parent, StringBuffer sb) {
        boolean tag_closed = true;
        String lastPath = null;

        for (Iterator i = et.elementIterator(); i.hasNext();) {
            Element elt = (Element) i.next();

            if (elt.getName().equals("int")) {

                if (!tag_closed)
                    sb.append("</li>\n");

                String cat = elt.valueOf("@name");
                String fullPath = lastPath = parent + cat + "/";
                SolrQuery newQuery = query.getCopy();
                newQuery.addFilterQuery(fieldName + ":" + fullPath);
                sb.append("<li><a onclick=\"window.location='elevator-search.jsp?id=" + esId + "&d=" + dsId + "&" + newQuery.toString() + "'\" href=\"#\">" + translate(cat)
                		+ "&nbsp;<span class=\"badge\">" + Integer.valueOf(elt.getText()) + "</span>" + "</a>");

                tag_closed = false;
            }

            if (elt.getName().equals("lst")) {
                sb.append("\n<ul>\n");

                travel_tree(esId, dsId, elt, query, fieldName, lastPath, sb);

                sb.append("</ul>\n");
                tag_closed = true;
            }

        }
        sb.append("</li>\n");

    }
%>
<%!
java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
java.text.DecimalFormat df = new java.text.DecimalFormat("$###,###");
String pricePattern = "(.*?):\\[([0-9\\*]+) TO ([0-9\\*]+)\\]";
String datePattern = "(.*?_dt.*?):\\[(.*? TO .*?)\\]";
String fgPattern = "(.*?):\\[(.*? TO .*?)\\]";

HashMap<String, String> translateMap = new HashMap<String, String>();
{
	translateMap.put("NOW/DAY TO NOW/DAY+1DAY", "近一日");
	translateMap.put("NOW/DAY+1DAY-3DAY TO NOW/DAY+1DAY", "近三日");
	translateMap.put("NOW/DAY+1DAY-1MONTH TO NOW/DAY+1DAY", "近一個月");
	translateMap.put("NOW/DAY+1DAY-3MONTH TO NOW/DAY+1DAY", "近三個月");
	translateMap.put("NOW/DAY+1DAY-6MONTH TO NOW/DAY+1DAY", "近半年");
	translateMap.put("NOW/DAY+1DAY-1YEAR TO NOW/DAY+1DAY", "近一年");
	translateMap.put("NOW/DAY+1DAY-3YEAR TO NOW/DAY+1DAY", "近三年");
	translateMap.put("* TO NOW/DAY+1DAY-6MONTH", "超過半年以前");
	translateMap.put("* TO NOW/DAY-3YEAR", "超過三年以前");
	translateMap.put("", "(N/A)");
    translateMap.put("score", "精確度");
}

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
	String url = req.getContextPath() + WiSeEnv.getAdminContextPath() + "/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}

public String translate(String org) {
	if (org == null)
		return "";

	if (org.matches(fgPattern)) {
		if (org.matches(pricePattern)) {
			Matcher m = Pattern.compile(pricePattern).matcher(org);
			m.find();
			String fieldName = m.group(1);
			String priceFrom = m.group(2);
			String priceTo = m.group(3);

			if ("*".equals(priceFrom)) {
				return df.format(Integer.parseInt(priceTo)) + " 以下";
			}
			if ("*".equals(priceTo)) {
				return df.format(Integer.parseInt(priceFrom)) + " 以上";
			}

			return df.format(Integer.parseInt(priceFrom)) + " ~ " + df.format(Integer.parseInt(priceTo));
		}
		if (org.matches(datePattern)) {
			Matcher m = Pattern.compile(datePattern).matcher(org);
			m.find();
			String fieldName = m.group(1);
			String dateRange = m.group(2);


			if (translateMap.containsKey(dateRange))
				return translateMap.get(dateRange);
			else
				return dateRange;
		}
		return org;
	}
	else {
		if(StringUtils.isNotBlank(ColumnNameMappingFacade.getInstance().getMappingName(org))){
			return ColumnNameMappingFacade.getInstance().getMappingName(org);
		}
		if (translateMap.containsKey(org))
			return translateMap.get(org);
		else
			return org;
	}
}
%>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<%
	AdminGroup admGrp = AdminGroupFacade.getInstance().getFromSession(session);
	if (admGrp == null || admGrp.getDataSetAdminCURD() == 0) {
		return;
	}

	SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
	String dsId = (String) pageContext.findAttribute("d");
	int acl = AdminGroupFacade.getInstance().getDataSetViewAcl(session, Integer.parseInt(dsId));
	if ((acl & GroupDataSet.O4) == 0) return; // 無權限

	ElevatorSet eSet = (ElevatorSet) pageContext.findAttribute("eSet");
	String esId = "" + eSet.getId();
	DataSet ds = DataSetFacade.getInstance().get(new Integer(dsId));
	Map<String,Map<String,List<String>>> highlights = (Map<String,Map<String,List<String>>>) pageContext.findAttribute("highlights");

	List<String> fixedAtTop = new ArrayList<String>();
	List<String> hiddened = new ArrayList<String>();
	List<String> facetQueryFixedAtTop = new ArrayList<String>();

	if (eSet != null) {
		fixedAtTop = eSet.getFixedAtTopAsList();
		hiddened = eSet.getHiddenList();
		facetQueryFixedAtTop = eSet.getFacetQueryFixedAtTopAsList();
	}
	SolrDocumentList docs = (SolrDocumentList) pageContext.findAttribute("results");
    int start = query.getStart();
    String forwardJSP = "elevator-search.jsp";
%>
<link href="<%= request.getContextPath() %>/styles/bootstrap.min.css" type="text/css" rel="stylesheet"/>
<link href="<%= request.getContextPath() %>/styles/bootstrap-responsive.min.css" type="text/css" rel="stylesheet"/>
<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js" type="text/javascript"></script>
<script src="<%=request.getContextPath()%>/script/bootstrap.min.js" type="text/javascript"></script>
<style>
.ui-state-default {
    font-size: 0.9em;
    text-align: left;
}

.cattext {
	color: green;
}

.ad {
	list-style: none;
	margin: 0;
	padding: 2px 5px 0 6px;
}

em {
	font-style: normal;
	font-weight: bold;
	color: red;
}
</style>
<script>
	window.clk=function(u,kw,docid,v,w){
		  var a=encodeURIComponent||escape;
		  window.location=["<%= request.getContextPath() %>/url.jsp?url=",a(u), "&kw=", a(kw), "&ttl=", a(v), "&dd=", a(docid), "&func=", a(w)].join("")
		  return true;
	};

	var tree1;
    var conf = {
        ui : {
            dots :false,
            hover_mode : false,
            theme_name :"default",
            context : []
        }
    };

    jQuery( function($) {
        $( function() {
            tree1 = $.tree_create();
            tree1.init($("#facet_tree1"), $.extend( {}, conf));
        });
    });
</script>
</head>
<body width="100%" border="0" cellpadding="0" cellspacing="0">
<div class="container">
		<div class="hero-unit">
		<h1>搜尋排序調整介面</h1>
		</div>
        <%
		Map<String, Long> coreCount = (Map<String, Long>) pageContext.findAttribute("coreCount");
		if (coreCount != null && coreCount.size() > 1) {
			%>
	   		<div id="tabs1">
	   			<ul class="nav nav-tabs">
				<%
	   			List<DataSet> dsList = DataSetFacade.getInstance().listAllEnabled();
           		if ("0".equals(dsId))
           			dsId = "" + dsList.get(0).getId();

				for (DataSet currDs: dsList)
				{
					Long ccValue = coreCount.get("" + currDs.getId());//iterator.next();

					int currAcl = AdminGroupFacade.getInstance().getDataSetViewAcl(session, currDs.getId());
					boolean hasPerm = (currAcl & GroupDataSet.O4) != 0; // 無權限

					if (hasPerm) {
					%>
					<li <%= currDs.getId() == new Integer(dsId) ? "id='current' class='active'" : "" %>>
					<a href="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + currDs.getId() + "&q=" + URLEncoder.encode(query.getQuery(), "UTF-8") ) %>">
					<%= currDs.getName() %> (<%= ccValue %>)
					</a>
					</li>
				<%
					} else {
					%>
					<li style="cursor: pointer;" ><a href="#" style="cursor: pointer;" title="No permission"><%= currDs.getName() %> (<%= ccValue %>)</a></li>
					<%
					}
					%>
				<%
				}
				%>
	   			</ul>
	   		</div>
			<%
		}
	    %>
	<div class="row">
    <div class="span2 well">
        <ol class="unstyled">
        <%
		Map<String, Integer> facetQuerys = (Map<String, Integer>) pageContext.findAttribute("facetQueryResults");
		if (facetQuerys != null)
		{
			boolean has = false;
			TreeSet<Map.Entry<String, Integer>> sorted = WiSeUtils.getSortedTreeSet();

			// Tag Cloud 功能 (Begin)
			Cloud cloud = new Cloud(java.util.Locale.TAIWAN);
			cloud.setMaxTagsToDisplay(30);
			cloud.setMaxWeight(10);
			cloud.setMinWeight(1);
			cloud.setDefaultLink(forwardJSP+"?id=" + esId + "&d=" + dsId + "&q=%s");
			// Tag Cloud 功能 (End)

           	for (Iterator<Map.Entry<String, Integer>> iter = facetQuerys.entrySet().iterator(); iter.hasNext(); ) {
           		Map.Entry<String, Integer> entry = iter.next();

           		if (entry.getKey().startsWith("__")) {
					String val = entry.getKey().substring(2);
           			int ccc = entry.getValue();
           			if (ccc == 0) continue;

          				has = true;
          				sorted.add(entry);

					// 有使用 Tag Cloud 就要加上此行
          				cloud.addTag(new Tag(val, forwardJSP + "?id=" + esId + "&d=" + dsId + "&q="+URLEncoder.encode(val,"utf-8"), ccc));
               	}
			}

           	if (false) {//has) {
				%>
				<br/>
				<div><span class="label"><font color=ORANGE><%= "延伸搜尋" %></font></span><BR>
				<!-- Tag Cloud Start -->
					<div class="tagcloud" style="margin: auto; width: 90%;">
					<%
					// cycle through output tags
					for (it.exprivia.cnos.opencloud.Tag tag : cloud.tags()) {
						// add a link for each tag
						// we use the method getWeight() method to set the font size of the link
					%>
						<a href="<%= tag.getLink() %>" class="t<%= tag.getWeightInt() %>"><%= tag.getName() %></a>
					<%

					}

					%>
					</div>
				<!-- Tag Cloud End -->
				<BR>
			<%
			}
			if (false) {//has) {
				%>
					<div><span class="label"><font color=ORANGE><%= "再搜尋" %></font></span><BR>
					<%
					int showCount = 0;
	            	for (Iterator<Map.Entry<String, Integer>> iter = sorted.iterator(); iter.hasNext(); ) {
	            		Map.Entry<String, Integer> entry = iter.next();
            			String val = entry.getKey().substring(2);
            			int ccc = entry.getValue();
            			if (ccc == 0) continue;
						if (++showCount > 8) break;
	                	SolrQuery newQuery = query.getCopy();
	                	newQuery.setStart(0);
	                	newQuery.addFilterQuery(val);
	                    %>
	                    <span class="facet">
	                    	<a href="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + newQuery.toString()) %>"><%= val %> (<%= ccc %>)</a>
	                    </span><BR>
	                    <%
	                }
               	%><BR><%
            	}
			}
        	List<String> queryList = new ArrayList<String>();

            com.intumit.solr.dataset.DataSet.FacetQuery[] dsFQs = ds.getFQ();
            for (int jjj = 0; dsFQs != null && jjj < dsFQs.length; jjj++) {

				queryList = dsFQs[jjj].facetQuries();

				facetQuerys = (Map<String, Integer>) pageContext.findAttribute("facetQueryResults");
				if (facetQuerys != null)
				{
            	%>
  					<H3><%= translate(dsFQs[jjj].getFieldName()) %></H3>
              	<%
  					int prevCcc = 0;

	            	for (String key: queryList) {
	            		if (facetQuerys.containsKey(key)) {
	            			int ccc = facetQuerys.get(key).intValue();

	            			if (ccc > 0 && ccc != prevCcc) {
	            				prevCcc = ccc;
			                	SolrQuery newQuery = query.getCopy();
			                	newQuery.setStart(0);
			                	newQuery.addFilterQuery("(" + key + ")");

			                	String mappedName = dsFQs[jjj].mappingName(key);
			                    %>
			                    <span class="facet">
			                    	<a href="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + newQuery.toString()) %>"><%= translate(key) %>
			                    	<span class="badge"><%= ccc %></span></a>
			                    </span><BR>
			                    <%
                    		}
	                	}
	                }
                	%><BR/><%
				}

            }

			List<FacetField> facetFields = (List<FacetField>) pageContext.findAttribute("facetFieldResults");
            if (facetFields != null)
            {
	            for (Iterator<FacetField> iterator = facetFields.iterator(); iterator.hasNext();)
	            {
	                FacetField facetEnt = iterator.next();

	                if (facetEnt.getValues() != null)
	                {
                    	%>
	    				<H3><%= translate(facetEnt.getName()) %></H3>
	                  	<%
			            for (Iterator<FacetField.Count> iterator2 = facetEnt.getValues().iterator(); iterator2.hasNext();)
			            {
	                		FacetField.Count facetCount = iterator2.next();

	                		if (facetCount.getCount() == 0)
	                			continue;

		                	SolrQuery newQuery = query.getCopy();
		                	newQuery.setStart(0);

		                	String qStr = facetEnt.getName() + ":\"" + facetCount.getName() + "\"";
		                	newQuery.addFilterQuery( qStr );

		                	boolean isFixedAtTop = facetQueryFixedAtTop.contains(qStr);
		                    %>
		                    <span class="facet">
		                    	<a href="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + newQuery.toString()) %>"><%=translate(facetCount.getName())%>
		                    	<span class="badge"><%= facetCount.getCount() %></span></a>
		                    </span>

							<td width=15% rowspan=2 align="center">
							<% if (isFixedAtTop) { %>
							<a class="btn btn-primary" href="elevator-update.jsp?d=<%= dsId %>&id=<%= esId %>&action=removeFQFAT&fq=<%= URLEncoder.encode(qStr, "UTF-8") %>"><i class="icon-remove icon-white"></i></a><br/>
							<% } else { %>
							<a class="btn btn-success" href="elevator-update.jsp?d=<%= dsId %>&id=<%= esId %>&action=addFQFAT&fq=<%= URLEncoder.encode(qStr, "UTF-8") %>"><i class="icon-thumbs-up icon-white"></i></a><br/>
							<% } %>
							</td>
		                    <BR>
		                    <%
	                    }
                   	%><BR><%
					}
                }
			}
			%>
		<div style="clear: both;width: 110;padding: 0">&nbsp;</div>
		</ol>
	</div>
	<div class="span9">
        <%
        //For simplification, we are not using the JSTL
        SolrDocumentList docList = (SolrDocumentList) pageContext.findAttribute("results");
		SolrQuery sortByQuery = query.getCopy();

		String[] sortables = StringUtils.trimToEmpty(ds.getSortables()).split(",");


        %>
        <div align="center" class="text">
         	依照
         		<A HREF="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + sortByQuery.setSortField("Date_dt", SolrQuery.ORDER.desc).toString()) %>">日期 <img src="<%= request.getContextPath() %>/img/icon_desc.gif"></A>
         		<A HREF="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + sortByQuery.setSortField("Date_dt", SolrQuery.ORDER.asc).toString()) %>"> <img src="<%= request.getContextPath() %>/img/icon_asc.gif"></A>
         		&nbsp;&nbsp;<A HREF="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + sortByQuery.setSortField("score", SolrQuery.ORDER.desc).toString()) %>">相關性 <img src="<%= request.getContextPath() %>/img/icon_desc.gif"></A>
         		<A HREF="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + sortByQuery.setSortField("score", SolrQuery.ORDER.asc).toString()) %>"><img src="<%= request.getContextPath() %>/img/icon_asc.gif"></A>
         		<%

				ColumnNameMappingFacade cm = ColumnNameMappingFacade.getInstance();


         		for (int sss=0; sss < sortables.length; sss++) {
                 	String colName = sortables[sss];

                 	if (StringUtils.isEmpty(colName)) continue;

                 	ColumnNameMapping columnName = cm.get(sortables[sss]);
                 	if(columnName !=null && columnName.getColumnName() != null)
                 		colName = columnName.getMappingName();
                 	else
                 		colName = null;
         		%>
         		&nbsp;&nbsp;<A HREF="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + sortByQuery.setSortField(sortables[sss], SolrQuery.ORDER.desc).toString()) %>"><%= translate(colName) %> <img src="<%= request.getContextPath() %>/img/icon_desc.gif"></A>
         		<A HREF="<%= makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + sortByQuery.setSortField(sortables[sss], SolrQuery.ORDER.asc).toString()) %>"><img src="<%= request.getContextPath() %>/img/icon_asc.gif"></A>
         		<%
         		}
         		%>
       		<%
       		StringBuilder paginationInfo = new StringBuilder();
       		paginationInfo.append("， 共查詢到 ");
       		paginationInfo.append(docs.getNumFound());
       		paginationInfo.append(" 筆 / 目前顯示第");
       		paginationInfo.append((start+1));
       		paginationInfo.append("-");
       		paginationInfo.append((start + query.getRows() > docs.getNumFound() ? docs.getNumFound() : start + query.getRows()));
       		paginationInfo.append("筆 ");
         		StringBuilder pagination = new StringBuilder();
         		%>
	         	&nbsp;排序 <%= docs.getNumFound() > 0 ? paginationInfo : "" %>
		<%
		int currPage = (int)Math.floor(start / query.getRows())+1;
		if (currPage > 1) {
			SolrQuery nextPageQuery = query.getCopy();
			nextPageQuery.setStart(0);

				pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
				pagination.append(makeUrl(request, forwardJSP,  "id=" + esId + "&d=" + dsId + "&" + nextPageQuery.toString()));
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
				pagination.append(makeUrl(request, forwardJSP, "id=" + esId + "&d=" + dsId + "&" + toPageQuery.toString()));
			pagination.append("\">");
			pagination.append((currPage+po));
			pagination.append("</A>");
		}
		int lastPage = (int)Math.floor((docs.getNumFound()-1) / query.getRows()) + 1;
		if (lastPage != currPage) {
			SolrQuery nextPageQuery = query.getCopy();
			nextPageQuery.setStart((lastPage-1)*query.getRows());

				pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
				pagination.append(makeUrl(request, forwardJSP, "id=" + esId + "&d=" + dsId + "&" + nextPageQuery.toString()));
				pagination.append("\"><i class=\"icon-forward\"></i>&nbsp;</A>");
		}
		%>
       		<div class="btn-toolbar">
       		<div class="btn-group">
			<%= pagination %>
	        </div>
			</div>
        </div>
        <ul class="unstyled">
         	<% if(docList !=null && docList.size() < 1){ %>
		    	<div id="noresult">
		    	<div class="alert alert-error">
很抱歉，我們無法找到符合“<em><%= query.getQuery() %></em>”的查詢結果。<br />
建議您：<br />
1.	請縮短查詢字組來擴大您的搜尋範圍。<br />
2.	請檢查查詢字有無錯別字。<br />
3.	請以新的關鍵字或相似詞重新查詢。<br />
若您對搜尋查詢結果仍有疑問，歡迎寫信到<a style='font-size: medium;' href='mailto:support@hithot.cc'>support@hithot.cc</a>，獲得更多建議。
         		</div>
         		</div>
         	<% } %>
          	<%
          	int i=0;
			for (Iterator<SolrDocument> iterator = docList.iterator(); iterator.hasNext(); i++)
			{
	           	SolrDocument result = iterator.next();
				String id = (String)result.getFieldValue("id");
	           	boolean isFixedAtTop = fixedAtTop.contains(id);
	           	boolean isHiddened = hiddened.contains(id);
				String summary = "";
				String sort = query.getSortField();
				String sortFieldName = null;


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

				if (result.getFieldValue("Description_mt") != null) {
	               	summary = (result.getFirstValue("Description_mt").toString().trim().length() > 100)
	               		? result.getFirstValue("Description_mt").toString().trim().substring(0, 100) + "..."
	               		: result.getFirstValue("Description_mt").toString().trim();
				}

				String orgUrl = "/v.jsp?id=";
				String target = null;

				%>
				<li>
                <table class="table">
                	<tr valign="top">
                    	<td width=65%>
							<H3><a onmousedown="return clk(this.href,'<%= StringEscapeUtils.escapeHtml(query.getQuery()) %>', '<%= id %>', this.title, this.d)" target="_new" d="<%= dsId %>" HREF="<%= orgUrl %>" title="<%= StringEscapeUtils.escapeHtml((String)result.getFieldValue("c_name_t")) %>" <%= target == null ? "" : ("target=\"" + target + "\"") %>>
							<%= getHighlightString(highlights, "Name_t", result.getFieldValue("id").toString(), (String)result.getFieldValue("Name_t")) %>
							</a>
							<% if (isFixedAtTop) { %><span class="label label-important">Top</span><% } %>
							<% if (isHiddened) { %><span class="label label-warning">Hide</span><% } %>
							</H3>
						</td>
						<td width=10%>
						 	<font color=BROWN>
						 	<%= result.getFieldValue("Date_dt") == null ? "N/A" : sdf.format(result.getFieldValue("Date_dt")) %>
						 	</font>
							<% if (!"score".equals(sort) && !"Date_dt".equals(sort) && !"FAQ_i".equals(sort)){ %>
							<div class="cattext" width=15%><%= sortFieldName %>：<%= WiSeUtils.output(result.getFieldValue(sort), "", "", "無")%></div>
							<% } %>
						</td>
						<td width=10%>
                            <%
                            SolrQuery mlt = query.getCopy();
                            mlt.setQuery("id:" + id);
                            %>
                            <A HREF="<%= makeUrl(request, "v.jsp", "id=" + esId + "&" + "d=" + ds.getId() + "&" + mlt.toString()) %>">庫存頁</A>
						</td>
						<td width=15% rowspan=2 align="center">
						<% if (isFixedAtTop) { %>
						<a class="btn btn-success" href="elevator-update.jsp?d=<%= dsId %>&id=<%= esId %>&action=removeFAT&docId=<%= id %>"><i class="icon-star icon-white"></i>取消置頂</a><br/>
						<% } else { %>
						<a class="btn btn-success" href="elevator-update.jsp?d=<%= dsId %>&id=<%= esId %>&action=addFAT&docId=<%= id %>"><i class="icon-star icon-white"></i>置頂</a><br/>
						<% } %>
						<% if (isHiddened) { %>
						<a class="btn btn-danger" href="elevator-update.jsp?d=<%= dsId %>&id=<%= esId %>&action=removeH&docId=<%= id %>"><i class="icon-ban-circle icon-white"></i>取消隱藏</a><br/>
						<% } else { %>
						<a class="btn btn-danger" href="elevator-update.jsp?d=<%= dsId %>&id=<%= esId %>&action=addH&docId=<%= id %>"><i class="icon-ban-circle icon-white"></i>隱藏</a><br/>
						<% } %>
						</td>
					</tr>
					<tr>
						<td colspan="3">
						<p>
						<%
						if (highlights != null
	                        && highlights.get(id) != null
	                        && highlights.get(id).get("Description_mt") != null
	                        ) {
	                            List<String> hlList = highlights.get(id).get("Description_mt");

	                            for (String hl: hlList) {
									%><%= hl %><BR><%
	                            }
	                    }
	                    else if (summary.trim().length() == 0) {
                            %>
                            <FONT COLOR=GRAY><%= translate((String)result.getFieldValue("DataType_s"))%></FONT>
                            <%
	                    }
	                    else {
	                        %>
	                        <%= summary %>
	                        <%
						}
						%>
						</p>
						</td>
					</tr>
					</table>
					</li>
				<%
				}
				%>
		<%--
		SolrServer server = SearchManager.getServer("core-click");
        SolrQuery hotDocQ = new SolrQuery();
        hotDocQ.setQuery("*:*");
        hotDocQ.addFilterQuery("Func_s:" + dsId);
        hotDocQ.addFilterQuery("LogTime:[NOW/MONTH TO NOW/MONTH+1MONTH]");
        hotDocQ.setFacet(true);
        hotDocQ.setFacetMinCount(1);
        hotDocQ.addFacetField("Target_s");
        hotDocQ.addFacetField("Identity_s");

        QueryResponse hotDocRsp = server.query(hotDocQ);
        FacetField hotDocLinkFF = hotDocRsp.getFacetField("Target_s");
        FacetField hotDocTitleFF = hotDocRsp.getFacetField("Identity_s");

        if (false) //hotDocLinkFF.getValues() != null)
        {
			%>
			<div><span class="label"><font color=ORANGE>熱門文章</font></span><BR>
			<%
			Iterator<FacetField.Count> itrTtl = hotDocTitleFF.getValues().iterator();
			for (Iterator<FacetField.Count> iterator2 = hotDocLinkFF.getValues().iterator(); iterator2.hasNext();)
			{
				FacetField.Count facetCount = iterator2.next();
				FacetField.Count facetTtl = itrTtl.next();

				if (facetCount.getCount() == 0)
					continue;
                %>
                <span class="facet">
                 	<a href=""></a>
                           <a HREF="<%= facetCount.getName() %>"
                           	d="<%= dsId %>"
                           	title="<%= StringEscapeUtils.escapeHtml(facetTtl.getName()) %>">
                           	<%= StringUtils.left(facetTtl.getName(), 50) %>
                           </a>
                </span><BR>
                <%
			}
			%><BR><%
        }
     	--%>
     	<div align="center" class="text">
       		<div class="btn-toolbar">
       		<div class="btn-group">
		<%= pagination %>
		</div>
		</div>
    </div>
	</div>
</div>
	</div>
</div>
<div style="clear: both;width: 110;padding: 0">&nbsp;</div>
</body>
</html>
