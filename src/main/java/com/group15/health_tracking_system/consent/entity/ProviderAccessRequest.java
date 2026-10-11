package com.group15.health_tracking_system.consent.entity;

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
import java.time.LocalDateTime;

/**
 * Represents a healthcare provider's request for permission to access
 * a patient's health data and analytics.
 *
 * This entity represents a request for permission only. It does not itself
 * grant access. Actual authorization continues to be represented by
 * {@link Consent}.
 *
 * Historical requests are retained even after approval, rejection or
 * cancellation so that the application preserves an audit trail of
 * provider access requests.
 */
@Entity
@Table(
        name = "provider_access_requests",
        indexes = {
            @Index(
                    name = "idx_access_request_patient_status",
                    columnList = "patient_id,status"
            ),
            @Index(
                    name = "idx_access_request_provider_status",
                    columnList = "provider_id,status"
            )
        }
)
public class ProviderAccessRequest extends BaseEntity {

    /**
     * Patient whose health data the provider is requesting access to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "patient_id",
            nullable = false
    )
    private User patient;

    /**
     * Provider requesting access to the patient.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "provider_id",
            nullable = false
    )
    private User provider;

    /**
     * Current lifecycle state of the request.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ProviderAccessRequestStatus status;

    /**
     * Time at which the provider submitted the request.
     */
    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime requestedAt;

    /**
     * Time at which the request reached a terminal state.
     *
     * This is populated when the patient approves or rejects the request,
     * or when the provider cancels it.
     */
    private LocalDateTime respondedAt;

    /**
     * Required by JPA.
     */
    protected ProviderAccessRequest() {
    }

    /**
     * Creates a new pending provider access request.
     *
     * @param patient patient whose data is being requested
     * @param provider provider requesting access
     */
    public ProviderAccessRequest(
            User patient,
            User provider) {

        this.patient = patient;
        this.provider = provider;
        this.status =
                ProviderAccessRequestStatus.PENDING;
        this.requestedAt =
                LocalDateTime.now();
        this.respondedAt =
                null;
    }

    /**
     * Approves this request.
     *
     * Approval of the request does not directly create authorization inside
     * this entity. The service layer will activate the corresponding
     * {@link Consent} in the same transaction.
     */
    public void approve() {

        requirePending();

        this.status =
                ProviderAccessRequestStatus.APPROVED;

        this.respondedAt =
                LocalDateTime.now();
    }

    /**
     * Rejects this request.
     */
    public void reject() {

        requirePending();

        this.status =
                ProviderAccessRequestStatus.REJECTED;

        this.respondedAt =
                LocalDateTime.now();
    }

    /**
     * Cancels this request before the patient has responded.
     *
     * Cancellation is intended for the requesting provider.
     * Authorization of who may invoke this operation is enforced
     * by the service layer.
     */
    public void cancel() {

        requirePending();

        this.status =
                ProviderAccessRequestStatus.CANCELLED;

        this.respondedAt =
                LocalDateTime.now();
    }

    /**
     * Indicates whether this request is still awaiting a response.
     *
     * @return true when the request is pending
     */
    public boolean isPending() {

        return status
                == ProviderAccessRequestStatus.PENDING;
    }

    /**
     * Prevents completed requests from being transitioned a second time.
     */
    private void requirePending() {

        if (!isPending()) {

            throw new IllegalStateException(
                    "Only pending provider access requests can be changed"
            );
        }
    }

    public User getPatient() {
        return patient;
    }

    public User getProvider() {
        return provider;
    }

    public ProviderAccessRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }
}