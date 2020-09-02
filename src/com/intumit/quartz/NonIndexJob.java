package com.intumit.quartz;

import java.util.HashMap;
import java.util.Map;

import org.quartz.StatefulJob;

abstract public class NonIndexJob implements StatefulJob {
	public static final String[] JOB_DETAIL_PROPERTIES = {"p1", "p2", "p3", "p4"};
	
	static Map<String, Class> registeredClass = new HashMap<String, Class>();

	static {
		register(NonIndexSampleJob.class);
		register(CalculateHourlySearchKeywordLogJob.class);
		register(PurgeSearchKeywordLogJob.class);
		register(BuildQAAltJob.class);
		register(AutoEvaluationJob.class);
		register(UserPushCriteriaCheckJob.class);
	}

	public static Class findRegisteredClass(String type) {
		return registeredClass.get(type);
	}
	
	public static void register(Class clazz) {
		registeredClass.put(clazz.getName(), clazz);
	}
}
