package com.intumit.solr.robot.dictionary;

import java.util.Set;

import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.hithot.HitHotLocale;
import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase.Purpose;
import com.intumit.solr.tenant.Tenant;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;

public class DictionaryDatabaseChange4DictionarySearcherListener implements
DictionaryDatabaseChangeListener {

	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);
	
	@Override
	public void update(DictionaryDatabase oldWord, DictionaryDatabase newWord) {
		// 處理 oldWord
		delete(oldWord);
		// 處理 newWord
		save(newWord);
		
		LOG.info("DictionaryDatabase updated, reload cache [" + oldWord + " -> " + newWord + "]");
	}

	@Override
	public void save(DictionaryDatabase word) {
		// 處理 KnowledgePointDictionary
		if (word.getPurposeSet().contains(Purpose.KNOWLEDGE_POINT)) {
			KnowledgePointDictionary.clear(word.getTenantId());

			Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
			if (word.isEnabled() && !whiteSet.contains(word.getKeyword())) {
				QAUtil.getInstance(word.getTenantId()).getUserDefinedForest().addBranch(word.getKeyword(), QAUtil.toUserDefinedValue(word.getKeyword(), "#UserDefined_KP", KnowledgePointDictionary.DEFAULT_SEG_SCORE));
				
				if (Tenant.get(word.getTenantId()).getLocale() == HitHotLocale.ja_JP) {
				QAUtil.getInstance(word.getTenantId()).addKuromojiUserDictionary(word.getKeyword(), "UserDefined_KP");
				QAUtil.getInstance(word.getTenantId()).reloadKuromojiUserDictionary();
			}
		}
		}
		// 是否要 reload AttentionKeywordDictionary
		if (word.getPurposeSet().contains(Purpose.BLACKLIST) || word.getPurposeSet().contains(Purpose.MARKETING)) {
			AttentionKeywordDictionary.clear(word.getTenantId());
		}
		// 是否要 reload OtherPossibleQuestionsBySearchDictionary
		if (word.getPurposeSet().contains(Purpose.KNOWLEDGE_POINT) 
				|| word.getPurposeSet().contains(Purpose.SEARCH)
				|| word.getPurposeSet().contains(Purpose.MARKETING)
				) {
			OtherPossibleQuestionsBySearchDictionary.clear(word.getTenantId());
		}
		LOG.info("DictionaryDatabase save, reload cache [" + word + "]");
	}

	@Override
	public void delete(DictionaryDatabase word) {
		// 處理 KnowledgePointDictionary
		if (word.getPurposeSet().contains(Purpose.KNOWLEDGE_POINT)) {
			KnowledgePointDictionary.clear(word.getTenantId());

			Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
			if (word.isEnabled() && !whiteSet.contains(word.getKeyword())) {
				QAUtil.getInstance(word.getTenantId()).forceRebuildUserDefiniedDictionary();
			}
		}
		// 是否要 reload AttentionKeywordDictionary
		if (word.getPurposeSet().contains(Purpose.BLACKLIST) || word.getPurposeSet().contains(Purpose.MARKETING)) {
			AttentionKeywordDictionary.clear(word.getTenantId());
		}
		// 是否要 reload OtherPossibleQuestionsBySearchDictionary
		if (word.getPurposeSet().contains(Purpose.KNOWLEDGE_POINT) 
				|| word.getPurposeSet().contains(Purpose.SEARCH)
				|| word.getPurposeSet().contains(Purpose.MARKETING)
				) {
			OtherPossibleQuestionsBySearchDictionary.clear(word.getTenantId());
		}
		LOG.info("DictionaryDatabase delete, reload cache [" + word + "]");
	}

}
