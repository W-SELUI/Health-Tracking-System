package com.group15.health_tracking_system.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a single scalar health measurement used for analytics.
 *
 * <p>
 * Each data point contains the exact recorded value and the date/time
 * at which the measurement was observed. These data points can later be
 * displayed chronologically in health analytics charts.
 * </p>
 *
 * <p>
 * This class is an analytics DTO only. It is not stored as a database entity.
 * </p>
 */
public class MetricDataPoint {

    private final LocalDateTime observedAt;
    private final BigDecimal value;

    /**
     * Creates a metric data point.
     *
     * @param observedAt date and time the health measurement was recorded
     * @param value exact recorded measurement value
     */
    public MetricDataPoint(LocalDateTime observedAt, BigDecimal value) {
        this.observedAt = observedAt;
        this.value = value;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public BigDecimal getValue() {
        return value;
    }
}