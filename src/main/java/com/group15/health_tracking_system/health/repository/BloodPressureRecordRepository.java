package com.group15.health_tracking_system.health.repository;

import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for blood-pressure records.
 *
 * Blood pressure is stored separately from scalar health measurements
 * because one observation requires both systolic and diastolic values.
 */
@Repository
public interface BloodPressureRecordRepository
        extends JpaRepository<BloodPressureRecord, Long> {

   
    List<BloodPressureRecord>
            findByPatientOrderByObservedAtDesc(User patient);

    
    Optional<BloodPressureRecord>
            findByIdAndPatient(Long id, User patient);

    /**
     * Returns a patient's blood-pressure observations within a
     * reporting period, newest first.
     */
    List<BloodPressureRecord>
            findByPatientAndObservedAtBetweenOrderByObservedAtDesc(
                    User patient,
                    LocalDateTime start,
                    LocalDateTime end
            );
}