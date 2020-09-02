package com.intumit.solr.robot;

import com.intumit.solr.tenant.Tenant;

public class DummyTenant extends Tenant {
	
	public DummyTenant() {
		super();
		setId(99999999);
		setName("Tmp tenant " + System.currentTimeMillis());
		setNotes("" + System.currentTimeMillis());
	}
}
