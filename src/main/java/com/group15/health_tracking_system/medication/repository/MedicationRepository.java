package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicationRepository extends JpaRepository<Medication, Long> {
}