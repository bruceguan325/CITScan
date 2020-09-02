<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="org.apache.commons.lang.*"
import="org.apache.wink.json4j.*"
import="org.apache.solr.client.solrj.*"
import="org.apache.solr.client.solrj.response.*"
import="org.apache.solr.common.*"
import="java.net.*"
import="java.util.*"
import="com.intumit.solr.robot.entity.*"
import="com.intumit.solr.robot.qarule.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.util.*"
import="com.intumit.solr.synonymKeywords.*"
import="com.intumit.message.MessageUtil"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if ((AdminGroupFacade.getInstance().getFromSession(session).getSystemAdminCURD() & AdminGroup.E2) == 0) {
	String url = request.getRequestURI();
	String qs = request.getQueryString();
	if (StringUtils.isNotBlank(qs)){
		url += "?" + qs;
	}
	%>
	<script>
	window.parent.location='<%= request.getContextPath() %>/wiseadm/login.jsp?r=<%= StringEscapeUtils.escapeJavaScript(URLEncoder.encode(url, "UTf-8")) %>';
	</script>
	<%
	return;
}

com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(session);

if (!t.getEnableScenario()) {
	out.println("Scenario management not enable.");
	return;
}

String name = request.getParameter("name");
JSONObject dialog = name != null ? QADialogRule.getDialogConfig(t, name) : new JSONObject();

int step = Integer.parseInt(request.getParameter("step"));
String dataType = request.getParameter("dataType");
%>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>情境精靈</title>
	<jsp:include page="header-qa.jsp"></jsp:include>
	<link href="<%=request.getContextPath()%>/wiseadm/css/ui-lightness/jquery-ui-1.9.2.custom.min.css" rel="stylesheet">
	<link href="<%=request.getContextPath()%>/styles/bootstrap-toggle.min.css" rel="stylesheet">
	<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>

	<!--// Loading bootstrap again (after jquery-ui) bcz there is a conflicts using button loading state if bootstrap load before jquery-ui -->
	<script src="<%=request.getContextPath()%>/script/jquery-1.12.4.min.js"></script>
	<script src="<%=request.getContextPath()%>/wiseadm/js/jquery-ui-1.9.2.custom.min.js"></script>
	<script src="<%=request.getContextPath()%>/script/bs3/bootstrap.min.js"></script>
	<script src="<%=request.getContextPath()%>/script/bootstrap-toggle.min.js"></script>
	<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
	<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/select2/select2.js"></script>
	<link href="<%=request.getContextPath()%>/assets/stylesheets/plugins/select2/select2.css" rel="stylesheet">
    <script src="js/jsoneditor.min.js"></script>

    <style>
	ul.dragAndDrop {
	    border:3px dotted Gray;
	    padding: 10px;
	    min-height: 500px;
	    display:inline-block;
	    vertical-align:top;
	}
	ul.dragAndDrop li {
	    background-color:Azure;
	    border:1px solid Black;
	    margin: 2px;
	    padding: 2px;
	}
	ul.dragAndDrop li.selected {
	    background-color:GoldenRod
	}

	ul.dragAndDrop .ui-widget input, .ui-widget select, .ui-widget textarea, .ui-widget button {
		font-size: 0.6em;
	}
    </style>
    <script>
    // Set the default CSS theme and icon library globally
    JSONEditor.defaults.theme = 'bootstrap3';
    JSONEditor.defaults.iconlib = 'bootstrap3';
    JSONEditor.plugins.ace.theme = 'textmate'
    </script>
  </head>
  <body>
	<jsp:include page="navbar-qa.jsp"></jsp:include>
    <div class='container'>
    <div class='row'>
      <div class='medium-12 columns'>
        <h1><%= dataType %> 情境精靈（Step<%= step %>）</h1>
      </div>
    </div>
    <div class='row'>
      <div class='medium-12-columns'>
      	<%
      	if (step == 1) {
			SolrServer server = t.getCoreServer();
			Map<String, Set> dataMap = new HashMap<String, Set>();
 			System.out.println("[ScenarioWizard Step" + step + "] Now processing DT[" + dataType + "]..");
			SolrQuery q = new SolrQuery();
			q.setQuery("*:* -dataType_s:(" + QAUtil.DATATYPE_COMMON_SENSE + " " + QAUtil.DATATYPE_CASUAL + ")");
			q.setFilterQueries("dataType_s:" + dataType);
			q.setRows(1000).setStart(0);

			try {
				QueryResponse result = server.query(q);
				SolrDocumentList docs = result.getResults();

				for (SolrDocument doc: docs) {
					Collection<String> fns = doc.getFieldNames();
					String dt = (String)doc.getFirstValue("dataType_s");

					if (dt == null) continue;

					for (String fn: fns) {
						if ("id".equalsIgnoreCase(fn) || "dataType_s".equalsIgnoreCase(fn))
							continue;

						if (StringUtils.endsWithAny(fn, new String[] {"_s", "_ms", "_t", "_mt"})) {
							String cleanedFn = fn.replaceAll("_m?[tsifldp]$", "");
							List<String> allPossibleTerms = QAUtil.getAllPossibleFacetTerms(server, cleanedFn, "dataType_s:" + dataType);

							for (String possibleTerm: allPossibleTerms) {
								Set values = dataMap.get(cleanedFn);

								if (values == null) {
									values = new HashSet();
									dataMap.put(cleanedFn, values);
								}

								values.addAll(allPossibleTerms);
							}
						}
					}
				}
			}
			catch (SolrServerException e) {
				e.printStackTrace();
			}

			session.setAttribute("ScenarioWizard-DataMap", dataMap);
			System.out.println("[ScenarioWizard Step" + step + "] DT[" + dataType + "]... Finished");

	      	%>
	      	配置實體及類型
	      	<form method="post" action="qaScenarioWizard.jsp">
	      	<input name="step" type="hidden" value="2">
	      	<input name="dataType" type="hidden" value="<%= dataType %>">
	      	<table class="table table-striped table-bordered">
	      	<thead>
	      		<tr>
	      			<th>CODE</th>
	      			<th>NAME</th>
	      			<th>TYPE</th>
	      			<th>VALUES</th>
	      		</tr>
	      	</thead>
			<%
			for (String key: dataMap.keySet()) {
				Set values = dataMap.get(key);

				String entityType = "string";
				boolean isNumber = true;
				boolean isDate = true;
				boolean isLocation = true;

				for (Object val: values) {
					if (val instanceof Date) {
						isDate = true;
						isNumber = false;
						isLocation = false;
						break;
					}
					if (val instanceof Number) {
						isDate = false;
						isNumber = true;
						isLocation = false;
						break;
					}
					if (val instanceof String) {
						String strVal = (String)val;

						if (isNumber && !StringUtils.isNumeric(strVal)) {
							System.out.println("isNotNumber[" + val + "]");
							isNumber = false;
						}
						if (isDate && !strVal.matches("^[\\d]{4}[/\\-][\\d]{2}[/\\-][\\d]{2}")) {
							isDate = false;
						}
						if (isLocation) {
							isLocation = false;
						}
					}
				}

				if (isNumber) {
					entityType = "number";
				}
				if (isDate) {
					entityType = "datetime";
				}
				if (isLocation) {
					entityType = "location";
				}
				%>
				<tr>
					<td class="col-md-1"><%= key %></td>
					<td class="col-md-2"><input type="text" name="<%= key %>-name"></td>
					<td class="col-md-1">
						<select name="<%= key %>-entityType">
						<% for (QAEntityType et: QAEntityType.values()) { %>
							<option value="<%= et.name().toLowerCase() %>"
								<%= entityType.equalsIgnoreCase(et.name()) ? "selected" : "" %>><%= et.name() %></option>
						<% } %>
						</select>
					</td>
					<td class="col-md-10">
						<%= StringUtils.join(values, "&nbsp;/&nbsp;") %>
					</td>
				</tr>
			<% } %>
	      	</table>
	        <a id='prev' href="qaScenarioWizard.jsp?step=2&dataType=" class='btn btn-danger'>上一步</a>
	        <button id='next' type="submit" class='btn btn-warning'>下一步</button>
	        </form>
	      	<%
      	}
      	else if (step == 2) {
	      	%>
	      	確認欄位值
	      	<form method="post" action="qaScenarioWizard.jsp">
	      	<input name="step" type="hidden" value="3">
	      	<input name="dataType" type="hidden" value="<%= dataType %>">
	      	<table class="table table-striped table-bordered">
	      	<thead>
	      		<tr>
	      			<th>CODE</th>
	      			<th>NAME</th>
	      			<th>TYPE</th>
	      			<th>VALUES</th>
	      		</tr>
	      	</thead>
			<%
			Map<String, Set> dataMap = (Map<String, Set>)session.getAttribute("ScenarioWizard-DataMap");
			Map<String, String> entityNameMap = new HashMap<String, String>();
			Map<String, QAEntityType> entityTypeMap = new HashMap<String, QAEntityType>();

			for (String key: dataMap.keySet()) {
				QAEntityType entityType = QAEntityType.valueOf(StringUtils.upperCase(request.getParameter(key + "-entityType")));
				String entityName = request.getParameter(key + "-name");

				entityNameMap.put(key, entityName);
				entityTypeMap.put(key, entityType);

				Set values = dataMap.get(key);
				%>
				<tr>
					<td class="col-md-1"><%= key %></td>
					<td class="col-md-2"><%= entityName %></td>
					<td class="col-md-1"><%= entityType %></td>
					<td class="col-md-10">
						<% if (entityType == QAEntityType.STRING) { %>
						<input type="text" style="width: 100%" name="<%= key %>-values" class=" col-md-12 values-select2" value="<%= StringUtils.join(values, "|") %>">
						<% } else if (entityType == QAEntityType.REGEXP) { %>
						<textarea style="width: 100%" name="<%= key %>-values" class=" col-md-12" placeholder="<%= StringUtils.join(values, " / ") %>"></textarea>
						<% } else { %>
						<%= StringUtils.join(values, ",") %>
						<% } %>
					</td>
				</tr>
			<% } %>
	      	</table>
	        <button id='prev' class='btn btn-danger'>上一步</button>
	        <button id='next' type="submit" class='btn btn-warning'>下一步</button>
	        </form>
	      	<%
      		session.setAttribute("ScenarioWizard-NameMap", entityNameMap);
  			session.setAttribute("ScenarioWizard-EntityTypeMap", entityTypeMap);
      	}
      	else if (step == 3) {
	      	%>
	      	設定同義詞（僅列出字串型態的所有關鍵字）
	      	<form method="post" action="qaScenarioWizard.jsp">
	      	<input name="step" type="hidden" value="4">
	      	<input name="dataType" type="hidden" value="<%= dataType %>">
	      	<table class="table table-striped table-bordered">
	      	<thead>
	      		<tr>
	      			<th>KEYWORD</th>
	      			<th>同義詞</th>
	      		</tr>
	      	</thead>
			<%
			Map<String, Set> dataMap = (Map<String, Set>)session.getAttribute("ScenarioWizard-DataMap");
			Map<String, String> entityNameMap = (Map<String, String>)session.getAttribute("ScenarioWizard-NameMap");
			Map<String, QAEntityType> entityTypeMap = (Map<String, QAEntityType>)session.getAttribute("ScenarioWizard-EntityTypeMap");
			Set<String> allKeywords = new HashSet<String>();

			for (String key: dataMap.keySet()) {
				QAEntityType et = entityTypeMap.get(key);

				if (et == QAEntityType.STRING) {
					allKeywords.addAll(Arrays.asList(StringUtils.split(request.getParameter(key + "-values"), "|")));
					Set values = new HashSet();
					values.addAll(Arrays.asList(StringUtils.split(request.getParameter(key + "-values"), "|")));
					dataMap.put(key, values);
				}
				else if (et == QAEntityType.REGEXP) {
					Set values = new HashSet();
					values.addAll(Arrays.asList(StringUtils.split(request.getParameter(key + "-values"), "\n")));
					dataMap.put(key, values);
				}
			}

			for (String kw: allKeywords) {
    			SynonymKeyword sk = SynonymKeywordFacade.getInstance().get(t.getId(), kw);
				String key = WiSeUtils.sha256(kw);
				String syns = "";
				if (sk != null) {
					syns = StringUtils.join(sk.getSynonymList(), "|");
				}
				%>
				<tr>
					<td class="col-md-1"><%= kw %></td>
					<td class="col-md-10">
						<input type="text" style="width: 100%" name="<%= key %>-synonyms" class=" col-md-12 synonyms-select2" value="<%= syns %>">
					</td>
				</tr>
			<% } %>
	      	</table>
	        <button id='prev' class='btn btn-danger'>上一步</button>
	        <button id='next' type="submit" class='btn btn-warning'>儲存所有實體設定</button>
	        </form>
	      	<%
	      		session.setAttribute("ScenarioWizard-AllKeywords", allKeywords);
	    	}
	    	else if (step == 4) {
	  		Map<String, Set> dataMap = (Map<String, Set>)session.getAttribute("ScenarioWizard-DataMap");
	  		Map<String, String> entityNameMap = (Map<String, String>)session.getAttribute("ScenarioWizard-NameMap");
	  		Map<String, QAEntityType> entityTypeMap = (Map<String, QAEntityType>)session.getAttribute("ScenarioWizard-EntityTypeMap");
	  		Set<String> allKeywords = (Set<String>)session.getAttribute("ScenarioWizard-AllKeywords");
	
	  		int[] synStatus = new int[] {0, 0, 0}; // add, update, delete
	  		int[] entityStatus = new int[] {0, 0}; // add, update
	
	  		for (String kw: allKeywords) {
	  			SynonymKeyword sk = SynonymKeywordFacade.getInstance().get(t.getId(), kw);
	  			String key = WiSeUtils.sha256(kw);
	  			String syn[] = StringUtils.split(request.getParameter(key + "-synonyms"), "|");
	
	  			if (sk == null) {
	  				if (syn != null && syn.length > 0) {
	   				try {
	   					sk = SynonymKeywordFacade.getInstance().save(t.getId(), kw, StringUtils.defaultString(StringUtils.join(syn, ","), ""), true, null);
		   				synStatus[0]++;
		   			} catch (Exception e) {
  						System.out.println(e);
  					}
	   			}
	  			}
	  			else {
	  				if (syn != null && syn.length > 0) {
	   				SynonymKeywordFacade.getInstance().update(Long.parseLong("" + t.getId()), kw, StringUtils.defaultString(StringUtils.join(syn, ","), ""), true, null);
	   				synStatus[1]++;
	   			}
	  				else {
	  					SynonymKeywordFacade.getInstance().delete(t.getId(), Long.parseLong("" + sk.getId()));
	  					synStatus[2]++;
	  				}
	  			}
	  		}
	
	  		for (String key: dataMap.keySet()) {
	   		QAEntity e = QAEntity.get(t.getId(), key, dataType, entityNameMap.get(key));
	
	   		if (e == null) {
	   			e = new QAEntity();
	   			entityStatus[0]++;
	   		}
	   		else {
	   			entityStatus[1]++;
	   		}
	   		e.setCategory(dataType);
	   		e.setTenantId(t.getId());
	   		e.setCode(key);
	   		e.setName(entityNameMap.get(key));
	   		e.setEntityType(entityTypeMap.get(key));
	   		e.setEntityValues(StringUtils.join(dataMap.get(key), "|"));
	   		e.setEnabled(true);
	
	   		QAEntity.saveOrUpdate(e);
  		}
      	%>
			<li>過濾條件是指使用者可以透過問句過濾資料，例如「<span class='text-warning'>我想查<span class='text-danger'>月繳</span>的保單</span>」，系統會從 Filters 當中判斷使否有設定該欄位，進而將指定欄位值為「<span class='text-danger'>月繳</span>」的資料篩選出來</li>
			<li>欄位是指使用者可以透過問句查詢指定欄位的資料，例如「<span class='text-warning'>我想查保單的<span class='text-danger'>繳款方式</span></span>」，系統會從 Fields 當中判斷哪個欄位是「<span class='text-danger'>繳款方式</span>」，進而將指定欄位資料顯示給使用者</li>
			<li>欄位後方的 checkbox 是代表預設欄位，當沒有指定要顯示的欄位時，系統會使用預設欄位來顯示（順序按照拖曳排序的順序）</li>
			</ul>
			</div>
	      	<form method="post" action="qaScenarioWizard.jsp">
	      	<input name="step" type="hidden" value="5">
	      	<input name="dataType" type="hidden" value="<%= dataType %>">
	      	<textarea id="fieldAndFilterConfig" name="fieldAndFilterConfig" style="display:none;"></textarea>
	      	<h2>情境類型</h2>
	      	<select name="scenarioType" id="scenarioType" >
	      		<option value="com.intumit.solr.robot.qadialog.DataSearchDialog">查詢型</option>
	      		<option value="com.intumit.solr.robot.qadialog.FillFormQADialog">填表型</option>
	      	</select>
			<div class="row">
				<div class="col-md-8" >
					<div class="col-md-12" >
					<H2>Filters（過濾條件）</H2>
					</div>
					<div class="col-md-6" >
					全部
					<ul class="list-unstyled dragAndDrop pull-right col-md-12 pair1 pair-unselect">
					<% for (String key: dataMap.keySet()) { %>
						<li data-key='<%= key %>' data-name='<%= entityNameMap.get(key) %>' data-type='<%= entityTypeMap.get(key).name().toLowerCase() %>'><%= key %> (<%= entityNameMap.get(key) %>)</li>
					<% } %>
					</ul>
					</div>
					<div class="col-md-6">
					啟用
					<ul class="list-unstyled dragAndDrop col-md-12 pair1 pair-selected">
					</ul>
					</div>
				</div>
			</div>
			<div class="row">
				<div class="col-md-8" >
					<div class="col-md-12" >
					<H2>Fields（欄位）</H2>
					</div>
					<div class="col-md-6" >
					全部
					<ul class="list-unstyled dragAndDrop pull-right col-md-12 pair2 pair-unselect">
					<% for (String key: dataMap.keySet()) { %>
						<li data-key='<%= key %>' data-name='<%= entityNameMap.get(key) %>' data-type='<%= entityTypeMap.get(key).name().toLowerCase() %>'><%= key %> (<%= entityNameMap.get(key) %>)
							<input type="checkbox" name="<%= key %>-default-field" class="pull-right">
						</li>
					<% } %>
					</ul>
					</div>
					<div class="col-md-6">
					啟用
					<ul class="list-unstyled dragAndDrop col-md-12 pair2 pair-selected">
					</ul>
					</div>
				</div>
			</div>
	        <button id='prev' class='btn btn-danger'>上一步</button>
	        <button id='next' class='btn btn-warning btnStep4Submit'>下一步</button>
	        </form>
      	<%
      	}
      	else if (step == 5) {
    		Map<String, Set> dataMap = (Map<String, Set>)session.getAttribute("ScenarioWizard-DataMap");
    		Map<String, String> entityNameMap = (Map<String, String>)session.getAttribute("ScenarioWizard-NameMap");
    		Map<String, QAEntityType> entityTypeMap = (Map<String, QAEntityType>)session.getAttribute("ScenarioWizard-EntityTypeMap");
    		Set<String> allKeywords = (Set<String>)session.getAttribute("ScenarioWizard-AllKeywords");
    		String fieldAndFilterConfig = request.getParameter("fieldAndFilterConfig");
    		JSONObject cfg = new JSONObject(fieldAndFilterConfig);

    		cfg.put("name", dataType);
    		cfg.put("description", dataType);
    		cfg.put("class", request.getParameter("scenarioType"));

    		session.setAttribute("ScenarioWizard-Config", cfg);
	      	%>
	      	<form>
			<div class="row">
				<div class="col-md-8" >
					<div class="col-md-12" >
					<H2>快完成了！</H2>

			      	<div class="alert alert-info">
					<ul>
					<li>基本配置已經完成，但注意尚未存檔</li>
					<li>點選下方「<span class='text-danger'>確認細部設定</span>」調整細部設定</li>
					<li>存檔按鈕於下一步當中，別忘了點選<span class='text-warning'>存檔</span>」，系統才會把本次情境的配置儲存起來喔</li>
					</ul>
					</div>
	      			<textarea rows=20 cols=100 style="display: none;"><%= cfg.toString(2) %></textarea>
					</div>
	      		</div>
	      	</div>
	      	<br>
	        <button id='prev' class='btn btn-danger'>上一步</button>
	        <a href='qaScenarioEditor.jsp' class='btn btn-warning' target="_dialog">確認細部設定</a>
	      	</form>
      	<%
      	}
      	%>
    </div>
    </div>
    </div>
    <script>
    var pairs = ['pair1', 'pair2'];
    var pairNames = ['filters', 'fields'];

    $(".values-select2").select2({
    	  tags: true,
    	  separator: "|",
    	  tokenSeparators: ['|']
    });
    $(".synonyms-select2").select2({
  	  tags: true,
  	  separator: "|",
  	  tokenSeparators: ['|']
    });
    $(".btnStep4Submit").click(function() {
    	var scenarioType = $('#scenarioType').val() == 'com.intumit.solr.robot.qadialog.DataSearchDialog' ? 1 : 2;
//    	var scenarioName = $('#scenarioName').val();
    	var config;

    	if (scenarioType == 1) {
    		config = {
	    		quitText: "請問還有其他需要為您服務的地方嗎?",
	    		enterTrigger: [
					{type: "rule",
					 contents: [],
					 rules: [
					       {match: [
					           { op: "AND",
					        	 intents: ["SEARCH"],
					        	 entities: [".*"],
					        	 restrictToEntityCategory: ["<%= dataType %>"],
					        	 "class": "com.intumit.solr.robot.qadialog.LUISCriteria"}
					        ],
					        class: "com.intumit.solr.robot.qadialog.LUISRule"
					       }
					 ]
					}
	    		],
				quitTrigger: [
	                {type: "status",
	                 contents: ["FINISHED"],
	                 showCurrentStatus: true,
	                 doubleConfirm: false
	                },
	                {type: "sentence",
	                 contents: ["放棄", "結束", "離開", "跳開", "再見", "掰掰", "停止", "quit"],
	                 showCurrentStatus: true,
	                 doubleConfirm: false
	                }
				]
	    	};
    	}
    	else if (scenarioType == 2) {
    		config = {
    	    		quitText: "請問還有其他需要為您服務的地方嗎?",
    	    		enterTrigger: [
    					{type: "rule",
    					 contents: [],
    					 rules: [
							{
							    "match": [
							       "(?ism)身份資料"
							    ],
							    "class": "com.intumit.solr.robot.qadialog.RegexRule",
							    "partial": true
							}
    					 ]
    					}
    	    		],
    				quitTrigger: [
    	                {type: "status",
    	                 contents: ["FINISHED"],
    	                 showCurrentStatus: true,
    	                 doubleConfirm: false
    	                },
    	                {type: "sentence",
    	                 contents: ["放棄", "結束", "離開", "跳開", "再見", "掰掰", "停止", "quit"],
    	                 showCurrentStatus: true,
    	                 doubleConfirm: false
    	                }
    				]
    	    	};
        }

    	for (var i in pairs) {
    		p = pairs[i];
    		pn = pairNames[i];

    		config[pn] = [];
    		$('ul.pair-selected.' + p + ' > li').each(function() {
    			checked = $(this).find('input:checked').length > 0;
    			defaultQuestion = "請問您要查詢的" + $(this).attr('data-name');

    			if (scenarioType == 2) {
	    			defaultQuestion = "請問您的" + $(this).attr('data-name');
    			}

    			rule = {
    					defaultQuestion: defaultQuestion,
    					require: 0,
    					multivalue: false,
    					doubleConfirm: false,
    					rules: []
    			};
    			rule['name'] = $(this).attr('data-key');
    			rule['targetIndexFieldName'] = $(this).attr('data-key') + "_s";
    			rule['showName'] = $(this).attr('data-name');
    			rule['type'] = $(this).attr('data-type');
    			rule['default'] = checked;
    			rule['entities'] = [$(this).attr('data-key')];
    			rule['outputExpr'] = "bundle.get('fieldValue')==null?'查無'+bundle.field.showName:bundle.field.showName+'為' + bundle.get('fieldValue')";
    			rule['simpleOutputExpr'] = "bundle.get('fieldValue')==null?'無':bundle.get('fieldValue')";
    			config[pn].push(rule);
    		});
    	}

    	$('#fieldAndFilterConfig').text(JSON.stringify(config));
    	$('form').submit();
    });

    for (var p in pairs) {
    	$("ul." + pairs[p]).on('click', 'li', function (e) {
    	    if (e.ctrlKey || e.metaKey) {
    	        $(this).toggleClass("selected");
    	    } else {
    	        $(this).addClass("selected").siblings().removeClass('selected');
    	    }
    	}).sortable({
    	    connectWith: "ul." + pairs[p],
    	    delay: 150, //Needed to prevent accidental drag when trying to select
    	    revert: 0,
    	    helper: function (e, item) {
    	        var helper = $('<li/>');
    	        if (!item.hasClass('selected')) {
    	            item.addClass('selected').siblings().removeClass('selected');
    	        }
    	        var elements = item.parent().children('.selected').clone();
    	        item.data('multidrag', elements).siblings('.selected').remove();
    	        return helper.append(elements);
    	    },
    	    stop: function (e, info) {
    	        info.item.after(info.item.data('multidrag')).remove();
    	        $('.pair-unselect li span').hide();
    	        $('.pair-selected li span').show();
    	    }

    	});
    }
    </script>
  </body>
</html>
