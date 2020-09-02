package com.intumit.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.intumit.solr.synonymKeywords.SynonymKeywordFacade;

public class QuerySynonymServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse response)
			throws ServletException, IOException {
		String keyword = "";
		String command = "";
		try {
			if (req.getParameter("keyword") != null)
				keyword = req.getParameter("keyword");
			if (req.getParameter("command") != null)
				command = req.getParameter("command");

		} catch (Exception e) {
			log(e.getMessage(), e);
		}
		response.setContentType("text/xml; charset=utf-8");
		response.setCharacterEncoding("utf-8");

		com.intumit.solr.tenant.Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(req.getSession());
		
		if("partial".equals(command)){
			response.getWriter().write(SynonymKeywordFacade.getInstance().querySynonymKeywordXmlPartial(t.getId(), keyword));
		}else{
			response.getWriter().write(SynonymKeywordFacade.getInstance().querySynonymKeywordXmlFull(t.getId(), keyword));
		}

	}
}
