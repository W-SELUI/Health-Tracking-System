package com.group15.health_tracking_system.health.dto;

import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import java.time.LocalDateTime;

/**
 * Read-only presentation model used by the Health Records history page.
 *
 * This class does not represent a database table. It allows scalar health
 * records and blood-pressure records to appear together in one unified list.
 */
public class HealthRecordListItem {

    public enum RecordType {
        SCALAR,
        BLOOD_PRESSURE
    }

    private final Long id;
    private final RecordType recordType;
    private final String metricName;
    private final String displayValue;
    private final LocalDateTime observedAt;
    private final HealthRecordSource source;
    private final boolean editable;

    public HealthRecordListItem(
            Long id,
            RecordType recordType,
            String metricName,
            String displayValue,
            LocalDateTime observedAt,
            HealthRecordSource source,
            boolean editable) {

        this.id = id;
        this.recordType = recordType;
        this.metricName = metricName;
        this.displayValue = displayValue;
        this.observedAt = observedAt;
        this.source = source;
        this.editable = editable;
    }

    public Long getId() {
        return id;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public LocalDateTime getObservedAt() {
        return observedAt;
    }

    public HealthRecordSource getSource() {
        return source;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isBloodPressure() {
        return recordType == RecordType.BLOOD_PRESSURE;
    }
}