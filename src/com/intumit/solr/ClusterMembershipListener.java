package com.intumit.solr;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.ReplicationHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.LifecycleEvent;
import com.hazelcast.core.LifecycleListener;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import com.intumit.solr.robot.WiSeReplicationSwitch;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.util.WiSeReplicationHandler;
import com.intumit.solr.util.WiSeSnapPuller;

public class ClusterMembershipListener implements MembershipListener, LifecycleListener {
	private static final Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);
	
	static ClusterMembershipListener instance = null;
	
	public static ClusterMembershipListener getInstance() {
		if (instance == null) {
			synchronized (ClusterMembershipListener.class) {
				if (instance == null) {
					instance = new ClusterMembershipListener();
				}
			}
		}
		
		return instance;
	}
	
	
	ClusterMembershipListener() {
		super();
		checkAndSwitchMode();
	}

	public void memberAdded(MembershipEvent membershipEvent) {
		LOG.info("Added: " + membershipEvent);
		checkAndSwitchMode();
	}

	public void memberRemoved(MembershipEvent membershipEvent) {
		LOG.info("Removed: " + membershipEvent);
		checkAndSwitchMode();
	}

	public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
		LOG.info("Member attribute changed: " + memberAttributeEvent);
	}


	@Override
	public void stateChanged(LifecycleEvent arg0) {
		LOG.info("stateChanged: " + arg0);
		checkAndSwitchMode();
	}

	public boolean isLeader() {

		// The first member in the set is the
		// oldest member, this is the one that
		// we always will point out as leader
		Member oldestMember = HazelcastUtil.getHzInstance().getCluster()
				.getMembers().iterator().next();

		return oldestMember.localMember();
	}
	
	Boolean isLeaderNow = null;
	
	public Boolean isLeaderNow() {
		if (isLeaderNow == null)
			checkAndSwitchMode();
		return isLeaderNow;
	}

	public void setLeaderNow(boolean isLeaderNow) {
		this.isLeaderNow = isLeaderNow;
	}


	public boolean checkAndSwitchMode() {
		if (SearchManager.isInitialized()) {
			List<WiSeReplicationSwitch> nodes = new ArrayList<WiSeReplicationSwitch>();
			try {
				nodes = WiSeReplicationSwitch.listNodes(InetAddress.getLocalHost().getHostName(), null);
				LOG.info("HostName : " + InetAddress.getLocalHost().getHostName());
			} catch (UnknownHostException e) {
				LOG.error(e.toString());
			}
			String masterHost = WiSeSnapPuller.getMasterHost();
			String masterPort = WiSeSnapPuller.getMasterPort();
			if (nodes.size() == 1 && masterHost != null && masterPort != null) {
				System.out.println(" MASTER is : " + masterHost + ":"+ masterPort);
				if (nodes.get(0).getReplicationStauts().endsWith(WiSeReplicationSwitch.MASTER)) {

					LOG.info("I'm WiSeReplicationSwitch Master, checking all LB-enabled cores...");

					switchingToMaster(masterHost + ":"+ masterPort);
					
					isLeaderNow = true;
					
				} else {
					LOG.info("I'm WiSeReplicationSwitch Slave now, checking all LB-enabled cores...");
					
					switchingToSlave(masterHost + ":"+ masterPort);
					
					isLeaderNow = false;
				}

			} else {
				if (isLeader()) {
					if (false) {//isLeaderNow != null && isLeaderNow) {
						LOG.info("I'm already leader, just sit and wait....");
					}
					else {
						LOG.info("I'm leader now, checking all LB-enabled cores...");
						
						switchingToMaster(null);
						
						isLeaderNow = true;
					}
				}
				else {
					if (true) {//isLeaderNow == null || isLeaderNow) {
						LOG.info("I'm slave now, checking all LB-enabled cores...");
						
						switchingToSlave(null);
						
						isLeaderNow = false;
					}
					else {
						LOG.info("I'm aleady a slave...");
					}
				}
			}
		}
		else {
			LOG.info("Solr is still not initialized, skip cluster checking.");
		}
		
		return true;
	}
	
	private void switchingToMaster(String masterHostPort) {
		CoreContainer cc = SearchManager.getLocalCores();

		for (String shardCoreName : cc.getAllCoreNames()) {
			SolrCore core = cc.getCore(shardCoreName);
			ReplicationHandler rh = (ReplicationHandler) core.getRequestHandlers().get("/replication");

			try {
				NamedList masterNL = (NamedList) rh.getInitArgs().get("master");
				NamedList slaveNL = (NamedList) rh.getInitArgs().get("slave");

				if (masterNL == null || slaveNL == null) {
					LOG.info("Core[" + core.getName() + "] is not a LB-enabled core, skip it.");
					continue;
				}

				String masterUrl = (String) slaveNL.get("masterUrl");
				if (masterHostPort != null) {
					masterUrl = masterUrl.replace("THE-OTHER-ONE:8080", masterHostPort);
				}
				SearchManager.setCoreUrl4Write(shardCoreName, masterUrl);
				SolrServer server = SearchManager.getServer(shardCoreName);
				ModifiableSolrParams params = new ModifiableSolrParams();
				params.set("qt", "/replication");
				params.set("command", "disablepoll");
				server.query(params);

				params = new ModifiableSolrParams();
				params.set("qt", "/replication");
				params.set("command", "enablereplication");
				server.query(params);

			} catch (Exception e) {
				LOG.debug("Switching to master mode failed", e);
			}
		}
	}
	
	private void switchingToSlave(String masterHostPort) {
		CoreContainer cc = SearchManager.getLocalCores();
		
		for (String shardCoreName : cc.getAllCoreNames()) {
			SolrCore core = cc.getCore(shardCoreName);
			ReplicationHandler rh = (ReplicationHandler) core.getRequestHandlers().get("/replication");
			
			try {
				NamedList masterNL = (NamedList) rh.getInitArgs().get("master");
				NamedList slaveNL = (NamedList) rh.getInitArgs().get("slave");
				
				if (masterNL == null || slaveNL == null) {
					LOG.info("Core[" + core.getName() + "] is not a LB-enabled core, skip it.");
					continue;
				}
				
				String masterUrl = (String)slaveNL.get("masterUrl");
				if (masterHostPort != null) {
					masterUrl = masterUrl.replace("THE-OTHER-ONE:8080", masterHostPort);
				}
				SearchManager.setCoreUrl4Write(shardCoreName, masterUrl);
				
				// Disable replication 
				SolrServer server = SearchManager.getServer(shardCoreName);
				ModifiableSolrParams params = new ModifiableSolrParams();
				params.set("qt", "/replication");
				params.set("command", "disablereplication");
				server.query(params);
				
				params = new ModifiableSolrParams();
				params.set("qt", "/replication");
				params.set("command", "enablepoll");
				server.query(params);
			} catch (Exception e) {
				LOG.debug("Switching to slave mode failed", e);
			}
		}
	}

	/*public String getReplicationMasterUrl() {
		System.out.println("solrconfig [" + shardCoreName + "] / :"
				+ core.getRequestHandlers().get("/replication"));
		org.apache.solr.handler.ReplicationHandler rh = (org.apache.solr.handler.ReplicationHandler) core
				.getRequestHandlers().get("/replication");
		System.out.println(rh.getInitArgs());
		try {
			System.out.println(((org.apache.solr.common.util.NamedList) rh
					.getInitArgs().get("slave")).get("masterUrl"));
		} catch (Exception ignore) {
		}
	}*/
}
