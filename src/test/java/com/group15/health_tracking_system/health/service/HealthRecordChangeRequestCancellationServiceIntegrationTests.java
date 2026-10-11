package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequest;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequestStatus;
import com.group15.health_tracking_system.health.repository.HealthRecordChangeRequestRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for cancellation of unresolved Provider Health Record
 * proposals after Patient consent is revoked.
 */
@SpringBootTest
@Transactional
class HealthRecordChangeRequestCancellationServiceIntegrationTests {

    @Autowired
    private HealthRecordChangeRequestCancellationService cancellationService;

    @Autowired
    private HealthRecordChangeRequestRepository changeRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patient;
    private User providerA;
    private User providerB;

    private LocalDateTime observationTime;

    @BeforeEach
    void setUp() {

        String passwordHash =
                passwordEncoder.encode(
                        "CancellationTest123!"
                );

        patient =
                saveUser(
                        "CancellationPatient",
                        Role.PATIENT,
                        passwordHash
                );

        providerA =
                saveUser(
                        "CancellationProviderA",
                        Role.PROVIDER,
                        passwordHash
                );

        providerB =
                saveUser(
                        "CancellationProviderB",
                        Role.PROVIDER,
                        passwordHash
                );

        observationTime =
                LocalDateTime.now()
                        .minusDays(1)
                        .withSecond(0)
                        .withNano(0);
    }

    @Test
    void revocationCancelsAllPendingRequestsForExactPatientProviderPair() {

        HealthRecordChangeRequest first =
                scalarCreateProposal(
                        providerA,
                        HealthMetricType.WEIGHT,
                        "78"
                );

        HealthRecordChangeRequest second =
                scalarCreateProposal(
                        providerA,
                        HealthMetricType.HEART_RATE,
                        "72"
                );

        changeRequestRepository.save(
                first
        );

        changeRequestRepository.save(
                second
        );

        int cancelledCount =
                cancellationService
                        .cancelPendingRequestsForRevokedConsent(
                                patient.getId(),
                                providerA.getId()
                        );

        assertEquals(
                2,
                cancelledCount
        );

        HealthRecordChangeRequest reloadedFirst =
                changeRequestRepository
                        .findById(
                                first.getId()
                        )
                        .orElseThrow();

        HealthRecordChangeRequest reloadedSecond =
                changeRequestRepository
                        .findById(
                                second.getId()
                        )
                        .orElseThrow();

        assertEquals(
                HealthRecordChangeRequestStatus.CANCELLED,
                reloadedFirst.getStatus()
        );

        assertEquals(
                HealthRecordChangeRequestStatus.CANCELLED,
                reloadedSecond.getStatus()
        );
    }

    @Test
    void revocationDoesNotCancelAnotherProvidersPendingRequest() {

        HealthRecordChangeRequest providerARequest =
                scalarCreateProposal(
                        providerA,
                        HealthMetricType.WEIGHT,
                        "78"
                );

        HealthRecordChangeRequest providerBRequest =
                scalarCreateProposal(
                        providerB,
                        HealthMetricType.WEIGHT,
                        "79"
                );

        changeRequestRepository.save(
                providerARequest
        );

        changeRequestRepository.save(
                providerBRequest
        );

        int cancelledCount =
                cancellationService
                        .cancelPendingRequestsForRevokedConsent(
                                patient.getId(),
                                providerA.getId()
                        );

        assertEquals(
                1,
                cancelledCount
        );

        HealthRecordChangeRequest reloadedProviderARequest =
                changeRequestRepository
                        .findById(
                                providerARequest.getId()
                        )
                        .orElseThrow();

        HealthRecordChangeRequest reloadedProviderBRequest =
                changeRequestRepository
                        .findById(
                                providerBRequest.getId()
                        )
                        .orElseThrow();

        assertEquals(
                HealthRecordChangeRequestStatus.CANCELLED,
                reloadedProviderARequest.getStatus()
        );

        assertEquals(
                HealthRecordChangeRequestStatus.PENDING,
                reloadedProviderBRequest.getStatus()
        );
    }

    private HealthRecordChangeRequest scalarCreateProposal(
            User provider,
            HealthMetricType metricType,
            String value) {

        return HealthRecordChangeRequest
                .proposeScalarCreate(
                        patient,
                        provider,
                        metricType,
                        new BigDecimal(value),
                        observationTime,
                        "Cancellation test proposal."
                );
    }

    private User saveUser(
            String firstName,
            Role role,
            String passwordHash) {

        return userRepository.save(
                new User(
                        firstName,
                        "Test",
                        "cancellation."
                        + firstName.toLowerCase()
                        + "@example.com",
                        passwordHash,
                        role
                )
        );
    }
}