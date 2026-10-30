package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class ExternalServiceException extends ApiException {

    public ExternalServiceException(String message) {
        super(ErrorCode.EXTERNAL_SERVICE_ERROR, message);
    }

    public ExternalServiceException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}