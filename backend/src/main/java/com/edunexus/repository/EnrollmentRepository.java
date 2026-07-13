package com.edunexus.repository;

import com.edunexus.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    List<Enrollment> findByStudentId(Integer studentId);
    Optional<Enrollment> findByStudentIdAndCourseId(Integer studentId, Integer courseId);
    boolean existsByStudentIdAndCourseId(Integer studentId, Integer courseId);
    List<Enrollment> findByCourseId(Integer courseId);
}
