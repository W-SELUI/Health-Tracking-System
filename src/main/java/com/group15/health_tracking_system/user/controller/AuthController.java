package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.common.exception.DuplicateResourceException;
import com.group15.health_tracking_system.user.dto.RegistrationRequest;
import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationRequest", new RegistrationRequest());
        model.addAttribute("registrationRoles", List.of(Role.PATIENT, Role.PROVIDER));
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationRequest") RegistrationRequest request,
            BindingResult bindingResult,
            Model model) {

        model.addAttribute("registrationRoles", List.of(Role.PATIENT, Role.PROVIDER));

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            bindingResult.rejectValue(
                    "confirmPassword",
                    "password.mismatch",
                    "Passwords do not match"
            );
        }

        if (request.getRole() == Role.ADMIN) {
            bindingResult.rejectValue(
                    "role",
                    "role.invalid",
                    "Administrator accounts cannot be self-registered"
            );
        }

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(request);
        } catch (DuplicateResourceException exception) {
            bindingResult.rejectValue(
                    "email",
                    "email.duplicate",
                    exception.getMessage()
            );
            return "auth/register";
        }

        return "redirect:/login?registered";
    }
}