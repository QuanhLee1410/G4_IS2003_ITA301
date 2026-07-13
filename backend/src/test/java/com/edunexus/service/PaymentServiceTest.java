package com.edunexus.service;

import com.edunexus.dto.TransactionDTO;
import com.edunexus.entity.Course;
import com.edunexus.entity.Role;
import com.edunexus.entity.Transaction;
import com.edunexus.entity.User;
import com.edunexus.exception.AppException;
import com.edunexus.exception.ResourceNotFoundException;
import com.edunexus.repository.CourseRepository;
import com.edunexus.repository.TransactionRepository;
import com.edunexus.repository.UserRepository;
import com.edunexus.service.payment.VNPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private VNPayService vnPayService;

    @InjectMocks
    private PaymentService paymentService;

    private User testStudent;
    private Course testCourse;
    private Transaction testTransaction;
    private Role studentRole;

    @BeforeEach
    public void setUp() {
        studentRole = new Role(1, "STUDENT");
        testStudent = new User(1, "student1", "hash", "student@test.com", "John Doe", studentRole, null, null);
        testCourse = new Course(1, "Java Course", "Learn Java", new BigDecimal("99.99"), null, testStudent, null, null, null, null);
        testTransaction = new Transaction(1, "TXN-123", testStudent, testCourse, new BigDecimal("99.99"), "PENDING", "VNPAY", null, null);
    }

    @Test
    public void testInitiatePayment_Success() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(1)).thenReturn(Optional.of(testCourse));
        when(enrollmentService.isStudentEnrolled(1, 1)).thenReturn(false);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        TransactionDTO result = paymentService.initiatePayment(1, 1);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("VNPAY", result.getPaymentMethod());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    public void testInitiatePayment_StudentNotFound() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.initiatePayment(1, 1));
    }

    @Test
    public void testInitiatePayment_StudentAlreadyEnrolled() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testStudent));
        when(courseRepository.findById(1)).thenReturn(Optional.of(testCourse));
        when(enrollmentService.isStudentEnrolled(1, 1)).thenReturn(true);

        assertThrows(AppException.class, () -> paymentService.initiatePayment(1, 1));
    }

    @Test
    public void testHandleVNPaySuccess_ValidSignature() {
        testTransaction.setStatus("PENDING");
        
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN-123");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "VNP123");

        when(vnPayService.verifyWebhookSignature(params)).thenReturn(true);
        when(vnPayService.isSuccessResponse("00")).thenReturn(true);
        when(transactionRepository.findByTransactionCode("TXN-123")).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(enrollmentService.isStudentEnrolled(anyInt(), anyInt())).thenReturn(false);

        paymentService.handleVNPaySuccess(params);

        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(enrollmentService, times(1)).enrollCourse(anyInt(), anyInt());
        verify(notificationService, times(1)).sendPaymentSuccessEmail(any(), any());
    }

    @Test
    public void testHandleVNPaySuccess_InvalidSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN-123");
        params.put("vnp_ResponseCode", "00");

        when(vnPayService.verifyWebhookSignature(params)).thenReturn(false);

        assertThrows(AppException.class, () -> paymentService.handleVNPaySuccess(params));
    }

    @Test
    public void testHandleVNPaySuccess_Idempotency() {
        testTransaction.setStatus("SUCCESS");
        
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "TXN-123");
        params.put("vnp_ResponseCode", "00");

        when(vnPayService.verifyWebhookSignature(params)).thenReturn(true);
        when(vnPayService.isSuccessResponse("00")).thenReturn(true);
        when(transactionRepository.findByTransactionCode("TXN-123")).thenReturn(Optional.of(testTransaction));

        // Should not throw exception and should not process again
        paymentService.handleVNPaySuccess(params);

        // enrollmentService should not be called for duplicate processing
        verify(enrollmentService, times(0)).enrollCourse(anyInt(), anyInt());
    }

    @Test
    public void testGetTransactionById_Success() {
        when(transactionRepository.findById(1)).thenReturn(Optional.of(testTransaction));

        TransactionDTO result = paymentService.getTransactionById(1);

        assertNotNull(result);
        assertEquals("TXN-123", result.getTransactionCode());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    public void testGetTransactionById_NotFound() {
        when(transactionRepository.findById(999)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.getTransactionById(999));
    }
}
