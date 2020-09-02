package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jdom.input.SAXBuilder;

import com.intumit.parser.FileParser;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.systemconfig.WiseSystemConfigFacade;

public class InitServlet implements Servlet,ServletConfig{

    private static final long serialVersionUID = 1L;
    private static InitServlet instance;


    public static InitServlet getInstance() {
        return instance;
    }

    public void init(ServletConfig config) throws ServletException {
        initIfilterPath();
        instance = this;
    }


    public void initIfilterPath() {
        try {
            //為了要拿到ifliter位置
			int str_length = String.valueOf(FileParser.class.getResource("")).length();
			String str = String.valueOf(FileParser.class.getResource("")).substring(6, str_length);
            System.setProperty("ifilter_path", str);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }



	public void destroy() {
        // TODO Auto-generated method stub
        
    }

    public ServletConfig getServletConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServletInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException,
            IOException {
        // TODO Auto-generated method stub
        
    }

    public String getInitParameter(String arg0) {
        // TODO Auto-generated method stub
        return null;
    }

    public Enumeration getInitParameterNames() {
        // TODO Auto-generated method stub
        return null;
    }

    public ServletContext getServletContext() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getServletName() {
        // TODO Auto-generated method stub
        return null;
    }

}
