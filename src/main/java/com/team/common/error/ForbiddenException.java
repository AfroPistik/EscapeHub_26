package com.team.common.error;

public class ForbiddenException extends ApiException {
    public ForbiddenException(ErrorCode code, String message) {
        super(code, message);
    }
}