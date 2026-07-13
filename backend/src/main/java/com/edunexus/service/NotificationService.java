package com.edunexus.service;

import com.edunexus.entity.Transaction;
import com.edunexus.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@edunexus.com}")
    private String fromEmail;

    @Async
    public void sendPaymentSuccessEmail(User student, Transaction transaction) {
        try {
            if (mailSender == null) {
                log.info("Mail sender not configured. Skipping email notification for transaction: {}", 
                        transaction.getTransactionCode());
                return;
            }

            String subject = "Thanh toán khóa học thành công - EduNexus";
            String content = buildPaymentSuccessEmailContent(student, transaction);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(student.getEmail());
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Payment success email sent to: {}", student.getEmail());
        } catch (Exception e) {
            log.error("Error sending payment success email", e);
        }
    }

    @Async
    public void sendPaymentFailureEmail(User student, Transaction transaction, String reason) {
        try {
            if (mailSender == null) {
                log.info("Mail sender not configured. Skipping email notification for transaction: {}", 
                        transaction.getTransactionCode());
                return;
            }

            String subject = "Thanh toán khóa học thất bại - EduNexus";
            String content = buildPaymentFailureEmailContent(student, transaction, reason);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(student.getEmail());
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Payment failure email sent to: {}", student.getEmail());
        } catch (Exception e) {
            log.error("Error sending payment failure email", e);
        }
    }

    @Async
    public void sendEnrollmentConfirmationEmail(User student, String courseName) {
        try {
            if (mailSender == null) {
                log.info("Mail sender not configured. Skipping enrollment confirmation email");
                return;
            }

            String subject = "Đăng ký khóa học thành công - EduNexus";
            String content = buildEnrollmentConfirmationEmailContent(student, courseName);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(student.getEmail());
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("Enrollment confirmation email sent to: {}", student.getEmail());
        } catch (Exception e) {
            log.error("Error sending enrollment confirmation email", e);
        }
    }

    private String buildPaymentSuccessEmailContent(User student, Transaction transaction) {
        return "Kính chào " + student.getFullName() + ",\n\n" +
                "Cảm ơn bạn đã thanh toán khóa học trên nền tảng EduNexus.\n\n" +
                "Chi tiết thanh toán:\n" +
                "- Khóa học: " + transaction.getCourse().getTitle() + "\n" +
                "- Số tiền: " + transaction.getAmount() + " VND\n" +
                "- Mã giao dịch: " + transaction.getTransactionCode() + "\n" +
                "- Thời gian: " + transaction.getPaidAt() + "\n\n" +
                "Bạn đã được cấp quyền truy cập khóa học. Hãy đăng nhập để bắt đầu học tập.\n\n" +
                "Chúc bạn có trải nghiệm học tập tuyệt vời!\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ EduNexus\n" +
                "support@edunexus.com";
    }

    private String buildPaymentFailureEmailContent(User student, Transaction transaction, String reason) {
        return "Kính chào " + student.getFullName() + ",\n\n" +
                "Thanh toán cho khóa học không thành công.\n\n" +
                "Chi tiết thanh toán:\n" +
                "- Khóa học: " + transaction.getCourse().getTitle() + "\n" +
                "- Số tiền: " + transaction.getAmount() + " VND\n" +
                "- Mã giao dịch: " + transaction.getTransactionCode() + "\n" +
                "- Lý do: " + reason + "\n\n" +
                "Vui lòng kiểm tra lại thông tin thanh toán và thử lại.\n\n" +
                "Nếu bạn gặp vấn đề, vui lòng liên hệ support@edunexus.com\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ EduNexus";
    }

    private String buildEnrollmentConfirmationEmailContent(User student, String courseName) {
        return "Kính chào " + student.getFullName() + ",\n\n" +
                "Chúc mừng! Bạn đã successfully đăng ký khóa học.\n\n" +
                "Khóa học: " + courseName + "\n\n" +
                "Hãy truy cập EduNexus để bắt đầu học tập ngay hôm nay.\n\n" +
                "Chúc bạn có trải nghiệm học tập tuyệt vời!\n\n" +
                "Trân trọng,\n" +
                "Đội ngũ EduNexus\n" +
                "support@edunexus.com";
    }
}
