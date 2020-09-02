package com.intumit.android.search.fuzzy;

import java.util.Collection;
import java.util.Iterator;

public class CollectionImportIterator implements ImportIterator {
	Iterator<String> collItr = null;
	

	public CollectionImportIterator(Collection<String> coll) {
		super();
		this.collItr = coll.iterator();
	}

	@Override
	public String next() {
		if (collItr.hasNext()) {
			return collItr.next();
		}
		return null;
	}

	@Override
	public void close() {
		// Nothing to do
	}

}
