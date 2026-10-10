package com.group15.health_tracking_system.health.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.health.entity.HealthMetricType;
import com.group15.health_tracking_system.health.entity.HealthRecord;
import com.group15.health_tracking_system.health.entity.HealthRecordSource;
import com.group15.health_tracking_system.health.repository.HealthRecordRepository;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class HealthRecordQueryControllerIntegrationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthRecordRepository healthRecordRepository;

    private MockMvc mockMvc;

    private User patientOne;
    private User patientTwo;
    private User provider;

    @BeforeEach
    void setUp() {

        healthRecordRepository.deleteAll();
        userRepository.deleteAll();

        patientOne = saveUser(
                "Patient",
                "One",
                "patient1@example.com",
                Role.PATIENT
        );

        patientTwo = saveUser(
                "Patient",
                "Two",
                "patient2@example.com",
                Role.PATIENT
        );

        provider = saveUser(
                "Provider",
                "User",
                "provider@example.com",
                Role.PROVIDER
        );

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedUserCannotViewHealthRecords() throws Exception {

        mockMvc.perform(
                        get("/patient/health-records")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void providerCannotUsePatientHealthRecordHistory() throws Exception {

        mockMvc.perform(
                        get("/patient/health-records")
                                .with(user(provider.getEmail())
                                        .roles(Role.PROVIDER.name()))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void patientHistoryShowsOnlyOwnRecords() throws Exception {

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "72",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        saveRecord(
                patientTwo,
                HealthMetricType.HEART_RATE,
                "90",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        MvcResult result = mockMvc.perform(
                        get("/patient/health-records")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/list"))
                .andExpect(model().attributeExists("records"))
                .andExpect(model().attributeExists("metricTypes"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<HealthRecord> records =
                (List<HealthRecord>) result
                        .getModelAndView()
                        .getModel()
                        .get("records");

        assertEquals(2, records.size());

        assertTrue(
                records.stream().allMatch(
                        record -> record.getPatient()
                                .getId()
                                .equals(patientOne.getId())
                )
        );
    }

    @Test
    void patientHistoryCanBeFilteredByMetric() throws Exception {

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "72",
                LocalDateTime.of(2026, 10, 9, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75",
                LocalDateTime.of(2026, 10, 9, 10, 0)
        );

        MvcResult result = mockMvc.perform(
                        get("/patient/health-records")
                                .param("metricType", "WEIGHT")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/list"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<HealthRecord> records =
                (List<HealthRecord>) result
                        .getModelAndView()
                        .getModel()
                        .get("records");

        assertEquals(1, records.size());

        assertEquals(
                HealthMetricType.WEIGHT,
                records.get(0).getMetricType()
        );
    }

    @Test
    void patientHistoryCanBeFilteredByDateRange() throws Exception {

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "68",
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "74",
                LocalDateTime.of(2026, 10, 5, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "80",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        MvcResult result = mockMvc.perform(
                        get("/patient/health-records")
                                .param("startDate", "2026-10-04")
                                .param("endDate", "2026-10-06")
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/list"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<HealthRecord> records =
                (List<HealthRecord>) result
                        .getModelAndView()
                        .getModel()
                        .get("records");

        assertEquals(1, records.size());

        assertEquals(
                0,
                records.get(0)
                        .getMetricValue()
                        .compareTo(new BigDecimal("74"))
        );
    }

    @Test
    void metricAndDateFiltersCanBeCombined() throws Exception {

        saveRecord(
                patientOne,
                HealthMetricType.HEART_RATE,
                "74",
                LocalDateTime.of(2026, 10, 5, 9, 0)
        );

        saveRecord(
                patientOne,
                HealthMetricType.WEIGHT,
                "75",
                LocalDateTime.of(2026, 10, 5, 10, 0)
        );

        MvcResult result = mockMvc.perform(
                        get("/patient/health-records")
                                .param(
                                        "metricType",
                                        "HEART_RATE"
                                )
                                .param(
                                        "startDate",
                                        "2026-10-04"
                                )
                                .param(
                                        "endDate",
                                        "2026-10-06"
                                )
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/list"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<HealthRecord> records =
                (List<HealthRecord>) result
                        .getModelAndView()
                        .getModel()
                        .get("records");

        assertEquals(1, records.size());

        assertEquals(
                HealthMetricType.HEART_RATE,
                records.get(0).getMetricType()
        );
    }

    @Test
    void incompleteDateRangeShowsValidationMessage() throws Exception {

        mockMvc.perform(
                        get("/patient/health-records")
                                .param(
                                        "startDate",
                                        "2026-10-01"
                                )
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/list"))
                .andExpect(
                        model().attributeExists("filterError")
                )
                .andExpect(
                        model().attributeExists("records")
                );
    }

    @Test
    void patientCanViewOwnHealthRecord() throws Exception {

        HealthRecord record = saveRecord(
                patientOne,
                HealthMetricType.OXYGEN_SATURATION,
                "98",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        mockMvc.perform(
                        get(
                                "/patient/health-records/"
                                + record.getId()
                        )
                                .with(user(patientOne.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("health-records/view"))
                .andExpect(model().attributeExists("record"));
    }

    private User saveUser(
            String firstName,
            String lastName,
            String email,
            Role role) {

        return userRepository.save(
                new User(
                        firstName,
                        lastName,
                        email,
                        "test-password-hash",
                        role
                )
        );
    }

    private HealthRecord saveRecord(
            User patient,
            HealthMetricType metricType,
            String value,
            LocalDateTime observedAt) {

        return healthRecordRepository.save(
                new HealthRecord(
                        patient,
                        patient,
                        metricType,
                        new BigDecimal(value),
                        observedAt,
                        null,
                        HealthRecordSource.PATIENT_REPORTED
                )
        );
    }
}