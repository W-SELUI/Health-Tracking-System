package com.group15.health_tracking_system.health.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.consent.service.ProviderAccessService;
import com.group15.health_tracking_system.health.dto.BloodPressureRecordRequest;
import com.group15.health_tracking_system.health.dto.HealthRecordRequest;
import com.group15.health_tracking_system.health.entity.BloodPressureRecord;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeOperation;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRecordType;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequest;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequestStatus;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.BloodPressureRecordRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordChangeRequestRepository;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles Provider-proposed changes to Patient Health Records.
 *
 * Provider proposals are stored separately from permanent HealthRecord and
 * BloodPressureRecord data. A proposal changes permanent data only after the
 * affected Patient explicitly approves it.
 */
@Service
public class HealthRecordChangeRequestService {

    private final HealthRecordChangeRequestRepository changeRequestRepository;
    private final HealthRecordRepository healthRecordRepository;
    private final BloodPressureRecordRepository bloodPressureRecordRepository;

    private final ProviderAccessService providerAccessService;
    private final ConsentService consentService;

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public HealthRecordChangeRequestService(
            HealthRecordChangeRequestRepository changeRequestRepository,
            HealthRecordRepository healthRecordRepository,
            BloodPressureRecordRepository bloodPressureRecordRepository,
            ProviderAccessService providerAccessService,
            ConsentService consentService,
            CurrentUserService currentUserService,
            UserRepository userRepository) {

        this.changeRequestRepository =
                changeRequestRepository;

        this.healthRecordRepository =
                healthRecordRepository;

        this.bloodPressureRecordRepository =
                bloodPressureRecordRepository;

        this.providerAccessService =
                providerAccessService;

        this.consentService =
                consentService;

        this.currentUserService =
                currentUserService;

        this.userRepository =
                userRepository;
    }


    /* =========================================================
       PROVIDER CREATE PROPOSALS
       ========================================================= */

    @Transactional
    @PreAuthorize("hasRole('PROVIDER')")
    public HealthRecordChangeRequest proposeScalarCreate(
            Long patientId,
            HealthRecordRequest request) {

        User provider =
                requireCurrentProvider();

        User patient =
                requireAuthorizedPatient(
                        patientId
                );

        requireScalarRequest(
                request
        );

        HealthRecordChangeRequest proposal =
                HealthRecordChangeRequest
                        .proposeScalarCreate(
                                patient,
                                provider,
                                request.getMetricType(),
                                request.getMetricValue(),
                                request.getObservedAt(),
                                normalizeNotes(
                                        request.getNotes()
                                )
                        );

        return changeRequestRepository.save(
                proposal
        );
    }

    @Transactional
    @PreAuthorize("hasRole('PROVIDER')")
    public HealthRecordChangeRequest proposeBloodPressureCreate(
            Long patientId,
            BloodPressureRecordRequest request) {

        User provider =
                requireCurrentProvider();

        User patient =
                requireAuthorizedPatient(
                        patientId
                );

        requireBloodPressureRequest(
                request
        );

        HealthRecordChangeRequest proposal =
                HealthRecordChangeRequest
                        .proposeBloodPressureCreate(
                                patient,
                                provider,
                                request.getSystolic(),
                                request.getDiastolic(),
                                request.getObservedAt(),
                                normalizeNotes(
                                        request.getNotes()
                                )
                        );

        return changeRequestRepository.save(
                proposal
        );
    }


    /* =========================================================
       PROVIDER UPDATE PROPOSALS
       ========================================================= */

    @Transactional
    @PreAuthorize("hasRole('PROVIDER')")
    public HealthRecordChangeRequest proposeScalarUpdate(
            Long patientId,
            Long recordId,
            HealthRecordRequest request) {

        User provider =
                requireCurrentProvider();

        User patient =
                requireAuthorizedPatient(
                        patientId
                );

        requireRecordId(
                recordId
        );

        requireScalarRequest(
                request
        );

        HealthRecord originalRecord =
                healthRecordRepository
                        .findByIdAndPatient(
                                recordId,
                                patient
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Health record not found"
                                )
                        );

        requireNoPendingUpdate(
                provider,
                patient,
                HealthRecordChangeRecordType.SCALAR,
                recordId
        );

        HealthRecordChangeRequest proposal =
                HealthRecordChangeRequest
                        .proposeScalarUpdate(
                                patient,
                                provider,
                                originalRecord,
                                request.getMetricType(),
                                request.getMetricValue(),
                                request.getObservedAt(),
                                normalizeNotes(
                                        request.getNotes()
                                )
                        );

        return changeRequestRepository.save(
                proposal
        );
    }

    @Transactional
    @PreAuthorize("hasRole('PROVIDER')")
    public HealthRecordChangeRequest proposeBloodPressureUpdate(
            Long patientId,
            Long recordId,
            BloodPressureRecordRequest request) {

        User provider =
                requireCurrentProvider();

        User patient =
                requireAuthorizedPatient(
                        patientId
                );

        requireRecordId(
                recordId
        );

        requireBloodPressureRequest(
                request
        );

        BloodPressureRecord originalRecord =
                bloodPressureRecordRepository
                        .findByIdAndPatient(
                                recordId,
                                patient
                        )
                        .orElseThrow(
                                () -> new ResourceNotFoundException(
                                        "Blood-pressure record not found"
                                )
                        );

        requireNoPendingUpdate(
                provider,
                patient,
                HealthRecordChangeRecordType.BLOOD_PRESSURE,
                recordId
        );

        HealthRecordChangeRequest proposal =
                HealthRecordChangeRequest
                        .proposeBloodPressureUpdate(
                                patient,
                                provider,
                                originalRecord,
                                request.getSystolic(),
                                request.getDiastolic(),
                                request.getObservedAt(),
                                normalizeNotes(
                                        request.getNotes()
                                )
                        );

        return changeRequestRepository.save(
                proposal
        );
    }


    /* =========================================================
       PATIENT QUERY
       ========================================================= */

    /**
     * Returns pending proposals awaiting the authenticated Patient.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PATIENT')")
    public List<HealthRecordChangeRequest>
            getPendingRequestsForCurrentPatient() {

        User patient =
                requireCurrentPatient();

        return changeRequestRepository
                .findByPatient_IdAndStatusOrderByRequestedAtDesc(
                        patient.getId(),
                        HealthRecordChangeRequestStatus.PENDING
                );
    }

    /**
     * Returns the authenticated Patient's complete proposal history.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PATIENT')")
    public List<HealthRecordChangeRequest>
            getRequestHistoryForCurrentPatient() {

        User patient =
                requireCurrentPatient();

        return changeRequestRepository
                .findByPatient_IdOrderByRequestedAtDesc(
                        patient.getId()
                );
    }

    /**
     * Returns the authenticated Provider's proposal history.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('PROVIDER')")
    public List<HealthRecordChangeRequest>
            getRequestHistoryForCurrentProvider() {

        User provider =
                requireCurrentProvider();

        return changeRequestRepository
                .findByProvider_IdOrderByRequestedAtDesc(
                        provider.getId()
                );
    }


    /* =========================================================
       PATIENT APPROVAL
       ========================================================= */

    /**
     * Approves one Provider proposal belonging to the authenticated Patient.
     *
     * CREATE requests create permanent Provider-recorded data.
     * UPDATE requests first verify that the permanent record still matches
     * the original snapshot stored when the proposal was submitted.
     *
     * If the target UPDATE record changed or disappeared, the proposal is
     * marked STALE and no permanent modification is performed.
     *
     * If Provider access is no longer active, the proposal is cancelled
     * instead of being applied.
     *
     * @param requestId proposal identifier
     * @return final proposal state
     */
    @Transactional
    @PreAuthorize("hasRole('PATIENT')")
    public HealthRecordChangeRequest approveRequest(
            Long requestId) {

        User patient =
                requireCurrentPatient();

        HealthRecordChangeRequest proposal =
                requirePatientRequest(
                        requestId,
                        patient
                );

        requirePending(
                proposal
        );

        /*
         * A proposal cannot be approved after its Provider loses access.
         *
         * Later we will also cancel these immediately from the Consent
         * revocation workflow. This check protects the approval boundary
         * even before that integration occurs.
         */
        if (!providerStillAuthorized(
                proposal
        )) {

            proposal.cancel();

            return changeRequestRepository.save(
                    proposal
            );
        }

        if (proposal.getOperation()
                == HealthRecordChangeOperation.CREATE) {

            return approveCreate(
                    proposal,
                    patient
            );
        }

        if (proposal.getOperation()
                == HealthRecordChangeOperation.UPDATE) {

            return approveUpdate(
                    proposal,
                    patient
            );
        }

        throw new IllegalStateException(
                "Unsupported Health Record change operation"
        );
    }

    /**
     * Rejects one pending proposal.
     *
     * Rejection never modifies permanent Health Record data.
     */
    @Transactional
    @PreAuthorize("hasRole('PATIENT')")
    public HealthRecordChangeRequest rejectRequest(
            Long requestId) {

        User patient =
                requireCurrentPatient();

        HealthRecordChangeRequest proposal =
                requirePatientRequest(
                        requestId,
                        patient
                );

        requirePending(
                proposal
        );

        proposal.reject();

        return changeRequestRepository.save(
                proposal
        );
    }


    /* =========================================================
       APPROVAL IMPLEMENTATION
       ========================================================= */

    private HealthRecordChangeRequest approveCreate(
            HealthRecordChangeRequest proposal,
            User patient) {

        if (proposal.getRecordType()
                == HealthRecordChangeRecordType.SCALAR) {

            return approveScalarCreate(
                    proposal,
                    patient
            );
        }

        if (proposal.getRecordType()
                == HealthRecordChangeRecordType.BLOOD_PRESSURE) {

            return approveBloodPressureCreate(
                    proposal,
                    patient
            );
        }

        throw new IllegalStateException(
                "Unsupported Health Record type"
        );
    }

    private HealthRecordChangeRequest approveScalarCreate(
            HealthRecordChangeRequest proposal,
            User patient) {

        HealthRecord record =
                new HealthRecord(
                        patient,
                        proposal.getProvider(),
                        proposal.getProposedMetricType(),
                        proposal.getProposedMetricValue(),
                        proposal.getProposedObservedAt(),
                        proposal.getProposedNotes(),
                        HealthRecordSource.PROVIDER_RECORDED
                );

        HealthRecord savedRecord =
                healthRecordRepository.save(
                        record
                );

        proposal.approve(
                savedRecord.getId()
        );

        return changeRequestRepository.save(
                proposal
        );
    }

    private HealthRecordChangeRequest approveBloodPressureCreate(
            HealthRecordChangeRequest proposal,
            User patient) {

        BloodPressureRecord record =
                new BloodPressureRecord(
                        patient,
                        proposal.getProvider(),
                        proposal.getProposedSystolic(),
                        proposal.getProposedDiastolic(),
                        proposal.getProposedObservedAt(),
                        proposal.getProposedNotes(),
                        HealthRecordSource.PROVIDER_RECORDED
                );

        BloodPressureRecord savedRecord =
                bloodPressureRecordRepository.save(
                        record
                );

        proposal.approve(
                savedRecord.getId()
        );

        return changeRequestRepository.save(
                proposal
        );
    }

    private HealthRecordChangeRequest approveUpdate(
            HealthRecordChangeRequest proposal,
            User patient) {

        if (proposal.getRecordType()
                == HealthRecordChangeRecordType.SCALAR) {

            return approveScalarUpdate(
                    proposal,
                    patient
            );
        }

        if (proposal.getRecordType()
                == HealthRecordChangeRecordType.BLOOD_PRESSURE) {

            return approveBloodPressureUpdate(
                    proposal,
                    patient
            );
        }

        throw new IllegalStateException(
                "Unsupported Health Record type"
        );
    }

    private HealthRecordChangeRequest approveScalarUpdate(
            HealthRecordChangeRequest proposal,
            User patient) {

        HealthRecord currentRecord =
                healthRecordRepository
                        .findByIdAndPatient(
                                proposal.getTargetRecordId(),
                                patient
                        )
                        .orElse(null);

        /*
         * The Patient may have deleted the original record while the
         * Provider proposal was pending.
         */
        if (currentRecord == null) {

            proposal.markStale();

            return changeRequestRepository.save(
                    proposal
            );
        }

        if (!scalarSnapshotMatches(
                proposal,
                currentRecord
        )) {

            proposal.markStale();

            return changeRequestRepository.save(
                    proposal
            );
        }

        /*
         * Only editable measurement fields are changed.
         *
         * patient, recordedBy and source deliberately remain untouched.
         */
        currentRecord.setMetricType(
                proposal.getProposedMetricType()
        );

        currentRecord.setMetricValue(
                proposal.getProposedMetricValue()
        );

        currentRecord.setObservedAt(
                proposal.getProposedObservedAt()
        );

        currentRecord.setNotes(
                proposal.getProposedNotes()
        );

        HealthRecord savedRecord =
                healthRecordRepository.save(
                        currentRecord
                );

        proposal.approve(
                savedRecord.getId()
        );

        return changeRequestRepository.save(
                proposal
        );
    }

    private HealthRecordChangeRequest approveBloodPressureUpdate(
            HealthRecordChangeRequest proposal,
            User patient) {

        BloodPressureRecord currentRecord =
                bloodPressureRecordRepository
                        .findByIdAndPatient(
                                proposal.getTargetRecordId(),
                                patient
                        )
                        .orElse(null);

        if (currentRecord == null) {

            proposal.markStale();

            return changeRequestRepository.save(
                    proposal
            );
        }

        if (!bloodPressureSnapshotMatches(
                proposal,
                currentRecord
        )) {

            proposal.markStale();

            return changeRequestRepository.save(
                    proposal
            );
        }

        currentRecord.setSystolic(
                proposal.getProposedSystolic()
        );

        currentRecord.setDiastolic(
                proposal.getProposedDiastolic()
        );

        currentRecord.setObservedAt(
                proposal.getProposedObservedAt()
        );

        currentRecord.setNotes(
                proposal.getProposedNotes()
        );

        BloodPressureRecord savedRecord =
                bloodPressureRecordRepository.save(
                        currentRecord
                );

        proposal.approve(
                savedRecord.getId()
        );

        return changeRequestRepository.save(
                proposal
        );
    }


    /* =========================================================
       STALE DETECTION
       ========================================================= */

    /**
     * Confirms that a scalar HealthRecord still exactly matches the
     * snapshot captured when the Provider created the proposal.
     */
    private boolean scalarSnapshotMatches(
            HealthRecordChangeRequest proposal,
            HealthRecord currentRecord) {

        return Objects.equals(
                        proposal.getOriginalMetricType(),
                        currentRecord.getMetricType()
                )
                && sameNumber(
                        proposal.getOriginalMetricValue(),
                        currentRecord.getMetricValue()
                )
                && Objects.equals(
                        proposal.getOriginalObservedAt(),
                        currentRecord.getObservedAt()
                )
                && Objects.equals(
                        proposal.getOriginalNotes(),
                        currentRecord.getNotes()
                )
                && Objects.equals(
                        proposal.getOriginalSource(),
                        currentRecord.getSource()
                )
                && Objects.equals(
                        proposal.getOriginalRecordedByUserId(),
                        userId(
                                currentRecord.getRecordedBy()
                        )
                )
                && Objects.equals(
                        proposal.getOriginalRecordUpdatedAt(),
                        currentRecord.getUpdatedAt()
                );
    }

    /**
     * Confirms that a BloodPressureRecord still exactly matches the
     * snapshot captured when the Provider created the proposal.
     */
    private boolean bloodPressureSnapshotMatches(
            HealthRecordChangeRequest proposal,
            BloodPressureRecord currentRecord) {

        return sameNumber(
                        proposal.getOriginalSystolic(),
                        currentRecord.getSystolic()
                )
                && sameNumber(
                        proposal.getOriginalDiastolic(),
                        currentRecord.getDiastolic()
                )
                && Objects.equals(
                        proposal.getOriginalObservedAt(),
                        currentRecord.getObservedAt()
                )
                && Objects.equals(
                        proposal.getOriginalNotes(),
                        currentRecord.getNotes()
                )
                && Objects.equals(
                        proposal.getOriginalSource(),
                        currentRecord.getSource()
                )
                && Objects.equals(
                        proposal.getOriginalRecordedByUserId(),
                        userId(
                                currentRecord.getRecordedBy()
                        )
                )
                && Objects.equals(
                        proposal.getOriginalRecordUpdatedAt(),
                        currentRecord.getUpdatedAt()
                );
    }

    private boolean sameNumber(
            BigDecimal first,
            BigDecimal second) {

        if (first == null
                && second == null) {

            return true;
        }

        if (first == null
                || second == null) {

            return false;
        }

        /*
         * compareTo avoids false stale detection caused only by decimal
         * scale differences such as 80.0 versus 80.000.
         */
        return first.compareTo(
                second
        ) == 0;
    }

    private Long userId(
            User user) {

        return user == null
                ? null
                : user.getId();
    }


    /* =========================================================
       PATIENT REQUEST OWNERSHIP
       ========================================================= */

    private HealthRecordChangeRequest requirePatientRequest(
            Long requestId,
            User patient) {

        if (requestId == null) {

            throw new IllegalArgumentException(
                    "Health Record change request ID is required"
            );
        }

        return changeRequestRepository
                .findByIdAndPatient_Id(
                        requestId,
                        patient.getId()
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Health Record change request not found"
                        )
                );
    }

    private void requirePending(
            HealthRecordChangeRequest proposal) {

        if (!proposal.isPending()) {

            throw new IllegalStateException(
                    "Only pending Health Record change requests "
                    + "can be reviewed"
            );
        }
    }


    /* =========================================================
       CONSENT CHECK DURING APPROVAL
       ========================================================= */

    private boolean providerStillAuthorized(
            HealthRecordChangeRequest proposal) {

        User provider =
                proposal.getProvider();

        if (provider == null
                || !provider.isEnabled()) {

            return false;
        }

        return consentService
                .hasActiveConsent(
                        proposal.getPatient().getId(),
                        provider.getId()
                );
    }


    /* =========================================================
       USER AUTHORIZATION
       ========================================================= */

    private User requireCurrentProvider() {

        User provider =
                currentUserService
                        .getCurrentUser();

        if (provider.getRole()
                != Role.PROVIDER) {

            throw new IllegalArgumentException(
                    "Only healthcare providers can propose "
                    + "Health Record changes"
            );
        }

        if (!provider.isEnabled()) {

            throw new IllegalArgumentException(
                    "Disabled healthcare providers cannot propose "
                    + "Health Record changes"
            );
        }

        return provider;
    }

    private User requireCurrentPatient() {

        User patient =
                currentUserService
                        .getCurrentUser();

        if (patient.getRole()
                != Role.PATIENT) {

            throw new IllegalArgumentException(
                    "Only patients can review "
                    + "Health Record change requests"
            );
        }

        if (!patient.isEnabled()) {

            throw new IllegalArgumentException(
                    "Disabled patients cannot review "
                    + "Health Record change requests"
            );
        }

        return patient;
    }

    private User requireAuthorizedPatient(
            Long patientId) {

        if (patientId == null) {

            throw new IllegalArgumentException(
                    "Patient is required"
            );
        }

        providerAccessService
                .requireActiveConsent(
                        patientId
                );

        return userRepository
                .findById(
                        patientId
                )
                .filter(user ->
                        user.getRole()
                                == Role.PATIENT
                )
                .filter(
                        User::isEnabled
                )
                .orElseThrow(
                        () -> new ResourceNotFoundException(
                                "Patient not found"
                        )
                );
    }


    /* =========================================================
       DUPLICATE UPDATE PROTECTION
       ========================================================= */

    private void requireNoPendingUpdate(
            User provider,
            User patient,
            HealthRecordChangeRecordType recordType,
            Long recordId) {

        boolean pendingProposalExists =
                changeRequestRepository
                        .existsByProvider_IdAndPatient_IdAndRecordTypeAndTargetRecordIdAndStatus(
                                provider.getId(),
                                patient.getId(),
                                recordType,
                                recordId,
                                HealthRecordChangeRequestStatus.PENDING
                        );

        if (pendingProposalExists) {

            throw new IllegalStateException(
                    "You already have a pending update proposal "
                    + "for this record"
            );
        }
    }


    /* =========================================================
       REQUEST VALIDATION
       ========================================================= */

    private void requireScalarRequest(
            HealthRecordRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Health record request is required"
            );
        }

        if (request.getMetricType() == null) {

            throw new IllegalArgumentException(
                    "Health metric is required"
            );
        }

        if (request.getMetricValue() == null) {

            throw new IllegalArgumentException(
                    "Measurement value is required"
            );
        }

        requireObservedAt(
                request.getObservedAt()
        );

        requireNotes(
                request.getNotes()
        );
    }

    private void requireBloodPressureRequest(
            BloodPressureRecordRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Blood-pressure request is required"
            );
        }

        if (request.getSystolic() == null) {

            throw new IllegalArgumentException(
                    "Systolic value is required"
            );
        }

        if (request.getDiastolic() == null) {

            throw new IllegalArgumentException(
                    "Diastolic value is required"
            );
        }

        requireObservedAt(
                request.getObservedAt()
        );

        requireNotes(
                request.getNotes()
        );
    }

    private void requireObservedAt(
            LocalDateTime observedAt) {

        if (observedAt == null) {

            throw new IllegalArgumentException(
                    "Observation date and time are required"
            );
        }

        if (observedAt.isAfter(
                LocalDateTime.now()
        )) {

            throw new IllegalArgumentException(
                    "Observation time cannot be in the future"
            );
        }
    }

    private void requireNotes(
            String notes) {

        if (notes != null
                && notes.length() > 1000) {

            throw new IllegalArgumentException(
                    "Notes must not exceed 1000 characters"
            );
        }
    }

    private void requireRecordId(
            Long recordId) {

        if (recordId == null) {

            throw new IllegalArgumentException(
                    "Health record ID is required"
            );
        }
    }


    /* =========================================================
       NORMALIZATION
       ========================================================= */

    private String normalizeNotes(
            String notes) {

        if (notes == null
                || notes.isBlank()) {

            return null;
        }

        return notes.trim();
    }
}