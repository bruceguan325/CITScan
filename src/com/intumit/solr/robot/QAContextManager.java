package com.intumit.solr.robot;

import org.apache.commons.lang.StringUtils;

import com.hazelcast.core.IMap;
import com.intumit.solr.servlet.HazelcastUtil;
import com.intumit.solr.tenant.Tenant;

public class QAContextManager {
	public static String generateQaId(Tenant t, QAChannel ch, String userIdentity) {
		if (ch != null) {
			switch (ch.getType()) {
				case LINE:
				case FACEBOOK_MESSENGER:
					return StringUtils.upperCase(ch.getType().name()) + ":" + t.getId() + ":" + (userIdentity != null ? userIdentity : "" + System.currentTimeMillis());
				default:
					return StringUtils.upperCase(ch.getType().name()) + ":" + t.getId() + ":" + java.util.UUID.randomUUID().toString();
			}
		}

		return "UNKNOWN:" + t.getId() + ":" + java.util.UUID.randomUUID().toString();
	}

	/*static LoadingCache<String, QAContext> contexts = CacheBuilder.newBuilder()
			.expireAfterAccess(QAUtil.QA_SESSION_TIMEOUT_MINS, TimeUnit.MINUTES)
			// .removalListener(MY_LISTENER)
			.build(new CacheLoader<String, QAContext>() {
				public QAContext load(String key) {
					return null;
				}
			});*/
	
	static IMap<String, QAContext> contexts = null;
	
	static {
		contexts = HazelcastUtil.getHzInstance().getMap("qacontext");
	}
	
	public static QAContext lookup(String qaId) {
		QAContext ctx = contexts.get(qaId); // Hazelcast
		if (ctx != null && ctx.getAccessTimestamp() != -1) {
			Tenant t = ctx.getTenant();
			long millisec = Tenant.DEFAULT_QACONTEXT_SESSION_TIMEOUT_IN_SECOND *1000;
			
			if (t != null) {
				millisec = t.getSessionExpirationSecond() * 1000;
			}
			
			if (millisec + ctx.getAccessTimestamp() < System.currentTimeMillis()) {
				ctx = null;
			}
		}
		
		return ctx;
		//return contexts.getIfPresent(qaId);
	}
	
	public static QAContext create(String qaId) {
		QAContext ctx = new QAContext();
		ctx.setContextId(qaId);
		contexts.put(qaId, ctx);
		return ctx;
	}
	
	public static void put(String qaId, QAContext ctx) {
		contexts.put(qaId, ctx);
	}
	
	public static void clear(String qaId) {
		contexts.removeAsync(qaId);
	}
}

