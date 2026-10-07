package com.group15.health_tracking_system.user.entity;

/**
 * System roles agreed for Assignment 2.
 */
public enum Role {
    PATIENT("Patient"),
    PROVIDER("Healthcare Provider"),
    ADMIN("Administrator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}