package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.health.entity.HealthRecord;
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
    private final UserRepository userRepository;

    public ProviderHealthRecordService(
            ProviderAccessService providerAccessService,
            HealthRecordQueryService healthRecordQueryService,
            UserRepository userRepository
    ) {
        this.providerAccessService = providerAccessService;
        this.healthRecordQueryService = healthRecordQueryService;
        this.userRepository = userRepository;
    }

    /**
     * Returns patient details for the provider's record-viewing page.
     */
    public User getPatient(Long patientId) {
        return requireAuthorizedPatient(patientId);
    }

    /**
     * Returns the consenting patient's health-record history.
     */
    public List<HealthRecord> getPatientHistory(Long patientId) {
        User patient = requireAuthorizedPatient(patientId);

        return healthRecordQueryService.getPatientHistory(patient);
    }

    /**
     * Returns a single record belonging to the consenting patient.
     */
    public HealthRecord getPatientRecord(
            Long patientId,
            Long recordId
    ) {
        User patient = requireAuthorizedPatient(patientId);

        // The shared service also checks that this record
        // belongs to the selected patient.
        return healthRecordQueryService.getPatientRecord(
                recordId,
                patient
        );
    }

    private User requireAuthorizedPatient(Long patientId) {
        // This checks the provider who is actually logged in.
        providerAccessService.requireActiveConsent(patientId);

        return userRepository.findById(patientId)
                .filter(user -> user.getRole() == Role.PATIENT)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Patient was not found."
                ));
    }
}