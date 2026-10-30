package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class CommandTimeoutException extends ApiException {

    public CommandTimeoutException(String message) {
        super(ErrorCode.VEHICLE_TIMEOUT, message);
    }

    public CommandTimeoutException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}