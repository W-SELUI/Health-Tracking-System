package com.group15.health_tracking_system.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/",
                                "/login",
                                "/register",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()

                        // Patient-only features
                        .requestMatchers(
                                "/activities",
                                "/activities/**",
                                "/patient/**",
                                "/appointments",
                                "/appointments/**",
                                "/medications",
                                "/medications/**",
                                "/schedules",
                                "/schedules/**",
                                "/dose-records",
                                "/dose-records/**",
                                "/adherence",
                                "/adherence/**",
                                "/medication-dashboard",
                                "/medication-dashboard/**"
                        ).hasRole("PATIENT")

                        // Provider-only features
                        .requestMatchers("/provider/**")
                        .hasRole("PROVIDER")

                        // Admin-only features
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        // Any other page requires login
                        .anyRequest()
                        .authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )

                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                );

        return http.build();
    }
}