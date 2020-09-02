package com.intumit.solr.robot;


public enum ExpireDuration {
	
	forever(0, "global.forever"){
		@Override
		public boolean is(Integer days) {
			return days == null || super.is(days);
		}
		@Override
		public long getExpireTime(long createdDate) {
			return Long.MAX_VALUE;
		}
	},
	three_days(3, "three.days"),
	one_week(7, "one.week"),
	one_month(30, "one.month");
	
	public final int days;
	
	public final String label;
	
	ExpireDuration(int days, String label){
		this.days = days;
		this.label = label;
	}
	
	public boolean is(Integer days){
		return days != null && days.equals(this.days);
	}
	
	public long getExpireTime(long createdDate){
		return createdDate + days * 24L * 60 * 60000;
	}
	
	public static ExpireDuration of(Integer days){
		ExpireDuration ed = null;
		for(ExpireDuration v:values()){
			if(v.is(days)){
				ed = v;
				break;
			}
		}
		return ed;
	}
	
}