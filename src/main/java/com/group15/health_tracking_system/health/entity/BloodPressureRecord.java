package com.group15.health_tracking_system.health.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import com.group15.health_tracking_system.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents one complete blood-pressure observation.
 *
 * Systolic and diastolic values belong to the same measurement and
 * therefore share one observation time and one database record.
 */
@Entity
@Table(
        name = "blood_pressure_records",
        indexes = {
            @Index(
                    name = "idx_blood_pressure_patient_observed",
                    columnList = "patient_id, observed_at"
            )
        }
)
public class BloodPressureRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private User recordedBy;

    @Column(
            name = "systolic",
            nullable = false,
            precision = 6,
            scale = 1
    )
    private BigDecimal systolic;

    @Column(
            name = "diastolic",
            nullable = false,
            precision = 6,
            scale = 1
    )
    private BigDecimal diastolic;

    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private HealthRecordSource source;

    public BloodPressureRecord() {
    }

    public BloodPressureRecord(
            User patient,
            User recordedBy,
            BigDecimal systolic,
            BigDecimal diastolic,
            LocalDateTime observedAt,
            String notes,
            HealthRecordSource source) {

        this.patient = patient;
        this.recordedBy = recordedBy;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.observedAt = observedAt;
        this.notes = notes;
        this.source = source;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
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

    public HealthRecordSource getSource() {
        return source;
    }

    public void setSource(HealthRecordSource source) {
        this.source = source;
    }

    public String getUnit() {
        return "mmHg";
    }
}