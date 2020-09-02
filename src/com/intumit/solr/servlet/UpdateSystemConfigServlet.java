package com.intumit.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.intumit.message.MessageUtil;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.systemconfig.WiseSystemConfigFacade;

public class UpdateSystemConfigServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res)
			throws ServletException, IOException {
		System.out.println("System config post.");
		WiseSystemConfigFacade facade = WiseSystemConfigFacade.getInstance();
		try {
			WiseSystemConfig config = facade.loadFromRequest(req);
			facade.update(config);

			if (config.getHotKeywordFrom() != null) {
				SearchKeywordLogFacade.getInstance().setDefaultDateFrom("NOW+8HOUR/DAY-8HOUR-" + config.getHotKeywordFrom() + "DAY");
			}
			
			res.sendRedirect(req.getContextPath() + WiSeEnv.getAdminContextPath() + "/wiseSystemConfig.jsp?msg=" + java.net.URLEncoder.encode(MessageUtil.getMessage(req.getLocale(), "already.update"), "UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
