package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.common.exception.DuplicateResourceException;
import com.group15.health_tracking_system.user.dto.RegistrationRequest;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.ProviderVerificationService;
import com.group15.health_tracking_system.user.service.UserService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class AuthController {

    private final UserService userService;
    private final ProviderVerificationService verificationService;

    public AuthController(
            UserService userService,
            ProviderVerificationService verificationService) {

        this.userService = userService;
        this.verificationService = verificationService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(
            Model model) {

        model.addAttribute(
                "registrationRequest",
                new RegistrationRequest());

        model.addAttribute(
                "registrationRoles",
                List.of(
                        Role.PATIENT,
                        Role.PROVIDER));

        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid
            @ModelAttribute("registrationRequest")
            RegistrationRequest request,

            BindingResult bindingResult,

            @RequestParam(
                    value = "licenceFile",
                    required = false)
            MultipartFile licenceFile,

            Model model) {

        model.addAttribute(
                "registrationRoles",
                List.of(
                        Role.PATIENT,
                        Role.PROVIDER));

        /*
         * Check that the passwords match.
         */
        if (request.getPassword() != null
                && request.getConfirmPassword() != null
                && !request.getPassword()
                        .equals(
                                request.getConfirmPassword())) {

            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Passwords do not match"
            );
        }

        /*
         * ADMIN accounts cannot be created
         * through public registration.
         */
        if (request.getRole()
                == Role.ADMIN) {

            bindingResult.rejectValue(
                    "role",
                    "role.invalid",
                    "Administrator accounts cannot be self-registered"
            );
        }

        /*
         * Healthcare providers must submit
         * a licence when registering.
         */
        if (request.getRole()
                == Role.PROVIDER) {

            if (licenceFile == null
                    || licenceFile.isEmpty()) {

                model.addAttribute(
                        "licenceError",
                        "Healthcare providers must upload a licence.");

                return "auth/register";
            }

            String fileName =
                    licenceFile.getOriginalFilename();

            if (fileName == null
                    || fileName.isBlank()) {

                model.addAttribute(
                        "licenceError",
                        "The uploaded licence must have a file name.");

                return "auth/register";
            }

            String lowerFileName =
                    fileName.toLowerCase();

            boolean validFile =
                    lowerFileName.endsWith(".pdf")
                    || lowerFileName.endsWith(".jpg")
                    || lowerFileName.endsWith(".jpeg")
                    || lowerFileName.endsWith(".png");

            if (!validFile) {

                model.addAttribute(
                        "licenceError",
                        "Only PDF, JPG, JPEG and PNG licence files are allowed.");

                return "auth/register";
            }

            long maximumSize =
                    10L * 1024L * 1024L;

            if (licenceFile.getSize()
                    > maximumSize) {

                model.addAttribute(
                        "licenceError",
                        "Licence file must not exceed 10 MB.");

                return "auth/register";
            }
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {

            /*
             * UserService automatically disables
             * provider accounts at registration.
             */
            User registeredUser =
                    userService.register(
                            request);

            /*
             * Providers immediately receive a
             * PENDING verification record.
             */
            if (request.getRole()
                    == Role.PROVIDER) {

                verificationService
                        .uploadLicence(
                                registeredUser,
                                licenceFile);

                return "redirect:/login?verificationPending";
            }

        } catch (DuplicateResourceException exception) {

            bindingResult.rejectValue(
                    "email",
                    "email.duplicate",
                    exception.getMessage()
            );

            return "auth/register";

        } catch (IllegalArgumentException
                | IllegalStateException exception) {

            model.addAttribute(
                    "licenceError",
                    exception.getMessage());

            return "auth/register";
        }

        /*
         * Patients can log in immediately.
         */
        return "redirect:/login?registered";
    }
}