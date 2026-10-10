package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdherenceServiceTests {

    private DoseRecordRepository doseRecordRepository;
    private AdherenceService adherenceService;

    @BeforeEach
    void setUp() {
        doseRecordRepository = mock(DoseRecordRepository.class);
        adherenceService = new AdherenceService(doseRecordRepository);
    }

    @Test
    void allDosesTakenShouldReturn100Percent() {
        when(doseRecordRepository.findByMedication_User_Id(1L))
                .thenReturn(List.of(
                        dose(DoseStatus.TAKEN),
                        dose(DoseStatus.TAKEN),
                        dose(DoseStatus.TAKEN)
                ));

        assertEquals(100.0, adherenceService.calculateAdherence(1L), 0.01);
    }

    @Test
    void mixedDoseStatusesShouldCalculateCorrectPercentage() {
        when(doseRecordRepository.findByMedication_User_Id(1L))
                .thenReturn(List.of(
                        dose(DoseStatus.TAKEN),
                        dose(DoseStatus.TAKEN),
                        dose(DoseStatus.TAKEN),
                        dose(DoseStatus.MISSED),
                        dose(DoseStatus.SKIPPED),
                        dose(DoseStatus.SKIPPED),
                        dose(DoseStatus.SKIPPED)
                ));

        assertEquals(42.857, adherenceService.calculateAdherence(1L), 0.01);
    }

    @Test
    void noDoseRecordsShouldReturnZeroPercent() {
        when(doseRecordRepository.findByMedication_User_Id(1L))
                .thenReturn(List.of());

        assertEquals(0.0, adherenceService.calculateAdherence(1L), 0.01);
    }

    @Test
    void dateRangeShouldUseOnlyRecordsWithinPeriod() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 7);

        when(doseRecordRepository
                .findByMedication_User_IdAndDoseDateBetween(1L, start, end))
                .thenReturn(List.of(
                        dose(DoseStatus.TAKEN),
                        dose(DoseStatus.TAKEN),
                        dose(DoseStatus.MISSED)
                ));

        assertEquals(
                66.667,
                adherenceService.calculateAdherence(1L, start, end),
                0.01
        );
    }

    @Test
    void startDateAfterEndDateShouldThrowException() {
        LocalDate start = LocalDate.of(2026, 10, 10);
        LocalDate end = LocalDate.of(2026, 10, 1);

        assertThrows(
                IllegalArgumentException.class,
                () -> adherenceService.calculateAdherence(1L, start, end)
        );
    }

    private DoseRecord dose(DoseStatus status) {
        DoseRecord record = new DoseRecord();
        record.setStatus(status);
        return record;
    }
}
