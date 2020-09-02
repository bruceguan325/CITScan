package com.intumit.solr.synonymKeywords;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intumit.solr.robot.QAUtil;
import com.intumit.solr.robot.dictionary.DictionaryDatabase;
import com.intumit.solr.robot.dictionary.DictionaryDatabase.Purpose;
import com.intumit.solr.whiteKeywords.WhiteWikiWordFacade;

public class SynonymChange4UserDefinedDictionary implements
		SynonymKeywordChangeListener {
	private static final Logger LOG = LoggerFactory.getLogger(com.intumit.solr.robot.QA.class);

	@Override
	public void update(SynonymKeyword oldSyn, SynonymKeyword newSyn) {
		LOG.info("SynonymKeyword updated, update dictionary forest [" + oldSyn + "->" + newSyn + "]");

		Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
		whiteSet.addAll(DictionaryDatabase.getDictionary(oldSyn.getTenantId(), Purpose.KNOWLEDGE_POINT));
		QAUtil qautil = QAUtil.getInstance(oldSyn.getTenantId());
		SynonymKeywordFacade skf = SynonymKeywordFacade.getInstance();

		for (String synStr : oldSyn.getKeywordAndSynonymList()) {
			if (!whiteSet.contains(synStr)) {
				List<SynonymKeyword> stillBeUsedByTheOtherSyns = skf.listByQuery(oldSyn.getTenantId(), synStr);
				
				// 如果仍然有在用某個詞的同義詞就是要被更新的那一個，那當然就要刪除
				if (stillBeUsedByTheOtherSyns != null && stillBeUsedByTheOtherSyns.size() == 1) {
					SynonymKeyword stillOne = stillBeUsedByTheOtherSyns.get(0);
					if (stillOne.getId().equals(oldSyn.getId())) {
						stillBeUsedByTheOtherSyns = new ArrayList<>();
					}
				}
				if (stillBeUsedByTheOtherSyns != null && stillBeUsedByTheOtherSyns.size() > 0) {
					whiteSet.add(synStr);
					System.out.println("Refuse Remove Synonym Keyword:" + synStr + ", it still be used in the other " + stillBeUsedByTheOtherSyns.size() + " synonyms.");
					continue;
				}
				
				String[] values = qautil.guessNature(synStr);
				//System.out.println("Remove Synonym Keyword:" + synStr + "/" + Arrays.asList(values));
				Library.removeWord(qautil.getUserDefinedForest(), synStr);
				qautil.deleteKuromojiUserDictionary(synStr);
			}
		}

		for (String synStr : newSyn.getKeywordAndSynonymList()) {
			if (!whiteSet.contains(synStr)) {
				whiteSet.add(synStr);
				qautil.addToUserDefinedForest(synStr, newSyn.getNature(), "UserDefined_SS_SAVE");
			}
		}
		System.out.println("Synonym Keyword:" + newSyn.getKeyword() + " loaded.");
		qautil.reloadKuromojiUserDictionary();
	}

	@Override
	public void save(SynonymKeyword syn) {
		LOG.info("SynonymKeyword saved, update dictionary forest [" + syn + "]");
		
		Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
		whiteSet.addAll(DictionaryDatabase.getDictionary(syn.getTenantId(), Purpose.KNOWLEDGE_POINT));
		
		QAUtil qautil = QAUtil.getInstance(syn.getTenantId());
		
		for (String synStr : syn.getKeywordAndSynonymList()) {
			if (!whiteSet.contains(synStr)) {
				whiteSet.add(synStr);
				qautil.addToUserDefinedForest(synStr, syn.getNature(), "UserDefined_SS_SAVE");
			}
		}
		qautil.reloadKuromojiUserDictionary();
	}

	@Override
	public void delete(SynonymKeyword syn) {
		LOG.info("SynonymKeyword deleted, update dictionary forest [" + syn + "]");
		
		Set<String> whiteSet = WhiteWikiWordFacade.getInstance().whiteSet();
		whiteSet.addAll(DictionaryDatabase.getDictionary(syn.getTenantId(), Purpose.KNOWLEDGE_POINT));

		SynonymKeywordFacade skf = SynonymKeywordFacade.getInstance();
		QAUtil qautil = QAUtil.getInstance(syn.getTenantId());
		for (String synStr : syn.getKeywordAndSynonymList()) {
			if (!whiteSet.contains(synStr)) {
				whiteSet.add(synStr);

				List<SynonymKeyword> stillBeUsedByTheOtherSyns = skf.listByQuery(syn.getTenantId(), synStr);
				if (stillBeUsedByTheOtherSyns != null && stillBeUsedByTheOtherSyns.size() > 0) {
					System.out.println("Refuse Remove Synonym Keyword:" + synStr + ", it still be used in the other " + stillBeUsedByTheOtherSyns.size() + " synonyms.");
					continue;
				}
				
				String[] values = qautil.guessNature(synStr);
				System.out.println("Remove Synonym Keyword:" + synStr + "/" + Arrays.asList(values));
				Library.removeWord(qautil.getUserDefinedForest(), synStr);
				qautil.deleteKuromojiUserDictionary(synStr);
			}
		}

		qautil.reloadKuromojiUserDictionary();
	};
}
