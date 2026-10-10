package com.group15.health_tracking_system.activity;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ActivityLogRepository extends JpaRepository<ActivityLog,Long> {
 Optional<ActivityLog> findByIdAndPatient_Id(Long id,Long patientId);
 List<ActivityLog> findByPatient_IdAndActivityDateBetweenOrderByActivityDateDescIdDesc(Long patientId,LocalDate start,LocalDate end);
}
