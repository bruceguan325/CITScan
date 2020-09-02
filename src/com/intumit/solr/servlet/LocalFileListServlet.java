package com.intumit.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LocalFileListServlet extends HttpServlet{

	private String josn="";

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
		String path=req.getParameter("path")==null?"":req.getParameter("path");
		
		if(path.equals("")){
			josn=josn+"[";
			josn=josn+"\"text\":";
			josn=josn+"{";
			josn=josn+"}";
			josn=josn+"]";
			
		}
		
	}

}
