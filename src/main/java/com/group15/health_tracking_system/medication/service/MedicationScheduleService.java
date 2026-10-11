package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.repository.MedicationScheduleRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class MedicationScheduleService {

    private final MedicationScheduleRepository repository;

    public MedicationScheduleService(
            MedicationScheduleRepository repository) {

        this.repository = repository;
    }

    public MedicationSchedule save(
            MedicationSchedule schedule) {

        return repository.save(schedule);
    }

    /*
     * Get all schedules belonging to
     * one patient's medications.
     */
    public List<MedicationSchedule> getByUserId(
            Long userId) {

        return repository
                .findByMedication_User_IdOrderByScheduledTimeAsc(
                        userId);
    }

    /*
     * Get a schedule by ID.
     *
     * Kept for compatibility with existing
     * controller code.
     */
    public Optional<MedicationSchedule> getById(
            Long id) {

        return repository.findById(id);
    }

    /*
     * Get one schedule only when it belongs
     * to the selected patient.
     */
    public Optional<MedicationSchedule> getByIdAndUserId(
            Long id,
            Long userId) {

        return repository
                .findByIdAndMedication_User_Id(
                        id,
                        userId);
    }

    /*
     * Get schedules belonging to one medication
     * owned by the selected patient.
     */
    public List<MedicationSchedule> getByMedicationAndUser(
            Long medicationId,
            Long userId) {

        return repository
                .findByMedication_IdAndMedication_User_IdOrderByScheduledTimeAsc(
                        medicationId,
                        userId);
    }

    /*
     * Delete a schedule.
     *
     * Existing method kept temporarily so
     * current controllers do not break.
     */
    public void delete(
            Long id) {

        repository.deleteById(id);
    }

    /*
     * Safer patient-owned delete method.
     */
    public void delete(
            Long id,
            Long userId) {

        MedicationSchedule schedule =
                repository
                        .findByIdAndMedication_User_Id(
                                id,
                                userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Medication schedule not found for this patient."));

        repository.delete(schedule);
    }
}