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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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

    @Value("${vnpay.hash-secret}")
    private String vnpayHashSecret;

    public TransactionDTO initiatePayment(Integer studentId, Integer courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

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

    public void handleVNPaySuccess(String transactionCode, String vnpTransactionNo) {
        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "code", transactionCode));

        transaction.setStatus("SUCCESS");
        transaction.setPaidAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        enrollmentService.enrollCourse(transaction.getStudent().getUserId(), transaction.getCourse().getCourseId());
        log.info("Payment successful: {} - Transaction marked as SUCCESS and enrollment created", transactionCode);
    }

    public void handleVNPayFailure(String transactionCode) {
        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "code", transactionCode));

        transaction.setStatus("FAILED");
        transactionRepository.save(transaction);
        log.warn("Payment failed: {}", transactionCode);
    }

    public void handleBankTransferSuccess(String transactionCode, BigDecimal amount) {
        Transaction transaction = transactionRepository.findByTransactionCode(transactionCode)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "code", transactionCode));

        if (transaction.getAmount().compareTo(amount) != 0) {
            throw new AppException("Payment amount mismatch");
        }

        transaction.setStatus("SUCCESS");
        transaction.setPaidAt(LocalDateTime.now());
        transaction.setPaymentMethod("BANK_TRANSFER");
        transaction = transactionRepository.save(transaction);

        enrollmentService.enrollCourse(transaction.getStudent().getUserId(), transaction.getCourse().getCourseId());
        log.info("Bank transfer confirmed: {} - Enrollment created", transactionCode);
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
