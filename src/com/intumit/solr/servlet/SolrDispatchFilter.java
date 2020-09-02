/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.intumit.solr.servlet;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Set;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.SearchManager;
import com.intumit.solr.admin.AdminGroupFacade;
import com.intumit.solr.admin.AdminLocation;
import com.intumit.solr.admin.AdminLocationFacade;
import com.intumit.solr.admin.AdminUser;
import com.intumit.solr.admin.AdminUserFacade;
import com.intumit.solr.robot.QAAltBuildQueue;
import com.intumit.solr.robot.QAEvaluationLogQueue;
import com.intumit.solr.tenant.Apikey;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.solr.util.XssStringFilter;
import com.intumit.systemconfig.WiseSystemConfigFacade;
import com.intumit.util.DesUtil;

/**
 * This filter looks at the incoming URL maps them to handlers defined in
 * solrconfig.xml
 *
 * @since solr 1.2
 */
public class SolrDispatchFilter extends org.apache.solr.servlet.SolrDispatchFilter {

    private static Set<String> liveNodeIps;
    private static final String CORE_OPER_REGEX = "/core.*?/.*?";
    static Logger infoLog = Logger.getLogger(SolrDispatchFilter.class.getName());
    static XssStringFilter xssStr = new XssStringFilter();

    public void init(FilterConfig config) throws ServletException {
        super.init(config);
        SearchManager.setLocalCores(cores);
        QAAltBuildQueue.startProcess();
        QAEvaluationLogQueue.startProcess();
    }

    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getRemoteAddr();

        if (WiseSystemConfigFacade.getInstance().get().isAllowedReverseProxy(ip)) {
            ip = new XssHttpServletRequestWrapper(request).getHeader("X-Forwarded-For");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
        }
        return ip;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = ((HttpServletRequest)request);
        HttpServletResponse resp = (HttpServletResponse)response;
        HttpSession sess = req.getSession();

        AdminUser user = AdminUserFacade.getInstance().getFromSession(sess);

        String remoteIp = request.getRemoteAddr().toString();
        String reqUri = req.getRequestURI();

        if (user == null && 
                (reqUri.toLowerCase().startsWith(req.getContextPath().toLowerCase() + "" + WiSeEnv.getAdminContextPath() + "/qa"))) {
            // 僅針對 wiseadm/qa* 的 URL 才企圖判斷 user code，不然會影響 solr 本身的 core servlet 行為（會拋 SolrException）
            infoLog.info(String.format("******IP [%s], URL [%s]", getClientIpAddr(req), req.getRequestURL()));
            user = getUserFromCode(sess, request.getParameter("code"));
        }

        Set<AdminLocation> admLocs = AdminLocationFacade.getInstance().findAll(remoteIp, user == null ? null : user.getLoginName());

        boolean gotoLogin = false;
        String loginFormDispatchPath = "/wiseadm/login.jsp";

        SecureRandom secRandom = new SecureRandom();
        int rand = secRandom.nextInt();
        String target = "&rnd=" + rand;

        if (reqUri.matches(req.getContextPath() + "/wiseadm" + CORE_OPER_REGEX) && (admLocs.isEmpty() || !isAllowCoreOpertaion(admLocs))) {
            System.out.println("Permission denied. Not allowed core operation. " + admLocs);
            gotoLogin = true;
        }
        else if (!(reqUri.equalsIgnoreCase(req.getContextPath() + loginFormDispatchPath))
                && !(SearchManager.isCloudMode() && isLiveNode(remoteIp)) // NOT (Cloud Mode && Live Node)
        ) {

            if (admLocs.isEmpty() || !isMatchUrlRegex(admLocs, reqUri)) {
                gotoLogin = true;
            }
            else {
                if (reqUri.toLowerCase().startsWith(req.getContextPath().toLowerCase() + "" + WiSeEnv.getAdminContextPath() + "/qa")) {
                    Tenant t = getTenantFromRequest(req);
                    if (t == null) {
                        if(!req.getRequestURL().toString().contains("?"))
                            target = "?rnd=" + rand;
                        resp.sendRedirect(req.getContextPath() + WiSeEnv.getAdminContextPath() +"/chooseTenant.jsp?r=" + URLEncoder.encode(xssStr.resoveSplitting(req.getRequestURL().toString()) + target, "UTF-8"));
                        return;
                    }
                }
            }
        }
        else if (!(SearchManager.isCloudMode() && isLiveNode(remoteIp)) && AdminGroupFacade.getInstance().getFromSession(sess).getIndexAdminCURD() == 0) {
            gotoLogin = true;
        }

        if (gotoLogin && !req.getRequestURI().startsWith(req.getContextPath() + loginFormDispatchPath)) {
            infoLog.info("Administration access attempt: [" + remoteIp + "] " + reqUri);
            target = "?rnd=" + rand;
            req.getRequestDispatcher(loginFormDispatchPath + target).forward(req, resp);
        }
        else {
            resp.addHeader("X-Frame-Options", "sameorigin");
            super.doFilter(request, response, chain);
        }
    }

    public static void setLiveNodes(Set<String> theNodeIps) {
        liveNodeIps = theNodeIps;
    }

    private static boolean isLiveNode(String remoteIp) {
        if (liveNodeIps == null)
            return false;

        return liveNodeIps.contains(remoteIp);
    }

    private static Tenant getTenantFromRequest(HttpServletRequest request) {
        Tenant t = com.intumit.solr.tenant.Tenant.getFromSession(request.getSession());
        if (t == null && StringUtils.isNotBlank(request.getParameter("apikey"))) {
            Apikey key = Apikey.getByApiKey(request.getParameter("apikey"));

            if (key != null) {
                Tenant.setSession(request.getSession(), key.getTenant());
            }
        }
        return t;
    }

    private static AdminUser getUserFromCode(HttpSession sess, String code) {
        if (StringUtils.isNotBlank(code)) {
            String[] userInfo = DesUtil.decrypt(code).split("@@");
            if (userInfo.length == 2 && System.currentTimeMillis() - 5 * 60 * 1000 < Long.valueOf(userInfo[1])) {
                AdminUser user = AdminUserFacade.getInstance().getByLoginName(userInfo[0]);
                if (user != null) {
                    sess.setAttribute("org.apache.struts.action.LOCALE", HitHotLocale.zh_TW.getLocale());
                    Locale.setDefault(HitHotLocale.zh_TW.getLocale());
                    AdminUserFacade.getInstance().setSession(sess, user);
                }
                return user;
            }
        }
        return null;
    }

    private boolean isAllowCoreOpertaion(Set<AdminLocation> als) {
        for (AdminLocation al : als) {
            if (al.getAllowCoreOperation()) {
                return true;
            }
        }
        return false;
    }

    private boolean isMatchUrlRegex(Set<AdminLocation> als, String url) {
        for (AdminLocation al : als) {
            if (al.getUrlRegex() == null || url.matches(al.getUrlRegex())) {
                return true;
            }
        }
        return false;
    }

}