package com.intumit.solr.robot;

import org.elasticsearch.common.lang3.StringUtils;

public enum QaCategory {

	Bank("銀行"),
	Credit_card("信用卡"),
	Exchange("外匯"),
	Fund("基金"),
	Corporate_finance("企金"),
	Securities("證券"),
	All("全部");

    private final String qaCategoryName;

    private QaCategory(String qaCategoryName) {
        this.qaCategoryName = qaCategoryName;
    }

    public String getQaCategoryName() {
    	return this.qaCategoryName;
    }

    @Override
    public String toString() {
       return this.qaCategoryName;
    }

    public static QaCategory fromName(String name) {
    	if(StringUtils.isNotBlank(name)) {
    		for(QaCategory qaCate : QaCategory.values()) {
    			if(StringUtils.equals(name, qaCate.name()))
    				return qaCate;
    		}
    	}
    	return null;
    }

}