package com.edunexus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private Integer transactionId;
    private String transactionCode;
    private Integer studentId;
    private Integer courseId;
    private String courseName;
    private BigDecimal amount;
    private String status; // PENDING, SUCCESS, FAILED
    private String paymentMethod;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class VNPayWebhookDTO {
    private String vnp_ResponseCode;
    private String vnp_TransactionNo;
    private String vnp_Amount;
    private String vnp_OrderInfo;
    private String vnp_SecureHash;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class PaymentInitRequest {
    private Integer courseId;
    private BigDecimal amount;
}
