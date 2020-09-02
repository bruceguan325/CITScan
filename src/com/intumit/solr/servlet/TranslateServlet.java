package com.intumit.solr.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import com.intumit.solr.util.WiSeUtils;

public class TranslateServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException {
    	
    	String content = req.getParameter("content");
    	String convertTo = req.getParameter("convertTo");
    	
    	JSONObject obj = new JSONObject();
    	String translated = null;
    	
    	if ("t".equalsIgnoreCase(convertTo)) {
    		translated = WiSeUtils.cn2tw(content);
    	}
    	else if ("s".equalsIgnoreCase(convertTo)) {
    		translated = WiSeUtils.tw2cn(content);
    	}
    	
    	if (translated != null) {
    		obj.put("status", 200);
    		obj.put("translated", translated);
    	}
    	else {
    		obj.put("status", 500);
    	}
    	
    	resp.setContentType("application/json");
    	resp.setCharacterEncoding("UTF-8");
    	
    	
    	try {
			resp.getWriter().print(obj.toJSONString());
			resp.flushBuffer();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    }
}
