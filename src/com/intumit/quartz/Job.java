package com.intumit.quartz;



public class Job {

	private String name;
	private String url;
	private String cron;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setCron(String cron) {
		this.cron = cron;
	}

	public String getCron() {
		return cron;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}


}
