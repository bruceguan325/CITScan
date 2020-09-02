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
         import="com.intumit.message.MessageUtil"
         import="com.intumit.solr.*" 
         import="com.intumit.solr.util.*" 
         import="com.intumit.solr.dataset.*"
         import="com.intumit.solr.SearchManager"
         import="com.intumit.solr.searchKeywords.*"
         import="com.intumit.solr.config.ColumnNameMappingFacade"
         import="com.intumit.solr.config.ColumnNameMapping"
		 contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" 
%><%!
public static String forwardJSP = "s.jsp";

public String makeUrl(HttpServletRequest req, String jsp, String query) {
	String url = req.getContextPath() + "/wiseadm/analytics/" + jsp + (query == null ? "" : ("?" + (query)));
	//System.out.println("URL[" + url + "]");
	return url;
}

%>
    <script type="text/javascript" src="<%=request.getContextPath() %>/script/common.js"></script>
    <script type="text/javascript" src="<%=request.getContextPath() %>/script/json/json2.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath() %>/script/jquery.dateFormat-1.0.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath() %>/script/palette.js"></script>
    <!-- Include FoamTree visualization code -->
    <script src="<%=request.getContextPath() %>/script/carrotsearch.foamtree.js"></script>
    <!-- Include Hammer.js for mobile interactions -->
    <script src="<%=request.getContextPath() %>/script/hammer.min.js"></script>
    <link rel="stylesheet" type="text/css" href="<%=request.getContextPath() %>/styles/hitft/layout.css"></link>
<%@ include file="translate-map.jsp" %>
<%
SolrQuery query = (SolrQuery) pageContext.findAttribute("q");
String dsId = (String) pageContext.findAttribute("d");
String selectedTarget = (String) pageContext.findAttribute("selectedTarget");
String selectedView = (String) pageContext.findAttribute("selectedView");
AnalyticsUtil au = (AnalyticsUtil) pageContext.findAttribute("analyticsUtil");

try {
	Locale locale = com.intumit.hithot.HitHotLocale.determineLocale(request, false, false);
	Calendar today = Calendar.getInstance();
	today.set(Calendar.HOUR_OF_DAY, 0);
	today.set(Calendar.MINUTE, 0);
	today.set(Calendar.SECOND, 0);
	
	String qtime = pageContext.findAttribute("QTime").toString();
	DataSet ds = DataSetFacade.getInstance().get(new Integer(dsId));
	Map<String,Map<String,List<String>>> highlights = (Map<String,Map<String,List<String>>>) pageContext.findAttribute("highlights");
	SolrDocumentList docs = (SolrDocumentList) pageContext.findAttribute("results");
	HashSet<String> blacks = com.intumit.solr.blackKeywords.BlackKeywordFacade.getInstance().loadBlackList();
    int start = query.getStart();
	String fqs = "";
	if (query.getFilterQueries() != null)
	for (String fq: query.getFilterQueries()) {
		try {
			fqs += "&fq=" + URLEncoder.encode(fq, "UTF-8");
		}
		catch (Exception ignored) {}
	}
    
    //For simplification, we are not using the JSTL
    SolrDocumentList docList = (SolrDocumentList) pageContext.findAttribute("results");
	SolrQuery sortByQuery = query.getCopy();
	
	String[] sortables = StringUtils.trimToEmpty(ds.getSortables()).split(",");

 	StringBuilder paginationInfo = new StringBuilder();
  	paginationInfo.append(MessageUtil.getMessage(locale, "search.number.found2", new String[] {"" + docs.getNumFound()}));
	StringBuilder pagination = new StringBuilder();
   		
	int currPage = (int)Math.floor(start / query.getRows())+1;
	if (currPage > 1) {
		SolrQuery nextPageQuery = query.getCopy();
		nextPageQuery.setStart(0);
		
		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP,  "v=" + selectedView + "&" + nextPageQuery.toString()));
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
		pagination.append(makeUrl(request, forwardJSP, "v=" + selectedView + "&" + toPageQuery.toString()));
		pagination.append("\">");
		pagination.append((currPage+po));
		pagination.append("</A>");
	}
	int lastPage = (int)Math.floor((docs.getNumFound()-1) / query.getRows()) + 1;
	if (lastPage != currPage) {
		SolrQuery nextPageQuery = query.getCopy();
		nextPageQuery.setStart((lastPage-1)*query.getRows());
		
		pagination.append("&nbsp;<A class=\"btn\" HREF=\"");
		pagination.append(makeUrl(request, forwardJSP, "v=" + selectedView + "&" + nextPageQuery.toString()));
		pagination.append("\"><i class=\"icon-forward\"></i>&nbsp;</A>");
	}
    %>
    <div align="left" class="text hidden-phone hide">
   		<div class="row" style="margin-left: 30px;">
			<div class="col-md-12" align="right" >
			<%= MessageUtil.getMessage(locale, "search.spend.second", new String[] {"" + qtime}) %>
			<%= docs.getNumFound() > 0 ? "，" + paginationInfo : "" %>
			</div>
		</div>
    </div>
<!-- HitFT Area -->
    <div class='row-fluid' id='content-wrapper'>
		<div class='row'>
        <div class='col-md-12'>
            <div class='col-md-12'>
            
           		<bean:message key="analytics.type"/>
			    <select id="selectedView" name="selectedView" class="form-control optionPanel input-medium">
			    	<% for (String at: au.getAnalyticTypes()) { %>
				    	<option value="<%= at %>"><%= com.intumit.message.MessageUtil.getMessage(locale, (String)au.getAnalyticTypeConfig(at).get("resourceKey")) %></option>
			    	<% } %>
			    </select>
			    
		    		<span class="function-separator"></span>
			    
        			<bean:message key="analytics.fn"/>
			    <select id="dataField" name="dataField" class="form-control optionPanel input-medium">
			    	<% for (String param: (List<String>)au.getAnalyticTypeConfig(selectedView).get("field1")) { %>
			    	<option value="<%= param %>" ><%= translate(locale, au, null, au.getParamNameResourceKey(param), false) %></option>
			    	<% } %>
			    </select>
            		
			    <select id="dataNum" name="dataNum" class="form-control optionPanel input-medium">
				    	<option value="10"><bean:message key="global.number" arg0="10"/></option>
				    	<option value="20"><bean:message key="global.number" arg0="20"/></option>
				    	<option value="30"><bean:message key="global.number" arg0="30"/></option>
				    	<option value="50" selected><bean:message key="global.number" arg0="50"/></option>
				    	<option value="1000"><bean:message key="global.number.all"/></option>
			    </select>
			    
		    		<span class="function-separator"></span>
		    	
        			<bean:message key="search.dataField.filterBlock"/>：
			    <select id="groupField" name="groupBy" class="form-control optionPanel input-medium">
			    	<option value="none" selected>None</option>
			    	<% for (String param: (List<String>)au.getAnalyticTypeConfig(selectedView).get("groupBy")) { %>
			    	<option value="<%= param %>" ><%= com.intumit.message.MessageUtil.getMessage(locale, au.getParamNameResourceKey(param)) %></option>
			    	<% } %>
			    </select>
			    
		    		<span class="function-separator"></span>
		    	
        			<bean:message key="search.dataField.showType"/>：
			    <select id="dataSmoothing" name="dataSmoothing" class="form-control optionPanel input-medium">
			    	<option value="equalDepth" selected><bean:message key='search.dataField.smoothing.equalDepth'/></option>
			    	<option value="sqrt"><bean:message key='search.dataField.smoothing.sqrt'/></option>
			    	<option value="power"><bean:message key='search.dataField.smoothing.power'/></option>
			    	<option value="nothing"><bean:message key='search.dataField.smoothing.nothing'/></option>
			    </select>
			    
		    		<span class="function-separator"></span>
			    	
				<button id='opRefresh' class='btn btn-primary hide'><bean:message key="global.refresh"/></button>
				
			    <script>
			    $('#selectedView').val("<%= selectedView %>");
			    $('#dataField').val("<%= au.getAnalyticTypeConfig(selectedView).get("field1Selected") %>");
			    
			    $('#selectedView').on("change", function() { 
			    	turl = '<%= makeUrl(request, forwardJSP, "st=" + selectedTarget + "&" + query.toString()) %>&v=' + $('#selectedView').val();
			    	window.location = turl;
			    });
			    </script>
			</div>
		</div>
        </div>
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
     	<% } else { %>
    		<div class='row' style="margin-top: 15px">
            	<div class='col-md-12'>
				    <!-- Some basic HTML scaffolding, optional. -->
				    <div id="main">
				      <div id="ftcontainer" style="background: rgba(0,0,0, 0.8);">
				              
				      </div>
				    </div>
	            </div>
	        </div>
    		<div class='row'>
            	<div class='col-md-12'>
				    <p id="copyright" style="margin-left: 30px; display: none;"><font color="red"></font><span>All rights reserved.|© 1999-2017 </span><a href="http://www.intumit.com/">Intumit Inc.</a></p>
	            </div>
			</div>
      	<% } %>
</div>

<script type="text/javascript">//<![CDATA[
	var lang_str="<%=com.intumit.hithot.HitHotLocale.zh_TW.name()%>";
	var temp_lang_str_href="tw";
	var keyword_str='<%=query%>';
	var page=8;
	var num=8;
	var tab='News';
	var newHeight = getPageHeight();
	var colors = palette('tol-rainbow', 50).reverse();
	
	console.log(colors);

	$('#main').height(Math.max(newHeight - 210, 600));//Math.max(newHeight - 70, newHeight - $('#main').position().top - 70));
	
	var myScroll;
	function loaded() {
	}
	
	// IE don't have addEventListener, use attachEvent
	if (document.addEventListener) {
		document.addEventListener('touchmove', function (e) { e.preventDefault(); }, false);
		document.addEventListener('DOMContentLoaded', loaded, false);	
	}
	else {
		document.attachEvent('touchmove', function (e) { e.preventDefault(); });
		document.attachEvent('DOMContentLoaded', loaded);
	}
	
	
	var baseDataUrl = "<%= request.getContextPath() %>/wiseadm/analytics/s-hitft-ajax.jsp?<%= query.toString() %>&st=<%= selectedTarget %>&v=<%= selectedView %>"
			+ "&ts=<%= System.currentTimeMillis() %>";
			
	var foamtree = null;

	function drawChart() {
	  	var dataField = $('#dataField :selected').val();
	  	var groupField = $('#groupField :selected').val();
	  	var dataNum = $('#dataNum :selected').val();
	  	var dataSmoothing = $('#dataSmoothing :selected').val();
	  	var dataUrl = baseDataUrl + "&deep=true&df=" + dataField + "&num=" + dataNum + "&dsm=" + dataSmoothing + "&groupBy=" + groupField + '&st=<%= selectedTarget %>&v=<%= selectedView %>';
	    
	    $.ajax({
	    	url: dataUrl,
	    	dataType: "json",
	    	success: function(dataObject) {
	    		if ("none" == groupField) {
	    			foamtree.set("layout", "relaxed");
	    			foamtree.set("relaxationInitializer", "random");
	    		}
	    		else {
	    			foamtree.set("layout", "squarified");
	    			foamtree.set("relaxationInitializer", "random");
	    		}
		    	foamtree.set("dataObject", dataObject);
	    	}
	    });
	}

	$('#opRefresh').click(function() {
  		drawChart();
	});
	
	window.addEventListener("load", function () {
      // Initialize FoamTree 3.0.x with settings that emulate FoamTree 2.0.x
      // as closely as possible. Feel free to tweak or disable the customizations
      // to use some of the new features introduced in version 3.0.x.
        foamtree = new CarrotSearchFoamTree({
        id: "ftcontainer",
        pixelRatio: window.devicePixelRatio || 1,

        // The "ordered" initializer produces layouts known from FoamTree 2.0.x defaults.
        //relaxationInitializer: "random",
        //layout: "squarified",
        stacking: "flattened",
        descriptionGroupMinHeight: 80,

        // Decrease the amount of relaxation to better preserve the left-to-right order
        // of rainbow colors. For the default relaxationQualityThreshold, the colors may mix up.
        relaxationQualityThreshold: 3,

        // The following decorator assigns rainbow colors in the order of group's weights
        groupColorDecorator: function (opts, params, vars) {
          if (params.level === 0) {
//        	vars.groupColor = "#" + (function(i) {return colors[i];})(params.indexByWeight);
            vars.groupColor.h = params.indexByWeight / params.siblingCount * 300;
            vars.groupColor.a = 0.98;
          }
        },

        // An alternative to the the above decorator may be linear color distribution
        // enabled by the following options. To use this alternative, comment out
        // the groupColorDecorator above.
        // rainbowColorDistribution: "linear",
        // rainbowColorDistributionAngle: 10,
        // rainbowEndColor: "hsla(300, 100%, 55%, 1)",

        // Disable saturation and lightness corrections introduced in FoamTree 3.0.x.
        //rainbowSaturationCorrection: 1,
        //rainbowLightnessCorrection: 0,

        // Make the subgroup color variations more subtle, like in FoamTree 1.0.x.
        //rainbowLightnessShift: 40,

        // FoamTree 1.0.x uses DejaVu Sans to display labels. Verdana is a close match.
        groupLabelFontFamily: "Verdana, sans-serif",
        groupLabelLightColor: "rgba(255, 255, 255, 0.9)",
        groupLabelDarkColor: "rgba(0, 0, 0, 0.9)",

        // FoamTree 1.0.x did not support group polygon strokes so we disable them.
        //groupStrokeType: "none",
        //groupFillType: "plain",

        // Use FoamTree 1.0.x defaults for polygon border sizes.
        /*
        // 立體感
        groupBorderWidth: 5,
        groupInsetWidth: 3,
        groupBorderRadius: 0.2,
        groupMinDiameter: 50,
        groupStrokeType: "gradient",
        groupStrokeWidth: 5,
        */
        
        // 抽象感
        groupBorderWidth: 3,
        groupBorderRadius: 0,
        groupStrokeWidth: 2,
		groupFillType: "plain",
		groupStrokePlainLightnessShift: -20,
		rainbowStartColor: "hsla(0, 100%, 60%, 1)",
		rainbowEndColor: "hsla(360, 100%, 60%, 1)",

        // Use FoamTree 1.0.x defaults for the parent group opacity.
        parentFillOpacity: 0.9,

        // Draw group selection outlines in white.
        groupSelectionOutlineColor: "rgba(255, 255, 255, 0.9)",

        // Draw selection outline in white, disable selection outline shadow
        groupSelectionOutlineWidth: 6,
        groupSelectionOutlineShadowSize: 0,
        groupSelectionOutlineShadowColor: "#000",

        // Make relaxation visible, just like in FoamTree 1.0.x.
        relaxationVisible: true,

        // Always draw labels during animation
        wireframeLabelDrawing: "always",

        // On double click, open the group for browsing instead of exposing it.
        onGroupDoubleClick: function (event) {
          // Prevent the default behavior (expose)
          event.preventDefault();

          // Open the group instead
          this.open(event.group);
        },

        // FoamTree 1.0.x opens the group on Shift+click and closes on Ctrl+Shift+click.
        onGroupClick: function(event) {
          if (event.shiftKey) {
            // Don't select group in this case
            event.preventDefault();

            // Open or close depending on the Ctrl key
            this.open({
              groups: event.ctrlKey ? event.bottommostOpenGroup : event.group,
              open: !event.ctrlKey
            });
          }
          else {
				var addPart = "";
				var q = $('#searchText').val();
				if (q != '') {
					addPart = "&q=" + encodeURIComponent(q);
				}
				addPart += getUrlQueryPart( event.group.fq );
        		document.location='<%= request.getContextPath() %>/wiseadm/analytics/s.jsp?v=s<%= fqs %>' + addPart;
          }
        },

        attributionTheme: "light"
      });
      
      drawChart();
        
      // Resize FoamTree on orientation change
      window.addEventListener("orientationchange", foamtree.resize);

      // Resize on window size changes
      window.addEventListener("resize", (function () {
        var timeout;
        return function () {
          window.clearTimeout(timeout);
          timeout = window.setTimeout(foamtree.resize, 300);
        }
      })());
    });
	
	// Adapted from getPageSize() by quirksmode.com
	function getPageHeight() {
	    var windowHeight
	    if (self.innerHeight) { // all except Explorer
	      windowHeight = self.innerHeight;
	    } else if (document.documentElement && document.documentElement.clientHeight) {
	      windowHeight = document.documentElement.clientHeight;
	    } else if (document.body) { // other Explorers
	      windowHeight = document.body.clientHeight;
	    }
	    return windowHeight;
	}

	$('#dataField,#groupField,#dataNum,#dataSmoothing').on('change', function() {
		drawChart();
	});

</script>
<%
}
catch (Exception e) {
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

