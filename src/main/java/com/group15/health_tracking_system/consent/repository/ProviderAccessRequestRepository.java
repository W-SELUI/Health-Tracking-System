package com.group15.health_tracking_system.consent.repository;

import com.group15.health_tracking_system.consent.entity.ProviderAccessRequest;
import com.group15.health_tracking_system.consent.entity.ProviderAccessRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence contract for provider access requests.
 *
 * Access requests are retained as historical records after approval,
 * rejection or cancellation. The service layer prevents duplicate
 * simultaneous pending requests for the same patient/provider pair.
 */
public interface ProviderAccessRequestRepository
        extends JpaRepository<ProviderAccessRequest, Long> {

    /**
     * Determines whether a request with the given status already exists
     * between a patient and provider.
     *
     * This is primarily used to prevent more than one PENDING request
     * from existing for the same patient/provider pair.
     *
     * @param patientId patient identifier
     * @param providerId provider identifier
     * @param status request status
     * @return true when a matching request exists
     */
    boolean existsByPatient_IdAndProvider_IdAndStatus(
            Long patientId,
            Long providerId,
            ProviderAccessRequestStatus status
    );

    /**
     * Returns a particular request only when it belongs to the specified
     * patient.
     *
     * The provider is eagerly loaded because patient review screens need
     * to display the requesting provider's identity.
     *
     * @param requestId request identifier
     * @param patientId patient identifier
     * @return matching request when found
     */
    @EntityGraph(attributePaths = "provider")
    Optional<ProviderAccessRequest> findByIdAndPatient_Id(
            Long requestId,
            Long patientId
    );

    /**
     * Returns a particular request only when it belongs to the specified
     * provider.
     *
     * The patient is eagerly loaded because provider request-history
     * screens need to display the target patient's identity.
     *
     * @param requestId request identifier
     * @param providerId provider identifier
     * @return matching request when found
     */
    @EntityGraph(attributePaths = "patient")
    Optional<ProviderAccessRequest> findByIdAndProvider_Id(
            Long requestId,
            Long providerId
    );

    /**
     * Returns the patient's pending provider access requests, newest first.
     *
     * @param patientId patient identifier
     * @param status request status
     * @return matching requests
     */
    @EntityGraph(attributePaths = "provider")
    List<ProviderAccessRequest>
            findByPatient_IdAndStatusOrderByRequestedAtDesc(
                    Long patientId,
                    ProviderAccessRequestStatus status
            );

    /**
     * Returns the complete provider-access request history for a patient,
     * newest first.
     *
     * @param patientId patient identifier
     * @return patient request history
     */
    @EntityGraph(attributePaths = "provider")
    List<ProviderAccessRequest>
            findByPatient_IdOrderByRequestedAtDesc(
                    Long patientId
            );

    /**
     * Returns a provider's requests having a particular status,
     * newest first.
     *
     * @param providerId provider identifier
     * @param status request status
     * @return matching requests
     */
    @EntityGraph(attributePaths = "patient")
    List<ProviderAccessRequest>
            findByProvider_IdAndStatusOrderByRequestedAtDesc(
                    Long providerId,
                    ProviderAccessRequestStatus status
            );

    /**
     * Returns the complete request history submitted by a provider,
     * newest first.
     *
     * @param providerId provider identifier
     * @return provider request history
     */
    @EntityGraph(attributePaths = "patient")
    List<ProviderAccessRequest>
            findByProvider_IdOrderByRequestedAtDesc(
                    Long providerId
            );
}