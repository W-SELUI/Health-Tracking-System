package com.group15.health_tracking_system.health.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.health.dto.HealthRecordFilterOption;
import com.group15.health_tracking_system.health.dto.HealthRecordListItem;
import com.group15.health_tracking_system.health.dto.HealthRecordListItem.RecordType;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
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
class HealthRecordHistoryServiceIntegrationTests {

    @Autowired
    private HealthRecordHistoryService historyService;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private BloodPressureRecordRepository bloodPressureRecordRepository;

    @Autowired
    private UserRepository userRepository;

    private User patient;

    @BeforeEach
    void setUp() {

        bloodPressureRecordRepository.deleteAll();
        healthRecordRepository.deleteAll();
        userRepository.deleteAll();

        patient = userRepository.save(
                new User(
                        "Patient",
                        "One",
                        "patient1@example.com",
                        "test-password-hash",
                        Role.PATIENT
                )
        );
    }

    @Test
    void unifiedHistoryContainsScalarAndBloodPressureRecords() {

        saveScalarRecord(
                HealthMetricType.HEART_RATE,
                "72",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        saveBloodPressureRecord(
                "118",
                "76",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        List<HealthRecordListItem> items =
                historyService.getPatientHistory(
                        patient,
                        null,
                        null,
                        null
                );

        assertEquals(2, items.size());

        assertEquals(
                RecordType.BLOOD_PRESSURE,
                items.get(0).getRecordType()
        );

        assertEquals(
                RecordType.SCALAR,
                items.get(1).getRecordType()
        );
    }

    @Test
    void unifiedHistoryIsSortedNewestFirst() {

        saveScalarRecord(
                HealthMetricType.WEIGHT,
                "74.5",
                LocalDateTime.of(2026, 10, 8, 8, 0)
        );

        saveBloodPressureRecord(
                "120",
                "80",
                LocalDateTime.of(2026, 10, 10, 8, 0)
        );

        saveScalarRecord(
                HealthMetricType.HEART_RATE,
                "70",
                LocalDateTime.of(2026, 10, 9, 8, 0)
        );

        List<HealthRecordListItem> items =
                historyService.getPatientHistory(
                        patient,
                        null,
                        null,
                        null
                );

        assertEquals(3, items.size());

        assertEquals(
                LocalDateTime.of(2026, 10, 10, 8, 0),
                items.get(0).getObservedAt()
        );

        assertEquals(
                LocalDateTime.of(2026, 10, 9, 8, 0),
                items.get(1).getObservedAt()
        );

        assertEquals(
                LocalDateTime.of(2026, 10, 8, 8, 0),
                items.get(2).getObservedAt()
        );
    }

    @Test
    void bloodPressureFilterReturnsOnlyBloodPressureRecords() {

        saveScalarRecord(
                HealthMetricType.HEART_RATE,
                "72",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        saveBloodPressureRecord(
                "118",
                "76",
                LocalDateTime.of(2026, 10, 9, 10, 0)
        );

        List<HealthRecordListItem> items =
                historyService.getPatientHistory(
                        patient,
                        HealthRecordFilterOption.BLOOD_PRESSURE,
                        null,
                        null
                );

        assertEquals(1, items.size());

        assertEquals(
                RecordType.BLOOD_PRESSURE,
                items.get(0).getRecordType()
        );

        assertEquals(
                "Blood Pressure",
                items.get(0).getMetricName()
        );

        assertEquals(
                "118 / 76 mmHg",
                items.get(0).getDisplayValue()
        );
    }

    @Test
    void scalarMetricFilterReturnsOnlySelectedMetric() {

        saveScalarRecord(
                HealthMetricType.WEIGHT,
                "74.5",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        saveScalarRecord(
                HealthMetricType.HEART_RATE,
                "72",
                LocalDateTime.of(2026, 10, 9, 10, 0)
        );

        saveBloodPressureRecord(
                "118",
                "76",
                LocalDateTime.of(2026, 10, 9, 11, 0)
        );

        List<HealthRecordListItem> items =
                historyService.getPatientHistory(
                        patient,
                        HealthRecordFilterOption.WEIGHT,
                        null,
                        null
                );

        assertEquals(1, items.size());

        assertEquals(
                "Weight",
                items.get(0).getMetricName()
        );

        assertEquals(
                "74.5 kg",
                items.get(0).getDisplayValue()
        );
    }

    @Test
    void dateRangeFiltersBothRecordTypes() {

        saveScalarRecord(
                HealthMetricType.HEART_RATE,
                "68",
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        saveScalarRecord(
                HealthMetricType.WEIGHT,
                "75",
                LocalDateTime.of(2026, 10, 5, 9, 0)
        );

        saveBloodPressureRecord(
                "118",
                "76",
                LocalDateTime.of(2026, 10, 5, 10, 0)
        );

        saveBloodPressureRecord(
                "125",
                "82",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        List<HealthRecordListItem> items =
                historyService.getPatientHistory(
                        patient,
                        null,
                        LocalDateTime.of(
                                2026, 10, 4, 0, 0
                        ),
                        LocalDateTime.of(
                                2026, 10, 6, 23, 59
                        )
                );

        assertEquals(2, items.size());

        assertTrue(
                items.stream().anyMatch(
                        item -> item.getMetricName()
                                .equals("Weight")
                )
        );

        assertTrue(
                items.stream().anyMatch(
                        item -> item.getMetricName()
                                .equals("Blood Pressure")
                )
        );
    }

    @Test
    void displayValuesDoNotContainUnnecessaryTrailingZeros() {

        saveScalarRecord(
                HealthMetricType.HEART_RATE,
                "72.000",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        saveBloodPressureRecord(
                "108.000",
                "67.000",
                LocalDateTime.of(2026, 10, 9, 10, 0)
        );

        List<HealthRecordListItem> items =
                historyService.getPatientHistory(
                        patient,
                        null,
                        null,
                        null
                );

        assertTrue(
                items.stream().anyMatch(
                        item -> item.getDisplayValue()
                                .equals("72 bpm")
                )
        );

        assertTrue(
                items.stream().anyMatch(
                        item -> item.getDisplayValue()
                                .equals("108 / 67 mmHg")
                )
        );
    }

    private HealthRecord saveScalarRecord(
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

    private BloodPressureRecord saveBloodPressureRecord(
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