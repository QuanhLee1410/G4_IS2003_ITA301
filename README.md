# EduNexus — Hệ thống học tập (LMS)

EduNexus là một nền tảng Learning Management System (LMS) giúp quản lý khóa học, phân phối nội dung video an toàn và tự động hoá quy trình thanh toán để học viên truy cập khóa học sau khi thanh toán thành công.

---

## Tính năng nổi bật

- 🚀 **Quản lý khóa học**: Course → Sections → Lessons, CRUD cho giảng viên và admin
- 🔐 **Xác thực & RBAC**: JWT token, BCrypt cho mật khẩu, phân quyền ADMIN/TEACHER/STUDENT/SME
- 🎥 **Video streaming an toàn**: Presigned S3 URLs cho bảo mật và hiệu năng
- 💳 **Thanh toán tự động**: VNPay (redirect) và SePay (webhook) với xác thực HMAC
- 🔁 **Auto-enrollment**: Tự động tạo enrollment khi giao dịch thành công
- 📧 **Thông báo bất đồng bộ**: Email thông báo thanh toán và xác nhận enrollment

---

## Công nghệ sử dụng

| Thành phần | Công nghệ chính |
|---|---|
| Frontend (gợi ý) | React (không tích hợp trong repo backend) |
| Backend | Java 11, Spring Boot 2.7, Spring Security, JPA/Hibernate |
| Database | Microsoft SQL Server (chính), Redis (cache) |
| Storage & DevOps | AWS S3 (video), Flyway (migrations), Docker |
| Thanh toán | VNPay (HMAC-SHA512), SePay (HMAC-SHA256 + Base64) |
| Testing & API | JUnit 5, Mockito, Springdoc OpenAPI (Swagger) |

---

## Yêu cầu & Chuẩn bị

- Java 11+ và Maven 3.6+
- SQL Server 2016+ (hoặc tương thích)
- AWS S3 (bucket cho video) — tuỳ chọn nếu muốn streaming
- VNPay / SePay sandbox credentials để test thanh toán

---

## Hướng dẫn cài đặt & chạy dự án

1. Clone repository

```bash
git clone https://github.com/QuanhLee1410/G4_IS2003_ITA301.git
cd G4_IS2003_ITA301/backend
```

2. Cập nhật cấu hình (src/main/resources/application.properties)

- Database
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=EduNexusDB;encrypt=false;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=YOUR_DB_PASSWORD
```

- AWS S3 (nếu dùng)
```properties
aws.s3.bucket-name=edunexus-videos
aws.s3.region=YOUR_REGION
aws.s3.access-key=YOUR_AWS_ACCESS_KEY
aws.s3.secret-key=YOUR_AWS_SECRET_KEY
```

- VNPay / SePay
```properties
# VNPay
vnpay.tmn-code=YOUR_TMN_CODE
vnpay.hash-secret=YOUR_HASH_SECRET
vnpay.notify-url=https://your-domain.com/api/payments/vnpay-webhook

# SePay
sepay.api-key=YOUR_SEPAY_KEY
sepay.webhook-url=https://your-domain.com/api/payments/sepay-webhook
```

3. Khởi tạo database (Flyway migrations)

```bash
# Nếu Flyway được cấu hình, nó sẽ tự chạy khi ứng dụng khởi động
mvn clean package
mvn spring-boot:run
```

4. Truy cập dịch vụ

- Ứng dụng mặc định chạy tại: http://localhost:8080
- Swagger UI (nếu bật): http://localhost:8080/swagger-ui.html

---

## Các endpoint quan trọng (tóm tắt)

- Authentication
  - POST /api/auth/register — Đăng ký
  - POST /api/auth/login — Đăng nhập
  - POST /api/auth/refresh — Refresh token

- Courses & Lessons
  - GET /api/courses
  - POST /api/courses (TEACHER/ADMIN)
  - GET /api/courses/{id}/sections
  - GET /api/lessons/{id}/video — trả presigned S3 URL

- Enrollments
  - POST /api/enrollments — tạo đăng ký (STUDENT)
  - GET /api/enrollments/my-courses — khóa học đã mua

- Payments (Phase 2)
  - POST /api/payments/init — khởi tạo giao dịch
  - GET /api/payments/vnpay-url/{transactionId} — lấy URL redirect
  - POST /api/payments/vnpay-webhook — VNPay callback (IPN)
  - POST /api/payments/sepay-webhook — SePay bank transfer callback

(Giữ nguyên: xem đầy đủ API trong code hoặc Swagger.)

---

## Hướng dẫn kiểm thử

- Unit tests

```bash
mvn test
```

- Chạy test riêng cho payment

```bash
mvn test -Dtest=VNPayServiceTest,SePayServiceTest,PaymentServiceTest
```

- Manual (Postman): import `EduNexus_Payment_API_Phase2.postman_collection.json` và chạy flow: đăng ký → login → init payment → simulate webhook

---

## Cấu trúc dữ liệu chính (tóm tắt)

- Users (id, username, email, passwordHash, role)
- Courses (id, title, price, teacherId)
- Sections, Lessons (cấu trúc phân cấp)
- Enrollments (studentId, courseId, status)
- Transactions (transactionCode, studentId, courseId, amount, status)

---

## Quy tắc bảo mật & vận hành

- Mật khẩu lưu bằng BCrypt (ít nhất 10 rounds)
- JWT HS512 cho access token; refresh token có thời hạn dài hơn
- Verify HMAC cho tất cả webhook từ VNPay/SePay
- Idempotency: không xử lý trùng lặp giao dịch
- Sử dụng HTTPS cho webhook và endpoint public

---

## Tài liệu & hỗ trợ

- Chi tiết thanh toán: `PAYMENT_INTEGRATION_GUIDE.md`
- Bản tóm tắt Phase 2: `PHASE2_IMPLEMENTATION_SUMMARY.md`
- Postman collection: `EduNexus_Payment_API_Phase2.postman_collection.json`

Vấn đề hoặc yêu cầu hỗ trợ: support@edunexus.com

---

## Góp ý & đóng góp

1. Fork repository
2. Tạo branch mới `feature/your-feature`
3. Commit & push
4. Tạo Pull Request mô tả thay đổi

---

**Last Updated:** 2026-07-13 23:22:25 +07:00
**Ngôn ngữ:** Tiếng Việt
