package com.group15.health_tracking_system.health.service;

import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequest;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequestStatus;
import com.group15.health_tracking_system.health.repository.HealthRecordChangeRequestRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Handles cancellation of unresolved Health Record change proposals when
 * the Provider's access to the affected Patient is revoked.
 *
 * <p>
 * This service is intentionally separate from
 * HealthRecordChangeRequestService so ConsentService can invoke it without
 * creating a circular service dependency.
 * </p>
 */
@Service
public class HealthRecordChangeRequestCancellationService {

    private final HealthRecordChangeRequestRepository changeRequestRepository;

    public HealthRecordChangeRequestCancellationService(
            HealthRecordChangeRequestRepository changeRequestRepository) {

        this.changeRequestRepository =
                changeRequestRepository;
    }

    /**
     * Cancels every pending Health Record proposal submitted by one Provider
     * for one Patient.
     *
     * @param patientId affected Patient
     * @param providerId Provider whose access was revoked
     * @return number of proposals cancelled
     */
    @Transactional
    public int cancelPendingRequestsForRevokedConsent(
            Long patientId,
            Long providerId) {

        List<HealthRecordChangeRequest> pendingRequests =
                changeRequestRepository
                        .findByPatient_IdAndProvider_IdAndStatus(
                                patientId,
                                providerId,
                                HealthRecordChangeRequestStatus.PENDING
                        );

        pendingRequests.forEach(
                HealthRecordChangeRequest::cancel
        );

        changeRequestRepository.saveAll(
                pendingRequests
        );

        return pendingRequests.size();
    }
}