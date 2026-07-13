package com.edunexus.service;

import com.edunexus.dto.CourseDTO;
import com.edunexus.entity.Course;
import com.edunexus.entity.Section;
import com.edunexus.entity.User;
import com.edunexus.exception.AppException;
import com.edunexus.exception.ResourceNotFoundException;
import com.edunexus.repository.CourseRepository;
import com.edunexus.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    public CourseDTO createCourse(Integer teacherId, CourseDTO courseDTO) {
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        if (!"TEACHER".equals(teacher.getRole().getRoleName()) && !"ADMIN".equals(teacher.getRole().getRoleName())) {
            throw new AppException("Only teachers and admins can create courses");
        }

        Course course = new Course();
        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        course.setPrice(courseDTO.getPrice());
        course.setThumbnailUrl(courseDTO.getThumbnailUrl());
        course.setTeacher(teacher);

        course = courseRepository.save(course);
        log.info("Course created: {} by teacher: {}", course.getCourseId(), teacher.getUsername());

        return mapToDTO(course);
    }

    public CourseDTO updateCourse(Integer courseId, Integer teacherId, CourseDTO courseDTO) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.getTeacher().getUserId().equals(teacherId)) {
            throw new AppException("Only the course owner can update this course");
        }

        course.setTitle(courseDTO.getTitle());
        course.setDescription(courseDTO.getDescription());
        course.setPrice(courseDTO.getPrice());
        course.setThumbnailUrl(courseDTO.getThumbnailUrl());

        course = courseRepository.save(course);
        log.info("Course updated: {}", courseId);

        return mapToDTO(course);
    }

    public void deleteCourse(Integer courseId, Integer teacherId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        if (!course.getTeacher().getUserId().equals(teacherId)) {
            throw new AppException("Only the course owner can delete this course");
        }

        courseRepository.delete(course);
        log.info("Course deleted: {}", courseId);
    }

    public CourseDTO getCourseById(Integer courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        return mapToDTO(course);
    }

    public List<CourseDTO> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CourseDTO> getCoursesByTeacher(Integer teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private CourseDTO mapToDTO(Course course) {
        return CourseDTO.builder()
                .courseId(course.getCourseId())
                .title(course.getTitle())
                .description(course.getDescription())
                .price(course.getPrice())
                .thumbnailUrl(course.getThumbnailUrl())
                .teacherId(course.getTeacher().getUserId())
                .teacherName(course.getTeacher().getFullName())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
