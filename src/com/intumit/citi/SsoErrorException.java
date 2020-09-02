package com.intumit.citi;

public class SsoErrorException extends Exception {

    private static final long serialVersionUID = 1L;

    private SsoErrorType errType;

    public SsoErrorException(SsoErrorType errType) {
        super();
        this.errType = errType;
    }

    public SsoErrorType getErrType() {
        return errType;
    }


}
