package com.intumit.solr.synonymKeywords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import com.intumit.solr.synonymKeywords.SynonymKeywordChangeEvent;

public class SynonymKeywordMessageListener implements
		MessageListener<SynonymKeywordChangeEvent> {

	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);

	public SynonymKeywordMessageListener() {
		super();
	}

	@Override
	public void onMessage(Message<SynonymKeywordChangeEvent> message) {
		SynonymKeywordChangeEvent event = message.getMessageObject();
		
		LOG.info("Got Hz Message [SynonymKeywordChangeEvent]:" + event);

		switch (event.type) {
		case SAVE:
			for (SynonymKeywordChangeListener l: SynonymKeywordFacade.getInstance().listeners) {
				l.save(event.newOne);
			}
			break;
		case UPDATE:
			for (SynonymKeywordChangeListener l: SynonymKeywordFacade.getInstance().listeners) {
				l.update(event.oldOne, event.newOne);
			}
			break;
		case DELETE:
			for (SynonymKeywordChangeListener l: SynonymKeywordFacade.getInstance().listeners) {
				l.delete(event.oldOne);
			}
			break;
		}
	}
}
