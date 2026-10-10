package com.group15.health_tracking_system.health.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles patient-side health record retrieval and filtering.
 *
 * All query operations are scoped to the authenticated patient so one
 * patient's records cannot be exposed to another patient.
 */
@Service
public class HealthRecordQueryService {

    private final HealthRecordRepository healthRecordRepository;

    public HealthRecordQueryService(
            HealthRecordRepository healthRecordRepository) {

        this.healthRecordRepository = healthRecordRepository;
    }

    /**
     * Returns the patient's complete health record history,
     * newest observation first.
     */
    @Transactional(readOnly = true)
    public List<HealthRecord> getPatientHistory(User currentUser) {

        requirePatient(currentUser);

        return healthRecordRepository
                .findByPatientOrderByObservedAtDesc(currentUser);
    }

    /**
     * Returns one health record only when it belongs to the
     * authenticated patient.
     */
    @Transactional(readOnly = true)
    public HealthRecord getPatientRecord(
            Long recordId,
            User currentUser) {

        requirePatient(currentUser);

        if (recordId == null) {
            throw new IllegalArgumentException(
                    "Health record ID is required"
            );
        }

        return healthRecordRepository
                .findByIdAndPatient(recordId, currentUser)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Health record not found"
                        )
                );
    }

    /**
     * Returns records for one health metric.
     */
    @Transactional(readOnly = true)
    public List<HealthRecord> getPatientHistoryByMetric(
            User currentUser,
            HealthMetricType metricType) {

        requirePatient(currentUser);

        if (metricType == null) {
            throw new IllegalArgumentException(
                    "Health metric is required"
            );
        }

        return healthRecordRepository
                .findByPatientAndMetricTypeOrderByObservedAtDesc(
                        currentUser,
                        metricType
                );
    }

    /**
     * Returns all patient records observed within a date/time range.
     */
    @Transactional(readOnly = true)
    public List<HealthRecord> getPatientHistoryByPeriod(
            User currentUser,
            LocalDateTime start,
            LocalDateTime end) {

        requirePatient(currentUser);
        validatePeriod(start, end);

        return healthRecordRepository
                .findByPatientAndObservedAtBetweenOrderByObservedAtDesc(
                        currentUser,
                        start,
                        end
                );
    }

    /**
     * Returns records for one metric within a date/time range.
     */
    @Transactional(readOnly = true)
    public List<HealthRecord> getPatientHistoryByMetricAndPeriod(
            User currentUser,
            HealthMetricType metricType,
            LocalDateTime start,
            LocalDateTime end) {

        requirePatient(currentUser);

        if (metricType == null) {
            throw new IllegalArgumentException(
                    "Health metric is required"
            );
        }

        validatePeriod(start, end);

        return healthRecordRepository
                .findByPatientAndMetricTypeAndObservedAtBetweenOrderByObservedAtDesc(
                        currentUser,
                        metricType,
                        start,
                        end
                );
    }

    private void validatePeriod(
            LocalDateTime start,
            LocalDateTime end) {

        if (start == null || end == null) {
            throw new IllegalArgumentException(
                    "Start and end date/time are required"
            );
        }

        if (start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date/time must not be after end date/time"
            );
        }
    }

    private void requirePatient(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "Current user is required"
            );
        }

        if (user.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException(
                    "Only patients can perform this operation"
            );
        }
    }
}