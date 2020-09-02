package com.intumit.smartwiki.model;

import java.util.Enumeration;
import java.util.Hashtable;

import com.intumit.hithot.HitHotLocale;
import com.intumit.smartwiki.recommend.WikiWord;
import com.intumit.smartwiki.util.Constants;

public class WikiWordtable extends Hashtable{
	HitHotLocale locale;
	

	public WikiWordtable(HitHotLocale locale) {
		super();
		this.locale = locale;
	}

//	public Object synonymousPut(Object key, Object value)
//	{
//		WikiWord word = (WikiWord)value;
//		int keywordId = word.getKeywordId();
//
//		Collection values = this.values();
//		for (Iterator iter = values.iterator(); iter.hasNext();) {
//			WikiWord element = (WikiWord) iter.next();
//			if(element.getKeywordId() == keywordId)
//			{
//				element.setFrequency(element.getFrequency()+ 1);
//				word.setFrequency(element.getFrequency());
//			}
//
//		}
//		return super.put(key, word);
//	}

	public WikiWord getOverLap(WikiWord newWiki) {
		Enumeration<String> titles = this.keys();

		while(titles.hasMoreElements())
		{
			String page_title = (String)titles.nextElement();
			WikiWord wiki = (WikiWord)this.get(page_title);
			int wikiLen = wiki.getWordLen(locale);
			int newWikiLen = newWiki.getWordLen(locale);
			if( newWiki.getFirstIndex() == wiki.getFirstIndex() ||
				newWiki.getFirstIndex() + newWikiLen == wiki.getFirstIndex() + wikiLen ||
				(newWiki.getFirstIndex() < wiki.getFirstIndex() && newWiki.getFirstIndex()+newWikiLen > wiki.getFirstIndex()) ||
				(wiki.getFirstIndex() < newWiki.getFirstIndex() && wiki.getFirstIndex() + wikiLen > newWiki.getFirstIndex())
			)
			{
				return wiki;
			}
		}
		return null;
	}

	public void replaceOverLap(WikiWord overLap, WikiWord newWiki) {
		this.remove(overLap.getPageTitle());
		this.put(newWiki.getPageTitle(), newWiki);
	}

	public WikiWordtable filterStopWord() {

		String[] EnglishStopWords = Constants.EnglishStopWords;

		Enumeration<String> titles = this.keys();

		while(titles.hasMoreElements())
		{
			String page_title = (String)titles.nextElement();

			WikiWord word = (WikiWord)this.get(page_title);
			if(word.getWordLen(locale) == 2)
			{
				String[] prefixWord = {"A_","The_"};
				for(String prefix: prefixWord)
				{
					if(word.getPageTitle().startsWith(prefix))
					{
						this.remove(page_title);
						break;
					}
				}
			}
			for(int i = 0; i < EnglishStopWords.length; i++)
			{
				if(page_title.equalsIgnoreCase(EnglishStopWords[i]))
				{
					this.remove(page_title);
					break;
				}
			}
		}

		return this;
	}

	public int getMaxFrequency() {
		String parent;
		int maxFrequency = 0;
		Enumeration enumeration = this.keys();
        while (enumeration.hasMoreElements()) {
        	parent = (String)enumeration.nextElement();
        	int temp = ((WikiWord)this.get(parent)).getFrequency();
        	if(temp > maxFrequency){
        		maxFrequency = temp;
        	}
        }
        return maxFrequency;
	}

	public WikiWordList toList() {
		WikiWordList result = new WikiWordList();
		result.addAll(this.values());
		return result;
	}

//	/**
//	 * 把所有同義字的頻率都加一
//	 * @param keywordId 正式用語的 keywordId
//	 */
//	public void updateKeywordFrequency(int keywordId) {
//		Collection keywords = this.values();
//		for (Iterator iter = keywords.iterator(); iter.hasNext();) {
//			WikiWord element = (WikiWord) iter.next();
//			if(element.getKeywordId() == keywordId)
//				element.setFrequency(element.getFrequency()+1);
//		}
//
//	}
}
