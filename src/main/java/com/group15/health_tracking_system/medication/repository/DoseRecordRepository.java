package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoseRecordRepository
        extends JpaRepository<DoseRecord, Long> {

    long countByStatus(DoseStatus status);

    // Find dose records belonging to a particular user
    List<DoseRecord> findByMedication_User_Id(Long userId);

    // Find one dose record belonging to a particular user
    Optional<DoseRecord> findByIdAndMedication_User_Id(
            Long id, Long userId);

    // Find a user's dose records within a selected date range
    List<DoseRecord> findByMedication_User_IdAndDoseDateBetween(
            Long userId,
            LocalDate startDate,
            LocalDate endDate);
}
