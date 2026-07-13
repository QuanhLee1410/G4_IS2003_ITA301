package com.edunexus.service.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SePayServiceTest {

    @InjectMocks
    private SePayService sePayService;

    private String testApiKey = "TEST_API_KEY_FOR_SEPAY";

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(sePayService, "sePayApiKey", testApiKey);
    }

    @Test
    public void testGenerateSignature() {
        String payload = "{\"amount\":100000,\"description\":\"TXN-123\",\"status\":\"00\"}";
        
        String signature = sePayService.generateSignature(payload);

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        
        // Verify it's valid Base64
        try {
            Base64.getDecoder().decode(signature);
        } catch (IllegalArgumentException e) {
            fail("Generated signature is not valid Base64");
        }
    }

    @Test
    public void testVerifyWebhookSignature_Valid() {
        String payload = "{\"amount\":100000,\"description\":\"TXN-123\",\"status\":\"00\"}";
        String signature = sePayService.generateSignature(payload);

        assertTrue(sePayService.verifyWebhookSignature(signature, payload));
    }

    @Test
    public void testVerifyWebhookSignature_Invalid() {
        String payload = "{\"amount\":100000,\"description\":\"TXN-123\",\"status\":\"00\"}";
        String invalidSignature = Base64.getEncoder().encodeToString("INVALID_SIGNATURE".getBytes());

        assertFalse(sePayService.verifyWebhookSignature(invalidSignature, payload));
    }

    @Test
    public void testVerifyWebhookSignature_TamperedPayload() {
        String payload = "{\"amount\":100000,\"description\":\"TXN-123\",\"status\":\"00\"}";
        String signature = sePayService.generateSignature(payload);
        
        String tamperedPayload = "{\"amount\":999999,\"description\":\"TXN-123\",\"status\":\"00\"}";

        assertFalse(sePayService.verifyWebhookSignature(signature, tamperedPayload));
    }

    @Test
    public void testIsTransferSuccess() {
        assertTrue(sePayService.isTransferSuccess("00"));
        assertTrue(sePayService.isTransferSuccess("success"));
        
        assertFalse(sePayService.isTransferSuccess("failed"));
        assertFalse(sePayService.isTransferSuccess("error"));
        assertFalse(sePayService.isTransferSuccess("pending"));
    }
}
