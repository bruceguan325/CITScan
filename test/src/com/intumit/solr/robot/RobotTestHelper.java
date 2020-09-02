package com.intumit.solr.robot;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.intumit.hithot.HitHotLocale;
import com.intumit.message.MessageUtil;
import com.intumit.solr.tenant.Tenant;

public class RobotTestHelper {
	public static class MyClassLoader extends ClassLoader {
		@Override
		public URL getResource(String name) {
			return MyClassLoader.class.getResource(name);
		}
	}

	public static QAContext createContext() {
		String qaId = java.util.UUID.randomUUID().toString();
		QAContext ctx = QAContextManager.create(qaId);
		ctx.setTenant(new DummyTenant());

		return ctx;
	}

	public static <T> Set<T> asSet(T... args) {
		return new HashSet<>(Arrays.asList(args));
	}

	public static void initMessageUtil() {
		/*
		 * 這個是 struts 的 MessageResource 在 getResource 時會抓
		 * Thread.currentThread().getContextClassLoader() 在 JUnit 當中拿到的 classLoader
		 * 無法正確抓取到 ResourceBundle 因此用一個自訂的 ClassLoader 來硬解
		 */
		Thread.currentThread().setContextClassLoader(new MyClassLoader());
		MessageUtil.initialize();
	}

	public static Tenant getAndSaveTenantForTest(HitHotLocale locale) {
		Tenant tmpTenant = getTenantForTest(locale);
		Tenant.saveOrUpdate(tmpTenant);
		return tmpTenant;
	}

	public static Tenant getTenantForTest(HitHotLocale locale) {
		Tenant tmpTenant = new Tenant();
		tmpTenant.setLocale(locale);
		tmpTenant.setName("UnitTest tenant " + System.currentTimeMillis());
		tmpTenant.setNotes("" + System.currentTimeMillis());
		return tmpTenant;
	}

	public static void tearDown(Tenant tenant) {
		try {
			Tenant.delete(tenant);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
