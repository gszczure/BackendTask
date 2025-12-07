package org.codibly.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.ZonedDateTime;

public record ErrorResponse(

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyy-MM-dd'T'HH:mm:ss")
        ZonedDateTime timestamp,
        int status,
        String errorMessage,
        String message,
        String path
) {
}
