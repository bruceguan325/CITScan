package com.intumit.android.search.fuzzy;

public enum PhoneticSimilarity {
	MOST_STRICT(new MostStrictPhoneticMap(), new MostStrictPhoneticMap()),
	STRICT(new StrictPhoneticMap(), new StrictPhoneticMap()),
	NORMAL(new DefaultPhoneticMap(), new StrictPhoneticMap()),
	FLEXIBLE(new FlexiblePhoneticMap(), new DefaultPhoneticMap()),
	MOST_FLEXIBLE(new FlexiblePhoneticMap(), new FlexiblePhoneticMap()),
	NUMERIC(new NumberPhoneticMap(), new NumberPhoneticMap()),
	;

	private PhoneticMap defaultMap;
	private PhoneticMap bigramMap;

    private PhoneticSimilarity(PhoneticMap ngram, PhoneticMap bigram) {
    	defaultMap = ngram;
    	bigramMap = bigram;
    }
    
    protected PhoneticMap getDefaultMap() {
    	return defaultMap;
    }
    
    protected PhoneticMap getBigramMap() {
    	return bigramMap;
    }
}
