package com.group15.health_tracking_system.health.entity;

/**
 * Health measurements supported by the Health Records module.
 *
 * Each metric defines its standard display name and unit so that
 * inconsistent metric/unit combinations cannot be submitted by users.
 */
public enum HealthMetricType {

    WEIGHT("Weight", "kg"),
    HEART_RATE("Heart Rate", "bpm"),
    BODY_TEMPERATURE("Body Temperature", "°C"),
    OXYGEN_SATURATION("Oxygen Saturation", "%"),
    BLOOD_GLUCOSE("Blood Glucose", "mmol/L"),
    BLOOD_PRESSURE_SYSTOLIC("Systolic Blood Pressure", "mmHg"),
    BLOOD_PRESSURE_DIASTOLIC("Diastolic Blood Pressure", "mmHg");

    private final String displayName;
    private final String unit;

    HealthMetricType(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }
}