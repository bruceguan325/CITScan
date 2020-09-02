package com.intumit.solr;

import org.apache.lucene.search.similarities.DefaultSimilarity;

public class OmitTermFreqSimilarity extends DefaultSimilarity {
	
	public float tf(float freq) {
		return freq > 0 ? 1.0f : 0;
	}

	public float tf(int freq) {
		return freq > 0 ? 1.0f : 0;
	}

}
