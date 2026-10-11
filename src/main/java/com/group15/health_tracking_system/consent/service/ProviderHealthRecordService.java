package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.dto.HealthRecordListItem;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.service.BloodPressureQueryService;
import com.group15.health_tracking_system.health.service.HealthRecordHistoryService;
import com.group15.health_tracking_system.health.service.HealthRecordQueryService;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allows providers to read a patient's health records
 * only after checking their active consent.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderHealthRecordService {

    private final ProviderAccessService providerAccessService;
    private final HealthRecordQueryService healthRecordQueryService;
    private final HealthRecordHistoryService healthRecordHistoryService;
    private final BloodPressureQueryService bloodPressureQueryService;
    private final UserRepository userRepository;

    public ProviderHealthRecordService(
            ProviderAccessService providerAccessService,
            HealthRecordQueryService healthRecordQueryService,
            HealthRecordHistoryService healthRecordHistoryService,
            BloodPressureQueryService bloodPressureQueryService,
            UserRepository userRepository
    ) {
        this.providerAccessService = providerAccessService;
        this.healthRecordQueryService = healthRecordQueryService;
        this.healthRecordHistoryService = healthRecordHistoryService;
        this.bloodPressureQueryService = bloodPressureQueryService;
        this.userRepository = userRepository;
    }

    /**
     * Returns patient details after checking the provider's consent.
     */
    public User getPatient(Long patientId) {
        return requireAuthorizedPatient(patientId);
    }

    /**
     * Returns single-value measurements, such as weight and heart rate.
     */
    public List<HealthRecord> getPatientHistory(Long patientId) {
        User patient = requireAuthorizedPatient(patientId);

        return healthRecordQueryService.getPatientHistory(patient);
    }

    /**
     * Returns the combined history of single-value measurements
     * and blood-pressure records, newest first.
     */
    public List<HealthRecordListItem> getPatientHistoryItems(
            Long patientId
    ) {
        User patient = requireAuthorizedPatient(patientId);

        // Null filters request all metrics and all observation dates.
        return healthRecordHistoryService.getPatientHistory(
                patient,
                null,
                null,
                null
        );
    }

    /**
     * Returns a single-value record belonging to the consenting patient.
     */
    public HealthRecord getPatientRecord(
            Long patientId,
            Long recordId
    ) {
        User patient = requireAuthorizedPatient(patientId);

        // The shared service also checks record ownership.
        return healthRecordQueryService.getPatientRecord(
                recordId,
                patient
        );
    }

    /**
     * Returns a blood-pressure record belonging to the consenting patient.
     */
    public BloodPressureRecord getPatientBloodPressureRecord(
            Long patientId,
            Long recordId
    ) {
        User patient = requireAuthorizedPatient(patientId);

        // Blood-pressure records have their own patient-scoped query.
        return bloodPressureQueryService.getPatientRecord(
                recordId,
                patient
        );
    }

    private User requireAuthorizedPatient(Long patientId) {
        // Check the provider who is actually logged in.
        providerAccessService.requireActiveConsent(patientId);

        return userRepository.findById(patientId)
                .filter(user -> user.getRole() == Role.PATIENT)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient was not found."
                ));
    }
}