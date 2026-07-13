# Phase 2: Payment Integration Guide - EduNexus

## Overview
Phase 2 implements automated payment processing with VNPay (redirect-based) and SePay (bank transfer webhook) integration. This guide covers setup, testing, and troubleshooting.

## Table of Contents
1. [Architecture Overview](#architecture-overview)
2. [VNPay Integration](#vnpay-integration)
3. [SePay Integration](#sepay-integration)
4. [Testing Payment Flow](#testing-payment-flow)
5. [Webhook Security](#webhook-security)
6. [Common Issues & Solutions](#common-issues--solutions)

---

## Architecture Overview

### Payment Flow Diagram
```
STUDENT                                    BACKEND                 PAYMENT GATEWAY
   |                                          |                          |
   |---- Initiate Payment (POST /init) ------>|                          |
   |                                          |                          |
   |<---- Transaction + URL (VNPAY_URL) ------| Generate Payment URL     |
   |                                          |                          |
   |---- Redirect to VNPay                   |                          |
   |                        (ONLY FOR VNPAY) |                          |
   |-------- ENTER CARD/BANK INFO ------------->|                       |
   |                                          |     Process Payment      |
   |                                          |<----- Success/Fail ----->|
   |                                          |                          |
   |<--- Redirect to Return URL --------------------------------|       |
   |  (with vnp_ResponseCode, vnp_SecureHash)                         |
   |                                          |                          |
   |---- Webhook Notification (IPN) -------->| (Background)            |
   |  (from payment gateway)                 |                          |
   |                                          |---- Verify Signature    |
   |                                          |---- Create Enrollment   |
   |                                          |---- Send Email          |
   |                                          |                          |
   |<---- Auto-Enrolled ----------------------|
   |                                          |
```

### Key Components
- **VNPayService**: Generates payment URLs, verifies HMAC-SHA512 signatures
- **SePayService**: Handles bank transfer webhooks with HMAC-SHA256 + Base64
- **PaymentService**: Orchestrates payment flow, auto-enrollment, notifications
- **NotificationService**: Async email notifications for payment events
- **PaymentController**: REST endpoints for payment initiation and webhook handlers

---

## VNPay Integration

### Setup Instructions

#### 1. Get VNPay Merchant Account
```
Website: https://sandbox.vnpayment.vn (Sandbox) or https://sandbox.vnpayment.vn (Production)
Contact VNPay for merchant credentials:
- TMN Code (Merchant ID)
- Hash Secret (for HMAC signature)
```

#### 2. Configure in `application.properties`
```properties
# VNPay Configuration
vnpay.tmn-code=YOUR_TMN_CODE
vnpay.hash-secret=YOUR_HASH_SECRET
vnpay.api-url=https://sandbox.vnpayment.vn/paygate/api/transaction
vnpay.return-url=http://localhost:3000/payment-result
vnpay.notify-url=http://localhost:8080/api/payments/vnpay-webhook
vnpay.api-version=2.1.0
```

#### 3. Understand VNPay Signature (Critical!)
VNPay uses **TreeMap** for parameter ordering (alphabetical). This ensures consistent hashing:

```java
Map<String, String> params = new TreeMap<>();  // Auto-sorts!
params.put("vnp_Amount", "1000000");           // In cents
params.put("vnp_Command", "pay");
params.put("vnp_CreateDate", "20240101120000"); // YYYYMMDDHHMMSS
params.put("vnp_CurrCode", "VND");
params.put("vnp_TmnCode", "YOUR_TMN_CODE");
params.put("vnp_TxnRef", "TXN-123");           // Your transaction code
params.put("vnp_OrderInfo", "Course: Java 101");

// Build query string (parameters in alphabetical order)
String queryString = "vnp_Amount=1000000&vnp_Command=pay&...";

// HMAC-SHA512
String hash = HMACSHA512(queryString, hashSecret);
```

### API Endpoints

#### 1. Initiate Payment
```bash
curl -X POST http://localhost:8080/api/payments/init \
  -H "Authorization: Bearer JWT_TOKEN" \
  -d "courseId=1"

# Response:
{
  "transactionId": 1,
  "transactionCode": "TXN-123",
  "amount": 99.99,
  "status": "PENDING",
  "paymentMethod": "VNPAY"
}
```

#### 2. Get VNPay Payment URL
```bash
curl -X GET http://localhost:8080/api/payments/vnpay-url/1 \
  -H "Authorization: Bearer JWT_TOKEN"

# Response:
{
  "paymentUrl": "https://sandbox.vnpayment.vn/paygate/api/transaction?vnp_Amount=9999&vnp_Command=pay&vnp_CreateDate=20240101&vnp_CurrCode=VND&..."
}
```

#### 3. VNPay Webhook (IPN)
```bash
# VNPay calls this endpoint automatically
POST http://localhost:8080/api/payments/vnpay-webhook?vnp_TxnRef=TXN-123&vnp_ResponseCode=00&vnp_SecureHash=...

# Response:
{
  "code": "00",
  "message": "Payment notification received successfully"
}
```

**Response Codes:**
- `00`: Success - Payment processed, enrollment created
- `97`: Invalid signature or amount mismatch
- `99`: Server error

---

## SePay Integration

### Setup Instructions

#### 1. Get SePay Account
```
Website: https://sandbox.se-pay.com (Sandbox)
Contact SePay for:
- API Key (for signature verification)
- Webhook URL configuration
```

#### 2. Configure in `application.properties`
```properties
# SePay Configuration
sepay.api-key=YOUR_API_KEY
sepay.webhook-url=http://localhost:8080/api/payments/sepay-webhook
sepay.bank-list=VCB,VIB,TCB,MBBank,ACB  # Supported banks
```

#### 3. Understand SePay Signature
SePay uses **HMAC-SHA256 + Base64 encoding**:

```java
String payload = """
{
  "description": "TXN-123",
  "amount": 100000,
  "status": "00"
}
""";

// HMAC-SHA256
byte[] hmac = HmacSHA256(payload, apiKey);
String signature = Base64.encode(hmac);

// Header:
// X-Signature: BASE64_ENCODED_SIGNATURE
```

### API Endpoints

#### 1. SePay Webhook (Bank Transfer Notification)
```bash
curl -X POST http://localhost:8080/api/payments/sepay-webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: BASE64_SIGNATURE" \
  -d '{
    "description": "TXN-123",
    "amount": 100000,
    "status": "00",
    "bank": "VCB",
    "transactionNo": "SEPAY-999"
  }'

# Response:
{
  "code": "00",
  "message": "Transfer notification received successfully"
}
```

**SePay Status Codes:**
- `00` or `success`: Transfer successful
- `failed`: Transfer failed
- `pending`: Transfer pending

---

## Testing Payment Flow

### Manual Testing Steps

#### Test 1: VNPay Payment Flow (Sandbox)
```bash
# 1. Register & Login
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "Test@123",
    "email": "test@test.com",
    "fullName": "Test User"
  }'

# Copy JWT token from response

# 2. Get VNPay URL
curl -X GET http://localhost:8080/api/payments/vnpay-url/1 \
  -H "Authorization: Bearer JWT_TOKEN"

# 3. Open URL in browser → Enter test card details
# VNPay Sandbox Test Card:
# - Card Number: 4111111111111111
# - CVV: 123
# - Expiry: 12/25
# - OTP: 123456

# 4. Check transaction status
curl -X GET http://localhost:8080/api/payments/1 \
  -H "Authorization: Bearer JWT_TOKEN"

# Response should show status: "SUCCESS"
```

#### Test 2: SePay Webhook Simulation
```bash
# Generate signature using Java code:
# String signature = SePayService.generateSignature(payload);

# Send webhook:
curl -X POST http://localhost:8080/api/payments/sepay-webhook \
  -H "Content-Type: application/json" \
  -H "X-Signature: YOUR_SIGNATURE" \
  -d '{
    "description": "TXN-123",
    "amount": 100000,
    "status": "00"
  }'

# Check enrollment was created
curl -X GET http://localhost:8080/api/enrollments/student/courses \
  -H "Authorization: Bearer JWT_TOKEN"
```

### Using Postman Collection
1. Import `EduNexus_Payment_API_Phase2.postman_collection.json`
2. Set environment variables:
   - `access_token`: JWT token from login
   - `courseId`: ID of course to purchase (default: 1)
3. Run requests in order

---

## Webhook Security

### HMAC-SHA512 Verification (VNPay)
```java
public boolean verifyWebhookSignature(Map<String, String> params) {
    String receivedHash = params.remove("vnp_SecureHash");
    
    // Build query string (alphabetical order)
    TreeMap<String, String> sorted = new TreeMap<>(params);
    String queryString = buildQueryString(sorted);
    
    // Generate HMAC
    String calculatedHash = generateSecureHash(queryString);
    
    // Compare (constant-time comparison prevents timing attacks)
    return MessageDigest.isEqual(
        receivedHash.getBytes(),
        calculatedHash.getBytes()
    );
}
```

### HMAC-SHA256 Verification (SePay)
```java
public boolean verifyWebhookSignature(String signature, String payload) {
    String calculatedSignature = generateSignature(payload);
    
    // Compare signatures
    return MessageDigest.isEqual(
        signature.getBytes(),
        calculatedSignature.getBytes()
    );
}
```

### Security Best Practices
1. ✅ Always verify webhook signatures before processing
2. ✅ Use idempotency checks (check if transaction already SUCCESS)
3. ✅ Log all webhook events for audit trail
4. ✅ Use HTTPS for all webhook endpoints
5. ✅ Rate limit webhook endpoints
6. ✅ Never expose webhook secrets in client-side code
7. ✅ Whitelist payment gateway IPs if possible

---

## Common Issues & Solutions

### Issue 1: Invalid Signature Error
**Symptom:** `"Invalid VNPay webhook signature"`

**Cause:** Parameter ordering or secret mismatch

**Solution:**
```java
// ✓ CORRECT - Use TreeMap for automatic alphabetical sorting
Map<String, String> params = new TreeMap<>();
params.put("vnp_Amount", "1000000");
params.put("vnp_Command", "pay");
// ... rest of params

// ✗ WRONG - HashMap doesn't guarantee order
Map<String, String> params = new HashMap<>();
```

### Issue 2: Amount Mismatch Error
**Symptom:** `"Payment amount does not match course price"`

**Cause:** VNPay sends amount in cents; SePay in base units

**Solution:**
```java
// VNPay: multiply by 100
long amountInVND = course.getPrice() * 100;

// SePay: use as-is
long amountInVND = course.getPrice();
```

### Issue 3: Student Already Enrolled
**Symptom:** `"Student is already enrolled in this course"`

**Cause:** Duplicate payment initiation

**Solution:**
```java
// Check before initiating payment
if (enrollmentService.isStudentEnrolled(studentId, courseId)) {
    throw new AppException("Already enrolled");
}
```

### Issue 4: Email Not Sending
**Symptom:** Payment succeeds but no email received

**Cause:** Email service not configured

**Solution:**
```properties
# Add to application.properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=app-password-here
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.from=noreply@edunexus.com
```

### Issue 5: Webhook Not Received
**Symptom:** Payment successful on VNPay but no callback

**Causes & Solutions:**
1. **Firewall blocking:** Whitelist VNPay IPs in firewall
2. **Localhost URL:** Use ngrok for local testing
   ```bash
   ngrok http 8080
   # Update notify-url in config to ngrok URL
   ```
3. **Wrong webhook URL:** Verify `vnpay.notify-url` in properties

---

## Deployment Checklist

- [ ] Configure production VNPay merchant credentials
- [ ] Configure production SePay API key
- [ ] Update webhook URLs to production domain
- [ ] Enable HTTPS for all endpoints
- [ ] Set up email service (Gmail, SendGrid, etc.)
- [ ] Test full payment flow in sandbox
- [ ] Set up monitoring/alerts for failed payments
- [ ] Create database backups
- [ ] Document admin procedures for refunds
- [ ] Train support team on payment troubleshooting

---

## References

- [VNPay API Documentation](https://sandbox.vnpayment.vn/apis/docs/)
- [SePay Webhook Guide](https://se-pay.com/docs/webhook)
- [HMAC-SHA512 Explanation](https://en.wikipedia.org/wiki/HMAC)
- [Spring Security Best Practices](https://spring.io/projects/spring-security)

---

**Last Updated:** 2024-01-01  
**Version:** 1.0 (Phase 2)
