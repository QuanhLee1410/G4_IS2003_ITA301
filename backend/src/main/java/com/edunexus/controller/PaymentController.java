package com.edunexus.controller;

import com.edunexus.dto.TransactionDTO;
import com.edunexus.security.UserPrincipal;
import com.edunexus.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Slf4j
@Tag(name = "Payments", description = "Payment gateway integration endpoints")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

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
    @Operation(summary = "VNPay webhook", description = "Webhook endpoint for VNPay payment notifications")
    public ResponseEntity<?> handleVNPayWebhook(@RequestParam Map<String, String> params) {
        String responseCode = params.get("vnp_ResponseCode");
        String transactionCode = params.get("vnp_TxnRef");
        String vnpTransactionNo = params.get("vnp_TransactionNo");

        log.info("VNPay webhook received: transactionCode={}, responseCode={}", transactionCode, responseCode);

        try {
            if ("00".equals(responseCode)) {
                paymentService.handleVNPaySuccess(transactionCode, vnpTransactionNo);
                return ResponseEntity.ok(Map.of("code", "00", "message", "Payment successful"));
            } else {
                paymentService.handleVNPayFailure(transactionCode);
                return ResponseEntity.ok(Map.of("code", responseCode, "message", "Payment failed"));
            }
        } catch (Exception e) {
            log.error("Error processing VNPay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "99", "message", "Error processing payment"));
        }
    }

    @PostMapping("/sepay-webhook")
    @Operation(summary = "SePay webhook", description = "Webhook endpoint for SePay bank transfer notifications")
    public ResponseEntity<?> handleSePayWebhook(@RequestBody Map<String, Object> payload) {
        String transactionCode = (String) payload.get("description");
        Object amountObj = payload.get("amount");

        log.info("SePay webhook received for transaction: {}", transactionCode);

        try {
            if (amountObj != null && transactionCode != null) {
                java.math.BigDecimal amount = new java.math.BigDecimal(amountObj.toString());
                paymentService.handleBankTransferSuccess(transactionCode, amount);
                return ResponseEntity.ok(Map.of("code", "00", "message", "Transfer received"));
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("code", "01", "message", "Invalid payload"));
        } catch (Exception e) {
            log.error("Error processing SePay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", "99", "message", "Error processing payment"));
        }
    }
}
