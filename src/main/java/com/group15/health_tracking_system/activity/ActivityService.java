package com.group15.health_tracking_system.activity;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles Activity Log creation, modification and patient-scoped querying.
 *
 * Normal patient-facing operations obtain the patient identity from
 * {@link CurrentPatient}. Read-only patient-ID based query methods are also
 * provided so higher-level services, such as the consent-protected Provider
 * Analytics workflow, can analyse a specific patient's activity data.
 *
 * The explicit patient query methods do not themselves grant Provider
 * authorization. A Provider caller must first pass the application's
 * Provider consent checks before invoking analytics based on these methods.
 */
@Service
@Transactional
public class ActivityService {

    private final ActivityLogRepository repository;
    private final CurrentPatient currentPatient;
    private final Validator validator;

    public ActivityService(
            ActivityLogRepository repository,
            CurrentPatient currentPatient,
            Validator validator) {

        this.repository = repository;
        this.currentPatient = currentPatient;
        this.validator = validator;
    }

    /**
     * Creates an Activity Log belonging to the authenticated patient.
     *
     * @param form activity form
     * @return persisted activity log
     */
    public ActivityLog create(
            ActivityForm form) {

        var patient =
                currentPatient.requirePatient();

        validate(form);

        return repository.save(
                new ActivityLog(
                        patient,
                        form
                )
        );
    }

    /**
     * Returns one Activity Log only when it belongs to the authenticated
     * patient.
     *
     * @param id activity identifier
     * @return owned activity
     */
    @Transactional(readOnly = true)
    public ActivityLog get(
            Long id) {

        return owned(
                id,
                currentPatient.requirePatientId()
        );
    }

    /**
     * Updates an Activity Log belonging to the authenticated patient.
     *
     * @param id activity identifier
     * @param form updated activity values
     * @return persisted activity
     */
    public ActivityLog edit(
            Long id,
            ActivityForm form) {

        ActivityLog log =
                owned(
                        id,
                        currentPatient.requirePatientId()
                );

        validate(form);

        log.apply(form);

        return repository.save(log);
    }

    /**
     * Deletes an Activity Log belonging to the authenticated patient.
     *
     * @param id activity identifier
     */
    public void delete(
            Long id) {

        repository.delete(
                owned(
                        id,
                        currentPatient.requirePatientId()
                )
        );
    }

    /**
     * Returns Activity Logs belonging to the authenticated patient within
     * an inclusive reporting period.
     *
     * @param start first date
     * @param end final date
     * @return matching Activity Logs
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> history(
            LocalDate start,
            LocalDate end) {

        return historyForPatient(
                currentPatient.requirePatientId(),
                start,
                end
        );
    }

    /**
     * Generates the existing Activity summary for the authenticated patient.
     *
     * @param start first date
     * @param end final date
     * @return activity summary
     */
    @Transactional(readOnly = true)
    public ActivitySummary summary(
            LocalDate start,
            LocalDate end) {

        return summaryForPatient(
                currentPatient.requirePatientId(),
                start,
                end
        );
    }

    /**
     * Returns Activity Logs belonging to an explicitly selected patient.
     *
     * This method exists for higher-level patient-scoped read workflows,
     * particularly Provider Analytics. It does not perform Provider consent
     * authorization itself. The calling Provider service must perform that
     * authorization before querying the target patient's data.
     *
     * @param patientId target patient identifier
     * @param start first date
     * @param end final date
     * @return matching Activity Logs
     */
    @Transactional(readOnly = true)
    public List<ActivityLog> historyForPatient(
            Long patientId,
            LocalDate start,
            LocalDate end) {

        requirePatientId(patientId);

        checkRange(
                start,
                end
        );

        return repository
                .findByPatient_IdAndActivityDateBetweenOrderByActivityDateDescIdDesc(
                        patientId,
                        start,
                        end
                );
    }

    /**
     * Generates an Activity summary for an explicitly selected patient.
     *
     * Patient-facing calls continue to use {@link #summary(LocalDate,
     * LocalDate)}. This explicit form allows the existing analytics
     * calculations to be reused after a Provider has been authorized for
     * the target patient.
     *
     * @param patientId target patient identifier
     * @param start first date
     * @param end final date
     * @return activity summary
     */
    @Transactional(readOnly = true)
    public ActivitySummary summaryForPatient(
            Long patientId,
            LocalDate start,
            LocalDate end) {

        List<ActivityLog> logs =
                historyForPatient(
                        patientId,
                        start,
                        end
                );

        return buildSummary(
                logs,
                start,
                end
        );
    }

    /**
     * Builds the shared Activity summary calculations.
     *
     * Both patient-facing analytics and future consent-protected Provider
     * analytics therefore use exactly the same calculations.
     */
    private ActivitySummary buildSummary(
            List<ActivityLog> logs,
            LocalDate start,
            LocalDate end) {

        long calendarDays =
                ChronoUnit.DAYS.between(
                        start,
                        end
                ) + 1;

        long totalSteps =
                logs.stream()
                        .mapToLong(
                                ActivityLog::getSteps
                        )
                        .sum();

        long totalDurationMinutes =
                logs.stream()
                        .mapToLong(log ->
                                log.getDurationMinutes() == null
                                        ? 0
                                        : log.getDurationMinutes()
                        )
                        .sum();

        BigDecimal totalCaloriesBurned =
                logs.stream()
                        .map(
                                ActivityLog::getCaloriesBurned
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        long daysWithEntries =
                logs.stream()
                        .map(
                                ActivityLog::getActivityDate
                        )
                        .distinct()
                        .count();

        BigDecimal averageStepsPerCalendarDay =
                BigDecimal.valueOf(
                        totalSteps
                )
                        .divide(
                                BigDecimal.valueOf(
                                        calendarDays
                                ),
                                2,
                                RoundingMode.HALF_UP
                        );

        return new ActivitySummary(
                logs.size(),
                daysWithEntries,
                calendarDays,
                totalSteps,
                totalDurationMinutes,
                totalCaloriesBurned,
                averageStepsPerCalendarDay
        );
    }

    /**
     * Returns an Activity Log only when it belongs to the specified patient.
     */
    private ActivityLog owned(
            Long id,
            Long patientId) {

        return repository
                .findByIdAndPatient_Id(
                        id,
                        patientId
                )
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Activity not found"
                        )
                );
    }

    /**
     * Performs Bean Validation on an Activity form.
     */
    private void validate(
            ActivityForm form) {

        var errors =
                validator.validate(form);

        if (!errors.isEmpty()) {

            throw new ConstraintViolationException(
                    errors
            );
        }
    }

    /**
     * Rejects an invalid explicit patient identifier.
     */
    private void requirePatientId(
            Long patientId) {

        if (patientId == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Patient is required."
            );
        }
    }

    /**
     * Validates an inclusive Activity reporting period.
     *
     * @param start first date
     * @param end final date
     */
    public static void checkRange(
            LocalDate start,
            LocalDate end) {

        if (start == null
                || end == null
                || start.isAfter(end)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Choose a valid date range: "
                    + "start must be on or before end."
            );
        }
    }
}