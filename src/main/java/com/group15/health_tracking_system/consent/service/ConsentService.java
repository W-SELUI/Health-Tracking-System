package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.entity.Consent;
import com.group15.health_tracking_system.consent.entity.ConsentStatus;
import com.group15.health_tracking_system.consent.repository.ConsentRepository;
import com.group15.health_tracking_system.health.service.HealthRecordChangeRequestCancellationService;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final UserRepository userRepository;

    private final HealthRecordChangeRequestCancellationService
            healthRecordChangeRequestCancellationService;

    public ConsentService(
            ConsentRepository consentRepository,
            UserRepository userRepository,
            HealthRecordChangeRequestCancellationService
                    healthRecordChangeRequestCancellationService) {

        this.consentRepository =
                consentRepository;

        this.userRepository =
                userRepository;

        this.healthRecordChangeRequestCancellationService =
                healthRecordChangeRequestCancellationService;
    }

    @Transactional
    public Consent grantConsent(
            Long patientId,
            Long providerId) {

        User patient =
                getUser(
                        patientId
                );

        User provider =
                getUser(
                        providerId
                );

        validateRoles(
                patient,
                provider
        );

        Consent consent =
                consentRepository
                        .findByPatient_IdAndProvider_Id(
                                patientId,
                                providerId
                        )
                        .orElseGet(
                                () -> new Consent(
                                        patient,
                                        provider
                                )
                        );

        consent.activate();

        return consentRepository.save(
                consent
        );
    }

    /**
     * Revokes Provider access and immediately cancels all unresolved
     * Health Record proposals between the same Provider and Patient.
     *
     * Both operations occur within the same transaction. Therefore access
     * revocation cannot successfully complete while leaving the Provider's
     * old Health Record proposals actionable.
     */
    @Transactional
    public void revokeConsent(
            Long patientId,
            Long providerId) {

        Consent consent =
                consentRepository
                        .findByPatient_IdAndProvider_Id(
                                patientId,
                                providerId
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Consent was not found"
                                )
                        );

        consent.revoke();

        consentRepository.save(
                consent
        );

        /*
         * A Provider must not retain actionable Health Record proposals
         * after the Patient revokes access.
         */
        healthRecordChangeRequestCancellationService
                .cancelPendingRequestsForRevokedConsent(
                        patientId,
                        providerId
                );
    }

    @Transactional
    public boolean hasActiveConsent(
            Long patientId,
            Long providerId) {

        return consentRepository
                .findByPatient_IdAndProvider_IdAndStatus(
                        patientId,
                        providerId,
                        ConsentStatus.ACTIVE
                )
                .isPresent();
    }

    @Transactional
    public List<User> getAvailableProviders() {

        return userRepository
                .findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(
                        Role.PROVIDER
                );
    }

    @Transactional
    public List<Consent> getPatientConsents(
            Long patientId) {

        return consentRepository
                .findByPatient_Id(
                        patientId
                );
    }

    @Transactional
    public List<User> getAuthorizedPatients(
            Long providerId) {

        User provider =
                getUser(
                        providerId
                );

        if (provider.getRole()
                != Role.PROVIDER) {

            throw new IllegalArgumentException(
                    "User must have the PROVIDER role"
            );
        }

        return consentRepository
                .findByProvider_IdAndStatus(
                        providerId,
                        ConsentStatus.ACTIVE
                )
                .stream()
                .map(
                        Consent::getPatient
                )
                .toList();
    }

    private User getUser(
            Long userId) {

        return userRepository
                .findById(
                        userId
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "User was not found: "
                                + userId
                        )
                );
    }

    private void validateRoles(
            User patient,
            User provider) {

        if (patient.getRole()
                != Role.PATIENT) {

            throw new IllegalArgumentException(
                    "Consent owner must have the PATIENT role"
            );
        }

        if (provider.getRole()
                != Role.PROVIDER) {

            throw new IllegalArgumentException(
                    "Consent recipient must have the PROVIDER role"
            );
        }
    }
}