
package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicationRepository
        extends JpaRepository<Medication, Long> {

    List<Medication> findByUserId(Long userId);

    Optional<Medication> findByIdAndUserId(
            Long id, Long userId);
}
