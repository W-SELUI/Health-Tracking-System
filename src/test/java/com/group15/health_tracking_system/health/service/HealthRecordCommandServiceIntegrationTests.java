package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.dto.HealthRecordRequest;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class HealthRecordCommandServiceIntegrationTests {

    @Autowired
    private HealthRecordCommandService healthRecordCommandService;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private User patientOne;
    private User patientTwo;
    private User provider;

    @BeforeEach
    void setUp() {

        healthRecordRepository.deleteAll();
        userRepository.deleteAll();

        patientOne = saveUser(
                "Patient",
                "One",
                "patient1@example.com",
                Role.PATIENT
        );

        patientTwo = saveUser(
                "Patient",
                "Two",
                "patient2@example.com",
                Role.PATIENT
        );

        provider = saveUser(
                "Provider",
                "User",
                "provider@example.com",
                Role.PROVIDER
        );
    }

    @Test
    void patientCanCreateOwnHealthRecord() {

        HealthRecordRequest request = new HealthRecordRequest(
                HealthMetricType.HEART_RATE,
                new BigDecimal("72"),
                LocalDateTime.of(2026, 10, 9, 9, 30),
                "  Morning reading  "
        );

        HealthRecord saved =
                healthRecordCommandService.createPatientRecord(
                        patientOne,
                        request
                );

        assertNotNull(saved.getId());

        assertEquals(
                patientOne.getId(),
                saved.getPatient().getId()
        );

        assertEquals(
                patientOne.getId(),
                saved.getRecordedBy().getId()
        );

        assertEquals(
                HealthRecordSource.PATIENT_REPORTED,
                saved.getSource()
        );

        assertEquals(
                HealthMetricType.HEART_RATE,
                saved.getMetricType()
        );

        assertEquals(
                0,
                saved.getMetricValue()
                        .compareTo(new BigDecimal("72"))
        );

        assertEquals("Morning reading", saved.getNotes());
        assertEquals("bpm", saved.getUnit());
    }

    @Test
    void patientCanUpdateOwnPatientReportedRecord() {

        HealthRecord record =
                savePatientRecord(
                        patientOne,
                        HealthMetricType.WEIGHT,
                        "75.0"
                );

        Long originalPatientId =
                record.getPatient().getId();

        Long originalRecordedById =
                record.getRecordedBy().getId();

        HealthRecordSource originalSource =
                record.getSource();

        HealthRecordRequest request = new HealthRecordRequest(
                HealthMetricType.WEIGHT,
                new BigDecimal("74.5"),
                LocalDateTime.of(2026, 10, 9, 10, 0),
                "Updated weight"
        );

        HealthRecord updated =
                healthRecordCommandService.updatePatientRecord(
                        record.getId(),
                        patientOne,
                        request
                );

        assertEquals(
                0,
                updated.getMetricValue()
                        .compareTo(new BigDecimal("74.5"))
        );

        assertEquals(
                "Updated weight",
                updated.getNotes()
        );

        // Ownership and provenance must not change.
        assertEquals(
                originalPatientId,
                updated.getPatient().getId()
        );

        assertEquals(
                originalRecordedById,
                updated.getRecordedBy().getId()
        );

        assertEquals(
                originalSource,
                updated.getSource()
        );
    }

    @Test
    void patientCanDeleteOwnPatientReportedRecord() {

        HealthRecord record =
                savePatientRecord(
                        patientOne,
                        HealthMetricType.BODY_TEMPERATURE,
                        "36.7"
                );

        Long recordId = record.getId();

        healthRecordCommandService.deletePatientRecord(
                recordId,
                patientOne
        );

        assertFalse(
                healthRecordRepository
                        .findById(recordId)
                        .isPresent()
        );
    }

    @Test
    void patientCannotModifyAnotherPatientsRecord() {

        HealthRecord otherPatientsRecord =
                savePatientRecord(
                        patientTwo,
                        HealthMetricType.OXYGEN_SATURATION,
                        "98"
                );

        HealthRecordRequest request = new HealthRecordRequest(
                HealthMetricType.OXYGEN_SATURATION,
                new BigDecimal("97"),
                LocalDateTime.of(2026, 10, 9, 11, 0),
                null
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> healthRecordCommandService
                        .updatePatientRecord(
                                otherPatientsRecord.getId(),
                                patientOne,
                                request
                        )
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> healthRecordCommandService
                        .deletePatientRecord(
                                otherPatientsRecord.getId(),
                                patientOne
                        )
        );
    }

    @Test
    void patientCannotModifyProviderRecordedRecord() {

        HealthRecord providerRecord = new HealthRecord(
                patientOne,
                provider,
                HealthMetricType.BLOOD_GLUCOSE,
                new BigDecimal("5.5"),
                LocalDateTime.of(2026, 10, 9, 12, 0),
                "Recorded by provider",
                HealthRecordSource.PROVIDER_RECORDED
        );

        providerRecord =
                healthRecordRepository.save(providerRecord);

        Long recordId = providerRecord.getId();

        HealthRecordRequest request = new HealthRecordRequest(
                HealthMetricType.BLOOD_GLUCOSE,
                new BigDecimal("6.0"),
                LocalDateTime.of(2026, 10, 9, 12, 30),
                "Changed"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> healthRecordCommandService
                        .updatePatientRecord(
                                recordId,
                                patientOne,
                                request
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> healthRecordCommandService
                        .deletePatientRecord(
                                recordId,
                                patientOne
                        )
        );

        assertTrue(
                healthRecordRepository
                        .findById(recordId)
                        .isPresent()
        );
    }

    @Test
    void providerCannotUsePatientCreateOperation() {

        HealthRecordRequest request = new HealthRecordRequest(
                HealthMetricType.HEART_RATE,
                new BigDecimal("70"),
                LocalDateTime.of(2026, 10, 9, 8, 0),
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> healthRecordCommandService
                        .createPatientRecord(
                                provider,
                                request
                        )
        );
    }

    @Test
    void createUpdateRequestContainsExistingEditableValues() {

        HealthRecord record =
                savePatientRecord(
                        patientOne,
                        HealthMetricType.WEIGHT,
                        "76.2"
                );

        HealthRecordRequest request =
                healthRecordCommandService
                        .createUpdateRequest(
                                record.getId(),
                                patientOne
                        );

        assertEquals(
                HealthMetricType.WEIGHT,
                request.getMetricType()
        );

        assertEquals(
                0,
                request.getMetricValue()
                        .compareTo(new BigDecimal("76.2"))
        );

        assertEquals(
                record.getObservedAt(),
                request.getObservedAt()
        );
    }

    private User saveUser(
            String firstName,
            String lastName,
            String email,
            Role role) {

        User user = new User(
                firstName,
                lastName,
                email,
                "test-password-hash",
                role
        );

        return userRepository.save(user);
    }

    private HealthRecord savePatientRecord(
            User patient,
            HealthMetricType metricType,
            String value) {

        HealthRecord record = new HealthRecord(
                patient,
                patient,
                metricType,
                new BigDecimal(value),
                LocalDateTime.of(2026, 10, 8, 9, 0),
                null,
                HealthRecordSource.PATIENT_REPORTED
        );

        return healthRecordRepository.save(record);
    }
}