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
 * A single reporting period is used to generate both health-measurement
 * analytics and activity analytics. The controller coordinates the existing
 * analytics services but does not perform statistical calculations itself.
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

        this.healthAnalyticsService = healthAnalyticsService;
        this.activityAnalyticsService = activityAnalyticsService;
        this.currentUserService = currentUserService;
    }

    /**
     * Displays the analytics dashboard and, when a complete reporting period
     * is supplied, generates health and activity analytics for that period.
     *
     * @param period reporting period supplied through query parameters
     * @param model Thymeleaf model
     * @return patient analytics template
     */
    @GetMapping
    public String showAnalytics(
            @ModelAttribute("period")
            AnalyticsPeriodRequest period,
            Model model) {

        /*
         * Used by the date inputs so the browser can prevent selection
         * of future dates.
         */
        model.addAttribute(
                "today",
                LocalDate.now()
        );

        /*
         * Initial visit:
         *
         * /patient/analytics
         *
         * Neither date is present, so simply show the reporting-period form.
         */
        if (period.isEmpty()) {

            model.addAttribute(
                    "generated",
                    false
            );

            return "analytics/patient-analytics";
        }

        /*
         * One date was supplied but the other was not.
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

            validateReportingPeriod(period);

            User currentUser =
                    currentUserService.getCurrentUser();

            HealthAnalyticsSummary healthAnalytics =
                    healthAnalyticsService.getPatientAnalytics(
                            currentUser,
                            period.getStartDate(),
                            period.getEndDate()
                    );

            ActivityAnalyticsSummary activityAnalytics =
                    activityAnalyticsService.getPatientAnalytics(
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

            /*
             * Expected reporting-period validation problems should return the
             * patient to the same dashboard with a useful message rather than
             * displaying a generic application error page.
             */
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
     * Performs presentation-level validation before either analytics service
     * is called.
     *
     * The analytics services perform their own validation as well. Keeping
     * those service checks is important because business logic must remain
     * safe even when called from somewhere other than this controller.
     */
    private void validateReportingPeriod(
            AnalyticsPeriodRequest period) {

        LocalDate startDate =
                period.getStartDate();

        LocalDate endDate =
                period.getEndDate();

        if (startDate.isAfter(endDate)) {

            throw new IllegalArgumentException(
                    "Start date must not be after end date."
            );
        }

        if (endDate.isAfter(LocalDate.now())) {

            throw new IllegalArgumentException(
                    "End date must not be in the future."
            );
        }
    }
}