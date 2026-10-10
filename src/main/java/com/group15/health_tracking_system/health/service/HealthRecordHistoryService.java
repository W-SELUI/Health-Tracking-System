package com.group15.health_tracking_system.health.service;

import com.group15.health_tracking_system.health.dto.HealthRecordFilterOption;
import com.group15.health_tracking_system.health.dto.HealthRecordListItem;
import com.group15.health_tracking_system.health.dto.HealthRecordListItem.RecordType;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds the unified patient Health Records history.
 *
 * Scalar HealthRecord entries and BloodPressureRecord entries remain
 * separate persistence models, but this service combines them into a
 * single read-only list for the patient interface.
 */
@Service
public class HealthRecordHistoryService {

    private final HealthRecordQueryService healthRecordQueryService;
    private final BloodPressureQueryService bloodPressureQueryService;

    public HealthRecordHistoryService(
            HealthRecordQueryService healthRecordQueryService,
            BloodPressureQueryService bloodPressureQueryService) {

        this.healthRecordQueryService = healthRecordQueryService;
        this.bloodPressureQueryService = bloodPressureQueryService;
    }

    /**
     * Returns unified patient history.
     *
     * filterOption may be null to represent "All Metrics".
     * start and end must either both be null or both be supplied.
     */
    @Transactional(readOnly = true)
    public List<HealthRecordListItem> getPatientHistory(
            User currentUser,
            HealthRecordFilterOption filterOption,
            LocalDateTime start,
            LocalDateTime end) {

        validatePeriodArguments(start, end);

        boolean hasPeriod = start != null;

        /*
         * No specific metric selected:
         * combine scalar records and blood-pressure records.
         */
        if (filterOption == null) {

            if (hasPeriod) {
                return combineAndSort(
                        healthRecordQueryService
                                .getPatientHistoryByPeriod(
                                        currentUser,
                                        start,
                                        end
                                ),
                        bloodPressureQueryService
                                .getPatientHistoryByPeriod(
                                        currentUser,
                                        start,
                                        end
                                )
                );
            }

            return combineAndSort(
                    healthRecordQueryService
                            .getPatientHistory(currentUser),
                    bloodPressureQueryService
                            .getPatientHistory(currentUser)
            );
        }

        /*
         * Blood pressure is stored separately because it is a
         * paired measurement.
         */
        if (filterOption.isBloodPressure()) {

            List<BloodPressureRecord> records;

            if (hasPeriod) {
                records = bloodPressureQueryService
                        .getPatientHistoryByPeriod(
                                currentUser,
                                start,
                                end
                        );
            } else {
                records = bloodPressureQueryService
                        .getPatientHistory(currentUser);
            }

            return toBloodPressureItems(records);
        }

        /*
         * All remaining filter options represent scalar metrics.
         */
        List<HealthRecord> records;

        if (hasPeriod) {
            records = healthRecordQueryService
                    .getPatientHistoryByMetricAndPeriod(
                            currentUser,
                            filterOption.getScalarMetricType(),
                            start,
                            end
                    );
        } else {
            records = healthRecordQueryService
                    .getPatientHistoryByMetric(
                            currentUser,
                            filterOption.getScalarMetricType()
                    );
        }

        return toScalarItems(records);
    }

    private List<HealthRecordListItem> combineAndSort(
            List<HealthRecord> scalarRecords,
            List<BloodPressureRecord> bloodPressureRecords) {

        List<HealthRecordListItem> items =
                new ArrayList<>();

        items.addAll(
                toScalarItems(scalarRecords)
        );

        items.addAll(
                toBloodPressureItems(
                        bloodPressureRecords
                )
        );

        items.sort(
                Comparator.comparing(
                        HealthRecordListItem::getObservedAt
                ).reversed()
        );

        return items;
    }

    private List<HealthRecordListItem> toScalarItems(
            List<HealthRecord> records) {

        return records.stream()
                .map(this::toScalarItem)
                .toList();
    }

    private HealthRecordListItem toScalarItem(
            HealthRecord record) {

        return new HealthRecordListItem(
                record.getId(),
                RecordType.SCALAR,
                record.getMetricType().getDisplayName(),
                formatNumber(record.getMetricValue())
                        + " "
                        + record.getUnit(),
                record.getObservedAt(),
                record.getSource(),
                isPatientEditable(record.getSource())
        );
    }

    private List<HealthRecordListItem> toBloodPressureItems(
            List<BloodPressureRecord> records) {

        return records.stream()
                .map(this::toBloodPressureItem)
                .toList();
    }

    private HealthRecordListItem toBloodPressureItem(
            BloodPressureRecord record) {

        String displayValue =
                formatNumber(record.getSystolic())
                + " / "
                + formatNumber(record.getDiastolic())
                + " "
                + record.getUnit();

        return new HealthRecordListItem(
                record.getId(),
                RecordType.BLOOD_PRESSURE,
                "Blood Pressure",
                displayValue,
                record.getObservedAt(),
                record.getSource(),
                isPatientEditable(record.getSource())
        );
    }

    private boolean isPatientEditable(
            HealthRecordSource source) {

        return source
                == HealthRecordSource.PATIENT_REPORTED;
    }

    private String formatNumber(
            BigDecimal value) {

        return value
                .stripTrailingZeros()
                .toPlainString();
    }

    private void validatePeriodArguments(
            LocalDateTime start,
            LocalDateTime end) {

        if ((start == null) != (end == null)) {
            throw new IllegalArgumentException(
                    "Both start date and end date are required"
            );
        }

        if (start != null && start.isAfter(end)) {
            throw new IllegalArgumentException(
                    "Start date/time must not be after end date/time"
            );
        }
    }
}