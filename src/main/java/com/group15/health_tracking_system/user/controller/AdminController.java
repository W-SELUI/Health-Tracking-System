package com.group15.health_tracking_system.user.controller;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.service.AdminService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(
            AdminService adminService) {

        this.adminService =
                adminService;
    }

    /*
     * Manage all users.
     */
    @GetMapping("/users")
    public String users(
            Model model) {

        model.addAttribute(
                "users",
                adminService.getAllUsers());

        return "admin/users";
    }

    /*
     * Manage providers.
     */
    @GetMapping("/providers")
    public String providers(
            Model model) {

        model.addAttribute(
                "providers",
                adminService.getAllUsers()
                        .stream()
                        .filter(user ->
                                user.getRole()
                                        == Role.PROVIDER)
                        .toList());

        return "admin/providers";
    }

    /*
     * Enable account.
     */
    @PostMapping("/users/{id}/enable")
    public String enableUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            adminService.enableUser(
                    id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User account enabled successfully.");

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/admin/users";
    }

    /*
     * Disable account.
     */
    @PostMapping("/users/{id}/disable")
    public String disableUser(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            User user =
                    adminService.getUserById(
                            id);

            /*
             * Prevent disabling administrator accounts.
             */
            if (user.getRole()
                    == Role.ADMIN) {

                throw new IllegalArgumentException(
                        "Administrator accounts cannot be disabled here.");
            }

            adminService.disableUser(
                    id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "User account disabled successfully.");

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());
        }

        return "redirect:/admin/users";
    }
}