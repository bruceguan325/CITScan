package com.intumit.solr.robot.dictionary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class DictionaryDatabaseMessageListener implements
		MessageListener<DictionaryDatabaseChangeEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);

	public DictionaryDatabaseMessageListener() {
		super();
	}

	@Override
	public void onMessage(Message<DictionaryDatabaseChangeEvent> message) {
		DictionaryDatabaseChangeEvent event = message.getMessageObject();
		
		LOG.info("Got Hz Message [DictionaryDatabaseChangeEvent]:" + event);

		switch (event.type) {
		case SAVE:
			for (DictionaryDatabaseChangeListener l: DictionaryDatabase.listeners) {
				l.save(event.newOne);
			}
			break;
		case UPDATE:
			for (DictionaryDatabaseChangeListener l: DictionaryDatabase.listeners) {
				l.update(event.oldOne, event.newOne);
			}
			break;
		case DELETE:
			for (DictionaryDatabaseChangeListener l: DictionaryDatabase.listeners) {
				l.delete(event.oldOne);
			}
			break;
		}
	}
}
