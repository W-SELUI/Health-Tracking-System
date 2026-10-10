package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.dto.BloodPressureRecordRequest;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
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
class BloodPressureCommandServiceIntegrationTests {

    @Autowired
    private BloodPressureCommandService bloodPressureCommandService;

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private User patientOne;
    private User patientTwo;
    private User provider;

    @BeforeEach
    void setUp() {

        bloodPressureRecordRepository.deleteAll();
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
    void patientCanCreateCompleteBloodPressureRecord() {

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("108"),
                        new BigDecimal("67"),
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                9,
                                30
                        ),
                        "  Morning reading  "
                );

        BloodPressureRecord saved =
                bloodPressureCommandService
                        .createPatientRecord(
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
                0,
                saved.getSystolic()
                        .compareTo(
                                new BigDecimal("108")
                        )
        );

        assertEquals(
                0,
                saved.getDiastolic()
                        .compareTo(
                                new BigDecimal("67")
                        )
        );

        assertEquals(
                "Morning reading",
                saved.getNotes()
        );

        assertEquals(
                "mmHg",
                saved.getUnit()
        );
    }

    @Test
    void bloodPressureCannotBeCreatedWithoutSystolic() {

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        null,
                        new BigDecimal("67"),
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                9,
                                30
                        ),
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bloodPressureCommandService
                        .createPatientRecord(
                                patientOne,
                                request
                        )
        );

        assertTrue(
                bloodPressureRecordRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void bloodPressureCannotBeCreatedWithoutDiastolic() {

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("108"),
                        null,
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                9,
                                30
                        ),
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bloodPressureCommandService
                        .createPatientRecord(
                                patientOne,
                                request
                        )
        );

        assertTrue(
                bloodPressureRecordRepository
                        .findAll()
                        .isEmpty()
        );
    }

    @Test
    void patientCanUpdateBothBloodPressureValuesTogether() {

        BloodPressureRecord record =
                savePatientRecord(
                        patientOne,
                        "108",
                        "67"
                );

        Long originalPatientId =
                record.getPatient().getId();

        Long originalRecordedById =
                record.getRecordedBy().getId();

        HealthRecordSource originalSource =
                record.getSource();

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("112"),
                        new BigDecimal("70"),
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                10,
                                0
                        ),
                        "Updated reading"
                );

        BloodPressureRecord updated =
                bloodPressureCommandService
                        .updatePatientRecord(
                                record.getId(),
                                patientOne,
                                request
                        );

        assertEquals(
                0,
                updated.getSystolic()
                        .compareTo(
                                new BigDecimal("112")
                        )
        );

        assertEquals(
                0,
                updated.getDiastolic()
                        .compareTo(
                                new BigDecimal("70")
                        )
        );

        assertEquals(
                "Updated reading",
                updated.getNotes()
        );

        /*
         * Ownership and provenance must remain unchanged.
         */
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
    void patientCanDeleteOwnBloodPressureRecord() {

        BloodPressureRecord record =
                savePatientRecord(
                        patientOne,
                        "118",
                        "76"
                );

        Long recordId = record.getId();

        bloodPressureCommandService
                .deletePatientRecord(
                        recordId,
                        patientOne
                );

        assertFalse(
                bloodPressureRecordRepository
                        .findById(recordId)
                        .isPresent()
        );
    }

    @Test
    void patientCannotModifyAnotherPatientsBloodPressureRecord() {

        BloodPressureRecord record =
                savePatientRecord(
                        patientTwo,
                        "120",
                        "80"
                );

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("110"),
                        new BigDecimal("70"),
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                11,
                                0
                        ),
                        null
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> bloodPressureCommandService
                        .updatePatientRecord(
                                record.getId(),
                                patientOne,
                                request
                        )
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> bloodPressureCommandService
                        .deletePatientRecord(
                                record.getId(),
                                patientOne
                        )
        );
    }

    @Test
    void patientCannotModifyProviderRecordedBloodPressure() {

        BloodPressureRecord record =
                new BloodPressureRecord(
                        patientOne,
                        provider,
                        new BigDecimal("122"),
                        new BigDecimal("78"),
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                12,
                                0
                        ),
                        "Provider reading",
                        HealthRecordSource.PROVIDER_RECORDED
                );

        record =
                bloodPressureRecordRepository
                        .save(record);

        Long recordId = record.getId();

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("110"),
                        new BigDecimal("70"),
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                12,
                                30
                        ),
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bloodPressureCommandService
                        .updatePatientRecord(
                                recordId,
                                patientOne,
                                request
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> bloodPressureCommandService
                        .deletePatientRecord(
                                recordId,
                                patientOne
                        )
        );

        assertTrue(
                bloodPressureRecordRepository
                        .findById(recordId)
                        .isPresent()
        );
    }

    @Test
    void providerCannotUsePatientBloodPressureCreateOperation() {

        BloodPressureRecordRequest request =
                new BloodPressureRecordRequest(
                        new BigDecimal("120"),
                        new BigDecimal("80"),
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                8,
                                0
                        ),
                        null
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> bloodPressureCommandService
                        .createPatientRecord(
                                provider,
                                request
                        )
        );
    }

    private User saveUser(
            String firstName,
            String lastName,
            String email,
            Role role) {

        return userRepository.save(
                new User(
                        firstName,
                        lastName,
                        email,
                        "test-password-hash",
                        role
                )
        );
    }

    private BloodPressureRecord savePatientRecord(
            User patient,
            String systolic,
            String diastolic) {

        return bloodPressureRecordRepository.save(
                new BloodPressureRecord(
                        patient,
                        patient,
                        new BigDecimal(systolic),
                        new BigDecimal(diastolic),
                        LocalDateTime.of(
                                2026,
                                10,
                                8,
                                9,
                                0
                        ),
                        null,
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }
}