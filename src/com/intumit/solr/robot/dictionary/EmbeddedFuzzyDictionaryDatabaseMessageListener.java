package com.intumit.solr.robot.dictionary;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class EmbeddedFuzzyDictionaryDatabaseMessageListener implements MessageListener<EmbeddedFuzzyDictionaryDatabaseChangeEvent> {

	@Override
	public void onMessage(Message<EmbeddedFuzzyDictionaryDatabaseChangeEvent> msg) {
		EmbeddedFuzzyDictionaryDatabaseChangeEvent event = msg.getMessageObject();
		switch(event.getType()) {
			case SAVE : 
				for (EmbeddedFuzzyDictionaryDatabaseChangeListener l : EmbeddedFuzzyDictionaryDatabase.listeners) {
					l.save(event.getEfd());
				}
				break;
			case UPDATE:
				for (EmbeddedFuzzyDictionaryDatabaseChangeListener l : EmbeddedFuzzyDictionaryDatabase.listeners) {
					l.update(event.getEfd(), event.getOldEfd());
				}
				break;
			case DELETE:
				for (EmbeddedFuzzyDictionaryDatabaseChangeListener l : EmbeddedFuzzyDictionaryDatabase.listeners) {
					l.delete(event.getEfd());
				}
				break;
		}
	}

}
