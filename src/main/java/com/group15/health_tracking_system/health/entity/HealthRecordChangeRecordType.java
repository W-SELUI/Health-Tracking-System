package com.group15.health_tracking_system.health.entity;

/**
 * Identifies which health-record persistence model is affected by
 * a Provider change proposal.
 *
 * Scalar measurements and Blood Pressure are separate because
 * Blood Pressure is one paired clinical observation containing
 * systolic and diastolic values.
 */
public enum HealthRecordChangeRecordType {

    SCALAR("Health Measurement"),
    BLOOD_PRESSURE("Blood Pressure");

    private final String displayName;

    HealthRecordChangeRecordType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}