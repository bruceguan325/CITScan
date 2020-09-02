package com.intumit.solr.synonymKeywords;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.dictionary.AttentionKeywordDictionary;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.DictionaryDatabase.Purpose;
import com.intumit.solr.robot.dictionary.KnowledgePointDictionary;
import com.intumit.solr.robot.dictionary.OtherPossibleQuestionsBySearchDictionary;

public class SynonymChange4DictionarySearcher implements
		SynonymKeywordChangeListener {
	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);

	@Override
	public void update(SynonymKeyword oldSyn, SynonymKeyword newSyn) {
		LOG.info("SynonymKeyword updated, update dictionary searcher [" + oldSyn + "->" + newSyn + "]");

		Integer tenantId = oldSyn.getTenantId();
		Set<String> kwAndSyns = new HashSet<String>(oldSyn.getKeywordAndSynonymList());
		kwAndSyns.addAll(newSyn.getKeywordAndSynonymList());
		
		Set<String> dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT));

		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			KnowledgePointDictionary.clear(tenantId);
			OtherPossibleQuestionsBySearchDictionary.clear(tenantId);
		}
		
		dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.BLACKLIST));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.MARKETING));
		
		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			AttentionKeywordDictionary.clear(tenantId);
		}

		dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.SEARCH));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.MARKETING));
		
		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			OtherPossibleQuestionsBySearchDictionary.clear(tenantId);
		}
	}

	@Override
	public void save(SynonymKeyword syn) {
		LOG.info("SynonymKeyword saved, update dictionary searcher [" + syn + "]");

		Integer tenantId = syn.getTenantId();
		Set<String> kwAndSyns = new HashSet<String>(syn.getKeywordAndSynonymList());
		
		Set<String> dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT));

		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			KnowledgePointDictionary.clear(tenantId);
			OtherPossibleQuestionsBySearchDictionary.clear(tenantId);
		}
		
		dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.BLACKLIST));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.MARKETING));
		
		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			AttentionKeywordDictionary.clear(tenantId);
		}

		dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.SEARCH));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.MARKETING));
		
		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			OtherPossibleQuestionsBySearchDictionary.clear(tenantId);
		}
	}

	@Override
	public void delete(SynonymKeyword syn) {
		LOG.info("SynonymKeyword deleted, update dictionary searcher [" + syn + "]");

		Integer tenantId = syn.getTenantId();
		Set<String> kwAndSyns = new HashSet<String>(syn.getKeywordAndSynonymList());
		
		Set<String> dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT));

		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			KnowledgePointDictionary.clear(tenantId);
			OtherPossibleQuestionsBySearchDictionary.clear(tenantId);
		}
		
		dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.BLACKLIST));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.MARKETING));
		
		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			AttentionKeywordDictionary.clear(tenantId);
		}

		dictSet = new HashSet<String>(DictionaryDatabase.getDictionary(tenantId, Purpose.KNOWLEDGE_POINT));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.SEARCH));
		dictSet.addAll(DictionaryDatabase.getDictionary(tenantId, Purpose.MARKETING));
		
		dictSet.retainAll(kwAndSyns);
		if (dictSet.size() > 0) {
			OtherPossibleQuestionsBySearchDictionary.clear(tenantId);
		}
	}

}
