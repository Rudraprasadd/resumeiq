package com.resumeiq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeiq.model.Subscription;
import com.resumeiq.model.User;
import com.resumeiq.repository.SubscriptionRepository;
import com.resumeiq.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

/**
 * Handles all Razorpay payment operations.
 *
 * FLOW:
 * 1. Frontend calls POST /api/payments/create-order
 *    → We create a Razorpay order via their API
 *    → Return orderId + amount to frontend
 *
 * 2. Frontend shows Razorpay checkout popup
 *    → User pays
 *    → Razorpay calls our webhook POST /api/payments/webhook
 *
 * 3. Webhook handler verifies HMAC signature (prevents fraud)
 *    → Activates PRO plan for the user
 *
 * NEVER activate PRO based on what the frontend tells you.
 * ONLY trust the webhook after HMAC verification.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    // PRO plan price: ₹99/month = 9900 paise
    public static final int PRO_MONTHLY_PRICE_PAISE = 9900;

    private final WebClient razorpayClient;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;
    private final String keyId;

    public PaymentService(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpay.webhook-secret}") String webhookSecret,
            SubscriptionRepository subscriptionRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.keyId                  = keyId;
        this.webhookSecret          = webhookSecret;
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository         = userRepository;
        this.objectMapper           = objectMapper;

        // Basic auth: keyId:keySecret as base64
        String credentials = java.util.Base64.getEncoder()
                .encodeToString((keyId + ":" + keySecret).getBytes());

        this.razorpayClient = WebClient.builder()
                .baseUrl("https://api.razorpay.com/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
                .build();
    }

    // ----------------------------------------------------------------
    // STEP 1: Create Razorpay order
    // ----------------------------------------------------------------

    public Map<String, Object> createOrder(User user) {
        Map<String, Object> orderRequest = Map.of(
            "amount",   PRO_MONTHLY_PRICE_PAISE,
            "currency", "INR",
            "receipt",  "resumeiq_pro_" + user.getId().toString().substring(0, 8),
            "notes",    Map.of("userId", user.getId().toString(), "plan", "PRO")
        );

        try {
            String response = razorpayClient.post()
                    .uri("/orders")
                    .bodyValue(orderRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, Object> order = objectMapper.readValue(response, Map.class);
            log.info("Razorpay order created: {} for user {}", order.get("id"), user.getEmail());

            // Return what frontend needs to show the checkout
            return Map.of(
                "orderId",  order.get("id"),
                "amount",   PRO_MONTHLY_PRICE_PAISE,
                "currency", "INR",
                "keyId",    keyId
            );
        } catch (Exception e) {
            log.error("Failed to create Razorpay order: {}", e.getMessage());
            throw new RuntimeException("Payment service unavailable. Please try again.");
        }
    }

    // ----------------------------------------------------------------
    // STEP 2: Webhook — verify signature and activate PRO
    // ----------------------------------------------------------------

    @Transactional
    public void handleWebhook(String payload, String razorpaySignature) {
        // 1. Verify HMAC-SHA256 signature — NEVER skip this
        if (!isSignatureValid(payload, razorpaySignature)) {
            log.warn("Razorpay webhook: invalid signature — possible fraud attempt");
            throw new SecurityException("Invalid webhook signature");
        }

        try {
            Map<String, Object> event = objectMapper.readValue(payload, Map.class);
            String eventType = (String) event.get("event");
            log.info("Razorpay webhook event: {}", eventType);

            // Only act on successful payment
            if ("payment.captured".equals(eventType)) {
                handlePaymentCaptured(event);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage());
            throw new RuntimeException("Webhook processing failed");
        }
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void handlePaymentCaptured(Map<String, Object> event) {
        Map<String, Object> payload  = (Map<String, Object>) event.get("payload");
        Map<String, Object> payment  = (Map<String, Object>) payload.get("payment");
        Map<String, Object> entity   = (Map<String, Object>) payment.get("entity");

        String paymentId = (String) entity.get("id");
        String orderId   = (String) entity.get("order_id");
        Map<String, Object> notes = (Map<String, Object>) entity.get("notes");
        String userId    = (String) notes.get("userId");

        log.info("Payment captured: {} for user {}", paymentId, userId);

        User user = userRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Idempotency check — don't activate twice for same payment
        if (subscriptionRepository.findByRazorpayOrderId(orderId).isPresent()) {
            log.warn("Duplicate webhook for order {} — ignoring", orderId);
            return;
        }

        // Activate PRO
        user.setPlan(User.Plan.PRO);
        userRepository.save(user);

        // Save subscription record
        Subscription sub = new Subscription();
        sub.setUser(user);
        sub.setPlan(User.Plan.PRO);
        sub.setStatus(Subscription.Status.ACTIVE);
        sub.setRazorpayOrderId(orderId);
        sub.setRazorpayPaymentId(paymentId);
        sub.setAmountPaise(PRO_MONTHLY_PRICE_PAISE);
        sub.setStartsAt(LocalDateTime.now());
        sub.setExpiresAt(LocalDateTime.now().plusMonths(1));
        subscriptionRepository.save(sub);

        log.info("PRO plan activated for user {} until {}", user.getEmail(), sub.getExpiresAt());
    }

    /**
     * Validates Razorpay webhook signature.
     * Razorpay signs the raw payload with your webhook secret using HMAC-SHA256.
     * If signatures match → request is genuinely from Razorpay.
     */
    private boolean isSignatureValid(String payload, String signature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equals(signature);
        } catch (Exception e) {
            log.error("HMAC verification failed: {}", e.getMessage());
            return false;
        }
    }
}