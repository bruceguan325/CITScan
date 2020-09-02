package com.intumit.solr.robot.dictionary;

import org.apache.commons.lang.StringUtils;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.tire.domain.Value;
import org.nlpcn.commons.lang.tire.library.Library;

import com.intumit.solr.robot.QAUtil;

public class EmbeddedFuzzyDictionaryDatabaseChange4DictionarySearcherListener implements EmbeddedFuzzyDictionaryDatabaseChangeListener {

	@Override
	public void save(EmbeddedFuzzyDictionaryDatabase efd) {
		QAUtil qaUtil = QAUtil.getInstance(efd.getTenantId());
		qaUtil.getToAnalysis();
		qaUtil.addToUserDefinedForest(efd.getKeyword(), null, "UserDefined_EFD");
		System.out.println();
	}
	
	@Override
	public void update(EmbeddedFuzzyDictionaryDatabase efd, EmbeddedFuzzyDictionaryDatabase oldEfd) {
		if(StringUtils.equals(efd.getKeyword(), oldEfd.getKeyword())) return;
		
		QAUtil qaUtil = QAUtil.getInstance(efd.getTenantId());
		qaUtil.getToAnalysis(); // init
		String[] oldValue = qaUtil.guessNature(oldEfd.getKeyword());
		String[] newValue = qaUtil.guessNature(efd.getKeyword());
		Forest userDefinedForest = qaUtil.getUserDefinedForest();
		
		if (oldValue != null) {
			try {
				if (StringUtils.equals(oldValue[0], "#UserDefined_EFD")) {
					Library.removeWord(userDefinedForest, oldEfd.getKeyword());
				}
			}
			catch (Exception ignore) {ignore.printStackTrace();}
		}

		qaUtil.addToUserDefinedForest(efd.getKeyword(), null, "UserDefined_EFD");
	}

	@Override
	public void delete(EmbeddedFuzzyDictionaryDatabase efd) {
		QAUtil qaUtil = QAUtil.getInstance(efd.getTenantId());
		qaUtil.getToAnalysis(); // init
		String[] value = qaUtil.guessNature(efd.getKeyword());
		Forest userDefinedForest = qaUtil.getUserDefinedForest();
		if (value != null && StringUtils.equalsIgnoreCase(value[0], "#UserDefined_EFD")) {
			Library.removeWord(userDefinedForest, efd.getKeyword());
		}
	}

}
