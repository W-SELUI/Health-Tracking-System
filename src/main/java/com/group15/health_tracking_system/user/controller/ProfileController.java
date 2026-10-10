package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.user.dto.ProfileUpdateRequest;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import com.group15.health_tracking_system.user.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Handles viewing and editing the authenticated user's profile.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final CurrentUserService currentUserService;
    private final ProfileService profileService;

    public ProfileController(
            CurrentUserService currentUserService,
            ProfileService profileService) {

        this.currentUserService = currentUserService;
        this.profileService = profileService;
    }

    @GetMapping
    public String viewProfile(Model model) {

        User currentUser = currentUserService.getCurrentUser();

        model.addAttribute("currentUser", currentUser);

        return "profile/view";
    }

    @GetMapping("/edit")
    public String editProfile(Model model) {

        User currentUser = currentUserService.getCurrentUser();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute(
                "profileUpdateRequest",
                profileService.createUpdateRequest(currentUser)
        );

        return "profile/edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
            @Valid
            @ModelAttribute("profileUpdateRequest")
            ProfileUpdateRequest request,
            BindingResult bindingResult,
            Model model) {

        User currentUser = currentUserService.getCurrentUser();

        if (bindingResult.hasErrors()) {
            model.addAttribute("currentUser", currentUser);
            return "profile/edit";
        }

        try {
            profileService.updateProfile(currentUser, request);
        } catch (IllegalArgumentException exception) {
            bindingResult.reject(
                    "profile.update",
                    exception.getMessage()
            );

            model.addAttribute("currentUser", currentUser);

            return "profile/edit";
        }

        return "redirect:/profile?updated";
    }
}