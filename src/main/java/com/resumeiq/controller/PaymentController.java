package com.resumeiq.controller;

import com.resumeiq.model.User;
import com.resumeiq.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Razorpay payment integration for PRO plan")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Step 1: Frontend calls this to get a Razorpay order.
     * Returns orderId + amount + keyId needed to open the Razorpay checkout popup.
     * Requires JWT authentication.
     */
    @PostMapping("/create-order")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Create a Razorpay order for PRO plan (₹99/month)",
        description = "Returns orderId and keyId needed to open Razorpay checkout on the frontend."
    )
    public ResponseEntity<Map<String, Object>> createOrder(@AuthenticationPrincipal User user) {
        Map<String, Object> order = paymentService.createOrder(user);
        return ResponseEntity.ok(order);
    }

    /**
     * Step 2: Razorpay calls this webhook after payment is captured.
     * This endpoint is PUBLIC (no JWT) — Razorpay calls it, not the user.
     * Security is enforced via HMAC-SHA256 signature verification inside PaymentService.
     *
     * Already permitted in SecurityConfig: .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
     */
    @PostMapping("/webhook")
    @Operation(
        summary = "Razorpay webhook — do not call this manually",
        description = "Called by Razorpay after payment. Verifies HMAC signature and activates PRO plan."
    )
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("x-razorpay-signature") String signature) {

        log.info("Razorpay webhook received");
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }
}