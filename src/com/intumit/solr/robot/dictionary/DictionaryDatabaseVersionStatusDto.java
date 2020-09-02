package com.intumit.solr.robot.dictionary;

import java.io.Serializable;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class DictionaryDatabaseVersionStatusDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean audit;

    private String lastPassTime;

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).build();
    }

    public boolean isAudit() {
        return audit;
    }

    public void setAudit(boolean audit) {
        this.audit = audit;
    }

    public String getLastPassTime() {
        return lastPassTime;
    }

    public void setLastPassTime(String lastPassTime) {
        this.lastPassTime = lastPassTime;
    }


}
