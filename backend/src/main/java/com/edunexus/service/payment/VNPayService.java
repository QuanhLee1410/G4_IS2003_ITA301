package com.edunexus.service.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Slf4j
public class VNPayService {

    @Value("${vnpay.api-url}")
    private String apiUrl;

    @Value("${vnpay.return-url}")
    private String returnUrl;

    @Value("${vnpay.notify-url}")
    private String notifyUrl;

    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    @Value("${vnpay.hash-secret}")
    private String hashSecret;

    private static final String QUERY_DR = "&";
    private static final String ENCODE_TYPE = "UTF-8";
    private static final String VERSION = "2.1.0";

    public String generatePaymentUrl(String orderId, Long amount, String description) {
        Map<String, String> vnpParams = new TreeMap<>();
        vnpParams.put("vnp_Version", VERSION);
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", tmnCode);
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", orderId);
        vnpParams.put("vnp_OrderInfo", description);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay requires amount in cents
        vnpParams.put("vnp_ReturnUrl", returnUrl);
        vnpParams.put("vnp_IpAddr", getClientIp());
        vnpParams.put("vnp_CreateDate", getCurrentTimestamp());

        // Generate secure hash
        String secureHash = generateSecureHash(vnpParams);
        vnpParams.put("vnp_SecureHash", secureHash);
        vnpParams.put("vnp_SecureHashType", "SHA512");

        // Build payment URL
        StringBuilder paymentUrl = new StringBuilder(apiUrl);
        paymentUrl.append("?");

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            if (paymentUrl.length() > apiUrl.length() + 1) {
                paymentUrl.append(QUERY_DR);
            }
            try {
                paymentUrl.append(entry.getKey()).append("=")
                        .append(URLEncoder.encode(entry.getValue(), ENCODE_TYPE));
            } catch (UnsupportedEncodingException e) {
                log.error("Error encoding URL parameter", e);
            }
        }

        log.info("Generated VNPay payment URL for order: {}", orderId);
        return paymentUrl.toString();
    }

    public boolean verifyWebhookSignature(Map<String, String> params) {
        String clientHash = params.get("vnp_SecureHash");
        if (clientHash == null || clientHash.isEmpty()) {
            log.warn("Missing secure hash in VNPay webhook");
            return false;
        }

        // Remove hash from params to calculate server hash
        Map<String, String> signParams = new TreeMap<>(params);
        signParams.remove("vnp_SecureHash");
        signParams.remove("vnp_SecureHashType");

        String serverHash = generateSecureHash(signParams);
        boolean isValid = clientHash.equals(serverHash);

        if (!isValid) {
            log.warn("Invalid VNPay webhook signature. Expected: {}, Got: {}", serverHash, clientHash);
        }

        return isValid;
    }

    public boolean isSuccessResponse(String responseCode) {
        return "00".equals(responseCode);
    }

    public String generateSecureHash(Map<String, String> params) {
        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append("&");
                }
                hashData.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }

        String data = hashData.toString();
        byte[] hashBytes = hmacSHA512(hashSecret, data);
        return toHexString(hashBytes);
    }

    private byte[] hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKeySpec = 
                    new javax.crypto.spec.SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKeySpec);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Error computing HMAC SHA512", e);
            throw new RuntimeException("HMAC SHA512 computation failed", e);
        }
    }

    private String toHexString(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private String getCurrentTimestamp() {
        return String.format("%d%02d%02d%02d%02d%02d",
                Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH) + 1,
                Calendar.getInstance().get(Calendar.DAY_OF_MONTH),
                Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                Calendar.getInstance().get(Calendar.MINUTE),
                Calendar.getInstance().get(Calendar.SECOND));
    }

    private String getClientIp() {
        // In a real scenario, get this from HttpServletRequest
        // For now, return a default
        return "127.0.0.1";
    }
}
