package com.group15.health_tracking_system.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents one recorded blood pressure observation used for analytics.
 *
 * <p>
 * A blood pressure observation always keeps the systolic and diastolic
 * values together with the same observation date and time. This prevents
 * values from different blood pressure records from being incorrectly
 * combined.
 * </p>
 *
 * <p>
 * This class is an analytics DTO only and is not persisted in the database.
 * </p>
 */
public class BloodPressureDataPoint {

    private final LocalDateTime observedAt;
    private final BigDecimal systolic;
    private final BigDecimal diastolic;

    /**
     * Creates a blood pressure analytics data point.
     *
     * @param observedAt date and time the blood pressure was recorded
     * @param systolic systolic value from the recorded observation
     * @param diastolic diastolic value from the same recorded observation
     */
    public BloodPressureDataPoint(
            LocalDateTime observedAt,
            BigDecimal systolic,
            BigDecimal diastolic) {

        this.observedAt = observedAt;
        this.systolic = systolic;
        this.diastolic = diastolic;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public BigDecimal getSystolic() {
        return systolic;
    }

    public BigDecimal getDiastolic() {
        return diastolic;
    }
}