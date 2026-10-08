package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.Medication;
import com.group15.health_tracking_system.medication.repository.MedicationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MedicationService {

    private final MedicationRepository medicationRepository;

    public MedicationService(MedicationRepository medicationRepository) {
        this.medicationRepository = medicationRepository;
    }

    public Medication saveMedication(Medication medication) {
        return medicationRepository.save(medication);
    }

    public List<Medication> getAllMedications() {
        return medicationRepository.findAll();
    }

    public Optional<Medication> getMedicationById(Long id) {
        return medicationRepository.findById(id);
    }

    public void deleteMedication(Long id) {
        medicationRepository.deleteById(id);
    }
}