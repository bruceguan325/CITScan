package com.intumit.citi;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

import com.intumit.message.MessageUtil;

public enum SsoErrorType {
    
    devProError("sso.error.dev.pro.error"), 
    robotError("sso.error.robot.error"), 
    userNoPermission("sso.error.user.noPermission");
    
    private String msgKey;
    
    private SsoErrorType(String msgKey) {
        this.msgKey = msgKey;
    }
    
    public String getMessage(Locale locale, String... arg) {
        return MessageUtil.getMessage(locale, msgKey, arg);
    }
    
    public static SsoErrorType fromName(String name) {
        SsoErrorType[] types = SsoErrorType.values();
        for(SsoErrorType type : types) {
            if(StringUtils.equals(type.name(), name)) {
                return type;
            }
        }
        return null;
    }
    

}
