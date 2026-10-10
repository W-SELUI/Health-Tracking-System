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

    // Get dose records belonging to a particular user
    public List<DoseRecord> getByUserId(Long userId) {
        return doseRecordRepository
                .findByMedication_User_Id(userId);
    }

    // Get a dose record only if it belongs to the user
    public DoseRecord getByIdAndUserId(
            Long id, Long userId) {
        return doseRecordRepository
                .findByIdAndMedication_User_Id(id, userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Dose record not found: " + id));
    }

    // Save a new dose record after validating its medication
    public DoseRecord save(DoseRecord doseRecord) {
        if (doseRecord.getMedication() == null
                || doseRecord.getMedication().getId() == null) {
            throw new IllegalArgumentException(
                    "A saved medication must be selected.");
        }

        return doseRecordRepository.save(doseRecord);
    }

    // Delete a dose record only if it belongs to the user
    public void deleteByIdAndUserId(Long id, Long userId) {
        DoseRecord doseRecord = getByIdAndUserId(id, userId);
        doseRecordRepository.delete(doseRecord);
    }
}
