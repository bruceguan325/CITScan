package com.intumit.solr.robot;

public class TemperalAndNumberStrategy {
	public static TemperalAndNumberStrategy PREBUILD_STRATEGY_SEGMENT_1 =
			new TemperalAndNumberStrategy(false, true, false, false, true, false);
	
	public static TemperalAndNumberStrategy PREBUILD_STRATEGY_SEGMENT_2 =
			new TemperalAndNumberStrategy(true, true, true, true, true, true);
	
	public static TemperalAndNumberStrategy PREBUILD_STRATEGY_DONT_CHANGE_ANYTHING =
			new TemperalAndNumberStrategy(true, false, false, true, false, false);
	
	public boolean keepOriginalTemperalStr = true;
	public boolean addSymbolToMatchAllTemperal = true;
	public boolean markAllTemperalStrIgnorable = true;
	
	public boolean keepOriginalNumericStr = true;
	public boolean addSymbolToMatchAllNumber = true;
	public boolean markAllNumericStrIgnorable = true;
	
	public TemperalAndNumberStrategy(boolean keepOriginalTemperalStr, boolean addSymbolToMatchAllTemperal, boolean markAllTemperalStrIgnorable,
			boolean keepOriginalNumericStr, boolean addSymbolToMatchAllNumber, boolean markAllNumericStrIgnorable) {
		super();
		this.keepOriginalTemperalStr = keepOriginalTemperalStr;
		this.addSymbolToMatchAllTemperal = addSymbolToMatchAllTemperal;
		this.markAllTemperalStrIgnorable = markAllTemperalStrIgnorable;
		this.keepOriginalNumericStr = keepOriginalNumericStr;
		this.addSymbolToMatchAllNumber = addSymbolToMatchAllNumber;
		this.markAllNumericStrIgnorable = markAllNumericStrIgnorable;
	}
	
	
}
