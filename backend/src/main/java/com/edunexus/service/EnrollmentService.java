package com.edunexus.service;

import com.edunexus.dto.EnrollmentDTO;
import com.edunexus.entity.Course;
import com.edunexus.entity.Enrollment;
import com.edunexus.entity.User;
import com.edunexus.exception.AppException;
import com.edunexus.exception.ResourceNotFoundException;
import com.edunexus.repository.CourseRepository;
import com.edunexus.repository.EnrollmentRepository;
import com.edunexus.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    public EnrollmentDTO enrollCourse(Integer studentId, Integer courseId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        if (!student.getRole().getRoleName().equals("STUDENT")) {
            throw new AppException("Only students can enroll in courses");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new AppException("Student is already enrolled in this course");
        }

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setStatus("ACTIVE");

        enrollment = enrollmentRepository.save(enrollment);
        log.info("Student {} enrolled in course {}", studentId, courseId);

        return mapToDTO(enrollment);
    }

    public List<EnrollmentDTO> getStudentEnrollments(Integer studentId) {
        return enrollmentRepository.findByStudentId(studentId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public boolean isStudentEnrolled(Integer studentId, Integer courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    public void updateEnrollmentStatus(Integer enrollmentId, String status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        enrollment.setStatus(status);
        enrollmentRepository.save(enrollment);
        log.info("Enrollment {} status updated to {}", enrollmentId, status);
    }

    private EnrollmentDTO mapToDTO(Enrollment enrollment) {
        return EnrollmentDTO.builder()
                .enrollmentId(enrollment.getEnrollmentId())
                .studentId(enrollment.getStudent().getUserId())
                .courseId(enrollment.getCourse().getCourseId())
                .courseName(enrollment.getCourse().getTitle())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
