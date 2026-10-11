package com.group15.health_tracking_system.consent.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.consent.service.ConsentService;
import com.group15.health_tracking_system.consent.service.ProviderAccessRequestService;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP integration tests for the provider-access request workflow.
 *
 * Service-level tests already verify persistence, state transitions and
 * ownership rules. These tests verify that the web layer exposes those
 * rules correctly to Patient and Provider users.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProviderAccessWorkflowControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConsentService consentService;

    @MockitoBean
    private ProviderAccessRequestService requestService;

    @MockitoBean
    private CurrentUserService currentUserService;

    private User patient;
    private User provider;

    @BeforeEach
    void setUp() {

        patient =
                org.mockito.Mockito.mock(
                        User.class
                );

        provider =
                org.mockito.Mockito.mock(
                        User.class
                );

        when(
                patient.getId()
        ).thenReturn(
                101L
        );

        when(
                patient.getFullName()
        ).thenReturn(
                "Patient Test"
        );

        when(
                provider.getId()
        ).thenReturn(
                201L
        );

        when(
                provider.getFullName()
        ).thenReturn(
                "Provider Test"
        );
    }


    /* =========================================================
       PROVIDER PAGE
       ========================================================= */

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCanOpenPatientAccessWorkspace()
            throws Exception {

        when(
                currentUserService.getCurrentUser()
        ).thenReturn(
                provider
        );

        when(
                consentService.getAuthorizedPatients(
                        provider.getId()
                )
        ).thenReturn(
                List.of()
        );

        when(
                requestService.getAvailablePatients()
        ).thenReturn(
                List.of()
        );

        when(
                requestService.getPendingRequestsForCurrentProvider()
        ).thenReturn(
                List.of()
        );

        when(
                requestService.getRequestHistoryForCurrentProvider()
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get(
                                "/provider/patients"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "consent/provider-patients"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "currentUser"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "authorizedPatients"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "availablePatients"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "pendingRequests"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "requestHistory"
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCannotOpenProviderPatientAccessWorkspace()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/provider/patients"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void unauthenticatedUserCannotOpenProviderPatientAccessWorkspace()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/provider/patients"
                        )
                )
                .andExpect(
                        status().is3xxRedirection()
                );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCanSubmitAccessRequest()
            throws Exception {

        Long patientId = 101L;

        mockMvc.perform(
                        post(
                                "/provider/patients/"
                                + patientId
                                + "/request-access"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/provider/patients"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "successMessage",
                                "Access request sent to the patient."
                        )
                );

        verify(
                requestService
        ).requestAccess(
                patientId
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void duplicateProviderRequestDisplaysErrorMessage()
            throws Exception {

        Long patientId = 101L;

        when(
                requestService.requestAccess(
                        patientId
                )
        ).thenThrow(
                new IllegalStateException(
                        "An access request for this patient is already pending."
                )
        );

        mockMvc.perform(
                        post(
                                "/provider/patients/"
                                + patientId
                                + "/request-access"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/provider/patients"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "errorMessage",
                                "An access request for this patient is already pending."
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCanCancelOwnPendingRequest()
            throws Exception {

        Long requestId = 501L;

        mockMvc.perform(
                        post(
                                "/provider/patients/access-requests/"
                                + requestId
                                + "/cancel"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/provider/patients"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "successMessage",
                                "Access request cancelled."
                        )
                );

        verify(
                requestService
        ).cancelRequest(
                requestId
        );
    }

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCannotSubmitProviderAccessRequestThroughProviderRoute()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/provider/patients/101/request-access"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                requestService,
                never()
        ).requestAccess(
                anyLong()
        );
    }


    /* =========================================================
       PATIENT PAGE
       ========================================================= */

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCanOpenProviderAccessInbox()
            throws Exception {

        when(
                currentUserService.getCurrentUser()
        ).thenReturn(
                patient
        );

        when(
                consentService.getPatientConsents(
                        patient.getId()
                )
        ).thenReturn(
                List.of()
        );

        when(
                consentService.getAvailableProviders()
        ).thenReturn(
                List.of()
        );

        when(
                requestService.getPendingRequestsForCurrentPatient()
        ).thenReturn(
                List.of()
        );

        when(
                requestService.getRequestHistoryForCurrentPatient()
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get(
                                "/patient/consents"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "consent/access"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "pendingRequests"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "activeConsents"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "revokedConsents"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "requestHistory"
                        )
                );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCannotOpenPatientProviderAccessInbox()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/consents"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void unauthenticatedUserCannotOpenPatientProviderAccessInbox()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/consents"
                        )
                )
                .andExpect(
                        status().is3xxRedirection()
                );
    }

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCanApproveProviderAccessRequest()
            throws Exception {

        Long requestId = 601L;

        mockMvc.perform(
                        post(
                                "/patient/consents/access-requests/"
                                + requestId
                                + "/approve"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/patient/consents"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "successMessage",
                                "Provider access request approved."
                        )
                );

        verify(
                requestService
        ).approveRequest(
                requestId
        );
    }

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCanRejectProviderAccessRequest()
            throws Exception {

        Long requestId = 602L;

        mockMvc.perform(
                        post(
                                "/patient/consents/access-requests/"
                                + requestId
                                + "/reject"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/patient/consents"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "successMessage",
                                "Provider access request rejected."
                        )
                );

        verify(
                requestService
        ).rejectRequest(
                requestId
        );
    }

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCanRevokeActiveProviderConsent()
            throws Exception {

        Long providerId = 201L;

        when(
                currentUserService.getCurrentUser()
        ).thenReturn(
                patient
        );

        mockMvc.perform(
                        post(
                                "/patient/consents/"
                                + providerId
                                + "/revoke"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/patient/consents"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "successMessage",
                                "Provider access revoked."
                        )
                );

        verify(
                consentService
        ).revokeConsent(
                patient.getId(),
                providerId
        );
    }


    /* =========================================================
       LEGACY DIRECT-GRANT PROTECTION
       ========================================================= */

    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void legacyDirectGrantRouteCannotActivateConsent()
            throws Exception {

        Long providerId = 201L;

        mockMvc.perform(
                        post(
                                "/patient/consents/"
                                + providerId
                                + "/grant"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/patient/consents"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "errorMessage",
                                "Direct access granting is no longer available. "
                                + "The provider must submit an access request first."
                        )
                );

        /*
         * This is the critical assertion:
         * the legacy endpoint must never activate Consent.
         */
        verify(
                consentService,
                never()
        ).grantConsent(
                anyLong(),
                anyLong()
        );
    }

    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCannotUseLegacyPatientGrantRoute()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/patient/consents/201/grant"
                        )
                                .with(
                                        csrf()
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                consentService,
                never()
        ).grantConsent(
                anyLong(),
                anyLong()
        );
    }
}