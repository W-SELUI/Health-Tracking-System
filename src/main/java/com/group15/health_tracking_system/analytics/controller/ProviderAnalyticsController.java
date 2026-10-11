package com.group15.health_tracking_system.analytics.controller;

import com.group15.health_tracking_system.analytics.dto.AnalyticsPeriodRequest;
import com.group15.health_tracking_system.analytics.dto.ProviderPatientAnalytics;
import com.group15.health_tracking_system.analytics.service.ProviderAnalyticsService;
import com.group15.health_tracking_system.user.entity.User;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Displays an authorized patient's Analytics dashboard to a healthcare
 * Provider.
 *
 * This controller does not calculate analytics and does not perform direct
 * database access. ProviderAnalyticsService verifies ACTIVE Consent before
 * returning the selected patient or any analytics information.
 *
 * The same Thymeleaf template used by the Patient dashboard is reused here,
 * ensuring both users see the same statistics, graphs and dashboard layout.
 */
@Controller
@RequestMapping(
        "/provider/patients/{patientId}/analytics"
)
@PreAuthorize("hasRole('PROVIDER')")
public class ProviderAnalyticsController {

    private final ProviderAnalyticsService providerAnalyticsService;

    public ProviderAnalyticsController(
            ProviderAnalyticsService providerAnalyticsService) {

        this.providerAnalyticsService =
                providerAnalyticsService;
    }

    /**
     * Displays the selected patient's analytics dashboard.
     *
     * @param patientId target patient
     * @param period selected reporting period
     * @param model Thymeleaf model
     * @return shared analytics template
     */
    @GetMapping
    public String showAnalytics(
            @PathVariable Long patientId,
            @ModelAttribute("period")
            AnalyticsPeriodRequest period,
            Model model) {

        /*
         * Authorization occurs even on the initial page before analytics
         * have been generated.
         *
         * Therefore a Provider cannot use the page itself to discover
         * information about unauthorized patients.
         */
        User patient =
                providerAnalyticsService
                        .getAuthorizedPatient(
                                patientId
                        );

        configureProviderView(
                model,
                patient
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
         * Only one reporting-period date supplied.
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

            /*
             * ProviderAnalyticsService checks ACTIVE Consent again before
             * retrieving analytics. This is deliberate defence in depth.
             */
            ProviderPatientAnalytics analytics =
                    providerAnalyticsService
                            .getPatientAnalytics(
                                    patientId,
                                    period.getStartDate(),
                                    period.getEndDate()
                            );

            model.addAttribute(
                    "healthAnalytics",
                    analytics.getHealthAnalytics()
            );

            model.addAttribute(
                    "activityAnalytics",
                    analytics.getActivityAnalytics()
            );

            model.addAttribute(
                    "hasAnyData",
                    analytics.isHasAnyData()
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
     * Supplies Provider-specific navigation and patient information while
     * retaining the shared Analytics dashboard.
     */
    private void configureProviderView(
            Model model,
            User patient) {

        model.addAttribute(
                "pageTitle",
                "Patient Health Analytics"
        );

        model.addAttribute(
                "pageHeading",
                "Health & Activity Analytics"
        );

        model.addAttribute(
                "pageDescription",
                "Review the patient's recorded health measurements "
                + "and activity for a selected reporting period."
        );

        model.addAttribute(
                "viewedPatient",
                patient
        );

        model.addAttribute(
                "analyticsAction",
                "/provider/patients/"
                + patient.getId()
                + "/analytics"
        );

        model.addAttribute(
                "backUrl",
                "/provider/patients"
        );

        model.addAttribute(
                "backLabel",
                "Back to Patient Access"
        );

        model.addAttribute(
                "providerView",
                true
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