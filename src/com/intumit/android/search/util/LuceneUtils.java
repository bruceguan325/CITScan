package com.intumit.android.search.util;

import org.apache.lucene.document.FieldType;

public class LuceneUtils {

	public final static FieldType storeOnlyNoIndex = new FieldType();
	public final static FieldType storeAlsoIndex = new FieldType();
	public final static FieldType sortableField = new FieldType();
	
	static {
		storeOnlyNoIndex.setIndexed(false);
		storeOnlyNoIndex.setStored(true);
		storeOnlyNoIndex.setTokenized(false);
		
		storeAlsoIndex.setIndexed(true);
		storeAlsoIndex.setStored(true);
		storeAlsoIndex.setTokenized(true);
		
		sortableField.setIndexed(true);
		sortableField.setStored(false);
		sortableField.setTokenized(false);
	}
}
