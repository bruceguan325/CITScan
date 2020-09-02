/**
 * 
 */
package com.intumit.solr.util.chinesetranslate;

import java.util.Comparator;
import java.util.TreeMap;

public class StringLengthOrderMap extends TreeMap<String, String> {

	private static final long serialVersionUID = 1226952209641403759L;

	public StringLengthOrderMap() {
		super(new Comparator<String>() {

			public int compare(String o1, String o2) {
				int c = o2.length() - o1.length();
				if (c != 0)
					return c;
				return o2.compareTo(o1);
			}
		});
	}

}
