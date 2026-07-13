package com.edunexus.service.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class VNPayServiceTest {

    @InjectMocks
    private VNPayService vnPayService;

    private String testHashSecret = "TEST_SECRET_KEY_FOR_TESTING_PURPOSE";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(vnPayService, "hashSecret", testHashSecret);
        ReflectionTestUtils.setField(vnPayService, "tmnCode", "TESTTMN");
        ReflectionTestUtils.setField(vnPayService, "apiUrl", "https://sandbox.vnpayment.vn/paygate/api/transaction");
        ReflectionTestUtils.setField(vnPayService, "returnUrl", "http://localhost:3000/payment-result");
        ReflectionTestUtils.setField(vnPayService, "notifyUrl", "http://localhost:8080/api/payments/vnpay-webhook");
    }

    @Test
    public void testGenerateSecureHash() {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_CreateDate", "20240101120000");
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_IpAddr", "127.0.0.1");
        params.put("vnp_Locale", "vn");
        params.put("vnp_OrderInfo", "Test Order");
        params.put("vnp_OrderType", "other");
        params.put("vnp_ReturnUrl", "http://localhost:3000/result");
        params.put("vnp_TmnCode", "TESTTMN");
        params.put("vnp_TxnRef", "TXN-123");
        params.put("vnp_Version", "2.1.0");

        String hash = vnPayService.generateSecureHash(params);

        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        assertEquals(128, hash.length()); // SHA512 hex is 128 characters
    }

    @Test
    public void testVerifyWebhookSignature_ValidSignature() {
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_Command", "pay");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "TXN-123");

        String validHash = vnPayService.generateSecureHash(params);
        params.put("vnp_SecureHash", validHash);

        assertTrue(vnPayService.verifyWebhookSignature(params));
    }

    @Test
    public void testVerifyWebhookSignature_InvalidSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "TXN-123");
        params.put("vnp_SecureHash", "INVALID_HASH_VALUE_12345");

        assertFalse(vnPayService.verifyWebhookSignature(params));
    }

    @Test
    public void testVerifyWebhookSignature_MissingHash() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Amount", "1000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TxnRef", "TXN-123");

        assertFalse(vnPayService.verifyWebhookSignature(params));
    }

    @Test
    public void testIsSuccessResponse() {
        assertTrue(vnPayService.isSuccessResponse("00"));
        assertFalse(vnPayService.isSuccessResponse("01"));
        assertFalse(vnPayService.isSuccessResponse("05"));
        assertFalse(vnPayService.isSuccessResponse("99"));
    }

    @Test
    public void testGeneratePaymentUrl() {
        String paymentUrl = vnPayService.generatePaymentUrl("TXN-123", 100000L, "Test Course");

        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.contains("https://sandbox.vnpayment.vn/paygate/api/transaction?"));
        assertTrue(paymentUrl.contains("vnp_TmnCode=TESTTMN"));
        assertTrue(paymentUrl.contains("vnp_TxnRef=TXN-123"));
        assertTrue(paymentUrl.contains("vnp_Amount=10000000")); // 100000 * 100
        assertTrue(paymentUrl.contains("vnp_SecureHash="));
    }
}
