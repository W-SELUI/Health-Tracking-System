package com.group15.health_tracking_system.health.repository;

import com.group15.health_tracking_system.health.entity.HealthRecordChangeRecordType;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequest;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Persistence operations for Provider-proposed Health Record changes.
 *
 * Change requests are deliberately stored separately from permanent
 * HealthRecord and BloodPressureRecord data. A PENDING proposal therefore
 * has no effect on the patient's permanent Health Records until the
 * Patient approves it.
 */
@Repository
public interface HealthRecordChangeRequestRepository
        extends JpaRepository<HealthRecordChangeRequest, Long> {

    /**
     * Returns the Patient's pending proposal inbox, newest first.
     *
     * @param patientId patient identifier
     * @param status expected proposal status
     * @return matching proposals
     */
    List<HealthRecordChangeRequest>
            findByPatient_IdAndStatusOrderByRequestedAtDesc(
                    Long patientId,
                    HealthRecordChangeRequestStatus status
            );

    /**
     * Returns the complete proposal history for one Patient.
     *
     * @param patientId patient identifier
     * @return proposal history, newest first
     */
    List<HealthRecordChangeRequest>
            findByPatient_IdOrderByRequestedAtDesc(
                    Long patientId
            );

    /**
     * Returns the complete proposal history for one Provider.
     *
     * @param providerId provider identifier
     * @return proposal history, newest first
     */
    List<HealthRecordChangeRequest>
            findByProvider_IdOrderByRequestedAtDesc(
                    Long providerId
            );

    /**
     * Returns proposals between one Provider and one Patient for the
     * requested lifecycle state.
     *
     * This query will later be used when patient Consent is revoked so
     * unresolved PENDING proposals from that Provider can be cancelled.
     *
     * @param patientId patient identifier
     * @param providerId provider identifier
     * @param status requested lifecycle state
     * @return matching proposals
     */
    List<HealthRecordChangeRequest>
            findByPatient_IdAndProvider_IdAndStatusOrderByRequestedAtDesc(
                    Long patientId,
                    Long providerId,
                    HealthRecordChangeRequestStatus status
            );

    /**
     * Resolves one proposal only when it belongs to the specified Patient.
     *
     * Patient approval and rejection operations should use this ownership
     * query rather than a plain findById().
     *
     * @param requestId proposal identifier
     * @param patientId patient identifier
     * @return matching proposal when owned by that Patient
     */
    Optional<HealthRecordChangeRequest>
            findByIdAndPatient_Id(
                    Long requestId,
                    Long patientId
            );

    /**
     * Resolves one proposal only when it belongs to the specified Provider.
     *
     * Provider-side viewing or cancellation operations should use this
     * query so another Provider cannot manipulate a proposal by changing
     * an identifier in the URL.
     *
     * @param requestId proposal identifier
     * @param providerId provider identifier
     * @return matching proposal when submitted by that Provider
     */
    Optional<HealthRecordChangeRequest>
            findByIdAndProvider_Id(
                    Long requestId,
                    Long providerId
            );

    /**
     * Determines whether the same Provider already has a pending UPDATE
     * proposal for a particular permanent Health Record.
     *
     * This prevents one Provider from accidentally submitting the same
     * correction repeatedly. Another authorized Provider may still submit
     * a separate proposal; stale-record protection will prevent an older
     * proposal from overwriting a newer approved change.
     *
     * @param providerId submitting Provider
     * @param patientId affected Patient
     * @param recordType scalar or Blood Pressure
     * @param targetRecordId permanent record identifier
     * @param status lifecycle state, normally PENDING
     * @return true when a matching proposal already exists
     */
    boolean existsByProvider_IdAndPatient_IdAndRecordTypeAndTargetRecordIdAndStatus(
            Long providerId,
            Long patientId,
            HealthRecordChangeRecordType recordType,
            Long targetRecordId,
            HealthRecordChangeRequestStatus status
    );

    /**
     * Returns all pending proposals submitted by one Provider for one
     * Patient.
     *
     * This shorter query is convenient for consent-revocation processing
     * when ordering is unnecessary.
     *
     * @param patientId patient identifier
     * @param providerId provider identifier
     * @param status lifecycle state
     * @return matching proposals
     */
    List<HealthRecordChangeRequest>
            findByPatient_IdAndProvider_IdAndStatus(
                    Long patientId,
                    Long providerId,
                    HealthRecordChangeRequestStatus status
            );
}