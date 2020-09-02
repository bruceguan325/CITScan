package com.intumit.solr.util;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限制流量的工具程式
 * 這裡的「流量」並非指網路流量，而是任何資源的使用頻率
 * 限制的方式是以每秒可使用的次數（permits per second）來實作
 * 因此應用上可以用來限制如「每秒API呼叫次數」「每秒下載次數」「每秒嘗試登入次數」等等...
 * 
 * Google Guava 也有 RateLimiter，但我們這個有 implements Serializable，Google 的沒有。
 * 當然，一樣有 Thread safe
 * 
 * @author herb
 *
 */
public class RateLimiter implements Serializable {

	Long timestamp = null;
	Integer capacity = null;
	Integer rate = null;
	Integer tokens = null;
	
	/**
	 * 每秒允許的流量（目前不允許浮點數，因此最少每秒一次，不能小於1，所以不能說要限制每三秒才能一次）
	 * @param rate Permits per second
	 * @return
	 */
	public static RateLimiter create(int rate) {
		return new RateLimiter((long)(System.currentTimeMillis() / 1000), rate, rate, rate);
	}
	
	RateLimiter() {
		super();
	}

	/**
	 * 
	 * @param timestamp use second as unit, not millisecond.
	 * @param capacity
	 * @param rate
	 * @param tokens
	 */
	public RateLimiter(long timestamp, int capacity, int rate, int tokens) {
		super();
		this.timestamp = timestamp;
		this.capacity = capacity;
		this.rate = rate;
		this.tokens =tokens;
	}
	
	/**
	 * 取得 permit，如果回傳 true，代表目前資源允許。如果 false 代表目前不行。
	 * 現在並沒有 wait 機制，所以這是直接返回行或者不行。
	 * 
	 * @return
	 */
	public synchronized boolean tryAcquire() {
		long now = (long)(System.currentTimeMillis() / 1000);
		
		// 補充可用的量，採用呼叫的時候才去算時間差補 token
		if (now > timestamp) {
			int regen = (int)((now - timestamp) * rate);
			tokens = (int)Math.min(capacity, tokens + regen);
			timestamp = now;
		}
		
		if (tokens > 0) {
			tokens--;
			return true;
		}
		
		return false;
	}
	
	/**
	 * 取得 permit，如果回傳 true，代表目前資源允許。如果 false 代表目前不行。
	 * acquire 會等待直到拿到 permit
	 * 
	 * @return
	 */
	public boolean acquire() {
		int c = 0;
		while (!tryAcquire()) {
			try {
				c++;
				Thread.sleep(500);
			}
			catch (InterruptedException ignoreIt) {}
			
			if (c >= 20 && c % 20 == 0) {
				System.out.println("[Rate Limiter Alert!] Acquire wait too long...[" + c + " times]");
			}
		}
		return true;
	}
	
	public static void main(String[] args) {
		// 來測試一下
		final RateLimiter rt = RateLimiter.create(3);
		final AtomicInteger sn = new AtomicInteger(0);
		final AtomicInteger counter = new AtomicInteger(0);
		
		Executor pool = Executors.newFixedThreadPool(10);
		
		for (int j=0; j < 50; j++) {
			Runnable r = new Runnable() {
				int serial = 0;
	
				@Override
				public void run() {
					SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss");
					serial = sn.incrementAndGet();
					
					for (int i=0; i < 10; i++) {
						int c = counter.incrementAndGet();
						
						if (i % 2 == 0) {
							if (rt.acquire()) {
								// acquire 會等待，所以執行到這裡代表已經排隊排到了
								System.out.println("Thread[" + serial + "] acquire " + (i+1) + "th, counter[" + c + "]:" + sdf.format(Calendar.getInstance().getTime()));
							}
						}
						else {
							// tryAcquire 不會等待，如果來不是直接可以買票，就離開（屬於不願意排隊的個性）
							if (rt.tryAcquire()) {
								System.out.println("Thread[" + serial + "] tryAcquire success " + (i+1) + "th, counter[" + c + "]:" + sdf.format(Calendar.getInstance().getTime()));
							}
							else {
								// 正常這樣的情境 failed 應該是大多數，除非有運氣好的 thread...
								System.out.println("Thread[" + serial + "] tryAcquire failed " + (i+1) + "th, counter[" + c + "]:" + sdf.format(Calendar.getInstance().getTime()));
							}
						}
					}
				}
			};
			
			pool.execute(r);
		}
		
		//MoreExecutors.shutdownAndAwaitTermination(pool, 1l, TimeUnit.MINUTE);
	}
}

