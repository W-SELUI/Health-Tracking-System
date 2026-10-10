
package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DoseRecordService {

    private final DoseRecordRepository doseRecordRepository;

    public DoseRecordService(DoseRecordRepository doseRecordRepository) {
        this.doseRecordRepository = doseRecordRepository;
    }

    // Get all dose records
    public List<DoseRecord> getAll() {
        return doseRecordRepository.findAll();
    }

    // Get a dose record by ID
    public DoseRecord getById(Long id) {
        return doseRecordRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Dose record not found: " + id));
    }

    // Save a new or updated dose record
    public DoseRecord save(DoseRecord doseRecord) {

        if (doseRecord.getMedication() == null
                || doseRecord.getMedication().getId() == null) {
            throw new IllegalArgumentException(
                    "A saved medication must be selected.");
        }

        return doseRecordRepository.save(doseRecord);
    }

    // Delete a dose record by ID
    public void deleteById(Long id) {

        if (!doseRecordRepository.existsById(id)) {
            throw new IllegalArgumentException(
                    "Dose record not found: " + id);
        }

        doseRecordRepository.deleteById(id);
    }
}
