package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
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
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProviderHealthRecordServiceIntegrationTests {

    @Autowired
    private ProviderHealthRecordService recordService;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    private User patient;
    private User otherPatient;
    private User providerA;
    private User providerB;

    private HealthRecord olderRecord;
    private HealthRecord newerRecord;
    private HealthRecord otherPatientsRecord;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        String passwordHash = passwordEncoder.encode("RecordTest123!");

        patient = saveUser("Patient", Role.PATIENT, passwordHash);
        otherPatient = saveUser("OtherPatient", Role.PATIENT, passwordHash);
        providerA = saveUser("ProviderA", Role.PROVIDER, passwordHash);
        providerB = saveUser("ProviderB", Role.PROVIDER, passwordHash);

        LocalDateTime firstObservation =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        olderRecord = saveRecord(
                patient,
                "70.5",
                firstObservation
        );

        newerRecord = saveRecord(
                patient,
                "71.5",
                firstObservation.plusDays(1)
        );

        otherPatientsRecord = saveRecord(
                otherPatient,
                "90.0",
                firstObservation.plusDays(2)
        );

        healthRecordRepository.flush();

        // Ensure subsequent operations retrieve saved database data.
        entityManager.clear();
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void activeConsentAllowsOnlyTheSelectedPatientsHistory() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        List<HealthRecord> records =
                recordService.getPatientHistory(patient.getId());

        assertEquals(
                List.of(newerRecord.getId(), olderRecord.getId()),
                records.stream().map(HealthRecord::getId).toList()
        );

        assertEquals(
                patient.getId(),
                recordService.getPatient(patient.getId()).getId()
        );
    }

    @Test
    void activeConsentAllowsAnIndividualRecord() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        HealthRecord record = recordService.getPatientRecord(
                patient.getId(),
                newerRecord.getId()
        );

        assertEquals(newerRecord.getId(), record.getId());
        assertEquals(patient.getId(), record.getPatient().getId());

        assertEquals(
                0,
                new BigDecimal("71.5").compareTo(record.getMetricValue())
        );
    }

    @Test
    void missingConsentBlocksPatientDetailsHistoryAndRecords() {
        logInAs(providerA);

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatient(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientHistory(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientRecord(
                        patient.getId(),
                        newerRecord.getId()
                )
        );
    }

    @Test
    void anotherProviderCannotReuseTheFirstProvidersConsent() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerB);

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientHistory(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientRecord(
                        patient.getId(),
                        newerRecord.getId()
                )
        );
    }

    @Test
    void consentForOnePatientDoesNotAllowAnotherPatientsRecords() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientHistory(otherPatient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientRecord(
                        otherPatient.getId(),
                        otherPatientsRecord.getId()
                )
        );
    }

    @Test
    void changingTheRecordIdCannotExposeAnotherPatientsRecord() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        // Keep the authorised patient ID but substitute another
        // patient's record ID. The shared query must reject it.
        assertThrows(ResourceNotFoundException.class, () ->
                recordService.getPatientRecord(
                        patient.getId(),
                        otherPatientsRecord.getId()
                )
        );
    }

    @Test
    void revokingConsentBlocksRecordsWithoutLoggingInAgain() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        assertEquals(
                2,
                recordService.getPatientHistory(patient.getId()).size()
        );

        assertEquals(
                newerRecord.getId(),
                recordService.getPatientRecord(
                        patient.getId(),
                        newerRecord.getId()
                ).getId()
        );

        consentService.revokeConsent(patient.getId(), providerA.getId());

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientHistory(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientRecord(
                        patient.getId(),
                        newerRecord.getId()
                )
        );
    }

    @Test
    void patientCannotUseTheProviderRecordService() {
        logInAs(patient);

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientHistory(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientRecord(
                        patient.getId(),
                        newerRecord.getId()
                )
        );
    }

    @Test
    void unauthenticatedCallerCannotReadHealthRecords() {
        SecurityContextHolder.clearContext();

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                recordService.getPatientHistory(patient.getId())
        );

        assertThrows(AuthenticationCredentialsNotFoundException.class, () ->
                recordService.getPatientRecord(
                        patient.getId(),
                        newerRecord.getId()
                )
        );
    }

    @Test
    void disabledProviderCannotUseExistingConsent() {
        consentService.grantConsent(patient.getId(), providerA.getId());
        logInAs(providerA);

        providerA.setEnabled(false);
        userRepository.saveAndFlush(providerA);

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientHistory(patient.getId())
        );

        assertThrows(AccessDeniedException.class, () ->
                recordService.getPatientRecord(
                        patient.getId(),
                        newerRecord.getId()
                )
        );
    }

    private User saveUser(
            String name,
            Role role,
            String passwordHash
    ) {
        return userRepository.save(new User(
                name,
                "Test",
                "recordtest." + name + "@example.com",
                passwordHash,
                role
        ));
    }

    private HealthRecord saveRecord(
            User owner,
            String value,
            LocalDateTime observedAt
    ) {
        return healthRecordRepository.save(new HealthRecord(
                owner,
                owner,
                HealthMetricType.WEIGHT,
                new BigDecimal(value),
                observedAt,
                "Sample record for integration testing.",
                HealthRecordSource.PATIENT_REPORTED
        ));
    }

    private void logInAs(User account) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        account.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority(
                                "ROLE_" + account.getRole().name()
                        ))
                );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}