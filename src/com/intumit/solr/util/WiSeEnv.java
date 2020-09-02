package com.intumit.solr.util;

public class WiSeEnv {
	static String kernelPath = null;
	static String wiseadmContextPath = "/wiseadm";
	static String webServerHome = null;
	static Boolean isSrbtIdxMode = null;
	
	public static String getHomePath() {
		if (kernelPath == null) {
			String solrHome = System.getProperty("solr.solr.home");
			
			if (solrHome != null) {
				kernelPath = solrHome;
			}
			else {
				kernelPath = ".";
			}
		}
		
		return kernelPath;
	}
	
	public static String getWebServerHome() {
		if (webServerHome == null) {
			String jettyHome = System.getProperty("jetty.home");
			
			if (jettyHome != null) {
				webServerHome = jettyHome;
			}
			else {
				webServerHome = ".";
			}
		}
		
		return webServerHome;
	}
	
	public static void setAdminContextPath(String newPath) {
		wiseadmContextPath = newPath;
	}
	
	public static String getAdminContextPath() {
		return wiseadmContextPath;
	}
	
	public static boolean isRobotIndexMode() {
		if (isSrbtIdxMode == null) {
    		String srbtIdxMode = System.getProperty("robot.index.mode");
    		
    		if (srbtIdxMode != null) {
    			isSrbtIdxMode = Boolean.parseBoolean(srbtIdxMode);
    		}
    		else {
    			isSrbtIdxMode = false;
    		}
		}
		
		return isSrbtIdxMode;
	}
}
