package com.intumit.solr.searchKeywords;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * 用來記錄一些 Click
 * 比如說紀錄使用者點選的搜尋結果，或者使用者點選的相關文章
 * 或者使用者點選的相關關鍵字
 * 
 * @author Herb
 */
public class ClickLog implements Serializable {

	private Long id;
	
	/**
	 * 點選的項目
	 * 文章？關鍵字？
	 * 最好能紀錄 ID，或者關鍵字本身
	 * 
	 */
	private String target;
	
	/**
	 * 視什麼情況下點選的？
	 * 搜尋結果，或者以文找文
	 */
	private String func;
	
	/**
	 * 辨識使用者的字串
	 * 可以利用 cookie 來儲存一些數字，這樣就可以辨識使用者
	 */
	private String identity;
	
	/**
	 * 事件發生時間
	 */
	private Timestamp logtime;
	
	/**
	 * 點擊ID
	 */
	private String clickId;
	
	/**
	 * 點擊標題
	 */
	private String clickTitle;

	private int frequency;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getFunc() {
		return func;
	}

	public void setFunc(String func) {
		this.func = func;
	}

	public String getIdentity() {
		return identity;
	}

	public void setIdentity(String identity) {
		this.identity = identity;
	}
	
	public void setLogtime(Timestamp logtime) {
		this.logtime = logtime;
	}

	public Timestamp getLogtime() {
		return logtime;
	}

	public String getClickId() {
		return clickId;
	}

	public void setClickId(String clickId) {
		this.clickId = clickId;
	}

	public String getClickTitle() {
		return clickTitle;
	}

	public void setClickTitle(String clickTitle) {
		this.clickTitle = clickTitle;
	}
	
	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}
}
