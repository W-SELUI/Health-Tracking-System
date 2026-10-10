package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AdherenceService {

    private final DoseRecordRepository doseRecordRepository;

    public AdherenceService(
            DoseRecordRepository doseRecordRepository) {
        this.doseRecordRepository = doseRecordRepository;
    }

    // Calculate adherence for one user using all dose records
    public double calculateAdherence(Long userId) {
        List<DoseRecord> records =
                doseRecordRepository.findByMedication_User_Id(userId);

        return calculatePercentage(records);
    }

    // Calculate adherence for one user within a selected period
    public double calculateAdherence(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(startDate, endDate);

        List<DoseRecord> records =
                doseRecordRepository
                        .findByMedication_User_IdAndDoseDateBetween(
                                userId, startDate, endDate);

        return calculatePercentage(records);
    }

    // Get dose counts for one user using all dose records
    public Map<String, Long> getDoseCounts(Long userId) {
        List<DoseRecord> records =
                doseRecordRepository.findByMedication_User_Id(userId);

        return countStatuses(records);
    }

    // Get dose counts for one user within a selected period
    public Map<String, Long> getDoseCounts(
            Long userId,
            LocalDate startDate,
            LocalDate endDate) {

        validateDateRange(startDate, endDate);

        List<DoseRecord> records =
                doseRecordRepository
                        .findByMedication_User_IdAndDoseDateBetween(
                                userId, startDate, endDate);

        return countStatuses(records);
    }

    private double calculatePercentage(List<DoseRecord> records) {
        Map<String, Long> counts = countStatuses(records);

        long taken = counts.get("taken");
        long missed = counts.get("missed");
        long skipped = counts.get("skipped");

        long total = taken + missed + skipped;

        if (total == 0) {
            return 0.0;
        }

        return ((double) taken / total) * 100.0;
    }

    private Map<String, Long> countStatuses(
            List<DoseRecord> records) {

        long taken = records.stream()
                .filter(record -> record.getStatus() == DoseStatus.TAKEN)
                .count();

        long missed = records.stream()
                .filter(record -> record.getStatus() == DoseStatus.MISSED)
                .count();

        long skipped = records.stream()
                .filter(record -> record.getStatus() == DoseStatus.SKIPPED)
                .count();

        Map<String, Long> counts = new HashMap<>();
        counts.put("taken", taken);
        counts.put("missed", missed);
        counts.put("skipped", skipped);

        return counts;
    }

    private void validateDateRange(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Start date and end date are required.");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date must not be after end date.");
        }
    }
}
