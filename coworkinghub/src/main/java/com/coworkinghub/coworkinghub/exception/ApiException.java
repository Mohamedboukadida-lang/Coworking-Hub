package com.coworkinghub.coworkinghub.exception;

import org.springframework.http.HttpStatus;

/** An error that maps directly to an HTTP status, handled by {@link ApiExceptionHandler}. */
public class ApiException extends RuntimeException {
    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
