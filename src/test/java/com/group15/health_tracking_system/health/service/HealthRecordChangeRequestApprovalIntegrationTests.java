package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Patient review of Provider-proposed
 * Health Record changes.
 *
 * These tests prove that approval applies permanent changes only when
 * the proposal is still valid, rejection leaves permanent data unchanged,
 * stale proposals cannot overwrite newer data, and proposal ownership is
 * enforced at the Patient boundary.
 */
@SpringBootTest
@Transactional
class HealthRecordChangeRequestApprovalIntegrationTests {

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
    private User provider;

    private HealthRecord scalarRecord;
    private BloodPressureRecord bloodPressureRecord;

    private LocalDateTime observationTime;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        String passwordHash =
                passwordEncoder.encode(
                        "ProposalApproval123!"
                );

        patient =
                saveUser(
                        "ApprovalPatient",
                        Role.PATIENT,
                        passwordHash
                );

        otherPatient =
                saveUser(
                        "ApprovalOtherPatient",
                        Role.PATIENT,
                        passwordHash
                );

        provider =
                saveUser(
                        "ApprovalProvider",
                        Role.PROVIDER,
                        passwordHash
                );

        observationTime =
                LocalDateTime.now()
                        .minusDays(5)
                        .withSecond(0)
                        .withNano(0);

        scalarRecord =
                healthRecordRepository.save(
                        new HealthRecord(
                                patient,
                                patient,
                                HealthMetricType.WEIGHT,
                                new BigDecimal("80.0"),
                                observationTime,
                                "Original weight.",
                                HealthRecordSource.PATIENT_REPORTED
                        )
                );

        bloodPressureRecord =
                bloodPressureRecordRepository.save(
                        new BloodPressureRecord(
                                patient,
                                patient,
                                new BigDecimal("120"),
                                new BigDecimal("80"),
                                observationTime.plusHours(1),
                                "Original blood pressure.",
                                HealthRecordSource.PATIENT_REPORTED
                        )
                );

        userRepository.flush();
        healthRecordRepository.flush();
        bloodPressureRecordRepository.flush();

        entityManager.clear();
    }

    @AfterEach
    void clearAuthentication() {

        SecurityContextHolder.clearContext();
    }


    /* =========================================================
       CREATE APPROVAL
       ========================================================= */

    @Test
    void approvingScalarCreateCreatesProviderRecordedPermanentRecord() {

        grantConsent();

        logInAs(
                provider
        );

        long permanentCountBefore =
                healthRecordRepository.count();

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("72"),
                        observationTime.plusDays(1),
                        "Provider recorded heart rate."
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

        /*
         * Proposal submission itself must still have no permanent effect.
         */
        assertEquals(
                permanentCountBefore,
                healthRecordRepository.count()
        );

        logInAs(
                patient
        );

        HealthRecordChangeRequest approved =
                changeRequestService
                        .approveRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.APPROVED,
                approved.getStatus()
        );

        assertNotNull(
                approved.getAppliedRecordId()
        );

        assertEquals(
                permanentCountBefore + 1,
                healthRecordRepository.count()
        );

        HealthRecord created =
                healthRecordRepository
                        .findById(
                                approved.getAppliedRecordId()
                        )
                        .orElseThrow();

        assertEquals(
                patient.getId(),
                created.getPatient().getId()
        );

        assertEquals(
                provider.getId(),
                created.getRecordedBy().getId()
        );

        assertEquals(
                HealthRecordSource.PROVIDER_RECORDED,
                created.getSource()
        );

        assertEquals(
                HealthMetricType.HEART_RATE,
                created.getMetricType()
        );

        assertBigDecimalEquals(
                "72",
                created.getMetricValue()
        );
    }

    @Test
    void approvingBloodPressureCreateCreatesOnePairedProviderRecord() {

        grantConsent();

        logInAs(
                provider
        );

        long permanentCountBefore =
                bloodPressureRecordRepository.count();

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("118"),
                        new BigDecimal("76"),
                        observationTime.plusDays(1),
                        "Provider BP reading."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeBloodPressureCreate(
                                patient.getId(),
                                request
                        );

        assertEquals(
                permanentCountBefore,
                bloodPressureRecordRepository.count()
        );

        logInAs(
                patient
        );

        HealthRecordChangeRequest approved =
                changeRequestService
                        .approveRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.APPROVED,
                approved.getStatus()
        );

        assertNotNull(
                approved.getAppliedRecordId()
        );

        assertEquals(
                permanentCountBefore + 1,
                bloodPressureRecordRepository.count()
        );

        BloodPressureRecord created =
                bloodPressureRecordRepository
                        .findById(
                                approved.getAppliedRecordId()
                        )
                        .orElseThrow();

        assertBigDecimalEquals(
                "118",
                created.getSystolic()
        );

        assertBigDecimalEquals(
                "76",
                created.getDiastolic()
        );

        assertEquals(
                provider.getId(),
                created.getRecordedBy().getId()
        );

        assertEquals(
                HealthRecordSource.PROVIDER_RECORDED,
                created.getSource()
        );
    }


    /* =========================================================
       UPDATE APPROVAL
       ========================================================= */

    @Test
    void approvingScalarUpdateChangesValuesButPreservesProvenance() {

        grantConsent();

        logInAs(
                provider
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("78.0"),
                        observationTime.plusHours(2),
                        "Provider proposed correction."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarUpdate(
                                patient.getId(),
                                scalarRecord.getId(),
                                request
                        );

        logInAs(
                patient
        );

        HealthRecordChangeRequest approved =
                changeRequestService
                        .approveRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.APPROVED,
                approved.getStatus()
        );

        assertEquals(
                scalarRecord.getId(),
                approved.getAppliedRecordId()
        );

        entityManager.flush();
        entityManager.clear();

        HealthRecord updated =
                healthRecordRepository
                        .findById(
                                scalarRecord.getId()
                        )
                        .orElseThrow();

        assertBigDecimalEquals(
                "78.0",
                updated.getMetricValue()
        );

        assertEquals(
                observationTime.plusHours(2),
                updated.getObservedAt()
        );

        assertEquals(
                "Provider proposed correction.",
                updated.getNotes()
        );

        /*
         * An approved correction does not rewrite historical provenance.
         */
        assertEquals(
                HealthRecordSource.PATIENT_REPORTED,
                updated.getSource()
        );

        assertEquals(
                patient.getId(),
                updated.getRecordedBy().getId()
        );
    }

    @Test
    void approvingBloodPressureUpdateChangesPairAndPreservesProvenance() {

        grantConsent();

        logInAs(
                provider
        );

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("116"),
                        new BigDecimal("74"),
                        observationTime.plusHours(3),
                        "Provider proposed BP correction."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeBloodPressureUpdate(
                                patient.getId(),
                                bloodPressureRecord.getId(),
                                request
                        );

        logInAs(
                patient
        );

        HealthRecordChangeRequest approved =
                changeRequestService
                        .approveRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.APPROVED,
                approved.getStatus()
        );

        assertEquals(
                bloodPressureRecord.getId(),
                approved.getAppliedRecordId()
        );

        entityManager.flush();
        entityManager.clear();

        BloodPressureRecord updated =
                bloodPressureRecordRepository
                        .findById(
                                bloodPressureRecord.getId()
                        )
                        .orElseThrow();

        assertBigDecimalEquals(
                "116",
                updated.getSystolic()
        );

        assertBigDecimalEquals(
                "74",
                updated.getDiastolic()
        );

        assertEquals(
                HealthRecordSource.PATIENT_REPORTED,
                updated.getSource()
        );

        assertEquals(
                patient.getId(),
                updated.getRecordedBy().getId()
        );
    }


    /* =========================================================
       REJECTION
       ========================================================= */

    @Test
    void rejectingProposalLeavesPermanentDataUntouched() {

        grantConsent();

        logInAs(
                provider
        );

        long countBefore =
                healthRecordRepository.count();

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("75"),
                        observationTime.plusDays(1),
                        null
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarCreate(
                                patient.getId(),
                                request
                        );

        logInAs(
                patient
        );

        HealthRecordChangeRequest rejected =
                changeRequestService
                        .rejectRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.REJECTED,
                rejected.getStatus()
        );

        assertEquals(
                countBefore,
                healthRecordRepository.count()
        );

        assertEquals(
                null,
                rejected.getAppliedRecordId()
        );
    }


    /* =========================================================
       STALE PROTECTION
       ========================================================= */

    @Test
    void scalarUpdateBecomesStaleWhenRecordChangedAfterProposal() {

        grantConsent();

        logInAs(
                provider
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("78"),
                        observationTime,
                        "Old Provider proposal."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarUpdate(
                                patient.getId(),
                                scalarRecord.getId(),
                                request
                        );

        /*
         * Simulate the Patient changing the permanent record while the
         * Provider proposal is still waiting.
         */
        HealthRecord changed =
                healthRecordRepository
                        .findById(
                                scalarRecord.getId()
                        )
                        .orElseThrow();

        changed.setMetricValue(
                new BigDecimal("79")
        );

        changed.setNotes(
                "Patient made a newer change."
        );

        healthRecordRepository.saveAndFlush(
                changed
        );

        entityManager.clear();

        logInAs(
                patient
        );

        HealthRecordChangeRequest result =
                changeRequestService
                        .approveRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.STALE,
                result.getStatus()
        );

        entityManager.flush();
        entityManager.clear();

        HealthRecord permanent =
                healthRecordRepository
                        .findById(
                                scalarRecord.getId()
                        )
                        .orElseThrow();

        /*
         * The old Provider proposal must not overwrite the newer value.
         */
        assertBigDecimalEquals(
                "79",
                permanent.getMetricValue()
        );

        assertEquals(
                "Patient made a newer change.",
                permanent.getNotes()
        );
    }

    @Test
    void bloodPressureUpdateBecomesStaleWhenRecordChangedAfterProposal() {

        grantConsent();

        logInAs(
                provider
        );

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("115"),
                        new BigDecimal("75"),
                        observationTime,
                        "Old BP proposal."
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeBloodPressureUpdate(
                                patient.getId(),
                                bloodPressureRecord.getId(),
                                request
                        );

        BloodPressureRecord changed =
                bloodPressureRecordRepository
                        .findById(
                                bloodPressureRecord.getId()
                        )
                        .orElseThrow();

        changed.setSystolic(
                new BigDecimal("119")
        );

        changed.setDiastolic(
                new BigDecimal("79")
        );

        bloodPressureRecordRepository.saveAndFlush(
                changed
        );

        entityManager.clear();

        logInAs(
                patient
        );

        HealthRecordChangeRequest result =
                changeRequestService
                        .approveRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.STALE,
                result.getStatus()
        );

        entityManager.flush();
        entityManager.clear();

        BloodPressureRecord permanent =
                bloodPressureRecordRepository
                        .findById(
                                bloodPressureRecord.getId()
                        )
                        .orElseThrow();

        assertBigDecimalEquals(
                "119",
                permanent.getSystolic()
        );

        assertBigDecimalEquals(
                "79",
                permanent.getDiastolic()
        );
    }

    @Test
    void deletedUpdateTargetBecomesStaleInsteadOfBeingRecreated() {

        grantConsent();

        logInAs(
                provider
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.WEIGHT,
                        new BigDecimal("77"),
                        observationTime,
                        null
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarUpdate(
                                patient.getId(),
                                scalarRecord.getId(),
                                request
                        );

        healthRecordRepository.deleteById(
                scalarRecord.getId()
        );

        healthRecordRepository.flush();

        entityManager.clear();

        logInAs(
                patient
        );

        HealthRecordChangeRequest result =
                changeRequestService
                        .approveRequest(
                                proposal.getId()
                        );

        assertEquals(
                HealthRecordChangeRequestStatus.STALE,
                result.getStatus()
        );

        assertEquals(
                false,
                healthRecordRepository
                        .existsById(
                                scalarRecord.getId()
                        )
        );
    }


    /* =========================================================
       OWNERSHIP / LIFECYCLE PROTECTION
       ========================================================= */

    @Test
    void anotherPatientCannotApproveSomeoneElsesProposal() {

        grantConsent();

        logInAs(
                provider
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("70"),
                        observationTime,
                        null
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarCreate(
                                patient.getId(),
                                request
                        );

        logInAs(
                otherPatient
        );

        assertThrows(
                ResourceNotFoundException.class,
                () ->
                        changeRequestService
                                .approveRequest(
                                        proposal.getId()
                                )
        );

        HealthRecordChangeRequest unchanged =
                changeRequestRepository
                        .findById(
                                proposal.getId()
                        )
                        .orElseThrow();

        assertEquals(
                HealthRecordChangeRequestStatus.PENDING,
                unchanged.getStatus()
        );
    }

    @Test
    void approvedProposalCannotBeApprovedTwice() {

        grantConsent();

        logInAs(
                provider
        );

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("72"),
                        observationTime,
                        null
                );

        HealthRecordChangeRequest proposal =
                changeRequestService
                        .proposeScalarCreate(
                                patient.getId(),
                                request
                        );

        logInAs(
                patient
        );

        changeRequestService
                .approveRequest(
                        proposal.getId()
                );

        long permanentCountAfterFirstApproval =
                healthRecordRepository.count();

        assertThrows(
                IllegalStateException.class,
                () ->
                        changeRequestService
                                .approveRequest(
                                        proposal.getId()
                                )
        );

        /*
         * No second permanent record is created.
         */
        assertEquals(
                permanentCountAfterFirstApproval,
                healthRecordRepository.count()
        );
    }

    @Test
    void revokingConsentImmediatelyCancelsPendingProposal() {

        grantConsent();

        logInAs(
                provider
        );

        long permanentCountBefore =
                healthRecordRepository.count();

        HealthRecordRequest request =
                new HealthRecordRequest(
                        HealthMetricType.HEART_RATE,
                        new BigDecimal("68"),
                        observationTime.plusDays(1),
                        null
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

        /*
         * Revocation itself must immediately cancel every unresolved
         * proposal for this Patient/Provider relationship.
         */
        consentService.revokeConsent(
                patient.getId(),
                provider.getId()
        );

        entityManager.flush();
        entityManager.clear();

        HealthRecordChangeRequest cancelled =
                changeRequestRepository
                        .findById(
                                proposal.getId()
                        )
                        .orElseThrow();

        assertEquals(
                HealthRecordChangeRequestStatus.CANCELLED,
                cancelled.getStatus()
        );

        assertNotNull(
                cancelled.getRespondedAt()
        );

        assertEquals(
                null,
                cancelled.getAppliedRecordId()
        );

        /*
         * Revocation itself must never apply the proposed record.
         */
        assertEquals(
                permanentCountBefore,
                healthRecordRepository.count()
        );
    }


    /* =========================================================
       HELPERS
       ========================================================= */

    private void grantConsent() {

        consentService.grantConsent(
                patient.getId(),
                provider.getId()
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
                        "approval."
                        + firstName.toLowerCase()
                        + "@example.com",
                        passwordHash,
                        role
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