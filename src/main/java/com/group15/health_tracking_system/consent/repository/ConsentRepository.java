package com.group15.health_tracking_system.consent.repository;

import com.group15.health_tracking_system.consent.entity.Consent;
import com.group15.health_tracking_system.consent.entity.ConsentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, Long> {

    Optional<Consent> findByPatient_IdAndProvider_Id(
            Long patientId,
            Long providerId
    );

    Optional<Consent> findByPatient_IdAndProvider_IdAndStatus(
            Long patientId,
            Long providerId,
            ConsentStatus status
    );
}