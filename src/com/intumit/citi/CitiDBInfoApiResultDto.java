package com.intumit.citi;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CitiDBInfoApiResultDto implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public static final CitiDBInfoApiResultDto BAD_RESULT = new CitiDBInfoApiResultDto(CitiDBInfo.EMPTY, CitiDBInfo.EMPTY);
    
    private CitiDBInfo dbInfo;
    
    private boolean needInit;
    
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(dbInfo).append(needInit).build();
    }
    
    @Override
    public boolean equals(Object o) {
        if(o instanceof CitiDBInfoApiResultDto) {
            CitiDBInfoApiResultDto that = (CitiDBInfoApiResultDto)o;
            return new EqualsBuilder().append(this.dbInfo, that.dbInfo).append(this.needInit, that.needInit).isEquals();
        }
        return false;
    }
    
    public CitiDBInfoApiResultDto(CitiDBInfo old, CitiDBInfo refresh) {
        dbInfo = old;
        if(!old.equals(refresh)) {
            dbInfo = refresh;
            needInit = true;
        }
    }

    public CitiDBInfo getDbInfo() {
        return dbInfo;
    }

    public boolean isNeedInit() {
        return needInit;
    }
    
    

}
