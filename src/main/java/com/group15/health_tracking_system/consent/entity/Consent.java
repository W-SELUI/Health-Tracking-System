package com.group15.health_tracking_system.consent.entity;

import com.group15.health_tracking_system.common.model.BaseEntity;
import com.group15.health_tracking_system.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "consents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_consent_patient_provider",
                columnNames = {"patient_id", "provider_id"}
        )
)
public class Consent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private User provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentStatus status;

    @Column(nullable = false)
    private LocalDateTime grantedAt;

    private LocalDateTime revokedAt;

    protected Consent() {
        // Required by JPA.
    }

    public Consent(User patient, User provider) {
        this.patient = patient;
        this.provider = provider;
        this.status = ConsentStatus.ACTIVE;
        this.grantedAt = LocalDateTime.now();
    }

    public void activate() {
        this.status = ConsentStatus.ACTIVE;
        this.grantedAt = LocalDateTime.now();
        this.revokedAt = null;
    }

    public void revoke() {
        this.status = ConsentStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == ConsentStatus.ACTIVE;
    }

    public User getPatient() {
        return patient;
    }

    public User getProvider() {
        return provider;
    }

    public ConsentStatus getStatus() {
        return status;
    }

    public LocalDateTime getGrantedAt() {
        return grantedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }
}