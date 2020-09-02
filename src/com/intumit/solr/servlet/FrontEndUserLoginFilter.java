package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.intumit.solr.tenant.Apikey;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.RateLimiter;
import com.intumit.solr.util.WiSeUtils;
//import com.intumit.solr.util.WiSeUtils;

public class FrontEndUserLoginFilter implements Filter {
    
	private static boolean checked = false;
	
    String[] bypassPathes = {
        "img", "images", "script", "styles", "template", "assets", "api", 
        "ckeditor", 
        "commons", "nss", "tts",
        "ErrorPage", // 錯誤畫面也不需要限制
        "cache",
        "wiseadm", // 後台 bypass 給後台的 Filter 處理
        "captcha", "Blank.html", 
        "ms-bfc", "conn-", // 給不同的通訊軟體專用的 connector
        "login.jsp", "sign-in.jsp", // 這個登入頁目前應該暫時可能沒機會用到了
        "chats.jsp", "chats_tpl.jsp", "chats_tpl_form.jsp", "chats-ajax.jsp", // 前端客戶問答介面，自帶 API 檢核，因此這邊跳過
        "qa-eval-log-ajax.jsp", "qa-eval-log-line.jsp",
        "qa-ajax", "qa-history.jsp", "qa-sim-ajax.jsp", // 標準 API 
        "sr-ajax", "qa-hot-ajax", "qa-keyword-ajax.jsp", // 標準 API
        "hoaLine",// 中亞
        "ro.jsp", // 導外
        "ss.jsp", // 縮址
        "nlp-ajax.jsp", // 不確定用途
        "bind-adm-user.jsp", // 給 KMS 用
        "capital-ia.jsp", "capitalNewsUploadFile", // 群益的新聞 Excel 自動標記股票代號的程式，是放在雲端
        "1", "2", "3", "4", // 給專屬 API URL Path 用
        "SCForm.jsp", "SCForm-ajax.jsp",
        "helpdesk",  // helpdesk demo 頁面用
        "webchat",  // 新版對話介面
        "citi-detail",
        "citi-misReport.jsp"
    };


	public void init(FilterConfig config) throws ServletException {

	}

	private static final Map<String, RateLimiter> KEY_RATE_MAP = new HashMap<String, RateLimiter>();

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
    ServletException {
		HttpServletRequest req = ((HttpServletRequest) request);
		HttpServletResponse resp = (HttpServletResponse) response;
        String reqUrl = req.getRequestURI();
		if (!checked && !StringUtils.startsWith(reqUrl, "/wise/wiseadm")) {
			try {
				String adminURL = "";
				try {
					if (System.getProperty("robot.admin.url") != null) {
						adminURL = System.getProperty("robot.admin.url");
					}
				} catch (Exception e) {
					System.out.println(e);
				}
				
				System.out.print("Now initializing login.jsp....");
				WiSeUtils.getDataFromUrl(adminURL);
				System.out.println("Done!");
				checked = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		String reqUri = req.getRequestURI();
		String page = reqUri.replaceAll(req.getContextPath() + "/", "");
		String apikey = req.getParameter("apikey");
		String qaId = req.getParameter("id");

		Tenant t = null;
		Apikey key = null;
		if (apikey != null) {
			key = Apikey.getByApiKey(apikey);

			if (key != null && key.isValid()) {
				t = key.getTenant();
			}
		}

		synchronized (KEY_RATE_MAP) {
			if (t != null && key != null && qaId != null) {
				// Prevent flooding API request from the same tenant, apikey and qaId.
				if (KEY_RATE_MAP.get(t.getId() + "-" + key.getApikey() + "-" + qaId) == null) {
					RateLimiter rateLimiter = null;
					if (key != null && key.getEnableRateLimitByQAContext()) {
						if (rateLimiter == null) {
							rateLimiter = RateLimiter.create(key.getRateLimitByQAContextPerSec());
						}
					} else if (t != null && t.getEnableRateLimitByQAContext()) {
						if (rateLimiter == null) {
							rateLimiter = RateLimiter.create(t.getRateLimitByQAContextPerSec());
						}
					}
					if (rateLimiter != null) {
						KEY_RATE_MAP.put(t.getId() + "-" + key.getApikey() + "-" + qaId, rateLimiter);
						rateLimiter.acquire();
					}
				} else {
					KEY_RATE_MAP.get(t.getId() + "-" + key.getApikey() + "-" + qaId).acquire();
				}
			}
		}

		if (StringUtils.startsWithAny(page, bypassPathes)) {
			if ((page.contains("sticker/") || StringUtils.startsWith(page, "ckeditor-upload/"))
					&& StringUtils.endsWithAny(page, new String[] { "/1040", "/700", "/460", "/300", "/240" })) {
				String newUrl = "/" + page.substring(0, page.lastIndexOf("/"));
				req.getRequestDispatcher(newUrl).forward(req, resp);
			} else {
				chain.doFilter(request, response);
			}
		} else if (t == null || key == null) {
			String forward = "/ErrorPage403.jsp";
			if (!req.getRequestURI().startsWith(forward))
				// resp.sendRedirect(req.getContextPath() + "/ErrorPage403.jsp");
				req.getRequestDispatcher(forward).forward(req, resp);
		}
	}

	@Override
	public void destroy() {

	}

}
