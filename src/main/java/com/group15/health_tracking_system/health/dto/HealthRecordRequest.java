package com.group15.health_tracking_system.health.dto;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Form data used when a patient creates or edits a health record.
 *
 * Ownership, provenance and units are intentionally not accepted from
 * the browser. These values are determined by the server.
 */
public class HealthRecordRequest {

    @NotNull(message = "Health metric is required")
    private HealthMetricType metricType;

    @NotNull(message = "Measurement value is required")
    private BigDecimal metricValue;

    @NotNull(message = "Observation date and time are required")
    @PastOrPresent(message = "Observation time cannot be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime observedAt;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public HealthRecordRequest() {
    }

    public HealthRecordRequest(
            HealthMetricType metricType,
            BigDecimal metricValue,
            LocalDateTime observedAt,
            String notes) {

        this.metricType = metricType;
        this.metricValue = metricValue;
        this.observedAt = observedAt;
        this.notes = notes;
    }

    public HealthMetricType getMetricType() {
        return metricType;
    }

    public void setMetricType(HealthMetricType metricType) {
        this.metricType = metricType;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(LocalDateTime observedAt) {
        this.observedAt = observedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}