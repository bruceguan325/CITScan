package com.intumit.solr.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.WiSeUtils;
import com.intumit.syslog.OperationLogEntity;

/**
 *
 * Robot APLog
 * Capture APLog Events
 * @author dudamel
 *
 */

public class RobotAPLogFilter implements Filter {

	@RequestMapping(value="/action/{action}")
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest req = ((HttpServletRequest) request);
		String reqUri = req.getRequestURI();

		String action = req.getParameter("action");
        String method = req.getMethod();
		String target = reqUri.replace(req.getContextPath() + WiSeEnv.getAdminContextPath(), "");
		if (action != null && OperationLogEntity.isPathExist(target)
				&& OperationLogEntity.getActions().contains(action)) {
			String paramJson = WiSeUtils.getParameterJson(req.getParameterMap());
			OperationLogEntity log = OperationLogEntity.log(req, target, action, paramJson, null);
			req.setAttribute("opLogId", log.getId());
        } else if (action == null && OperationLogEntity.isRestPathExist(target)
                && OperationLogEntity.getRestActions().contains(method)) {
            req = new MultiReadHttpServletRequest((HttpServletRequest) request);
            String payload = WiSeUtils.getPayload(req);
            if (method.equals("POST")) {
                action = "save";
            } else if (method.equals("PUT")) {
                action = "update";
            } else if (method.equals("DELETE")) {
                action = "delete";
            }else if (method.equals("add")) {
                action = "save";
            }
            OperationLogEntity log = OperationLogEntity.log(req, target, action, payload, null);
            req.setAttribute("opLogId", log.getId());
        }
		
        chain.doFilter(req, response);
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void destroy() {

	}
}
