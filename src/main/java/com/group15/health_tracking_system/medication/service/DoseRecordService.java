package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DoseRecordService {

    private final DoseRecordRepository repository;

    public DoseRecordService(DoseRecordRepository repository) {
        this.repository = repository;
    }

    public DoseRecord save(DoseRecord doseRecord) {
        return repository.save(doseRecord);
    }

    public List<DoseRecord> getAll() {
        return repository.findAll();
    }

    public Optional<DoseRecord> getById(Long id) {
        return repository.findById(id);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}