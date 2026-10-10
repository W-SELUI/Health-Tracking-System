package com.group15.health_tracking_system.health.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.dto.HealthRecordRequest;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


//Handles create, update and delete operations for health records.

@Service
public class HealthRecordCommandService {

    private final HealthRecordRepository healthRecordRepository;

    public HealthRecordCommandService(
            HealthRecordRepository healthRecordRepository) {

        this.healthRecordRepository = healthRecordRepository;
    }

    /**
     * Creates a health record owned and recorded by the currently
     * authenticated patient.
     */
    @Transactional
    public HealthRecord createPatientRecord(
            User currentUser,
            HealthRecordRequest request) {

        requirePatient(currentUser);
        requireRequest(request);

        HealthRecord record = new HealthRecord(
                currentUser,
                currentUser,
                request.getMetricType(),
                request.getMetricValue(),
                request.getObservedAt(),
                normalizeNotes(request.getNotes()),
                HealthRecordSource.PATIENT_REPORTED
        );

        return healthRecordRepository.save(record);
    }

    /**
     * Returns one patient-owned record for modification.
     *
     * This method is intentionally limited to the modification workflow.
     * General history and filtering will belong to HealthRecordQueryService.
     */
    @Transactional(readOnly = true)
    public HealthRecord getPatientRecordForModification(
            Long recordId,
            User currentUser) {

        requirePatient(currentUser);

        HealthRecord record = healthRecordRepository
                .findByIdAndPatient(recordId, currentUser)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Health record not found"
                        )
                );

        requirePatientEditable(record, currentUser);

        return record;
    }

    /**
     * Creates form data from an existing editable health record.
     */
    @Transactional(readOnly = true)
    public HealthRecordRequest createUpdateRequest(
            Long recordId,
            User currentUser) {

        HealthRecord record =
                getPatientRecordForModification(
                        recordId,
                        currentUser
                );

        return new HealthRecordRequest(
                record.getMetricType(),
                record.getMetricValue(),
                record.getObservedAt(),
                record.getNotes()
        );
    }

    /**
     * Updates only the editable measurement fields.
     *
     * Patient ownership, recordedBy and source cannot be changed.
     */
    @Transactional
    public HealthRecord updatePatientRecord(
            Long recordId,
            User currentUser,
            HealthRecordRequest request) {

        requireRequest(request);

        HealthRecord record =
                getPatientRecordForModification(
                        recordId,
                        currentUser
                );

        record.setMetricType(request.getMetricType());
        record.setMetricValue(request.getMetricValue());
        record.setObservedAt(request.getObservedAt());
        record.setNotes(
                normalizeNotes(request.getNotes())
        );

        return healthRecordRepository.save(record);
    }

    /**
     * Deletes a patient-created health record.
     *
     * Provider-recorded health records cannot be deleted through
     * the patient workflow.
     */
    @Transactional
    public void deletePatientRecord(
            Long recordId,
            User currentUser) {

        HealthRecord record =
                getPatientRecordForModification(
                        recordId,
                        currentUser
                );

        healthRecordRepository.delete(record);
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
            HealthRecordRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Health record request is required"
            );
        }

        if (request.getMetricType() == null) {
            throw new IllegalArgumentException(
                    "Health metric is required"
            );
        }

        if (request.getMetricValue() == null) {
            throw new IllegalArgumentException(
                    "Measurement value is required"
            );
        }

        if (request.getObservedAt() == null) {
            throw new IllegalArgumentException(
                    "Observation date and time are required"
            );
        }
    }

    private void requirePatientEditable(
            HealthRecord record,
            User currentUser) {

        if (record.getSource()
                != HealthRecordSource.PATIENT_REPORTED) {

            throw new IllegalArgumentException(
                    "Provider-recorded health records cannot "
                    + "be modified by the patient"
            );
        }

        if (record.getRecordedBy() == null
                || !Objects.equals(
                        record.getRecordedBy().getId(),
                        currentUser.getId())) {

            throw new IllegalArgumentException(
                    "This health record cannot be modified "
                    + "by the current patient"
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