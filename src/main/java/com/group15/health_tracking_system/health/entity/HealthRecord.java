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
 * Represents one measurable health observation belonging to a patient.
 *
 * A record may be entered by the patient or, in a later integration stage,
 * by an authorised healthcare provider.
 */
@Entity
@Table(
        name = "health_records",
        indexes = {
            @Index(
                    name = "idx_health_record_patient_observed",
                    columnList = "patient_id, observed_at"
            ),
            @Index(
                    name = "idx_health_record_patient_metric",
                    columnList = "patient_id, metric_type"
            )
        }
)
public class HealthRecord extends BaseEntity {

    /**
     * Patient whose health information this record represents.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    /**
     * User who entered the record.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private User recordedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false, length = 50)
    private HealthMetricType metricType;

    /**
     * Numeric measurement for the selected metric.
     */
    @Column(
            name = "metric_value",
            nullable = false,
            precision = 12,
            scale = 3
    )
    private BigDecimal metricValue;

    /**
     * Date and time at which the measurement was actually observed.
     */
    @Column(name = "observed_at", nullable = false)
    private LocalDateTime observedAt;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 30)
    private HealthRecordSource source;

    public HealthRecord() {
    }

    public HealthRecord(
            User patient,
            User recordedBy,
            HealthMetricType metricType,
            BigDecimal metricValue,
            LocalDateTime observedAt,
            String notes,
            HealthRecordSource source) {

        this.patient = patient;
        this.recordedBy = recordedBy;
        this.metricType = metricType;
        this.metricValue = metricValue;
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

    public HealthRecordSource getSource() {
        return source;
    }

    public void setSource(HealthRecordSource source) {
        this.source = source;
    }

    /**
     * Unit is derived from the metric type rather than stored independently.
     */
    public String getUnit() {
        return metricType == null ? null : metricType.getUnit();
    }
}