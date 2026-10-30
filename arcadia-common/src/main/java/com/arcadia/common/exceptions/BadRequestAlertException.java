package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class BadRequestAlertException extends ApiException {

    public BadRequestAlertException(String message) {
        super(ErrorCode.INVALID_REQUEST, message);
    }

    public BadRequestAlertException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}