package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.user.entity.ProviderVerification;
import com.group15.health_tracking_system.user.service.ProviderVerificationService;

import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/provider-verifications")
public class AdminProviderVerificationController {

    private final ProviderVerificationService verificationService;

    public AdminProviderVerificationController(
            ProviderVerificationService verificationService) {

        this.verificationService =
                verificationService;
    }

    /*
     * List all provider verification records.
     */
    @GetMapping
    public String listVerifications(
            Model model) {

        model.addAttribute(
                "verifications",
                verificationService.getAll());

        return "admin/provider-verifications";
    }

    /*
     * View one provider verification.
     */
    @GetMapping("/{id}")
    public String viewVerification(
            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {

            ProviderVerification verification =
                    verificationService.getById(id);

            model.addAttribute(
                    "verification",
                    verification);

            return "admin/provider-verification-details";

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

            return "redirect:/admin/provider-verifications";
        }
    }

    /*
     * Open the uploaded provider licence.
     */
    @GetMapping("/{id}/licence")
    public ResponseEntity<Resource> viewLicence(
            @PathVariable Long id) {

        ProviderVerification verification =
                verificationService.getById(id);

        Resource resource =
                verificationService
                        .loadLicenceFile(id);

        String contentType =
                verification.getFileType();

        if (contentType == null
                || contentType.isBlank()) {

            contentType =
                    MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                contentType))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(
                                        verification
                                                .getOriginalFileName())
                                .build()
                                .toString())
                .body(resource);
    }

    /*
     * Verify provider licence.
     */
    @PostMapping("/{id}/verify")
    public String verify(
            @PathVariable Long id,
            @RequestParam(
                    value = "adminNote",
                    required = false)
            String adminNote,
            RedirectAttributes redirectAttributes) {

        try {

            verificationService
                    .verifyProvider(
                            id,
                            adminNote);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Provider licence verified successfully.");

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/admin/provider-verifications/"
                + id;
    }

    /*
     * Reject provider licence.
     */
    @PostMapping("/{id}/reject")
    public String reject(
            @PathVariable Long id,
            @RequestParam(
                    value = "adminNote",
                    required = false)
            String adminNote,
            RedirectAttributes redirectAttributes) {

        try {

            verificationService
                    .rejectProvider(
                            id,
                            adminNote);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Provider licence rejected.");

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/admin/provider-verifications/"
                + id;
    }
}