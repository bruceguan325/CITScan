package com.intumit.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class SystemMessageListener implements MessageListener<SystemEvent> {
	private static final Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);

	@Override
	public void onMessage(Message<SystemEvent> message) {
		SystemEvent event = message.getMessageObject();
		
		LOG.info("Got Hz Message [SystemEvent]:" + event);
		
	}

}
