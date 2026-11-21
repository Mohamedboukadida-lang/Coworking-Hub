package com.coworkinghub.coworkinghub.exception;

/**
 * Thrown when a requested entity does not exist. Mapped to HTTP 404 by {@link ApiExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id " + id);
    }
}
