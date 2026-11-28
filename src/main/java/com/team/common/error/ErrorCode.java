
package com.team.common.error;

public enum ErrorCode {
    // generic (same as the OpenAPI draft)
    VALIDATION_ERROR,
    METHOD_NOT_ALLOWED,
    UNAUTHORIZED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INTERNAL_SERVER_ERROR,

    // auth
    EMAIL_TAKEN,
    USERNAME_TAKEN,
    INVALID_CREDENTIALS,

    // teams
    TEAM_NOT_FOUND,
    USER_NOT_FOUND,
    NOT_A_MEMBER,
    INVALID_INVITE_CODE,
    ALREADY_MEMBER,
    OWNER_CANNOT_LEAVE,
    OWNER_CANNOT_REMOVE_SELF
}
