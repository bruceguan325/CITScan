<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="com.intumit.solr.robot.QAAltBuild"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.Map"%>
<%@page import="com.fasterxml.jackson.databind.ObjectMapper"%>
<%@page import="java.util.List"%>
<%@page import="com.intumit.solr.robot.*"%>
<%@page import="java.util.Collection"%>
<%@page import="com.intumit.solr.robot.QASaver"%>
<%@page import="com.intumit.solr.robot.QA"%>
<%@page import="com.intumit.solr.robot.QAUtil"%>
<%@page import="com.intumit.solr.tenant.Tenant"%>
<%@ page language="java" contentType="application/json"
	pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%
    Tenant t = Tenant.getFromSession(session);
    Long kid = new Long(request.getParameter("kid"));
    String q = request.getParameter("q");
    int n = 10;
    try {
        n = new Integer(request.getParameter("n"));
    } catch(NumberFormatException e) {
        e.printStackTrace();
    }

    QAUtil qaUtil = QAUtil.getInstance(t);
    QA qa = new QA(qaUtil.getMainQASolrDocument(kid));
    QAAltBuild build = new QAAltBuild();
    build.setTenantId(t.getId());
    build.setDerivedAltTpls(StringUtils.join(qa.getQuestionAltTemplates(), '\n'));
    QASaver qaSaver = new QASaver(build, qa);
    Collection<String> alts = qaSaver.expandAlt(null);

    MostRelatedQAAltFinder finder = new MostRelatedQAAltFinder();
    List<RelatedQAAltSearchResult> altSearchResults = finder.search(t.getCoreName(), q,
            alts.toArray(new String[] {}), n);

    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> result = new HashMap<>();
    result.put("alts", altSearchResults);
%>
<%=mapper.writeValueAsString(result)%>