package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.health.dto.BloodPressureRecordRequest;
import com.group15.health_tracking_system.health.dto.HealthRecordRequest;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequest;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequestStatus;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordChangeRequestRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Provider submission of Health Record
 * change proposals.
 *
 * These tests prove that proposed CREATE and UPDATE operations are stored
 * separately from permanent Health Records until Patient approval.
 */
@SpringBootTest
@Transactional
class HealthRecordChangeRequestServiceIntegrationTests {

    @Autowired
    private HealthRecordChangeRequestService changeRequestService;

    @Autowired
    private HealthRecordChangeRequestRepository changeRequestRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private User patient;
    private User otherPatient;

    private User providerA;
    private User providerB;

    private HealthRecord scalarRecord;
    private HealthRecord otherPatientsScalarRecord;

    private BloodPressureRecord bloodPressureRecord;
    private BloodPressureRecord otherPatientsBloodPressureRecord;

    private LocalDateTime observationTime;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        String passwordHash =
                passwordEncoder.encode(
                        "ProposalTest123!"
                );

        patient =
                saveUser(
                        "ProposalPatient",
                        Role.PATIENT,
                        passwordHash
                );

        otherPatient =
                saveUser(
                        "ProposalOtherPatient",
                        Role.PATIENT,
                        passwordHash
                );

        providerA =
                saveUser(
                        "ProposalProviderA",
                        Role.PROVIDER,
                        passwordHash
                );

        providerB =
                saveUser(
                        "ProposalProviderB",
                        Role.PROVIDER,
                        passwordHash
                );

        observationTime =
                LocalDateTime.now()
                        .minusDays(5)
                        .withSecond(0)
                        .withNano(0);

        scalarRecord =
                saveScalarRecord(
                        patient,
                        "80.0",
                        observationTime
                );

        otherPatientsScalarRecord =
                saveScalarRecord(
                        otherPatient,
                        "95.0",
                        observationTime.plusHours(1)
                );

        bloodPressureRecord =
                saveBloodPressureRecord(
                        patient,
                        "120",
                        "80",
                        observationTime.plusHours(2)
                );

        otherPatientsBloodPressureRecord =
                saveBloodPressureRecord(
                        otherPatient,
                        "140",
                        "90",
                        observationTime.plusHours(3)
                );

        healthRecordRepository.flush();
        bloodPressureRecordRepository.flush();
        userRepository.flush();

        /*
         * Ensure subsequent service calls operate against persisted
         * database state rather than relying on the first-level cache.
         */
        entityManager.clear();
    }

    @AfterEach
    void clearAuthentication() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void scalarCreateProposalIsPendingWithoutCreatingPermanentRecord() {

        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerA
        );

        long permanentCountBefore =
                healthRecordRepository.count();

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("78.5"),
                        observationTime.plusDays(1),
                        "  Provider proposed measurement.  "
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarCreate(
                                patient.getId(),
                                request
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.PENDING,
                proposal.getStatus()
        );

        assertEquals(
                patient.getId(),
                proposal.getPatient().getId()
        );

        assertEquals(
                providerA.getId(),
                proposal.getProvider().getId()
        );

        /*
         * Critical rule:
         * proposal creation must not create a permanent HealthRecord.
         */
        assertEquals(
                permanentCountBefore,
                healthRecordRepository.count()
        );

        assertEquals(
                1,
                changeRequestRepository.count()
        );
    }

    @Test
    void bloodPressureCreateProposalIsPendingWithoutCreatingPermanentRecord() {

        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerA
        );

        long permanentCountBefore =
                bloodPressureRecordRepository.count();

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("118"),
                        new BigDecimal("76"),
                        observationTime.plusDays(1),
                        "Provider proposed BP observation."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeBloodPressureCreate(
                                patient.getId(),
                                request
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.PENDING,
                proposal.getStatus()
        );

        assertEquals(
                patient.getId(),
                proposal.getPatient().getId()
        );

        assertEquals(
                providerA.getId(),
                proposal.getProvider().getId()
        );

        /*
         * Critical rule:
         * no permanent paired BP observation exists until approval.
         */
        assertEquals(
                permanentCountBefore,
                bloodPressureRecordRepository.count()
        );

        assertEquals(
                1,
                changeRequestRepository.count()
        );
    }

    @Test
    void scalarUpdateProposalDoesNotModifyPermanentRecord() {

        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerA
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("77.0"),
                        observationTime,
                        "Proposed correction."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarUpdate(
                                patient.getId(),
                                scalarRecord.getId(),
                                request
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.PENDING,
                proposal.getStatus()
        );

        assertEquals(
                scalarRecord.getId(),
                proposal.getTargetRecordId()
        );

        entityManager.flush();
        entityManager.clear();

        HealthRecord unchanged =
                healthRecordRepository
                        .findById(
                                scalarRecord.getId()
                        )
                        .orElseThrow();

        assertBigDecimalEquals(
                "80.0",
                unchanged.getMetricValue()
        );

        assertEquals(
                HealthRecordSource.PATIENT_REPORTED,
                unchanged.getSource()
        );

        assertEquals(
                patient.getId(),
                unchanged.getRecordedBy().getId()
        );
    }

    @Test
    void bloodPressureUpdateProposalDoesNotModifyPermanentRecord() {

        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerA
        );

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("116"),
                        new BigDecimal("74"),
                        bloodPressureRecord.getObservedAt(),
                        "Proposed BP correction."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeBloodPressureUpdate(
                                patient.getId(),
                                bloodPressureRecord.getId(),
                                request
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.PENDING,
                proposal.getStatus()
        );

        assertEquals(
                bloodPressureRecord.getId(),
                proposal.getTargetRecordId()
        );

        entityManager.flush();
        entityManager.clear();

        BloodPressureRecord unchanged =
                bloodPressureRecordRepository
                        .findById(
                                bloodPressureRecord.getId()
                        )
                        .orElseThrow();

        assertBigDecimalEquals(
                "120",
                unchanged.getSystolic()
        );

        assertBigDecimalEquals(
                "80",
                unchanged.getDiastolic()
        );

        assertEquals(
                HealthRecordSource.PATIENT_REPORTED,
                unchanged.getSource()
        );
    }

    @Test
    void missingConsentBlocksProposalAndPersistsNothing() {

        logInAs(
                providerA
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("72"),
                        observationTime,
                        null
                );

        assertThrows(
                AccessDeniedException.class,
                () ->
                        changeRequestService
                                .proposeScalarCreate(
                                        patient.getId(),
                                        request
                                )
        );

        assertEquals(
                0,
                changeRequestRepository.count()
        );
    }

    @Test
    void anotherProviderCannotReuseExistingConsent() {

        /*
         * Consent belongs specifically to Provider A.
         */
        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerB
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("72"),
                        observationTime,
                        null
                );

        assertThrows(
                AccessDeniedException.class,
                () ->
                        changeRequestService
                                .proposeScalarCreate(
                                        patient.getId(),
                                        request
                                )
        );

        assertEquals(
                0,
                changeRequestRepository.count()
        );
    }

    @Test
    void authorizedPatientIdCannotBeCombinedWithAnotherPatientsRecordId() {

        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerA
        );

        /*
         * Provider has access to patient, but supplies a HealthRecord
         * identifier belonging to otherPatient.
         */
        HealthRecordRequest scalarRequest =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("90"),
                        observationTime,
                        null
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        changeRequestService
                                .proposeScalarUpdate(
                                        patient.getId(),
                                        otherPatientsScalarRecord.getId(),
                                        scalarRequest
                                )
        );

        /*
         * Perform the same ownership attack against Blood Pressure.
         */
        BloodPressureRecordRequest bloodPressureRequest =
                new BloodPressureRecordRequest(
                        new BigDecimal("130"),
                        new BigDecimal("85"),
                        observationTime,
                        null
                );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        changeRequestService
                                .proposeBloodPressureUpdate(
                                        patient.getId(),
                                        otherPatientsBloodPressureRecord.getId(),
                                        bloodPressureRequest
                                )
        );

        assertEquals(
                0,
                changeRequestRepository.count()
        );
    }

    @Test
    void duplicatePendingUpdateFromSameProviderIsBlocked() {

        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerA
        );

        HealthRecordRequest firstRequest =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("79"),
                        observationTime,
                        "First proposal"
                );

        HealthRecordRequest secondRequest =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("78"),
                        observationTime,
                        "Duplicate proposal"
                );

        changeRequestService
                .proposeScalarUpdate(
                        patient.getId(),
                        scalarRecord.getId(),
                        firstRequest
                );

        assertThrows(
                IllegalStateException.class,
                () ->
                        changeRequestService
                                .proposeScalarUpdate(
                                        patient.getId(),
                                        scalarRecord.getId(),
                                        secondRequest
                                )
        );

        assertEquals(
                1,
                changeRequestRepository.count()
        );
    }

    @Test
    void revokingConsentImmediatelyBlocksNewProposalsWithoutRelogin() {

        consentService.grantConsent(
                patient.getId(),
                providerA.getId()
        );

        logInAs(
                providerA
        );

        HealthRecordRequest firstRequest =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("70"),
                        observationTime,
                        null
                );

        changeRequestService
                .proposeScalarCreate(
                        patient.getId(),
                        firstRequest
                );

        /*
         * Same Provider session; only Consent changes.
         */
        consentService.revokeConsent(
                patient.getId(),
                providerA.getId()
        );

        HealthRecordRequest secondRequest =
                new HealthRecordRequest(
                        HealthMetricType.BLOOD_GLUCOSE,
                        new BigDecimal("5.5"),
                        observationTime,
                        null
                );

        assertThrows(
                AccessDeniedException.class,
                () ->
                        changeRequestService
                                .proposeScalarCreate(
                                        patient.getId(),
                                        secondRequest
                                )
        );

        /*
         * Only the proposal created before revocation exists.
         */
        assertEquals(
                1,
                changeRequestRepository.count()
        );
    }

    @Test
    void patientCannotSubmitProviderChangeProposal() {

        logInAs(
                patient
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("79"),
                        observationTime,
                        null
                );

        assertThrows(
                AccessDeniedException.class,
                () ->
                        changeRequestService
                                .proposeScalarCreate(
                                        patient.getId(),
                                        request
                                )
        );

        assertEquals(
                0,
                changeRequestRepository.count()
        );
    }


    /* =========================================================
       TEST HELPERS
       ========================================================= */

    private User saveUser(
            String name,
            Role role,
            String passwordHash) {

        return userRepository.save(
                new User(
                        name,
                        "Test",
                        "healthproposal."
                        + name.toLowerCase()
                        + "@example.com",
                        passwordHash,
                        role
                )
        );
    }

    private HealthRecord saveScalarRecord(
            User owner,
            String value,
            LocalDateTime observedAt) {

        return healthRecordRepository.save(
                new HealthRecord(
                        owner,
                        owner,
                        HealthMetricType.WEIGHT,
                        new BigDecimal(value),
                        observedAt,
                        "Original scalar record.",
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }

    private BloodPressureRecord saveBloodPressureRecord(
            User owner,
            String systolic,
            String diastolic,
            LocalDateTime observedAt) {

        return bloodPressureRecordRepository.save(
                new BloodPressureRecord(
                        owner,
                        owner,
                        new BigDecimal(systolic),
                        new BigDecimal(diastolic),
                        observedAt,
                        "Original BP record.",
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }

    private void logInAs(
            User account) {

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        account.getEmail(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_"
                                        + account.getRole().name()
                                )
                        )
                );

        SecurityContext context =
                SecurityContextHolder
                        .createEmptyContext();

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                context
        );
    }

    private void assertBigDecimalEquals(
            String expected,
            BigDecimal actual) {

        assertEquals(
                0,
                new BigDecimal(expected)
                        .compareTo(actual)
        );
    }
}