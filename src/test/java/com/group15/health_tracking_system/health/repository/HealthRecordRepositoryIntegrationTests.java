package com.group15.health_tracking_system.health.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
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
class HealthRecordRepositoryIntegrationTests {

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private User patientOne;
    private User patientTwo;

    @BeforeEach
    void setUp() {
        healthRecordRepository.deleteAll();
        userRepository.deleteAll();

        patientOne = savePatient(
                "Patient",
                "One",
                "patient1@example.com"
        );

        patientTwo = savePatient(
                "Patient",
                "Two",
                "patient2@example.com"
        );
    }

    @Test
    void healthRecordCanBePersisted() {

        LocalDateTime observedAt =
                LocalDateTime.of(2026, 10, 10, 9, 30);

        HealthRecord record = new HealthRecord(
                patientOne,
                patientOne,
                HealthMetricType.HEART_RATE,
                new BigDecimal("72"),
                observedAt,
                "Morning reading",
                HealthRecordSource.PATIENT_REPORTED
        );

        HealthRecord saved =
                healthRecordRepository.saveAndFlush(record);

        assertTrue(saved.getId() != null);
        assertEquals(patientOne.getId(), saved.getPatient().getId());
        assertEquals(patientOne.getId(), saved.getRecordedBy().getId());

        assertEquals(
                HealthMetricType.HEART_RATE,
                saved.getMetricType()
        );

        assertEquals(
                0,
                saved.getMetricValue()
                        .compareTo(new BigDecimal("72"))
        );

        assertEquals(observedAt, saved.getObservedAt());
        assertEquals("Morning reading", saved.getNotes());

        assertEquals(
                HealthRecordSource.PATIENT_REPORTED,
                saved.getSource()
        );

        assertEquals("bpm", saved.getUnit());
    }

    @Test
    void recordsAreReturnedOnlyForSpecifiedPatient() {

        saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75.5",
                LocalDateTime.of(2026, 10, 8, 8, 0)
        );

        saveRecord(
                patientTwo,
                HealthMetricType.WEIGHT,
                "82.0",
                LocalDateTime.of(2026, 10, 8, 8, 0)
        );

        List<HealthRecord> patientOneRecords =
                healthRecordRepository
                        .findByPatientOrderByObservedAtDesc(
                                patientOne
                        );

        assertEquals(1, patientOneRecords.size());

        assertEquals(
                patientOne.getId(),
                patientOneRecords.get(0)
                        .getPatient()
                        .getId()
        );
    }

    @Test
    void recordsAreOrderedNewestFirst() {

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
                patientOne,
                HealthMetricType.HEART_RATE,
                "75",
                LocalDateTime.of(2026, 10, 9, 8, 0)
        );

        List<HealthRecord> records =
                healthRecordRepository
                        .findByPatientOrderByObservedAtDesc(
                                patientOne
                        );

        assertEquals(3, records.size());

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
                        .compareTo(new BigDecimal("75"))
        );

        assertEquals(
                0,
                records.get(2)
                        .getMetricValue()
                        .compareTo(new BigDecimal("70"))
        );
    }

    @Test
    void recordsCanBeFilteredByMetricType() {

        saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "72",
                LocalDateTime.of(2026, 10, 9, 10, 0)
        );

        List<HealthRecord> weightRecords =
                healthRecordRepository
                        .findByPatientAndMetricTypeOrderByObservedAtDesc(
                                patientOne,
                                HealthMetricType.WEIGHT
                        );

        assertEquals(1, weightRecords.size());

        assertEquals(
                HealthMetricType.WEIGHT,
                weightRecords.get(0).getMetricType()
        );

        assertEquals("kg", weightRecords.get(0).getUnit());
    }

    @Test
    void recordsCanBeFilteredByDateRange() {

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

        LocalDateTime start =
                LocalDateTime.of(2026, 10, 4, 0, 0);

        LocalDateTime end =
                LocalDateTime.of(2026, 10, 6, 23, 59);

        List<HealthRecord> records =
                healthRecordRepository
                        .findByPatientAndObservedAtBetweenOrderByObservedAtDesc(
                                patientOne,
                                start,
                                end
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
    void recordCannotBeRetrievedThroughAnotherPatient() {

        HealthRecord record = saveRecord(
                patientOne,
                HealthMetricType.OXYGEN_SATURATION,
                "98",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        assertTrue(
                healthRecordRepository
                        .findByIdAndPatient(
                                record.getId(),
                                patientOne
                        )
                        .isPresent()
        );

        assertFalse(
                healthRecordRepository
                        .findByIdAndPatient(
                                record.getId(),
                                patientTwo
                        )
                        .isPresent()
        );
    }

    private User savePatient(
            String firstName,
            String lastName,
            String email) {

        User user = new User(
                firstName,
                lastName,
                email,
                "test-password-hash",
                Role.PATIENT
        );

        return userRepository.save(user);
    }

    private HealthRecord saveRecord(
            User patient,
            HealthMetricType metricType,
            String value,
            LocalDateTime observedAt) {

        HealthRecord record = new HealthRecord(
                patient,
                patient,
                metricType,
                new BigDecimal(value),
                observedAt,
                null,
                HealthRecordSource.PATIENT_REPORTED
        );

        return healthRecordRepository.save(record);
    }
}