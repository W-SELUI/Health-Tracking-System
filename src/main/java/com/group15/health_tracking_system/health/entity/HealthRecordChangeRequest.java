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
 * Represents a healthcare Provider's proposed change to a Patient's
 * Health Records.
 *
 * A proposal does not modify permanent Health Record data when it is
 * created. The affected Patient must explicitly approve the request first.
 *
 * Both scalar HealthRecord values and paired BloodPressureRecord values
 * are supported. The recordType field identifies which persistence model
 * the proposal belongs to.
 *
 * UPDATE proposals retain a snapshot of the original record. This allows
 * the Patient to compare the current and proposed values and allows the
 * service layer to detect stale proposals before applying them.
 */
@Entity
@Table(
        name = "health_record_change_requests",
        indexes = {
            @Index(
                    name = "idx_health_change_patient_status",
                    columnList = "patient_id, status"
            ),
            @Index(
                    name = "idx_health_change_provider_status",
                    columnList = "provider_id, status"
            ),
            @Index(
                    name = "idx_health_change_target",
                    columnList = "record_type, target_record_id"
            )
        }
)
public class HealthRecordChangeRequest extends BaseEntity {

    /**
     * Patient whose Health Records would be changed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "patient_id",
            nullable = false
    )
    private User patient;

    /**
     * Healthcare Provider who submitted the proposal.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "provider_id",
            nullable = false
    )
    private User provider;

    /**
     * CREATE or UPDATE.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "operation",
            nullable = false,
            length = 20
    )
    private HealthRecordChangeOperation operation;

    /**
     * Scalar HealthRecord or paired BloodPressureRecord.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "record_type",
            nullable = false,
            length = 30
    )
    private HealthRecordChangeRecordType recordType;

    /**
     * Current lifecycle state of this proposal.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private HealthRecordChangeRequestStatus status;

    /**
     * Existing HealthRecord or BloodPressureRecord identifier for UPDATE.
     *
     * Null for CREATE proposals.
     *
     * recordType determines which table this identifier refers to.
     */
    @Column(name = "target_record_id")
    private Long targetRecordId;

    /**
     * Identifier of the permanent record affected or created after approval.
     *
     * Null until the proposal is successfully approved and applied.
     */
    @Column(name = "applied_record_id")
    private Long appliedRecordId;


    /* =========================================================
       ORIGINAL SNAPSHOT
       ========================================================= */

    /**
     * Original scalar metric type.
     *
     * Used only for scalar UPDATE proposals.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "original_metric_type",
            length = 50
    )
    private HealthMetricType originalMetricType;

    /**
     * Original scalar value.
     *
     * Used only for scalar UPDATE proposals.
     */
    @Column(
            name = "original_metric_value",
            precision = 12,
            scale = 3
    )
    private BigDecimal originalMetricValue;

    /**
     * Original systolic value.
     *
     * Used only for Blood Pressure UPDATE proposals.
     */
    @Column(
            name = "original_systolic",
            precision = 6,
            scale = 1
    )
    private BigDecimal originalSystolic;

    /**
     * Original diastolic value.
     *
     * Used only for Blood Pressure UPDATE proposals.
     */
    @Column(
            name = "original_diastolic",
            precision = 6,
            scale = 1
    )
    private BigDecimal originalDiastolic;

    /**
     * Original observation time.
     *
     * Null for CREATE proposals.
     */
    @Column(name = "original_observed_at")
    private LocalDateTime originalObservedAt;

    /**
     * Original Health Record notes.
     *
     * Null for CREATE proposals or when the original record had no notes.
     */
    @Column(
            name = "original_notes",
            length = 1000
    )
    private String originalNotes;

    /**
     * Original record provenance.
     *
     * This is retained as part of the snapshot for stale-change detection.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "original_source",
            length = 30
    )
    private HealthRecordSource originalSource;

    /**
     * User who originally recorded the target record.
     *
     * Stored as a snapshot identifier rather than a relationship because
     * this value describes the historical state of the target record.
     */
    @Column(name = "original_recorded_by_user_id")
    private Long originalRecordedByUserId;

    /**
     * BaseEntity updatedAt value when the Provider created the UPDATE
     * proposal.
     *
     * The approval service may use this together with the original field
     * snapshot to identify records changed after proposal creation.
     */
    @Column(name = "original_record_updated_at")
    private LocalDateTime originalRecordUpdatedAt;


    /* =========================================================
       PROPOSED VALUES
       ========================================================= */

    /**
     * Proposed scalar metric type.
     *
     * Used for scalar CREATE and UPDATE proposals.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "proposed_metric_type",
            length = 50
    )
    private HealthMetricType proposedMetricType;

    /**
     * Proposed scalar measurement.
     */
    @Column(
            name = "proposed_metric_value",
            precision = 12,
            scale = 3
    )
    private BigDecimal proposedMetricValue;

    /**
     * Proposed Blood Pressure systolic measurement.
     */
    @Column(
            name = "proposed_systolic",
            precision = 6,
            scale = 1
    )
    private BigDecimal proposedSystolic;

    /**
     * Proposed Blood Pressure diastolic measurement.
     */
    @Column(
            name = "proposed_diastolic",
            precision = 6,
            scale = 1
    )
    private BigDecimal proposedDiastolic;

    /**
     * Proposed observation time.
     */
    @Column(
            name = "proposed_observed_at",
            nullable = false
    )
    private LocalDateTime proposedObservedAt;

    /**
     * Proposed Health Record notes.
     */
    @Column(
            name = "proposed_notes",
            length = 1000
    )
    private String proposedNotes;


    /* =========================================================
       REQUEST LIFECYCLE
       ========================================================= */

    /**
     * Time the Provider submitted the proposal.
     */
    @Column(
            name = "requested_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime requestedAt;

    /**
     * Time the proposal reached a terminal state.
     *
     * Null while PENDING.
     */
    @Column(name = "responded_at")
    private LocalDateTime respondedAt;


    /**
     * Required by JPA.
     */
    protected HealthRecordChangeRequest() {
    }

    private HealthRecordChangeRequest(
            User patient,
            User provider,
            HealthRecordChangeOperation operation,
            HealthRecordChangeRecordType recordType) {

        if (patient == null) {
            throw new IllegalArgumentException(
                    "Patient is required"
            );
        }

        if (provider == null) {
            throw new IllegalArgumentException(
                    "Provider is required"
            );
        }

        this.patient = patient;
        this.provider = provider;
        this.operation = operation;
        this.recordType = recordType;

        this.status =
                HealthRecordChangeRequestStatus.PENDING;

        this.requestedAt =
                LocalDateTime.now();
    }


    /* =========================================================
       CREATE FACTORIES
       ========================================================= */

    /**
     * Creates a proposal for a new scalar Health Record.
     */
    public static HealthRecordChangeRequest proposeScalarCreate(
            User patient,
            User provider,
            HealthMetricType metricType,
            BigDecimal metricValue,
            LocalDateTime observedAt,
            String notes) {

        if (metricType == null) {
            throw new IllegalArgumentException(
                    "Health metric is required"
            );
        }

        if (metricValue == null) {
            throw new IllegalArgumentException(
                    "Measurement value is required"
            );
        }

        requireObservedAt(
                observedAt
        );

        HealthRecordChangeRequest request =
                new HealthRecordChangeRequest(
                        patient,
                        provider,
                        HealthRecordChangeOperation.CREATE,
                        HealthRecordChangeRecordType.SCALAR
                );

        request.proposedMetricType =
                metricType;

        request.proposedMetricValue =
                metricValue;

        request.proposedObservedAt =
                observedAt;

        request.proposedNotes =
                notes;

        return request;
    }

    /**
     * Creates a proposal for a new Blood Pressure observation.
     */
    public static HealthRecordChangeRequest proposeBloodPressureCreate(
            User patient,
            User provider,
            BigDecimal systolic,
            BigDecimal diastolic,
            LocalDateTime observedAt,
            String notes) {

        requireBloodPressureValues(
                systolic,
                diastolic
        );

        requireObservedAt(
                observedAt
        );

        HealthRecordChangeRequest request =
                new HealthRecordChangeRequest(
                        patient,
                        provider,
                        HealthRecordChangeOperation.CREATE,
                        HealthRecordChangeRecordType.BLOOD_PRESSURE
                );

        request.proposedSystolic =
                systolic;

        request.proposedDiastolic =
                diastolic;

        request.proposedObservedAt =
                observedAt;

        request.proposedNotes =
                notes;

        return request;
    }


    /* =========================================================
       UPDATE FACTORIES
       ========================================================= */

    /**
     * Creates an UPDATE proposal for an existing scalar Health Record.
     *
     * <p>
     * A complete original snapshot is stored before any proposed values
     * are recorded.
     * </p>
     */
    public static HealthRecordChangeRequest proposeScalarUpdate(
            User patient,
            User provider,
            HealthRecord originalRecord,
            HealthMetricType proposedMetricType,
            BigDecimal proposedMetricValue,
            LocalDateTime proposedObservedAt,
            String proposedNotes) {

        if (originalRecord == null
                || originalRecord.getId() == null) {

            throw new IllegalArgumentException(
                    "Existing Health Record is required"
            );
        }

        if (proposedMetricType == null) {
            throw new IllegalArgumentException(
                    "Health metric is required"
            );
        }

        if (proposedMetricValue == null) {
            throw new IllegalArgumentException(
                    "Measurement value is required"
            );
        }

        requireObservedAt(
                proposedObservedAt
        );

        HealthRecordChangeRequest request =
                new HealthRecordChangeRequest(
                        patient,
                        provider,
                        HealthRecordChangeOperation.UPDATE,
                        HealthRecordChangeRecordType.SCALAR
                );

        request.targetRecordId =
                originalRecord.getId();

        request.originalMetricType =
                originalRecord.getMetricType();

        request.originalMetricValue =
                originalRecord.getMetricValue();

        request.originalObservedAt =
                originalRecord.getObservedAt();

        request.originalNotes =
                originalRecord.getNotes();

        request.originalSource =
                originalRecord.getSource();

        request.originalRecordedByUserId =
                originalRecord.getRecordedBy() == null
                        ? null
                        : originalRecord
                                .getRecordedBy()
                                .getId();

        request.originalRecordUpdatedAt =
                originalRecord.getUpdatedAt();

        request.proposedMetricType =
                proposedMetricType;

        request.proposedMetricValue =
                proposedMetricValue;

        request.proposedObservedAt =
                proposedObservedAt;

        request.proposedNotes =
                proposedNotes;

        return request;
    }

    /**
     * Creates an UPDATE proposal for an existing Blood Pressure record.
     */
    public static HealthRecordChangeRequest proposeBloodPressureUpdate(
            User patient,
            User provider,
            BloodPressureRecord originalRecord,
            BigDecimal proposedSystolic,
            BigDecimal proposedDiastolic,
            LocalDateTime proposedObservedAt,
            String proposedNotes) {

        if (originalRecord == null
                || originalRecord.getId() == null) {

            throw new IllegalArgumentException(
                    "Existing Blood Pressure record is required"
            );
        }

        requireBloodPressureValues(
                proposedSystolic,
                proposedDiastolic
        );

        requireObservedAt(
                proposedObservedAt
        );

        HealthRecordChangeRequest request =
                new HealthRecordChangeRequest(
                        patient,
                        provider,
                        HealthRecordChangeOperation.UPDATE,
                        HealthRecordChangeRecordType.BLOOD_PRESSURE
                );

        request.targetRecordId =
                originalRecord.getId();

        request.originalSystolic =
                originalRecord.getSystolic();

        request.originalDiastolic =
                originalRecord.getDiastolic();

        request.originalObservedAt =
                originalRecord.getObservedAt();

        request.originalNotes =
                originalRecord.getNotes();

        request.originalSource =
                originalRecord.getSource();

        request.originalRecordedByUserId =
                originalRecord.getRecordedBy() == null
                        ? null
                        : originalRecord
                                .getRecordedBy()
                                .getId();

        request.originalRecordUpdatedAt =
                originalRecord.getUpdatedAt();

        request.proposedSystolic =
                proposedSystolic;

        request.proposedDiastolic =
                proposedDiastolic;

        request.proposedObservedAt =
                proposedObservedAt;

        request.proposedNotes =
                proposedNotes;

        return request;
    }


    /* =========================================================
       STATE TRANSITIONS
       ========================================================= */

    /**
     * Marks the proposal as successfully approved and applied.
     *
     * <p>
     * The service layer must apply the permanent Health Record change
     * before calling this method. Both operations will later occur in the
     * same transaction.
     * </p>
     */
    public void approve(
            Long appliedRecordId) {

        requirePending();

        if (appliedRecordId == null) {
            throw new IllegalArgumentException(
                    "Applied Health Record identifier is required"
            );
        }

        this.appliedRecordId =
                appliedRecordId;

        this.status =
                HealthRecordChangeRequestStatus.APPROVED;

        this.respondedAt =
                LocalDateTime.now();
    }

    /**
     * Records an explicit Patient rejection.
     */
    public void reject() {

        requirePending();

        this.status =
                HealthRecordChangeRequestStatus.REJECTED;

        this.respondedAt =
                LocalDateTime.now();
    }

    /**
     * Cancels an unresolved proposal.
     */
    public void cancel() {

        requirePending();

        this.status =
                HealthRecordChangeRequestStatus.CANCELLED;

        this.respondedAt =
                LocalDateTime.now();
    }

    /**
     * Marks an UPDATE proposal as stale because the permanent record has
     * changed since this proposal was created.
     */
    public void markStale() {

        requirePending();

        if (operation
                != HealthRecordChangeOperation.UPDATE) {

            throw new IllegalStateException(
                    "Only UPDATE proposals can become stale"
            );
        }

        this.status =
                HealthRecordChangeRequestStatus.STALE;

        this.respondedAt =
                LocalDateTime.now();
    }

    private void requirePending() {

        if (status
                != HealthRecordChangeRequestStatus.PENDING) {

            throw new IllegalStateException(
                    "Only pending Health Record change requests "
                    + "can be changed"
            );
        }
    }


    /* =========================================================
       STRUCTURAL VALIDATION
       ========================================================= */

    private static void requireObservedAt(
            LocalDateTime observedAt) {

        if (observedAt == null) {
            throw new IllegalArgumentException(
                    "Observation date and time are required"
            );
        }
    }

    private static void requireBloodPressureValues(
            BigDecimal systolic,
            BigDecimal diastolic) {

        if (systolic == null) {
            throw new IllegalArgumentException(
                    "Systolic value is required"
            );
        }

        if (diastolic == null) {
            throw new IllegalArgumentException(
                    "Diastolic value is required"
            );
        }
    }


    /* =========================================================
       CONVENIENCE METHODS
       ========================================================= */

    public boolean isPending() {
        return status
                == HealthRecordChangeRequestStatus.PENDING;
    }

    public boolean isCreate() {
        return operation
                == HealthRecordChangeOperation.CREATE;
    }

    public boolean isUpdate() {
        return operation
                == HealthRecordChangeOperation.UPDATE;
    }

    public boolean isScalar() {
        return recordType
                == HealthRecordChangeRecordType.SCALAR;
    }

    public boolean isBloodPressure() {
        return recordType
                == HealthRecordChangeRecordType.BLOOD_PRESSURE;
    }


    /* =========================================================
       GETTERS
       ========================================================= */

    public User getPatient() {
        return patient;
    }

    public User getProvider() {
        return provider;
    }

    public HealthRecordChangeOperation getOperation() {
        return operation;
    }

    public HealthRecordChangeRecordType getRecordType() {
        return recordType;
    }

    public HealthRecordChangeRequestStatus getStatus() {
        return status;
    }

    public Long getTargetRecordId() {
        return targetRecordId;
    }

    public Long getAppliedRecordId() {
        return appliedRecordId;
    }

    public HealthMetricType getOriginalMetricType() {
        return originalMetricType;
    }

    public BigDecimal getOriginalMetricValue() {
        return originalMetricValue;
    }

    public BigDecimal getOriginalSystolic() {
        return originalSystolic;
    }

    public BigDecimal getOriginalDiastolic() {
        return originalDiastolic;
    }

    public LocalDateTime getOriginalObservedAt() {
        return originalObservedAt;
    }

    public String getOriginalNotes() {
        return originalNotes;
    }

    public HealthRecordSource getOriginalSource() {
        return originalSource;
    }

    public Long getOriginalRecordedByUserId() {
        return originalRecordedByUserId;
    }

    public LocalDateTime getOriginalRecordUpdatedAt() {
        return originalRecordUpdatedAt;
    }

    public HealthMetricType getProposedMetricType() {
        return proposedMetricType;
    }

    public BigDecimal getProposedMetricValue() {
        return proposedMetricValue;
    }

    public BigDecimal getProposedSystolic() {
        return proposedSystolic;
    }

    public BigDecimal getProposedDiastolic() {
        return proposedDiastolic;
    }

    public LocalDateTime getProposedObservedAt() {
        return proposedObservedAt;
    }

    public String getProposedNotes() {
        return proposedNotes;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
}