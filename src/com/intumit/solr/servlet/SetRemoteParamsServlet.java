package com.intumit.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.intumit.dir.Authenticator;
import com.intumit.dir.BrowseDirService;
import com.intumit.dir.RemoteConnParams;

public class SetRemoteParamsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

        String host = req.getParameter("host");
        Authenticator auth = new Authenticator();
        auth.setDomain(req.getParameter("domain")==null?"":req.getParameter("domain"));
        auth.setUserName(req.getParameter("userName")==null?"":req.getParameter("userName"));
        auth.setPassword(req.getParameter("password")==null?"":req.getParameter("password"));

        RemoteConnParams params = new RemoteConnParams(host, auth);

        BrowseDirService bds = BrowseDirService.getInstance();
        if (bds.testConnect(host, auth)) {
            params.setToSession(req.getSession());
    		req.getRequestDispatcher("BrowseRemoteDirDialog.jsp").forward(req,
    				resp);
        }
        else {
           System.out.println("remote connection error");
       
        }
		
	}

}
