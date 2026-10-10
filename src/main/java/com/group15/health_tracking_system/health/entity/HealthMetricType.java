package com.group15.health_tracking_system.health.entity;

/**
 * Scalar health measurements supported by the Health Records module.
 *
 * Blood pressure is deliberately not included because it consists of
 * two related values and is represented by BloodPressureRecord.
 */
public enum HealthMetricType {

    WEIGHT("Weight", "kg"),
    HEART_RATE("Heart Rate", "bpm"),
    BODY_TEMPERATURE("Body Temperature", "°C"),
    OXYGEN_SATURATION("Oxygen Saturation", "%"),
    BLOOD_GLUCOSE("Blood Glucose", "mmol/L");

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