package com.intumit.license;

import java.util.Locale;

import com.intumit.message.MessageUtil;


public enum InitialError {
    licence("initialError.licence"); //

    private String resourceKey;

    private InitialError(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    public String toString() {
        return MessageUtil.getMessage(Locale.TAIWAN, resourceKey);
    }

}
