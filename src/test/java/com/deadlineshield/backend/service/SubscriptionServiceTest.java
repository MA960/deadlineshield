package com.deadlineshield.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.deadlineshield.backend.model.Organization;
import com.deadlineshield.backend.repository.OrganizationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SubscriptionServiceTest {

    private final OrganizationRepository organizationRepository = Mockito.mock(OrganizationRepository.class);
    private final SubscriptionService subscriptionService =
            new SubscriptionService(organizationRepository, new ObjectMapper(), "", "http://localhost:3000");

    @Test
    void activatesOrganizationForSuccessfulInvoicePayment() {
        Organization organization = new Organization();
        organization.setStripeCustomerId("cus_test");
        when(organizationRepository.findByStripeCustomerId("cus_test")).thenReturn(Optional.of(organization));
        when(organizationRepository.save(organization)).thenReturn(organization);
        String payload = """
                {"data":{"object":{"customer":"cus_test","subscription":"sub_test"}}}
                """;

        subscriptionService.handleInvoicePaymentSucceeded(payload);

        assertEquals("ACTIVE", organization.getSubscriptionStatus());
        assertEquals("sub_test", organization.getStripeSubscriptionId());
        verify(organizationRepository).save(organization);
    }

    @Test
    void returnsServiceUnavailableWhenStripeCheckoutIsNotConfigured() {
        UUID organizationId = UUID.randomUUID();
        when(organizationRepository.findById(organizationId)).thenReturn(Optional.of(new Organization()));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> subscriptionService.createCheckoutSession(organizationId));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
    }
}
