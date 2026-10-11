package com.group15.health_tracking_system.health.entity;

/**
 * Represents the type of permanent health-record change proposed
 * by a healthcare Provider.
 *
 * DELETE is intentionally unsupported. Providers may propose new
 * records or corrections to existing records, but cannot delete
 * patient health information.
 */
public enum HealthRecordChangeOperation {

    CREATE("Create"),
    UPDATE("Update");

    private final String displayName;

    HealthRecordChangeOperation(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}