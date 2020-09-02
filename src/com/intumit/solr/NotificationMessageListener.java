package com.intumit.solr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.intumit.solr.robot.NotificationServiceServlet;

public class NotificationMessageListener implements MessageListener<NotificationEvent> {
	private static final Logger LOG = LoggerFactory.getLogger(com.hazelcast.core.Hazelcast.class);

	@Override
	public void onMessage(Message<NotificationEvent> message) {
		NotificationEvent event = message.getMessageObject();
		
		LOG.info("Got Hz Message [NotificationEvent]:" + event);
		NotificationServiceServlet.notifyEvent(event);
	}

}
