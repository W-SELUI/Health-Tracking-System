package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.entity.Consent;
import com.group15.health_tracking_system.consent.entity.ConsentStatus;
import com.group15.health_tracking_system.consent.repository.ConsentRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;

    public ConsentService(
            ConsentRepository consentRepository,
            UserRepository userRepository
    ) {
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Consent grantConsent(Long patientId, Long providerId) {
        User patient = getUser(patientId);
        User provider = getUser(providerId);

        validateRoles(patient, provider);

        Consent consent = consentRepository
                .findByPatient_IdAndProvider_Id(patientId, providerId)
                .orElseGet(() -> new Consent(patient, provider));

        consent.activate();

        return consentRepository.save(consent);
    }

    @Transactional
    public void revokeConsent(Long patientId, Long providerId) {
        Consent consent = consentRepository
                .findByPatient_IdAndProvider_Id(patientId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Consent was not found"
                ));

        consent.revoke();
        consentRepository.save(consent);
    }

    @Transactional
    public boolean hasActiveConsent(Long patientId, Long providerId) {
        return consentRepository
                .findByPatient_IdAndProvider_IdAndStatus(
                        patientId,
                        providerId,
                        ConsentStatus.ACTIVE
                )
                .isPresent();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User was not found: " + userId
                ));
    }

    private void validateRoles(User patient, User provider) {
        if (patient.getRole() != Role.PATIENT) {
            throw new IllegalArgumentException(
                    "Consent owner must have the PATIENT role"
            );
        }

        if (provider.getRole() != Role.PROVIDER) {
            throw new IllegalArgumentException(
                    "Consent recipient must have the PROVIDER role"
            );
        }
    }
}