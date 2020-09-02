package com.intumit.android.search;

public enum FuzzyLevel {
	HIGH(0.4D),		// 40%
	DEFAULT(0.6D),	// 60%
	LOW(0.8D),		// 80%
	NOFUZZY(1),		// 100%
	;
	
	private double level;

    private FuzzyLevel(double level) {
    	this.level = level;
    }
    
    public double getLevel() {
    	return level;
    }
    
    public static FuzzyLevel findByLevel(double level) {
    	for (FuzzyLevel fl: FuzzyLevel.values()) {
    		if (fl.getLevel() == level) {
    			return fl;
    		}
    	}
    	
    	return null;
    }
}
