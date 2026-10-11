package com.group15.health_tracking_system.user.service;

import com.group15.health_tracking_system.user.entity.Role;
import com.group15.health_tracking_system.user.entity.User;
import com.group15.health_tracking_system.user.repository.UserRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(
            UserRepository userRepository) {

        this.userRepository =
                userRepository;
    }

    /*
     * Get all users.
     */
    public List<User> getAllUsers() {

        return userRepository.findAll();
    }

    /*
     * Get all patients.
     */
    public List<User> getPatients() {

        return userRepository
                .findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(
                        Role.PATIENT);
    }

    /*
     * Get all enabled providers.
     */
    public List<User> getProviders() {

        return userRepository
                .findByRoleAndEnabledTrueOrderByFirstNameAscLastNameAsc(
                        Role.PROVIDER);
    }

    /*
     * Get one user by ID.
     */
    public User getUserById(
            Long id) {

        return userRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found."));
    }

    /*
     * Enable user account.
     */
    public User enableUser(
            Long id) {

        User user =
                getUserById(id);

        user.setEnabled(
                true);

        return userRepository.save(
                user);
    }

    /*
     * Disable user account.
     */
    public User disableUser(
            Long id) {

        User user =
                getUserById(id);

        user.setEnabled(
                false);

        return userRepository.save(
                user);
    }

    public long countUsers() {

        return userRepository.count();
    }

    public long countPatients() {

        return userRepository.findAll()
                .stream()
                .filter(user ->
                        user.getRole()
                                == Role.PATIENT)
                .count();
    }

    public long countProviders() {

        return userRepository.findAll()
                .stream()
                .filter(user ->
                        user.getRole()
                                == Role.PROVIDER)
                .count();
    }

    public long countEnabledUsers() {

        return userRepository.findAll()
                .stream()
                .filter(
                        User::isEnabled)
                .count();
    }

    public long countDisabledUsers() {

        return userRepository.findAll()
                .stream()
                .filter(user ->
                        !user.isEnabled())
                .count();
    }
}