package com.intumit.solr.robot.qadialog;

import java.io.Serializable;
import java.util.Calendar;

import com.intumit.android.search.util.TaiwanAddressNormalizeUtil;

public class AddressRuleCheckResult extends RuleCheckResult implements Serializable {
	public static final AddressRuleCheckResult NO_MATCH = new AddressRuleCheckResult(RuleCheckResultStatus.NOT_MATCH, null, null, null, null);
	
	TaiwanAddressNormalizeUtil addr;
	String message;
	
	public AddressRuleCheckResult(RuleCheckResultStatus status, String value, Rule rule, TaiwanAddressNormalizeUtil addr, String message) {
		super(status, value, rule);
		this.addr = addr;
		this.message = message;
	}

	public TaiwanAddressNormalizeUtil getAddr() {
		return addr;
	}

	public void setAddr(TaiwanAddressNormalizeUtil addr) {
		this.addr = addr;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getPostal_code() {
		return addr.getPostal_code();
	}

	public String getCountry() {
		return addr.getCountry();
	}

	public String getAdministrative_area() {
		return addr.getAdministrative_area();
	}

	public String getLocality() {
		return addr.getLocality();
	}

	public String getSublocality() {
		return addr.getSublocality();
	}

	public String getStreet_number() {
		return addr.getStreet_number();
	}

	public String getFloor() {
		return addr.getFloor();
	}

	public String getFormatAddress() {
		return addr.getFormatAddress();
	}

	@Override
	public String toString() {
		return "AddressRuleCheckResult [addr=" + addr + "]";
	}
	
}
