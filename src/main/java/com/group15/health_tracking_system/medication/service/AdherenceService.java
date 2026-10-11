package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;
import com.group15.health_tracking_system.medication.repository.MedicationScheduleRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AdherenceService {

    private final DoseRecordRepository doseRecordRepository;
    private final MedicationScheduleRepository medicationScheduleRepository;

    public AdherenceService(
            DoseRecordRepository doseRecordRepository,
            MedicationScheduleRepository medicationScheduleRepository) {

        this.doseRecordRepository =
                doseRecordRepository;

        this.medicationScheduleRepository =
                medicationScheduleRepository;
    }

    /*
     * Calculate adherence across all available
     * medication schedules belonging to a patient.
     */
    public double calculateAdherence(
            Long userId) {

        List<MedicationSchedule> schedules =
                medicationScheduleRepository
                        .findByMedication_User_IdOrderByScheduledTimeAsc(
                                userId);

        LocalDate earliestStart =
                findEarliestStartDate(
                        schedules);

        /*
         * No valid schedules exist.
         */
        if (earliestStart == null) {
            return 0.0;
        }

        /*
         * All schedules begin in the future.
         * No doses are expected yet.
         */
        if (earliestStart.isAfter(
                LocalDate.now())) {

            return 0.0;
        }

        return calculateAdherence(
                userId,
                earliestStart,
                LocalDate.now());
    }

    /*
     * Calculate adherence for a selected period.
     *
     * Formula:
     *
     * Taken /
     * (Taken + Missed + Skipped)
     * x 100
     *
     * Expected doses which should already
     * have occurred but have no record are
     * treated as missed.
     */
    public double calculateAdherence(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(
                startDate,
                endDate);

        Map<String, Long> counts =
                getDoseCounts(
                        userId,
                        startDate,
                        endDate);

        long taken =
                counts.get("taken");

        long missed =
                counts.get("missed");

        long skipped =
                counts.get("skipped");

        long total =
                taken
                        + missed
                        + skipped;

        if (total == 0) {
            return 0.0;
        }

        return ((double) taken / total)
                * 100.0;
    }

    /*
     * Get adherence counts across
     * all available schedules.
     */
    public Map<String, Long> getDoseCounts(
            Long userId) {

        List<MedicationSchedule> schedules =
                medicationScheduleRepository
                        .findByMedication_User_IdOrderByScheduledTimeAsc(
                                userId);

        LocalDate earliestStart =
                findEarliestStartDate(
                        schedules);

        /*
         * No valid medication schedules.
         */
        if (earliestStart == null) {

            return emptyCounts();
        }

        /*
         * All medication schedules begin
         * in the future.
         */
        if (earliestStart.isAfter(
                LocalDate.now())) {

            return emptyCounts();
        }

        return getDoseCounts(
                userId,
                earliestStart,
                LocalDate.now());
    }

    /*
     * Get adherence counts for
     * one selected reporting period.
     */
    public Map<String, Long> getDoseCounts(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(
                startDate,
                endDate);

        /*
         * Never calculate adherence beyond today.
         */
        LocalDate effectiveEndDate =
                endDate.isAfter(LocalDate.now())
                        ? LocalDate.now()
                        : endDate;

        /*
         * Selected period is completely
         * in the future.
         */
        if (startDate.isAfter(
                effectiveEndDate)) {

            return emptyCounts();
        }

        List<MedicationSchedule> schedules =
                medicationScheduleRepository
                        .findByMedication_User_IdOrderByScheduledTimeAsc(
                                userId);

        List<DoseRecord> records =
                doseRecordRepository
                        .findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                                userId,
                                startDate,
                                effectiveEndDate);

        /*
         * Only records linked to a schedule
         * belong to the schedule-based
         * adherence calculation.
         */
        List<DoseRecord> scheduledRecords =
                records.stream()
                        .filter(record ->
                                record.getSchedule() != null)
                        .toList();

        long taken =
                scheduledRecords.stream()
                        .filter(record ->
                                record.getStatus()
                                        == DoseStatus.TAKEN)
                        .count();

        long explicitlyMissed =
                scheduledRecords.stream()
                        .filter(record ->
                                record.getStatus()
                                        == DoseStatus.MISSED)
                        .count();

        long skipped =
                scheduledRecords.stream()
                        .filter(record ->
                                record.getStatus()
                                        == DoseStatus.SKIPPED)
                        .count();

        long recordedTotal =
                taken
                        + explicitlyMissed
                        + skipped;

        /*
         * Count every scheduled occurrence,
         * not merely one occurrence per day.
         */
        long expectedDoses =
                countExpectedDoses(
                        schedules,
                        startDate,
                        effectiveEndDate);

        /*
         * If an expected dose has already
         * occurred but has no record,
         * count it as missed.
         */
        long unrecordedExpected =
                Math.max(
                        0,
                        expectedDoses
                                - recordedTotal);

        long totalMissed =
                explicitlyMissed
                        + unrecordedExpected;

        Map<String, Long> counts =
                new HashMap<>();

        counts.put(
                "taken",
                taken);

        counts.put(
                "missed",
                totalMissed);

        counts.put(
                "skipped",
                skipped);

        counts.put(
                "expected",
                expectedDoses);

        counts.put(
                "unrecorded",
                unrecordedExpected);

        return counts;
    }

    public long getTakenCount(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        return getDoseCounts(
                userId,
                startDate,
                endDate)
                .get("taken");
    }

    public long getMissedCount(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        return getDoseCounts(
                userId,
                startDate,
                endDate)
                .get("missed");
    }

    public long getSkippedCount(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        return getDoseCounts(
                userId,
                startDate,
                endDate)
                .get("skipped");
    }

    /*
     * Count how many dose occurrences
     * should already have happened.
     */
    private long countExpectedDoses(
            List<MedicationSchedule> schedules,
            LocalDate requestedStart,
            LocalDate requestedEnd) {

        long expected =
                0;

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime reportStart =
                requestedStart
                        .atStartOfDay();

        LocalDateTime reportEnd;

        if (requestedEnd.equals(
                LocalDate.now())) {

            reportEnd =
                    now;

        } else {

            reportEnd =
                    requestedEnd.atTime(
                            LocalTime.MAX);
        }

        for (MedicationSchedule schedule : schedules) {

            /*
             * Ignore incomplete schedules.
             */
            if (schedule.getStartDate() == null
                    || schedule.getScheduledTime() == null
                    || schedule.getIntervalHours() == null
                    || schedule.getIntervalHours() <= 0) {

                continue;
            }

            LocalDateTime scheduleStart =
                    schedule
                            .getStartDate()
                            .atTime(
                                    schedule.getScheduledTime());

            LocalDateTime scheduleEnd =
                    schedule.getEndDate() == null
                            ? reportEnd
                            : schedule
                                    .getEndDate()
                                    .atTime(
                                            LocalTime.MAX);

            LocalDateTime effectiveStart =
                    scheduleStart.isAfter(
                            reportStart)
                            ? scheduleStart
                            : reportStart;

            LocalDateTime effectiveEnd =
                    scheduleEnd.isBefore(
                            reportEnd)
                            ? scheduleEnd
                            : reportEnd;

            /*
             * Schedule does not overlap
             * the selected reporting period.
             */
            if (effectiveStart.isAfter(
                    effectiveEnd)) {

                continue;
            }

            LocalDateTime occurrence =
                    scheduleStart;

            /*
             * Skip occurrences before the
             * reporting period.
             */
            while (occurrence.isBefore(
                    effectiveStart)) {

                occurrence =
                        occurrence.plusHours(
                                schedule.getIntervalHours());
            }

            /*
             * Count every occurrence in the
             * reporting period that should
             * already have happened.
             */
            while (!occurrence.isAfter(
                    effectiveEnd)) {

                expected++;

                occurrence =
                        occurrence.plusHours(
                                schedule.getIntervalHours());
            }
        }

        return expected;
    }

    /*
     * Find the earliest usable schedule date.
     */
    private LocalDate findEarliestStartDate(
            List<MedicationSchedule> schedules) {

        return schedules.stream()
                .filter(schedule ->
                        schedule.getStartDate() != null)
                .filter(schedule ->
                        schedule.getScheduledTime() != null)
                .filter(schedule ->
                        schedule.getIntervalHours() != null
                                && schedule.getIntervalHours() > 0)
                .map(
                        MedicationSchedule::getStartDate)
                .min(
                        LocalDate::compareTo)
                .orElse(null);
    }

    /*
     * Return zero values when no
     * adherence data exists.
     */
    private Map<String, Long> emptyCounts() {

        Map<String, Long> counts =
                new HashMap<>();

        counts.put(
                "taken",
                0L);

        counts.put(
                "missed",
                0L);

        counts.put(
                "skipped",
                0L);

        counts.put(
                "expected",
                0L);

        counts.put(
                "unrecorded",
                0L);

        return counts;
    }

    /*
     * Validate reporting period.
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
                    "Start date must not be after end date.");
        }
    }
}