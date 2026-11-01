package com.deadlineshield.backend.service;

import com.deadlineshield.backend.dto.CheckoutSessionResponse;
import com.deadlineshield.backend.model.Organization;
import com.deadlineshield.backend.repository.OrganizationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionService.class);
    private static final long MONTHLY_PRICE_CENTS = 4_900L;

    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;
    private final String stripeApiKey;
    private final String frontendUrl;

    public SubscriptionService(
            OrganizationRepository organizationRepository,
            ObjectMapper objectMapper,
            @Value("${stripe.api-key:}") String stripeApiKey,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.organizationRepository = organizationRepository;
        this.objectMapper = objectMapper;
        this.stripeApiKey = stripeApiKey;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    }

    @Transactional
    public CheckoutSessionResponse createCheckoutSession(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        StripeClient stripe = stripeClient();
        try {
            String customerId = organization.getStripeCustomerId();
            if (customerId == null || customerId.isBlank()) {
                Customer customer = stripe.customers().create(CustomerCreateParams.builder()
                        .setName(organization.getName())
                        .putMetadata("organizationId", organization.getId().toString())
                        .build());
                customerId = customer.getId();
                organization.setStripeCustomerId(customerId);
                organizationRepository.save(organization);
            }

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setClientReferenceId(organization.getId().toString())
                    .setSuccessUrl(frontendUrl + "/?billing=success&session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(frontendUrl + "/?billing=cancelled")
                    .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                            .putMetadata("organizationId", organization.getId().toString())
                            .build())
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(MONTHLY_PRICE_CENTS)
                                    .setRecurring(SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                            .build())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("DeadlineShield Pro")
                                            .build())
                                    .build())
                            .build())
                    .build();
            Session session = stripe.checkout().sessions().create(params);
            if (session.getUrl() == null || session.getUrl().isBlank()) {
                throw new IllegalStateException("Stripe did not return a checkout URL");
            }
            return new CheckoutSessionResponse(session.getUrl());
        } catch (StripeException exception) {
            logger.error("Stripe checkout session creation failed for organization {}", organizationId, exception);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Unable to create a Stripe checkout session");
        }
    }

    @Transactional
    public void handleInvoicePaymentSucceeded(String signedPayload) {
        try {
            JsonNode invoice = objectMapper.readTree(signedPayload).path("data").path("object");
            String customerId = invoice.path("customer").asText(null);
            String subscriptionId = invoice.path("subscription").asText(null);
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException("Invoice event is missing its Stripe customer ID");
            }

            Organization organization = organizationRepository.findByStripeCustomerId(customerId).orElse(null);
            if (organization == null) {
                logger.warn("Received successful Stripe invoice for an unrecognized customer");
                return;
            }
            organization.setSubscriptionStatus("ACTIVE");
            if (subscriptionId != null && !subscriptionId.isBlank()) {
                organization.setStripeSubscriptionId(subscriptionId);
            }
            organizationRepository.save(organization);
            logger.info("Marked subscription active for organization {}", organization.getId());
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Unable to read Stripe invoice event", exception);
        }
    }

    private StripeClient stripeClient() {
        if (stripeApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe billing is not configured");
        }
        return new StripeClient(stripeApiKey);
    }
}
