package com.group15.health_tracking_system.health.repository;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for patient health records.
 *
 * Queries include the owning patient wherever possible so records belonging
 * to one patient cannot accidentally be retrieved as another patient's data.
 */
@Repository
public interface HealthRecordRepository
        extends JpaRepository<HealthRecord, Long> {

    /**
     * Returns all records belonging to a patient, newest first.
     */
    List<HealthRecord> findByPatientOrderByObservedAtDesc(User patient);

    /**
     * Finds one record only when it belongs to the specified patient.
     */
    Optional<HealthRecord> findByIdAndPatient(Long id, User patient);

    /**
     * Returns a patient's records for one metric, newest first.
     */
    List<HealthRecord> findByPatientAndMetricTypeOrderByObservedAtDesc(
            User patient,
            HealthMetricType metricType
    );

    /**
     * Returns a patient's records observed within the supplied period,
     * newest first.
     */
    List<HealthRecord>
            findByPatientAndObservedAtBetweenOrderByObservedAtDesc(
                    User patient,
                    LocalDateTime start,
                    LocalDateTime end
            );

    /**
     * Returns a patient's records for one metric within the supplied period,
     * newest first.
     */
    List<HealthRecord>
            findByPatientAndMetricTypeAndObservedAtBetweenOrderByObservedAtDesc(
                    User patient,
                    HealthMetricType metricType,
                    LocalDateTime start,
                    LocalDateTime end
            );
}