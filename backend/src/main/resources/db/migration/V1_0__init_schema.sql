-- =====================================================================
-- KỊCH BẢN KHỞI TẠO CƠ SỞ DỮ LIỆU EDUNEXUS
-- Flyway Migration: V1_0__init_schema.sql
-- =====================================================================

-- PHẦN 1: QUẢN LÝ NGƯỜI DÙNG & PHÂN QUYỀN

-- Bảng Vai trò (Roles)
CREATE TABLE Roles (
    role_id INT IDENTITY(1,1) PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE
);

-- Bảng Người dùng (Users)
CREATE TABLE Users (
    user_id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    full_name NVARCHAR(100) NOT NULL,
    role_id INT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Users_Roles FOREIGN KEY (role_id) REFERENCES Roles(role_id)
);

-- PHẦN 2: CẤU TRÚC PHÂN CẤP NỘI DUNG HỌC TẬP

-- Bảng Khóa học (Courses)
CREATE TABLE Courses (
    course_id INT IDENTITY(1,1) PRIMARY KEY,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX),
    price DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    thumbnail_url VARCHAR(500),
    teacher_id INT NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    updated_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Courses_Users FOREIGN KEY (teacher_id) REFERENCES Users(user_id)
);

-- Bảng Chương học (Sections)
CREATE TABLE Sections (
    section_id INT IDENTITY(1,1) PRIMARY KEY,
    course_id INT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    order_index INT NOT NULL DEFAULT 1,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Sections_Courses FOREIGN KEY (course_id) REFERENCES Courses(course_id) ON DELETE CASCADE
);

-- Bảng Bài học (Lessons)
CREATE TABLE Lessons (
    lesson_id INT IDENTITY(1,1) PRIMARY KEY,
    section_id INT NOT NULL,
    title NVARCHAR(255) NOT NULL,
    content_type VARCHAR(20) NOT NULL DEFAULT 'VIDEO',
    video_url VARCHAR(500),
    document_url VARCHAR(500),
    duration_seconds INT DEFAULT 0,
    order_index INT NOT NULL DEFAULT 1,
    is_preview BIT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Lessons_Sections FOREIGN KEY (section_id) REFERENCES Sections(section_id) ON DELETE CASCADE
);

-- PHẦN 3: VẬN HÀNH VÀ THANH TOÁN

-- Bảng Đăng ký học (Enrollments)
CREATE TABLE Enrollments (
    enrollment_id INT IDENTITY(1,1) PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    enrolled_at DATETIME DEFAULT GETDATE(),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT FK_Enrollments_Users FOREIGN KEY (student_id) REFERENCES Users(user_id),
    CONSTRAINT FK_Enrollments_Courses FOREIGN KEY (course_id) REFERENCES Courses(course_id),
    CONSTRAINT UC_Student_Course UNIQUE (student_id, course_id)
);

-- Bảng Giao dịch thanh toán (Transactions)
CREATE TABLE Transactions (
    transaction_id INT IDENTITY(1,1) PRIMARY KEY,
    transaction_code VARCHAR(100) NOT NULL UNIQUE,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    amount DECIMAL(18, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    paid_at DATETIME,
    created_at DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Transactions_Users FOREIGN KEY (student_id) REFERENCES Users(user_id),
    CONSTRAINT FK_Transactions_Courses FOREIGN KEY (course_id) REFERENCES Courses(course_id)
);

-- PHẦN 4: KHỞI TẠO DỮ LIỆU MẶC ĐỊNH

-- Thêm các vai trò
INSERT INTO Roles (role_name) VALUES ('ADMIN'), ('TEACHER'), ('STUDENT'), ('SME');

-- Tạo các index cho tối ưu hóa query
CREATE INDEX IDX_Users_Username ON Users(username);
CREATE INDEX IDX_Users_Email ON Users(email);
CREATE INDEX IDX_Courses_TeacherId ON Courses(teacher_id);
CREATE INDEX IDX_Sections_CourseId ON Sections(course_id);
CREATE INDEX IDX_Lessons_SectionId ON Lessons(section_id);
CREATE INDEX IDX_Enrollments_StudentId ON Enrollments(student_id);
CREATE INDEX IDX_Enrollments_CourseId ON Enrollments(course_id);
CREATE INDEX IDX_Transactions_StudentId ON Transactions(student_id);
CREATE INDEX IDX_Transactions_CourseId ON Transactions(course_id);
CREATE INDEX IDX_Transactions_Status ON Transactions(status);
