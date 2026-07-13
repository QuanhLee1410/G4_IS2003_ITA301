package com.edunexus.controller;

import com.edunexus.dto.TransactionDTO;
import com.edunexus.security.UserPrincipal;
import com.edunexus.service.PaymentService;
import com.edunexus.service.payment.SePayService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Slf4j
@Tag(name = "Payments", description = "Payment gateway integration endpoints")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SePayService sePayService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping("/init")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Initiate payment", description = "Create a payment transaction for a course")
    public ResponseEntity<TransactionDTO> initiatePayment(@RequestParam Integer courseId) {
        UserPrincipal user = getCurrentUser();
        log.info("Payment initialization for student: {} course: {}", user.getUserId(), courseId);
        TransactionDTO transaction = paymentService.initiatePayment(user.getUserId(), courseId);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    @GetMapping("/vnpay-url/{transactionId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Generate VNPay URL", description = "Generate payment redirect URL for VNPay")
    public ResponseEntity<?> generateVNPayUrl(@PathVariable Integer transactionId) {
        log.info("Generating VNPay URL for transaction: {}", transactionId);
        try {
            String paymentUrl = paymentService.generateVNPayPaymentUrl(transactionId);
            return ResponseEntity.ok(Map.of("paymentUrl", paymentUrl));
        } catch (Exception e) {
            log.error("Error generating VNPay URL", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{transactionId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get transaction status", description = "Check payment transaction status")
    public ResponseEntity<TransactionDTO> getTransactionStatus(@PathVariable Integer transactionId) {
        log.info("Getting transaction status: {}", transactionId);
        TransactionDTO transaction = paymentService.getTransactionById(transactionId);
        return new ResponseEntity<>(transaction, HttpStatus.OK);
    }

    @GetMapping("/student/transactions")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get my transactions", description = "List all payment transactions for the current student")
    public ResponseEntity<List<TransactionDTO>> getMyTransactions() {
        UserPrincipal user = getCurrentUser();
        log.info("Getting transactions for student: {}", user.getUserId());
        List<TransactionDTO> transactions = paymentService.getStudentTransactions(user.getUserId());
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @PostMapping("/vnpay-webhook")
    @Operation(summary = "VNPay webhook", description = "Webhook endpoint for VNPay payment notifications (IPN)")
    public ResponseEntity<?> handleVNPayWebhook(@RequestParam Map<String, String> params) {
        String transactionCode = params.get("vnp_TxnRef");
        log.info("VNPay webhook received for transaction: {}", transactionCode);

        try {
            paymentService.handleVNPaySuccess(params);
            return ResponseEntity.ok(Map.of("code", "00", "message", "Payment notification received successfully"));
        } catch (Exception e) {
            log.error("Error processing VNPay webhook for transaction: {}", transactionCode, e);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("code", "97", "message", e.getMessage()));
        }
    }

    @PostMapping("/sepay-webhook")
    @Operation(summary = "SePay webhook", description = "Webhook endpoint for SePay bank transfer notifications")
    public ResponseEntity<?> handleSePayWebhook(
            @RequestHeader(value = "X-Signature", required = false) String signature,
            @RequestBody String payload) {
        log.info("SePay webhook received with signature: {}", signature != null ? "present" : "missing");

        try {
            // Verify signature if provided
            if (signature != null && !signature.isEmpty()) {
                if (!sePayService.verifyWebhookSignature(signature, payload)) {
                    log.warn("SePay webhook signature verification failed");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("code", "02", "message", "Invalid signature"));
                }
            }

            // Parse JSON payload
            JsonNode jsonPayload = objectMapper.readTree(payload);
            String description = jsonPayload.get("description").asText();
            String status = jsonPayload.get("status").asText();
            long amount = jsonPayload.get("amount").asLong();

            log.info("SePay transfer: code={}, amount={}, status={}", description, amount, status);

            paymentService.handleSePaySuccess(description, new BigDecimal(amount), status);
            return ResponseEntity.ok(Map.of("code", "00", "message", "Transfer notification received successfully"));
        } catch (IOException e) {
            log.error("Error parsing SePay webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "01", "message", "Invalid JSON payload"));
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(Map.of("code", "97", "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "Payment service health check", description = "Check if payment service is running")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of("status", "OK", "message", "Payment service is running"));
    }
}
