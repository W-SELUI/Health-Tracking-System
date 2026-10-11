package com.group15.health_tracking_system.analytics.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.analytics.dto.ActivityAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.HealthAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.ProviderPatientAnalytics;
import com.group15.health_tracking_system.analytics.service.ProviderAnalyticsService;
import com.group15.health_tracking_system.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the Provider Analytics controller.
 *
 *
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProviderAnalyticsControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderAnalyticsService providerAnalyticsService;

    private User patient;

    private HealthAnalyticsSummary healthSummary;
    private ActivityAnalyticsSummary activitySummary;

    private ProviderPatientAnalytics providerAnalytics;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {

        patient =
                mock(
                        User.class
                );

        when(
                patient.getId()
        ).thenReturn(
                101L
        );

        when(
                patient.getFullName()
        ).thenReturn(
                "Analytics Patient"
        );

        when(
                patient.getEmail()
        ).thenReturn(
                "analytics.patient@example.com"
        );

        healthSummary =
                mock(
                        HealthAnalyticsSummary.class
                );

        activitySummary =
                mock(
                        ActivityAnalyticsSummary.class
                );

        /*
         * Safe defaults required by the shared Thymeleaf dashboard.
         */
        when(
                healthSummary.getMetricSummaries()
        ).thenReturn(
                List.of()
        );

        when(
                healthSummary.getBloodPressureSummary()
        ).thenReturn(
                null
        );

        when(
                activitySummary.getDailyDataPoints()
        ).thenReturn(
                List.of()
        );

        when(
                activitySummary.getExerciseTypeAnalytics()
        ).thenReturn(
                List.of()
        );

        when(
                activitySummary.getTrackingCoveragePercentage()
        ).thenReturn(
                BigDecimal.ZERO
        );

        providerAnalytics =
                new ProviderPatientAnalytics(
                        patient,
                        healthSummary,
                        activitySummary
                );

        startDate =
                LocalDate.now()
                        .minusDays(10);

        endDate =
                LocalDate.now()
                        .minusDays(1);
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCanOpenAuthorizedPatientsInitialAnalyticsPage()
            throws Exception {

        when(
                providerAnalyticsService
                        .getAuthorizedPatient(
                                101L
                        )
        ).thenReturn(
                patient
        );

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "analytics/patient-analytics"
                        )
                )
                .andExpect(
                        model().attribute(
                                "generated",
                                false
                        )
                )
                .andExpect(
                        model().attribute(
                                "providerView",
                                true
                        )
                )
                .andExpect(
                        model().attribute(
                                "viewedPatient",
                                patient
                        )
                )
                .andExpect(
                        model().attribute(
                                "analyticsAction",
                                "/provider/patients/101/analytics"
                        )
                )
                .andExpect(
                        model().attribute(
                                "backUrl",
                                "/provider/patients"
                        )
                )
                .andExpect(
                        model().attribute(
                                "backLabel",
                                "Back to patient access"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "period"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "today"
                        )
                );

        verify(
                providerAnalyticsService
        ).getAuthorizedPatient(
                101L
        );

        /*
         * Initial page load must verify Consent, but should not calculate
         * analytics until a reporting period is submitted.
         */
        verify(
                providerAnalyticsService,
                never()
        ).getPatientAnalytics(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCannotAccessProviderAnalyticsRoute()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                providerAnalyticsService,
                never()
        ).getAuthorizedPatient(
                101L
        );
    }

    @Test
    void unauthenticatedUserCannotAccessProviderAnalyticsRoute()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                )
                .andExpect(
                        status().is3xxRedirection()
                );

        verify(
                providerAnalyticsService,
                never()
        ).getAuthorizedPatient(
                101L
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerWithoutActiveConsentCannotOpenAnalyticsPage()
            throws Exception {

        when(
                providerAnalyticsService
                        .getAuthorizedPatient(
                                101L
                        )
        ).thenThrow(
                new AccessDeniedException(
                        "You do not have permission to view this patient's health records."
                )
        );

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                providerAnalyticsService
        ).getAuthorizedPatient(
                101L
        );

        verify(
                providerAnalyticsService,
                never()
        ).getPatientAnalytics(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void authorizedProviderCanGeneratePatientAnalytics()
            throws Exception {

        when(
                providerAnalyticsService
                        .getAuthorizedPatient(
                                101L
                        )
        ).thenReturn(
                patient
        );

        when(
                providerAnalyticsService
                        .getPatientAnalytics(
                                101L,
                                startDate,
                                endDate
                        )
        ).thenReturn(
                providerAnalytics
        );

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                                .param(
                                        "startDate",
                                        startDate.toString()
                                )
                                .param(
                                        "endDate",
                                        endDate.toString()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "analytics/patient-analytics"
                        )
                )
                .andExpect(
                        model().attribute(
                                "generated",
                                true
                        )
                )
                .andExpect(
                        model().attribute(
                                "providerView",
                                true
                        )
                )
                .andExpect(
                        model().attribute(
                                "viewedPatient",
                                patient
                        )
                )
                .andExpect(
                        model().attribute(
                                "healthAnalytics",
                                healthSummary
                        )
                )
                .andExpect(
                        model().attribute(
                                "activityAnalytics",
                                activitySummary
                        )
                )
                .andExpect(
                        model().attribute(
                                "hasAnyData",
                                providerAnalytics.isHasAnyData()
                        )
                );

        verify(
                providerAnalyticsService
        ).getAuthorizedPatient(
                101L
        );

        verify(
                providerAnalyticsService
        ).getPatientAnalytics(
                101L,
                startDate,
                endDate
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void missingEndDateShowsValidationErrorWithoutGeneratingAnalytics()
            throws Exception {

        when(
                providerAnalyticsService
                        .getAuthorizedPatient(
                                101L
                        )
        ).thenReturn(
                patient
        );

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                                .param(
                                        "startDate",
                                        startDate.toString()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "analytics/patient-analytics"
                        )
                )
                .andExpect(
                        model().attribute(
                                "generated",
                                false
                        )
                )
                .andExpect(
                        model().attribute(
                                "periodError",
                                "Both start date and end date are required."
                        )
                );

        verify(
                providerAnalyticsService
        ).getAuthorizedPatient(
                101L
        );

        verify(
                providerAnalyticsService,
                never()
        ).getPatientAnalytics(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void startDateAfterEndDateShowsValidationError()
            throws Exception {

        when(
                providerAnalyticsService
                        .getAuthorizedPatient(
                                101L
                        )
        ).thenReturn(
                patient
        );

        LocalDate invalidStart =
                LocalDate.now()
                        .minusDays(1);

        LocalDate invalidEnd =
                LocalDate.now()
                        .minusDays(5);

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                                .param(
                                        "startDate",
                                        invalidStart.toString()
                                )
                                .param(
                                        "endDate",
                                        invalidEnd.toString()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        model().attribute(
                                "generated",
                                false
                        )
                )
                .andExpect(
                        model().attribute(
                                "periodError",
                                "Start date must not be after end date."
                        )
                );

        verify(
                providerAnalyticsService,
                never()
        ).getPatientAnalytics(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void futureEndDateShowsValidationError()
            throws Exception {

        when(
                providerAnalyticsService
                        .getAuthorizedPatient(
                                101L
                        )
        ).thenReturn(
                patient
        );

        LocalDate futureEnd =
                LocalDate.now()
                        .plusDays(1);

        mockMvc.perform(
                        get(
                                "/provider/patients/101/analytics"
                        )
                                .param(
                                        "startDate",
                                        startDate.toString()
                                )
                                .param(
                                        "endDate",
                                        futureEnd.toString()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        model().attribute(
                                "generated",
                                false
                        )
                )
                .andExpect(
                        model().attribute(
                                "periodError",
                                "End date must not be in the future."
                        )
                );

        verify(
                providerAnalyticsService,
                never()
        ).getPatientAnalytics(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
