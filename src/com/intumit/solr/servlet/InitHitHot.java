package com.intumit.solr.servlet;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.elasticsearch.common.lang3.StringUtils;
import org.json.JSONObject;

import com.intumit.license.LicenseChecker;
import com.intumit.message.MessageUtil;
import com.intumit.solr.SearchManager;
import com.intumit.solr.robot.EventCenter;
import com.intumit.solr.robot.EventType;
import com.intumit.solr.robot.QAChannel;
import com.intumit.solr.robot.QADialogConfig;
import com.intumit.solr.robot.RobotFormalAnswers;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.EmbeddedFuzzyDictionaryDatabase;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.intumit.solr.tenant.Apikey;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.systemconfig.WiseSystemConfigFacade;

public class InitHitHot implements ServletContextListener {
	
	private static final Logger logger = Logger.getLogger(InitHitHot.class);

	public void contextInitialized(ServletContextEvent servletcontextevent) {
		ServletContext servletCtx = servletcontextevent.getServletContext();
		String skMode = servletCtx.getInitParameter("SearchKeywordLog.Mode");
		
		Logger.getLogger("com.mchange").setLevel(Level.INFO);
		Logger.getLogger("com.intumit").setLevel(Level.INFO);
		Logger.getLogger("org.apache").setLevel(Level.INFO);
		Logger.getLogger("org.hibernate").setLevel(Level.INFO);
		Logger.getLogger("org.quartz").setLevel(Level.INFO);

		if (skMode == null) {
			SearchKeywordLogFacade.getInstance().setMode("db");
		}
		else {
			SearchKeywordLogFacade.getInstance().setMode(skMode);
		}

		WiseSystemConfig cfg = WiseSystemConfigFacade.getInstance().get();

		if (cfg == null) {
			cfg = WiseSystemConfigFacade.getInstance().createInitValue();
			try {
				WiseSystemConfigFacade.getInstance().update(cfg);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		String skDateFrom = servletcontextevent.getServletContext().getInitParameter("SearchKeywordLog.DefaultDateFrom");
		if (skDateFrom != null) {
			SearchKeywordLogFacade.setDefaultDateFrom(skDateFrom);
		}
		else {
			if (cfg.getHotKeywordFrom() != null) {
				SearchKeywordLogFacade.setDefaultDateFrom("NOW+8HOUR/DAY-8HOUR-" + cfg.getHotKeywordFrom() + "DAY");
			}
		}
		String skDateTo = servletcontextevent.getServletContext().getInitParameter("SearchKeywordLog.DefaultDateTo");
		if (skDateTo != null) {
			SearchKeywordLogFacade.setDefaultDateTo(skDateTo);
		}
		
		MessageUtil.initialize();
		SearchManager.initESNode();
		LicenseChecker.check();
		
		if (!WiSeEnv.isRobotIndexMode()) {
    		HazelcastUtil.init();
    		EventCenter.init();
    		DictionaryDatabase.init();
    		EmbeddedFuzzyDictionaryDatabase.init();
    		RobotFormalAnswers.init();
    		Tenant.initDefaultRobotFormalAnswer(null);
    		EventType.initBuiltInTypes();
    		
    		// Migrate Tenant.getAllowedOptionsJson to QAChannel.getReturnOptionsInJson
    		try {
    			for (Tenant t: Tenant.list()) {
    				if (StringUtils.isNotEmpty(t.getAllowedOptionsJson())) {
    					List<String> chCodes = Arrays.asList(StringUtils.split(t.getAllowedOptionsJson(), ","));
    					for (String chCode: chCodes) {
    						QAChannel ch = QAChannel.get(t.getId(), chCode);
    						if (ch != null) {
    							ch.setReturnOptionInJson(true);
    							ch.setAppendOptionToOutput(false);
    							QAChannel.saveOrUpdate(ch);
    						}
    					}
    					// 移到了 QAChannel 中，這裡當然就要清掉
    					t.setAllowedOptionsJson("");
    					Tenant.saveOrUpdate(t);
    					
    					System.out.println("Migrate getAllowedOptionsJson from Tenant to QAChannel [" + t.getName() + "]...done");
    				}
    			}
    		}
    		catch (Exception ex) {
    			ex.printStackTrace();
    		}
    		
    		// Migrate Tenant.apikey to Apikey
    		// Actually we should remove those code someday.
    		for (Tenant t: Tenant.list()) {
    			if (StringUtils.isNotEmpty(t.getApikey())) {
    				Apikey key = Apikey.getByApiKey(t.getApikey());
    				
    				if (key == null) {
    					// 將這個 key 新增到 Apikey 當中
    					Apikey newKey = new Apikey();
    					newKey.setApikey(t.getApikey());
    					newKey.setTenantId(t.getId());
    					newKey.setName("Migrate from Tenant.Apikey");
    					newKey.setEnableApikey(true);
    					Apikey.saveOrUpdate(newKey);
    					
    					// 移到了 Apikey table 中，這裡當然就要清掉
    					t.setApikey("");
    					Tenant.saveOrUpdate(t);
    					
    					System.out.println("Migrate apikey from Tenant to Apikey [" + t.getName() + "]...done");
    				}
    			}
    		}
    		
    		// Migrate Tenant.lineBotConfigJson format 
    		for (Tenant t: Tenant.list()) {
    			if (StringUtils.isNotEmpty(t.getLineBotConfigJson())) {
    				try {
    					JSONObject j = new JSONObject(t.getLineBotConfigJson());
    					
    					// If has "accessToken" in root, it should be old format only for LINE, let's migrate it!
    					if (j.has("accessToken")) {
    						JSONObject newJ = new JSONObject();
    						newJ.put("line", j);
    						
    						t.setLineBotConfigJson(newJ.toString(2));
    						
    						Tenant.saveOrUpdate(t);
    						System.out.println("Migrate old bot config json in [" + t.getName() + "]...done");
    					}
    				}
    				catch (Exception ignoreIt) {}
    			}
    		}
		}
	}

	public void contextDestroyed(ServletContextEvent servletcontextevent) {
		SearchManager.closeESNode();
	}

}
