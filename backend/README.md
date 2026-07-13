# EduNexus Backend - Learning Management System

Một nền tảng LMS (Learning Management System) hiện đại hỗ trợ quản lý khóa học, xác thực người dùng, thanh toán tự động, và tích hợp AI (Phase 2).

## Tính năng chính

### Phase 1: MVP (Đã hoàn thành)
- ✅ **Quản lý Khóa học** (Courses > Sections > Lessons)
- ✅ **Xác thực & Phân quyền** (JWT + RBAC)
- ✅ **Video Streaming** (AWS S3 Presigned URLs)
- ✅ **Đăng ký Khóa học** (Enrollments)
- ✅ **Exception Handling** (Global Error Handler)

### Phase 2: Thanh toán (Sẵn sàng)
- ✅ **Tích hợp VNPay** (Redirect Payment)
- ✅ **Tích hợp SePay** (Bank Transfer Webhook)
- ✅ **Tự động tạo Enrollment** (Khi thanh toán thành công)
- ✅ **Email/SMS Notification** (Tuỳ chọn)

### Phase 3: Quiz & AI (Tiếp theo)
- [ ] Hệ thống Quiz (CRUD + Auto Grading)
- [ ] Tích hợp Gemini API (Tóm tắt video, Flashcard, Chấm bài)

## Tech Stack

```
Backend:
  - Java 11+
  - Spring Boot 2.7
  - Spring Security + JWT
  - JPA/Hibernate
  - SQL Server 2016+
  - Redis (Cache)

Cloud & Storage:
  - AWS S3 (Video hosting)
  - Flyway (DB Migration)

Payment:
  - VNPay API
  - SePay Webhook

API Documentation:
  - OpenAPI/Swagger 3.0
  - Springdoc OpenAPI
```

## Setup & Installation

### 1. Yêu cầu hệ thống
```bash
- Java 11 hoặc cao hơn
- Maven 3.6+
- SQL Server 2016+
- Git
```

### 2. Clone repository
```bash
git clone https://github.com/QuanhLee1410/G4_IS2003_ITA301.git
cd G4_IS2003_ITA301/backend
```

### 3. Cấu hình Database
Cập nhật `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=EduNexusDB;encrypt=false;trustServerCertificate=true
spring.datasource.username=sa
spring.datasource.password=Admin@123
```

### 4. Cấu hình AWS S3 (tuỳ chọn)
```properties
aws.s3.bucket-name=edunexus-videos
aws.s3.region=us-east-1
aws.s3.access-key=YOUR_AWS_ACCESS_KEY
aws.s3.secret-key=YOUR_AWS_SECRET_KEY
```

### 5. Cấu hình Payment Gateway
```properties
# VNPay
vnpay.tmn-code=YOUR_TMN_CODE
vnpay.hash-secret=YOUR_HASH_SECRET

# SePay
sepay.api-key=YOUR_SEPAY_KEY
sepay.webhook-secret=YOUR_WEBHOOK_SECRET
```

### 6. Build & Run
```bash
# Build project
mvn clean package

# Run application
mvn spring-boot:run

# Ứng dụng chạy tại: http://localhost:8080/edunexus
# Swagger UI: http://localhost:8080/edunexus/swagger-ui.html
```

## API Endpoints

### Authentication
```
POST   /api/auth/register           - Đăng ký người dùng mới
POST   /api/auth/login              - Đăng nhập
POST   /api/auth/refresh            - Refresh access token
```

### Courses
```
GET    /api/courses                 - Danh sách tất cả khóa học (public)
POST   /api/courses                 - Tạo khóa học (TEACHER/ADMIN)
GET    /api/courses/{id}            - Chi tiết khóa học
PUT    /api/courses/{id}            - Cập nhật khóa học (owner)
DELETE /api/courses/{id}            - Xóa khóa học (owner)
GET    /api/courses/{id}/sections   - Danh sách chương
```

### Lessons
```
GET    /api/lessons/{id}            - Chi tiết bài học
GET    /api/lessons/{id}/video      - Lấy presigned URL video
PUT    /api/lessons/{id}/video-url  - Cập nhật URL video (TEACHER)
```

### Enrollments
```
POST   /api/enrollments             - Đăng ký khóa học (STUDENT)
GET    /api/enrollments/my-courses  - Khóa học đã đăng ký (STUDENT)
GET    /api/enrollments/check/{courseId} - Kiểm tra enrollment (STUDENT)
```

### Payments
```
POST   /api/payments/init           - Khởi tạo thanh toán (STUDENT)
GET    /api/payments/{id}           - Trạng thái giao dịch
GET    /api/payments/student/transactions - Danh sách giao dịch (STUDENT)
POST   /api/payments/vnpay-webhook  - VNPay callback
POST   /api/payments/sepay-webhook  - SePay callback
```

## Roles & Permissions

```
ADMIN:
  - Quản lý tất cả khóa học
  - Quản lý người dùng
  - Xem báo cáo

TEACHER:
  - Tạo/sửa/xóa khóa học riêng
  - Upload video & tài liệu
  - Chấm điểm bài tập
  - Xem danh sách học viên

STUDENT:
  - Xem khóa học đã mua
  - Xem video & tài liệu
  - Làm bài quiz
  - Thanh toán khóa học

SME:
  - Giống TEACHER
  - Hỗ trợ chính sắc bộ môn
```

## Database Schema

### Bảng chính
- `Users` - Người dùng (ID, Username, Email, Password, Role)
- `Courses` - Khóa học (ID, Title, Price, Teacher)
- `Sections` - Chương (ID, Course, Title, Order)
- `Lessons` - Bài học (ID, Section, Title, VideoUrl, Content)
- `Enrollments` - Đăng ký (ID, Student, Course, Status)
- `Transactions` - Giao dịch (ID, Student, Course, Amount, Status)

## JWT Token Structure

```json
Header:
{
  "alg": "HS512",
  "typ": "JWT"
}

Payload:
{
  "sub": "user_id",
  "username": "username",
  "role": "STUDENT",
  "iat": 1234567890,
  "exp": 1234571490
}

Token sẽ được gửi trong header: Authorization: Bearer <token>
```

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn test -P integration-test
```

### Manual Testing with Postman
```bash
# Import Postman collection từ:
# docs/postman/EduNexus.postman_collection.json
```

## Deployment

### Docker
```bash
# Build Docker image
docker build -t edunexus-backend .

# Run container
docker run -p 8080:8080 edunexus-backend
```

### Docker Compose
```bash
docker-compose up -d
```

### Production Checklist
- [ ] Thay đổi JWT secret key
- [ ] Cấu hình CORS domain
- [ ] Enable HTTPS/SSL
- [ ] Setup Redis cache
- [ ] Configure logging & monitoring
- [ ] Database backup strategy
- [ ] Load balancer setup

## Troubleshooting

### Database Connection Error
```
Kiểm tra: SQL Server đang chạy, credentials, port 1433 open
```

### JWT Validation Failed
```
Kiểm tra: Token format, secret key, expiration time
```

### S3 Upload Error
```
Kiểm tra: AWS credentials, bucket permissions, region
```

### Payment Webhook Not Working
```
Kiểm tra: Webhook URL public accessible, signature verification, IP whitelist
```

## Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## License

MIT License - See LICENSE file for details

## Support

Liên hệ: support@edunexus.com

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Frontend (React)                         │
└────────────────────────┬────────────────────────────────────┘
                         │
                    HTTP/HTTPS
                         │
        ┌────────────────┴─────────────────┐
        │                                   │
┌──────────────────────┐        ┌─────────────────────┐
│  Spring Boot Backend │        │  AI Service (Phase2)│
│  - Auth             │        │  - Python FastAPI   │
│  - Courses          │        │  - Gemini Integration
│  - Payments         │        │  - AI Tasks         │
│  - Quiz             │        │                     │
└──────────┬───────────┘        └─────────────────────┘
           │
    ┌──────┴──────────────────┐
    │                         │
┌───────────────────┐  ┌──────────────┐
│   SQL Server      │  │    Redis     │
│   - EduNexusDB    │  │   - Cache    │
└───────────────────┘  └──────────────┘
           │
    ┌──────┴──────────────────┐
    │                         │
┌───────────────────┐  ┌──────────────┐
│     AWS S3        │  │  Payment API │
│  - Video Files    │  │  - VNPay     │
│  - Documents      │  │  - SePay     │
└───────────────────┘  └──────────────┘
```

## Roadmap

### Q1 2024
- ✅ Phase 1: Core LMS (Completed)
- ✅ Phase 2: Payment Gateway (In Progress)

### Q2 2024
- [ ] Phase 3: Quiz System
- [ ] Phase 3: AI Integration (Gemini)
- [ ] Mobile App (React Native)

### Q3 2024
- [ ] Analytics & Reporting
- [ ] Live Class (WebRTC)
- [ ] Community Forum

### Q4 2024
- [ ] Advanced AI Features
- [ ] Marketplace
- [ ] Certification System

---

**Last Updated**: 2024-01-01
**Version**: 1.0.0-SNAPSHOT
