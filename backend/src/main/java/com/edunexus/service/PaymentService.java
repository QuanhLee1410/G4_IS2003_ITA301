package com.edunexus.service;

import com.edunexus.dto.TransactionDTO;
import com.edunexus.entity.Course;
import com.edunexus.entity.Transaction;
import com.edunexus.entity.User;
import com.edunexus.exception.AppException;
import com.edunexus.exception.ResourceNotFoundException;
import com.edunexus.repository.CourseRepository;
import com.edunexus.repository.TransactionRepository;
import com.edunexus.repository.UserRepository;
import com.edunexus.service.payment.VNPayService;
import com.edunexus.service.payment.SePayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PaymentService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private VNPayService vnPayService;

    @Autowired
    private SePayService sePayService;

    @Value("${vnpay.hash-secret}")
    private String vnpayHashSecret;

    public TransactionDTO initiatePayment(Integer studentId, Integer courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        if (!student.getRole().getRoleName().equals("STUDENT")) {
            throw new AppException("Only students can initiate payments");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        // Check if already enrolled
        if (enrollmentService.isStudentEnrolled(studentId, courseId)) {
            throw new AppException("Student is already enrolled in this course");
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionCode(UUID.randomUUID().toString());
        transaction.setStudent(student);
        transaction.setCourse(course);
        transaction.setAmount(course.getPrice());
        transaction.setStatus("PENDING");
        transaction.setPaymentMethod("VNPAY");

        transaction = transactionRepository.save(transaction);
        log.info("Payment initiated: {} for student: {} course: {}", transaction.getTransactionCode(), studentId, courseId);

        return mapToDTO(transaction);
    }

    public String generateVNPayPaymentUrl(Integer transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));

        if (!"PENDING".equals(transaction.getStatus())) {
            throw new AppException("Can only generate payment URL for pending transactions");
        }

        String description = "Thanh toan khoa hoc: " + transaction.getCourse().getTitle();
        long amountInVND = transaction.getAmount().longValue();

        return vnPayService.generatePaymentUrl(transaction.getTransactionCode(), amountInVND, description);
    }

    public void handleVNPaySuccess(Map<String, String> params) {
        // Verify webhook signature
        if (!vnPayService.verifyWebhookSignature(params)) {
            throw new AppException("Invalid VNPay webhook signature");
        }

        String transactionCode = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        if (!vnPayService.isSuccessResponse(responseCode)) {
            log.warn("VNPay payment failed with response code: {}", responseCode);
            Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Transaction", "code", transactionCode));
            transaction.setStatus("FAILED");
            transactionRepository.save(transaction);
            notificationService.sendPaymentFailureEmail(transaction.getStudent(), transaction, 
                    "VNPay returned error code: " + responseCode);
            throw new AppException("Payment failed with code: " + responseCode);
        }

        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "code", transactionCode));

        // Check for duplicate processing (idempotency)
        if ("SUCCESS".equals(transaction.getStatus())) {
            log.info("Transaction already processed: {}", transactionCode);
            return;
        }

        transaction.setStatus("SUCCESS");
        transaction.setPaidAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        // Auto-enroll student in course
        try {
            enrollmentService.enrollCourse(transaction.getStudent().getUserId(), transaction.getCourse().getCourseId());
            log.info("Auto-enrollment successful for VNPay transaction: {}", transactionCode);
        } catch (Exception e) {
            log.error("Error during auto-enrollment for transaction: {}", transactionCode, e);
            notificationService.sendPaymentSuccessEmail(transaction.getStudent(), transaction);
            throw new AppException("Payment succeeded but enrollment failed. Please contact support.");
        }

        notificationService.sendPaymentSuccessEmail(transaction.getStudent(), transaction);
        notificationService.sendEnrollmentConfirmationEmail(transaction.getStudent(), transaction.getCourse().getTitle());
    }

    public void handleSePaySuccess(String description, BigDecimal amount, String status) {
        if (!sePayService.isTransferSuccess(status)) {
            log.warn("SePay transfer failed with status: {}", status);
            throw new AppException("Bank transfer failed");
        }

        Transaction transaction = transactionRepository.findByTransactionCode(description)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "code", description));

        // Check for duplicate processing (idempotency)
        if ("SUCCESS".equals(transaction.getStatus())) {
            log.info("SePay transaction already processed: {}", description);
            return;
        }

        // Verify amount
        if (transaction.getAmount().compareTo(amount) != 0) {
            log.warn("Amount mismatch for SePay transaction: {}. Expected: {}, Got: {}", 
                    description, transaction.getAmount(), amount);
            throw new AppException("Transfer amount does not match course price");
        }

        transaction.setStatus("SUCCESS");
        transaction.setPaidAt(LocalDateTime.now());
        transaction.setPaymentMethod("BANK_TRANSFER");
        transaction = transactionRepository.save(transaction);

        // Auto-enroll student
        try {
            enrollmentService.enrollCourse(transaction.getStudent().getUserId(), transaction.getCourse().getCourseId());
            log.info("Auto-enrollment successful for SePay transaction: {}", description);
        } catch (Exception e) {
            log.error("Error during auto-enrollment for SePay transaction: {}", description, e);
            notificationService.sendPaymentSuccessEmail(transaction.getStudent(), transaction);
            throw new AppException("Transfer confirmed but enrollment failed. Please contact support.");
        }

        notificationService.sendPaymentSuccessEmail(transaction.getStudent(), transaction);
        notificationService.sendEnrollmentConfirmationEmail(transaction.getStudent(), transaction.getCourse().getTitle());
    }

    public void handleVNPayFailure(String transactionCode) {
        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "code", transactionCode));

        transaction.setStatus("FAILED");
        transactionRepository.save(transaction);
        log.warn("Payment failed: {}", transactionCode);
    }

    public TransactionDTO getTransactionById(Integer transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", transactionId));
        return mapToDTO(transaction);
    }

    public List<TransactionDTO> getStudentTransactions(Integer studentId) {
        return transactionRepository.findByStudentId(studentId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<TransactionDTO> getPendingTransactions() {
        return transactionRepository.findByStatus("PENDING").stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private TransactionDTO mapToDTO(Transaction transaction) {
        return TransactionDTO.builder()
                .transactionId(transaction.getTransactionId())
                .transactionCode(transaction.getTransactionCode())
                .studentId(transaction.getStudent().getUserId())
                .courseId(transaction.getCourse().getCourseId())
                .courseName(transaction.getCourse().getTitle())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .paymentMethod(transaction.getPaymentMethod())
                .paidAt(transaction.getPaidAt())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
