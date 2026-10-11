package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DoseRecordService {

    private final DoseRecordRepository doseRecordRepository;

    public DoseRecordService(
            DoseRecordRepository doseRecordRepository) {

        this.doseRecordRepository =
                doseRecordRepository;
    }

    /*
     * Get all dose records belonging
     * to one patient.
     */
    public List<DoseRecord> getByUserId(
            Long userId) {

        return doseRecordRepository
                .findByMedication_User_IdOrderByDoseDateDescScheduledTimeAsc(
                        userId);
    }

    /*
     * Get one dose record only if
     * it belongs to the patient.
     */
    public DoseRecord getByIdAndUserId(
            Long id,
            Long userId) {

        return doseRecordRepository
                .findByIdAndMedication_User_Id(
                        id,
                        userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Dose record not found for this patient."));
    }

    /*
     * Get dose records for one patient
     * within a selected date range.
     */
    public List<DoseRecord> getByUserIdAndDateRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(
                startDate,
                endDate);

        return doseRecordRepository
                .findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                        userId,
                        startDate,
                        endDate);
    }

    /*
     * Find one exact scheduled dose.
     */
    public DoseRecord getByScheduleDateAndTime(
            Long scheduleId,
            LocalDate doseDate,
            LocalTime scheduledTime) {

        return doseRecordRepository
                .findBySchedule_IdAndDoseDateAndScheduledTime(
                        scheduleId,
                        doseDate,
                        scheduledTime)
                .orElse(null);
    }

    /*
     * Check whether one exact scheduled
     * occurrence already has a record.
     */
    public boolean existsForScheduleDateAndTime(
            Long scheduleId,
            LocalDate doseDate,
            LocalTime scheduledTime) {

        return doseRecordRepository
                .existsBySchedule_IdAndDoseDateAndScheduledTime(
                        scheduleId,
                        doseDate,
                        scheduledTime);
    }

    /*
     * Get all recorded doses for one
     * schedule on one date.
     */
    public List<DoseRecord> getByScheduleAndDate(
            Long scheduleId,
            LocalDate doseDate) {

        return doseRecordRepository
                .findBySchedule_IdAndDoseDateOrderByScheduledTimeAsc(
                        scheduleId,
                        doseDate);
    }

    /*
     * Calculate all valid dose times for
     * one schedule on one selected date.
     *
     * Example:
     *
     * Start:
     * 2026-10-11 06:00
     *
     * Interval:
     * 6 hours
     *
     * Results for 2026-10-11:
     * 06:00
     * 12:00
     * 18:00
     *
     * Results for 2026-10-12:
     * 00:00
     * 06:00
     * 12:00
     * 18:00
     */
    public List<LocalTime> getScheduledTimesForDate(
            MedicationSchedule schedule,
            LocalDate doseDate) {

        validateSchedule(
                schedule);

        if (doseDate == null) {

            throw new IllegalArgumentException(
                    "Dose date is required.");
        }

        /*
         * Date before schedule begins.
         */
        if (doseDate.isBefore(
                schedule.getStartDate())) {

            return List.of();
        }

        /*
         * Date after schedule finishes.
         */
        if (schedule.getEndDate() != null
                && doseDate.isAfter(
                        schedule.getEndDate())) {

            return List.of();
        }

        LocalDateTime scheduleStart =
                schedule
                        .getStartDate()
                        .atTime(
                                schedule.getScheduledTime());

        LocalDateTime dayStart =
                doseDate.atStartOfDay();

        LocalDateTime dayEnd =
                doseDate.atTime(
                        LocalTime.MAX);

        long intervalMinutes =
                schedule.getIntervalHours()
                        * 60L;

        LocalDateTime occurrence;

        /*
         * If the selected day contains the
         * very first dose, begin there.
         */
        if (!dayStart.isAfter(scheduleStart)) {

            occurrence =
                    scheduleStart;

        } else {

            /*
             * Jump directly to the first
             * occurrence on or after this day.
             */
            long minutesFromStart =
                    Duration.between(
                            scheduleStart,
                            dayStart)
                            .toMinutes();

            long intervalsPassed =
                    (minutesFromStart
                            + intervalMinutes
                            - 1)
                            / intervalMinutes;

            occurrence =
                    scheduleStart.plusMinutes(
                            intervalsPassed
                                    * intervalMinutes);
        }

        List<LocalTime> times =
                new ArrayList<>();

        while (!occurrence.isAfter(dayEnd)) {

            /*
             * Stop if the occurrence is beyond
             * the schedule's final date.
             */
            if (schedule.getEndDate() != null
                    && occurrence
                            .toLocalDate()
                            .isAfter(
                                    schedule.getEndDate())) {

                break;
            }

            /*
             * Only add occurrences that actually
             * belong to the selected date.
             */
            if (occurrence
                    .toLocalDate()
                    .equals(doseDate)) {

                times.add(
                        occurrence.toLocalTime());
            }

            occurrence =
                    occurrence.plusHours(
                            schedule.getIntervalHours());
        }

        return times;
    }

    /*
     * Save an existing dose record.
     */
    public DoseRecord save(
            DoseRecord doseRecord) {

        if (doseRecord.getMedication() == null
                || doseRecord.getMedication().getId() == null) {

            throw new IllegalArgumentException(
                    "A saved medication must be selected.");
        }

        if (doseRecord.getDoseDate() == null) {

            throw new IllegalArgumentException(
                    "Dose date is required.");
        }

        if (doseRecord.getScheduledTime() == null) {

            throw new IllegalArgumentException(
                    "Scheduled dose time is required.");
        }

        if (doseRecord.getStatus() == null) {

            throw new IllegalArgumentException(
                    "Dose status is required.");
        }

        LocalDateTime scheduledDateTime =
                doseRecord
                        .getDoseDate()
                        .atTime(
                                doseRecord.getScheduledTime());

        if (scheduledDateTime.isAfter(
                LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "A future scheduled dose cannot be recorded yet.");
        }

        return doseRecordRepository.save(
                doseRecord);
    }

    /*
     * Record one exact scheduled occurrence.
     */
    public DoseRecord recordScheduledDose(
            MedicationSchedule schedule,
            LocalDate doseDate,
            LocalTime scheduledTime,
            DoseStatus status) {

        validateSchedule(
                schedule);

        if (doseDate == null) {

            throw new IllegalArgumentException(
                    "Dose date is required.");
        }

        if (scheduledTime == null) {

            throw new IllegalArgumentException(
                    "Scheduled dose time is required.");
        }

        if (status == null) {

            throw new IllegalArgumentException(
                    "Dose status is required.");
        }

        if (doseDate.isBefore(
                schedule.getStartDate())) {

            throw new IllegalArgumentException(
                    "Dose date is before the schedule start date.");
        }

        if (schedule.getEndDate() != null
                && doseDate.isAfter(
                        schedule.getEndDate())) {

            throw new IllegalArgumentException(
                    "Dose date is after the schedule end date.");
        }

        /*
         * Verify that the selected time really
         * belongs to the repeating schedule.
         */
        List<LocalTime> validTimes =
                getScheduledTimesForDate(
                        schedule,
                        doseDate);

        if (!validTimes.contains(
                scheduledTime)) {

            throw new IllegalArgumentException(
                    "The selected time is not a valid dose time for this schedule.");
        }

        LocalDateTime expectedDoseTime =
                doseDate.atTime(
                        scheduledTime);

        /*
         * Do not mark a dose before its
         * scheduled date/time arrives.
         */
        if (expectedDoseTime.isAfter(
                LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "This scheduled dose has not occurred yet.");
        }

        /*
         * Prevent duplicate records for the
         * same schedule/date/time.
         */
        if (doseRecordRepository
                .existsBySchedule_IdAndDoseDateAndScheduledTime(
                        schedule.getId(),
                        doseDate,
                        scheduledTime)) {

            throw new IllegalStateException(
                    "This scheduled dose has already been recorded.");
        }

        DoseRecord doseRecord =
                new DoseRecord();

        doseRecord.setSchedule(
                schedule);

        doseRecord.setMedication(
                schedule.getMedication());

        doseRecord.setDoseDate(
                doseDate);

        doseRecord.setScheduledTime(
                scheduledTime);

        doseRecord.setStatus(
                status);

        /*
         * Only TAKEN doses receive an
         * actual taken timestamp.
         */
        if (status == DoseStatus.TAKEN) {

            doseRecord.setActualTakenAt(
                    LocalDateTime.now());

        } else {

            doseRecord.setActualTakenAt(
                    null);
        }

        return doseRecordRepository.save(
                doseRecord);
    }

    /*
     * Temporary compatibility method.
     *
     * Existing controller code can still compile
     * until we update the dose form to send an
     * exact scheduled time.
     *
     * This uses only the schedule's first time.
     */
    public DoseRecord recordScheduledDose(
            MedicationSchedule schedule,
            LocalDate doseDate,
            DoseStatus status) {

        if (schedule == null) {

            throw new IllegalArgumentException(
                    "Medication schedule is required.");
        }

        return recordScheduledDose(
                schedule,
                doseDate,
                schedule.getScheduledTime(),
                status);
    }

    /*
     * Count one dose status for one patient
     * within a reporting period.
     */
    public long countByStatusAndDateRange(
            Long userId,
            DoseStatus status,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(
                startDate,
                endDate);

        return doseRecordRepository
                .countByMedication_User_IdAndStatusAndDoseDateBetween(
                        userId,
                        status,
                        startDate,
                        endDate);
    }

    /*
     * Get dose history for one medication.
     */
    public List<DoseRecord> getByMedicationAndUser(
            Long medicationId,
            Long userId) {

        return doseRecordRepository
                .findByMedication_IdAndMedication_User_IdOrderByDoseDateDescScheduledTimeAsc(
                        medicationId,
                        userId);
    }

    /*
     * Delete a dose record only if it
     * belongs to the patient.
     */
    public void deleteByIdAndUserId(
            Long id,
            Long userId) {

        DoseRecord doseRecord =
                getByIdAndUserId(
                        id,
                        userId);

        doseRecordRepository.delete(
                doseRecord);
    }

    /*
     * Validate a medication schedule.
     */
    private void validateSchedule(
            MedicationSchedule schedule) {

        if (schedule == null
                || schedule.getId() == null) {

            throw new IllegalArgumentException(
                    "A saved medication schedule is required.");
        }

        if (schedule.getMedication() == null
                || schedule.getMedication().getId() == null) {

            throw new IllegalArgumentException(
                    "The schedule must belong to a saved medication.");
        }

        if (schedule.getStartDate() == null) {

            throw new IllegalStateException(
                    "The medication schedule does not have a start date.");
        }

        if (schedule.getScheduledTime() == null) {

            throw new IllegalStateException(
                    "The medication schedule does not have a start time.");
        }

        if (schedule.getIntervalHours() == null
                || schedule.getIntervalHours() <= 0) {

            throw new IllegalStateException(
                    "The medication schedule does not have a valid interval.");
        }
    }

    /*
     * Validate a reporting period.
     */
    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null
                || endDate == null) {

            throw new IllegalArgumentException(
                    "Start date and end date are required.");
        }

        if (startDate.isAfter(
                endDate)) {

            throw new IllegalArgumentException(
                    "Start date cannot be after end date.");
        }
    }
}