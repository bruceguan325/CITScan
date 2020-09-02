package com.intumit.android.search.fuzzy;

import java.util.Enumeration;
import java.util.Hashtable;

public class WikiWordtable extends Hashtable{

	public WikiWord getOverLap(WikiWord newWiki) {
		Enumeration<String> titles = this.keys();

		while(titles.hasMoreElements())
		{
			String page_title = (String)titles.nextElement();
			WikiWord wiki = (WikiWord)this.get(page_title);
			int wikiLen = wiki.getWordLen();
			int newWikiLen = newWiki.getWordLen();
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
}
