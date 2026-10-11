package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.entity.ProviderAccessRequest;
import com.group15.health_tracking_system.consent.entity.ProviderAccessRequestStatus;
import com.group15.health_tracking_system.consent.repository.ProviderAccessRequestRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates the provider-access request workflow.
 *
 * A ProviderAccessRequest represents a request for permission only.
 * Actual authorization continues to be represented by Consent.
 *
 *
 * 
 */
@Service
public class ProviderAccessRequestService {

    private final ProviderAccessRequestRepository requestRepository;
    private final ConsentService consentService;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public ProviderAccessRequestService(
            ProviderAccessRequestRepository requestRepository,
            ConsentService consentService,
            UserRepository userRepository,
            CurrentUserService currentUserService) {

        this.requestRepository = requestRepository;
        this.consentService = consentService;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * Submits a new provider access request for a patient.
     *
     * The authenticated provider cannot submit another request when:
     *
     * 
     *
     * @param patientId patient whose data the provider wants to access
     * @return newly created pending request
     */
    @Transactional
    @PreAuthorize("hasRole('PROVIDER')")
    public ProviderAccessRequest requestAccess(
            Long patientId) {

        User provider =
                currentUserService.getCurrentUser();

        validateProvider(provider);

        User patient =
                getUser(patientId);

        validatePatient(patient);

        /*
         * A provider who already has active permission does not need,
         * and must not create, another access request.
         */
        if (consentService.hasActiveConsent(
                patient.getId(),
                provider.getId())) {

            throw new IllegalStateException(
                    "You already have active access to this patient."
            );
        }

        /*
         * Only one unresolved request is allowed for a patient/provider
         * pair at a time. Historical APPROVED, REJECTED and CANCELLED
         * requests are retained and do not block future requests.
         */
        boolean pendingRequestExists =
                requestRepository
                        .existsByPatient_IdAndProvider_IdAndStatus(
                                patient.getId(),
                                provider.getId(),
                                ProviderAccessRequestStatus.PENDING
                        );

        if (pendingRequestExists) {

            throw new IllegalStateException(
                    "An access request for this patient is already pending."
            );
        }

        ProviderAccessRequest request =
                new ProviderAccessRequest(
                        patient,
                        provider
                );

        return requestRepository.save(request);
    }

    /**
     * Approves a pending request belonging to the authenticated patient.
     *
     * Request approval and Consent activation occur inside the same
     * transaction. Therefore they either both succeed or both roll back.
     *
     * @param requestId access-request identifier
     * @return approved request
     */
    @Transactional
    @PreAuthorize("hasRole('PATIENT')")
    public ProviderAccessRequest approveRequest(
            Long requestId) {

        User patient =
                currentUserService.getCurrentUser();

        validatePatient(patient);

        ProviderAccessRequest request =
                getPatientRequest(
                        requestId,
                        patient.getId()
                );

        User provider =
                request.getProvider();

        /*
         * The provider may have been disabled after submitting the request.
         * In that case access must not be granted.
         */
        validateProvider(provider);

        /*
         * The entity itself also checks this rule, but keeping the check
         * here produces a clearer service-level error.
         */
        requirePending(request);

        request.approve();

        /*
         * Existing REVOKED Consent is reactivated.
         * If no Consent exists, a new ACTIVE Consent is created.
         */
        consentService.grantConsent(
                patient.getId(),
                provider.getId()
        );

        return requestRepository.save(request);
    }

    /**
     * Rejects a pending request belonging to the authenticated patient.
     *
     * Rejection does not create or activate Consent.
     *
     * @param requestId access-request identifier
     * @return rejected request
     */
    @Transactional
    @PreAuthorize("hasRole('PATIENT')")
    public ProviderAccessRequest rejectRequest(
            Long requestId) {

        User patient =
                currentUserService.getCurrentUser();

        validatePatient(patient);

        ProviderAccessRequest request =
                getPatientRequest(
                        requestId,
                        patient.getId()
                );

        requirePending(request);

        request.reject();

        return requestRepository.save(request);
    }

    /**
     * Cancels a pending request submitted by the authenticated provider.
     *
     * A provider cannot cancel another provider's request because the
     * repository query requires both request ID and provider ID.
     *
     * @param requestId access-request identifier
     * @return cancelled request
     */
    @Transactional
    @PreAuthorize("hasRole('PROVIDER')")
    public ProviderAccessRequest cancelRequest(
            Long requestId) {

        User provider =
                currentUserService.getCurrentUser();

        validateProvider(provider);

        ProviderAccessRequest request =
                requestRepository
                        .findByIdAndProvider_Id(
                                requestId,
                                provider.getId()
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Provider access request was not found"
                                )
                        );

        requirePending(request);

        request.cancel();

        return requestRepository.save(request);
    }

    /**
     * Returns pending access requests awaiting action from the
     * authenticated patient.
     *
     * @return pending patient requests, newest first
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PATIENT')")
    public List<ProviderAccessRequest>
            getPendingRequestsForCurrentPatient() {

        User patient =
                currentUserService.getCurrentUser();

        validatePatient(patient);

        return requestRepository
                .findByPatient_IdAndStatusOrderByRequestedAtDesc(
                        patient.getId(),
                        ProviderAccessRequestStatus.PENDING
                );
    }

    /**
     * Returns the complete provider-access request history for the
     * authenticated patient.
     *
     * @return patient request history, newest first
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PATIENT')")
    public List<ProviderAccessRequest>
            getRequestHistoryForCurrentPatient() {

        User patient =
                currentUserService.getCurrentUser();

        validatePatient(patient);

        return requestRepository
                .findByPatient_IdOrderByRequestedAtDesc(
                        patient.getId()
                );
    }

    /**
     * Returns the authenticated provider's currently pending requests.
     *
     * @return provider pending requests, newest first
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PROVIDER')")
    public List<ProviderAccessRequest>
            getPendingRequestsForCurrentProvider() {

        User provider =
                currentUserService.getCurrentUser();

        validateProvider(provider);

        return requestRepository
                .findByProvider_IdAndStatusOrderByRequestedAtDesc(
                        provider.getId(),
                        ProviderAccessRequestStatus.PENDING
                );
    }

    /**
     * Returns the authenticated provider's complete access-request history.
     *
     * @return provider request history, newest first
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PROVIDER')")
    public List<ProviderAccessRequest>
            getRequestHistoryForCurrentProvider() {

        User provider =
                currentUserService.getCurrentUser();

        validateProvider(provider);

        return requestRepository
                .findByProvider_IdOrderByRequestedAtDesc(
                        provider.getId()
                );
    }

    /**
     * Returns enabled patients that a provider could potentially request
     * access to.
     *
     * The presentation layer can still indicate whether each patient
     * already has active consent or a pending request.
     *
     * @return enabled Patient users ordered by name
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PROVIDER')")
    public List<User> getAvailablePatients() {

        User provider =
                currentUserService.getCurrentUser();

        validateProvider(provider);

        return userRepository
                .findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(
                        Role.PATIENT
                );
    }

    /**
     * Determines whether the authenticated provider currently has a pending
     * request for the specified patient.
     *
     * @param patientId patient identifier
     * @return true when a pending request already exists
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PROVIDER')")
    public boolean hasPendingRequest(
            Long patientId) {

        User provider =
                currentUserService.getCurrentUser();

        validateProvider(provider);

        return requestRepository
                .existsByPatient_IdAndProvider_IdAndStatus(
                        patientId,
                        provider.getId(),
                        ProviderAccessRequestStatus.PENDING
                );
    }

    /**
     * Loads a request only when it belongs to the specified patient.
     */
    private ProviderAccessRequest getPatientRequest(
            Long requestId,
            Long patientId) {

        if (requestId == null) {

            throw new ResourceNotFoundException(
                    "Provider access request was not found"
            );
        }

        return requestRepository
                .findByIdAndPatient_Id(
                        requestId,
                        patientId
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Provider access request was not found"
                        )
                );
    }

    /**
     * Loads a User or reports a normal domain-level not-found error.
     */
    private User getUser(
            Long userId) {

        if (userId == null) {

            throw new ResourceNotFoundException(
                    "User was not found"
            );
        }

        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "User was not found: " + userId
                        )
                );
    }

    /**
     * Ensures the supplied account is an enabled Patient.
     */
    private void validatePatient(
            User patient) {

        if (patient == null
                || patient.getRole() != Role.PATIENT) {

            throw new IllegalArgumentException(
                    "User must have the PATIENT role"
            );
        }

        if (!patient.isEnabled()) {

            throw new IllegalStateException(
                    "Patient account is disabled"
            );
        }
    }

    /**
     * Ensures the supplied account is an enabled Provider.
     */
    private void validateProvider(
            User provider) {

        if (provider == null
                || provider.getRole() != Role.PROVIDER) {

            throw new IllegalArgumentException(
                    "User must have the PROVIDER role"
            );
        }

        if (!provider.isEnabled()) {

            throw new IllegalStateException(
                    "Provider account is disabled"
            );
        }
    }

    /**
     * Ensures that a completed request cannot be acted upon again.
     */
    private void requirePending(
            ProviderAccessRequest request) {

        if (!request.isPending()) {

            throw new IllegalStateException(
                    "Only pending provider access requests can be changed"
            );
        }
    }
}