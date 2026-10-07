package com.group15.health_tracking_system.common.exception;

/**
 * Thrown when an operation would create a resource that must be unique.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}