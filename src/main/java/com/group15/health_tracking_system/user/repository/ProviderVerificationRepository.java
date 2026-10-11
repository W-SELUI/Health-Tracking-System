package com.group15.health_tracking_system.user.repository;

import com.group15.health_tracking_system.user.entity.ProviderVerification;
import com.group15.health_tracking_system.user.entity.VerificationStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderVerificationRepository
        extends JpaRepository<ProviderVerification, Long> {

    Optional<ProviderVerification>
            findByProvider_Id(
                    Long providerId);

    List<ProviderVerification>
            findByStatusOrderByCreatedAtDesc(
                    VerificationStatus status);

    List<ProviderVerification>
            findAllByOrderByCreatedAtDesc();

    boolean existsByProvider_Id(
            Long providerId);
}