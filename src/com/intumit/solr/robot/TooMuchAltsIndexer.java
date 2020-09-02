package com.intumit.solr.robot;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Set;

import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;

public class TooMuchAltsIndexer {
	SolrServer server;
	String docId;
	long kid;
	QA qa;
	Collection<String> altTPL;
	
	int startWithPartNumber = 1;

	public TooMuchAltsIndexer(SolrServer server, String docId, long kid,
			QA docBase) {
		super();
		this.server = server;
		this.docId = docId;
		this.kid = kid;
		this.qa = docBase;
	}
	
	public boolean continueSaveQA(Collection<String> partOfAlts) {
		try {
			qa.setAltRebuildDate(Calendar.getInstance().getTime());
			qa.setAltRebuildTimeCost(-1);
			qa.setAltCount(partOfAlts.size());
			qa.setField("AltCount_i", partOfAlts.size());
			qa.setDataType(QAUtil.DATATYPE_COMMON_SENSE);
			
			int parts = QASaver.indexQA(server, docId, kid, qa, partOfAlts, startWithPartNumber);
			startWithPartNumber += parts;
			server.commit(true, true, false);
			
			try {
				// wait for softCommit
				Thread.sleep(1000);
			} catch (InterruptedException ignore) {
			} 
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public void cleanRestOfParts() {
		try {
			server.deleteByQuery("+kid_l:" + kid + " +isPart_i:[" + (startWithPartNumber+1) + " TO *]");
			server.commit(true, true, false);
			
			try {
				// wait for softCommit
				Thread.sleep(1000);
			} catch (InterruptedException ignore) {
			} 
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
