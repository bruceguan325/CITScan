<%@page language="java" contentType="application/json; charset=UTF-8"

import="org.apache.wink.json4j.*"
import="java.util.HashMap"
import="java.util.Map"
import="java.util.ArrayList"
import="java.util.List"
import="java.io.*"
import="com.intumit.solr.robot.*"
import="org.apache.struts.Globals"
import="java.util.Locale"
%>
<%@ page import="com.intumit.solr.admin.*" %>
<%
if (AdminGroupFacade.getInstance().getFromSession(session).getAdminAdminCURD() == 0
		&& !("admin".equals(AdminUserFacade.getInstance().getFromSession(session).getLoginName()) && 1==AdminUserFacade.getInstance().getFromSession(session).getId())) {
	return;
}
%>
<%
int tenantId = Integer.valueOf(request.getParameter("tenantId"));
Locale locale = (Locale)request.getSession().getAttribute(Globals.LOCALE_KEY);
Integer id = null;
String type = request.getParameter("type");
String name = request.getParameter("name");
String code = request.getParameter("code");
String defaultChannelCode = request.getParameter("defaultChannelCode");
String channelType = request.getParameter("channelType");

String returnOptionInJson = request.getParameter("returnOptionInJson");
String appendOptionToOutput = request.getParameter("appendOptionToOutput");
String useHtmlNewline = request.getParameter("useHtmlNewline");
String supportMultiRichMessages = request.getParameter("supportMultiRichMessages");
String useCustomMatchCtrlFlow = request.getParameter("useCustomMatchCtrlFlow");

try {
	id = new Integer(request.getParameter("id"));
}
catch (Exception ignoreIt) {}

System.out.println( "add attr tenantId:" + tenantId + " type:"+type + " name:" + name + " code:" + code );

JSONObject json = new JSONObject();
Map<String, Object> resultData = new HashMap<String, Object>();

if (name==null || code ==null || name.equals("") || code.equals("") ) {
	resultData.put("fail", "名稱與代號不可為空值");
	json.putAll( resultData );
	response.getWriter().println(json.toString(4));
	return;
}

if (type.equals("channel")) {
	QAChannel existChannel = null;
	QAChannel dupChannel = null;
	
	if (id != null && id >= 0) {
		existChannel = QAChannel.get(id);
		
		if (existChannel != null && existChannel.getTenantId() != tenantId)
			return;  // 避免跨公司 hack
	}
	
	dupChannel = QAChannel.get(tenantId, name, null);
	
	if (dupChannel != null) {
		if (existChannel != null && existChannel.getId() != dupChannel.getId()) {
			resultData.put("fail", "名稱重複");
			json.putAll( resultData );
			response.getWriter().println(json.toString(4));
			return;
		}
		else {
		}
	}
	
	dupChannel = QAChannel.get(tenantId, null, code);
	if (dupChannel != null) {
		if (existChannel != null && existChannel.getId() != dupChannel.getId()) {
			resultData.put("fail", "代號重複");
			json.putAll( resultData );
			response.getWriter().println(json.toString(4));
			return;
		}
		else {
		}
	}
	
	QAChannel newChannel = existChannel != null ? existChannel : new QAChannel();
	
	// 如果已存在的Channel代號被更改，MultiChannelAnswer所有原為這個代號的一併更新新的代號
	if (existChannel != null && !existChannel.getCode().equals(code)) {
		List<MultiChannelAnswer> mcas = MultiChannelAnswer.listByChannel(tenantId, existChannel.getCode());
		for (MultiChannelAnswer mca : mcas) {
			mca.setChannel(code);
			MultiChannelAnswer.saveOrUpdate(mca);
		}
	}
	
	// 若指定的 defaultChannel 不存在了，就指回 DEFAULT_CHANNEL_CODE
	QAChannel defaultChannel = QAChannel.get(tenantId, null, defaultChannelCode);
	if (defaultChannel == null) {
		defaultChannelCode = QAChannel.DEFAULT_CHANNEL_CODE;
	}
	
	newChannel.setTenantId(tenantId);
	newChannel.setName(name);
	newChannel.setCode(code);
	newChannel.setDefaultChannelCode(defaultChannelCode);
	newChannel.setAppendOptionToOutput(appendOptionToOutput != null ? new Boolean(appendOptionToOutput) : null);
	newChannel.setReturnOptionInJson(returnOptionInJson != null ? new Boolean(returnOptionInJson) : null);
	newChannel.setUseHtmlNewline(useHtmlNewline != null ? new Boolean(useHtmlNewline) : null);
	newChannel.setType(QAChannelType.valueOf(channelType));
	newChannel.setSupportMultiRichMessages(supportMultiRichMessages != null ? new Boolean(supportMultiRichMessages) : null);
	newChannel.setUseCustomMatchCtrlFlow(useCustomMatchCtrlFlow != null ? new Boolean(useCustomMatchCtrlFlow) : null);
	QAChannel.saveOrUpdate(newChannel);
	
	JSONArray channelArray = new JSONArray(QAChannel.getArrayData(tenantId));
	resultData.put("channel", channelArray);
	
	json.putAll( resultData );
}

if (type.equals("userType")) {
	
	if (QAUserType.get(tenantId, name, null)!=null) {
		resultData.put("fail", "名稱重複");
		json.putAll( resultData );
		response.getWriter().println(json.toString(4));
		return;
	}
	if (QAUserType.get(tenantId, null, code)!=null) {
		resultData.put("fail", "代號重複");
		json.putAll( resultData );
		response.getWriter().println(json.toString(4));
		return;
	}
	
	QAUserType newUserType = new QAUserType();
	newUserType.setTenantId(tenantId);
	newUserType.setName(name);
	newUserType.setCode(code);
	QAUserType.saveOrUpdate(newUserType);
	
	JSONArray userTypeArray = new JSONArray(QAUserType.getArrayData(tenantId, locale));
	resultData.put("userType", userTypeArray);
	
	json.putAll( resultData );
}
%>
<%= json.toString(4) %>
