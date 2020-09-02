package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.SearchManager;
import com.intumit.solr.searchKeywords.SearchKeywordLog;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.thoughtworks.xstream.XStream;

public class SearchManagerServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String command = StringUtils.defaultString(req.getParameter("command"))
				.toLowerCase();
		ArrayList<String>status = new ArrayList<String>();

		if (StringUtils.isNotEmpty(command)) {
			resp.setContentType("text/xml; charset=utf-8");
			resp.setCharacterEncoding("utf-8");
			
			status.add(command);
			String coreName = "core0";
			
			if (req.getParameter("core") != null) {
				int core = Integer.parseInt(req.getParameter("core"));
				coreName = "core" + core;
			}
			else if (req.getParameter("coreName") != null) {
				coreName = req.getParameter("coreName");
			}
			status.add("" + coreName);
			
			if ("commit".equals(command)) {
				boolean result = SearchManager.commit(coreName);
				if (result)
					status.add("succeed");
				else
					status.add("failed");
				
				
			} else if ("XXXX".equals(command)) {
			} else {
				status.add("Unknown command [" + command + "]");
			}

			XStream out = new XStream();
			out.alias("SMS", SearchManagerServlet.class);
			resp.getWriter().write(out.toXML(status));
		}
	}
}
