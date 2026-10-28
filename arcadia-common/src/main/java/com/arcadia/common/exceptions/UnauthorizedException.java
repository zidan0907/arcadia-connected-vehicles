package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class UnauthorizedException extends ApiException {
    public UnauthorizedException(String message) { super(ErrorCode.UNAUTHORIZED, message); }
    public UnauthorizedException(String message, Throwable cause) { super(ErrorCode.UNAUTHORIZED, message, cause); }
}
