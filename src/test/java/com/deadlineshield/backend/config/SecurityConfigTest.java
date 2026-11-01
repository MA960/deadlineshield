package com.deadlineshield.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void restrictsCorsToTheConfiguredLocalDashboardOrigin() {
        var request = new MockHttpServletRequest("GET", "/api/projects");
        var cors = securityConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertEquals(List.of("http://localhost:3000"), cors.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), cors.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type", "Accept"), cors.getAllowedHeaders());
        assertTrue(cors.getAllowCredentials());
    }

    @Test
    void configuresAgencyCredentialsWithHashedPasswordAndRole() {
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();
        var users = securityConfig.userDetailsService("agency", "local-test-password-123", passwordEncoder);
        var agency = users.loadUserByUsername("agency");

        assertTrue(passwordEncoder.matches("local-test-password-123", agency.getPassword()));
        assertTrue(agency.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_AGENCY")));
    }
}
