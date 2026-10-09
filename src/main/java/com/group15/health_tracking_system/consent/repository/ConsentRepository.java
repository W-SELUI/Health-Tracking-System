package com.group15.health_tracking_system.consent.repository;

import com.group15.health_tracking_system.consent.entity.Consent;
import com.group15.health_tracking_system.consent.entity.ConsentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = "provider")
    List<Consent> findByPatient_Id(Long patientId);

    @EntityGraph(attributePaths = "patient")
    List<Consent> findByProvider_IdAndStatus(
            Long providerId,
            ConsentStatus status
    );
}