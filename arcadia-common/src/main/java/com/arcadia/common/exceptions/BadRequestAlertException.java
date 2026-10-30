package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class BadRequestAlertException extends ApiException {
    public BadRequestAlertException(String message) { super(ErrorCode.BAD_REQUEST, message); }
    public BadRequestAlertException(String message, Throwable cause) { super(ErrorCode.BAD_REQUEST, message, cause); }
}
