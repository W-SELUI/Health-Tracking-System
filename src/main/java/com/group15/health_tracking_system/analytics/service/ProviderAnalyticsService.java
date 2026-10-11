package com.group15.health_tracking_system.analytics.service;

import com.group15.health_tracking_system.analytics.dto.ActivityAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.HealthAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.ProviderPatientAnalytics;
import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.service.ProviderAccessService;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Provides patient analytics to healthcare providers who currently have
 * active patient Consent.
 *
 * This service is the authorization boundary between Provider Access and
 * Analytics. It does not contain any statistical calculations itself.
 * Instead, after confirming active Consent, it delegates to the same
 * HealthAnalyticsService and ActivityAnalyticsService used by the
 * patient's own analytics dashboard.
 *
 * Therefore a Patient and an authorized Provider viewing the same patient
 * and reporting period receive analytics generated from the same stored
 * records and the same calculation logic.
 */
@Service
@Transactional(readOnly = true)
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderAnalyticsService {

    private final ProviderAccessService providerAccessService;
    private final UserRepository userRepository;

    private final HealthAnalyticsService healthAnalyticsService;
    private final ActivityAnalyticsService activityAnalyticsService;

    public ProviderAnalyticsService(
            ProviderAccessService providerAccessService,
            UserRepository userRepository,
            HealthAnalyticsService healthAnalyticsService,
            ActivityAnalyticsService activityAnalyticsService) {

        this.providerAccessService =
                providerAccessService;

        this.userRepository =
                userRepository;

        this.healthAnalyticsService =
                healthAnalyticsService;

        this.activityAnalyticsService =
                activityAnalyticsService;
    }

    /**
     * Returns the selected patient after verifying that the signed-in
     * Provider currently has active Consent for that patient.
     *
     * This method can be used by the Provider analytics page before a
     * reporting period has been selected so that the page can still display
     * the patient's identity safely.
     *
     * @param patientId requested patient identifier
     * @return authorized patient
     */
    public User getAuthorizedPatient(
            Long patientId) {

        return requireAuthorizedPatient(
                patientId
        );
    }

    /**
     * Generates the same Health and Activity analytics that the patient
     * would receive for the selected reporting period.
     *
     * Authorization is checked before either analytics service is called.
     * A Provider without active Consent therefore cannot use this service
     * to retrieve or calculate another patient's analytics.
     *
     * @param patientId target patient identifier
     * @param startDate first date in the reporting period
     * @param endDate last date in the reporting period
     * @return authorized patient's combined analytics result
     */
    public ProviderPatientAnalytics getPatientAnalytics(
            Long patientId,
            LocalDate startDate,
            LocalDate endDate) {

        User patient =
                requireAuthorizedPatient(
                        patientId
                );

        HealthAnalyticsSummary healthAnalytics =
                healthAnalyticsService
                        .getPatientAnalytics(
                                patient,
                                startDate,
                                endDate
                        );

        ActivityAnalyticsSummary activityAnalytics =
                activityAnalyticsService
                        .getPatientAnalytics(
                                patient.getId(),
                                startDate,
                                endDate
                        );

        return new ProviderPatientAnalytics(
                patient,
                healthAnalytics,
                activityAnalytics
        );
    }

    /**
     * Verifies Provider authorization and resolves the target Patient.
     *
     * Consent is checked before patient data is returned.
     */
    private User requireAuthorizedPatient(
            Long patientId) {

        /*
         * ProviderAccessService resolves the actually authenticated Provider
         * and verifies that ACTIVE Consent exists for this exact patient.
         */
        providerAccessService.requireActiveConsent(
                patientId
        );

        return userRepository
                .findById(
                        patientId
                )
                .filter(user ->
                        user.getRole()
                                == Role.PATIENT
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Patient was not found."
                        )
                );
    }
}