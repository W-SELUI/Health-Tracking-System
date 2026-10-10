package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BloodPressureQueryServiceIntegrationTests {

    @Autowired
    private BloodPressureQueryService bloodPressureQueryService;

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
    void patientHistoryReturnsOnlyOwnRecordsNewestFirst() {

        saveRecord(
                patientOne,
                "110",
                "70",
                LocalDateTime.of(2026, 10, 8, 8, 0)
        );

        saveRecord(
                patientOne,
                "120",
                "80",
                LocalDateTime.of(2026, 10, 10, 8, 0)
        );

        saveRecord(
                patientTwo,
                "130",
                "85",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        List<BloodPressureRecord> records =
                bloodPressureQueryService
                        .getPatientHistory(patientOne);

        assertEquals(2, records.size());

        assertEquals(
                0,
                records.get(0)
                        .getSystolic()
                        .compareTo(new BigDecimal("120"))
        );

        assertEquals(
                0,
                records.get(0)
                        .getDiastolic()
                        .compareTo(new BigDecimal("80"))
        );

        assertEquals(
                0,
                records.get(1)
                        .getSystolic()
                        .compareTo(new BigDecimal("110"))
        );
    }

    @Test
    void patientCanRetrieveOwnBloodPressureRecord() {

        BloodPressureRecord record =
                saveRecord(
                        patientOne,
                        "118",
                        "76",
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                9,
                                0
                        )
                );

        BloodPressureRecord result =
                bloodPressureQueryService
                        .getPatientRecord(
                                record.getId(),
                                patientOne
                        );

        assertEquals(
                record.getId(),
                result.getId()
        );

        assertEquals(
                patientOne.getId(),
                result.getPatient().getId()
        );
    }

    @Test
    void patientCannotRetrieveAnotherPatientsBloodPressureRecord() {

        BloodPressureRecord record =
                saveRecord(
                        patientTwo,
                        "125",
                        "82",
                        LocalDateTime.of(
                                2026,
                                10,
                                9,
                                9,
                                0
                        )
                );

        assertThrows(
                ResourceNotFoundException.class,
                () -> bloodPressureQueryService
                        .getPatientRecord(
                                record.getId(),
                                patientOne
                        )
        );
    }

    @Test
    void bloodPressureHistoryCanBeFilteredByPeriod() {

        saveRecord(
                patientOne,
                "110",
                "70",
                LocalDateTime.of(
                        2026,
                        10,
                        1,
                        9,
                        0
                )
        );

        saveRecord(
                patientOne,
                "118",
                "76",
                LocalDateTime.of(
                        2026,
                        10,
                        5,
                        9,
                        0
                )
        );

        saveRecord(
                patientOne,
                "125",
                "82",
                LocalDateTime.of(
                        2026,
                        10,
                        10,
                        9,
                        0
                )
        );

        List<BloodPressureRecord> records =
                bloodPressureQueryService
                        .getPatientHistoryByPeriod(
                                patientOne,
                                LocalDateTime.of(
                                        2026,
                                        10,
                                        4,
                                        0,
                                        0
                                ),
                                LocalDateTime.of(
                                        2026,
                                        10,
                                        6,
                                        23,
                                        59
                                )
                        );

        assertEquals(1, records.size());

        assertEquals(
                0,
                records.get(0)
                        .getSystolic()
                        .compareTo(new BigDecimal("118"))
        );

        assertEquals(
                0,
                records.get(0)
                        .getDiastolic()
                        .compareTo(new BigDecimal("76"))
        );
    }

    @Test
    void invalidDateRangeIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> bloodPressureQueryService
                        .getPatientHistoryByPeriod(
                                patientOne,
                                LocalDateTime.of(
                                        2026,
                                        10,
                                        10,
                                        0,
                                        0
                                ),
                                LocalDateTime.of(
                                        2026,
                                        10,
                                        1,
                                        0,
                                        0
                                )
                        )
        );
    }

    @Test
    void providerCannotUsePatientBloodPressureQueries() {

        assertThrows(
                IllegalArgumentException.class,
                () -> bloodPressureQueryService
                        .getPatientHistory(provider)
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

    private BloodPressureRecord saveRecord(
            User patient,
            String systolic,
            String diastolic,
            LocalDateTime observedAt) {

        return bloodPressureRecordRepository.save(
                new BloodPressureRecord(
                        patient,
                        patient,
                        new BigDecimal(systolic),
                        new BigDecimal(diastolic),
                        observedAt,
                        null,
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }
}