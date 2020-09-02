package com.intumit.solr.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class IntumitTestServlet
 */
@WebServlet("/wiseadm/IntumitVlog")
public class IntumitTestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String testCode = request.getParameter("Vlog");
		if("intumit!!".contentEquals(testCode)) {
			request.getSession().setAttribute("isTestAdmin", true);
			request.getRequestDispatcher("LogList.jsp").forward(request, response);
		}else {
			request.setAttribute("isTestAdmin", false);
			request.getRequestDispatcher("../ErrorPage404.jsp").forward(request, response);;
		}
	}

}
