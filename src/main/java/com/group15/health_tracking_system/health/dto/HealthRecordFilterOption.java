package com.group15.health_tracking_system.health.dto;

import com.group15.health_tracking_system.health.entity.HealthMetricType;

/**
 * Options available in the unified Health Records history filter.
 *
 * Scalar measurements map to HealthMetricType.
 * Blood pressure is represented separately because it has its own entity.
 */
public enum HealthRecordFilterOption {

    WEIGHT(
            "Weight",
            HealthMetricType.WEIGHT
    ),

    HEART_RATE(
            "Heart Rate",
            HealthMetricType.HEART_RATE
    ),

    BODY_TEMPERATURE(
            "Body Temperature",
            HealthMetricType.BODY_TEMPERATURE
    ),

    OXYGEN_SATURATION(
            "Oxygen Saturation",
            HealthMetricType.OXYGEN_SATURATION
    ),

    BLOOD_GLUCOSE(
            "Blood Glucose",
            HealthMetricType.BLOOD_GLUCOSE
    ),

    BLOOD_PRESSURE(
            "Blood Pressure",
            null
    );

    private final String displayName;
    private final HealthMetricType scalarMetricType;

    HealthRecordFilterOption(
            String displayName,
            HealthMetricType scalarMetricType) {

        this.displayName = displayName;
        this.scalarMetricType = scalarMetricType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public HealthMetricType getScalarMetricType() {
        return scalarMetricType;
    }

    public boolean isBloodPressure() {
        return this == BLOOD_PRESSURE;
    }

    public boolean isScalarMetric() {
        return scalarMetricType != null;
    }
}