package com.group15.health_tracking_system.analytics.controller;

import com.group15.health_tracking_system.analytics.dto.ActivityAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.AnalyticsPeriodRequest;
import com.group15.health_tracking_system.analytics.dto.HealthAnalyticsSummary;
import com.group15.health_tracking_system.analytics.service.ActivityAnalyticsService;
import com.group15.health_tracking_system.analytics.service.HealthAnalyticsService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Handles the patient-facing analytics dashboard.
 *
 * A single reporting period is used to generate both Health and Activity
 * analytics. The same Thymeleaf dashboard is also reused by the authorized
 * Provider analytics workflow.
 */
@Controller
@RequestMapping("/patient/analytics")
public class PatientAnalyticsController {

    private final HealthAnalyticsService healthAnalyticsService;
    private final ActivityAnalyticsService activityAnalyticsService;
    private final CurrentUserService currentUserService;

    public PatientAnalyticsController(
            HealthAnalyticsService healthAnalyticsService,
            ActivityAnalyticsService activityAnalyticsService,
            CurrentUserService currentUserService) {

        this.healthAnalyticsService =
                healthAnalyticsService;

        this.activityAnalyticsService =
                activityAnalyticsService;

        this.currentUserService =
                currentUserService;
    }

    /**
     * Displays the patient's analytics dashboard.
     */
    @GetMapping
    public String showAnalytics(
            @ModelAttribute("period")
            AnalyticsPeriodRequest period,
            Model model) {

        configurePatientView(
                model
        );

        model.addAttribute(
                "today",
                LocalDate.now()
        );

        /*
         * Initial page visit.
         */
        if (period.isEmpty()) {

            model.addAttribute(
                    "generated",
                    false
            );

            return "analytics/patient-analytics";
        }

        /*
         * Only one date supplied.
         */
        if (!period.isComplete()) {

            model.addAttribute(
                    "generated",
                    false
            );

            model.addAttribute(
                    "periodError",
                    "Both start date and end date are required."
            );

            return "analytics/patient-analytics";
        }

        try {

            validateReportingPeriod(
                    period
            );

            User currentUser =
                    currentUserService
                            .getCurrentUser();

            HealthAnalyticsSummary healthAnalytics =
                    healthAnalyticsService
                            .getPatientAnalytics(
                                    currentUser,
                                    period.getStartDate(),
                                    period.getEndDate()
                            );

            ActivityAnalyticsSummary activityAnalytics =
                    activityAnalyticsService
                            .getPatientAnalytics(
                                    period.getStartDate(),
                                    period.getEndDate()
                            );

            boolean hasAnyData =
                    healthAnalytics.isHasData()
                    || activityAnalytics.isHasData();

            model.addAttribute(
                    "healthAnalytics",
                    healthAnalytics
            );

            model.addAttribute(
                    "activityAnalytics",
                    activityAnalytics
            );

            model.addAttribute(
                    "hasAnyData",
                    hasAnyData
            );

            model.addAttribute(
                    "generated",
                    true
            );

        } catch (IllegalArgumentException exception) {

            model.addAttribute(
                    "generated",
                    false
            );

            model.addAttribute(
                    "periodError",
                    exception.getMessage()
            );
        }

        return "analytics/patient-analytics";
    }

    /**
     * Supplies presentation settings for the Patient version of the shared
     * analytics dashboard.
     */
    private void configurePatientView(
            Model model) {

        model.addAttribute(
                "pageTitle",
                "Health analytics"
        );

        model.addAttribute(
                "pageHeading",
                "Health and activity analytics"
        );

        model.addAttribute(
                "pageDescription",
                "Review your recorded health measurements and activity "
                + "for a selected reporting period."
        );

        model.addAttribute(
                "analyticsAction",
                "/patient/analytics"
        );

        model.addAttribute(
                "backUrl",
                "/patient/dashboard"
        );

        model.addAttribute(
                "backLabel",
                "Back to dashboard"
        );

        model.addAttribute(
                "providerView",
                false
        );
    }

    /**
     * Performs presentation-level reporting-period validation.
     */
    private void validateReportingPeriod(
            AnalyticsPeriodRequest period) {

        LocalDate startDate =
                period.getStartDate();

        LocalDate endDate =
                period.getEndDate();

        if (startDate.isAfter(
                endDate
        )) {

            throw new IllegalArgumentException(
                    "Start date must not be after end date."
            );
        }

        if (endDate.isAfter(
                LocalDate.now()
        )) {

            throw new IllegalArgumentException(
                    "End date must not be in the future."
            );
        }
    }
}
