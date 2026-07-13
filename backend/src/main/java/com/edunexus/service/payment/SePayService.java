package com.edunexus.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@Slf4j
public class SePayService {

    @Value("${sepay.webhook-secret}")
    private String webhookSecret;

    public boolean verifyWebhookSignature(String signature, String payload) {
        try {
            String computedSignature = generateSignature(payload);
            boolean isValid = computedSignature.equals(signature);

            if (!isValid) {
                log.warn("Invalid SePay webhook signature. Expected: {}, Got: {}", computedSignature, signature);
            }

            return isValid;
        } catch (Exception e) {
            log.error("Error verifying SePay webhook signature", e);
            return false;
        }
    }

    public String generateSignature(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error generating SePay signature", e);
            throw new RuntimeException("Signature generation failed", e);
        }
    }

    public boolean isTransferSuccess(String status) {
        return "00".equals(status) || "COMPLETED".equalsIgnoreCase(status);
    }

    public boolean isValidTransferStatus(String status) {
        return status != null && !status.isEmpty();
    }
}
