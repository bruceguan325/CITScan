<%@ include file="/commons/taglib.jsp"%>
<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java"
import="org.apache.commons.lang.*"
import="org.apache.wink.json4j.*"
import="java.net.*"
import="java.util.*"
import="com.intumit.solr.robot.*"
import="com.intumit.solr.robot.qarule.*"
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

	JSONObject dialog = null;
	String id = request.getParameter("id");
	if (id != null) {
		QADialogConfig dlg = QADialogConfig.get(Integer.parseInt(id));
		if (dlg.getTenantId() != t.getId())
			return;
		
		dialog = dlg.getDialogConfigObject();
	}
	else {
		JSONObject tmp = (JSONObject)session.getAttribute("ScenarioWizard-Config");
		
		if (tmp != null) {
			dialog = tmp;
		}
		else {
			dialog = new JSONObject();
		}
	}
	%>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8" />
    <title>情境設定</title>
	<jsp:include page="header-qa.jsp"></jsp:include>
	<script src="<%=request.getContextPath()%>/assets/javascripts/plugins/common/moment.min.js"></script>
	
	<!--// Loading bootstrap again (after jquery-ui) bcz there is a conflicts using button loading state if bootstrap load before jquery-ui -->
	<script src='<%= request.getContextPath() %>/script/typed.js' type='text/javascript'></script>
	<script src='https://cdnjs.cloudflare.com/ajax/libs/ace/1.2.5/ace.js' type='text/javascript'></script>
	<script src='https://cdnjs.cloudflare.com/ajax/libs/ace/1.2.5/mode-groovy.js' type='text/javascript'></script>
   
    <script src="js/jsoneditor.min.js"></script>
    
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
        <h1>情境設定介面</h1>
      </div>
    </div>
    <!-- div class='row'>
      <div class='medium-6 columns'>
        <p>JSON Editor supports these popular CSS frameworks:</p>
        <ul>
          <li>Bootstrap 2</li>
          <li>Bootstrap 3</li>
          <li>Foundation 3</li>
          <li>Foundation 4</li>
          <li>Foundation 5 (shown here)</li>
          <li>jQuery UI</li>
        </ul>
      </div>
      <div class='medium-6 columns'>
        <p>JSON Editor supports these popular icon libraries:</p>
        <ul>
          <li>Bootstrap 2 Glyphicons</li>
          <li>Bootstrap 3 Glyphicons</li>
          <li>Foundicons 2</li>
          <li>Foundicons 3</li>
          <li>jQueryUI</li>
          <li>Font Awesome 3</li>
          <li>Font Awesome 4 (shown here)</li>
        </ul>
      </div>
    </div -->
    <div class='row'>
      <div class='medium-12-columns'>
        <button id='submit' class='btn btn-danger'>儲存</button>
        <button id='restore' class='btn btn-warning'>重設</button>
        <span id='valid_indicator' class='label'></span>
      </div>
    </div>
    <div class='row'>
      <div id='editor_holder' class='medium-12 columns'></div>
    </div>
    </div>
    
    <script>
      // This is the starting value for the editor
      // We will use this to seed the initial editor 
      // and to provide a "Restore to Default" button.
      var starting_value = <%= dialog.toString() %>;
      
      // Initialize the editor
      var editor = new JSONEditor(document.getElementById('editor_holder'),{
        // Enable fetching schemas via ajax
        ajax: true,
        
        // The schema for the editor
        schema: {
          $ref: "qa-dialog-schema-ajax.jsp?type=dialog",
        },
        
        // Seed the form with a starting value
        startval: starting_value
      });
      
      // Hook up the submit button to log to the console
      document.getElementById('submit').addEventListener('click',function() {
        // Get the value from the editor
        console.log(editor.getValue());
      });
      
      // Hook up the Restore to Default button
      document.getElementById('restore').addEventListener('click',function() {
        editor.setValue(starting_value);
      });
      
      // Hook up the validation indicator to update its 
      // status whenever the editor changes
      editor.on('change',function() {
        // Get an array of errors from the validator
        var errors = editor.validate();
        
        var indicator = document.getElementById('valid_indicator');
        
        // Not valid
        if(errors.length) {
          indicator.className = 'label alert';
          indicator.textContent = 'not valid';
        }
        // Valid
        else {
          indicator.className = 'label success';
          indicator.textContent = 'valid';
        }
      });
      
      $('#submit').click(function() {
    	  val = editor.getValue();
    	  
    	  <% if (id != null) { %>
    	  data = { action: "saveNew", id: '<%= id %>', data: JSON.stringify(val) };
    	  <% } else { %>
    	  data = { action: "save", data: JSON.stringify(val) };
    	  <% } %>
    	  
    	  $.ajax({
    		url: "qaScenarioEditor-ajax.jsp",
    		type: "post",
    		data: data,
    		dataType: "json",
    		success: function() {
    			
    		}
    	  });
      });
      
    </script>
  </body>
</html>
