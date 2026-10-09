package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
public class ProviderAccessService {

    private final ConsentService consentService;
    private final CurrentUserService currentUserService;

    public ProviderAccessService(
            ConsentService consentService,
            CurrentUserService currentUserService
    ) {
        this.consentService = consentService;
        this.currentUserService = currentUserService;
    }

    /**
     * Checks the signed-in provider's consent for the requested patient.
     * Call this before retrieving the patient's health information.
     */
    @PreAuthorize("hasRole('PROVIDER')")
    public void requireActiveConsent(Long patientId) {
        User provider = currentUserService.getCurrentUser();

        if (patientId == null
                || provider.getRole() != Role.PROVIDER
                || !provider.isEnabled()
                || !consentService.hasActiveConsent(
                        patientId,
                        provider.getId()
                )) {
            throw new AccessDeniedException(
                    "You do not have permission to view this patient's health records."
            );
        }
    }
}