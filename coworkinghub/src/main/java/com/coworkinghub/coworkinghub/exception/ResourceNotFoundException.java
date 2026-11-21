package com.coworkinghub.coworkinghub.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity does not exist; mapped to HTTP 404. */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String resource, Long id) {
        super(HttpStatus.NOT_FOUND, resource + " not found with id " + id);
    }
}
