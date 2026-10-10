package com.group15.health_tracking_system.health.entity;

/**
 * Identifies how a health record entered the system.
 */
public enum HealthRecordSource {

    PATIENT_REPORTED("Patient Reported"),
    PROVIDER_RECORDED("Provider Recorded");

    private final String displayName;

    HealthRecordSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}