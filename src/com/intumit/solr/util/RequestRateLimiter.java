package com.intumit.solr.util;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.RateLimiter;
import com.intumit.solr.tenant.Tenant;

public class RequestRateLimiter {
	private static final Logger logger = LoggerFactory.getLogger(RequestRateLimiter.class);
	private static final int MAX_ENTRIES = 4096;
	private static final double DEFAULT_RATE = 512.0;
	private static final String DEFAULT_ENDPOINT_KEY = "magic_key_endpoint";

	/**
	 * Restricts endpoint(s) below the default rate.
	 */
	private static final Map<String, Double> ENDPOINT_RATE_MAP = new ImmutableMap.Builder<String, Double>().build();
	private static RequestRateLimiter instance = null;

	private final LoadingCache<CacheKey, RateLimiter> rateLimits = CacheBuilder.newBuilder()
			.maximumSize(MAX_ENTRIES)
			.build(new Loader());

	private static final class CacheKey {
		public final String ip;
		public final Tenant tenant;
		public final String endpoint;

		public CacheKey(String ip, Tenant tenant, String endpoint) {
			this.ip = Objects.requireNonNull(ip);
			this.tenant = tenant;
			this.endpoint = Objects.requireNonNull(endpoint);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
			result = prime * result + ((ip == null) ? 0 : ip.hashCode());
			result = prime * result + ((tenant == null) ? 0 : tenant.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			CacheKey other = (CacheKey) obj;
			if (endpoint == null) {
				if (other.endpoint != null) return false;
			}
			else if (!endpoint.equals(other.endpoint)) return false;
			if (ip == null) {
				if (other.ip != null) return false;
			}
			else if (!ip.equals(other.ip)) return false;
			if (tenant == null) {
				if (other.tenant != null) return false;
			}
			else if (!tenant.equals(other.tenant)) return false;
			return true;
		}

	}

	private final static class Loader extends CacheLoader<CacheKey, RateLimiter> {
		@Override
		public RateLimiter load(CacheKey key) {
			double rateLimit = DEFAULT_RATE;
			
			if (key.tenant.getEnableRateLimitByIP()) {
				Double tenantIpRateLimit = ENDPOINT_RATE_MAP.get(key.tenant.getId());
	
				if (tenantIpRateLimit != null) {
					rateLimit = tenantIpRateLimit;
				}
	
			}
			logger.debug(
					String.format("new RateLimiter ip=%s tenant=%s endpoint=%s -> limit=%f",
							key.ip, key.tenant.getName(), key.endpoint, rateLimit));
			return RateLimiter.create(rateLimit);
		}
	}

	/**
	 * 
	 * @param ip
	 * @param tenant
	 * @param endpoint 針對不同的網址限流，這個參數目前無用
	 * @param timeout second
	 * @return
	 */
	private boolean isRequestPermittedWithTimeout(String ip, Tenant tenant, String endpoint, int timeout) {
		CacheKey key = new CacheKey(ip, tenant, DEFAULT_ENDPOINT_KEY); // 目前無論如何都塞 DEFAULT_ENDPOINT_KEY，未來有需要再針對不同 endpoint 給予不同的流量限制
		RateLimiter limit = rateLimits.getUnchecked(key);
		if (limit.tryAcquire()) {
			return true;
		}

		// didn't get it right away, log, wait for [timeout] second, then fail request
		logger.warn(
				String.format("rate limited; waiting for up to 1 second (ip %s, tenant %s, endpoint %s)", 
						ip, tenant.getName(), endpoint 
					));
		return limit.tryAcquire(timeout, TimeUnit.SECONDS);
	}

	public boolean isRequestPermitted(String ip, Tenant tenant, String endpoint) {
		return isRequestPermittedWithTimeout(ip, tenant, endpoint, 1);
	}
	
	public static boolean acquireRequestPermition(String ip, Tenant tenant, String endpoint) {
		return getInstance().isRequestPermitted(ip, tenant, endpoint);
	}

	private static RequestRateLimiter getInstance() {
		if (instance  == null) {
			instance = new RequestRateLimiter();
		}
		return instance;
	}
}
