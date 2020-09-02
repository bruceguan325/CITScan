package com.intumit.solr.servlet;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;

import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.robot.EventCenter;
import com.intumit.solr.robot.WiSeReplicationSwitch;
import com.intumit.solr.searchKeywords.SearchKeywordLogFacade;
import com.intumit.solr.util.WiSeEnv;
import com.intumit.solr.util.XssHttpServletRequestWrapper;
import com.intumit.systemconfig.WiseSystemConfig;
import com.intumit.systemconfig.WiseSystemConfigFacade;

public class UpdateReplicationConfigServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		System.out.println("System config post.");
		WiseSystemConfigFacade facade = WiseSystemConfigFacade.getInstance();
		try {
			if ("update".equalsIgnoreCase(req.getParameter("action"))) {
				XssHttpServletRequestWrapper xssReq = new XssHttpServletRequestWrapper(req);
				boolean fireEvent = false;
				WiseSystemConfig config = facade.loadFromRequest(req);
				facade.update(config);

				if (config.getHotKeywordFrom() != null) {
					SearchKeywordLogFacade.getInstance()
							.setDefaultDateFrom("NOW+8HOUR/DAY-8HOUR-" + config.getHotKeywordFrom() + "DAY");
				}
				WiSeReplicationSwitch masterNode = null;
				List<WiSeReplicationSwitch> replications = WiSeReplicationSwitch.listNodes(null,
						WiSeReplicationSwitch.MASTER);
				// 先找是否存在舊的MASTER
				if (replications.size() == 1) {
					masterNode = replications.get(0);
				}
				// 新增
				String newHost = StringUtils.trimToNull(req.getParameter("replicationHost"));
				String newPort = StringUtils.trimToNull(req.getParameter("replicationPort"));
				String newStatus = StringUtils.trimToNull(req.getParameter("replicationStatus"));
				String newGroupId = StringUtils.trimToNull(req.getParameter("replicationGroupId"));
				if (newHost != null && newPort != null && newStatus != null && newStatus != null) {
					WiSeReplicationSwitch replication = new WiSeReplicationSwitch();
					replication.setHost(newHost);
					replication.setPort(newPort);
					replication.setGroupId(Integer.valueOf(newGroupId));
					replications = WiSeReplicationSwitch.listNodes(replication.getHost(), null);
					if (replications.size() == 0) {
						if (newStatus.equals(WiSeReplicationSwitch.MASTER)) {
							replication.setReplicationStauts(WiSeReplicationSwitch.MASTER);
							//原MASTER改SLAVE
							masterNode.setReplicationStauts(WiSeReplicationSwitch.SLAVE);
							WiSeReplicationSwitch.saveOrUpdate(masterNode);
						} else {
							replication.setReplicationStauts(WiSeReplicationSwitch.SLAVE);
						}
						WiSeReplicationSwitch.saveOrUpdate(replication);
						fireEvent = true;
					}
				}
				replications = WiSeReplicationSwitch.listNodes(null, null);
				for (WiSeReplicationSwitch replication : replications) {
					String status = xssReq.getParameter(replication.getHost() + "_replicationStatus");
					String groupId = xssReq.getParameter(replication.getHost() + "_replicationGroupId");
					if (status != null && status.equals(WiSeReplicationSwitch.MASTER)) {
						replication.setReplicationStauts(WiSeReplicationSwitch.MASTER);
						if (masterNode != null) {
							//原MASTER改SLAVE
							masterNode.setReplicationStauts(WiSeReplicationSwitch.SLAVE);
							WiSeReplicationSwitch.saveOrUpdate(masterNode);
						}
						WiSeReplicationSwitch.saveOrUpdate(replication);
						fireEvent = true;
					}
					if (groupId != null) {
						replication.setGroupId(Integer.valueOf(groupId));
						WiSeReplicationSwitch.saveOrUpdate(replication);
					}
				}
				if (fireEvent)
					EventCenter.fireEvent(WiSeReplicationSwitch.class.getName(), 0, "reload", null);
			} else if ("delete".equalsIgnoreCase(req.getParameter("action"))) {
				String nodeId = req.getParameter("nodeId");
				if (nodeId != null) {
					WiSeReplicationSwitch.delete(Integer.valueOf(nodeId));
				}
			}
			res.sendRedirect(req.getContextPath() + WiSeEnv.getAdminContextPath() + "/wiseReplicationConfig.jsp?msg="
					+ java.net.URLEncoder.encode(
							MessageUtil.getMessage(HitHotLocale.determineLocale(req, false, false), "already.update"),
							"UTF-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
