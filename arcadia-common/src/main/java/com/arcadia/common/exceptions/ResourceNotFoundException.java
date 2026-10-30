package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) { super(ErrorCode.RESOURCE_NOT_FOUND, message); }
    public ResourceNotFoundException(String resourceName, Object resourceId) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resourceName + " not found: " + resourceId);
    }
    public ResourceNotFoundException(String message, Throwable cause) { super(ErrorCode.RESOURCE_NOT_FOUND, message, cause); }
}
