package com.intumit.smartwiki.recommend;

public class Percent {

	private static final ThreadLocal<Integer> threadLocal = new ThreadLocal<Integer>();

	public static int getHighLightPercent() {
		if (threadLocal.get() == null)
			return 0;
		else
			return (Integer)threadLocal.get();
	}

	public static void setHighLightPercent(int highLightPercent) {
		threadLocal.set(highLightPercent);
	}
}
