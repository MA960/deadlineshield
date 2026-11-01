package com.deadlineshield.backend.controller;

import com.deadlineshield.backend.dto.CheckoutSessionResponse;
import com.deadlineshield.backend.dto.CreateCheckoutRequest;
import com.deadlineshield.backend.service.SubscriptionService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private static final Logger logger = LoggerFactory.getLogger(BillingController.class);

    private final SubscriptionService subscriptionService;
    private final String webhookSecret;

    public BillingController(
            SubscriptionService subscriptionService,
            @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.subscriptionService = subscriptionService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutRequest request) {
        return ResponseEntity.ok(subscriptionService.createCheckoutSession(request.organizationId()));
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature) {
        if (webhookSecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook is not configured");
        }
        if (signature == null || signature.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
        } catch (SignatureVerificationException | RuntimeException exception) {
            logger.warn("Rejected Stripe webhook with invalid signature or payload");
            return ResponseEntity.badRequest().build();
        }

        if ("invoice.payment_succeeded".equals(event.getType())) {
            subscriptionService.handleInvoicePaymentSucceeded(payload);
        }
        return ResponseEntity.ok().build();
    }
}
