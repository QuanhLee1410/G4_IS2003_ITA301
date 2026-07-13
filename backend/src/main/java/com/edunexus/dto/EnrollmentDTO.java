package com.edunexus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {
    private Integer enrollmentId;
    private Integer studentId;
    private Integer courseId;
    private String courseName;
    private String status; // ACTIVE, COMPLETED, SUSPENDED
    private LocalDateTime enrolledAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class StudentWorkspaceDTO {
    private Integer studentId;
    private String username;
    private String fullName;
    private Integer totalCoursesEnrolled;
    private java.util.List<EnrollmentDTO> enrollments;
}
