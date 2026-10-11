package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DoseRecordRepository
        extends JpaRepository<DoseRecord, Long> {

    /*
     * Find all dose records belonging
     * to one patient.
     */
    List<DoseRecord>
            findByMedication_User_IdOrderByDoseDateDescScheduledTimeAsc(
                    Long userId);

    /*
     * Find one dose record only if it belongs
     * to the logged-in patient.
     */
    Optional<DoseRecord>
            findByIdAndMedication_User_Id(
                    Long id,
                    Long userId);

    /*
     * Find patient dose records within
     * a selected date range.
     */
    List<DoseRecord>
            findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                    Long userId,
                    LocalDate startDate,
                    LocalDate endDate);

    /*
     * Find one exact scheduled dose occurrence.
     *
     * Example:
     * Schedule 5
     * 2026-10-11
     * 12:00
     */
    Optional<DoseRecord>
            findBySchedule_IdAndDoseDateAndScheduledTime(
                    Long scheduleId,
                    LocalDate doseDate,
                    LocalTime scheduledTime);

    /*
     * Check whether one exact scheduled
     * dose occurrence already has a record.
     */
    boolean existsBySchedule_IdAndDoseDateAndScheduledTime(
            Long scheduleId,
            LocalDate doseDate,
            LocalTime scheduledTime);

    /*
     * Get all recorded dose occurrences
     * for one schedule on one date.
     */
    List<DoseRecord>
            findBySchedule_IdAndDoseDateOrderByScheduledTimeAsc(
                    Long scheduleId,
                    LocalDate doseDate);

    /*
     * Count one status for one patient
     * within a selected reporting period.
     */
    long countByMedication_User_IdAndStatusAndDoseDateBetween(
            Long userId,
            DoseStatus status,
            LocalDate startDate,
            LocalDate endDate);

    /*
     * Find all records belonging to one
     * medication and one patient.
     */
    List<DoseRecord>
            findByMedication_IdAndMedication_User_IdOrderByDoseDateDescScheduledTimeAsc(
                    Long medicationId,
                    Long userId);
}