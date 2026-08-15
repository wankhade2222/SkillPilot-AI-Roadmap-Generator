package com.skillforge.exception;

import org.springframework.http.HttpStatus;

public class GeminiIntegrationException extends RuntimeException {

    private final HttpStatus status;

    public GeminiIntegrationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
