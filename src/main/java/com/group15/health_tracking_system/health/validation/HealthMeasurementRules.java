package com.group15.health_tracking_system.health.validation;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import java.math.BigDecimal;
import org.springframework.validation.BindingResult;

/**
 * Sensible ranges for health measurements.
 *
 * These are not medical limits. They only stop values that cannot be a real
 * reading (for example a negative heart rate or a body temperature of 70 °C)
 * from being saved and then skewing analytics. The ranges are deliberately
 * wide so unusual but genuine readings are still accepted.
 */
public final class HealthMeasurementRules {

    private static final BigDecimal SYSTOLIC_MIN = new BigDecimal("50");
    private static final BigDecimal SYSTOLIC_MAX = new BigDecimal("260");
    private static final BigDecimal DIASTOLIC_MIN = new BigDecimal("30");
    private static final BigDecimal DIASTOLIC_MAX = new BigDecimal("160");

    private HealthMeasurementRules() {
    }

    /** Adds a field error when a scalar measurement is outside its range. */
    public static void checkScalarValue(
            HealthMetricType metricType,
            BigDecimal value,
            BindingResult bindingResult,
            String field) {

        if (metricType == null || value == null) {
            return;
        }

        BigDecimal min = minimum(metricType);
        BigDecimal max = maximum(metricType);

        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            bindingResult.rejectValue(
                    field,
                    "range." + field,
                    metricType.getDisplayName() + " must be between "
                            + min.stripTrailingZeros().toPlainString() + " and "
                            + max.stripTrailingZeros().toPlainString() + " "
                            + metricType.getUnit()
            );
        }
    }

    /** Adds field errors when a blood pressure reading is not realistic. */
    public static void checkBloodPressure(
            BigDecimal systolic,
            BigDecimal diastolic,
            BindingResult bindingResult) {

        if (systolic != null
                && (systolic.compareTo(SYSTOLIC_MIN) < 0
                || systolic.compareTo(SYSTOLIC_MAX) > 0)) {

            bindingResult.rejectValue(
                    "systolic",
                    "range.systolic",
                    "Systolic must be between 50 and 260 mmHg"
            );
        }

        if (diastolic != null
                && (diastolic.compareTo(DIASTOLIC_MIN) < 0
                || diastolic.compareTo(DIASTOLIC_MAX) > 0)) {

            bindingResult.rejectValue(
                    "diastolic",
                    "range.diastolic",
                    "Diastolic must be between 30 and 160 mmHg"
            );
        }

        if (systolic != null
                && diastolic != null
                && systolic.compareTo(diastolic) <= 0
                && !bindingResult.hasFieldErrors("diastolic")) {

            bindingResult.rejectValue(
                    "diastolic",
                    "order.diastolic",
                    "Diastolic must be lower than systolic"
            );
        }
    }

    private static BigDecimal minimum(HealthMetricType metricType) {
        return switch (metricType) {
            case WEIGHT -> new BigDecimal("1");
            case HEART_RATE -> new BigDecimal("20");
            case BODY_TEMPERATURE -> new BigDecimal("30");
            case OXYGEN_SATURATION -> new BigDecimal("50");
            case BLOOD_GLUCOSE -> new BigDecimal("1");
        };
    }

    private static BigDecimal maximum(HealthMetricType metricType) {
        return switch (metricType) {
            case WEIGHT -> new BigDecimal("500");
            case HEART_RATE -> new BigDecimal("250");
            case BODY_TEMPERATURE -> new BigDecimal("45");
            case OXYGEN_SATURATION -> new BigDecimal("100");
            case BLOOD_GLUCOSE -> new BigDecimal("40");
        };
    }
}
