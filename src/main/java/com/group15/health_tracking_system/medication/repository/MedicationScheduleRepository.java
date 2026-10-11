package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.MedicationSchedule;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationScheduleRepository
        extends JpaRepository<MedicationSchedule, Long> {

    /*
     * Find all schedules belonging to the
     * logged-in patient's medications.
     */
    List<MedicationSchedule>
            findByMedication_User_IdOrderByScheduledTimeAsc(
                    Long userId);

    /*
     * Find a specific schedule only if it belongs
     * to the logged-in patient.
     */
    Optional<MedicationSchedule>
            findByIdAndMedication_User_Id(
                    Long id,
                    Long userId);

    /*
     * Find schedules belonging to one medication
     * owned by the logged-in patient.
     */
    List<MedicationSchedule>
            findByMedication_IdAndMedication_User_IdOrderByScheduledTimeAsc(
                    Long medicationId,
                    Long userId);

    /*
     * Find schedules that may be active
     * on or before a selected end date.
     *
     * Final date filtering can be completed
     * in the service layer.
     */
    List<MedicationSchedule>
            findByMedication_User_IdAndStartDateLessThanEqual(
                    Long userId,
                    LocalDate endDate);
}