package com.intumit.solr.robot;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;

public class RobotFormalAnswersMessageListener implements MessageListener<RobotFormalAnswersEvent>{

	@Override
	public void onMessage(Message<RobotFormalAnswersEvent> message) {
		RobotFormalAnswersEvent event = message.getMessageObject();
		RobotFormalAnswers.reload(event.getTenantId());
	}

}
