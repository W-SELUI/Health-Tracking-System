package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationScheduleRepository
        extends JpaRepository<MedicationSchedule, Long> {

    // Find schedules belonging to a particular patient's medications
    List<MedicationSchedule> findByMedication_User_Id(Long userId);
}
