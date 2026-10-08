package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoseRecordService {

    private final DoseRecordRepository doseRecordRepository;

    public DoseRecordService(
            DoseRecordRepository doseRecordRepository) {

        this.doseRecordRepository = doseRecordRepository;
    }

    public List<DoseRecord> getAll() {
        return doseRecordRepository.findAll();
    }

    public DoseRecord save(DoseRecord doseRecord) {
        return doseRecordRepository.save(doseRecord);
    }
}