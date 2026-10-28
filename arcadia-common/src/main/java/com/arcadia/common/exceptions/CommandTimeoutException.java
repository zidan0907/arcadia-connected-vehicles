package com.arcadia.common.exceptions;

import com.arcadia.common.error.ErrorCode;

public class CommandTimeoutException extends ApiException {
    public CommandTimeoutException(String message) { super(ErrorCode.COMMAND_TIMEOUT, message); }
    public CommandTimeoutException(String message, Throwable cause) { super(ErrorCode.COMMAND_TIMEOUT, message, cause); }
}
