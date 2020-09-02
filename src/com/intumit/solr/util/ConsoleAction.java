package com.intumit.solr.util;


import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.control.CompilerConfiguration;

import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;

public class ConsoleAction extends HttpServlet  {
	private static final long serialVersionUID = 12346254546356L;
	public static String ipAuth ;
	public static String envAuth ;
	public static String admAuth ;
	
	public static OutputStream groovyOut;
	
	
    public void init(){
		ipAuth =getServletConfig().getInitParameter("ipAuth");
		ipAuth=ipAuth==null ?null :ipAuth.trim();
		envAuth =getServletConfig().getInitParameter("envAuth");
		envAuth=envAuth==null ?null :envAuth.trim();
		admAuth =getServletConfig().getInitParameter("admAuth");
		admAuth=admAuth==null ?null :admAuth.trim();

		groovyOut = new ByteArrayOutputStream();
		Binding binding = new Binding();
		binding.setProperty("out", new PrintStream(groovyOut));
		GroovyClassLoader gcloader = new GroovyClassLoader(ConsoleAction.class.getClassLoader());
		CompilerConfiguration gcon = new CompilerConfiguration();
		gcon.setScriptBaseClass("com.intumit.solr.util.GroovyScriptTool");
		shell = new GroovyShell(gcloader, binding, gcon);
    }


	public static GroovyShell shell=null;


	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException{
		this.securityChech(request, response);
		init();
		if(request.getParameter("file")!=null){
			InputStream is = ConsoleAction.class.getClassLoader().getResourceAsStream("/gwc/" + request.getParameter("file"));
			ServletOutputStream os = response.getOutputStream();
			IOUtils.copy(is, os);
			is.close();
			os.close();
		}else if(request.getParameter("code")!=null){
			String code = request.getParameter("code");
			Object re = null;
			try {
				
				if (code != null && code.length() > 0)
					re = shell.evaluate(code);
				
				re = groovyOut.toString();
			} catch (Exception ex) {
				re = ex;
			}

			try {
				response.setHeader("content-type", "text/html; charset=UTF-8");
				String restring = re==null?"":re.toString();
				response.getOutputStream().write(restring.toString().getBytes());
				response.getOutputStream().flush();
				response.getOutputStream().close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else{
			InputStream is = ConsoleAction.class.getClassLoader().getResourceAsStream("/gwc/groovy.htm");
			ServletOutputStream os = response.getOutputStream();
			IOUtils.copy(is, os);
			is.close();
			os.close();
		}
	}
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
		doGet(request,response);
	}
	
	
	private void securityChech(HttpServletRequest request, HttpServletResponse response){
		String sysValue="";
		if(ipAuth==null && envAuth==null && admAuth==null){
			try {
				response.getWriter().write("請設定ipAuth或者envAuth或者admAuth");
				response.getWriter().flush();
				response.getWriter().close();
				return ;
			} catch (IOException e) {
				e.printStackTrace();
			};
		}
		
		boolean ipok=false;
		boolean envok=false;
		boolean admok=false;
		
		if(ipAuth==null)ipok=true;
		else{
			String ip= request.getRemoteAddr();
			if(ip.matches(ipAuth))ipok=true;
			else ipok=false;
		}
		if(envAuth==null)envok=true;
		else{
			String envAuthKey = envAuth.substring(0, envAuth.indexOf('='));
			String envAuthValue = envAuth.substring(envAuth.indexOf('=')+1);
			sysValue = System.getProperty(envAuthKey);
			if(sysValue==null || !sysValue.matches(envAuthValue))envok=false;
			else envok=true;
		}
		if(admAuth==null)admok=true;
		else{
			List<String> allowedAdminId = Arrays.asList(StringUtils.split(admAuth, ","));
			AdminUser user = AdminUserFacade.getInstance().getFromSession(request.getSession());
			
			if (user != null && (allowedAdminId.contains("" + user.getId()) || allowedAdminId.contains("*"))) {
				admok = true;
			}
			else {
				admok = false;
			}
		}
		
		if(ipok&&envok&&admok);
		else{
			try {
				String freason = "";
				if(!ipok) freason ="your ip is forbidden to access, your ip :"+request.getRemoteAddr()+"\n";
				if(!envok) freason =freason+"  current env is forbidden to access console, current env is :"+sysValue;
				if(!admok) freason =freason+"  current admin is forbidden to access console";
				response.getWriter().write(freason);
				response.getWriter().flush();
				response.getWriter().close();
				return ;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
