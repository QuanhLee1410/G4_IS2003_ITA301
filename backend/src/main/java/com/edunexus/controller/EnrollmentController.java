package com.edunexus.controller;

import com.edunexus.dto.EnrollmentDTO;
import com.edunexus.security.UserPrincipal;
import com.edunexus.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@Slf4j
@Tag(name = "Enrollments", description = "Student course enrollment endpoints")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    private UserPrincipal getCurrentUser() {
        return (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Enroll in a course", description = "Student enrolls in a course they have purchased")
    public ResponseEntity<EnrollmentDTO> enrollCourse(@RequestParam Integer courseId) {
        UserPrincipal user = getCurrentUser();
        log.info("User {} enrolling in course {}", user.getUserId(), courseId);
        EnrollmentDTO enrollment = enrollmentService.enrollCourse(user.getUserId(), courseId);
        return new ResponseEntity<>(enrollment, HttpStatus.CREATED);
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get student's enrolled courses", description = "List all courses the student is enrolled in")
    public ResponseEntity<List<EnrollmentDTO>> getMyEnrollments() {
        UserPrincipal user = getCurrentUser();
        log.info("Getting enrollments for student: {}", user.getUserId());
        List<EnrollmentDTO> enrollments = enrollmentService.getStudentEnrollments(user.getUserId());
        return new ResponseEntity<>(enrollments, HttpStatus.OK);
    }

    @GetMapping("/check/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Check enrollment status", description = "Check if student is enrolled in a course")
    public ResponseEntity<?> checkEnrollment(@PathVariable Integer courseId) {
        UserPrincipal user = getCurrentUser();
        boolean isEnrolled = enrollmentService.isStudentEnrolled(user.getUserId(), courseId);
        return new ResponseEntity<>(
                java.util.Map.of("enrolled", isEnrolled),
                HttpStatus.OK
        );
    }
}
