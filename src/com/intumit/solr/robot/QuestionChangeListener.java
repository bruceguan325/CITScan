package com.intumit.solr.robot;

import java.io.Serializable;

public interface QuestionChangeListener extends Serializable {
	public void listenerAdded(QAContext ctx, String currQuestion);
	public void changed(QAContext ctx, String orig, String newQuestion);
}
