package com.intumit.solr.servlet;

import java.net.UnknownHostException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.Config;
import com.hazelcast.config.ListenerConfig;
import com.hazelcast.config.MemberAttributeConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ITopic;
import com.hazelcast.core.Member;
import com.intumit.solr.ClusterMembershipListener;
import com.intumit.solr.NotificationMessageListener;
import com.intumit.solr.SystemMessageListener;
import com.intumit.solr.synonymKeywords.SynonymKeywordMessageListener;

public class HazelcastUtil {

	private static final Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);

	static HazelcastInstance hcz = null;
	static {
		init();
	}
	
	public static Logger log() {
		return LOG;
	}
	
	public static void init() {
		if (hcz == null) {
			synchronized (HazelcastUtil.class) {
				if (hcz == null) {
					MemberAttributeConfig mcfg = new MemberAttributeConfig();
					mcfg.setStringAttribute( "INSTANCE_TAG", "SmartRobot" );
					try {
						String hostName = java.net.InetAddress.getLocalHost().getHostName();
						mcfg.setStringAttribute( "HOSTNAME", hostName );
						
					} catch (UnknownHostException e) {
						e.printStackTrace();
						mcfg.setStringAttribute( "HOSTNAME", "Unknown: " + e.getMessage() );
					}
					
					hcz = Hazelcast.newHazelcastInstance();
					hcz.getConfig().setMemberAttributeConfig( mcfg );
					hcz.getCluster().addMembershipListener(ClusterMembershipListener.getInstance());
					hcz.getLifecycleService().addLifecycleListener(ClusterMembershipListener.getInstance());
					
					// Initialize system-wide listeners
					ITopic topic = HazelcastUtil.getTopic( "system-event" );
					topic.addMessageListener(new SystemMessageListener());
					
					topic = HazelcastUtil.getTopic( "system-notification" );
					topic.addMessageListener(new NotificationMessageListener());
				}
			}
		}
	}
	
	public static void shutdown() {
		if (hcz != null) {
			hcz.shutdown();
		}
	}
	
	public static HazelcastInstance getHzInstance() {
		if (LOG.isDebugEnabled()) {
			Set<Member> members = hcz.getCluster().getMembers();
			for (Member member: members) {
				LOG.info("Hazelcast members info dump (getHzInstance):" + member.toString());
				LOG.info("		UUID:" + member.getUuid());
				LOG.info("		ATTRIBUTES:" + member.getAttributes());
			}
		}
		return hcz;
	}
	
	public static ITopic getTopic(String topicName) {
		if (LOG.isDebugEnabled()) {
			Set<Member> members = hcz.getCluster().getMembers();
			for (Member member: members) {
				LOG.info("Hazelcast members info dump (getTopic:" + topicName + "):" + member.toString());
				LOG.info("		UUID:" + member.getUuid());
				LOG.info("		ATTRIBUTES:" + member.getAttributes());
			}
		}
		return hcz.getTopic(topicName);
	}
}
