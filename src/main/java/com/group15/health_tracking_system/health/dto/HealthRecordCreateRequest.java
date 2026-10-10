package com.group15.health_tracking_system.health.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Unified form data used when a patient creates a health record.
 *
 * Scalar measurements use metricValue. Blood pressure uses systolic and
 * diastolic together. Ownership, provenance and units are server-controlled.
 */
public class HealthRecordCreateRequest {

    @NotNull(message = "Health metric is required")
    private HealthRecordFilterOption metricType;

    private BigDecimal metricValue;

    private BigDecimal systolic;

    private BigDecimal diastolic;

    @NotNull(message = "Observation date and time are required")
    @PastOrPresent(message = "Observation time cannot be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime observedAt;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public HealthRecordCreateRequest() {
    }

    public HealthRecordFilterOption getMetricType() {
        return metricType;
    }

    public void setMetricType(HealthRecordFilterOption metricType) {
        this.metricType = metricType;
    }

    public BigDecimal getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(BigDecimal metricValue) {
        this.metricValue = metricValue;
    }

    public BigDecimal getSystolic() {
        return systolic;
    }

    public void setSystolic(BigDecimal systolic) {
        this.systolic = systolic;
    }

    public BigDecimal getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(BigDecimal diastolic) {
        this.diastolic = diastolic;
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
