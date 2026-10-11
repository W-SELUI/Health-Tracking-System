package com.group15.health_tracking_system.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group15.health_tracking_system.activity.ActivityForm;
import com.group15.health_tracking_system.activity.ActivityService;
import com.group15.health_tracking_system.activity.ExerciseType;
import com.group15.health_tracking_system.analytics.dto.ActivityAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.HealthAnalyticsSummary;
import com.group15.health_tracking_system.analytics.dto.ProviderPatientAnalytics;
import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for consent-protected Provider Analytics.
 *
 * <p>
 * These tests verify that a healthcare provider can view a patient's
 * Health and Activity analytics only while ACTIVE Consent exists.
 * They also verify that the Provider receives analytics for the correct
 * patient and that Provider results match the same analytics shown to
 * that patient.
 * </p>
 */
@SpringBootTest
@Transactional
class ProviderAnalyticsServiceIntegrationTests {

    @Autowired
    private ProviderAnalyticsService providerAnalyticsService;

    @Autowired
    private HealthAnalyticsService healthAnalyticsService;

    @Autowired
    private ActivityAnalyticsService activityAnalyticsService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patientA;
    private User patientB;

    private User providerA;
    private User providerB;

    private LocalDate startDate;
    private LocalDate endDate;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        String passwordHash =
                passwordEncoder.encode(
                        "ProviderAnalytics123!"
                );

        patientA =
                saveUser(
                        "Patient",
                        "Alpha",
                        "provider.analytics.patient.a@example.com",
                        Role.PATIENT,
                        passwordHash
                );

        patientB =
                saveUser(
                        "Patient",
                        "Beta",
                        "provider.analytics.patient.b@example.com",
                        Role.PATIENT,
                        passwordHash
                );

        providerA =
                saveUser(
                        "Provider",
                        "Alpha",
                        "provider.analytics.provider.a@example.com",
                        Role.PROVIDER,
                        passwordHash
                );

        providerB =
                saveUser(
                        "Provider",
                        "Beta",
                        "provider.analytics.provider.b@example.com",
                        Role.PROVIDER,
                        passwordHash
                );

        /*
         * Keep all analytics data safely in the past.
         */
        startDate =
                LocalDate.now()
                        .minusDays(10);

        endDate =
                startDate.plusDays(4);

        /*
         * Patient A Health data.
         */
        saveHealthRecord(
                patientA,
                HealthMetricType.WEIGHT,
                "80",
                startDate.atTime(8, 0)
        );

        saveHealthRecord(
                patientA,
                HealthMetricType.WEIGHT,
                "78",
                startDate.plusDays(2)
                        .atTime(8, 0)
        );

        /*
         * Patient B gets intentionally different Health data.
         * This allows the tests to detect cross-patient leakage.
         */
        saveHealthRecord(
                patientB,
                HealthMetricType.WEIGHT,
                "95",
                startDate.plusDays(1)
                        .atTime(9, 0)
        );

        /*
         * Activity records must be created through ActivityService because
         * Activity ownership is intentionally derived from the authenticated
         * Patient rather than supplied by the caller.
         */
        createActivityFor(
                patientA,
                startDate,
                4000,
                ExerciseType.WALKING,
                30,
                "150.00"
        );

        createActivityFor(
                patientA,
                startDate.plusDays(2),
                6000,
                ExerciseType.RUNNING,
                40,
                "250.00"
        );

        /*
         * Patient B again receives clearly different values.
         */
        createActivityFor(
                patientB,
                startDate.plusDays(1),
                12000,
                ExerciseType.CYCLING,
                60,
                "500.00"
        );

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearAuthentication() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void activeConsentAllowsCombinedHealthAndActivityAnalytics() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderPatientAnalytics result =
                providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        );

        assertEquals(
                patientA.getId(),
                result.getPatient().getId()
        );

        assertTrue(
                result.getHealthAnalytics()
                        .isHasData()
        );

        assertTrue(
                result.getActivityAnalytics()
                        .isHasData()
        );

        assertTrue(
                result.isHasAnyData()
        );

        /*
         * Patient A owns exactly two scalar Health readings.
         */
        assertEquals(
                2,
                result.getHealthAnalytics()
                        .getTotalReadingCount()
        );

        /*
         * Patient A owns exactly two Activity logs.
         */
        assertEquals(
                2,
                result.getActivityAnalytics()
                        .getLogCount()
        );

        assertEquals(
                10000,
                result.getActivityAnalytics()
                        .getTotalSteps()
        );

        assertBigDecimalEquals(
                "400.00",
                result.getActivityAnalytics()
                        .getTotalCaloriesBurned()
        );
    }

    @Test
    void providerAnalyticsMatchesPatientsOwnAnalyticsForSamePeriod() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        /*
         * First generate the Provider result.
         */
        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderPatientAnalytics providerResult =
                providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        );

        /*
         * Then generate exactly what Patient A sees through the existing
         * Patient analytics path.
         */
        logInAs(
                patientA,
                Role.PATIENT
        );

        HealthAnalyticsSummary patientHealth =
                healthAnalyticsService
                        .getPatientAnalytics(
                                patientA,
                                startDate,
                                endDate
                        );

        ActivityAnalyticsSummary patientActivity =
                activityAnalyticsService
                        .getPatientAnalytics(
                                startDate,
                                endDate
                        );

        HealthAnalyticsSummary providerHealth =
                providerResult.getHealthAnalytics();

        ActivityAnalyticsSummary providerActivity =
                providerResult.getActivityAnalytics();

        assertEquals(
                patientHealth.getTotalReadingCount(),
                providerHealth.getTotalReadingCount()
        );

        assertEquals(
                patientHealth.getMetricTypesWithData(),
                providerHealth.getMetricTypesWithData()
        );

        assertEquals(
                patientHealth.isHasData(),
                providerHealth.isHasData()
        );

        assertEquals(
                patientActivity.getLogCount(),
                providerActivity.getLogCount()
        );

        assertEquals(
                patientActivity.getDaysWithEntries(),
                providerActivity.getDaysWithEntries()
        );

        assertEquals(
                patientActivity.getTotalSteps(),
                providerActivity.getTotalSteps()
        );

        assertBigDecimalEquals(
                patientActivity
                        .getTrackingCoveragePercentage()
                        .toPlainString(),
                providerActivity
                        .getTrackingCoveragePercentage()
        );

        assertBigDecimalEquals(
                patientActivity
                        .getTotalCaloriesBurned()
                        .toPlainString(),
                providerActivity
                        .getTotalCaloriesBurned()
        );

        assertEquals(
                patientActivity.getTotalDurationMinutes(),
                providerActivity.getTotalDurationMinutes()
        );

        /*
         * The chronological chart data must also be the same.
         */
        assertEquals(
                patientActivity
                        .getDailyDataPoints()
                        .size(),
                providerActivity
                        .getDailyDataPoints()
                        .size()
        );

        for (int index = 0;
                index < patientActivity
                        .getDailyDataPoints()
                        .size();
                index++) {

            var patientPoint =
                    patientActivity
                            .getDailyDataPoints()
                            .get(index);

            var providerPoint =
                    providerActivity
                            .getDailyDataPoints()
                            .get(index);

            assertEquals(
                    patientPoint.getDate(),
                    providerPoint.getDate()
            );

            assertEquals(
                    patientPoint.getSteps(),
                    providerPoint.getSteps()
            );

            assertEquals(
                    patientPoint.getDurationMinutes(),
                    providerPoint.getDurationMinutes()
            );

            assertBigDecimalEquals(
                    patientPoint
                            .getCaloriesBurned()
                            .toPlainString(),
                    providerPoint
                            .getCaloriesBurned()
            );

            assertEquals(
                    patientPoint.getLogCount(),
                    providerPoint.getLogCount()
            );
        }
    }

    @Test
    void missingConsentBlocksProviderAnalytics() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        assertThrows(
                AccessDeniedException.class,
                () -> providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        )
        );
    }

    @Test
    void anotherProviderCannotReuseExistingConsent() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        logInAs(
                providerB,
                Role.PROVIDER
        );

        assertThrows(
                AccessDeniedException.class,
                () -> providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        )
        );
    }

    @Test
    void consentForOnePatientDoesNotExposeAnotherPatientsAnalytics() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        assertThrows(
                AccessDeniedException.class,
                () -> providerAnalyticsService
                        .getPatientAnalytics(
                                patientB.getId(),
                                startDate,
                                endDate
                        )
        );
    }

    @Test
    void analyticsContainOnlyTheAuthorizedPatientsData() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderPatientAnalytics result =
                providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        );

        /*
         * Patient B has 12,000 steps, but Patient A has only 10,000.
         * If ownership filtering failed this assertion would expose it.
         */
        assertEquals(
                10000,
                result.getActivityAnalytics()
                        .getTotalSteps()
        );

        /*
         * Patient B's 95 kg reading must not appear in Patient A's
         * Health analytics.
         */
        var weightSummary =
                result.getHealthAnalytics()
                        .getMetricSummaries()
                        .stream()
                        .filter(summary ->
                                summary.getMetricType()
                                        == HealthMetricType.WEIGHT
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                2,
                weightSummary.getReadingCount()
        );

        assertBigDecimalEquals(
                "78",
                weightSummary.getLatestValue()
        );

        assertBigDecimalEquals(
                "79",
                weightSummary.getAverageValue()
        );
    }

    @Test
    void revokingConsentImmediatelyBlocksAnalyticsWithoutRelogin() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderPatientAnalytics beforeRevocation =
                providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        );

        assertTrue(
                beforeRevocation.isHasAnyData()
        );

        consentService.revokeConsent(
                patientA.getId(),
                providerA.getId()
        );

        /*
         * Same authenticated Provider session.
         * Consent revocation must take effect immediately.
         */
        assertThrows(
                AccessDeniedException.class,
                () -> providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        )
        );
    }

    @Test
    void patientCannotUseProviderAnalyticsService() {

        logInAs(
                patientA,
                Role.PATIENT
        );

        assertThrows(
                AccessDeniedException.class,
                () -> providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        )
        );
    }

    @Test
    void unauthenticatedCallerCannotUseProviderAnalyticsService() {

        SecurityContextHolder.clearContext();

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        )
        );
    }

    @Test
    void disabledProviderCannotUseExistingConsent() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        /*
         * Confirm the access worked before disabling the Provider.
         */
        assertEquals(
                patientA.getId(),
                providerAnalyticsService
                        .getAuthorizedPatient(
                                patientA.getId()
                        )
                        .getId()
        );

        providerA.setEnabled(false);

        userRepository.saveAndFlush(
                providerA
        );

        assertThrows(
                AccessDeniedException.class,
                () -> providerAnalyticsService
                        .getPatientAnalytics(
                                patientA.getId(),
                                startDate,
                                endDate
                        )
        );
    }

    /**
     * Persists one application User.
     */
    private User saveUser(
            String firstName,
            String lastName,
            String email,
            Role role,
            String passwordHash) {

        return userRepository.save(
                new User(
                        firstName,
                        lastName,
                        email,
                        passwordHash,
                        role
                )
        );
    }

    /**
     * Persists one scalar Health Record belonging to the selected Patient.
     */
    private void saveHealthRecord(
            User patient,
            HealthMetricType metricType,
            String value,
            LocalDateTime observedAt) {

        healthRecordRepository.save(
                new HealthRecord(
                        patient,
                        patient,
                        metricType,
                        new BigDecimal(value),
                        observedAt,
                        "Provider analytics integration test.",
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }

    /**
     * Creates Activity data through the real ActivityService.
     *
     * <p>
     * This preserves Shaniza's ownership rule: the Patient is determined
     * from authentication rather than supplied directly to create().
     * </p>
     */
    private void createActivityFor(
            User patient,
            LocalDate date,
            int steps,
            ExerciseType exerciseType,
            Integer durationMinutes,
            String calories) {

        logInAs(
                patient,
                Role.PATIENT
        );

        ActivityForm form =
                new ActivityForm();

        form.setActivityDate(
                date
        );

        form.setSteps(
                steps
        );

        form.setExerciseType(
                exerciseType
        );

        form.setDurationMinutes(
                durationMinutes
        );

        form.setCaloriesBurned(
                new BigDecimal(
                        calories
                )
        );

        activityService.create(
                form
        );
    }

    /**
     * Replaces the authenticated account used by Spring Security.
     */
    private void logInAs(
            User account,
            Role authorityRole) {

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        account.getEmail(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_"
                                        + authorityRole.name()
                                )
                        )
                );

        SecurityContext context =
                SecurityContextHolder
                        .createEmptyContext();

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                context
        );
    }

    /**
     * Compares BigDecimal values numerically rather than by scale.
     */
    private void assertBigDecimalEquals(
            String expected,
            BigDecimal actual) {

        assertEquals(
                0,
                new BigDecimal(
                        expected
                ).compareTo(
                        actual
                )
        );
    }
}