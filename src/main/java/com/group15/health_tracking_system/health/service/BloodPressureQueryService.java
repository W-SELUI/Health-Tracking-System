package com.group15.health_tracking_system.health.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles patient-side retrieval of blood-pressure records.
 *
 * All queries are scoped to the authenticated patient.
 */
@Service
public class BloodPressureQueryService {

    private final BloodPressureRecordRepository bloodPressureRecordRepository;

    public BloodPressureQueryService(
            BloodPressureRecordRepository bloodPressureRecordRepository) {

        this.bloodPressureRecordRepository =
                bloodPressureRecordRepository;
    }

    /**
     * Returns all blood-pressure observations belonging to the patient,
     * newest first.
     */
    @Transactional(readOnly = true)
    public List<BloodPressureRecord> getPatientHistory(
            User currentUser) {

        requirePatient(currentUser);

        return bloodPressureRecordRepository
                .findByPatientOrderByObservedAtDesc(
                        currentUser
                );
    }

    /**
     * Returns one blood-pressure record only when it belongs
     * to the authenticated patient.
     */
    @Transactional(readOnly = true)
    public BloodPressureRecord getPatientRecord(
            Long recordId,
            User currentUser) {

        requirePatient(currentUser);

        if (recordId == null) {
            throw new IllegalArgumentException(
                    "Blood-pressure record ID is required"
            );
        }

        return bloodPressureRecordRepository
                .findByIdAndPatient(
                        recordId,
                        currentUser
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Blood-pressure record not found"
                        )
                );
    }

    /**
     * Returns blood-pressure observations within a reporting period.
     */
    @Transactional(readOnly = true)
    public List<BloodPressureRecord> getPatientHistoryByPeriod(
            User currentUser,
            LocalDateTime start,
            LocalDateTime end) {

        requirePatient(currentUser);
        validatePeriod(start, end);

        return bloodPressureRecordRepository
                .findByPatientAndObservedAtBetweenOrderByObservedAtDesc(
                        currentUser,
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