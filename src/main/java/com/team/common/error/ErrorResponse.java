
package com.team.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        ErrorCode code,
        String message,
        Instant timestamp,
        Map<String, String> fieldErrors) {

    public ErrorResponse {
        fieldErrors = fieldErrors == null
                ? null
                : Map.copyOf(fieldErrors);
    }

    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code, message, Instant.now(), null);
    }

    public static ErrorResponse withFields(
            ErrorCode code,
            String message,
            Map<String, String> fieldErrors) {

        return new ErrorResponse(code, message, Instant.now(), fieldErrors);
    }
}
