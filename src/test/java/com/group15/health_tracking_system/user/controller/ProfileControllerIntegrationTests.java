package com.group15.health_tracking_system.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
class ProfileControllerIntegrationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    private User patient;
    private User provider;
    private User admin;

    @BeforeEach
    void setUp() {

        userRepository.deleteAll();

        patient = saveUser(
                "Patient",
                "User",
                "patient@example.com",
                Role.PATIENT
        );

        provider = saveUser(
                "Provider",
                "User",
                "provider@example.com",
                Role.PROVIDER
        );

        admin = saveUser(
                "Admin",
                "User",
                "admin@example.com",
                Role.ADMIN
        );

        mockMvc = MockMvcBuilders
                .webAppContextSetup(applicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedUserCannotOpenProfile() throws Exception {

        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void patientCanViewOwnProfile() throws Exception {

        mockMvc.perform(
                        get("/profile")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"))
                .andExpect(model().attributeExists("currentUser"));
    }

    @Test
    void providerAndAdministratorCanViewTheirProfiles() throws Exception {

        mockMvc.perform(
                        get("/profile")
                                .with(user(provider.getEmail())
                                        .roles(Role.PROVIDER.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"));

        mockMvc.perform(
                        get("/profile")
                                .with(user(admin.getEmail())
                                        .roles(Role.ADMIN.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("profile/view"));
    }

    @Test
    void editPageLoadsCurrentProfileInformation() throws Exception {

        mockMvc.perform(
                        get("/profile/edit")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("profileUpdateRequest"));
    }

    @Test
    void validProfileUpdatePersistsAndRedirects() throws Exception {

        mockMvc.perform(
                        post("/profile/edit")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("firstName", "Updated")
                                .param("lastName", "Patient")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated"));

        User persisted = userRepository
                .findByEmailIgnoreCase("patient@example.com")
                .orElseThrow();

        assertEquals("Updated", persisted.getFirstName());
        assertEquals("Patient", persisted.getLastName());

        // These fields must remain unchanged.
        assertEquals("patient@example.com", persisted.getEmail());
        assertEquals(Role.PATIENT, persisted.getRole());
    }

    @Test
    void invalidProfileUpdateReturnsToEditPage() throws Exception {

        mockMvc.perform(
                        post("/profile/edit")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                                .param("firstName", "")
                                .param("lastName", "Patient")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("profile/edit"))
                .andExpect(
                        model().attributeHasFieldErrors(
                                "profileUpdateRequest",
                                "firstName"
                        )
                );

        User persisted = userRepository
                .findByEmailIgnoreCase("patient@example.com")
                .orElseThrow();

        assertEquals("Patient", persisted.getFirstName());
        assertEquals("User", persisted.getLastName());
    }

    @Test
    void everyAccountRoleCanUploadACoverPhoto() throws Exception {

        byte[] imageBytes = new byte[]{1, 2, 3, 4, 5};

        for (User account : java.util.List.of(patient, provider, admin)) {
            MockMultipartFile coverPhoto = new MockMultipartFile(
                    "coverPhoto",
                    "cover.png",
                    MediaType.IMAGE_PNG_VALUE,
                    imageBytes
            );

            mockMvc.perform(
                            multipart("/profile/cover-photo")
                                    .file(coverPhoto)
                                    .with(user(account.getEmail())
                                            .roles(account.getRole().name()))
                                    .with(csrf())
                    )
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/profile?coverUpdated"));

            User persisted = userRepository
                    .findByEmailIgnoreCase(account.getEmail())
                    .orElseThrow();

            assertArrayEquals(imageBytes, persisted.getCoverPhotoData());
            assertEquals(
                    MediaType.IMAGE_PNG_VALUE,
                    persisted.getCoverPhotoContentType()
            );
        }
    }

    @Test
    void uploadedCoverPhotoCanBeDisplayed() throws Exception {

        byte[] imageBytes = new byte[]{7, 8, 9};
        patient.setCoverPhotoData(imageBytes);
        patient.setCoverPhotoContentType(MediaType.IMAGE_JPEG_VALUE);
        userRepository.save(patient);

        mockMvc.perform(
                        get("/profile/cover-photo")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(imageBytes));
    }

    @Test
    void nonImageCoverUploadReturnsAHelpfulError() throws Exception {

        MockMultipartFile textFile = new MockMultipartFile(
                "coverPhoto",
                "notes.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes()
        );

        mockMvc.perform(
                        multipart("/profile/cover-photo")
                                .file(textFile)
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attributeExists("coverError"));
    }

    @Test
    void userCanRemoveTheirCoverPhoto() throws Exception {

        patient.setCoverPhotoData(new byte[]{4, 3, 2, 1});
        patient.setCoverPhotoContentType("image/webp");
        userRepository.save(patient);

        mockMvc.perform(
                        post("/profile/cover-photo/remove")
                                .with(user(patient.getEmail())
                                        .roles(Role.PATIENT.name()))
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?coverRemoved"));

        User persisted = userRepository
                .findByEmailIgnoreCase(patient.getEmail())
                .orElseThrow();

        assertFalse(persisted.isCoverPhotoPresent());
    }

    private User saveUser(
            String firstName,
            String lastName,
            String email,
            Role role) {

        User user = new User(
                firstName,
                lastName,
                email,
                passwordEncoder.encode("password123"),
                role
        );

        return userRepository.save(user);
    }
}
