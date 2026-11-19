package com.team.common.error;

public class ConflictException extends ApiException {
    public ConflictException(ErrorCode code, String message) {
        super(code, message);
    }
}