package com.group15.health_tracking_system.analytics.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.analytics.dto.ActivityAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.HealthAnalyticsSummary;
import com.group15.health_tracking_system.analytics.service.ActivityAnalyticsService;
import com.group15.health_tracking_system.analytics.service.HealthAnalyticsService;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the patient analytics dashboard controller.
 *
 * These tests verify reporting-period handling, integration between the
 * Health Analytics and Activity Analytics services, dashboard states and
 * patient-only access.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PatientAnalyticsControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthAnalyticsService healthAnalyticsService;

    @MockitoBean
    private ActivityAnalyticsService activityAnalyticsService;

    @MockitoBean
    private CurrentUserService currentUserService;

    private User patient;

    private HealthAnalyticsSummary healthSummary;
    private ActivityAnalyticsSummary activitySummary;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {

        patient =
                new User(
                        "Analytics",
                        "Patient",
                        "analytics.patient@example.com",
                        "unused-test-password-hash",
                        Role.PATIENT
                );

        startDate =
                LocalDate.now()
                        .minusDays(10);

        endDate =
                LocalDate.now()
                        .minusDays(1);

        /*
         * Mock the analytics result DTOs so these tests focus specifically
         * on controller behaviour rather than recalculating statistics that
         * are already covered by the 2A and 2B service integration tests.
         */
        healthSummary =
                mock(
                        HealthAnalyticsSummary.class
                );

        activitySummary =
                mock(
                        ActivityAnalyticsSummary.class
                );

        /*
         * Safe defaults required by the Thymeleaf template.
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

        when(
                currentUserService.getCurrentUser()
        ).thenReturn(
                patient
        );
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void patientCanOpenInitialAnalyticsPage() throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/analytics"
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
                        model().attributeExists(
                                "period"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "today"
                        )
                );

        /*
         * Initial page load must not perform analytics calculations.
         */
        verifyNoInteractions(
                healthAnalyticsService,
                activityAnalyticsService
        );

        verify(
                currentUserService,
                never()
        ).getCurrentUser();
    }

    @Test
    void unauthenticatedUserCannotAccessAnalyticsPage()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/analytics"
                        )
                )
                .andExpect(
                        status().is3xxRedirection()
                );

        verifyNoInteractions(
                healthAnalyticsService,
                activityAnalyticsService,
                currentUserService
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCannotAccessPatientAnalyticsPage()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/analytics"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verifyNoInteractions(
                healthAnalyticsService,
                activityAnalyticsService,
                currentUserService
        );
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void missingEndDateShowsValidationError()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/analytics"
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

        verifyNoInteractions(
                healthAnalyticsService,
                activityAnalyticsService
        );

        verify(
                currentUserService,
                never()
        ).getCurrentUser();
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void missingStartDateShowsValidationError()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/analytics"
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
                                false
                        )
                )
                .andExpect(
                        model().attribute(
                                "periodError",
                                "Both start date and end date are required."
                        )
                );

        verifyNoInteractions(
                healthAnalyticsService,
                activityAnalyticsService
        );

        verify(
                currentUserService,
                never()
        ).getCurrentUser();
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void startDateAfterEndDateShowsValidationError()
            throws Exception {

        LocalDate invalidStart =
                endDate.plusDays(1);

        mockMvc.perform(
                        get(
                                "/patient/analytics"
                        )
                                .param(
                                        "startDate",
                                        invalidStart.toString()
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

        verifyNoInteractions(
                healthAnalyticsService,
                activityAnalyticsService
        );

        verify(
                currentUserService,
                never()
        ).getCurrentUser();
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void futureEndDateShowsValidationError()
            throws Exception {

        LocalDate futureEnd =
                LocalDate.now()
                        .plusDays(1);

        mockMvc.perform(
                        get(
                                "/patient/analytics"
                        )
                                .param(
                                        "startDate",
                                        LocalDate.now()
                                                .toString()
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

        verifyNoInteractions(
                healthAnalyticsService,
                activityAnalyticsService
        );

        verify(
                currentUserService,
                never()
        ).getCurrentUser();
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void validPeriodGeneratesNoDataStateWhenBothModulesAreEmpty()
            throws Exception {

        when(
                healthSummary.isHasData()
        ).thenReturn(
                false
        );

        when(
                activitySummary.isHasData()
        ).thenReturn(
                false
        );

        configureValidAnalyticsResponses();

        mockMvc.perform(
                        validAnalyticsRequest()
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
                                "hasAnyData",
                                false
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
                );

        verifyAnalyticsServicesWereCalled();
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void healthOnlyDataProducesPartialDataState()
            throws Exception {

        when(
                healthSummary.isHasData()
        ).thenReturn(
                true
        );

        when(
                activitySummary.isHasData()
        ).thenReturn(
                false
        );

        configureValidAnalyticsResponses();

        mockMvc.perform(
                        validAnalyticsRequest()
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        model().attribute(
                                "generated",
                                true
                        )
                )
                .andExpect(
                        model().attribute(
                                "hasAnyData",
                                true
                        )
                );

        verifyAnalyticsServicesWereCalled();
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void activityOnlyDataProducesPartialDataState()
            throws Exception {

        when(
                healthSummary.isHasData()
        ).thenReturn(
                false
        );

        when(
                activitySummary.isHasData()
        ).thenReturn(
                true
        );

        configureValidAnalyticsResponses();

        mockMvc.perform(
                        validAnalyticsRequest()
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        model().attribute(
                                "generated",
                                true
                        )
                )
                .andExpect(
                        model().attribute(
                                "hasAnyData",
                                true
                        )
                );

        verifyAnalyticsServicesWereCalled();
    }

    @Test
    @WithMockUser(
            username = "analytics.patient@example.com",
            roles = "PATIENT"
    )
    void bothHealthAndActivityDataProduceSuccessfulDashboard()
            throws Exception {

        when(
                healthSummary.isHasData()
        ).thenReturn(
                true
        );

        when(
                activitySummary.isHasData()
        ).thenReturn(
                true
        );

        configureValidAnalyticsResponses();

        mockMvc.perform(
                        validAnalyticsRequest()
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
                                "hasAnyData",
                                true
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "healthAnalytics"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "activityAnalytics"
                        )
                );

        verifyAnalyticsServicesWereCalled();
    }

    /**
     * Configures the two completed Analytics Core services for a valid
     * reporting-period request.
     */
    private void configureValidAnalyticsResponses() {

        when(
                healthAnalyticsService.getPatientAnalytics(
                        patient,
                        startDate,
                        endDate
                )
        ).thenReturn(
                healthSummary
        );

        when(
                activityAnalyticsService.getPatientAnalytics(
                        startDate,
                        endDate
                )
        ).thenReturn(
                activitySummary
        );
    }

    /**
     * Creates the standard valid Analytics dashboard request.
     */
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
            validAnalyticsRequest() {

        return get(
                "/patient/analytics"
        )
                .param(
                        "startDate",
                        startDate.toString()
                )
                .param(
                        "endDate",
                        endDate.toString()
                );
    }

    /**
     * Verifies that the controller uses the authenticated patient and the
     * same reporting period for both Analytics Core services.
     */
    private void verifyAnalyticsServicesWereCalled() {

        verify(
                currentUserService
        ).getCurrentUser();

        verify(
                healthAnalyticsService
        ).getPatientAnalytics(
                patient,
                startDate,
                endDate
        );

        verify(
                activityAnalyticsService
        ).getPatientAnalytics(
                startDate,
                endDate
        );
    }
}