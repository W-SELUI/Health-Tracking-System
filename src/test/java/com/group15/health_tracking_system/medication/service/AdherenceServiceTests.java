package com.group15.health_tracking_system.medication.service;

import com.group15.health_tracking_system.medication.entity.DoseRecord;
import com.group15.health_tracking_system.medication.entity.DoseStatus;
import com.group15.health_tracking_system.medication.entity.MedicationSchedule;
import com.group15.health_tracking_system.medication.repository.DoseRecordRepository;
import com.group15.health_tracking_system.medication.repository.MedicationScheduleRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdherenceServiceTests {

    private DoseRecordRepository doseRecordRepository;
    private MedicationScheduleRepository medicationScheduleRepository;
    private AdherenceService adherenceService;

    @BeforeEach
    void setUp() {

        doseRecordRepository =
                mock(DoseRecordRepository.class);

        medicationScheduleRepository =
                mock(MedicationScheduleRepository.class);

        adherenceService =
                new AdherenceService(
                        doseRecordRepository,
                        medicationScheduleRepository);
    }

    /*
     * A medication starts at 06:00 and repeats
     * every 6 hours on one completed day.
     *
     * Expected occurrences:
     * 06:00
     * 12:00
     * 18:00
     *
     * All three were taken.
     *
     * Adherence = 3 / 3 * 100 = 100%
     */
    @Test
    void allExpectedSixHourlyDosesTakenShouldReturn100Percent() {

        LocalDate day =
                LocalDate.now().minusDays(1);

        MedicationSchedule schedule =
                schedule(
                        1L,
                        day,
                        day,
                        LocalTime.of(6, 0),
                        6);

        List<DoseRecord> records =
                List.of(
                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                day,
                                LocalTime.of(6, 0)),

                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                day,
                                LocalTime.of(12, 0)),

                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                day,
                                LocalTime.of(18, 0))
                );

        when(medicationScheduleRepository
                .findByMedication_User_IdOrderByScheduledTimeAsc(
                        1L))
                .thenReturn(
                        List.of(schedule));

        when(doseRecordRepository
                .findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                        1L,
                        day,
                        day))
                .thenReturn(records);

        assertEquals(
                100.0,
                adherenceService.calculateAdherence(
                        1L,
                        day,
                        day),
                0.01);
    }

    /*
     * Seven daily expected doses:
     *
     * 3 TAKEN
     * 1 MISSED
     * 3 SKIPPED
     *
     * Adherence = 3 / 7 * 100
     *           = 42.857%
     */
    @Test
    void mixedDoseStatusesShouldCalculateCorrectPercentage() {

        LocalDate start =
                LocalDate.now().minusDays(7);

        LocalDate end =
                LocalDate.now().minusDays(1);

        MedicationSchedule schedule =
                schedule(
                        1L,
                        start,
                        end,
                        LocalTime.of(8, 0),
                        24);

        List<DoseRecord> records =
                List.of(
                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                start,
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                start.plusDays(1),
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                start.plusDays(2),
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.MISSED,
                                schedule,
                                start.plusDays(3),
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.SKIPPED,
                                schedule,
                                start.plusDays(4),
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.SKIPPED,
                                schedule,
                                start.plusDays(5),
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.SKIPPED,
                                schedule,
                                end,
                                LocalTime.of(8, 0))
                );

        when(medicationScheduleRepository
                .findByMedication_User_IdOrderByScheduledTimeAsc(
                        1L))
                .thenReturn(
                        List.of(schedule));

        when(doseRecordRepository
                .findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                        1L,
                        start,
                        end))
                .thenReturn(records);

        assertEquals(
                42.857,
                adherenceService.calculateAdherence(
                        1L,
                        start,
                        end),
                0.01);
    }

    /*
     * Five daily doses were expected,
     * but only two were recorded as TAKEN.
     *
     * The remaining three expected doses
     * have no record, so they count as missed.
     *
     * Adherence = 2 / 5 * 100 = 40%
     */
    @Test
    void missingExpectedDoseRecordsShouldReduceAdherence() {

        LocalDate start =
                LocalDate.now().minusDays(5);

        LocalDate end =
                LocalDate.now().minusDays(1);

        MedicationSchedule schedule =
                schedule(
                        1L,
                        start,
                        end,
                        LocalTime.of(8, 0),
                        24);

        List<DoseRecord> records =
                List.of(
                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                start,
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                start.plusDays(1),
                                LocalTime.of(8, 0))
                );

        when(medicationScheduleRepository
                .findByMedication_User_IdOrderByScheduledTimeAsc(
                        1L))
                .thenReturn(
                        List.of(schedule));

        when(doseRecordRepository
                .findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                        1L,
                        start,
                        end))
                .thenReturn(records);

        assertEquals(
                40.0,
                adherenceService.calculateAdherence(
                        1L,
                        start,
                        end),
                0.01);
    }

    /*
     * No medication schedules means
     * no expected doses.
     */
    @Test
    void noSchedulesShouldReturnZeroPercent() {

        when(medicationScheduleRepository
                .findByMedication_User_IdOrderByScheduledTimeAsc(
                        1L))
                .thenReturn(
                        List.of());

        assertEquals(
                0.0,
                adherenceService.calculateAdherence(
                        1L),
                0.01);
    }

    /*
     * Three daily doses are expected
     * within the selected period:
     *
     * 2 TAKEN
     * 1 MISSED
     *
     * Adherence = 2 / 3 * 100
     *           = 66.667%
     */
    @Test
    void dateRangeShouldUseOnlyExpectedDosesWithinPeriod() {

        LocalDate start =
                LocalDate.now().minusDays(3);

        LocalDate end =
                LocalDate.now().minusDays(1);

        MedicationSchedule schedule =
                schedule(
                        1L,
                        start,
                        end,
                        LocalTime.of(8, 0),
                        24);

        List<DoseRecord> records =
                List.of(
                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                start,
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.TAKEN,
                                schedule,
                                start.plusDays(1),
                                LocalTime.of(8, 0)),

                        dose(
                                DoseStatus.MISSED,
                                schedule,
                                end,
                                LocalTime.of(8, 0))
                );

        when(medicationScheduleRepository
                .findByMedication_User_IdOrderByScheduledTimeAsc(
                        1L))
                .thenReturn(
                        List.of(schedule));

        when(doseRecordRepository
                .findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                        1L,
                        start,
                        end))
                .thenReturn(records);

        assertEquals(
                66.667,
                adherenceService.calculateAdherence(
                        1L,
                        start,
                        end),
                0.01);
    }

    /*
     * Three daily doses were expected,
     * but there are no dose records.
     *
     * All three therefore count as missed.
     *
     * Adherence = 0%
     */
    @Test
    void expectedDosesWithNoRecordsShouldReturnZeroPercent() {

        LocalDate start =
                LocalDate.now().minusDays(3);

        LocalDate end =
                LocalDate.now().minusDays(1);

        MedicationSchedule schedule =
                schedule(
                        1L,
                        start,
                        end,
                        LocalTime.of(8, 0),
                        24);

        when(medicationScheduleRepository
                .findByMedication_User_IdOrderByScheduledTimeAsc(
                        1L))
                .thenReturn(
                        List.of(schedule));

        when(doseRecordRepository
                .findByMedication_User_IdAndDoseDateBetweenOrderByDoseDateAscScheduledTimeAsc(
                        1L,
                        start,
                        end))
                .thenReturn(
                        List.of());

        assertEquals(
                0.0,
                adherenceService.calculateAdherence(
                        1L,
                        start,
                        end),
                0.01);
    }

    /*
     * Invalid reporting periods
     * must be rejected.
     */
    @Test
    void startDateAfterEndDateShouldThrowException() {

        LocalDate start =
                LocalDate.now();

        LocalDate end =
                LocalDate.now().minusDays(5);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        adherenceService
                                .calculateAdherence(
                                        1L,
                                        start,
                                        end));
    }

    /*
     * Helper for creating a mocked
     * medication schedule.
     */
    private MedicationSchedule schedule(
            Long id,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime scheduledTime,
            Integer intervalHours) {

        MedicationSchedule schedule =
                mock(MedicationSchedule.class);

        when(schedule.getId())
                .thenReturn(id);

        when(schedule.getStartDate())
                .thenReturn(startDate);

        when(schedule.getEndDate())
                .thenReturn(endDate);

        when(schedule.getScheduledTime())
                .thenReturn(scheduledTime);

        when(schedule.getIntervalHours())
                .thenReturn(intervalHours);

        return schedule;
    }

    /*
     * Helper for creating one mocked
     * scheduled dose record.
     */
    private DoseRecord dose(
            DoseStatus status,
            MedicationSchedule schedule,
            LocalDate doseDate,
            LocalTime scheduledTime) {

        DoseRecord record =
                mock(DoseRecord.class);

        when(record.getStatus())
                .thenReturn(status);

        when(record.getSchedule())
                .thenReturn(schedule);

        when(record.getDoseDate())
                .thenReturn(doseDate);

        when(record.getScheduledTime())
                .thenReturn(scheduledTime);

        return record;
    }
}