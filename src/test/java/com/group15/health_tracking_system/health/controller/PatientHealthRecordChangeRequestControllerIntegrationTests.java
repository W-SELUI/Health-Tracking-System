package com.group15.health_tracking_system.health.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequest;
import com.group15.health_tracking_system.health.entity.HealthRecordChangeRequestStatus;
import com.group15.health_tracking_system.health.service.HealthRecordChangeRequestService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for the Patient Health Record change-request
 * web workflow.
 *
 * 
 */
@SpringBootTest
@AutoConfigureMockMvc
class PatientHealthRecordChangeRequestControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthRecordChangeRequestService changeRequestService;


    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCanOpenHealthRecordChangeRequestInbox()
            throws Exception {

        List<HealthRecordChangeRequest> pending =
                List.of();

        List<HealthRecordChangeRequest> history =
                List.of();

        when(
                changeRequestService
                        .getPendingRequestsForCurrentPatient()
        ).thenReturn(
                pending
        );

        when(
                changeRequestService
                        .getRequestHistoryForCurrentPatient()
        ).thenReturn(
                history
        );

        mockMvc.perform(
                        get(
                                "/patient/health-record-change-requests"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        view().name(
                                "health-records/change-requests"
                        )
                )
                .andExpect(
                        model().attribute(
                                "pendingRequests",
                                pending
                        )
                )
                .andExpect(
                        model().attribute(
                                "requestHistory",
                                history
                        )
                );

        verify(
                changeRequestService
        ).getPendingRequestsForCurrentPatient();

        verify(
                changeRequestService
        ).getRequestHistoryForCurrentPatient();
    }


    @Test
    @WithMockUser(
            username = "provider@example.com",
            roles = "PROVIDER"
    )
    void providerCannotOpenPatientChangeRequestInbox()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/health-record-change-requests"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                changeRequestService,
                never()
        ).getPendingRequestsForCurrentPatient();
    }


    @Test
    void unauthenticatedUserCannotOpenPatientChangeRequestInbox()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/patient/health-record-change-requests"
                        )
                )
                .andExpect(
                        status().is3xxRedirection()
                );

        verify(
                changeRequestService,
                never()
        ).getPendingRequestsForCurrentPatient();
    }


    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void approvedRequestRedirectsWithApprovedMessage()
            throws Exception {

        HealthRecordChangeRequest request =
                org.mockito.Mockito.mock(
                        HealthRecordChangeRequest.class
                );

        when(
                request.getStatus()
        ).thenReturn(
                HealthRecordChangeRequestStatus.APPROVED
        );

        when(
                changeRequestService
                        .approveRequest(
                                101L
                        )
        ).thenReturn(
                request
        );

        mockMvc.perform(
                        post(
                                "/patient/health-record-change-requests/101/approve"
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
                                "/patient/health-record-change-requests?approved"
                        )
                );

        verify(
                changeRequestService
        ).approveRequest(
                101L
        );
    }


    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void staleRequestRedirectsWithStaleMessage()
            throws Exception {

        HealthRecordChangeRequest request =
                org.mockito.Mockito.mock(
                        HealthRecordChangeRequest.class
                );

        when(
                request.getStatus()
        ).thenReturn(
                HealthRecordChangeRequestStatus.STALE
        );

        when(
                changeRequestService
                        .approveRequest(
                                102L
                        )
        ).thenReturn(
                request
        );

        mockMvc.perform(
                        post(
                                "/patient/health-record-change-requests/102/approve"
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
                                "/patient/health-record-change-requests?stale"
                        )
                );

        verify(
                changeRequestService
        ).approveRequest(
                102L
        );
    }


    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void cancelledRequestRedirectsWithCancelledMessage()
            throws Exception {

        HealthRecordChangeRequest request =
                org.mockito.Mockito.mock(
                        HealthRecordChangeRequest.class
                );

        when(
                request.getStatus()
        ).thenReturn(
                HealthRecordChangeRequestStatus.CANCELLED
        );

        when(
                changeRequestService
                        .approveRequest(
                                103L
                        )
        ).thenReturn(
                request
        );

        mockMvc.perform(
                        post(
                                "/patient/health-record-change-requests/103/approve"
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
                                "/patient/health-record-change-requests?cancelled"
                        )
                );

        verify(
                changeRequestService
        ).approveRequest(
                103L
        );
    }


    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void patientCanRejectPendingRequest()
            throws Exception {

        HealthRecordChangeRequest request =
                org.mockito.Mockito.mock(
                        HealthRecordChangeRequest.class
                );

        when(
                changeRequestService
                        .rejectRequest(
                                104L
                        )
        ).thenReturn(
                request
        );

        mockMvc.perform(
                        post(
                                "/patient/health-record-change-requests/104/reject"
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
                                "/patient/health-record-change-requests?rejected"
                        )
                );

        verify(
                changeRequestService
        ).rejectRequest(
                104L
        );
    }


    @Test
    @WithMockUser(
            username = "patient@example.com",
            roles = "PATIENT"
    )
    void approvalPostWithoutCsrfIsRejected()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/patient/health-record-change-requests/105/approve"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                changeRequestService,
                never()
        ).approveRequest(
                105L
        );
    }
}