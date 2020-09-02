package com.intumit.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

public class APIRequestHandler extends HttpServlet {

	private static final long serialVersionUID = 3747679347912671624L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setCharacterEncoding("UTF-8");

		String cmd = req.getRequestURI();
		cmd = StringUtils.substringAfter(cmd, req.getContextPath());
		System.out.print("API REQUEST: [" + cmd + "]");
		
		if (cmd.startsWith("/1/")) {
			// 違反人體工學的 [\.\-\\/] 變成 [\\.\\-\\\\/]
			String jspName = "/api/" + StringUtils.substringAfter(cmd, "/1/").replaceAll("[\\.\\-\\\\/]", "-") + ".jsp";
			System.out.print(" ==> Final Target: [" + jspName + "]");
			
			if (StringUtils.isNotBlank(jspName)) {
				req.setAttribute("fromAPIRequestHandler", true);
				req.getRequestDispatcher(jspName).forward(req, resp);
			}
			else {
				resp.sendRedirect(req.getContextPath());
			}
		}
		else {
			resp.sendRedirect(req.getContextPath());
		}
		
		System.out.println();
	}

}
