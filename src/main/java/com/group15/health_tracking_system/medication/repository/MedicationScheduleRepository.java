package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {
}