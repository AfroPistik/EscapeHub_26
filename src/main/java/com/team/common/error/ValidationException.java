package com.team.common.error;

public class ValidationException extends ApiException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}