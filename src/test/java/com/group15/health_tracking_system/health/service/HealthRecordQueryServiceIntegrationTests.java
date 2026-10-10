package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
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
class HealthRecordQueryServiceIntegrationTests {

    @Autowired
    private HealthRecordQueryService queryService;

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
    void patientHistoryReturnsOnlyOwnRecordsNewestFirst() {

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "70",
                LocalDateTime.of(2026, 10, 8, 8, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "80",
                LocalDateTime.of(2026, 10, 10, 8, 0)
        );

        saveRecord(
                patientTwo,
                HealthMetricType.HEART_RATE,
                "90",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        List<HealthRecord> records =
                queryService.getPatientHistory(patientOne);

        assertEquals(2, records.size());

        assertEquals(
                0,
                records.get(0)
                        .getMetricValue()
                        .compareTo(new BigDecimal("80"))
        );

        assertEquals(
                0,
                records.get(1)
                        .getMetricValue()
                        .compareTo(new BigDecimal("70"))
        );
    }

    @Test
    void patientCanRetrieveOwnRecord() {

        HealthRecord record = saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75.5",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        HealthRecord result =
                queryService.getPatientRecord(
                        record.getId(),
                        patientOne
                );

        assertEquals(record.getId(), result.getId());
        assertEquals(
                patientOne.getId(),
                result.getPatient().getId()
        );
    }

    @Test
    void patientCannotRetrieveAnotherPatientsRecord() {

        HealthRecord record = saveRecord(
                patientTwo,
                HealthMetricType.WEIGHT,
                "82.0",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> queryService.getPatientRecord(
                        record.getId(),
                        patientOne
                )
        );
    }

    @Test
    void historyCanBeFilteredByMetric() {

        saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75.0",
                LocalDateTime.of(2026, 10, 8, 8, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "72",
                LocalDateTime.of(2026, 10, 8, 9, 0)
        );

        List<HealthRecord> records =
                queryService.getPatientHistoryByMetric(
                        patientOne,
                        HealthMetricType.WEIGHT
                );

        assertEquals(1, records.size());

        assertEquals(
                HealthMetricType.WEIGHT,
                records.get(0).getMetricType()
        );
    }

    @Test
    void historyCanBeFilteredByPeriod() {

        saveRecord(
                patientOne,
                HealthMetricType.BODY_TEMPERATURE,
                "36.5",
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.BODY_TEMPERATURE,
                "36.8",
                LocalDateTime.of(2026, 10, 5, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.BODY_TEMPERATURE,
                "37.0",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        List<HealthRecord> records =
                queryService.getPatientHistoryByPeriod(
                        patientOne,
                        LocalDateTime.of(
                                2026, 10, 4, 0, 0
                        ),
                        LocalDateTime.of(
                                2026, 10, 6, 23, 59
                        )
                );

        assertEquals(1, records.size());

        assertEquals(
                0,
                records.get(0)
                        .getMetricValue()
                        .compareTo(new BigDecimal("36.8"))
        );
    }

    @Test
    void historyCanBeFilteredByMetricAndPeriod() {

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "68",
                LocalDateTime.of(2026, 10, 2, 8, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "74",
                LocalDateTime.of(2026, 10, 5, 8, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75",
                LocalDateTime.of(2026, 10, 5, 9, 0)
        );

        List<HealthRecord> records =
                queryService
                        .getPatientHistoryByMetricAndPeriod(
                                patientOne,
                                HealthMetricType.HEART_RATE,
                                LocalDateTime.of(
                                        2026, 10, 4, 0, 0
                                ),
                                LocalDateTime.of(
                                        2026, 10, 6, 23, 59
                                )
                        );

        assertEquals(1, records.size());

        assertEquals(
                0,
                records.get(0)
                        .getMetricValue()
                        .compareTo(new BigDecimal("74"))
        );
    }

    @Test
    void invalidDateRangeIsRejected() {

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 10, 0, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 1, 0, 0);

        assertThrows(
                IllegalArgumentException.class,
                () -> queryService.getPatientHistoryByPeriod(
                        patientOne,
                        start,
                        end
                )
        );
    }

    @Test
    void missingMetricIsRejected() {

        assertThrows(
                IllegalArgumentException.class,
                () -> queryService.getPatientHistoryByMetric(
                        patientOne,
                        null
                )
        );
    }

    @Test
    void providerCannotUsePatientQueryOperations() {

        assertThrows(
                IllegalArgumentException.class,
                () -> queryService.getPatientHistory(provider)
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

    private HealthRecord saveRecord(
            User patient,
            HealthMetricType metricType,
            String value,
            LocalDateTime observedAt) {

        return healthRecordRepository.save(
                new HealthRecord(
                        patient,
                        patient,
                        metricType,
                        new BigDecimal(value),
                        observedAt,
                        null,
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }
}