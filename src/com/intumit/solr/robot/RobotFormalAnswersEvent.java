package com.intumit.solr.robot;

import java.io.Serializable;

public class RobotFormalAnswersEvent implements Serializable {

	private static final long serialVersionUID = -1200623854712025402L;

	private Integer tenantId;

	public RobotFormalAnswersEvent(Integer tenantId) {
		this.tenantId = tenantId;
	}

	public Integer getTenantId() {
		return tenantId;
	}
}
