package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class ResourceConflictException extends ApiException {
    public ResourceConflictException(String message) { super(ErrorCode.RESOURCE_CONFLICT, message); }
    public ResourceConflictException(String message, Throwable cause) { super(ErrorCode.RESOURCE_CONFLICT, message, cause); }
}
