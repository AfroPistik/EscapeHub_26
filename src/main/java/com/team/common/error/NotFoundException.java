package com.team.common.error;

public class NotFoundException extends ApiException {
    public NotFoundException(ErrorCode code, String message) {
        super(code, message);
    }
}