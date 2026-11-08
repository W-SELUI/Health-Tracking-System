package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.user.dto.ProfileUpdateRequest;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.CurrentUserService;
import com.group15.health_tracking_system.user.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @PostMapping("/cover-photo")
    public String updateCoverPhoto(
            @RequestParam("coverPhoto") MultipartFile coverPhoto,
            RedirectAttributes redirectAttributes) {

        User currentUser = currentUserService.getCurrentUser();

        try {
            profileService.updateCoverPhoto(currentUser, coverPhoto);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "coverError",
                    exception.getMessage()
            );
            return "redirect:/profile";
        }

        return "redirect:/profile?coverUpdated";
    }

    @PostMapping("/cover-photo/remove")
    public String removeCoverPhoto() {

        profileService.removeCoverPhoto(
                currentUserService.getCurrentUser()
        );

        return "redirect:/profile?coverRemoved";
    }

    @GetMapping("/cover-photo")
    @ResponseBody
    public ResponseEntity<byte[]> viewCoverPhoto() {

        User currentUser = currentUserService.getCurrentUser();

        if (!currentUser.isCoverPhotoPresent()) {
            // No photo yet: send the browser to the default avatar instead
            // of a 404, so the sidebar does not log an error on every page.
            return ResponseEntity.status(org.springframework.http.HttpStatus.FOUND)
                    .location(java.net.URI.create("/provider-access/assets/profile-avatar.svg"))
                    .build();
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(
                        currentUser.getCoverPhotoContentType()
                ))
                .body(currentUser.getCoverPhotoData());
    }
}
