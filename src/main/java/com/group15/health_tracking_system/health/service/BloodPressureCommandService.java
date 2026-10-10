package com.group15.health_tracking_system.health.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.dto.BloodPressureRecordRequest;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles create, update and delete operations for blood-pressure records.
 *
 * A blood-pressure observation always contains both systolic and diastolic
 * values in one record.
 */
@Service
public class BloodPressureCommandService {

    private final BloodPressureRecordRepository bloodPressureRecordRepository;

    public BloodPressureCommandService(
            BloodPressureRecordRepository bloodPressureRecordRepository) {

        this.bloodPressureRecordRepository =
                bloodPressureRecordRepository;
    }

    /**
     * Creates a patient-reported blood-pressure observation.
     */
    @Transactional
    public BloodPressureRecord createPatientRecord(
            User currentUser,
            BloodPressureRecordRequest request) {

        requirePatient(currentUser);
        requireRequest(request);

        BloodPressureRecord record =
                new BloodPressureRecord(
                        currentUser,
                        currentUser,
                        request.getSystolic(),
                        request.getDiastolic(),
                        request.getObservedAt(),
                        normalizeNotes(request.getNotes()),
                        HealthRecordSource.PATIENT_REPORTED
                );

        return bloodPressureRecordRepository.save(record);
    }

    /**
     * Retrieves a blood-pressure record for patient-side modification.
     */
    @Transactional(readOnly = true)
    public BloodPressureRecord getPatientRecordForModification(
            Long recordId,
            User currentUser) {

        requirePatient(currentUser);

        if (recordId == null) {
            throw new IllegalArgumentException(
                    "Blood-pressure record ID is required"
            );
        }

        BloodPressureRecord record =
                bloodPressureRecordRepository
                        .findByIdAndPatient(
                                recordId,
                                currentUser
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Blood-pressure record not found"
                                )
                        );

        requirePatientEditable(
                record,
                currentUser
        );

        return record;
    }

    /**
     * Creates form data from an existing editable record.
     */
    @Transactional(readOnly = true)
    public BloodPressureRecordRequest createUpdateRequest(
            Long recordId,
            User currentUser) {

        BloodPressureRecord record =
                getPatientRecordForModification(
                        recordId,
                        currentUser
                );

        return new BloodPressureRecordRequest(
                record.getSystolic(),
                record.getDiastolic(),
                record.getObservedAt(),
                record.getNotes()
        );
    }

    /**
     * Updates the systolic and diastolic values together.
     */
    @Transactional
    public BloodPressureRecord updatePatientRecord(
            Long recordId,
            User currentUser,
            BloodPressureRecordRequest request) {

        requireRequest(request);

        BloodPressureRecord record =
                getPatientRecordForModification(
                        recordId,
                        currentUser
                );

        record.setSystolic(
                request.getSystolic()
        );

        record.setDiastolic(
                request.getDiastolic()
        );

        record.setObservedAt(
                request.getObservedAt()
        );

        record.setNotes(
                normalizeNotes(
                        request.getNotes()
                )
        );

        return bloodPressureRecordRepository.save(record);
    }

    /**
     * Deletes one patient-created blood-pressure observation.
     */
    @Transactional
    public void deletePatientRecord(
            Long recordId,
            User currentUser) {

        BloodPressureRecord record =
                getPatientRecordForModification(
                        recordId,
                        currentUser
                );

        bloodPressureRecordRepository.delete(record);
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

    private void requireRequest(
            BloodPressureRecordRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Blood-pressure request is required"
            );
        }

        if (request.getSystolic() == null) {
            throw new IllegalArgumentException(
                    "Systolic value is required"
            );
        }

        if (request.getDiastolic() == null) {
            throw new IllegalArgumentException(
                    "Diastolic value is required"
            );
        }

        if (request.getObservedAt() == null) {
            throw new IllegalArgumentException(
                    "Observation date and time are required"
            );
        }
    }

    private void requirePatientEditable(
            BloodPressureRecord record,
            User currentUser) {

        if (record.getSource()
                != HealthRecordSource.PATIENT_REPORTED) {

            throw new IllegalArgumentException(
                    "Provider-recorded blood-pressure records "
                    + "cannot be modified by the patient"
            );
        }

        if (record.getRecordedBy() == null
                || !Objects.equals(
                        record.getRecordedBy().getId(),
                        currentUser.getId())) {

            throw new IllegalArgumentException(
                    "This blood-pressure record cannot be "
                    + "modified by the current patient"
            );
        }
    }

    private String normalizeNotes(String notes) {

        if (notes == null || notes.isBlank()) {
            return null;
        }

        return notes.trim();
    }
}