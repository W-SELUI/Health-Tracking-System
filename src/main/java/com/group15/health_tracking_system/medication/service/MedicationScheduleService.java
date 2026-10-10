package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.repository.MedicationScheduleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicationScheduleService {

    private final MedicationScheduleRepository repository;

    public MedicationScheduleService(
            MedicationScheduleRepository repository) {
        this.repository = repository;
    }

    public MedicationSchedule save(MedicationSchedule schedule) {
        return repository.save(schedule);
    }

    // Get schedules belonging to one patient's medications
    public List<MedicationSchedule> getByUserId(Long userId) {
        return repository.findByMedication_User_Id(userId);
    }

    // Get a schedule by ID (existing functionality)
    public Optional<MedicationSchedule> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
