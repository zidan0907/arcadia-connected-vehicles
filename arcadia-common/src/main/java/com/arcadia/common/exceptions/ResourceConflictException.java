package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class ResourceConflictException extends ApiException {

    public ResourceConflictException(String message) {
        super(ErrorCode.RESOURCE_CONFLICT, message);
    }

    public ResourceConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}