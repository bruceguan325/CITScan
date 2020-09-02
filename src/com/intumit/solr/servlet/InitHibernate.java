package com.intumit.solr.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.lang.StringUtils;

import com.intumit.hibernate.HibernateUtil;
import com.intumit.hibernate.HibernateUtil.SqlType;
import com.intumit.solr.SearchManager;

public class InitHibernate implements ServletContextListener {

	public void contextInitialized(ServletContextEvent servletcontextevent) {
		
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				
				String sql = StringUtils.defaultString(System.getProperty("sql"), "MSSQL");
				
				try {
					SqlType st = SqlType.valueOf(sql);
					if (st != null) {
						HibernateUtil.SQL_TYPE = st;
					}
				}
				catch (Exception ex) {
					System.out.println("Unknown SQL TYPE [" + sql + "]");
				}
				
				HibernateUtil.init();
			}
			
		});
		
		try {
			if (SearchManager.isCloudMode()) {
				t.run();
				// t.start();
				// maybe someday we can use t.start() (Async initialization) to avoid db failed in cloud mode.
			}
			else {
				t.run();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void contextDestroyed(ServletContextEvent servletcontextevent) {
		HibernateUtil.shutdown();
	}

}
