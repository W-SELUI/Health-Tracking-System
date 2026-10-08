package com.group15.health_tracking_system.medication.repository;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoseRecordRepository extends JpaRepository<DoseRecord, Long> {

    long countByStatus(DoseStatus status);

}