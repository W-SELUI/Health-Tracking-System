package com.group15.health_tracking_system.health.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Form data used when a patient creates or edits a blood-pressure record.
 *
 * A blood-pressure observation is valid only when both systolic and
 * diastolic values are supplied for the same observation time.
 */
public class BloodPressureRecordRequest {

    @NotNull(message = "Systolic value is required")
    private BigDecimal systolic;

    @NotNull(message = "Diastolic value is required")
    private BigDecimal diastolic;

    @NotNull(message = "Observation date and time are required")
    @PastOrPresent(message = "Observation time cannot be in the future")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime observedAt;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public BloodPressureRecordRequest() {
    }

    public BloodPressureRecordRequest(
            BigDecimal systolic,
            BigDecimal diastolic,
            LocalDateTime observedAt,
            String notes) {

        this.systolic = systolic;
        this.diastolic = diastolic;
        this.observedAt = observedAt;
        this.notes = notes;
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