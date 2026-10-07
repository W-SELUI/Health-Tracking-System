package com.group15.health_tracking_system.common.exception;


// Thrown when a requested application resource does not exist.

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}