package com.group15.health_tracking_system.consent.service;

import com.group15.health_tracking_system.common.exception.ResourceNotFoundException;
import com.group15.health_tracking_system.consent.entity.Consent;
import com.group15.health_tracking_system.consent.entity.ConsentStatus;
import com.group15.health_tracking_system.consent.entity.ProviderAccessRequest;
import com.group15.health_tracking_system.consent.entity.ProviderAccessRequestStatus;
import com.group15.health_tracking_system.consent.repository.ConsentRepository;
import com.group15.health_tracking_system.consent.repository.ProviderAccessRequestRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for the ProviderAccessRequest workflow.
 *
 * These tests verify the complete service-level authorization lifecycle:
 * provider request submission, patient approval/rejection, provider
 * cancellation, Consent activation, patient/provider isolation and
 * method-security protection.
 */
@SpringBootTest
@Transactional
class ProviderAccessRequestServiceIntegrationTests {

    @Autowired
    private ProviderAccessRequestService requestService;

    @Autowired
    private ProviderAccessRequestRepository requestRepository;

    @Autowired
    private ConsentService consentService;

    @Autowired
    private ConsentRepository consentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User patientA;
    private User patientB;

    private User providerA;
    private User providerB;

    private String passwordHash;

    @BeforeEach
    void setUp() {

        SecurityContextHolder.clearContext();

        passwordHash =
                passwordEncoder.encode(
                        "ProviderAccessRequest123!"
                );

        patientA =
                saveUser(
                        "PatientA",
                        "access.patient.a@example.com",
                        Role.PATIENT
                );

        patientB =
                saveUser(
                        "PatientB",
                        "access.patient.b@example.com",
                        Role.PATIENT
                );

        providerA =
                saveUser(
                        "ProviderA",
                        "access.provider.a@example.com",
                        Role.PROVIDER
                );

        providerB =
                saveUser(
                        "ProviderB",
                        "access.provider.b@example.com",
                        Role.PROVIDER
                );
    }

    @AfterEach
    void clearAuthentication() {

        SecurityContextHolder.clearContext();
    }

    @Test
    void providerCanSubmitPendingAccessRequestWithoutReceivingConsent() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        assertNotNull(
                request.getId()
        );

        assertEquals(
                patientA.getId(),
                request.getPatient().getId()
        );

        assertEquals(
                providerA.getId(),
                request.getProvider().getId()
        );

        assertEquals(
                ProviderAccessRequestStatus.PENDING,
                request.getStatus()
        );

        assertTrue(
                request.isPending()
        );

        assertNotNull(
                request.getRequestedAt()
        );

        assertNull(
                request.getRespondedAt()
        );

        /*
         * Merely requesting permission must never grant permission.
         */
        assertFalse(
                consentService.hasActiveConsent(
                        patientA.getId(),
                        providerA.getId()
                )
        );
    }

    @Test
    void duplicatePendingRequestIsRejected() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        requestService.requestAccess(
                patientA.getId()
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> requestService.requestAccess(
                                patientA.getId()
                        )
                );

        assertEquals(
                "An access request for this patient is already pending.",
                exception.getMessage()
        );

        assertEquals(
                1,
                requestRepository
                        .findByProvider_IdAndStatusOrderByRequestedAtDesc(
                                providerA.getId(),
                                ProviderAccessRequestStatus.PENDING
                        )
                        .size()
        );
    }

    @Test
    void activeConsentPreventsAnotherAccessRequest() {

        consentService.grantConsent(
                patientA.getId(),
                providerA.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () -> requestService.requestAccess(
                                patientA.getId()
                        )
                );

        assertEquals(
                "You already have active access to this patient.",
                exception.getMessage()
        );
    }

    @Test
    void rejectedHistoricalRequestDoesNotBlockLaterRequest() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest firstRequest =
                requestService.requestAccess(
                        patientA.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        requestService.rejectRequest(
                firstRequest.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest secondRequest =
                requestService.requestAccess(
                        patientA.getId()
                );

        assertEquals(
                ProviderAccessRequestStatus.REJECTED,
                firstRequest.getStatus()
        );

        assertEquals(
                ProviderAccessRequestStatus.PENDING,
                secondRequest.getStatus()
        );

        assertFalse(
                firstRequest.getId()
                        .equals(
                                secondRequest.getId()
                        )
        );
    }

    @Test
    void cancelledHistoricalRequestDoesNotBlockLaterRequest() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest firstRequest =
                requestService.requestAccess(
                        patientA.getId()
                );

        requestService.cancelRequest(
                firstRequest.getId()
        );

        ProviderAccessRequest secondRequest =
                requestService.requestAccess(
                        patientA.getId()
                );

        assertEquals(
                ProviderAccessRequestStatus.CANCELLED,
                firstRequest.getStatus()
        );

        assertEquals(
                ProviderAccessRequestStatus.PENDING,
                secondRequest.getStatus()
        );
    }

    @Test
    void patientCanApproveOwnRequestAndConsentBecomesActive() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        ProviderAccessRequest approved =
                requestService.approveRequest(
                        request.getId()
                );

        assertEquals(
                ProviderAccessRequestStatus.APPROVED,
                approved.getStatus()
        );

        assertFalse(
                approved.isPending()
        );

        assertNotNull(
                approved.getRespondedAt()
        );

        assertTrue(
                consentService.hasActiveConsent(
                        patientA.getId(),
                        providerA.getId()
                )
        );

        Consent consent =
                consentRepository
                        .findByPatient_IdAndProvider_Id(
                                patientA.getId(),
                                providerA.getId()
                        )
                        .orElseThrow();

        assertEquals(
                ConsentStatus.ACTIVE,
                consent.getStatus()
        );
    }

    @Test
    void approvalReactivatesRevokedConsentWithoutCreatingDuplicateConsent() {

        Consent originalConsent =
                consentService.grantConsent(
                        patientA.getId(),
                        providerA.getId()
                );

        Long originalConsentId =
                originalConsent.getId();

        consentService.revokeConsent(
                patientA.getId(),
                providerA.getId()
        );

        assertFalse(
                consentService.hasActiveConsent(
                        patientA.getId(),
                        providerA.getId()
                )
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        requestService.approveRequest(
                request.getId()
        );

        Consent reactivatedConsent =
                consentRepository
                        .findByPatient_IdAndProvider_Id(
                                patientA.getId(),
                                providerA.getId()
                        )
                        .orElseThrow();

        assertEquals(
                originalConsentId,
                reactivatedConsent.getId()
        );

        assertEquals(
                ConsentStatus.ACTIVE,
                reactivatedConsent.getStatus()
        );

        assertNull(
                reactivatedConsent.getRevokedAt()
        );

        assertEquals(
                1,
                consentRepository
                        .findByPatient_Id(
                                patientA.getId()
                        )
                        .stream()
                        .filter(consent ->
                                consent.getProvider()
                                        .getId()
                                        .equals(
                                                providerA.getId()
                                        )
                        )
                        .count()
        );
    }

    @Test
    void patientCanRejectOwnRequestWithoutGrantingConsent() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        ProviderAccessRequest rejected =
                requestService.rejectRequest(
                        request.getId()
                );

        assertEquals(
                ProviderAccessRequestStatus.REJECTED,
                rejected.getStatus()
        );

        assertNotNull(
                rejected.getRespondedAt()
        );

        assertFalse(
                consentService.hasActiveConsent(
                        patientA.getId(),
                        providerA.getId()
                )
        );

        assertTrue(
                consentRepository
                        .findByPatient_IdAndProvider_Id(
                                patientA.getId(),
                                providerA.getId()
                        )
                        .isEmpty()
        );
    }

    @Test
    void providerCanCancelOwnPendingRequest() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        ProviderAccessRequest cancelled =
                requestService.cancelRequest(
                        request.getId()
                );

        assertEquals(
                ProviderAccessRequestStatus.CANCELLED,
                cancelled.getStatus()
        );

        assertNotNull(
                cancelled.getRespondedAt()
        );

        assertFalse(
                consentService.hasActiveConsent(
                        patientA.getId(),
                        providerA.getId()
                )
        );
    }

    @Test
    void providerCannotCancelAnotherProvidersRequest() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        logInAs(
                providerB,
                Role.PROVIDER
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> requestService.cancelRequest(
                        request.getId()
                )
        );

        assertEquals(
                ProviderAccessRequestStatus.PENDING,
                request.getStatus()
        );
    }

    @Test
    void patientCannotApproveRequestBelongingToAnotherPatient() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientB.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> requestService.approveRequest(
                        request.getId()
                )
        );

        assertFalse(
                consentService.hasActiveConsent(
                        patientB.getId(),
                        providerA.getId()
                )
        );
    }

    @Test
    void patientCannotRejectRequestBelongingToAnotherPatient() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientB.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        assertThrows(
                ResourceNotFoundException.class,
                () -> requestService.rejectRequest(
                        request.getId()
                )
        );

        assertEquals(
                ProviderAccessRequestStatus.PENDING,
                request.getStatus()
        );
    }

    @Test
    void completedRequestCannotBeChangedAgain() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        requestService.approveRequest(
                request.getId()
        );

        assertThrows(
                IllegalStateException.class,
                () -> requestService.rejectRequest(
                        request.getId()
                )
        );

        assertEquals(
                ProviderAccessRequestStatus.APPROVED,
                request.getStatus()
        );
    }

    @Test
    void disabledProviderCannotBeApprovedAndRequestRemainsPending() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        providerA.setEnabled(false);

        userRepository.save(
                providerA
        );

        logInAs(
                patientA,
                Role.PATIENT
        );

        assertThrows(
                IllegalStateException.class,
                () -> requestService.approveRequest(
                        request.getId()
                )
        );

        assertEquals(
                ProviderAccessRequestStatus.PENDING,
                request.getStatus()
        );

        assertFalse(
                consentService.hasActiveConsent(
                        patientA.getId(),
                        providerA.getId()
                )
        );
    }

    @Test
    void disabledPatientCannotReceiveNewAccessRequest() {

        patientA.setEnabled(false);

        userRepository.save(
                patientA
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        assertThrows(
                IllegalStateException.class,
                () -> requestService.requestAccess(
                        patientA.getId()
                )
        );
    }

    @Test
    void patientPendingInboxContainsOnlyThatPatientsPendingRequests() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        requestService.requestAccess(
                patientA.getId()
        );

        requestService.requestAccess(
                patientB.getId()
        );

        logInAs(
                providerB,
                Role.PROVIDER
        );

        requestService.requestAccess(
                patientA.getId()
        );

        logInAs(
                patientA,
                Role.PATIENT
        );

        List<ProviderAccessRequest> pending =
                requestService
                        .getPendingRequestsForCurrentPatient();

        assertEquals(
                2,
                pending.size()
        );

        assertTrue(
                pending.stream()
                        .allMatch(request ->
                                request.getPatient()
                                        .getId()
                                        .equals(
                                                patientA.getId()
                                        )
                        )
        );

        Set<Long> providerIds =
                pending.stream()
                        .map(request ->
                                request.getProvider().getId()
                        )
                        .collect(
                                Collectors.toSet()
                        );

        assertEquals(
                Set.of(
                        providerA.getId(),
                        providerB.getId()
                ),
                providerIds
        );
    }

    @Test
    void providerHistoryIsScopedAndRetainsCompletedRequests() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest rejectedRequest =
                requestService.requestAccess(
                        patientA.getId()
                );

        logInAs(
                patientA,
                Role.PATIENT
        );

        requestService.rejectRequest(
                rejectedRequest.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest pendingRequest =
                requestService.requestAccess(
                        patientB.getId()
                );

        logInAs(
                providerB,
                Role.PROVIDER
        );

        requestService.requestAccess(
                patientA.getId()
        );

        logInAs(
                providerA,
                Role.PROVIDER
        );

        List<ProviderAccessRequest> history =
                requestService
                        .getRequestHistoryForCurrentProvider();

        assertEquals(
                2,
                history.size()
        );

        assertTrue(
                history.stream()
                        .allMatch(request ->
                                request.getProvider()
                                        .getId()
                                        .equals(
                                                providerA.getId()
                                        )
                        )
        );

        Set<ProviderAccessRequestStatus> statuses =
                history.stream()
                        .map(
                                ProviderAccessRequest::getStatus
                        )
                        .collect(
                                Collectors.toSet()
                        );

        assertEquals(
                Set.of(
                        ProviderAccessRequestStatus.REJECTED,
                        ProviderAccessRequestStatus.PENDING
                ),
                statuses
        );

        assertTrue(
                history.stream()
                        .anyMatch(request ->
                                request.getId()
                                        .equals(
                                                pendingRequest.getId()
                                        )
                        )
        );
    }

    @Test
    void patientAuthorityCannotSubmitProviderRequest() {

        logInAs(
                patientA,
                Role.PATIENT
        );

        assertThrows(
                AccessDeniedException.class,
                () -> requestService.requestAccess(
                        patientB.getId()
                )
        );
    }

    @Test
    void providerAuthorityCannotApprovePatientRequest() {

        logInAs(
                providerA,
                Role.PROVIDER
        );

        ProviderAccessRequest request =
                requestService.requestAccess(
                        patientA.getId()
                );

        assertThrows(
                AccessDeniedException.class,
                () -> requestService.approveRequest(
                        request.getId()
                )
        );
    }

    @Test
    void patientAccountClaimingProviderAuthorityIsStillRejected() {

        /*
         * The Spring authority claims PROVIDER, so @PreAuthorize passes.
         * The service must still verify the stored database role.
         */
        logInAs(
                patientA,
                Role.PROVIDER
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> requestService.requestAccess(
                        patientB.getId()
                )
        );
    }

    @Test
    void unauthenticatedCallerCannotUseRequestWorkflow() {

        SecurityContextHolder.clearContext();

        assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> requestService.requestAccess(
                        patientA.getId()
                )
        );
    }

    /**
     * Creates an enabled application user for a test.
     */
    private User saveUser(
            String firstName,
            String email,
            Role role) {

        return userRepository.save(
                new User(
                        firstName,
                        "Test",
                        email,
                        passwordHash,
                        role
                )
        );
    }

    /**
     * Replaces the active Spring Security authentication.
     *
     * The authority role is deliberately supplied independently from the
     * stored database role so tests can verify defence in depth when those
     * two values disagree.
     */
    private void logInAs(
            User account,
            Role authorityRole) {

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        account.getEmail(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_"
                                        + authorityRole.name()
                                )
                        )
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                context
        );
    }
}