package com.intumit.citi;

import java.io.Serializable;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class CitiDBInfo implements Serializable {

    private static final long serialVersionUID = 1L;
    
    public static final CitiDBInfo EMPTY = new CitiDBInfo();

    private String address;

    private String dbName;

    private String account;

    private String pwd;
    
    private long updateTime;

    @Override
    public String toString() {
        return new ReflectionToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(address).append(dbName).append(account).append(pwd)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CitiDBInfo) {
            CitiDBInfo that = (CitiDBInfo)o;
            return new EqualsBuilder().append(this.address, that.address)
                    .append(this.dbName, that.dbName).append(this.account, that.account)
                    .append(this.pwd, that.pwd).isEquals();
        }
        return false;
    }

    public String getAddress() {
        return address;
    }

    public String getDbName() {
        return dbName;
    }

    public String getAccount() {
        return account;
    }

    public String getPwd() {
        return pwd;
    }
    
    public void refreshUpdateTime() {
        updateTime = System.currentTimeMillis();
    }
    
    public boolean isExpired(Integer thresholdInSecond) {
        return (System.currentTimeMillis() - updateTime) > thresholdInSecond * 1000L;
    }
    
    public static class Builder {
        
        private CitiDBInfo dbInfo = new CitiDBInfo();
        
        public Builder setAddress(String address) {
            dbInfo.address = address;
            return this;
        }
        
        public Builder setDbName(String dbName) {
            dbInfo.dbName = dbName;
            return this;
        }
        
        public Builder setAccount(String account) {
            dbInfo.account = account;
            return this;
        }
        
        public Builder setPwd(String pwd) {
            dbInfo.pwd = pwd;
            return this;
        }
        
        public Builder setUpdateTime(long updateTime) {
            dbInfo.updateTime = updateTime;
            return this;
        }
        
        public CitiDBInfo build() {
            return dbInfo;
        }
    }


}
